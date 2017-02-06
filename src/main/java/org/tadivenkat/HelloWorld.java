package org.tadivenkat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld extends AbstractVerticle {

   private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

   public static void main(String args[]) {
      launch();
   }

   /**
    ** Launch application with default deployment options
    **/
   public static void launch() {
        Vertx.vertx().deployVerticle(new HelloWorld(), result -> {
            if (result.succeeded()) {
                log.info("Application launched");
            } else {
                log.error("Application launch failed", result.cause());
            }
        });
   }

   public void start(Future<Void> future) {
      Router router = Router.router(vertx);
      router.get("/message/:id").handler(this::getMessage);
      vertx
         .createHttpServer()
         .requestHandler(router::accept)
         .listen(8080, result -> {
            if (result.succeeded()) {
               future.complete();
            } else {
               future.fail(result.cause());
            }
         });
   }

   private void getMessage(RoutingContext routingContext) {
      JsonObject responseObject = new JsonObject();
      try {
         responseObject.put("message", GreetingService.getMessage(Integer.parseInt(routingContext.request().getParam("id"))));
      } catch (Exception exception) {
         log.error("Error retrieving the value from db " + exception.getMessage());
         responseObject.put("message", "This value is not from db");
      }
      routingContext.response()
         .putHeader("content-type", "application/json; charset=utf-8")
         .end(responseObject.encodePrettily());
   }
}
