package com.ldap.springbootusermanagementldap.service.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ldap.springbootusermanagementldap.dto.AuthRequest;
import com.ldap.springbootusermanagementldap.dto.AuthResponse;
import com.ldap.springbootusermanagementldap.dto.ChangePasswordRequest;
import com.ldap.springbootusermanagementldap.dto.UserRequestDto;
import com.ldap.springbootusermanagementldap.dto.UserResponseDto;
import com.ldap.springbootusermanagementldap.model.User;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {


  List<UserResponseDto> listUsers();

  User save(UserRequestDto userRequestDto);

  User update(String uid, UserRequestDto userRequestDto);

  User get(String uid);

  AuthResponse login(AuthRequest authRequest) throws JsonProcessingException;

  /**
   * 修改密码。modifyOwn=true 时校验旧密码；管理员修改他人密码传 false。
   */
  void changePassword(String uid, ChangePasswordRequest request, boolean modifyOwn);

  void delete(String uid);
}
