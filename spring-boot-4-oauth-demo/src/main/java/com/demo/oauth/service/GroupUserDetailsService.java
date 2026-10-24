package com.demo.oauth.service;

import com.demo.oauth.entity.AppUser;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users from the database (MyBatis-Plus); authorities come from
 * permission-group membership (group_members -> permission_groups) and are
 * exposed as ROLE_&lt;GROUP&gt; so Spring Security's hasRole() checks work.
 */
@Service
public class GroupUserDetailsService implements UserDetailsService {

  private final AppUserService userService;
  private final GroupMemberService memberService;

  public GroupUserDetailsService(AppUserService userService, GroupMemberService memberService) {
    this.userService = userService;
    this.memberService = memberService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AppUser user = userService.findByUsername(username);
    if (user == null) {
      throw new UsernameNotFoundException("user not found: " + username);
    }
    List<SimpleGrantedAuthority> authorities = memberService.groupsOf(username).stream()
        .map(group -> new SimpleGrantedAuthority("ROLE_" + group))
        .toList();
    return new User(user.getUsername(), user.getPassword(), Boolean.TRUE.equals(user.getEnabled()),
        true, true, true, authorities);
  }
}
