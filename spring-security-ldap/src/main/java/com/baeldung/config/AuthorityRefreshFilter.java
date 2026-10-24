package com.baeldung.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求级权限刷新过滤器。
 *
 * 表单登录的角色在登录那一刻固化进会话，之后管理员在「权限组管理」里授权/撤权，
 * 在线用户要重新登录才生效——体验差。本过滤器在每个请求上重新查询 LDAP 组关系，
 * 发现角色变化就用最新角色重建 Authentication 放回 SecurityContext，
 * 请求结束时 SecurityContextPersistenceFilter 会把新上下文写回会话。
 *
 * 效果：授权/撤权对在线用户「下一次请求」即生效，无需踢下线。
 * 开销：每请求一次 LDAP 组查询，对内嵌 UnboundID 可忽略；
 * 连远程 LDAP 时可给 loadAuthorities 加 10~30 秒短 TTL 缓存。
 */
@Component
public class AuthorityRefreshFilter extends OncePerRequestFilter {

  private final LdapTemplate ldapTemplate;

  public AuthorityRefreshFilter(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      Set<GrantedAuthority> fresh = loadAuthorities(auth.getName());
      if (!fresh.equals(new HashSet<>(auth.getAuthorities()))) {
        UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
            auth.getPrincipal(), auth.getCredentials(), fresh);
        refreshed.setDetails(auth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
      }
    }
    filterChain.doFilter(request, response);
  }

  /** 与 DefaultLdapAuthoritiesPopulator 的规则一致：组 cn 大写 + ROLE_ 前缀 */
  private Set<GrantedAuthority> loadAuthorities(String username) {
    String memberDn = "uid=" + username + ",ou=people,dc=baeldung,dc=com";
    List<String> groups = ldapTemplate.search("ou=groups",
        "(member=" + LdapEncoder.filterEncode(memberDn) + ")",
        (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get());
    return groups.stream()
        .map(cn -> new SimpleGrantedAuthority("ROLE_" + cn.toUpperCase()))
        .collect(Collectors.toSet());
  }
}
