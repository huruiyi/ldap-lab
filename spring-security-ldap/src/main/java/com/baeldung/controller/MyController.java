package com.baeldung.controller;

import com.baeldung.ldap.client.LdapClient;
import com.baeldung.ldap.data.service.UserService;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Spring Controller Definitions.
 */
@Controller
public class MyController {

  @Autowired
  LdapClient ldapClient;

  @Autowired
  UserService userService;

  @RequestMapping("/")
  public String init(Map<String, Object> model, Principal principal) {
    model.put("title", "PUBLIC AREA");
    model.put("message", "Any user can view this page");
    model.put("username", getUserName(principal));
    model.put("userroles", getUserRoles(principal));
    return "home";
  }

  //uid: baeldung
  //userPassword: password
  @RequestMapping("/secure")
  public String secure(Map<String, Object> model, Principal principal) {
    // 演示 LDAP 写操作（注意：LDAP 里没有 user2，直接调用会报错，先 create 再 modify）：
    // ldapClient.create("user2", "user2-pass");
    // userService.modify("user2", "user2-new-pass");
    model.put("title", "SECURE AREA");
    model.put("message", "Only Authorised Users Can See This Page");
    model.put("username", getUserName(principal));
    model.put("userroles", getUserRoles(principal));
    return "home";
  }

  private String getUserName(Principal principal) {
    if (principal == null) {
      return "anonymous";
    } else {
      final UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
      Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
      for (GrantedAuthority grantedAuthority : authorities) {
        System.out.println(grantedAuthority.getAuthority());
      }
      return principal.getName();
    }
  }

  private Collection<String> getUserRoles(Principal principal) {
    if (principal == null) {
      return Arrays.asList("none");
    } else {

      Set<String> roles = new HashSet<String>();

      final UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
      Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
      for (GrantedAuthority grantedAuthority : authorities) {
        roles.add(grantedAuthority.getAuthority());
      }
      return roles;
    }
  }

}
