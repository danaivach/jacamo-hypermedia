package org.hyperagents.jacamo.artifacts.yggdrasil;

import cartago.LINK;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.hmas.core.hostables.AbstractResource;
import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ResourceProfile;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class WorkspaceResourceArtifact extends WebSubResourceArtifact {
  private IRI workspaceIRI;
  private Model graph;
  private ValueFactory rdf;
  private final Map<String, ObsProperty> parentWorkspaces = new HashMap();
  private final Map<String, ObsProperty> workspaces = new HashMap();
  private final Map<String, ObsProperty> artifacts = new HashMap();

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

  @LINK
  @Override
  public void onNotification(Notification notification) {
    try {
      this.profile = ResourceProfileGraphReader.readFromString(notification.getMessage());
      exposeMemberProperties();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void exposeMemberProperties() {
    if (profile.getResource().getIRI().isPresent() && ((AbstractResource) profile.getResource()).getGraph().isPresent()) {
      this.workspaceIRI = profile.getResource().getIRI().get();
      this.graph = profile.getGraph().get();

      // Extract parent workspaces (if any) from the workspace description
      List<String> parents = Models.objectIRIs(graph.filter(workspaceIRI, CORE.IS_CONTAINED_IN, null))
        .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
      exposeProperties(parents, "parentWorkspace");

      // Extract child workspaces (if any) from the workspace description
      List<String> workspaces = getMembersOfType(CORE.WORKSPACE);
      exposeProperties(workspaces, "workspace");

      // Extract artifacts (if any) from the workspace description
      List<String> artifacts = getMembersOfType(CORE.ARTIFACT);
      exposeProperties(artifacts, "artifact");
    } else {
      failed("Could not read RDF graph for container: " + profile.getResource().getIRI());
    }
  }

  /*
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
   */

  private List<String> getMembersOfType(IRI memberType) {
    return Models.objectIRIs(graph.filter(workspaceIRI, CORE.CONTAINS, null))
      .stream().filter(iri -> graph.contains(iri, RDF.TYPE, memberType))
      .map(iri -> iri.stringValue())
      .collect(Collectors.toList());
  }

  private void exposeProperties(List<String> list, String obsPropertyName) {
    Map<String, Map<String, ObsProperty>> propertyMap = Map.of(
      "parentWorkspace", parentWorkspaces,
      "workspace", workspaces,
      "artifact", artifacts
    );

    Map<String, ObsProperty> targetMap = propertyMap.get(obsPropertyName);
    if (targetMap == null) {
      throw new IllegalArgumentException("Invalid obsPropertyName: " + obsPropertyName);
    }

    for (String memberIRI : list) {
      if (!parentWorkspaces.containsKey(memberIRI) && !workspaces.containsKey(memberIRI) && !artifacts.containsKey(memberIRI)) {
        MemberMetadata data = new MemberMetadata(memberIRI);
        ObsProperty property = this.defineObsProperty(obsPropertyName, memberIRI, data.memberName,
          data.memberTypes.toArray(new String[0]));
        targetMap.put(memberIRI, property);
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
        ResourceProfile profile = ResourceProfileGraphReader.readFromURL(memberIRI);
        this.memberTypes = Models.objectIRIs(profile.getGraph().get().filter(rdf.createIRI(memberIRI), RDF.TYPE, null))
          .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
        this.memberName = extractMemberName(this.memberIRI, this.memberTypes);
      } catch (IOException | NoSuchElementException e) {
        failed(e.getMessage());
      }
    }

    private String extractMemberName(String memberIRI, List<String> memberTypes) {
      int startIndex = -1;
      int endIndex = -1;

      if (memberTypes.contains(CORE.WORKSPACE.stringValue())) {
        startIndex = memberIRI.indexOf("workspaces/") + "workspaces/".length();
        endIndex = memberIRI.indexOf("/#workspace", startIndex);
      }
      else if (memberTypes.contains(CORE.ARTIFACT.stringValue())) {
        startIndex = memberIRI.indexOf("artifacts/") + "artifacts/".length();
        endIndex = memberIRI.indexOf("/#artifact", startIndex);
      }

      String memberName = endIndex != -1 ? memberIRI.substring(startIndex, endIndex) : memberIRI.substring(startIndex);

      memberName = memberName.replaceAll("[^a-zA-Z0-9_]", "");

      // Ensure the result does not start with a number
      if (memberName.isEmpty() || Character.isDigit(memberName.charAt(0))) {
        return null;  // or return "" or "Invalid name"
      }

      return memberName;
    }
  }
}
