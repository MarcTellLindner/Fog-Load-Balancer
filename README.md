# Fog-Load-Balancer
A load-balancer for fog-related usecases

## Installation
1. Download code
2. Download [InspectIT Ocelot](https://github.com/inspectIT/inspectit-ocelot/releases "GitHub")

## Running
### Running the worker
When running the [WokerNode](src/main/java/de/unikassel/WorkerNode.java)
you have to use specify InspectIT Ocelot as a Java-agent and specify its
[configuration](config/WorkerNodeConfig.json).

JVM-arguments: `-Dinspectit.config.file-based.path="config/" -javaagent:"path/to/inspectit-ocelot-agent.jar"`

### Running the LoadBalancer
The [LoadBalancer](src/main/java/de/unikassel/LoadBalancer.java) does not require any configuration.

## Documentation
The documentation can be found [here](https://marctelllindner.github.io/Fog-Load-Balancer/index.html).