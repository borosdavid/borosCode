package hu.boros.inventory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(AccountSecurityTest.TestApp.class)
class AccountSecurityTest {
  @Configuration
  @EnableWebMvc
  @Import({AccountSecurity.class, AccountPasswords.class, AccountPages.class, Endpoints.class})
  static class TestApp {
    @Bean
    AccountRepository accounts() {
      return mock(AccountRepository.class);
    }
  }

  @RestController
  static class Endpoints {
    @GetMapping({"/api/items", "/api/photos/id/original", "/api/photos/id/small"})
    List<String> shared() {
      return List.of("közös leltár");
    }

    @PostMapping({"/api/places", "/api/items"})
    Map<String, Boolean> save() {
      return Map.of("saved", true);
    }

    @GetMapping("/leltar")
    String page() {
      return "protected";
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired AccountRepository accounts;
  @Autowired PasswordEncoder encoder;
  MockMvc mvc;
  AccountEntity first;
  AccountEntity second;
  static final String PASSWORD = "Correct-test-password-42";

  @BeforeEach
  void setup() {
    reset(accounts);
    first = new AccountEntity("alice", encoder.encode(PASSWORD));
    second = new AccountEntity("boris", encoder.encode(PASSWORD));
    when(accounts.findByUsername("alice")).thenReturn(Optional.of(first));
    when(accounts.findByUsername("boris")).thenReturn(Optional.of(second));
    when(accounts.findById(first.id)).thenAnswer(call -> Optional.of(first));
    when(accounts.findById(second.id)).thenAnswer(call -> Optional.of(second));
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .addFilters(new LocalAccessFilter())
            .defaultRequest(get("/").header("Host", "localhost").header("X-Otthontar-Request", "1"))
            .build();
  }

  MockHttpSession login(String username) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", username)
                    .param("password", PASSWORD)
                    .accept("application/json"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void protectsApiPhotosAndPages() throws Exception {
    for (String path :
        List.of("/api/items", "/api/photos/id/original", "/api/photos/id/small", "/api/session"))
      mvc.perform(get(path))
          .andExpect(status().isUnauthorized())
          .andExpect(content().contentTypeCompatibleWith("application/json"));
    mvc.perform(get("/leltar")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
    mvc.perform(post("/api/items")).andExpect(status().isUnauthorized());
  }

  @Test
  void bothAccountsHaveIdenticalSharedAccess() throws Exception {
    for (String username : List.of(" ALICE ", "boris")) {
      var session = login(username);
      mvc.perform(get("/api/items").session(session))
          .andExpect(status().isOk())
          .andExpect(content().json("[\"közös leltár\"]"));
      mvc.perform(get("/api/session").session(session))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.token").isNotEmpty());
    }
  }

  @Test
  void rejectsMissingCsrfAndWrongAccountIncludingMultipart() throws Exception {
    var session = login("alice");
    mvc.perform(post("/api/places").session(session).header("X-Otthontar-User", first.id))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/places").session(session).with(csrf()).header("X-Otthontar-User", first.id))
        .andExpect(status().isOk());
    mvc.perform(
            multipart("/api/items")
                .file("files", new byte[] {1, 2})
                .session(session)
                .with(csrf().asHeader())
                .header("X-Otthontar-User", first.id))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/places").session(session).with(csrf()).header("X-Otthontar-User", second.id))
        .andExpect(status().isConflict());
    mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().isFound());
    assertTrue(session.isInvalid());
  }

  @Test
  void disabledOrChangedPasswordRevokesExistingSession() throws Exception {
    var session = login("alice");
    first.enabled = false;
    mvc.perform(get("/api/items").session(session)).andExpect(status().isUnauthorized());
    assertTrue(session.isInvalid());
    first.enabled = true;
    var replacement = login("alice");
    first.version++;
    mvc.perform(get("/api/items").session(replacement)).andExpect(status().isUnauthorized());
    assertTrue(replacement.isInvalid());
  }

  @Test
  void loginRequiresCsrfChangesSessionIdAndErasesPassword() throws Exception {
    mvc.perform(post("/login").param("username", "alice").param("password", PASSWORD))
        .andExpect(status().isForbidden());
    var session = new MockHttpSession();
    String original = session.getId();
    mvc.perform(
            post("/login")
                .session(session)
                .with(
                    request -> {
                      request.setRequestedSessionId(session.getId());
                      request.setRequestedSessionIdValid(true);
                      return request;
                    })
                .with(csrf())
                .param("username", "alice")
                .param("password", PASSWORD))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/leltar"));
    assertNotEquals(original, session.getId());
    var securityContext =
        (org.springframework.security.core.context.SecurityContext)
            session.getAttribute("SPRING_SECURITY_CONTEXT");
    var principal = (AccountSecurity.LoginUser) securityContext.getAuthentication().getPrincipal();
    assertNull(principal.getPassword());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "alice")
                .param("password", "wrong")
                .accept("application/json"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void hashNormalizationAndRateLimitHaveExplicitBounds() {
    assertEquals("alice", AccountPasswords.username(" ALICE "));
    assertThrows(IllegalArgumentException.class, () -> AccountPasswords.username("../admin"));
    assertThrows(IllegalArgumentException.class, () -> AccountPasswords.validatePassword("short"));
    assertNotEquals(PASSWORD, first.passwordHash);
    assertTrue(first.passwordHash.startsWith("{bcrypt}"));
    assertTrue(encoder.matches(PASSWORD, first.passwordHash));
    var attempts = new AccountSecurity.LoginAttempts(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    for (int i = 0; i < 5; i++) assertTrue(attempts.take("user:alice", 5));
    assertFalse(attempts.take("user:alice", 5));
    var expired =
        new AccountSecurity.LoginAttempts(
            Clock.offset(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), Duration.ofMinutes(16)));
    expired.windows.putAll(attempts.windows);
    assertTrue(expired.take("user:alice", 5));
  }
}
