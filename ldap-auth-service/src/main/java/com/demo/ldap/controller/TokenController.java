package com.demo.ldap.controller;

import com.demo.ldap.security.CustomAuthenticationManager;
import com.demo.ldap.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Token endpoint replacing the legacy OAuth2 password grant:
 * POST /oauth/token with grant_type=password, username and password returns
 * a signed JWT access token after LDAP authentication.
 */
@RestController
public class TokenController {

  private final CustomAuthenticationManager authenticationManager;
  private final JwtTokenService tokenService;

  public TokenController(CustomAuthenticationManager authenticationManager, JwtTokenService tokenService) {
    this.authenticationManager = authenticationManager;
    this.tokenService = tokenService;
  }

  @PostMapping("/oauth/token")
  public ResponseEntity<Map<String, Object>> token(@RequestParam("grant_type") String grantType,
                                                   @RequestParam String username, @RequestParam String password) {
    if (!"password".equalsIgnoreCase(grantType)) {
      throw new BadCredentialsException("Unsupported grant_type: " + grantType);
    }
    try {
      Authentication authentication = authenticationManager
          .authenticate(new UsernamePasswordAuthenticationToken(username, password));
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("access_token", tokenService.createToken(authentication));
      body.put("token_type", "bearer");
      body.put("expires_in", tokenService.getExpiresIn());
      return ResponseEntity.ok(body);
    } catch (AuthenticationException e) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("error", "invalid_grant");
      error.put("error_description", "Bad credentials");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
  }

}
