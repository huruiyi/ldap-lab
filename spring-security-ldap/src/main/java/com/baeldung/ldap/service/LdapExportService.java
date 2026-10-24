package com.baeldung.ldap.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

/**
 * LDIF 固化：把内嵌 LDAP（内存态）当前的目录内容重新生成 users.ldif，
 * 覆盖写回 src/main/resources/users.ldif，应用重启后运行期的变更（注册的用户、
 * 权限组调整）就不会丢了。
 */
@Service
public class LdapExportService {

  private static final String ROOT = "dc=baeldung,dc=com";
  /** 运行时固化的位置：SecurityConfig 优先从这里加载，重启即生效 */
  private static final Path RUNTIME_TARGET = Paths.get("data", "users.ldif");
  /** 源码固化位置：随仓库保存，`mvn clean` 后也能作为初始数据 */
  private static final Path SOURCE_TARGET = Paths.get("src/main/resources/users.ldif");

  private final LdapTemplate ldapTemplate;

  public LdapExportService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  /** 生成完整 LDIF 文本（ou 结构 + 全部用户 + 全部组） */
  public String buildLdif() {
    StringBuilder sb = new StringBuilder();
    sb.append("# 本文件由「固化」功能自动生成，可手工编辑，启动时整文件导入内嵌 LDAP\n\n");
    ouEntry(sb, "groups");
    ouEntry(sb, "people");

    // 用户条目：导出 uid/cn/sn/givenName/mail/userPassword
    List<Map<String, String>> people = ldapTemplate.search("ou=people",
        "(objectClass=inetOrgPerson)",
        (AttributesMapper<Map<String, String>>) this::entryToMap);
    for (Map<String, String> e : people) {
      sb.append("\ndn: uid=").append(e.get("uid")).append(",ou=people,").append(ROOT).append('\n');
      sb.append("objectclass: top\nobjectclass: person\nobjectclass: organizationalPerson\nobjectclass: inetOrgPerson\n");
      appendAttr(sb, "cn", e);
      appendAttr(sb, "sn", e);
      appendAttr(sb, "givenName", e);
      appendAttr(sb, "uid", e);
      appendAttr(sb, "mail", e);
      appendAttr(sb, "userPassword", e);
    }

    // 组条目：导出 member 列表
    for (String cn : List.of("admin", "user")) {
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

  /** 固化：同时写 data/users.ldif（重启即生效）和 src/main/resources/users.ldif（随源码保存） */
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
      // userPassword 等属性可能以 byte[] 返回，统一转字符串
      map.put(attr.getID(), value instanceof byte[] b ? new String(b, StandardCharsets.UTF_8) : String.valueOf(value));
    }
    return map;
  }
}
