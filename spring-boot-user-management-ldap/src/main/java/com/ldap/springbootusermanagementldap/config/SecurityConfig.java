package com.ldap.springbootusermanagementldap.config;

import com.ldap.springbootusermanagementldap.jwt.JwtTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 配置（原 WebSecurityConfigurerAdapter 已移除，改为组件式）。
 * 全局 AuthenticationManager 由 UserDetailsService Bean（UserServiceImpl）
 * + PasswordEncoder Bean 自动装配，无需手动 configure。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

  private final Environment environment;

  public SecurityConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenFilter jwtTokenFilter) throws Exception {
    // 白名单属性里的路径含空格，逐个 trim，否则永远匹配不上
    String[] whiteList = environment.getProperty("auth.white-list", String[].class);
    String[] patterns = whiteList == null ? new String[0]
        : Arrays.stream(whiteList).map(String::trim).toArray(String[]::new);

    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(patterns).permitAll()
        .anyRequest().authenticated());
    http.exceptionHandling(e -> e.authenticationEntryPoint(
        (request, response, ex) -> response.sendError(
            HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage())));
    http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
