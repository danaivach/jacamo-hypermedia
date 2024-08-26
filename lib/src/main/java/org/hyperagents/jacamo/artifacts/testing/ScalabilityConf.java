package org.hyperagents.jacamo.artifacts.testing;

import cartago.Artifact;
import cartago.OPERATION;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.util.*;

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

  private static final String[] ROMAN_SUFFIXES = {
    "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"
  };

  private static final String EX_NS = "https://example.org/";
  private static final String WEB_ID = "https://example.org/env-manager";
  private static final String REL_URI = "artifacts/test/";

  private ResourceProfile.Builder testProfileBuilder;
  private List<Signifier> allSignifiers;
  private int signifierNum;
  private String envURL;

  private final Random random = new Random();

  public void init(String url, int signifierNum) {
    this.signifierNum = signifierNum;
    this.envURL = url;
    this.testProfileBuilder = new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
      .Builder()
      .setIRIAsString(this.envURL + "/" + REL_URI + "#artifact")
      .addSemanticType(EX_NS + "SpellBook")
      .build())
      .setIRIAsString(this.envURL + "/" + REL_URI);

    this.initAllSignifiers();

    this.publishEmptyProfile();


    ResourceProfile profile = this.getUpdatedProfile();
    this.updateAgentSituation(profile.getExposedSignifiers());
    this.updatePublishedProfile(profile);
  }

  @OPERATION
  private void increaseSignifiers() {
    this.signifierNum++;
    ResourceProfile profile = this.getUpdatedProfile();
    this.updateAgentSituation(profile.getExposedSignifiers());
    this.updatePublishedProfile(profile);
  }

  private void publishEmptyProfile() {
    String profileStr = new ResourceProfileGraphWriter(testProfileBuilder.build()).write();
    HttpClient client = new HttpClient();

    try {
      client.start();

      ContentResponse response = client.POST(this.envURL + "/artifacts/")
        .content(new StringContentProvider(profileStr), "text/turtle")
        .header("X-Agent-WebID", WEB_ID)
        .header("Slug", "test").send();

      if (response.getStatus() != HttpStatus.SC_CREATED) {
        log("Request failed: " + response.getStatus());
      }

      ResourceProfile profile = ResourceProfileGraphReader.readFromURL(this.envURL + "/" + REL_URI);
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

      ContentResponse response = client.newRequest(this.envURL + "/" + REL_URI + "#artifact")
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

  private void updateAgentSituation(Set<Signifier> exposedSignifiers) {
    List<Signifier> exposedSignifiersLst = new ArrayList<>(exposedSignifiers);
    int index = random.nextInt(exposedSignifiersLst.size());

    // Return the signifier at the random index
    Signifier randomSignifier = exposedSignifiersLst.get(index);

    Iterator<Ability> abilitiesIt = randomSignifier.getRecommendedAbilities().iterator();
    Ability ability = abilitiesIt.hasNext() ? abilitiesIt.next() : null;
    if (ability != null) {
      Set<String> abilityTypes = new HashSet<>(ability.getSemanticTypes());
      abilityTypes.remove(INTERACTION.TERM.ABILITY.toString());

      if (this.getObsProperty("ability") == null) {
        this.defineObsProperty("ability", abilityTypes.toArray());
      } else {
        this.getObsProperty("ability").updateValue(abilityTypes.toArray());
      }
    }

    Iterator<String> actionTypesIt = randomSignifier.getActionSpecification().getRequiredSemanticTypes().iterator();
    String goalActionType = actionTypesIt.hasNext() ? actionTypesIt.next() : null;

    if (goalActionType != null) {
      if (this.getObsProperty("goalActionType") == null) {
        this.defineObsProperty("goalActionType", goalActionType);
      } else {
        this.getObsProperty("goalActionType").updateValue(goalActionType);
      }
    }
  }

  private ResourceProfile getUpdatedProfile() {
    return this.testProfileBuilder
      .exposeSignifier(this.allSignifiers.get(signifierNum-1))
      .build();
  }

  private void initAllSignifiers() {
    this.allSignifiers = new ArrayList<>();

    for (int i = 0; i < ROMAN_SUFFIXES.length; i++) {
      String level = ROMAN_SUFFIXES[i];
      for (int j = 0; j < BASE_ABILITIES.length; j++) {
        String action = ACTIONS[j];
        String ability = BASE_ABILITIES[j];

        ActionSpecification spec = new ActionSpecification.Builder(new Form
          .Builder("http://localhost:8000/" + action.toLowerCase() + level.toLowerCase())
          .setMethodName("POST")
          .build())
          .addRequiredSemanticType(EX_NS + action)
          .build();

        Signifier sig = new Signifier.Builder(spec).addRecommendedAbility(new Ability
            .Builder()
            .addSemanticType(EX_NS + ability + level)
            .build())
          .setIRIAsString(this.envURL + "/" + REL_URI + "#" + ACTIONS[j].toLowerCase() + level.toLowerCase())
          .build();

        this.allSignifiers.add(sig);
      }
    }
  }

}
