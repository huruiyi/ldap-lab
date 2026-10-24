package com.demo.ldap.service;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * LDIF export ("persist"): regenerates the current in-memory directory
 * content (users + group memberships) as ldap.ldif and writes it back, so
 * runtime changes (registered users, group changes) survive a restart.
 */
@Service
public class LdapExportService {

  private static final String ROOT = "dc=example,dc=com";
  /**
   * runtime target: EmbeddedLdapConfig loads from here first on restart
   */
  private static final Path RUNTIME_TARGET = Paths.get("data", "ldap.ldif");
  /**
   * source target: kept with the repo, serves as initial data after mvn clean
   */
  private static final Path SOURCE_TARGET = Paths.get("src/main/resources/ldap.ldif");

  private final LdapTemplate ldapTemplate;

  public LdapExportService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  /**
   * Builds the full LDIF text (ou structure + all users + all groups).
   */
  public String buildLdif() {
    StringBuilder sb = new StringBuilder();
    sb.append("# 本文件由「固化」功能自动生成，可手工编辑，启动时整文件导入内嵌 LDAP\n\n");
    sb.append("dn: ").append(ROOT).append('\n');
    sb.append("objectclass: top\nobjectclass: domain\nobjectclass: extensibleObject\n");
    sb.append("dc: example\n\n");
    ouEntry(sb, "users");
    ouEntry(sb, "groups");

    // user entries: export cn/sn/givenName/uid/mail/userPassword
    List<Map<String, String>> users = ldapTemplate.search("ou=users",
        "(objectClass=inetOrgPerson)",
        (AttributesMapper<Map<String, String>>) this::entryToMap);
    for (Map<String, String> e : users) {
      sb.append("\ndn: cn=").append(e.get("cn")).append(",ou=users,").append(ROOT).append('\n');
      sb.append("objectclass: top\nobjectclass: person\nobjectclass: organizationalPerson\nobjectclass: inetOrgPerson\n");
      appendAttr(sb, "cn", e);
      appendAttr(sb, "sn", e);
      appendAttr(sb, "givenName", e);
      appendAttr(sb, "uid", e);
      appendAttr(sb, "mail", e);
      appendAttr(sb, "userPassword", e);
    }

    // group entries: export all groups with their member lists
    List<String> groupNames = ldapTemplate.search("ou=groups", "(objectClass=groupOfNames)",
        (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get());
    for (String cn : groupNames) {
      DirContextOperations group = ldapTemplate.lookupContext(
          LdapNameBuilder.newInstance("ou=groups").add("cn", cn).build());
      sb.append("\ndn: cn=").append(cn).append(",ou=groups,").append(ROOT).append('\n');
      sb.append("objectclass: top\nobjectclass: groupOfNames\n");
      sb.append("cn: ").append(cn).append('\n');
      Object[] members = group.getObjectAttributes("member");
      if (members != null) {
        for (Object member : members) {
          sb.append("member: ").append(member).append('\n');
        }
      }
    }
    return sb.toString();
  }

  /**
   * Persist: write data/ldap.ldif (effective on restart) and src/main/resources/ldap.ldif (kept in repo).
   */
  public String exportToFile() throws IOException {
    String ldif = buildLdif();
    Files.createDirectories(RUNTIME_TARGET.getParent());
    Files.writeString(RUNTIME_TARGET, ldif, StandardCharsets.UTF_8);
    Files.writeString(SOURCE_TARGET, ldif, StandardCharsets.UTF_8);
    return RUNTIME_TARGET.toAbsolutePath() + " 与 " + SOURCE_TARGET.toAbsolutePath();
  }

  private void ouEntry(StringBuilder sb, String ou) {
    sb.append("dn: ou=").append(ou).append(',').append(ROOT).append('\n');
    sb.append("objectclass: top\nobjectclass: organizationalUnit\n");
    sb.append("ou: ").append(ou).append("\n\n");
  }

  private void appendAttr(StringBuilder sb, String name, Map<String, String> entry) {
    String v = entry.get(name);
    if (v != null) {
      sb.append(name).append(": ").append(v).append('\n');
    }
  }

  private Map<String, String> entryToMap(javax.naming.directory.Attributes attrs) throws NamingException {
    Map<String, String> map = new java.util.LinkedHashMap<>();
    NamingEnumeration<? extends Attribute> all = attrs.getAll();
    while (all.hasMore()) {
      Attribute attr = all.next();
      Object value = attr.get();
      // userPassword may come back as byte[], normalise to string
      map.put(attr.getID(),
          value instanceof byte[] b ? new String(b, StandardCharsets.UTF_8) : String.valueOf(value));
    }
    return map;
  }

}
