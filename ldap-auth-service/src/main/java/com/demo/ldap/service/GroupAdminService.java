package com.demo.ldap.service;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.util.*;

/**
 * Permission group administration: reads and edits the member list of the
 * groupOfNames entries under ou=groups. Who is an admin is decided by whose
 * DN is a member of cn=admin.
 */
@Service
public class GroupAdminService {

  private static final String USERS_BASE = "ou=users";
  private static final String GROUPS_BASE = "ou=groups";

  private final LdapTemplate ldapTemplate;

  public GroupAdminService(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  /**
   * All users with their roles (derived from group memberships).
   */
  public List<Map<String, Object>> listUsersWithRoles() {
    List<String> users = ldapTemplate.search(USERS_BASE, "(objectClass=inetOrgPerson)",
        (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get());
    Set<String> admins = membersOf("admin");

    List<Map<String, Object>> result = new ArrayList<>();
    for (String username : users) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("username", username);
      item.put("admin", admins.contains(username));
      result.add(item);
    }
    return result;
  }

  /* ---------- 自定义权限组 ---------- */

  /** Names of all groups under ou=groups. */
  public List<String> listGroupNames() {
    return ldapTemplate.search(GROUPS_BASE, "(objectClass=groupOfNames)",
        (AttributesMapper<String>) attrs -> (String) attrs.get("cn").get());
  }

  /** All groups with their cn, role name and member usernames. */
  public List<Map<String, Object>> listGroups() {
    List<String> groupNames = listGroupNames();
    List<Map<String, Object>> result = new ArrayList<>();
    for (String cn : groupNames) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("cn", cn);
      item.put("role", "ROLE_" + cn.toUpperCase());
      item.put("members", new ArrayList<>(membersOf(cn)));
      item.put("builtin", "admin".equals(cn) || "user".equals(cn));
      result.add(item);
    }
    return result;
  }

  /** Create a new group; the creator becomes its first member (groupOfNames requires one). */
  public void createGroup(String cn, String creatorUsername) {
    if (cn == null || !cn.matches("[a-zA-Z0-9_-]{1,32}")) {
      throw new IllegalStateException("组名只能包含字母、数字、中划线、下划线，最长 32 字符");
    }
    if (groupExists(cn)) {
      throw new IllegalStateException("组已存在：" + cn);
    }
    org.springframework.ldap.core.DirContextAdapter context =
        new org.springframework.ldap.core.DirContextAdapter(
            LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
    context.setAttributeValues("objectClass", new String[]{"top", "groupOfNames"});
    context.setAttributeValue("cn", cn);
    context.setAttributeValue("member", LdapGroupService.userDn(creatorUsername));
    ldapTemplate.bind(context);
  }

  /** Delete a custom group; the built-in admin/user groups cannot be deleted. */
  public void deleteGroup(String cn) {
    if ("admin".equals(cn) || "user".equals(cn)) {
      throw new IllegalStateException("内置组 admin / user 不允许删除");
    }
    ldapTemplate.unbind(LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
  }

  /** Add a user to a group. */
  public void addMember(String cn, String username) {
    if (membersOf(cn).contains(username)) {
      throw new IllegalStateException("用户已在组内：" + username);
    }
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
    group.addAttributeValue("member", LdapGroupService.userDn(username));
    ldapTemplate.modifyAttributes(group);
  }

  /** Remove a user from a group; a group must keep at least one member. */
  public void removeMember(String cn, String username) {
    if ("admin".equals(cn) && "admin".equals(username)) {
      throw new IllegalStateException("内置 admin 账号不能移出 admin 组");
    }
    Set<String> members = membersOf(cn);
    if (!members.contains(username)) {
      throw new IllegalStateException("用户不在组内：" + username);
    }
    if (members.size() <= 1) {
      throw new IllegalStateException("组至少需要保留一名成员");
    }
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
    group.removeAttributeValue("member", LdapGroupService.userDn(username));
    ldapTemplate.modifyAttributes(group);
  }

  public boolean groupExists(String cn) {
    try {
      ldapTemplate.lookupContext(LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", cn).build());
      return true;
    } catch (org.springframework.ldap.NamingException e) {
      return false;
    }
  }

  /**
   * Grant or revoke admin role; takes effect on the user's next request.
   */
  public void setAdmin(String username, boolean admin) {
    if ("admin".equals(username)) {
      // built-in admin is the super administrator, its role is fixed
      throw new IllegalStateException("内置 admin 账号默认具备最高权限，不允许修改");
    }
    DirContextOperations group = ldapTemplate.lookupContext(
        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", "admin").build());
    String memberDn = LdapGroupService.userDn(username);
    Set<String> admins = membersOf("admin");
    if (admin) {
      if (!admins.contains(username)) {
        group.addAttributeValue("member", memberDn);
      }
    } else {
      // groupOfNames must keep at least one member
      if (admins.size() <= 1) {
        throw new IllegalStateException("至少需要保留一名管理员");
      }
      group.removeAttributeValue("member", memberDn);
    }
    ldapTemplate.modifyAttributes(group);
  }

  /**
   * Reads the member usernames of a group (member is a full DN, take its first RDN value).
   */
  public Set<String> membersOf(String cn) {
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
        // ignore unparseable members
      }
    }
    return result;
  }

}
