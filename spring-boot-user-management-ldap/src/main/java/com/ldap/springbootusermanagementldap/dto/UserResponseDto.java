package com.ldap.springbootusermanagementldap.dto;

import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class UserResponseDto {


  String uid;

  String fullName;

  String lastName;

  /** 用户当前持有的角色（LDAP o 属性，多值），可能为 null */
  Set<String> roles;

}
