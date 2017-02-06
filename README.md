## A simple web application exposing RESTful API. Demonstrates the usage of vertx Verticle and connecting to db. Built using gradle.

### Prerequisites
Make sure you have installed Java 8. There is no other dependency other than having a RDBMS database. Make sure you have the corresponding driver in the lib folder.

`./gradlew uberjar`

`java -DLOG_LEVEL=info -DROLLOVER_POLICY=org.apache.log4j.DailyRollingFileAppender -jar build/libs/hello-java-vertx.jar`

### Supported End Points
GET /message/:id

Once the app is running, invoke the request from a browser or any REST client.
