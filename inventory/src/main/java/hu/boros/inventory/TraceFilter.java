package hu.boros.inventory;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class TraceFilter extends OncePerRequestFilter {
  private final Logger log = LoggerFactory.getLogger(getClass());

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String id = req.getHeader("X-Request-Id");
    if (id == null || !id.matches("[a-zA-Z0-9-]{1,64}")) id = UUID.randomUUID().toString();
    try (var scope = MDC.putCloseable("requestId", id)) {
      res.setHeader("X-Request-Id", id);
      chain.doFilter(req, res);
      log.info("{} {} {}", req.getMethod(), req.getRequestURI(), res.getStatus());
    }
  }
}
