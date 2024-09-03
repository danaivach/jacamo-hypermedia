package org.hyperagents.jacamo.artifacts.hmas;

import cartago.ArtifactId;
import cartago.LINK;
import cartago.OPERATION;
import ch.unisg.ics.interactions.hmas.core.hostables.AbstractResource;
import ch.unisg.ics.interactions.hmas.core.hostables.Agent;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Ability;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.CapableAgent;
import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public abstract class AbstractAgentBoardArtifact extends ResourceArtifact {

  protected Set<Ability> abilities;
  protected IRI agentIRI;

  public void init(String url, boolean dryRun) {
    super.init(url, dryRun);

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
