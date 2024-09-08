package org.hyperagents.jacamo.artifacts.yggdrasil;

public record Subscription(String topic, String hubIri) {

  @Override
  public String topic() {
    return topic;
  }

  @Override
  public String hubIri() {
    return hubIri;
  }
}
