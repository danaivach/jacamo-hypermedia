package org.hyperagents.jacamo.artifacts.yggdrasil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.hyperagents.jacamo.artifacts.hmas.HMAS;
import org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact;

import cartago.OPERATION;
import cartago.LINK;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;

/**
 * A hypermedia artifact that can contain other artifacts. The containment
 * relation is given by
 * {@code eve:contains}. Contained artifacts are exposed as observable
 * properties using by default
 * the Jason functor "member" or one that is passed as an argument during
 * artifact initialization.
 *
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St.Gallen
 *
 */
public class WorkspaceThingArtifact extends WebSubThingArtifact {
  private IRI workspaceIRI;
  private Model graph;
  private ValueFactory rdf;

  @Override
  public void init(String url, boolean dryRun) {
    init(url);
    this.dryRun = dryRun;
  }

  @Override
  public void init(String url) {
    super.init(url);
    this.rdf = SimpleValueFactory.getInstance();
    exposeMemberProperties();
  }

  @OPERATION
  public void joinHypermediaWorkspace() {
    if (td.getThingURI().isPresent() && td.getGraph().isPresent()) {
      this.invokeAction("joinWorkspace");
    }
  }

  @LINK
  @Override
  public void onNotification(Notification notification) {
    try {
      this.td = TDGraphReader.readFromString(TDFormat.RDF_TURTLE, notification.getMessage());
      exposeMemberProperties();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void exposeMemberProperties() {
    if (td.getThingURI().isPresent() && td.getGraph().isPresent()) {
      this.workspaceIRI = rdf.createIRI(td.getThingURI().get());
      this.graph = td.getGraph().get();

      // Extract parent workspaces (if any) from the workspace description
      List<String> parents = Models.objectIRIs(graph.filter(workspaceIRI, HMAS.isContainedIn, null))
        .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
      exposeProperties(parents, "parentHypermediaWorkspace");

      // Extract child workspaces (if any) from the workspace description
      List<String> workspaces = getMembersOfType(HMAS.Workspace);
      exposeProperties(workspaces, "hypermediaWorkspace");

      // Extract artifacts (if any) from the workspace description
      List<String> artifacts = getMembersOfType(HMAS.Artifact);
      exposeProperties(artifacts, "hypermediaArtifact");
    } else {
      failed("Could not read RDF graph for container: " + td.getThingURI());
    }
  }

  String writeToString(RDFFormat format, Model model) {
    OutputStream out = new ByteArrayOutputStream();

    try {
      Rio.write(model, out, format,
        new WriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true));
    } finally {
      try {
        out.close();
      } catch (IOException e) {

      }
    }

    return out.toString();
  }

  private List<String> getMembersOfType(IRI memberType) {
    return Models.objectIRIs(graph.filter(workspaceIRI, HMAS.contains, null))
      .stream().filter(iri -> graph.contains(iri, RDF.TYPE, memberType))
      .map(iri -> iri.stringValue())
      .collect(Collectors.toList());
  }

  private void exposeProperties(List<String> list, String obsPropertyName) {
    for (String memberIRI : list) {
      MemberMetadata data = new MemberMetadata(memberIRI);
      if (getObsPropertyByTemplate(obsPropertyName, memberIRI, data.memberName) == null) {
        this.defineObsProperty(obsPropertyName, memberIRI, data.memberName,
          data.memberTypes.toArray(new String[0]));
      }
    }
  }

  class MemberMetadata {
    String memberIRI;
    String memberName;
    List<String> memberTypes;

    MemberMetadata(String memberIri) {
      this.memberIRI = memberIri;

      try {
        ThingDescription td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, memberIRI);
        memberName = td.getTitle();
        memberTypes = Models.objectIRIs(td.getGraph().get().filter(rdf.createIRI(memberIRI), RDF.TYPE, null))
          .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
      } catch (IOException | NoSuchElementException e) {
        failed(e.getMessage());
      }
    }
  }
}
