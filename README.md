# Fog-Load-Balancer
A load-balancer for fog-related usecases

## Installation
1. Download code
2. Download [InspectIT Ocelot](https://github.com/inspectIT/inspectit-ocelot/releases "GitHub")

## Running
### Running the worker
When running the [WokerNodeTest](src/test/java/de/unikassel/WorkerNodeTest.java)
You have to specify the location of the InspectIt-jar and of its  
[configuration](config/WorkerNodeConfig.json).

Environment-variables: 

| Name    | Value            |    
|---------|------------------| 
|inspectit|_location of jar_ |
|config   |_config-directory_|


### Running the LoadBalancer
The [LoadBalancerTest](src/test/java/de/unikassel/LoadBalancerTest.java) requires
a running WorkerNodeTest and the password of the WorkerNode.

Environment-variables: 

| Name    | Value            |    
|---------|------------------| 
|password |_the password_    |

## Documentation
The documentation can be found [here](https://marctelllindner.github.io/Fog-Load-Balancer/index.html).