package org.hyperagents.jacamo.artifacts.yggdrasil;

public class Notification {
    private String entityIRI;
    private String message;
    private String contentType;

    /**
     * Represents a notification with an entity IRI and a message.
     */
    public Notification(String entityIRI, String message, String contentType) {
        this.entityIRI = entityIRI;
        this.message = message;
        this.contentType = contentType;
    }

    public String getEntityIRI() {
        return entityIRI;
    }

    public String getMessage() {
        return message;
    }

    public String getContentType() {
    return contentType;
  }
}
