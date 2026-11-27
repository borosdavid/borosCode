package hu.boros.inventory;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocalAccessFilterTest {
  record Result(boolean allowed, int status, Map<String, String> headers) {}

  private Result request(String method, String path, Map<String, String> requestHeaders)
      throws Exception {
    return request(new LocalAccessFilter(), method, path, requestHeaders);
  }

  private Result request(
      LocalAccessFilter filter, String method, String path, Map<String, String> requestHeaders)
      throws Exception {
    var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headers.put("Host", "127.0.0.1:8740");
    headers.putAll(requestHeaders);
    var responseHeaders = new HashMap<String, String>();
    int[] status = {200};
    boolean[] allowed = {false};
    var request =
        (HttpServletRequest)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, call, args) ->
                    switch (call.getName()) {
                      case "getMethod" -> method;
                      case "getRequestURI" -> path;
                      case "getContextPath" -> path.startsWith("/otthontar/") ? "/otthontar" : "";
                      case "getScheme" -> "http";
                      case "getRemoteAddr" -> headers.getOrDefault("Peer", "127.0.0.1");
                      case "getServerName" -> headers.get("Host").split(":")[0];
                      case "getHeader" -> headers.get((String) args[0]);
                      case "getHeaderNames" -> Collections.enumeration(headers.keySet());
                      default -> throw new UnsupportedOperationException(call.getName());
                    });
    var response =
        (HttpServletResponse)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, call, args) -> {
                  if (call.getName().equals("setHeader"))
                    responseHeaders.put((String) args[0], (String) args[1]);
                  else if (call.getName().equals("sendError")) status[0] = (int) args[0];
                  else throw new UnsupportedOperationException(call.getName());
                  return null;
                });
    filter.doFilterInternal(request, response, (req, res) -> allowed[0] = true);
    return new Result(allowed[0], status[0], responseHeaders);
  }

  @Test
  void permitsLocalReadAndPreventsPrivateHttpCaching() throws Exception {
    var result = request("GET", "/api/photos/id/original", Map.of());
    assertTrue(result.allowed());
    assertEquals("no-store", result.headers().get("Cache-Control"));
    assertEquals("DENY", result.headers().get("X-Frame-Options"));
  }

  @Test
  void enforcesCacheAndWritePoliciesWithAContextPath() throws Exception {
    var get = request("GET", "/otthontar/api/photos/id/original", Map.of());
    assertEquals("no-store", get.headers().get("Cache-Control"));
    assertEquals(403, request("POST", "/otthontar/api/items", Map.of()).status());
    var filter = new LocalAccessFilter("https://boroscode.com", true, "127.0.0.1");
    assertEquals(
        403,
        request(filter, "GET", "/otthontar/actuator/health", Map.of("Host", "boroscode.com"))
            .status());
  }

  @Test
  void deniesRemoteHostsAndForwardedRequests() throws Exception {
    for (var headers :
        List.of(
            Map.of("Host", "home.ts.net"),
            Map.of("Forwarded", "for=100.64.0.1"),
            Map.of("X-Forwarded-For", "100.64.0.1"),
            Map.of("Sec-Fetch-Site", "cross-site"))) {
      assertEquals(403, request("GET", "/leltar", headers).status());
    }
  }

  @Test
  void requiresCustomHeaderAndMatchingOriginForWrites() throws Exception {
    assertEquals(403, request("POST", "/api/items", Map.of()).status());
    assertEquals(
        403,
        request(
                "POST",
                "/api/items",
                Map.of("X-Otthontar-Request", "1", "Origin", "https://other.example"))
            .status());
    assertTrue(
        request(
                "POST",
                "/api/items",
                Map.of("X-Otthontar-Request", "1", "Origin", "http://127.0.0.1:8740"))
            .allowed());
  }

  @Test
  void explicitHttpsHostRequiresLoopbackProxyAndPreservesOriginChecks() throws Exception {
    var filter = new LocalAccessFilter("https://home.ts.net", true, "127.0.0.1");
    var headers =
        Map.of(
            "Host", "home.ts.net", "X-Forwarded-Proto", "https", "X-Forwarded-For", "100.64.0.1");
    assertTrue(request(filter, "GET", "/leltar", headers).allowed());
    assertEquals(403, request(filter, "GET", "/actuator/health", headers).status());
    assertEquals(
        403,
        request(filter, "GET", "/leltar", Map.of("Host", "home.ts.net", "Peer", "192.168.0.2"))
            .status());
    assertEquals(
        403,
        request(
                filter,
                "GET",
                "/leltar",
                Map.of("Host", "home.ts.net", "X-Forwarded-Proto", "http"))
            .status());
    assertEquals(
        403,
        request(filter, "GET", "/leltar", Map.of("Host", "home.ts.net", "Forwarded", "proto=https"))
            .status());
    assertTrue(
        request(
                filter,
                "POST",
                "/api/places",
                Map.of(
                    "Host",
                    "home.ts.net",
                    "X-Otthontar-Request",
                    "1",
                    "Origin",
                    "https://home.ts.net"))
            .allowed());
    assertEquals(
        403,
        request(
                filter,
                "POST",
                "/api/places",
                Map.of(
                    "Host",
                    "home.ts.net",
                    "X-Otthontar-Request",
                    "1",
                    "Origin",
                    "https://other.ts.net"))
            .status());
  }

  @Test
  void allowsOnlyTheExplicitProxyForwardedHostAlongsideThePublicHost() throws Exception {
    var origin = "https://boroscode.com";
    var forwardedHost = "otthontar-origin.boroscode.com";
    var filter = new LocalAccessFilter(origin, true, "127.0.0.1", forwardedHost);
    var headers =
        Map.of(
            "Host",
            "boroscode.com",
            "X-Forwarded-Host",
            forwardedHost,
            "X-Forwarded-Proto",
            "https");
    var path = "/otthontar/login";
    assertTrue(request(filter, "GET", path, headers).allowed());
    assertEquals(
        403,
        request(new LocalAccessFilter(origin, true, "127.0.0.1"), "GET", path, headers).status());

    var mismatched = new HashMap<>(headers);
    mismatched.put("X-Forwarded-Host", forwardedHost + ".example");
    assertEquals(403, request(filter, "GET", path, mismatched).status());
    mismatched.put("X-Forwarded-Host", forwardedHost);
    mismatched.put("Host", forwardedHost);
    assertEquals(403, request(filter, "GET", path, mismatched).status());
    mismatched.put("Host", "boroscode.com");
    mismatched.put("Peer", "192.168.0.2");
    assertEquals(403, request(filter, "GET", path, mismatched).status());
  }

  @Test
  void publicOriginFailsClosedForUnsafeConfiguration() {
    for (String origin :
        List.of(
            "http://home.ts.net",
            "https://home.ts.net/path",
            "https://user@home.ts.net",
            "https://home.ts.net:8443"))
      assertThrows(
          IllegalArgumentException.class, () -> new LocalAccessFilter(origin, true, "127.0.0.1"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LocalAccessFilter("https://home.ts.net", false, "127.0.0.1"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LocalAccessFilter("https://home.ts.net", true, "0.0.0.0"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LocalAccessFilter("", true, "127.0.0.1", "origin.example"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LocalAccessFilter("https://home.ts.net", true, "127.0.0.1", "origin.example:443"));
  }
}
