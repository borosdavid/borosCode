package hu.boros.inventory;

import java.util.Arrays;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local maintenance only: no web endpoint, default password, environment password or shell
 * argument.
 */
public final class AccountTool {
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = AccountEntity.class)
  @EnableJpaRepositories(basePackageClasses = AccountRepository.class)
  @Import(AccountPasswords.class)
  static class DatabaseOnly {}

  static void run(String[] args) {
    if (args.length != 2
        || !Arrays.asList("create", "password", "disable", "enable").contains(args[0]))
      throw new IllegalArgumentException(
          "Használat: account create|password|disable|enable FELHASZNALONEV");
    String username = AccountPasswords.username(args[1]);
    String password = null;
    if (args[0].equals("create") || args[0].equals("password")) {
      var console = System.console();
      if (console == null)
        throw new IllegalStateException(
            "Interaktív terminál szükséges a biztonságos jelszóbevitelhez.");
      char[] first = console.readPassword("Jelszó: ");
      char[] second = console.readPassword("Jelszó újra: ");
      try {
        if (first == null || second == null || !Arrays.equals(first, second))
          throw new IllegalArgumentException("A jelszavak nem egyeznek.");
        password = new String(first);
        AccountPasswords.validatePassword(password);
      } finally {
        if (first != null) Arrays.fill(first, '\0');
        if (second != null) Arrays.fill(second, '\0');
      }
    }
    final String secret = password;
    try (var context =
        new SpringApplicationBuilder(DatabaseOnly.class)
            .web(WebApplicationType.NONE)
            .logStartupInfo(false)
            .run()) {
      var repository = context.getBean(AccountRepository.class);
      var encoder = context.getBean(PasswordEncoder.class);
      new TransactionTemplate(context.getBean(PlatformTransactionManager.class))
          .executeWithoutResult(
              status -> {
                AccountEntity account = repository.findByUsername(username).orElse(null);
                if (args[0].equals("create")) {
                  if (account != null) throw new IllegalArgumentException("A fiók már létezik.");
                  repository.saveAndFlush(new AccountEntity(username, encoder.encode(secret)));
                } else {
                  if (account == null) throw new IllegalArgumentException("Nincs ilyen fiók.");
                  switch (args[0]) {
                    case "password" -> account.passwordHash = encoder.encode(secret);
                    case "disable" -> account.enabled = false;
                    case "enable" -> account.enabled = true;
                    default -> throw new IllegalArgumentException("Ismeretlen művelet.");
                  }
                  repository.saveAndFlush(account);
                }
              });
      System.out.println(
          "A fiókművelet sikeres. A módosított fiók korábbi online munkamenete új belépést igényel.");
    }
  }
}
