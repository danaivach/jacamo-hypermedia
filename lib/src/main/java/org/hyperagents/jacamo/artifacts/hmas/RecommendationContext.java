package org.hyperagents.jacamo.artifacts.hmas;

import cartago.AgentId;

import java.util.Objects;

public class RecommendationContext {

  private final String resourceProfileUrl;
  private final String agentProfileUrl;

  public RecommendationContext(String resourceProfileUrl, String agentProfileUrl) {
    this.resourceProfileUrl = resourceProfileUrl;
    this.agentProfileUrl = agentProfileUrl;
  }

  public String getResourceProfileUrl() {
    return resourceProfileUrl;
  }

  public String getAgentProfileUrl() {
    return agentProfileUrl;
  }

  // Override equals and hashCode to ensure proper comparison based on artifactProfileUrl
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecommendationContext that = (RecommendationContext) o;
    return resourceProfileUrl.equals(that.resourceProfileUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceProfileUrl);
  }
}
