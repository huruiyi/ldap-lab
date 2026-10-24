package com.ldap.springbootusermanagementldap.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ldap.springbootusermanagementldap.dto.AuthRequest;
import com.ldap.springbootusermanagementldap.dto.ChangePasswordRequest;
import com.ldap.springbootusermanagementldap.dto.UserRequestDto;
import com.ldap.springbootusermanagementldap.dto.UserResponseDto;
import com.ldap.springbootusermanagementldap.model.User;
import com.ldap.springbootusermanagementldap.service.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "User")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/versions/1")
public class UserController {

  final UserService userService;

  @Operation(summary = "This Service's goal,Get all Users Info")
  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  @GetMapping("/users")
  public List<UserResponseDto> getAllUsers() {
    return userService.listUsers();
  }


  @Operation(summary = "This Service's goal,Save User in User Organization Unit")
  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CREATOR')")
  @PostMapping("/user")
  public User save(@RequestBody UserRequestDto userRequestDto) {
    return userService.save(userRequestDto);
  }

  @Operation(summary = "This Service's goal,Delete User in User Organization Unit")

  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_DELETER')")
  @DeleteMapping("/user/{username}")
  public void delete(@PathVariable("username") String username) {
    userService.delete(username);
  }

  @Operation(summary = "This Service's goal,Get User Info in User Organization Unit")
  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  @GetMapping("/user/{username}")
  public User get(@PathVariable("username") String username) {
    return userService.get(username);
  }

  @Operation(summary = "This Service's goal,Update User in User Organization Unit")
  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_UPDATER')")
  @PutMapping("/user/{username}")
  public User update(@PathVariable("username") String username, @RequestBody UserRequestDto userRequestDto) {
    return userService.update(username, userRequestDto);
  }

  @PostMapping("/login")
  public ResponseEntity<?> loginPage(@RequestBody @Valid AuthRequest authRequest) {
    try {

      return ResponseEntity.ok(userService.login(authRequest));
    } catch (BadCredentialsException | JsonProcessingException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

  }

  @Operation(summary = "公开注册（页面注册入口，无需登录）")
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody @Valid UserRequestDto userRequestDto) {
    try {
      User saved = userService.save(userRequestDto);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(new UserResponseDto(saved.getUsername(), saved.getFullName(), saved.getLastName(), saved.getRoles()));
    } catch (Exception ex) {
      // 最常见的是 uid 已存在（LDAP EntryAlreadyExists）
      return ResponseEntity.status(HttpStatus.CONFLICT).body("注册失败：用户名可能已被占用");
    }
  }

  @Operation(summary = "修改密码：本人需校验旧密码，管理员可直接重置他人密码")
  @SecurityRequirement(name = "Bearer")
  @PreAuthorize("#username == authentication.name or hasAnyAuthority('ROLE_ADMIN')")
  @PutMapping("/user/{username}/password")
  public ResponseEntity<?> changePassword(@PathVariable("username") String username,
      @RequestBody @Valid ChangePasswordRequest request, Authentication authentication) {
    // 改自己的密码一律校验旧密码；管理员重置他人密码才跳过
    boolean modifyOwn = username.equals(authentication.getName());
    try {
      userService.changePassword(username, request, modifyOwn);
      return ResponseEntity.noContent().build();
    } catch (BadCredentialsException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    } catch (UsernameNotFoundException ex) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
  }
}
