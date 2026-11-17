package hu.boros.inventory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Local development boundary until a household authentication model is configured. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalAccessFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "same-origin");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
    String path = request.getRequestURI();
    if (path.startsWith("/api/") || path.startsWith("/leltar") || path.equals("/sw.js")) {
      response.setHeader("Cache-Control", "no-store");
    }
    boolean forwarded =
        Collections.list(request.getHeaderNames()).stream()
            .anyMatch(
                name ->
                    name.equalsIgnoreCase("Forwarded")
                        || name.toLowerCase(java.util.Locale.ROOT).startsWith("x-forwarded-"));
    String host = request.getServerName();
    if (forwarded
        || !Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(host)
        || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
      response.sendError(403, "Remote access is not configured. Use the local application.");
      return;
    }
    if (path.startsWith("/api/")
        && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
      String origin = request.getHeader("Origin");
      String expected = request.getScheme() + "://" + request.getHeader("Host");
      if (!"1".equals(request.getHeader("X-Otthontar-Request"))
          || (origin != null && !origin.equals(expected))) {
        response.sendError(403, "Same-origin application request required.");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
