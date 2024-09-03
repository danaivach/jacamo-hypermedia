package org.hyperagents.jacamo.artifacts.hmas;

import cartago.ArtifactId;
import cartago.LINK;
import cartago.OPERATION;
import ch.unisg.ics.interactions.hmas.core.hostables.AbstractResource;
import ch.unisg.ics.interactions.hmas.core.hostables.Agent;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Ability;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.CapableAgent;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ResourceProfile;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public abstract class AbstractAgentBoardArtifact extends ResourceArtifact {

  private Set<Ability> abilities;
  private IRI agentIRI;

  public void init(String url, ArtifactId signifierManager, boolean dryRun) {
    super.init(url, signifierManager, dryRun);

    if (!(this.profile.getResource() instanceof Agent)) {
      failed("An AgentBoard should be initialised based on the profile of an Agent");
    }

    if (!(this.profile.getResource() instanceof Agent)) {
      failed("An AgentBoard should be initialised based on the profile of an Agent");
    }

    if (profile.getResource().getIRI().isPresent() && ((AbstractResource) profile.getResource()).getGraph().isPresent()) {
      this.agentIRI = profile.getResource().getIRI().get();
      CapableAgent agent = (CapableAgent) this.profile.getResource();
      this.abilities = agent.getAbilities();
    } else {
      failed("Could not read RDF graph for container: " + profile.getResource().getIRI());
    }
  }

  @LINK
  @OPERATION
  public abstract void writeAbility(String[] abilityTypes);

  @LINK
  @OPERATION
  public abstract String[] readAbilityTypes();

  @LINK
  @OPERATION
  public abstract void deleteAbility(String abilityType);
}
