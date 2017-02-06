package org.tadivenkat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreetingService {
   private static final Logger log = LoggerFactory.getLogger(GreetingService.class);
   public static String getMessage(int id) throws Exception {
      return DataSource.getInstance().getValue(id);
   }
}
