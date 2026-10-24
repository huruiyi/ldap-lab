package com.demo.oauth.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.demo.oauth.entity.GroupMember;
import com.demo.oauth.mapper.GroupMemberMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberService extends ServiceImpl<GroupMemberMapper, GroupMember> {

  /** Group names the given user belongs to. */
  public List<String> groupsOf(String username) {
    return lambdaQuery()
        .eq(GroupMember::getUsername, username)
        .list()
        .stream()
        .map(GroupMember::getGroupName)
        .toList();
  }

  public boolean membershipExists(String groupName, String username) {
    return lambdaQuery()
        .eq(GroupMember::getGroupName, groupName)
        .eq(GroupMember::getUsername, username)
        .exists();
  }
}
