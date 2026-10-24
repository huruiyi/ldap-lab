package com.demo.ldap.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for authenticating the user using username and
 * password received as input from user using LDAP authentication.
 */
@Component
public class CustomAuthenticationManager implements AuthenticationManager {

  @Autowired
  private CustomLdapAuthenticator ldapAuthenticator;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return ldapAuthenticator.authenticate(authentication);
  }

}
