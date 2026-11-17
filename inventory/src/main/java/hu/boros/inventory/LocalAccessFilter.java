package hu.boros.inventory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Explicit host boundary: local by default; optional HTTPS origin through a loopback proxy only.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalAccessFilter extends OncePerRequestFilter {
  private final URI publicOrigin;

  public LocalAccessFilter() {
    this("", true, "127.0.0.1");
  }

  @Autowired
  public LocalAccessFilter(
      @Value("${app.security.public-origin:}") String origin,
      @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookie,
      @Value("${server.address:127.0.0.1}") String bind) {
    publicOrigin = origin.isBlank() ? null : URI.create(origin);
    if (publicOrigin != null
        && (!"https".equals(publicOrigin.getScheme())
            || publicOrigin.getHost() == null
            || publicOrigin.getUserInfo() != null
            || publicOrigin.getRawQuery() != null
            || publicOrigin.getRawFragment() != null
            || !Set.of("", "/").contains(publicOrigin.getRawPath())
            || (publicOrigin.getPort() != -1 && publicOrigin.getPort() != 443)
            || !secureCookie
            || !Set.of("localhost", "127.0.0.1", "::1").contains(bind)))
      throw new IllegalArgumentException(
          "Public origin requires HTTPS, secure cookies and loopback binding.");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "same-origin");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
    String path = request.getRequestURI();
    if (path.startsWith("/api/")
        || path.startsWith("/leltar")
        || path.startsWith("/auth/")
        || path.equals("/login")
        || path.equals("/logout")
        || path.equals("/sw.js")) {
      response.setHeader("Cache-Control", "no-store");
    }
    boolean forwarded =
        Collections.list(request.getHeaderNames()).stream()
            .anyMatch(
                name ->
                    name.equalsIgnoreCase("Forwarded")
                        || name.toLowerCase(java.util.Locale.ROOT).startsWith("x-forwarded-"));
    String host = request.getServerName();
    boolean local = Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(host);
    boolean loopback =
        Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(request.getRemoteAddr());
    boolean trustedProxy =
        publicOrigin != null
            && publicOrigin.getHost().equalsIgnoreCase(host)
            && loopback
            && request.getHeader("Forwarded") == null
            && (request.getHeader("X-Forwarded-Proto") == null
                || "https".equals(request.getHeader("X-Forwarded-Proto")))
            && (request.getHeader("X-Forwarded-Host") == null
                || publicOrigin.getHost().equalsIgnoreCase(request.getHeader("X-Forwarded-Host")))
            && (request.getHeader("Host").equalsIgnoreCase(publicOrigin.getHost())
                || request.getHeader("Host").equalsIgnoreCase(publicOrigin.getHost() + ":443"));
    if ((!trustedProxy && (forwarded || !local))
        || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
      response.sendError(403, "Remote access is not configured. Use the local application.");
      return;
    }
    if (trustedProxy && path.startsWith("/actuator")) {
      response.sendError(403, "Health is only available on the local interface.");
      return;
    }
    if (path.startsWith("/api/")
        && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
      String origin = request.getHeader("Origin");
      String expected =
          trustedProxy
              ? "https://" + request.getHeader("Host")
              : request.getScheme() + "://" + request.getHeader("Host");
      if (!"1".equals(request.getHeader("X-Otthontar-Request"))
          || (origin != null && !origin.equals(expected))) {
        response.sendError(403, "Same-origin application request required.");
        return;
      }
    }
    if (trustedProxy) {
      // The configured origin and verified socket peer establish HTTPS, not client-supplied
      // headers.
      chain.doFilter(
          new HttpServletRequestWrapper(request) {
            @Override
            public String getScheme() {
              return "https";
            }

            @Override
            public boolean isSecure() {
              return true;
            }

            @Override
            public int getServerPort() {
              return 443;
            }

            @Override
            public StringBuffer getRequestURL() {
              return new StringBuffer("https://").append(getServerName()).append(getRequestURI());
            }
          },
          response);
    } else chain.doFilter(request, response);
  }
}
