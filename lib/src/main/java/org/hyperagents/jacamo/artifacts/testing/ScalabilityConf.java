package org.hyperagents.jacamo.artifacts.testing;

import cartago.*;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@ARTIFACT_INFO(
  outports = {
    @OUTPORT(name = "conf-out")
  }
)
public class ScalabilityConf extends Artifact {

  private static final String[] ACTIONS = {
    "Ignite", "Drench", "Gust", "Stabilize", "Charge", "Freeze", "Forge", "Enshroud", "Illuminate", "Disperse"
  };

  private static final String[] BASE_ABILITIES = {
    "Pyromancy", "Hydromancy", "Aeromancy", "Geomancy", "Electromancy",
    "Cryomancy", "Metallurgy", "Shadowcraft", "Lumomancy", "Voidweaving"
  };

  private static final String[] SUPPORTING_ITEMS = {
    "Glyph", "Rune", "Mark", "Sigil", "Emblem", "Seal", "Insignia", "Crest", "Symbol", "Badge"
  };

  private static final Namespace EX_NS = new SimpleNamespace("ex", "https://example.org/");
  private static final String WEB_ID = "https://example.org/env-manager";

  private ResourceProfile.Builder testProfileBuilder;
  private List<Signifier> allSignifiers;

  private String envURL;
  private String workspaceName;
  private String artifactName;
  private int signifierNum;
  private int maxSignifierNum;
  private boolean logTime;

  private final Random random = new Random();

  private static String getCurie(String str, Namespace ns) {
    return str.replace(ns.getName(), ns.getPrefix() + ":");
  }

  public void init(String url, String workspaceName, String artifactName, int signifierNum, int maxSignifierNum, boolean logTime) {
    this.envURL = url;
    this.artifactName = artifactName;
    this.workspaceName = workspaceName;
    this.signifierNum = signifierNum;
    this.maxSignifierNum = maxSignifierNum;
    this.logTime = logTime;

    this.testProfileBuilder = new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
      .Builder()
      .setIRIAsString(this.envURL + "/artifacts/" + this.artifactName + "/#artifact")
      .addSemanticType(EX_NS.getName() + "SpellBook")
      .build())
      .setIRIAsString(this.envURL + "/artifacts/" + this.artifactName + "/");

    this.initAllSignifiers();

    this.updateAgentSituation();
    this.publishEmptyProfile();
    this.updatePublishedProfile(this.getUpdatedProfile());
  }

  @OPERATION
  private void increaseSignifiers() {
    if (this.signifierNum < this.maxSignifierNum) {
      this.signifierNum++;
      this.updateAgentSituation();
      try {
        if (logTime) {
          execLinkedOp("conf-out", "setSignifiersNum", signifierNum);
        }
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
      this.updatePublishedProfile(this.getUpdatedProfile());
    }
  }

  private void publishEmptyProfile() {
    String profileStr = new ResourceProfileGraphWriter(testProfileBuilder.build()).write();
    HttpClient client = new HttpClient();

    try {
      client.start();

      ContentResponse response = client.POST(this.envURL + "/artifacts/")
        .content(new StringContentProvider(profileStr), "text/turtle")
        .header("X-Agent-WebID", WEB_ID)
        .header("Slug", this.artifactName).send();

      if (response.getStatus() != HttpStatus.SC_CREATED) {
        log("Request failed: " + response.getStatus());
      }

      ResourceProfile profile = ResourceProfileGraphReader.readFromURL(this.envURL + "/artifacts/" + this.artifactName + "/");
      this.testProfileBuilder.exposeSignifiers(profile.getExposedSignifiers());

      client.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updatePublishedProfile(ResourceProfile profile) {
    String profileStr = new ResourceProfileGraphWriter(profile).write();
    HttpClient client = new HttpClient();
    try {
      client.start();

      ContentResponse response = client.newRequest(this.envURL + "/artifacts/" + this.artifactName + "/#artifact")
        .method(HttpMethod.PUT)
        .content(new StringContentProvider(profileStr), "text/turtle")
        .header("X-Agent-WebID", WEB_ID)
        .send();

      if (response.getStatus() != HttpStatus.SC_OK) {
        log("Request failed: " + response.getStatus());
      }

      client.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private ResourceProfile getUpdatedProfile() {
    if (signifierNum > 0) {
      return this.testProfileBuilder
        .exposeSignifier(this.allSignifiers.get(signifierNum-1))
        .build();
    }
    return this.testProfileBuilder.build();
  }

  @OPERATION
  public void updateAgentSituation() {
    System.out.println(signifierNum);
    if (signifierNum > 0) {
      List<Signifier> exposedSignifiers = this.allSignifiers.subList(0, signifierNum);
      int index = random.nextInt(exposedSignifiers.size());

      // Return the signifier at the random index
      Signifier randomSignifier = exposedSignifiers.get(index);

      Iterator<Ability> abilitiesIt = randomSignifier.getRecommendedAbilities().iterator();
      Ability ability = abilitiesIt.hasNext() ? abilitiesIt.next() : null;

      Iterator<String> actionTypesIt = randomSignifier.getActionSpecification().getRequiredSemanticTypes().iterator();
      String knownActionType = actionTypesIt.hasNext() ? actionTypesIt.next() : null;

      if (ability != null & knownActionType != null) {

        Object[] prefixedAbilityTypes = ability.getSemanticTypes()
          .stream()
          .filter(type -> !type.equals(INTERACTION.TERM.ABILITY.toString()))
          .map(type -> getCurie(type, EX_NS))
          .collect(Collectors.toSet()).toArray();

        String artifactId = "[artifact_name("
          + this.artifactName + "), wsp("
          + this.workspaceName + ")]";

        String knownPlan = "@test_goal +!test_goal : ability(Ability) " +
          "& signifier([\"" + getCurie(knownActionType, EX_NS) + "\"], [Ability])" +
          "<- invokeAction(\"" + getCurie(knownActionType, EX_NS) + "\")" + artifactId + ". ";

        if (this.getObsProperty("agent_metadata") == null) {
          this.defineObsProperty("agent_metadata", knownPlan, prefixedAbilityTypes[0]);
        } else {
          this.getObsProperty("agent_metadata").updateValues(knownPlan, prefixedAbilityTypes[0]);
        }
      }
    }
  }

  private void initAllSignifiers() {
    this.allSignifiers = new ArrayList<>();

    for (int level = 0; level < this.maxSignifierNum/10 ; level++) {
      for (int j = 0; j < BASE_ABILITIES.length; j++) {
        String action = ACTIONS[j];
        String ability = BASE_ABILITIES[j];

        ActionSpecification spec = new ActionSpecification.Builder(new Form
          .Builder("http://localhost:8000/" + action.toLowerCase() + level)
          .setMethodName("POST")
          .build())
          .addRequiredSemanticType(EX_NS.getName() + action + level)
          .build();

        Signifier sig = new Signifier.Builder(spec)
          .addRecommendedAbility(new Ability
            .Builder()
            .addSemanticType(EX_NS.getName() + ability + level)
            .build())
          .setIRIAsString(this.envURL + "/artifacts/" + this.artifactName + "/#"
            + ACTIONS[j].toLowerCase()
            + level)
          .build();

        this.allSignifiers.add(sig);
      }
    }
  }

}
