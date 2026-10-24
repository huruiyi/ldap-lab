package com.demo.ldap.config;

import com.demo.ldap.filter.AuthorityRefreshFilter;
import com.demo.ldap.security.CustomAuthenticationManager;
import com.demo.ldap.service.LdapGroupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Web security configuration: form-based login backed by LDAP authentication,
 * plus JWT bearer-token validation for API calls. Roles come from LDAP group
 * memberships (ou=groups); /admin/** requires ROLE_ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final CustomAuthenticationManager authenticationManager;
  private final LdapGroupService groupService;

  public SecurityConfig(CustomAuthenticationManager authenticationManager, LdapGroupService groupService) {
    this.authenticationManager = authenticationManager;
    this.groupService = groupService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authenticationManager(authenticationManager)
        .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth/token"))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/login", "/register", "/403", "/css/**", "/error", "/oauth/token").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        // friendly 403 page for browser requests; bare 403 for API clients
        .exceptionHandling(exception -> exception
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                if (request.getRequestURI().startsWith("/api/")) {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                }
                else {
                    if (!response.isCommitted()) {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    }
                    request.getRequestDispatcher("/403").forward(request, response);
                }
            })
            // formLogin and oauth2ResourceServer each register an entry point;
            // the Bearer one would otherwise win and browsers would get a bare
            // 401 instead of being redirected to the login page
            .authenticationEntryPoint((request, response, authException) -> {
                if (request.getRequestURI().startsWith("/api/")) {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                }
                else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
            }))
        // refresh roles from LDAP on every request, so group changes take
        // effect immediately without re-login
        .addFilterAfter(new AuthorityRefreshFilter(groupService), SecurityContextHolderFilter.class)
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/home", true)
            .failureUrl("/login?error")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }

}
