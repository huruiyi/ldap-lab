package com.demo.oauth.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected API — only reachable with a valid bearer token, exactly like the
 * legacy resource server demanded.
 */
@RestController
public class ApiController {

  /** Needs any valid token with the api.read scope. */
  @GetMapping("/api/hello")
  @PreAuthorize("hasAuthority('SCOPE_api.read')")
  public Map<String, Object> hello(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", "hello from the protected resource server");
    body.put("subject", jwt.getSubject());
    body.put("scopes", jwt.getClaimAsStringList("scope"));
    body.put("issuedAt", jwt.getIssuedAt());
    body.put("expiresAt", jwt.getExpiresAt());
    return body;
  }

  /** Needs a token carrying the api.write scope. */
  @PostMapping("/api/echo")
  @PreAuthorize("hasAuthority('SCOPE_api.write')")
  public Map<String, Object> echo(@AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) Map<String, Object> payload) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("echo", payload == null ? Map.of() : payload);
    body.put("subject", jwt.getSubject());
    return body;
  }

  /**
   * Needs membership of the ADMIN permission group. The "groups" JWT claim is
   * mapped to ROLE_ADMIN by the resource server's JwtAuthenticationConverter;
   * users in only the USER group get 403 here.
   */
  @GetMapping("/api/admin/stats")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> adminStats(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", "仅管理员组可见");
    body.put("subject", jwt.getSubject());
    body.put("groups", jwt.getClaimAsStringList("groups"));
    return body;
  }
}
