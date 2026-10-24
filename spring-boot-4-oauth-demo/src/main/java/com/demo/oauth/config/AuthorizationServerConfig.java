package com.demo.oauth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
// Spring Security 7 moved the authorization-server configurers out of the SAS
// artifact into spring-security-config under this package
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Authorization server — the modern replacement for the legacy
 * {@code @EnableAuthorizationServer} / {@code AuthorizationServerConfigurerAdapter}
 * setup (spring-security-oauth2, deprecated since Spring Boot 2.6).
 *
 * Mapping from the legacy OauthAuthorisationServer:
 *   JdbcClientDetailsService  -> JdbcRegisteredClientRepository
 *   JdbcTokenStore            -> JdbcOAuth2AuthorizationService (JDBC token storage)
 *   DefaultTokenServices      -> handled by the framework (JWT, refresh tokens on)
 *   password grant            -> removed in OAuth 2.1; use authorization_code instead
 */
@Configuration
public class AuthorizationServerConfig {

  /**
   * Filter chain for the protocol endpoints (/oauth2/token, /oauth2/authorize,
   * /oauth2/jwks, ...). Everything matched here requires an authenticated user;
   * browsers hitting /oauth2/authorize get redirected to the login page.
   */
  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();
    return http
        .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .cors(Customizer.withDefaults())
        .with(authorizationServerConfigurer, Customizer.withDefaults())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .build();
  }

  /**
   * Client registrations stored in JDBC (oauth2_registered_client table), seeded
   * with one demo client on first start.
   */
  @Bean
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate,
      PasswordEncoder passwordEncoder) {
    JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
    if (repository.findByClientId("demo-client") == null) {
      RegisteredClient demoClient = RegisteredClient.withId(UUID.randomUUID().toString())
          .clientId("demo-client")
          .clientSecret(passwordEncoder.encode("demo-secret"))
          .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
          .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
          .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
          .redirectUri("http://127.0.0.1:9000/callback")
          .scope("api.read")
          .scope("api.write")
          .tokenSettings(TokenSettings.builder()
              .accessTokenTimeToLive(Duration.ofHours(1))
              .refreshTokenTimeToLive(Duration.ofDays(1))
              .build())
          .build();
      repository.save(demoClient);
    }
    if (repository.findByClientId("vue-client") == null) {
      // public client for the Vue SPA: no secret, PKCE (S256) is mandatory
      TokenSettings tokenSettings = TokenSettings.builder()
          .accessTokenTimeToLive(Duration.ofHours(1))
          .refreshTokenTimeToLive(Duration.ofDays(1))
          .build();
      RegisteredClient vueClient = RegisteredClient.withId(UUID.randomUUID().toString())
          .clientId("vue-client")
          .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
          // dev server port is 5173 by default; 7100 is the preview port
          .redirectUri("http://localhost:5173/callback")
          .redirectUri("http://127.0.0.1:5173/callback")
          .redirectUri("http://localhost:7100/callback")
          .redirectUri("http://127.0.0.1:7100/callback")
          .postLogoutRedirectUri("http://localhost:5173/")
          .scope("api.read")
          .scope("api.write")
          .clientSettings(ClientSettings.builder()
              .requireProofKey(true)
              .requireAuthorizationConsent(false)
              .build())
          .tokenSettings(tokenSettings)
          .build();
      repository.save(vueClient);
    }
    return repository;
  }

  /**
   * CORS for the Vue dev server: the SPA calls /oauth2/token and /api/** from
   * its own origin, and this bean lets both security chains answer preflight.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:7100", "http://127.0.0.1:7100"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /** JDBC-backed token storage — the counterpart of the legacy JdbcTokenStore. */
  @Bean
  OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
  }

  /**
   * Adds a "groups" claim (e.g. ["ADMIN","USER"]) to JWT access tokens issued
   * to human users; the resource server maps them back to ROLE_* authorities.
   * Client-credentials tokens carry no ROLE_ authorities, so they get none.
   */
  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
      if (!"access_token".equals(context.getTokenType().getValue())) {
        return;
      }
      List<String> groups = context.getPrincipal().getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .filter(authority -> authority.startsWith("ROLE_"))
          .map(authority -> authority.substring(5))
          .sorted()
          .toList();
      if (!groups.isEmpty()) {
        context.getClaims().claim("groups", groups);
      }
    };
  }

  /** SAS 7 no longer auto-configures this; declare the issuer explicitly. */
  @Bean
  AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer("http://localhost:9000")
        .build();
  }

  /**
   * Ephemeral RSA key pair for signing JWT access tokens. Restarting the app
   * invalidates previously issued tokens; persist the key for production use.
   */
  @Bean
  JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsaKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  /** Lets the resource-server side validate the JWTs this server signs. */
  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  private static RSAKey generateRsaKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
          .privateKey((RSAPrivateKey) keyPair.getPrivate())
          .keyID(UUID.randomUUID().toString())
          .build();
    }
    catch (Exception ex) {
      throw new IllegalStateException("failed to generate RSA key pair", ex);
    }
  }
}
