package com.demo.oauth.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Resource server + browser login — the modern replacement for the legacy
 * {@code @EnableResourceServer} / {@code ResourceServerConfigurerAdapter}.
 *
 * Users come from the database (see GroupUserDetailsService); their permission
 * groups are carried in the JWT "groups" claim and mapped back to ROLE_*
 * authorities here, so group-based rules like hasRole('ADMIN') work on APIs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

  @Bean
  @Order(2)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(authorize -> authorize
            // legacy config permitted OPTIONS on the token endpoints
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/login", "/callback", "/error").permitAll()
            .anyRequest().authenticated())
        // stateless API endpoints are called with bearer tokens, not cookies
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .formLogin(form -> form.loginPage("/login").permitAll())
        // GET-based logout so the SPA can kill the SSO session via a plain
        // browser redirect; ?redirect= sends the browser back to the SPA
        // (validated against the known SPA origins)
        .logout(logout -> logout
            .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/logout"))
            .logoutSuccessHandler((request, response, authentication) -> {
                String redirect = request.getParameter("redirect");
                String target = "/login?logout";
                if (redirect != null && SPA_ORIGINS.stream().anyMatch(redirect::startsWith)) {
                    target = redirect;
                }
                response.sendRedirect(target);
            })
            .permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  private static final List<String> SPA_ORIGINS = List.of(
      "http://localhost:5173", "http://127.0.0.1:5173",
      "http://localhost:7100", "http://127.0.0.1:7100");

  /**
   * Builds authorities from two JWT claims: scope -> SCOPE_*, groups -> ROLE_*.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
      List<String> groups = jwt.getClaimAsStringList("groups");
      if (groups != null) {
        groups.forEach(group -> authorities.add(new SimpleGrantedAuthority("ROLE_" + group)));
      }
      return authorities;
    });
    return converter;
  }
}
