package org.hyperagents.jacamo.artifacts.hmas;

import cartago.*;
import cartago.events.ArtifactObsEvent;
import ch.unisg.ics.interactions.hmas.core.io.InvalidResourceProfileException;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Structure;
import org.apache.jena.graph.Graph;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.hyperagents.jacamo.artifacts.namespaces.NSRegistry;
import org.hyperagents.jacamo.artifacts.yggdrasil.Notification;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SignifierExposureArtifact extends Artifact {

  private final ReentrantLock lock = new ReentrantLock();
  protected Map<AgentId, Set<RecommendationContext>> recommendationContextMap;
  protected Map<String, String> namespaces;
  protected Map<String, Model> situations;
  protected Map<String, Set<Model>> recommendedContexts;
  protected Model situation;
  protected Set<String> agentAbilityTypes = new HashSet<>();

  public void init() {
    this.recommendationContextMap = new HashMap();
    this.namespaces = new HashMap<>();
    this.situations = new HashMap<>();
    this.recommendedContexts = new HashMap<>();
    this.situation = new LinkedHashModel();
  }

  @LINK
  public void onNotification(Notification notification) {
    String contentType = notification.getContentType();

    if ("text/turtle".equals(contentType)) {
      try {
        ResourceProfile profile = ResourceProfileGraphReader.readFromString(notification.getMessage());
        Set<Signifier> signifiers = profile.getExposedSignifiers();
        this.handleSignifiers(signifiers);
      } catch (InvalidResourceProfileException e) {
        this.handleSituationNotification(notification);
      }
    }
  }

  private void handleSignifiers(Set<Signifier> signifiers) {
    lock.lock();
    try {
      for (Signifier signifier : signifiers) {
        if (signifier.getIRIAsString().isPresent()) {
          Set<Context> contexts = signifier.getRecommendedContexts();
          Set<Model> contextModels = new HashSet<>();
          for (Context context : contexts) {
            contextModels.add(context.getModel());
          }
          this.recommendedContexts.put(signifier.getIRIAsString().get(),contextModels);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private void handleSituationNotification(Notification notification) {
    lock.lock();
    try {
      String topic = notification.getTopic();
      Model situationModel = new LinkedHashModel(); // Create a new model

      // Use RDFParser to parse the message
      RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE); // Specify the RDF format

      try (StringReader reader = new StringReader(notification.getMessage())) {
        rdfParser.setRDFHandler(new org.eclipse.rdf4j.rio.helpers.StatementCollector(situationModel)); // Collect statements in the model
        rdfParser.parse(reader, "");
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      situations.put(topic, situationModel);
      situation.clear();
      for (Model model : situations.values()) {
        situation.addAll(model);
      }
    } finally {
      lock.unlock();
    }
  }

  @OPERATION
  public void setNamespace(String prefix, String namespace) {
    this.namespaces.put(prefix, namespace);
  }

  @OPERATION
  public void addRecommendationContext(String artifactProfileUrl, String agentProfileUrl, OpFeedbackParam<IEventFilter> filter) {

    AgentId agentId = this.getCurrentOpAgentId();

    // Create the new RecommendationContext for this subscription
    RecommendationContext newContext = new RecommendationContext(artifactProfileUrl, agentProfileUrl);

    // Retrieve the set of subscriptions for this agent
    Set<RecommendationContext> agentSubscriptions = recommendationContextMap.getOrDefault(agentId, new HashSet<>());

    // Check if the agent has already subscribed for this artifactProfileUrl
    // Remove the old subscription with the same artifactProfileUrl
    agentSubscriptions.remove(newContext);

    // Add the new subscription (whether it replaces or adds a new one)
    agentSubscriptions.add(newContext);

    // Update the subscription map for this agent
    recommendationContextMap.put(agentId, agentSubscriptions);

    try {
      ResourceProfile profile = ResourceProfileGraphReader.readFromURL(artifactProfileUrl);
      Set<Signifier> signifiers = profile.getExposedSignifiers();
      this.handleSignifiers(signifiers);
    } catch (IOException e) {
      failed("Unable to retrieve resource profile " + artifactProfileUrl + ". " + e.getMessage());
    }

    try {
      SignifierFilter sigFilter = getSignifierFilter(agentProfileUrl);
      filter.set(sigFilter);
    } catch (IOException e) {
      failed("Unable to retrieve agent profile " + agentProfileUrl + ". " + e.getMessage());
    }
  }

  protected SignifierFilter getSignifierFilter(String agentProfileUrl) throws IOException {
    ResourceProfile agentProfile = ResourceProfileGraphReader.readFromURL(agentProfileUrl);
    return new SignifierFilter(this, agentProfile);
  }

  public class SignifierFilter implements IEventFilter, java.io.Serializable {

    protected final Set<String> agentAbilityTypes = new HashSet<>();
    protected SignifierExposureArtifact artifact;
    protected ResourceProfile agentProfile;

    public SignifierFilter(SignifierExposureArtifact artifact, ResourceProfile agentProfile) {
      this.artifact = artifact;
      this.agentProfile = agentProfile;
      this.updateAgentMetadata();  // Update metadata after initialization
    }

    public void updateAgentProfile(ResourceProfile agentProfile) {
      this.agentProfile = agentProfile;
      this.updateAgentMetadata();
    }

    protected void updateAgentMetadata() {
      CapableAgent agent = (CapableAgent) this.agentProfile.getResource();
      Set<Ability> abilities = agent.getAbilities();
      for (Ability ability : abilities) {
        Set<String> types = ability.getSemanticTypes()
          .stream()
          .filter(type -> !type.equals(INTERACTION.ABILITY.toString()))
          .map(type -> NSRegistry.getPrefixedIRI(type, artifact.namespaces))
          .collect(Collectors.toSet());
        this.agentAbilityTypes.addAll(types);
      }
    }

    protected boolean isComplementary(ArtifactObsProperty prop) {

      if (prop.getValues().length < 2) {
        return true;
      }
      Object[] abilities = (Object[]) prop.getValue(1);
      String[] recommendedAbilityTypes = Arrays.copyOf(abilities, abilities.length, String[].class);
      boolean complementaryAbilities = complementaryAbilities(recommendedAbilityTypes);

      if (prop.getValues().length < 3 || !complementaryAbilities) {
        return complementaryAbilities;
      }
      if (((Object[]) prop.getValue(2)).length == 0) {
        return true;
      }

      List<Object> annotations = prop.getAnnots();
      for (Object annotation : annotations) {
        Structure structure = (Structure) annotation;
        if ("iri".equals(structure.getFunctor())) {
          StringTerm signifierIri = (StringTerm) structure.getTerm(0);
          return complementarySituation(signifierIri.getString());
        }
      }
      return false;
    }

    private boolean complementarySituation(String signifierIri) {
      lock.lock();
      try{
        if (!recommendedContexts.containsKey(signifierIri) || recommendedContexts.get(signifierIri).isEmpty()) {
          return true;
        }

        ShaclSail shaclSail = new ShaclSail(new MemoryStore());
        Repository repo = new SailRepository(shaclSail);

        try (RepositoryConnection connection = repo.getConnection()) {
          connection.begin();
          connection.add(situation);
          connection.commit();

          Set<Model> contexts = recommendedContexts.get(signifierIri);
          for (Model context : contexts) {
            connection.begin();
            connection.add(context, RDF4J.SHACL_SHAPE_GRAPH);
            connection.commit();
          }
        } catch (RepositoryException e) {
          return false;
        }
        return true;
      } finally {
        lock.unlock(); // Release the lock
      }
    }

    private boolean complementaryAbilities(String[] recommendedAbilityTypes) {
      return Arrays.stream(recommendedAbilityTypes)
        .allMatch(agentAbilityTypes::contains);
    }

    @Override
    public boolean select(ArtifactObsEvent ev) {

      ArtifactObsProperty[] added = ev.getAddedProperties();
      ArtifactObsProperty[] changed = ev.getChangedProperties();
      ArtifactObsProperty[] removed = ev.getRemovedProperties();

      if (added != null) {
        for (ArtifactObsProperty prop : added) {
          String propName = prop.getName();
          if ("signifier".equals(propName)) {
            return isComplementary(prop);
          } else {
            return true;
          }
        }
      }

      if (changed != null) {
        for (ArtifactObsProperty prop : changed) {
          String propName = prop.getName();
          if ("signifier".equals(propName)) {
            return isComplementary(prop);
          } else {
            return true;
          }
        }
      }

      if (removed != null) {
        for (ArtifactObsProperty prop : removed) {
          String propName = prop.getName();
          if ("signifier".equals(propName)) {
            return isComplementary(prop);
          } else {
            return true;
          }
        }
      }
      return false;
    }
  }
}
