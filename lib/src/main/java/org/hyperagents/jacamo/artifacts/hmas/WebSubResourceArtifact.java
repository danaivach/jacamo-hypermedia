package org.hyperagents.jacamo.artifacts.hmas;

import cartago.LINK;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.hmas.core.io.InvalidResourceProfileException;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import jason.asSyntax.*;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.hyperagents.jacamo.artifacts.namespaces.NSRegistry;
import org.hyperagents.jacamo.artifacts.yggdrasil.Notification;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSubResourceArtifact extends ResourceArtifact {

  @Override
  public void init(String url) {
    super.init(url);
    exposeWebSubIRIs(url);
  }

  @LINK
  public void onNotification(Notification notification) {

    if ("text/turtle".equals(notification.getContentType())) {
      try {
        this.profile = ResourceProfileGraphReader.readFromString(notification.getMessage());
        this.exposeSignifiers();
      } catch (InvalidResourceProfileException e) {
        handlePropertyNotification(notification);
      }
    } else if ("application/json".equals(notification.getContentType())) {
      handlePropertyNotification(notification);
    }
  }

  private void handlePropertyNotification(Notification notification) {
    if ("text/turtle".equals(notification.getContentType())) {
      try {
        Optional<IRI> resourceIri = this.profile.getResource().getIRI();
        if (resourceIri.isPresent()) {
          StringReader reader = new StringReader(notification.getMessage());
          Model model = Rio.parse(reader, "", RDFFormat.TURTLE);
          Set<Statement> propertyStatements = new HashSet<>(model.filter(resourceIri.get(), null, null));
          for (Statement statement : propertyStatements) {
            ObsProperty property;
            String predicate = NSRegistry.getPrefixedIRI(statement.getPredicate().stringValue(), this.namespaces);
            String object = statement.getObject().stringValue();
            if (statement.getObject().isResource()) {
              object = NSRegistry.getPrefixedIRI(object, this.namespaces);
              property = this.defineObsProperty("property", predicate, object);
            } else {
              property = this.defineObsProperty("property", predicate, new Atom(object));
            }
            StringTerm targetResource = new StringTermImpl(resourceIri.get().stringValue());
            Structure iriAnnotation = ASSyntax.createStructure("iri", targetResource);
            property.addAnnot(iriAnnotation);
          }
        }
      } catch (RDFParseException e1) {
        throw new RuntimeException("RDFParseException: Failed to parse Turtle RDF message.", e1);
      } catch (IOException e2) {
        throw new RuntimeException("IOException: Failed to read Turtle RDF message.", e2);
      } catch (RDFHandlerException e3) {
        throw new RuntimeException("RDFHandlerException: Error during RDF processing.", e3);
      } catch (Exception ex) {
        throw new RuntimeException("Unexpected error during RDF parsing.", ex);
      }
    } else if ("application/json".equals(notification.getContentType())) {
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
  }

  /*
   * Expose WebSub IRIs to the Agent from the given URL. This method checks the
   * headers and the content of the URL if it is an HTML document.
   */
  private void exposeWebSubIRIs(String url) {
    try {

      ClassicHttpResponse classicResponse = (ClassicHttpResponse) Request.get(url).execute().returnResponse();
      Header[] linkHeaders = classicResponse.getHeaders("Link");
      String contentType = classicResponse.getFirstHeader("content-type").getValue();
      HttpEntity entity = classicResponse.getEntity();

      String content = entity != null && contentType.contains("text/html")
        ? EntityUtils.toString(entity)
        : null;

      Optional<String> hub = Optional.empty();
      Optional<String> topic = Optional.empty();

      // Parse the Link headers
      for (Header header : linkHeaders) {
        Map<String, String> links = parseLinkHeader(header.getValue());
        if (links.containsKey("hub")) {
          hub = Optional.of(links.get("hub"));
        }
        if (links.containsKey("self")) {
          topic = Optional.of(links.get("self"));
        }
        if (hub.isPresent() && topic.isPresent()) {
          break;
        }
      }

      if (hub.isPresent() && topic.isPresent()) {
        //log("Found WebSub links in headers: " + hub.get() + ", " + topic.get());
        defineObsProperty("websub", hub.get(), topic.get());
        return;
      }

      // Parse the HTML content if headers did not contain the links
      if (content != null) {
        hub = extractLinkFromContent(content, "<link rel=\"hub\" href=\"([^\"]+)\">");
        topic = extractLinkFromContent(content, "<link rel=\"self\" href=\"([^\"]+)\">");
      }

      if (hub.isPresent() && topic.isPresent()) {
        log("Found WebSub links in Document: " + hub.get() + ", " + topic.get());
        defineObsProperty("websub", hub.get(), topic.get());
      }

    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }
  }

  private Map<String, String> parseLinkHeader(String headerValue) {
    Map<String, String> links = new HashMap<>();
    String[] parts = headerValue.split(",\\s*<");
    for (String part : parts) {
      String[] linkAndRel = part.split(">;\\s*rel=\"");
      if (linkAndRel.length == 2) {
        String url = linkAndRel[0].replace("<", "");
        String rel = linkAndRel[1].replace("\"", "");
        if ("hub".equals(rel) || "self".equals(rel)) {
          links.put(rel, url);
        }
      }
    }
    return links;
  }

  private Optional<String> extractLinkFromContent(String content, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    }
    return Optional.empty();
  }

}
