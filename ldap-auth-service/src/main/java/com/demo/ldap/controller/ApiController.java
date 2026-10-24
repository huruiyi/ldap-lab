package com.demo.ldap.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sample JWT-protected API: returns the current user's identity.
 * Works both with a bearer token (JWT) and a form-login session.
 */
@RestController
public class ApiController {

  @GetMapping("/api/me")
  public Map<String, Object> me(Authentication authentication) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("username", authentication.getName());
    body.put("authorities", authentication.getAuthorities());
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      body.put("token_claims", jwtAuthentication.getToken().getClaims());
    }
    return body;
  }

}
