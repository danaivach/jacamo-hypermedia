/* Initial beliefs and rules */

agent_n(1).

ability("https://purl.org/hmas/HMASAbility").

entry_url("http://172.27.52.55:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

@initialization
+!start : entry_url(Url) <-
  .print("hello world.");
  ?web_id(WebId);
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8081], NotifServerId);
  setOperatorWebId(WebId)[artifact_id(NotifServerId)];
  !load_environment(Url, "61");
  .wait(5000);
  !setup_agents.

@web_id_initialization
+?web_id(WebId) : .my_name(AgentName) <-
 .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
 +web_id(WebId).

@agents_setup
+!setup_agents : agent_n(N) <-
  for (.range(AgentSuffix, 0, N-1)) {
    .concat("agent", AgentSuffix, AgentName);
    .create_agent(AgentName,"hmas_operator_collocated_agent.asl")
  }.

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
