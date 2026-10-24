package com.demo.ldap.filter;

import com.demo.ldap.service.LdapGroupService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-request authority refresh filter.
 * <p>
 * Form-login roles are fixed in the session at login time; after an admin
 * changes group memberships, online users would need to log in again. This
 * filter re-queries LDAP groups on every request and rebuilds the
 * Authentication when roles changed, so grant/revoke takes effect on the
 * user's next request. JWT-authenticated requests are left untouched.
 */
public class AuthorityRefreshFilter extends OncePerRequestFilter {

  private final LdapGroupService groupService;

  public AuthorityRefreshFilter(LdapGroupService groupService) {
    this.groupService = groupService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()
        && !(auth instanceof AnonymousAuthenticationToken)
        && !(auth instanceof JwtAuthenticationToken)) {
      Set<GrantedAuthority> fresh = groupService.loadAuthorities(auth.getName());
      if (!fresh.equals(new HashSet<>(auth.getAuthorities()))) {
        UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
            auth.getPrincipal(), auth.getCredentials(), fresh);
        refreshed.setDetails(auth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
      }
    }
    filterChain.doFilter(request, response);
  }

}
