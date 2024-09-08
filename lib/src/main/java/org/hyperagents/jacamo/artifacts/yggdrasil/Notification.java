package org.hyperagents.jacamo.artifacts.yggdrasil;

public class Notification {
    private String topic;
    private String message;
    private String contentType;

    /**
     * Represents a notification with an entity IRI and a message.
     */
    public Notification(String topic, String message, String contentType) {
        this.topic = topic;
        this.message = message;
        this.contentType = contentType;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    public String getContentType() {
    return contentType;
  }
}
