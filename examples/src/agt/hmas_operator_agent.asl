/* Initial beliefs and rules */

ability("https://purl.org/hmas/HMASAbility").

entry_url("http://172.27.52.55:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

@initialization
+!start : entry_url(Url) <-
  .print("hello world.");
  !setNamespace("ex", "https://example.org/");

  makeArtifact("conf", "org.hyperagents.jacamo.artifacts.testing.ScalabilityConf", [Url, 1], ConfId);

  focus(ConfId);
  .wait(10000);
  increaseSignifiers;
  .wait(5000);
  increaseSignifiers;

  !set_up_web_id(WebId);
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8081], NotifServerId);
  setOperatorWebId(WebId)[artifact_id(NotifServerId)];
  !load_environment(Url, "61").

+setUpDone : true <- stopTimerAndLog(4).

@logger_initialization
+!set_up_logger(LoggerId) : true <-
  makeArtifact("logger", "org.hyperagents.jacamo.artifacts.testing.TimeLogger", [], LoggerId).

@web_id_initialization
+!set_up_web_id(WebId) : .my_name(AgentName) <-
 .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
 +web_id(WebId).

@hypermedia_artifact_instantiation_hmas_custom
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : ability("https://purl.org/hmas/HMASAbility") & web_id(WebId) <-
  makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI], ArtId)[wid(WkspId)];

  // Set logger
  !set_up_logger(LoggerId);
  linkArtifacts(ArtId, "out-1", LoggerId);
  setLogTime(true)[artifact_id(ArtId)];

  !registerNamespaces(ArtId);
  setOperatorWebId(WebId)[artifact_id(ArtId)];
  -+setUpDone.


{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
