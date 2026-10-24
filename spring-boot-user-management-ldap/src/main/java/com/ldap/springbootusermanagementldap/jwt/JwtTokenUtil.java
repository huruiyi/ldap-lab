package com.ldap.springbootusermanagementldap.jwt;

import com.ldap.springbootusermanagementldap.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 工具（jjwt 0.12.x API：builder/parser 方法与 0.9.x 完全不同）。
 * 密钥需 ≥64 字节（HS512），在 application.properties 的 app.jwt.secret 配置。
 */
@Component
public class JwtTokenUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtil.class);

  @Value("${app.jwt.secret}")
  private String SECRET_KEY;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(String.format("%s", user.getUsername()))
        .issuer("Muho")
        .claim("roles", user.getRoles().stream().collect(Collectors.toList()))
        .issuedAt(new Date())
        .expiration(getCustomExpirationTime())
        .signWith(key())
        .compact();
  }

  public boolean validateAccessToken(String token) {
    try {
      Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
      return true;
    } catch (ExpiredJwtException ex) {
      LOGGER.error("JWT expired: {}", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      LOGGER.error("Token is null, empty or only whitespace: {}", ex.getMessage());
    } catch (MalformedJwtException ex) {
      LOGGER.error("JWT is invalid", ex);
    } catch (UnsupportedJwtException ex) {
      LOGGER.error("JWT is not supported", ex);
    } catch (SignatureException ex) {
      LOGGER.error("Signature validation failed");
    }
    return false;
  }

  public String getSubject(String token) {
    return parseClaims(token).getSubject();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public Date getCustomExpirationTime() {
    Date date = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.MINUTE, 10);
    return c.getTime();
  }
}
