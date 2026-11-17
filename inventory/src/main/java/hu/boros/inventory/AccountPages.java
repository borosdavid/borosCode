package hu.boros.inventory;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountPages {
  @GetMapping("/login")
  String login() {
    return "login";
  }

  @GetMapping("/auth/csrf")
  @ResponseBody
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  @GetMapping("/api/session")
  @ResponseBody
  Map<String, String> session(Authentication auth, CsrfToken token) {
    var user = (AccountSecurity.LoginUser) auth.getPrincipal();
    return Map.of(
        "id",
        user.id,
        "username",
        user.username,
        "headerName",
        token.getHeaderName(),
        "token",
        token.getToken());
  }
}
