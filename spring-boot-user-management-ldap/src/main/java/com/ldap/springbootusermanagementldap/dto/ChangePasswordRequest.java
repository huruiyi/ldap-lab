package com.ldap.springbootusermanagementldap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

  /** 修改自己的密码时必填；管理员修改他人密码时可为空 */
  private String oldPassword;

  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 64, message = "新密码长度需在 6-64 位之间")
  private String newPassword;
}
