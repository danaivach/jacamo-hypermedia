package org.hyperagents.jacamo.artifacts.hmas;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.hmas.bindings.Action;
import ch.unisg.ics.interactions.hmas.bindings.ActionExecution;
import ch.unisg.ics.interactions.hmas.bindings.protocols.ProtocolBinding;
import ch.unisg.ics.interactions.hmas.bindings.protocols.ProtocolBindings;
import ch.unisg.ics.interactions.hmas.bindings.protocols.http.HttpAction;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.SHACL;
import jason.asSyntax.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hyperagents.jacamo.artifacts.namespaces.NSRegistry;

import java.io.IOException;
import java.util.*;

/**
 * A CArtAgO artifact that can interpret an hMAS Resource Profile and exposes signifiers that reveal
 * to agents how to interact with a resource. The artifact uses the hypermedia controls provided in the
 * Profile to compose and issue requests for the signified possible interactions based on the available
 * protocol and payload bindings.
 * <p>
 * Contributors:
 * - Danai Vachtsevanou (author), Interactions-HSG, University of St.Gallen
 */

public class ResourceArtifact extends Artifact {

  protected ResourceProfile profile;
  protected Optional<String> agentWebId;
  protected Optional<String> apiKey;
  protected Map<String, ObsProperty> exposedSignifiers;
  protected boolean interactionGuidance = false;
  protected boolean dryRun;
  protected Map<String, String> namespaces;
  private String exposureState;

  // Method to find all subjects that have a specific predicate in the model
  private static Set<Resource> findSubjectsWithPredicate(Model model, org.eclipse.rdf4j.model.IRI predicate) {
    Set<Resource> subjects = new HashSet<>();
    for (Statement statement : model.filter(null, predicate, null)) {
      subjects.add(statement.getSubject());
    }
    return subjects;
  }

  // Extract properties into a Map for a given subject
  private static Map<String, String> extractProperties(Model model, Resource subject) {
    Map<String, String> properties = new HashMap<>();
    properties.put("propertyType", findObjectOfPredicate(model, subject, SHACL.PATH));
    properties.put("propertyValue", findObjectOfPredicate(model, subject, SHACL.HAS_VALUE));
    properties.put("targetArtifact", findObjectOfPredicate(model, subject, SimpleValueFactory.getInstance()
      .createIRI(SHACL.NAMESPACE + "targetNode")));
    return properties;
  }

  // Helper method to find the object of a specific predicate for a given subject
  private static String findObjectOfPredicate(Model model, Resource subject, org.eclipse.rdf4j.model.IRI predicate) {
    return model.filter(subject, predicate, null)
      .stream()
      .findFirst()
      .map(Statement::getObject)
      .map(Value::stringValue)
      .orElse(null);
  }

  public void init(String url) {
    try {
      profile = ResourceProfileGraphReader.readFromURL(url);
    } catch (IOException e) {
      failed(e.getMessage());
    }

    this.agentWebId = Optional.empty();
    this.apiKey = Optional.empty();
    this.exposedSignifiers = new HashMap<>();
    this.dryRun = false;
    this.namespaces = new HashMap<>();
    defineObsProperty("exposureState", "inProgress");
    this.exposeSignifiers();
  }

  public void init(String url, boolean interactionGuidance) {
    this.interactionGuidance = interactionGuidance;
    init(url);
  }

  public void init(String url, boolean interactionGuidance, boolean dryRun) {
    init(url, interactionGuidance);
    this.dryRun = dryRun;
  }

  /**
   * CArtAgO operation for exposing hMAS signifiers to the belief base of the caller agent.
   * Signifiers reveal information about the possible interactions offered by a resource.
   */
  void exposeSignifiers() {
    getObsProperty("exposureState").updateValue("inProgress");

    for (Signifier signifier : this.profile.getExposedSignifiers()) {
      signifier.getIRIAsString().ifPresent(signifierIri -> {
        Set<String> actionTypes = signifier.getActionSpecification().getRequiredSemanticTypes();
        List<String> curieActionTypes = actionTypes.stream()
          .map(type -> NSRegistry.getPrefixedIRI(type, this.namespaces))
          .toList();

        // Get recommended abilities and contexts if interactionGuidance is true
        List<String> recommendedAbilities = interactionGuidance ? getRecommendedAbilities(signifier) : null;
        List<Literal> recommendedContexts = interactionGuidance ? getRecommendedContexts(signifier) : null;

        ObsProperty signifierProperty = createOrUpdateObsProperty(signifierIri, curieActionTypes, recommendedAbilities, recommendedContexts);

        // Add annotation to the property
        Structure iriAnnotation = ASSyntax.createStructure("iri", ASSyntax.createString(signifierIri));
        signifierProperty.addAnnot(iriAnnotation);
      });
    }

    getObsProperty("exposureState").updateValue("done");
  }

  // Helper method to create or update ObsProperty
  private ObsProperty createOrUpdateObsProperty(String signifierIri, List<String> curieActionTypes, List<String> recommendedAbilities, List<Literal> recommendedContexts) {
    ObsProperty signifierProperty;

    if (this.exposedSignifiers.containsKey(signifierIri)) {
      signifierProperty = this.exposedSignifiers.get(signifierIri);
      if (interactionGuidance) {
        signifierProperty.updateValues(curieActionTypes.toArray(),
          recommendedAbilities != null ? recommendedAbilities.toArray() : new String[0],
          recommendedContexts != null ? recommendedContexts.toArray() : new String[0]);
      } else {
        List<StringTermImpl> types = curieActionTypes.stream()
          .map(StringTermImpl::new).toList();

        ListTerm typesList = new ListTermImpl();
        typesList.addAll(types);
        signifierProperty.updateValues(typesList);
      }
    } else {
      signifierProperty = createObsProperty(curieActionTypes, recommendedAbilities, recommendedContexts);
      this.exposedSignifiers.put(signifierIri, signifierProperty);
    }

    return signifierProperty;
  }

  // Helper method to create ObsProperty based on the conditions
  private ObsProperty createObsProperty(List<String> curieActionTypes, List<String> recommendedAbilities, List<Literal> recommendedContexts) {
    if (interactionGuidance) {
      return this.defineObsProperty("signifier", curieActionTypes.toArray(),
        recommendedAbilities != null ? recommendedAbilities.toArray() : new String[0],
        recommendedContexts != null ? recommendedContexts.toArray() : new String[0]);
    } else {
      ListTerm typesList = new ListTermImpl();
      curieActionTypes.forEach(type -> typesList.append(new StringTermImpl(type)));
      return this.defineObsProperty("signifier", typesList);
    }
  }

  /**
   * CArtAgO operation for setting the WebID of an operating agent using the artifact.
   *
   * @param webId The operating agent's WebID as a string.
   */
  @OPERATION
  public void setOperatorWebId(String webId) {
    this.agentWebId = Optional.of(webId);
  }

  /**
   * <p>CArtAgO operation for setting a namespace, which will be used for operations,
   * observable properties, and observable events of the artifact.</p>
   *
   * <p>For example, by setting a namespace with <code>prefix="saref"</code> and
   * <code>namespace="https://saref.etsi.org/core/"</code>, an agent can invoke actions using
   * either the full IRI, e.g., <code>invokeAction("https://saref.etsi.org/core/ToggleCommand")</code>,
   * or the CURIE (Compact URI), e.g., <code>invokeAction("saref:ToggleCommand")</code>, and both will
   * produce the same result.</p>
   *
   * @param prefix    The prefix of the namespace, e.g., "saref".
   * @param namespace The name of the namespace, e.g., "https://saref.etsi.org/core/".
   */
  @OPERATION
  public void setNamespace(String prefix, String namespace) {
    this.namespaces.put(prefix, namespace);
    this.exposeSignifiers();
  }

  /**
   * CArtAgO operation for invoking an action on a resource using a semantic model of the resource.
   *
   * @param actionTag An IRI that identifies the action type, e.g. "https://saref.etsi.org/core/ToggleCommand"
   */
  @OPERATION
  public void invokeAction(String actionTag) {

    actionTag = NSRegistry.getResolvedIRI(actionTag, this.namespaces);
    Optional<Signifier> signifierOp = this.profile.getFirstExposedSignifier(actionTag);

    if (signifierOp.isPresent()) {
      Signifier signifier = signifierOp.get();
      ActionSpecification actionSpec = signifier.getActionSpecification();
      Form form = actionSpec.getFirstForm();

      ProtocolBinding binding = ProtocolBindings.getBinding(form);
      Action action = binding.bind(form);

      this.agentWebId.ifPresent(action::setActorId);

      if (action instanceof HttpAction) {
        String agentName = this.getCurrentOpAgentId().getAgentName();
        ((HttpAction) action).setHeader("X-Agent-LocalName", agentName);
      }

      if (dryRun) {
        System.out.println(action);
      } else {
        try {
          ActionExecution actionExec = action.execute();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      failed("Unknown action: " + actionTag);
    }
  }

  protected List<String> getRecommendedAbilities(Signifier signifier) {
    Set<Ability> recommendedAbilities = signifier.getRecommendedAbilities();
    return recommendedAbilities.stream()
      .map(a -> {
        Set<String> abilityTypes = new HashSet<>(a.getSemanticTypes());
        abilityTypes.remove(INTERACTION.ABILITY.stringValue());
        return NSRegistry.getPrefixedIRI((String) abilityTypes.toArray()[0], this.namespaces);
      })
      .toList();
  }

  protected List<Literal> getRecommendedContexts(Signifier signifier) {
    Set<Context> contexts = signifier.getRecommendedContexts();
    List<Literal> recommendedContexts = new ArrayList<>();

    SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

    for (Context context : contexts) {
      Model contextModel = context.getModel();

      Set<Resource> subjects = findSubjectsWithPredicate(contextModel, SHACL.PATH);

      // Process each subject and print the extracted properties
      subjects.forEach(shapeResource -> {
        Map<String, String> properties = extractProperties(contextModel, shapeResource);

        StringTerm propertyType = ASSyntax.createString(NSRegistry.getPrefixedIRI(properties.get("propertyType"), this.namespaces));
        StringTerm propertyValue = ASSyntax.createString(properties.get("propertyValue"));
        StringTerm targetResource = ASSyntax.createString(properties.get("targetArtifact"));

        Literal contextProperty = new LiteralImpl("property");
        contextProperty.addTerm(propertyType);
        contextProperty.addTerm(propertyValue);

        Structure iriAnnotation = ASSyntax.createStructure("iri", targetResource);
        contextProperty.addAnnot(iriAnnotation);
        recommendedContexts.add(contextProperty);
      });
    }
    return recommendedContexts;
  }
}
