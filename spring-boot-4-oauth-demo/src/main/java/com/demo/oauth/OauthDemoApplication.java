package com.demo.oauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OAuth2 demo combining an authorization server and a resource server in one
 * application — the same layout the legacy ldap-auth-service code used, but
 * built on Spring Authorization Server instead of the deprecated
 * spring-security-oauth2 stack.
 */
@SpringBootApplication
@MapperScan("com.demo.oauth.mapper")
public class OauthDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(OauthDemoApplication.class, args);
  }
}
