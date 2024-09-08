package org.hyperagents.jacamo.artifacts.yggdrasil;

import cartago.*;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * NotificationServerArtifact is an artifact that acts as a server for receiving
 * notifications from Yggdrasil WebSub nodes.
 * It is responsible for registering artifacts for WebSub
 * and delivering notifications to the registered artifacts.
 *
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 * - Valentin Berger, Interactions-HSG, University of St. Gallen
 *
 */
public class NotificationServerArtifact extends Artifact {
  private Map<Subscription, Set<ArtifactId>> artifactRegistry;
    private AbstractQueue<Notification> notifications;

    private String callbackUri;

    private Server server;
    private boolean httpServerRunning;

  public static final int NOTIFICATION_DELIVERY_DELAY = 100;
  protected Optional<String> agentWebId;

  void init(String host, Integer port) {

      this.agentWebId = Optional.empty();
      StringBuilder callbackBuilder = new StringBuilder("http://").append(host);

      if (port != null) {
        callbackBuilder.append(":")
          .append(port);
      }

      //callbackUri = callbackBuilder.append("/notifications/").toString();
      server = new Server(port);

      String serverURIStr = server.getURI().toString();
      String hostStr = serverURIStr.substring(0, serverURIStr.length() - 1);
      callbackUri = hostStr + ":" + port + "/notifications/";

      server.setHandler(new NotificationHandler());

    artifactRegistry = new Hashtable<Subscription, Set<ArtifactId>>();
      notifications = new ConcurrentLinkedQueue<Notification>();
  }

  @OPERATION
  public void setOperatorWebId(String webId) {
    this.agentWebId = Optional.of(webId);
  }

  /**
     * Registers an artifact for WebSub and sends a subscribe request to the
     * specified hub.
     *
     * @param artifactIRI The IRI of the artifact.
     * @param artifactId  The ID of the artifact.
     * @param hubIRI      The IRI of the WebSub hub.
     */
  @LINK
  @OPERATION
  void registerArtifactForWebSub(String artifactIRI, ArtifactId artifactId, String hubIRI) {
    Subscription subscription = new Subscription(artifactIRI, hubIRI);
    if (artifactRegistry.containsKey(subscription)) {
      artifactRegistry.get(subscription).add(artifactId);
    } else {
      Set<ArtifactId> artifactIds = new HashSet<>();
      artifactIds.add(artifactId);
      artifactRegistry.put(subscription, artifactIds);
      sendSubscribeRequest(hubIRI, artifactIRI);
    }
  }

  /**
   * Registers an artifact for focus in the workspace and sends a focus request.
   *
   * @param workspaceIRI The IRI of the workspace.
   * @param artifactIRI  The IRI of the artifact.
   * @param artifactId   The ID of the artifact.
   * @param artifactName The name of the artifact.
   */
  @OPERATION
  void registerArtifactForFocus(String workspaceIRI, String artifactIRI, ArtifactId artifactId,
                                String artifactName) {
    Subscription subscription = new Subscription(artifactName, workspaceIRI);
    if (artifactRegistry.containsKey(subscription)) {
      artifactRegistry.get(subscription).add(artifactId);
    } else {
      Set<ArtifactId> artifactIds = new HashSet<>();
      artifactIds.add(artifactId);
      artifactRegistry.put(subscription, artifactIds);
      sendFocusRequest(workspaceIRI, artifactName);
    }
  }

  /**
   * Starts the notification server.
   * This method sets the httpServerRunning flag to true, executes the internal
   * operation "deliverNotifications",
   * and starts the server.
   * If an exception occurs, it will be printed to the standard error stream.
   */
  @OPERATION
  void start() {
    try {
      httpServerRunning = true;

      execInternalOp("deliverNotifications");

      server.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Stops the notification server.
   */
  @OPERATION
  void stop() {
    try {
      server.stop();
      httpServerRunning = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Delivers notifications to registered artifacts.
   * This method runs in an internal operation and continuously checks for
   * notifications in the queue.
   * If a notification is found, it retrieves the corresponding artifact and
   * invokes the "onNotification" method on it.
   * If an exception occurs during the invocation, it is printed to the standard
   * error stream.
   */
  @INTERNAL_OPERATION
  void deliverNotifications() {
    while (httpServerRunning) {
      while (!notifications.isEmpty()) {

        Notification n = notifications.poll();

        Set<ArtifactId> allArtifactIds = getSubscriberIds(n);

        for (ArtifactId artifactId : allArtifactIds) {
          try {

            execLinkedOp(artifactId, "onNotification", n);

          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

      await_time(NOTIFICATION_DELIVERY_DELAY);
    }
  }

  private void sendSubscribeRequest(String hubIRI, String artifactIRI) {
        HttpClient client = new HttpClient();
        try {
            client.start();

            ContentResponse response = client.POST(hubIRI)
                    .content(new StringContentProvider("{"
                            + "\"hub.mode\" : \"subscribe\","
                            + "\"hub.topic\" : \"" + artifactIRI + "\","
                            + "\"hub.callback\" : \"" + callbackUri + "\""
                            + "}"), "application/json")
                    .send();
            if (response.getStatus() != HttpStatus.SC_OK) {
               // log("Request failed: " + response.getStatus());
            }

            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFocusRequest(String workspaceIRI, String artifactName) {
        HttpClient client = new HttpClient();
        try {
            client.start();

          String agentWebIdHeaderValue = this.agentWebId
            .orElse("http://localhost:8080/agents/" + this.getCurrentOpAgentId().getAgentName());

          ContentResponse response = client.POST(workspaceIRI + "/focus")
            .header("X-Agent-WebID", agentWebIdHeaderValue)
                    .content(new StringContentProvider("{"
                            + "\"artifactName\" : \"" + artifactName + "\","
                            + "\"callbackIri\" : \"" + callbackUri + "\""
                            + "}"), "application/json")
                    .send();
            if (response.getStatus() != HttpStatus.SC_OK) {
                log("Request failed: " + response.getStatus());
            }

            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  protected boolean isSubscriptionTopic(String artifactIRI) {
    return artifactRegistry.entrySet().stream()
      .anyMatch(entry -> entry.getKey().topic().equals(artifactIRI));
  }

  protected Set<ArtifactId> getSubscriberIds(Notification n) {
    Set<ArtifactId> allArtifactIds = new HashSet<>();
    for (Map.Entry<Subscription, Set<ArtifactId>> entry : artifactRegistry.entrySet()) {
      Subscription subscription = entry.getKey();
      Set<ArtifactId> artifactIds = entry.getValue();

      // Check if the subscription's topic (artifactIRI) matches the input artifactIRI
      if (subscription.topic().equals(n.getTopic())) {
        allArtifactIds.addAll(artifactIds); // Add all artifactIds from the matching subscription
      }
    }
    return allArtifactIds;
  }

  class NotificationHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {

      String artifactIRI = null;
      Enumeration<String> linkHeadersEnum = baseRequest.getHeaders("Link");

      while (linkHeadersEnum.hasMoreElements()) {
        String value = linkHeadersEnum.nextElement();

        if (value.endsWith("rel=\"self\"")) {
          artifactIRI = value.substring(1, value.indexOf('>'));
        }
      }

      if (artifactIRI == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.getWriter()
          .println("Link headers are missing! See the W3C WebSub Recommendation for details.");
      } else {
        Subscription subscription = new Subscription(artifactIRI, baseRequest.getUri().toString());
        if (isSubscriptionTopic(artifactIRI)) {
          String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
          notifications.add(new Notification(artifactIRI, payload, baseRequest.getContentType()));

          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      }
      baseRequest.setHandled(true);
    }
  }

}
