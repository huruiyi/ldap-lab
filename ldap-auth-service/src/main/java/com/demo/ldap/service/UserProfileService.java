package com.demo.ldap.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.stereotype.Service;

/**
 * Reads user profile attributes (givenName / sn / mail) from the LDAP
 * directory for display purposes.
 */
@Service
public class UserProfileService {

    private static final String USERS_BASE = "ou=users";

    private final LdapTemplate ldapTemplate;

    public UserProfileService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    /** Profile attributes of the given user; empty map when not found. */
    public Map<String, String> getProfile(String username) {
        String filter = "(cn=" + LdapEncoder.filterEncode(username) + ")";
        List<Map<String, String>> results = ldapTemplate.search(USERS_BASE, filter,
                (AttributesMapper<Map<String, String>>) this::toProfile);
        return results.isEmpty() ? Map.of() : results.get(0);
    }

    private Map<String, String> toProfile(javax.naming.directory.Attributes attrs) throws NamingException {
        Map<String, String> map = new LinkedHashMap<>();
        NamingEnumeration<? extends Attribute> all = attrs.getAll();
        while (all.hasMore()) {
            Attribute attr = all.next();
            Object value = attr.get();
            if ("givenName".equals(attr.getID()) || "sn".equals(attr.getID()) || "mail".equals(attr.getID())) {
                map.put(attr.getID(), value instanceof byte[] b
                        ? new String(b, java.nio.charset.StandardCharsets.UTF_8) : String.valueOf(value));
            }
        }
        return map;
    }

}
