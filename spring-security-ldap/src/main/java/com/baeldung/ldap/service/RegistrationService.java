package com.baeldung.ldap.service;

import javax.naming.Name;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

/**
 * 注册服务：把新用户写入内嵌 LDAP 目录（参考 ldap-jwt-auth-microservice 的做法）。
 * - 用户条目：uid=xxx,ou=people 下创建 inetOrgPerson
 * - 密码明文存储：UnboundID 内嵌服务器绑定认证时按字面值比较，
 *   存 {SHA} 摘要会导致登录失败（已实测验证）
 * - 同时把用户加入 cn=user,ou=groups 组，登录后才能拿到 ROLE_user 权限
 */
@Service
public class RegistrationService {

  private final LdapTemplate ldapTemplate;

  public RegistrationService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  public boolean userExists(String username) {
    String filter = new EqualsFilter("uid", username).encode();
    return !ldapTemplate.search("ou=people", filter,
        (AttributesMapper<String>) attrs -> username).isEmpty();
  }

  public void register(String username, String rawPassword,
                       String fullName, String lastName, String email) {
    if (userExists(username)) {
      throw new IllegalStateException("用户名已存在：" + username);
    }

    // 1. 创建用户条目（cn/sn 是 person 的必填属性，留空则回退为用户名）
    Name dn = LdapNameBuilder.newInstance("ou=people").add("uid", username).build();
    DirContextAdapter context = new DirContextAdapter(dn);
    context.setAttributeValues("objectClass",
        new String[] {"top", "person", "organizationalPerson", "inetOrgPerson"});
    context.setAttributeValue("uid", username);
    context.setAttributeValue("cn", (fullName == null || fullName.isBlank()) ? username : fullName);
    context.setAttributeValue("sn", (lastName == null || lastName.isBlank()) ? username : lastName);
    if (email != null && !email.isBlank()) {
      context.setAttributeValue("mail", email);
    }
    context.setAttributeValue("userPassword", rawPassword);
    ldapTemplate.bind(context);

    // 2. 加入 cn=user 组（groupOfNames 的 member 必须是完整 DN）
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance("ou=groups").add("cn", "user").build());
    group.addAttributeValue("member",
        "uid=" + username + ",ou=people,dc=baeldung,dc=com");
    ldapTemplate.modifyAttributes(group);
  }

  /** 验证用户当前密码（用 LDAP bind 比对） */
  public boolean verifyPassword(String username, String rawPassword) {
    String filter = new EqualsFilter("uid", username).encode();
    return ldapTemplate.authenticate("ou=people", filter, rawPassword);
  }

  /** 修改密码：更新 LDAP 条目的 userPassword（明文，与内嵌服务器的比对方式一致） */
  public void changePassword(String username, String newRawPassword) {
    DirContextOperations context = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance("ou=people").add("uid", username).build());
    context.setAttributeValue("userPassword", newRawPassword);
    ldapTemplate.modifyAttributes(context);
  }
}
