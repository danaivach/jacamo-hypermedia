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
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
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
  protected Map<String, String> namespaces;
  protected Map<String, ObsProperty> exposedSignifiers;
  protected boolean dryRun;

  protected boolean logTime;

  /**
   * Method called by CArtAgO to initialize the artifact. The hMAS Resource Profile used by this
   * artifact is retrieved and parsed during initialization.
   *
   * @param url A URL that dereferences to an hMAS Resource Profile.
   */
  public void init(String url) {
    try {
      profile = ResourceProfileGraphReader.readFromURL(url);
    } catch (IOException e) {
      failed(e.getMessage());
    }

    this.logTime = false;

    this.agentWebId = Optional.empty();
    this.apiKey = Optional.empty();
    this.namespaces = new HashMap<>();
    this.exposedSignifiers = new HashMap<>();
    this.dryRun = false;

    defineObsProperty("exposureState", "inProgress");
    this.exposeSignifiers();
  }

  /**
   * Method called by CArtAgO to initialize the artifact. The hMAS Resource Profile used by this
   * artifact is retrieved and parsed during initialization.
   *
   * @param url    A URL that dereferences to an hMAS Resource Profile.
   * @param dryRun When set to true, the requests are logged, but not executed.
   */
  public void init(String url, boolean dryRun) {
    init(url);
    this.dryRun = dryRun;
  }

  @OPERATION
  public void setLogTime(boolean enabled) {
    this.logTime = enabled;
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
        Set<Ability> recommendedAbilities = signifier.getRecommendedAbilities();

        List<String> curieActionTypes = actionTypes.stream()
          .map(this::getCurie)  // Apply getCurie to each element
          .collect(Collectors.toList());

        List<String> curieAbilities = recommendedAbilities.stream()
          .map(a -> {
            Set<String> abilityTypes = new HashSet<>(a.getSemanticTypes());
            abilityTypes.remove(INTERACTION.ABILITY.stringValue());
            return this.getCurie((String) abilityTypes.toArray()[0]);
          })  // Apply getCurie to each element
          .toList();

        if (!this.exposedSignifiers.containsKey(signifierIri)) {
          Structure iriAnnotation = ASSyntax.createStructure("iri", ASSyntax.createString(signifierIri));

          ObsProperty signifierProperty = this.defineObsProperty("signifier", curieActionTypes.toArray(),
            curieAbilities.toArray());
          signifierProperty.addAnnot(iriAnnotation);
          this.exposedSignifiers.put(signifierIri, signifierProperty);

        } else {
          this.exposedSignifiers.get(signifierIri).updateValues(curieActionTypes.toArray(), curieAbilities.toArray());
        }
      }
    }
    getObsProperty("exposureState").updateValue("done");
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
   * CArtAgO operation for setting namespaces for this artifact, so that the agent can
   * use compact URIs upon using the artifact, e.g., use "saref:ToggleCommand" instead
   * of "https://saref.etsi.org/core/ToggleCommand"
   *
   * @param prefix    The prefix of the namespace
   * @param namespace The IRI of the namespace
   */
  @OPERATION
  public void setNamespace(String prefix, String namespace) {
    try {
      new ValidatingValueFactory().createIRI(namespace);
    } catch (IllegalArgumentException e) {
      failed("IRIs of registered namespaces must be absolute. Invalid namespace: " + namespace);
    }
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
    String resolvedActionTag = resolveCurie(actionTag);

    Optional<Signifier> signifierOp = this.profile.getFirstExposedSignifier(resolvedActionTag);

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

  private String getCurie(String iri) {

    try {
      ParsedIRI parsedAbsoluteIri = new ParsedIRI(iri);

      for (Map.Entry<String, String> nsEntry : namespaces.entrySet()) {
        ParsedIRI parsedNamespace = new ParsedIRI(nsEntry.getValue());
        ParsedIRI relativeIri = parsedNamespace.relativize(parsedAbsoluteIri);
        if (!parsedAbsoluteIri.equals(relativeIri)) {
          return nsEntry.getKey() + ":" + relativeIri;
        }
      }
    } catch (URISyntaxException e) {
      failed("Attempted to use an invalid IRI: " + iri);
    }
    return iri;
  }

  private String resolveCurie(String iri) {

    Set<Namespace> nsSet = namespaces.entrySet().stream()
      .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
      .collect(Collectors.toSet());

    return Values.iri(nsSet, iri).stringValue();
  }

}
