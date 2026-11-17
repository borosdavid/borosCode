package hu.boros.inventory;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AccountPasswords {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
  }

  static String username(String name) {
    String value = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
    if (!value.matches("[a-z0-9][a-z0-9._-]{2,63}"))
      throw new IllegalArgumentException(
          "A felhasználónév 3–64 ASCII betű, szám, pont, kötőjel vagy aláhúzás lehet.");
    return value;
  }

  static void validatePassword(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "A jelszó legalább 12 karakter és legfeljebb 72 UTF-8 bájt lehet.");
  }
}
