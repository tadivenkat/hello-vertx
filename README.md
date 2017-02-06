### A simple web application exposing RESTful API. Demonstrates the usage of vertx Verticle and connecting to db. Built using gradle.

`./gradlew uberjar`

`java -DLOG_LEVEL=info -DROLLOVER_POLICY=org.apache.log4j.DailyRollingFileAppender -jar build/libs/hello-java-vertx.jar`
