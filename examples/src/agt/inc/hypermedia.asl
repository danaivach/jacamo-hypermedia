/*
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

@environment_loading
 +!load_environment(EnvUrl, EnvName) : true <-
   .print("Loading environment (entry point): ", EnvUrl);
   !setNamespace("jacamo", "https://purl.org/hmas/jacamo/");
   +workspace(EnvUrl, EnvName).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

@workspace_discovery[atomic]
+workspace(WkspIRI, WkspName) : true <-
  .print("Discovered workspace (name: ", WkspName ,"): ", WkspIRI);

 .term2string(WkspNameTerm, WkspName);
  ?joinedWsp(WkspId, WkspNameTerm, _);

  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., to create an artifact).
  !makeMirroringWorkspace(WkspIRI, WkspName, WkspId);

  // Join the hypermedia WorkspaceArtifact
  !joinHypermediaWorkspace(WkspName);

  .print("Created workspace artifact ", WkspName, ", joined, and registered for notifications").

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

@artifact_discovery
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : true <-
  .print("Discovered artifact (name: ", ArtName ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);
  ?joinedWsp(WkspId, WkspNameTerm, WkspName);

  // Create a hypermedia ResourceArtifact for this artifact.
  !makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId);

  // Focus on the ResourceArtifact
  focus(ArtId);

  // Register to the ResourceArtifact for notifications
  !registerForWebSub(ArtName, ArtId);

  .term2string(WkspNameTerm, WkspNameStr);
  ?workspace(WkspIRI, WkspNameStr);

  // Focus on the ResourceArtifact to observe its properties and events
  registerArtifactForFocus(WkspIRI, ArtIRI, ArtId, ArtName);

  .print("Created artifact ", ArtName, ", and registered for notifications").

/* Supporting plans */

/* Workspaces */

@workspace_instantiation_hmas
+!makeMirroringWorkspace(WkspIRI, WkspName, WkspId) : vocabulary("https://purl.org/hmas/") & web_id(WebId) <-
  makeArtifact(WkspName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceResourceArtifact", [WkspIRI], WkspArtId)[wid(WkspId)];
  .print("Registering namespaces for ", WkspName);
  !registerNamespaces(WkspArtId);
  setOperatorWebId(WebId)[artifact_id(WkspArtId)].

@workspace_instantiation_wot
+!makeMirroringWorkspace(WkspIRI, WkspName, WkspId) : true <-
  makeArtifact(WkspName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceThingArtifact", [WkspIRI], WkspArtId)[wid(WkspId)].

@workspace_joining
+!joinWorkspace(WkspName, WkspId) : true <-
  joinWorkspace(WkspName, WkspId);
  .term2string(WkspNameTerm, WkspName);
  ?joinedWsp(WkspId, WkspNameTerm,_).

@hypermedia_workspace_joining
+!joinHypermediaWorkspace(WkspName) : true <-
  lookupArtifact(WkspName, WkspArtId);
  focus(WkspArtId);
  !joinHypermediaWorkspace(WkspName, WkspArtId);
  !registerForWebSub(WkspName, WkspArtIdTerm).

@hypermedia_workspace_joining_hmas
+!joinHypermediaWorkspace(WkspName, WkspArtId) : vocabulary("https://purl.org/hmas/") & signifier(["jacamo:JoinWorkspace"],_,_) <-
  invokeAction("jacamo:JoinWorkspace")[artifact_id(WkspArtId)].

@hypermedia_workspace_joining_td
+!joinHypermediaWorkspace(WkspName, WkspArtId) : true <-
  joinHypermediaWorkspace[artifact_id(WkspArtId)].

/* Artifacts */

@hypermedia_artifact_instantiation_hmas
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") & web_id(WebId) <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI], ArtId)[wid(WkspId)];
  !registerNamespaces(ArtId);
  setOperatorWebId(WebId)[artifact_id(ArtId)].

@hypermedia_artifact_instantiation_wot
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : true <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact", [ArtIRI], ArtId)[wid(WkspId)].

/* WebSub */

@websub_registration
+!registerForWebSub(ArtName, ArtId) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_id(ArtId)];
  registerArtifactForWebSub(TopicIRI, ArtId, HubIRI).

@websub_registration_failure
-!registerForWebSub(ArtName, ArtId) : true <-
  .print("WebSub not available for artifact: ", ArtName).

/* Namespaces */

@namespace_registry_instantiation
+?nsRegistry(RegistryId) : true <-
  makeArtifact("nsRegistry", "org.hyperagents.jacamo.artifacts.hmas.NSRegistryArtifact", [], RegistryId);
  +nsRegistry(RegistryId);
  focus(RegistryId);
  .print("Created namespace registry").

@artifactNamespaceRegistration
+!registerNamespaces(ArtId) : true <-
  ?nsRegistry(RegistryId);
  .findall(Prefix, namespace(Prefix, Namespace), Prefixes);
  !registerNamespaces(Prefixes, ArtId).

@artifact_namespace_registration_end
+!registerNamespaces([], ArtId).

@artifact_namespaces_registration_ongoing
+!registerNamespaces([Prefix | Prefixes], ArtId) : namespace(Prefix, Namespace) <-
  setNamespace(Prefix, Namespace)[artifact_id(ArtId)];
  !registerNamespaces(Prefixes, ArtId).

@namespaceRegistration
+!setNamespace(Prefix, Namespace): true <-
  ?nsRegistry(RegistryId);
  setNamespace(Prefix, Namespace)[artifact_id(RegistryId)].



