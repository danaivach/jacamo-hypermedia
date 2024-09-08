package org.hyperagents.jacamo.artifacts.hmas;

import cartago.*;
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

  public void init(){
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
  }

  /*
  @OPERATION @LINK
  public void prefixedIRI(String iri, OpFeedbackParam<String> prefixedIRI) {
    prefixedIRI.set(NSRegistry.getPrefixedIRI(iri, this.registry));
  }

  @OPERATION @LINK
  public void resolvedIRI(String prefixedIRI, OpFeedbackParam<String> iri) {
    iri.set(NSRegistry.getResolvedIRI(prefixedIRI, this.registry));
  }
  */


  public class NSRegistry {

    protected Map<String, String> namespaces;

    protected NSRegistry(Map<String, String> namespaces) {
      this.namespaces = namespaces;
    }

    protected NSRegistry() {
      this.namespaces = new HashMap<>();
    }

    protected Map<String, String> getNamespaces() {
      return namespaces;
    }

    protected void setNamespaces(Map<String, String> namespaces) {
      this.namespaces = namespaces;
    }

    protected void addNamespaces(Map<String, String> namespaces) {
      this.namespaces.putAll(namespaces);
    }

    protected void setNamespace(String prefix, String namespace) {
      try {
        new ValidatingValueFactory().createIRI(namespace);
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      }
      defineObsProperty("namespace", prefix, namespace);
      this.namespaces.put(prefix, namespace);
    }

    protected void removeNamespace(String prefix) {
      this.namespaces.remove(prefix);
    }

    public static String getPrefixedIRI(String iri, NSRegistry registry) {
      try {
        ParsedIRI parsedAbsoluteIri = new ParsedIRI(iri);

        for (Map.Entry<String, String> nsEntry : registry.getNamespaces().entrySet()) {
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

    public static String getResolvedIRI(String iri, NSRegistry registry) {
      Set<Namespace> nsSet = registry.getNamespaces().entrySet().stream()
        .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
        .collect(Collectors.toSet());

      return Values.iri(nsSet, iri).stringValue();
    }
  }
}
