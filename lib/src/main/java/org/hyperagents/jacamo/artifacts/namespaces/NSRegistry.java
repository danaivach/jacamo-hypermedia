package org.hyperagents.jacamo.artifacts.namespaces;

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

        // Check if the absolute IRI starts with the namespace IRI
        if (parsedAbsoluteIri.toString().startsWith(parsedNamespace.toString())) {
          // Calculate the relative path
          String relativePath = parsedAbsoluteIri.toString().substring(parsedNamespace.toString().length());
          return nsEntry.getKey() + ":" + relativePath;
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return iri; // Return original IRI if no match found
  }

  public static String getResolvedIRI(String iri, NSRegistry registry) {
    if (iri.contains(":")) {
      // Split the IRI at the first colon to separate the prefix and local part
      String[] parts = iri.split(":", 2);

      // If there are exactly two parts, assume it's a prefix + local name
      if (parts.length == 2) {
        String prefix = parts[0];
        String localName = parts[1];

        // Get the namespaces from the registry
        Map<String, String> namespaces = registry.getNamespaces();
        String namespace = namespaces.get(prefix);

        // If the prefix is found, resolve the IRI using the namespace
        if (namespace != null) {
          return namespace + localName;
        } else {
          return iri;
        }
      }
    }

    // If the IRI does not contain a colon or cannot be resolved, return it as-is
    return iri;
  }

  public static String getPrefixedIRI(String iri, Map<String, String> namespaces) {
    return getPrefixedIRI(iri, new NSRegistry(namespaces));
  }

  public static String getResolvedIRI(String iri, Map<String, String> namespaces) {
    return getResolvedIRI(iri, new NSRegistry(namespaces));
  }


}
