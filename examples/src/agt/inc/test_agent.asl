@agent_metadata_update[atomic]
+agent_metadata(KnownPlan, AbilityType) : true <-
  -+ability(AbilityType);
  .relevant_plans({+!test_goal}, _, LL);
  .remove_plan(LL);
  .add_plan(KnownPlan).

@workspace_discovery_custom[atomic]
+workspace(WkspIRI, WkspName) : true <-
  .print("Discovered workspace (name: ", WkspName ,"): ", WkspIRI);

  // Create a CArtAgO Workspace that will contain the hypermedia WorkspaceArtifact and its contained artifacts.
  //createWorkspace(WkspName);

  // Join the CArtAgO Workspace
  //!joinWorkspace(WkspName, WkspId);

  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., to create an artifact).
  .term2string(WkspNameTerm, WkspName);
  ?joinedWsp(WkspId, WkspNameTerm,_);
  !makeMirroringWorkspace(WkspIRI, WkspName, WkspId);

  // Join the hypermedia WorkspaceArtifact
  !joinHypermediaWorkspace(WkspName);

  .print("Created workspace artifact ", WkspName, ", joined, and registered for notifications").

@artifact_discovery_custom[atomic]
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : true <-
  .print("Discovered artifact (name: ", ArtName ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);
  ?joinedWsp(WkspId, WkspNameTerm, WkspName);

  // Create a hypermedia ResourceArtifact for this artifact.
  !makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId);

  // Focus on the ResourceArtifact
  focus(ArtId);

  // Register to the ResourceArtifact for notifications
  ?websub(HubIRI, TopicIRI)[artifact_id(ArtId)];
  registerArtifactForWebSub(TopicIRI, ArtId, HubIRI);

  .print("Created artifact ", ArtName, ", and registered for notifications");
  +setupDone.

@hypermedia_artifact_instantiation_hmas_dryrun
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") & web_id(WebId) <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true], ArtId)[wid(WkspId)];
  !registerNamespaces(ArtId);
  setOperatorWebId(WebId)[artifact_id(ArtId)].

