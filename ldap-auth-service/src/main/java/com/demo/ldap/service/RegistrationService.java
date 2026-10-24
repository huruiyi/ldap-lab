package com.demo.ldap.service;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;

/**
 * Registration service: writes new users into the LDAP directory.
 * - user entry: cn=xxx,ou=users as inetOrgPerson (login searches on cn,
 * so cn must equal the username)
 * - password stored as plain text: the embedded UnboundID server compares
 * bind credentials literally, a {SHA} digest would break login
 * - new users join cn=user,ou=groups so they get ROLE_USER after login
 */
@Service
public class RegistrationService {

  private static final String USERS_BASE = "ou=users";

  private final LdapTemplate ldapTemplate;

  public RegistrationService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  public boolean userExists(String username) {
    String filter = new EqualsFilter("cn", username).encode();
    return !ldapTemplate.search(USERS_BASE, filter,
        (AttributesMapper<String>) attrs -> username).isEmpty();
  }

  public void register(String username, String rawPassword, String firstName, String lastName, String email) {
    if (userExists(username)) {
      throw new IllegalStateException("用户名已存在：" + username);
    }

    // 1. create the user entry (cn/sn are mandatory for person)
    Name dn = LdapNameBuilder.newInstance(USERS_BASE).add("cn", username).build();
    DirContextAdapter context = new DirContextAdapter(dn);
    context.setAttributeValues("objectClass",
        new String[]{"top", "person", "organizationalPerson", "inetOrgPerson"});
    context.setAttributeValue("cn", username);
    context.setAttributeValue("sn", (lastName == null || lastName.isBlank()) ? username : lastName);
    if (firstName != null && !firstName.isBlank()) {
      context.setAttributeValue("givenName", firstName);
    }
    context.setAttributeValue("uid", username);
    if (email != null && !email.isBlank()) {
      context.setAttributeValue("mail", email);
    }
    context.setAttributeValue("userPassword", rawPassword);
    ldapTemplate.bind(context);

    // 2. join the cn=user group (groupOfNames member must be a full DN)
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance("ou=groups").add("cn", "user").build());
    group.addAttributeValue("member", LdapGroupService.userDn(username));
    ldapTemplate.modifyAttributes(group);
  }

}
