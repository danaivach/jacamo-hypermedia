package org.hyperagents.jacamo.artifacts.hmas;

import cartago.*;
import cartago.events.ArtifactObsEvent;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Ability;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.CapableAgent;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ResourceProfile;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.hyperagents.jacamo.artifacts.yggdrasil.Notification;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class SignifierExposureArtifact extends Artifact {

  protected Map<AgentId, Set<RecommendationContext>> recommendationContextMap;
  protected Optional<ArtifactId> nsRegistryId = Optional.empty();
  protected Map<String, String> namespaces;

  public void init() {
    this.recommendationContextMap = new HashMap<AgentId, Set<RecommendationContext>>();
    this.namespaces = new HashMap<>();
  }

  @LINK
  public void onNotification(Notification notification) {
    //System.out.println("HOOOOOOOOOOOOOOOOORAAAAAAAAAY");
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
      SignifierFilter sigFilter = getSignifierFilter(agentProfileUrl);
      filter.set(sigFilter);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected SignifierFilter getSignifierFilter(String agentProfileUrl) throws IOException {
    ResourceProfile agentProfile = ResourceProfileGraphReader.readFromURL(agentProfileUrl);
    return new SignifierFilter(this, agentProfile);
  }

  private String getPrefixedIRI(String iri) {
    try {
      ParsedIRI parsedAbsoluteIri = new ParsedIRI(iri);

      for (Map.Entry<String, String> nsEntry : this.namespaces.entrySet()) {
        ParsedIRI parsedNamespace = new ParsedIRI(nsEntry.getValue());
        ParsedIRI relativeIri = parsedNamespace.relativize(parsedAbsoluteIri);
        if (!parsedAbsoluteIri.equals(relativeIri)) {
          return nsEntry.getKey() + ":" + relativeIri;
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return iri;
  }

  private String getResolvedIRI(String iri) {
    Set<Namespace> nsSet = this.namespaces.entrySet().stream()
      .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
      .collect(Collectors.toSet());

    return Values.iri(nsSet, iri).stringValue();
  }

  public class SignifierFilter implements IEventFilter, java.io.Serializable {

    protected SignifierExposureArtifact artifact;
    private ResourceProfile agentProfile;
    private final Set<String> agentAbilityTypes = new HashSet<>();

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
          .map(type -> artifact.getPrefixedIRI(type))
          .collect(Collectors.toSet());
        this.agentAbilityTypes.addAll(types);
      }
      System.out.println("Discovered agent abilities: " + this.agentAbilityTypes);
    }

    protected boolean idComplementary(ArtifactObsProperty prop) {
      Object[] objArray = (Object[]) prop.getValue(1);
      String[] recommendedAbilityTypes = Arrays.copyOf(objArray, objArray.length, String[].class);
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
            return idComplementary(prop);
          } else {
            return true;
          }
        }
      }

      if (changed != null) {
        for (ArtifactObsProperty prop : changed) {
          String propName = prop.getName();
          if ("signifier".equals(propName)) {
            return idComplementary(prop);
          } else {
            return true;
          }
        }
      }

      if (removed != null) {
        for (ArtifactObsProperty prop : removed) {
          String propName = prop.getName();
          if ("signifier".equals(propName)) {
            return idComplementary(prop);
          } else {
            return true;
          }
        }
      }
      return false;
    }
  }
}
