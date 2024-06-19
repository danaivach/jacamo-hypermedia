package org.hyperagents.jacamo.artifacts.yggdrasil;

import java.util.Optional;
import java.io.IOException;

import cartago.LINK;

import org.hyperagents.jacamo.artifacts.wot.ThingArtifact;

import org.apache.hc.core5.http.Header;
import org.apache.hc.client5.http.fluent.Request;

public class YggdrasilThingArtifact extends ThingArtifact {

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
     * Registers for WebSub to an Yggdrasil node. This is not a generic
     * implementation, but one specific to Yggdrasil.
     */
    private void exposeWebSubIRIs(String url) {
        try {
            Header[] headers = Request.get(url).execute().returnResponse().getHeaders("Link");

            // This current implementation is specific to Yggdrasil, not a general
            // implementation
            if (headers.length != 2) {
                return;
            }

            Optional<String> hub = Optional.empty();
            Optional<String> topic = Optional.empty();

            for (Header h : headers) {
                if (h.getValue().endsWith("rel=\"hub\"")) {
                    hub = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
                }
                if (h.getValue().endsWith("rel=\"self\"")) {
                    topic = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
                }
            }

            if (hub.isPresent() && topic.isPresent()) {
                log("Found WebSub links: " + hub.get() + ", " + topic.get());
                defineObsProperty("websub", hub.get(), topic.get());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
