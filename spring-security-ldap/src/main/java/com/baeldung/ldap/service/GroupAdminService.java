package com.baeldung.ldap.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

/**
 * 权限组管理：读取/修改 ou=groups 下 groupOfNames 组条目的 member 列表。
 * 机制：组的 cn 转大写加 ROLE_ 前缀即角色（cn=admin → ROLE_ADMIN），
 * 「谁是管理员」取决于谁的 DN 在 cn=admin 的 member 里。
 */
@Service
public class GroupAdminService {

  private static final String PEOPLE_BASE = "ou=people";
  private static final String GROUPS_BASE = "ou=groups";

  private final LdapTemplate ldapTemplate;

  public GroupAdminService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  /** 全部用户及其角色（来自组成员关系） */
  public List<Map<String, Object>> listUsersWithRoles() {
    List<String> uids = ldapTemplate.search(PEOPLE_BASE, "(objectClass=inetOrgPerson)",
        (AttributesMapper<String>) attrs -> (String) attrs.get("uid").get());
    Set<String> admins = membersOf("admin");
    Set<String> users = membersOf("user");

    List<Map<String, Object>> result = new ArrayList<>();
    for (String uid : uids) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("username", uid);
      item.put("admin", admins.contains(uid));
      item.put("user", users.contains(uid));
      result.add(item);
    }
    return result;
  }

  /** 授予/收回管理员权限（实时生效，对方下一次请求即变化） */
  public void setAdmin(String username, boolean admin) {
    if ("admin".equals(username)) {
      // 内置 admin 是超级管理员，权限固定，不可授予/收回
      throw new IllegalStateException("内置 admin 账号默认具备最高权限，不允许修改");
    }
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", "admin").build());
    String memberDn = "uid=" + username + "," + PEOPLE_BASE + ",dc=baeldung,dc=com";
    Set<String> admins = membersOf("admin");
    if (admin) {
      if (!admins.contains(username)) {
        group.addAttributeValue("member", memberDn);
      }
    } else {
      // groupOfNames 至少保留一个 member，不能把最后一个管理员移除
      if (admins.size() <= 1) {
        throw new IllegalStateException("至少需要保留一名管理员");
      }
      group.removeAttributeValue("member", memberDn);
    }
    ldapTemplate.modifyAttributes(group);
  }

  /** 读取某组成员的 uid 集合（member 是完整 DN，取 uid RDN 部分） */
  private Set<String> membersOf(String cn) {
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
    Object[] members = group.getObjectAttributes("member");
    Set<String> result = new HashSet<>();
    if (members == null) {
      return result;
    }
    for (Object member : members) {
      try {
        LdapName dn = new LdapName(member.toString());
        result.add(dn.getRdn(dn.size() - 1).getValue().toString());
      } catch (InvalidNameException e) {
        // 忽略解析不了的 member
      }
    }
    return result;
  }
}
