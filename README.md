# Carbon Dioxide CO2 Sensor API

This is an API that collects data from sensors and alerts if the CO2 concentration reach criticxal levels.

# Getting Started

### Build the Project
```
./gradlew clean build
```

### Run the API
```
./gradlew bootRun
```
The command above will run the REST API on port 8080 by default. To change the port,use the following command:
```
SERVER_PORT=<desired_port> ./gradlew bootRun
```

### API Explorer, Specification and Documentation
This project uses OpenAPI Specification (OAS).

Once service is up and running, you can check the OpenAPI spec on this url, [/v3/api-docs](http://localhost:8080/v3/api-docs).

Likewise, API explorer, where you can try the APIs, is available on this url, [/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/sensor-api-controller](http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/sensor-api-controller)


