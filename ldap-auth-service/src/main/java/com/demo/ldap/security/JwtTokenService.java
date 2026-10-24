package com.demo.ldap.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Issues signed JWT access tokens for successfully LDAP-authenticated users.
 */
@Service
public class JwtTokenService {

  private final JwtEncoder jwtEncoder;
  private final long expiresIn;

  public JwtTokenService(JwtEncoder jwtEncoder, @Value("${jwt.expires-in:3600}") long expiresIn) {
    this.jwtEncoder = jwtEncoder;
    this.expiresIn = expiresIn;
  }

  public String createToken(Authentication authentication) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("ldap-auth-service")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(expiresIn))
        .subject(authentication.getName())
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long getExpiresIn() {
    return expiresIn;
  }

}
