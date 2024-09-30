/* Rules for inferring whether the agent has beliefs in a list */
applies([]) :- true.
applies([Context | RemainingContext]) :- Context & applies(RemainingContext).

/*
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

@environment_loading
 +!load_environment(EnvUrl, EnvName) : true <-
   .print("Loading environment (entry point): ", EnvUrl);

   // Check available vocabulary or set TD by default
   ?vocabulary(Vocabulary);
   .print("Vocabulary: ", Vocabulary);

   // Register the JaCaMo namespace in the NS Registry
   !setNamespace("jacamo", "https://purl.org/hmas/jacamo/");
   !setNamespace("websub", "https://purl.org/hmas/websub/");

   // Handle the entry workspace
   +workspace(EnvUrl, EnvName).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

@workspace_discovery[atomic]
+workspace(WkspIRI, WkspName) : true <-
  .print("Discovered workspace (name: ", WkspName ,"): ", WkspIRI);

 .term2string(WkspNameTerm, WkspName);
  ?joinedWsp(WkspId, WkspNameTerm, _);

  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., to create an artifact).
  !makeMirroringWorkspace(WkspIRI, WkspName, WkspId, WkspArtId);

  // Set WebId
  ?web_id(WebId);
  setOperatorWebId(WebId)[artifact_id(WkspArtId)];

  // Join the hypermedia WorkspaceArtifact
  !joinHypermediaWorkspace(WkspName);

  .print("Created workspace artifact ", WkspName, ", joined, and registered for notifications").

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

@artifact_discovery[atomic]
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : true <-
  //.print("Discovered artifact (name: ", ArtName ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);
  ?joinedWsp(WkspId, WkspNameTerm, WkspName);

  // Create a hypermedia ResourceArtifact for this artifact.
  !makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId);

  // Set WebId
  ?web_id(WebId);
  setOperatorWebId(WebId)[artifact_id(ArtId)];

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
+!makeMirroringWorkspace(WkspIRI, WkspName, WkspId, WkspArtId) : vocabulary("https://purl.org/hmas/") <-
  makeArtifact(WkspName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceResourceArtifact", [WkspIRI], WkspArtId)[wid(WkspId)];
  !registerNamespaces(WkspArtId);
  focus(WkspArtId).

@workspace_instantiation_wot
+!makeMirroringWorkspace(WkspIRI, WkspName, WkspId, WkspArtId) : vocabulary("https://www.w3.org/2019/wot/td#") <-
  makeArtifact(WkspName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceThingArtifact", [WkspIRI], WkspArtId)[wid(WkspId)];
  !registerNamespaces(WkspArtId);
  focus(WkspArtId).

@workspace_joining
+!joinWorkspace(WkspName, WkspId) : true <-
  joinWorkspace(WkspName, WkspId);
  .term2string(WkspNameTerm, WkspName);
  ?joinedWsp(WkspId, WkspNameTerm,_).

@hypermedia_workspace_joining
+!joinHypermediaWorkspace(WkspName) : true <-
  lookupArtifact(WkspName, WkspArtId);
  //!joinHypermediaWorkspace(WkspName, WkspArtId);
  !registerForWebSub(WkspName, WkspArtIdTerm).

@hypermedia_workspace_joining_hmas
+!joinHypermediaWorkspace(WkspName, WkspArtId) : vocabulary("https://purl.org/hmas/") & signifier(["jacamo:JoinWorkspace"]) <-
  invokeAction("jacamo:JoinWorkspace")[artifact_id(WkspArtId)].

@hypermedia_workspace_joining_td
+!joinHypermediaWorkspace(WkspName, WkspArtId) : vocabulary("https://www.w3.org/2019/wot/td#") <-
  joinHypermediaWorkspace[artifact_id(WkspArtId)].

/* Artifacts */

@hypermedia_artifact_instantiation_hmas
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI], ArtId)[wid(WkspId)];
  !registerNamespaces(ArtId);
  focus(ArtId).

@hypermedia_artifact_instantiation_wot
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://www.w3.org/2019/wot/td#") <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact", [ArtIRI], ArtId)[wid(WkspId)];
  !registerNamespaces(ArtId);
  focus(ArtId).

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
  makeArtifact("nsRegistry", "org.hyperagents.jacamo.artifacts.namespaces.NSRegistryArtifact", [], RegistryId);
  +nsRegistry(RegistryId);
  focus(RegistryId);
  .print("Created namespace registry").

@artifact_namespace_registration
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

@namespace_registration
+!setNamespace(Prefix, Namespace): true <-
  ?nsRegistry(RegistryId);
  +nsRegistry(RegistryId);
  setNamespace(Prefix, Namespace)[artifact_id(RegistryId)].

@default_vocabulary_setup
+?vocabulary(Vocabulary) : true <-
  +vocabulary("https://www.w3.org/2019/wot/td#").

@default_webId_setup
+?web_id(WebId) : .my_name(AgentName) <-
  .concat("https://example.org/", AgentName, "/profile/card#me", WebId);
  +web_id(WebId).
