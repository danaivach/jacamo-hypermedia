package org.hyperagents.jacamo.artifacts.hmas;

import cartago.Artifact;
import cartago.GUARD;
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
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

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
  protected boolean dryRun;
  protected Map<String, String> namespaces;
  private String exposureState;

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

  public void init(String url, boolean dryRun) {
    init(url);
    this.dryRun = dryRun;
  }

  /**
   * CArtAgO operation for exposing hMAS signifiers to the belief base of the caller agent.
   * Signifiers reveal information about the possible interactions offered by a resource.
   */
  void exposeSignifiers() {
    getObsProperty("exposureState").updateValue("inProgress");
    for (Signifier signifier : this.profile.getExposedSignifiers()) {
      if (signifier.getIRIAsString().isPresent()) {
        String signifierIri = signifier.getIRIAsString().get();
        Set<String> actionTypes = signifier.getActionSpecification().getRequiredSemanticTypes();

        List<String> curieActionTypes = actionTypes.stream()
          .map(this::getPrefixedIRI)  // Apply getCurie to each element
          .collect(Collectors.toList());

        List<String> recommendedAbilities = getRecommendedAbilities(signifier);
        List<String> recommendedContexts = getRecommendedContexts(signifier);

        if (!this.exposedSignifiers.containsKey(signifierIri)) {
          Structure iriAnnotation = ASSyntax.createStructure("iri", ASSyntax.createString(signifierIri));

          ObsProperty signifierProperty = this.defineObsProperty("signifier", curieActionTypes.toArray(),
            recommendedAbilities.toArray(), recommendedContexts.toArray());
          signifierProperty.addAnnot(iriAnnotation);
          this.exposedSignifiers.put(signifierIri, signifierProperty);
        } else {
          this.exposedSignifiers.get(signifierIri).updateValues(curieActionTypes.toArray(),
            recommendedAbilities.toArray(), recommendedContexts.toArray());
        }
      }
    }
    getObsProperty("exposureState").updateValue("done");
  }

  @GUARD
  boolean exposureState(String state) {
    return this.exposureState.equals(state);
  }

  @OPERATION
  void updateExposureState() {
    this.exposureState = "done";
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

    actionTag = getResolvedIRI(actionTag);
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

  /*
  private String getPrefixedIri(String iri) {
    if (nsRegistryId.isPresent()) {
      OpFeedbackParam<String> prefixedIri = new OpFeedbackParam<>();
      try {
        execLinkedOp(this.nsRegistryId.get(), "prefixedIRI", iri, prefixedIri);
        return prefixedIri.get();
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
    }
    return iri;
  }

  private String getResolvedIri(String iri) {
    if (nsRegistryId.isPresent()) {
      OpFeedbackParam<String> resolvedIri = new OpFeedbackParam<>();
      try {
        execLinkedOp(this.nsRegistryId.get(), "resolvedIRI", iri, resolvedIri);
        return resolvedIri.get();
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
    }
    return iri;
  }
  */

  protected List<String> getRecommendedAbilities(Signifier signifier) {
    Set<Ability> recommendedAbilities = signifier.getRecommendedAbilities();
    return recommendedAbilities.stream()
      .map(a -> {
        Set<String> abilityTypes = new HashSet<>(a.getSemanticTypes());
        abilityTypes.remove(INTERACTION.ABILITY.stringValue());
        return this.getPrefixedIRI((String) abilityTypes.toArray()[0]);
      })
      .toList();
  }


  protected List<String> getRecommendedContexts(Signifier signifier) {
    Set<Context> contexts = signifier.getRecommendedContexts();
    List<String> BDIContexts = new ArrayList<>();

    SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
    IRI hasBeliefResource = valueFactory.createIRI("http://example.org/hasBelief");

// Iterate over each context
    for (Context context : contexts) {
      Model contextModel = context.getModel();

      // Extract beliefs (subjects) for the current context
      List<Resource> beliefsForCurrentContext = contextModel.filter(null, SHACL.PATH, hasBeliefResource)
        .stream()
        .map(Statement::getSubject)
        .collect(Collectors.toList());

      // For each belief (subject), collect the objects of statements where SHACL.HAS_VALUE is the predicate
      for (Resource belief : beliefsForCurrentContext) {
        List<String> beliefContents = contextModel.filter(belief, SHACL.HAS_VALUE, null)
          .stream()
          .map(statement -> statement.getObject().stringValue()) // Convert Value to String
          .collect(Collectors.toList());

        // Add the belief content (as Strings) to the overall list
        BDIContexts.addAll(beliefContents);
      }
    }
    return BDIContexts;
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

}
