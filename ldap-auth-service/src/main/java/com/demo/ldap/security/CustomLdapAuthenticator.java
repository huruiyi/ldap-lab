package com.demo.ldap.security;

import com.demo.ldap.exception.CustomException;
import com.demo.ldap.service.LdapGroupService;
import com.demo.ldap.service.LdapUserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Class to provide customised Ldap Authentication, by using LDAP authentication
 * properties. Successful logins are populated with roles from LDAP group
 * memberships (ou=groups).
 */
@Component
public class CustomLdapAuthenticator {
  private static final Logger log = LoggerFactory.getLogger(CustomLdapAuthenticator.class);

  @Autowired
  private LdapUserServiceImpl ldapManager;

  @Autowired
  private LdapGroupService groupService;

  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials() == null ? null
        : authentication.getCredentials().toString();
    log.info("Authentication of user details started for user: {}", username);

    boolean userAttribFetched;
    try {
      userAttribFetched = ldapManager.authenticateUser(username, password);
    } catch (CustomException e) {
      throw new BadCredentialsException(e.getMessage(), e);
    } catch (Exception e) {
      log.error("LDAP authentication failed for user: {}", username, e);
      throw new BadCredentialsException("LDAP authentication failed", e);
    }

    if (!userAttribFetched) {
      throw new CustomException("Not Authorised", HttpStatus.UNAUTHORIZED);
    }
    Set<GrantedAuthority> authorities = groupService.loadAuthorities(username);
    log.info("Authentication done for the user: {} with roles {}", username, authorities);
    return new UsernamePasswordAuthenticationToken(username, null, authorities);
  }

  public boolean supports(Class<?> authentication) {
    return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
  }
}
