package com.demo.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Custom login page for the authorization server's form login (the page the
 * browser lands on during the authorization_code flow).
 */
@Controller
public class LoginController {

  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
