/* Initial beliefs and rules */

ability("https://purl.org/hmas/HMASAbility").

entry_url("http://172.27.52.55:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

@initialization
+!start : entry_url(Url) <-
  .print("hello world.");
  .wait(10000);
  ?web_id(WebId);
  lookupArtifact("notification-server", NotifServerId);
  setOperatorWebId(WebId)[artifact_id(NotifServerId)];
  !load_environment(Url, "61").

@web_id_initialization
+?web_id(WebId) : .my_name(AgentName) <-
 .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
 +web_id(WebId).

@workspace_discovery_handled[atomic]
+workspace(WkspIRI, WkspName) : true <-
  .print("Discovered workspace (name: ", WkspName ,"): ", WkspIRI);

  // Join the CArtAgO Workspace
  !joinWorkspace(WkspName, _);

  // Join the hypermedia WorkspaceArtifact
  !joinHypermediaWorkspace(WkspName);

  .print("Joined workspace artifact ", WkspName, ", and registered for notifications").

@artifact_discovery_custom
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : .my_name(AgentName) <-
  lookupArtifact(ArtName, _);
  .delete("agent", AgentName, AgentSuffix);
  .concat(ArtName, AgentSuffix, ArtNameCustom);

  .print("Discovered artifact (name: ", ArtNameCustom ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);

  ?joinedWsp(WkspId, WkspNameTerm, WkspName);
  !makeMirroringArtifact(ArtIRI, ArtNameCustom, ArtId, WkspId);
  focus(ArtId);
  !registerForWebSub(ArtNameCustom, ArtId);
  .term2string(WkspNameTerm, WkspNameStr);
  ?workspace(WkspIRI, WkspNameStr);
  registerArtifactForFocus(WkspIRI, ArtIRI, ArtId, ArtNameCustom);

  .print("Created artifact ", ArtNameCustom, ", focused on it, and registered for notifications").

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
