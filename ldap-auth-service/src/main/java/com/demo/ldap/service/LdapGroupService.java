package com.demo.ldap.service;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a user's LDAP group memberships into Spring Security roles.
 * Rule: group cn uppercased with a ROLE_ prefix (cn=admin -> ROLE_ADMIN).
 */
@Service
public class LdapGroupService {

  private static final String GROUPS_BASE = "ou=groups";
  private static final String USER_DN_PATTERN = "cn=%s,ou=users,dc=example,dc=com";

  private final LdapTemplate ldapTemplate;

  public LdapGroupService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  public Set<GrantedAuthority> loadAuthorities(String username) {
    String memberDn = String.format(USER_DN_PATTERN, username);
    List<String> groups = ldapTemplate.search(GROUPS_BASE,
        "(member=" + LdapEncoder.filterEncode(memberDn) + ")",
        (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get());
    return groups.stream()
        .map(cn -> new SimpleGrantedAuthority("ROLE_" + cn.toUpperCase()))
        .collect(Collectors.toSet());
  }

  public static String userDn(String username) {
    return String.format(USER_DN_PATTERN, username);
  }

}
