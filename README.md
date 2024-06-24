# JaCaMo-hypermedia

JaCaMo-hypermedia is a library of [CArtAgO](https://github.com/cartago-lang/cartago) artifacts that
can be used with [JaCaMo](https://github.com/jacamo-lang/jacamo) to program cognitive agents that
operate in hypermedia environments. This library is developed as part of the [HyperAgents project](https://project.hyperagents.org).

List of available artifacts:

- `ThingArtifact`: an artifact that agents can use to interact with Web Things in a [W3C Web of Things](https://www.w3.org/TR/wot-architecture/) environment; this artifact is based on the [WoT-TD-Java](https://github.com/interactions-hsg/wot-td-java) library.

## Getting Started

You can add this library to your JaCaMo application via [JitPack](https://jitpack.io/):

### Step 1: Add the JitPack repository to your build file:

```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

### Step 2: Add a dependency to JaCaMo-hypermedia:

```groovy
implementation 'com.github.HyperAgents:jacamo-hypermedia:main-SNAPSHOT'
```

For the library's version, you can reference any available release tags or the main branch as shown
above. Referencing the main branch will access the latest developments, but a release tag would
typically point to a more stable version.

## Examples, Development, and Testing

This repository includes [examples](examples/) that illustrate how to use the provided artifacts in
your JaCaMo application. In addition, if you wish to contribute to this library, the examples provide
a quick way to dive into development and testing.

### Running the examples

#### Default Example

To run the default example on Mac OS and Linux (use the appropriate Gradle wrapper for your OS):

```
 ./gradlew examples:run
```

To run a specific example, you can specify the corresponding JaCaMo project file from the [examples](examples/)
folder as follows:

```
./gradlew examples:run -PjcmFile=my_jacamo_application.jcm
```

#### WebSub Example

Before the WebSub example can be run, a local Yggdrasil instance must be set up with the configuration shown below.
A tutorial on how to set up a local Yggdrasil can be found [here](https://github.com/Interactions-HSG/yggdrasil-dev-template).

##### Yggdrasil Configuration:

```JSON
{
  "http-config" : {
    "host" : "localhost",
    "port" : 8080,
    "base-uri" : "http://localhost:8080/"
  },
  "notification-config" : {
    "enabled" : true
  },
  "environment-config" : {
    "enabled" : true,
    "known-artifacts" : [
      {
        "class" : "http://example.org/Counter",
        "template" : "ch.unisg.ics.interactions.Counter"
      }
    ],
    "workspaces" : [
      {
        "name" : "61"
      },
      {
        "name" : "102",
        "parent-name" : "61"
      },
      {
        "name" : "103",
        "parent-name" : "61"
      }
    ]
  }
}
```

Once Yggdrasil is running locally change the `WEBID_PREFIX` to `"http://localhost:8080/agents/"` in the [ThingArtifact.java](./lib/src/main/java/org/hyperagents/jacamo/artifacts/wot/ThingArtifact.java).

You can now run the WebSub example on Mac OS and Linux (use the appropriate Gradle wrapper for your OS).

First, run the observer agent:

```
 ./gradlew examples:runWebSubObserver
```

Then run the WebSub example:

```
 ./gradlew examples:runWebSubExample
```

The counter agent will create a `CounterArtifact` in Workspace `103` and start to count to 3.

The observer agent should observe the updates on the `CounterArtifact`.

### Building the library

To build the library on Mac OS and Linux (use the appropriate Gradle wrapper for your OS):

```
./gradlew test build
```
