/* Initial beliefs and rules */

vocabulary("https://purl.org/hmas/").

entry_url("http://172.27.52.55:8080/workspaces/61").

env_name("61").

/* Initial goals */

!start.

/* Plans */

@initialization
+!start : entry_url(EnvUrl) & env_name(EnvName)<-
  .print("hello world");

  createWorkspace(EnvName);
  !joinWorkspace(EnvName,_);

  !setNamespace("ex", "https://example.org/");

  // Set logger and logger artifacts
  makeArtifact("logger", "org.hyperagents.jacamo.artifacts.testing.TimeLogger", [0], LoggerId);
  makeArtifact("conf", "org.hyperagents.jacamo.artifacts.testing.ScalabilityConf", [EnvUrl, EnvName, "test", 0, 1000, true], ConfId);
  linkArtifacts(ConfId, "conf-out", LoggerId);
  focus(ConfId);

  // Set WebId
  !set_up_web_id(WebId);

  // Load environment 61
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8081], NotifServerId);
  setOperatorWebId(WebId)[artifact_id(NotifServerId)];
  start;
  !load_environment(EnvUrl, "61").

@testing[atomic]
+exposureState("done")[artifact_name(test)] : true <-
  startTimer;
  !test_goal;
  stopTimerAndLog;
  increaseSignifiers.

@web_id_initialization
+!set_up_web_id(WebId) : .my_name(AgentName) <-
 .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
 +web_id(WebId).

@test_goal
+!test_goal : true <- .print("Initial plan").

{ include("inc/test_agent.asl") }
{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
