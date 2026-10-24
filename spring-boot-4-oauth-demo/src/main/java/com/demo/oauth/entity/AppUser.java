package com.demo.oauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("app_users")
public class AppUser {

  @TableId(value = "username", type = IdType.INPUT)
  private String username;

  private String password;

  private String displayName;

  private Boolean enabled;

  public AppUser() {
  }

  public AppUser(String username, String password, String displayName, Boolean enabled) {
    this.username = username;
    this.password = password;
    this.displayName = displayName;
    this.enabled = enabled;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
