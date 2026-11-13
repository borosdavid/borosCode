package hu.boros.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Inventory {
  public static void main(String[] args) {
    SpringApplication.run(Inventory.class, args);
  }
}
