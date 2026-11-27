package hu.boros.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Inventory {
  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("account")) {
      AccountTool.run(java.util.Arrays.copyOfRange(args, 1, args.length));
      return;
    }
    SpringApplication.run(Inventory.class, args);
  }
}
