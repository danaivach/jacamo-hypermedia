package org.hyperagents.jacamo.artifacts.hmas;

import cartago.*;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;

import java.util.Set;

public abstract class AbstractSignifierExposureArtifact extends Artifact {

  private ArtifactId agentBoardId;
  public void init(ArtifactId agentBoardId) {
    this.agentBoardId = agentBoardId;
  }

  @LINK
  public abstract void exposeSignifiers(Set<Signifier> signifiers) ;
}
