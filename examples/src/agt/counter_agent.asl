/* Initial beliefs and rules */

entry_url("http://172.27.52.55:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

+!start : entry_url(Url) <-
  .print("hello world.");

  // Create local workspace
  createWorkspace("61");
  !joinWorkspace("61",_);

  // Load hypermedia environment
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8081], _);
  start;
  !load_environment(Url, "61");
  .print("Environment loaded...");

  // Create Counter hypermedia artifact
  .print("Creating counter...");
  invokeAction("makeArtifact",
    ["artifactClass", "artifactName"],
    ["http://example.org/Counter", "c2"]
  )[artifact_name("103")];
  .wait(3000);
  !countTo(3).

+!countTo(0) : true .

+!countTo(X) : true <-
  .print(X, "...");
  lookupArtifact("c2", CounterId);
  invokeAction("http://example.org/Increment", [])[artifact_id(CounterId)];
  .wait(1000);
  !countTo(X - 1).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
