package org.hyperagents.jacamo.artifacts.yggdrasil;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * The HMAS class represents the Hypermedia Multi-Agent System (HMAS).
 * It provides a set of constants for the HMAS vocabulary.
 */
public final class HMAS {
  public static final String PREFIX = "https://purl.org/hmas/";

  public static final IRI contains = createIRI("contains");

  public static IRI createIRI(String fragment) {
    return SimpleValueFactory.getInstance().createIRI(PREFIX + fragment);
  }

  private HMAS() {
  }
}
