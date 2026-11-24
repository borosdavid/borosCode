package hu.boros.inventory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class AccountSecurity {
  // Spring's content-versioned resource names must be readable before login.
  // Keep the allowlist restricted to known static file stems and 32-hex fingerprints.
  private static final Pattern PUBLIC_VERSIONED_ASSET =
      Pattern.compile(
          "^/(?:inventory-[0-9a-f]{32}\\.(?:css|js)|(?:auth|photo-input|pwa|sw)-[0-9a-f]{32}\\.js|manifest-[0-9a-f]{32}\\.webmanifest)$");

  static boolean publicVersionedAsset(HttpServletRequest request) {
    return Set.of("GET", "HEAD").contains(request.getMethod())
        && PUBLIC_VERSIONED_ASSET.matcher(request.getServletPath()).matches();
  }

  static boolean api(HttpServletRequest request) {
    String path = LocalAccessFilter.applicationPath(request);
    return path.equals("/api") || path.startsWith("/api/");
  }

  static void unauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response
        .getWriter()
        .write("{\"detail\":\"Jelentkezz be újra. A nyitott piszkozat megmarad.\"}");
  }

  @Bean
  AuthenticationProvider accountAuthenticationProvider(
      AccountRepository accounts, PasswordEncoder encoder) {
    return new AccountProvider(accounts, encoder, new LoginAttempts(Clock.systemUTC()));
  }

  @Bean
  SecurityFilterChain accountFilterChain(
      HttpSecurity http, AccountRepository accounts, AuthenticationProvider provider)
      throws Exception {
    http.authenticationProvider(provider)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/login",
                        "/auth/csrf",
                        "/error",
                        "/offline.html",
                        "/inventory.css",
                        "/inventory.js",
                        "/auth.js",
                        "/photo-input.js",
                        "/pwa.js",
                        "/sw.js",
                        "/manifest.webmanifest",
                        "/icons/**",
                        "/actuator/health")
                    .permitAll()
                    .requestMatchers(AccountSecurity::publicVersionedAsset)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .requestCache(cache -> cache.requestCache(new NullRequestCache()))
        .formLogin(
            form ->
                form.loginPage("/login")
                    .successHandler(
                        (request, response, authentication) -> {
                          if (Objects.toString(request.getHeader("Accept"), "")
                              .contains("application/json")) response.setStatus(204);
                          else response.sendRedirect(request.getContextPath() + "/leltar");
                        })
                    .failureHandler(
                        (request, response, exception) -> {
                          if (Objects.toString(request.getHeader("Accept"), "")
                              .contains("application/json")) unauthorized(response);
                          else response.sendRedirect(request.getContextPath() + "/login?error");
                        })
                    .permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID"))
        .sessionManagement(
            session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          if (api(request)) unauthorized(response);
                          else response.sendRedirect(request.getContextPath() + "/login");
                        })
                    .accessDeniedHandler(
                        (request, response, exception) -> {
                          Authentication auth =
                              SecurityContextHolder.getContext().getAuthentication();
                          if (api(request)
                              && (auth == null || auth instanceof AnonymousAuthenticationToken))
                            unauthorized(response);
                          else {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response
                                .getWriter()
                                .write(
                                    "{\"detail\":\"A biztonsági ellenőrzés sikertelen. Próbáld újra.\"}");
                          }
                        }))
        .addFilterAfter(new AccountSessionFilter(accounts), SecurityContextHolderFilter.class);
    // Spring Security's session-backed CSRF protection stays enabled, including login/logout.
    return http.build();
  }

  static final class LoginUser implements UserDetails, CredentialsContainer, Serializable {
    final String id;
    final String username;
    final long version;
    private String password;

    LoginUser(AccountEntity account) {
      id = account.id;
      username = account.username;
      version = account.version;
      password = account.passwordHash;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public void eraseCredentials() {
      password = null;
    }
  }

  static final class AccountProvider implements AuthenticationProvider {
    final AccountRepository accounts;
    final PasswordEncoder encoder;
    final LoginAttempts attempts;
    final String dummyHash;

    AccountProvider(AccountRepository accounts, PasswordEncoder encoder, LoginAttempts attempts) {
      this.accounts = accounts;
      this.encoder = encoder;
      this.attempts = attempts;
      dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    public Authentication authenticate(Authentication authentication) {
      String name;
      try {
        name = AccountPasswords.username(authentication.getName());
      } catch (IllegalArgumentException ex) {
        name = "!invalid";
      }
      String address =
          authentication.getDetails() instanceof WebAuthenticationDetails details
              ? details.getRemoteAddress()
              : "unknown";
      String accountKey = "user:" + name;
      String ipKey = "ip:" + address;
      if (!attempts.take(accountKey, 5) || !attempts.take(ipKey, 20))
        throw new BadCredentialsException("Sikertelen bejelentkezés.");
      AccountEntity account = accounts.findByUsername(name).orElse(null);
      String password = Objects.toString(authentication.getCredentials(), "");
      boolean valid =
          password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
              && encoder.matches(password, account == null ? dummyHash : account.passwordHash);
      if (!valid || account == null || !account.enabled)
        throw new BadCredentialsException("Sikertelen bejelentkezés.");
      attempts.clear(accountKey);
      return UsernamePasswordAuthenticationToken.authenticated(
          new LoginUser(account), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public boolean supports(Class<?> authentication) {
      return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
  }

  /** Bounded, process-local limit; IP uses the socket peer, never an untrusted forwarded header. */
  static final class LoginAttempts {
    record Window(long expires, int count) {}

    final Map<String, Window> windows = new HashMap<>();
    final Clock clock;

    LoginAttempts(Clock clock) {
      this.clock = clock;
    }

    synchronized boolean take(String key, int limit) {
      long now = clock.millis();
      windows.entrySet().removeIf(e -> e.getValue().expires <= now);
      Window window = windows.get(key);
      if (window == null && windows.size() >= 4096) return false;
      if (window != null && window.count >= limit) return false;
      windows.put(
          key,
          new Window(
              window == null ? now + Duration.ofMinutes(15).toMillis() : window.expires,
              window == null ? 1 : window.count + 1));
      return true;
    }

    synchronized void clear(String key) {
      windows.remove(key);
    }
  }

  static final class AccountSessionFilter extends OncePerRequestFilter {
    final AccountRepository accounts;

    AccountSessionFilter(AccountRepository accounts) {
      this.accounts = accounts;
    }

    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
        AccountEntity current = accounts.findById(user.id).orElse(null);
        if (current == null || !current.enabled || current.version != user.version) {
          HttpSession session = request.getSession(false);
          if (session != null) session.invalidate();
          SecurityContextHolder.clearContext();
          if (api(request)) {
            unauthorized(response);
            return;
          }
        } else if (api(request)
            && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
            && !user.id.equals(request.getHeader("X-Otthontar-User"))) {
          response.setStatus(409);
          response.setContentType("application/json;charset=UTF-8");
          response
              .getWriter()
              .write("{\"detail\":\"A fiók megváltozott. Ne töltsd fel másik fiók piszkozatát.\"}");
          return;
        }
      }
      chain.doFilter(request, response);
    }
  }
}
