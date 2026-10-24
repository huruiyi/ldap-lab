package com.demo.ldap.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

/**
 * User administration (admin only): list users with profile attributes and
 * roles, create users, reset passwords, delete users (also removing them
 * from all permission groups).
 */
@Service
public class UserAdminService {

    private static final String USERS_BASE = "ou=users";
    private static final String GROUPS_BASE = "ou=groups";

    private final LdapTemplate ldapTemplate;
    private final RegistrationService registrationService;
    private final GroupAdminService groupAdminService;

    public UserAdminService(LdapTemplate ldapTemplate, RegistrationService registrationService,
            GroupAdminService groupAdminService) {
        this.ldapTemplate = ldapTemplate;
        this.registrationService = registrationService;
        this.groupAdminService = groupAdminService;
    }

    /** All users with profile attributes and admin flag. */
    public List<Map<String, Object>> listUsers() {
        List<Map<String, String>> users = ldapTemplate.search(USERS_BASE,
                "(objectClass=inetOrgPerson)",
                (AttributesMapper<Map<String, String>>) this::entryToMap);
        Set<String> admins = groupAdminService.membersOf("admin");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, String> e : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", e.get("cn"));
            item.put("firstName", e.getOrDefault("givenName", ""));
            item.put("lastName", e.getOrDefault("sn", ""));
            item.put("fullName", e.getOrDefault("sn", "") + e.getOrDefault("givenName", ""));
            item.put("email", e.getOrDefault("mail", ""));
            item.put("admin", admins.contains(e.get("cn")));
            result.add(item);
        }
        return result;
    }

    /**
     * Update a user's profile: username (RDN rename, group memberships follow),
     * givenName, sn and mail. Blank optional attributes are removed.
     */
    public void updateUser(String username, String newUsername, String firstName, String lastName, String email) {
        String newName = (newUsername == null || newUsername.isBlank()) ? username : newUsername.trim();
        javax.naming.ldap.LdapName dn = LdapNameBuilder.newInstance(USERS_BASE).add("cn", username).build();

        if (!newName.equals(username)) {
            if ("admin".equals(username)) {
                throw new IllegalStateException("内置 admin 账号不允许修改用户名");
            }
            if (registrationService.userExists(newName)) {
                throw new IllegalStateException("用户名已存在：" + newName);
            }
            javax.naming.ldap.LdapName newDn = LdapNameBuilder.newInstance(USERS_BASE).add("cn", newName).build();
            ldapTemplate.rename(dn, newDn);
            // groupOfNames member DNs must follow the rename
            String oldMemberDn = LdapGroupService.userDn(username);
            String newMemberDn = LdapGroupService.userDn(newName);
            for (String groupCn : groupAdminService.listGroupNames()) {
                if (groupAdminService.membersOf(groupCn).contains(username)) {
                    DirContextOperations group = ldapTemplate.lookupContext(
                            LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", groupCn).build());
                    group.removeAttributeValue("member", oldMemberDn);
                    group.addAttributeValue("member", newMemberDn);
                    ldapTemplate.modifyAttributes(group);
                }
            }
            dn = newDn;
        }

        DirContextOperations context = ldapTemplate.lookupContext(dn);
        // sn is mandatory for person, fall back to the username
        context.setAttributeValue("sn", (lastName == null || lastName.isBlank()) ? newName : lastName);
        setOrRemove(context, "givenName", blankToNull(firstName));
        setOrRemove(context, "mail", blankToNull(email));
        context.setAttributeValue("uid", newName);
        ldapTemplate.modifyAttributes(context);
    }

    /** Sets the attribute when value is non-null; removes it when null. */
    private static void setOrRemove(DirContextOperations context, String name, String value) {
        if (value == null) {
            if (context.attributeExists(name)) {
                context.removeAttributeValue(name, context.getObjectAttribute(name));
            }
        }
        else {
            context.setAttributeValue(name, value);
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Create a user via the registration service (joins the user group automatically). */
    public void createUser(String username, String password, String firstName, String lastName, String email) {
        registrationService.register(username, password, firstName, lastName, email);
    }

    /** Reset a user's password (plain text, matching the embedded server's bind comparison). */
    public void resetPassword(String username, String newPassword) {
        if ("admin".equals(username)) {
            throw new IllegalStateException("内置 admin 账号的密码不允许在此处重置");
        }
        DirContextOperations context = ldapTemplate.lookupContext(
                LdapNameBuilder.newInstance(USERS_BASE).add("cn", username).build());
        context.setAttributeValue("userPassword", newPassword);
        ldapTemplate.modifyAttributes(context);
    }

    /** Delete a user entry and remove it from every permission group. */
    public void deleteUser(String username) {
        if ("admin".equals(username)) {
            throw new IllegalStateException("内置 admin 账号不允许删除");
        }
        String memberDn = LdapGroupService.userDn(username);
        // remove from all groups first (groupOfNames members must reference existing DNs)
        for (String groupCn : groupAdminService.listGroupNames()) {
            if (groupAdminService.membersOf(groupCn).contains(username)) {
                DirContextOperations group = ldapTemplate.lookupContext(
                        LdapNameBuilder.newInstance(GROUPS_BASE).add("cn", groupCn).build());
                group.removeAttributeValue("member", memberDn);
                ldapTemplate.modifyAttributes(group);
            }
        }
        ldapTemplate.unbind(LdapNameBuilder.newInstance(USERS_BASE).add("cn", username).build());
    }

    private Map<String, String> entryToMap(javax.naming.directory.Attributes attrs) throws NamingException {
        Map<String, String> map = new LinkedHashMap<>();
        NamingEnumeration<? extends Attribute> all = attrs.getAll();
        while (all.hasMore()) {
            Attribute attr = all.next();
            Object value = attr.get();
            map.put(attr.getID(), value instanceof byte[] b
                    ? new String(b, java.nio.charset.StandardCharsets.UTF_8) : String.valueOf(value));
        }
        return map;
    }

}
