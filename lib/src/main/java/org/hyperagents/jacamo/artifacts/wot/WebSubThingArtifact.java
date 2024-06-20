package org.hyperagents.jacamo.artifacts.wot;

import java.util.Optional;
import java.io.IOException;

import cartago.LINK;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.hyperagents.jacamo.artifacts.yggdrasil.Notification;
import org.apache.hc.client5.http.fluent.Request;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension to the ThingArtifact class that adds Yggdrasil-specific WebSub
 * support.
 * WebSubThingArtifact is a subclass of ThingArtifact and provides additional
 * functionality
 * for registering WebSub to a Yggdrasil node.
 *
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 * - Valentin Berger, Interactions-HSG, University of St. Gallen
 *
 */
public class WebSubThingArtifact extends ThingArtifact {

    @Override
    public void init(String url) {
        super.init(url);
        exposeWebSubIRIs(url);
    }

    @LINK
    public void onNotification(Notification notification) {
        log("The state of this ThingArtifact has changed: " + notification.getMessage());

        String obsProp = notification.getMessage();
        String functor = obsProp.substring(0, obsProp.indexOf("("));
        String[] params = obsProp.substring(obsProp.indexOf("(") + 1, obsProp.length() - 1)
                .split(",");

        if (this.hasObsPropertyByTemplate(functor, (Object[]) params)) {
            this.updateObsProperty(functor, (Object[]) params);
        } else {
            this.defineObsProperty(functor, (Object[]) params);
        }
    }

    /*
     * Expose WebSub IRIs to the Agent from the given URL. This method checks the
     * headers and the content of the URL if it is an HTML document.
     */
    private void exposeWebSubIRIs(String url) {
        try {
            ClassicHttpResponse classicResponse = (ClassicHttpResponse) Request
                    .get(url).execute().returnResponse();
            Header[] linkHeaders = classicResponse.getHeaders("Link");

            String contentType = classicResponse.getFirstHeader("content-type").getValue();

            HttpEntity entity = classicResponse.getEntity();
            String content = "";
            if (entity != null && contentType.contains("text/html")) {
                content = EntityUtils.toString(entity);
            }

            Optional<String> hub = Optional.empty();
            Optional<String> topic = Optional.empty();

            // Check if the headers contain the WebSub links
            for (Header h : linkHeaders) {
                if (h.getValue().endsWith("rel=\"hub\"")) {
                    hub = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
                }
                if (h.getValue().endsWith("rel=\"self\"")) {
                    topic = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
                }
            }

            if (hub.isPresent() && topic.isPresent()) {
                log("Found WebSub links in headers: " + hub.get() + ", " + topic.get());
                defineObsProperty("websub", hub.get(), topic.get());
                return;
            }

            // Check if the content contains the WebSub links
            if (!hub.isPresent() && !topic.isPresent() && contentType.contains("text/html")) {
                Pattern hubPattern = Pattern.compile("<link rel=\"hub\" href=\"([^\"]+)\">");
                Pattern selfPattern = Pattern.compile("<link rel=\"self\" href=\"([^\"]+)\">");
                Matcher hubMatcher = hubPattern.matcher(content);
                Matcher selfMatcher = selfPattern.matcher(content);

                if (hubMatcher.find()) {
                    String hubHref = hubMatcher.group(1);
                    hub = Optional.of(hubHref);
                }

                if (selfMatcher.find()) {
                    String selfHref = selfMatcher.group(1);
                    topic = Optional.of(selfHref);
                }
            }

            if (hub.isPresent() && topic.isPresent()) {
                log("Found WebSub links in Document: " + hub.get() + ", " + topic.get());
                defineObsProperty("websub", hub.get(), topic.get());
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}