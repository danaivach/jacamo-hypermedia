/* Initial beliefs and rules */

vocabulary("https://purl.org/hmas/").
entry_url("http://172.27.52.55:8080/workspaces/61").
web_id("https://wiser-solid-xi.interactions.ics.unisg.ch/agent/profile/card#me").

/* Initial goals */

!start.

/* Plans */

+!start : entry_url(Url) <-
  .print("hello world.");

  // Create local workspace
  createWorkspace("61");
  !joinWorkspace("61",_);

  // Load hypermedia environment
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8082], _);
  start;
  !load_environment(Url, "61");
  .print("Environment loaded...").

+count(X) : true <-
  .print("The count on the remote artifact has changed to: ", X).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
