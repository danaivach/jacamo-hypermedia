package org.hyperagents.jacamo.artifacts.hmas;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.LINK;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;

import java.util.Set;

public abstract class AbstractSignifierExposureArtifact extends Artifact {

  protected ArtifactId agentBoardId;
  public void init(ArtifactId agentBoardId) {
    this.agentBoardId = agentBoardId;
  }

  @LINK
  public abstract void exposeSignifiers(Set<Signifier> signifiers) ;
}
