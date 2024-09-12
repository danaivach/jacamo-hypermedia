package org.hyperagents.jacamo.artifacts.namespaces;

import cartago.Artifact;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.eclipse.rdf4j.model.util.Values;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NSRegistryArtifact extends Artifact {

  protected NSRegistry registry;

  public void init() {
    this.registry = new NSRegistry();
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
    this.registry.setNamespace(prefix, namespace);
    defineObsProperty("namespace", prefix, namespace);
  }


  @OPERATION @LINK
  public void prefixedIRI(String iri, OpFeedbackParam<String> prefixedIRI) {
    prefixedIRI.set(NSRegistry.getPrefixedIRI(iri, this.registry));
  }

  @OPERATION @LINK
  public void resolvedIRI(String prefixedIRI, OpFeedbackParam<String> iri) {
    iri.set(NSRegistry.getResolvedIRI(prefixedIRI, this.registry));
  }

}
