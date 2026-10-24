package com.demo.oauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Membership link between a user and a permission group. The real primary key
 * is the (group_name, username) pair enforced by the database.
 */
@TableName("group_members")
public class GroupMember {

  @TableId(value = "group_name", type = IdType.INPUT)
  private String groupName;

  private String username;

  public GroupMember() {
  }

  public GroupMember(String groupName, String username) {
    this.groupName = groupName;
    this.username = username;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
