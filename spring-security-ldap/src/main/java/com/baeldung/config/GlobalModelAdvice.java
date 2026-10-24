package com.baeldung.config;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局模型注入：让所有页面的导航栏都能拿到当前用户名和角色，
 * 不必每个 Controller 重复 model.put("username", ...)。
 */
@ControllerAdvice
public class GlobalModelAdvice {

  @ModelAttribute
  public void injectCurrentUser(Model model, Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
      model.addAttribute("username", "anonymous");
      model.addAttribute("userroles", Collections.emptyList());
      return;
    }
    model.addAttribute("username", authentication.getName());
    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
    model.addAttribute("userroles", roles);
  }
}
