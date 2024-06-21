/* Initial beliefs and rules */

entry_url("http://localhost:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

+!start : entry_url(Url) <-
  .print("hello world.");
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8082], _);
  start;
  !load_environment("61", Url);
  .print("Environment loaded...").


+count(X) : true <-
  .print("The count on the remote artifact has changed to: ", X).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
