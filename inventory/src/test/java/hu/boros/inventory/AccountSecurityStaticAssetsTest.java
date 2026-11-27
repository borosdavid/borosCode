package hu.boros.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AccountSecurityStaticAssetsTest {
  private static final String HASH = "05dd3b1888cc3bf8fbb4ec6e31139991";

  private static boolean allowed(String method, String path) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/otthontar" + path);
    request.setContextPath("/otthontar");
    request.setServletPath(path);
    return AccountSecurity.publicVersionedAsset(request);
  }

  @Test
  void allowsFingerprintCssJavaScriptAndManifestBeforeLogin() {
    String[] paths = {
      "/inventory-" + HASH + ".css",
      "/inventory-" + HASH + ".js",
      "/auth-" + HASH + ".js",
      "/photo-input-" + HASH + ".js",
      "/pwa-" + HASH + ".js",
      "/sw-" + HASH + ".js",
      "/manifest-" + HASH + ".webmanifest"
    };
    for (String path : paths) {
      assertTrue(allowed("GET", path), path);
      assertTrue(allowed("HEAD", path), path);
    }
  }

  @Test
  void rejectsNonAssetAndMalformedPaths() {
    String[] paths = {
      "/leltar",
      "/api/items",
      "/admin",
      "/.env",
      "/inventory.css.bak",
      "/inventory-123.css",
      "/inventory-gggggggggggggggggggggggggggggggg.css",
      "/other-" + HASH + ".js",
      "/manifest-" + HASH + ".js",
      "/auth-" + HASH + ".css",
      "/inventory-" + HASH + ".css/secret",
    };
    for (String path : paths) assertFalse(allowed("GET", path), path);
  }

  @Test
  void doesNotOpenWriteMethodsOnVersionedAssets() {
    assertFalse(allowed("POST", "/inventory-" + HASH + ".css"));
    assertFalse(allowed("PUT", "/auth-" + HASH + ".js"));
    assertFalse(allowed("DELETE", "/manifest-" + HASH + ".webmanifest"));
  }
}
