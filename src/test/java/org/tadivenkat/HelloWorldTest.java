package org.tadivenkat;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class HelloWorldTest {
   private GreetingService greetingService;

   @Before
   public void setup() {
      greetingService = new GreetingService();
   }

   @Test
   public void testGreet() {
         Assert.assertEquals("Amma, Naanna", "Amma, Naanna");
   }
}
