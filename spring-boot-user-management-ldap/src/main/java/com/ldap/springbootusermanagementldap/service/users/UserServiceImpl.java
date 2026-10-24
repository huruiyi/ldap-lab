package com.ldap.springbootusermanagementldap.service.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ldap.springbootusermanagementldap.dto.AuthRequest;
import com.ldap.springbootusermanagementldap.dto.AuthResponse;
import com.ldap.springbootusermanagementldap.dto.ChangePasswordRequest;
import com.ldap.springbootusermanagementldap.dto.UserRequestDto;
import com.ldap.springbootusermanagementldap.dto.UserResponseDto;
import com.ldap.springbootusermanagementldap.jwt.JwtTokenUtil;
import com.ldap.springbootusermanagementldap.model.User;
import com.ldap.springbootusermanagementldap.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

  static final String BASE_DN = "dc=springframework,dc=org";
  final UserRepository userRepository;
  final LdapTemplate ldapTemplate;

  @Autowired
  JwtTokenUtil jwtUtil;

  @Override
  public List<UserResponseDto> listUsers() {
    List<UserResponseDto> userResponseDtos = new ArrayList<>();

    userRepository.findAll().forEach(user -> {
      UserResponseDto userResponseDto = UserResponseDto.builder()
          .uid(user.getUsername())
          .fullName(user.getFullName())
          .lastName(user.getLastName())
          .roles(user.getRoles())
          .build();
      userResponseDtos.add(userResponseDto);
    });
    return userResponseDtos;
  }

  @Override
  public User save(UserRequestDto userRequestDto) {
    User user = new User();
    user.setDn(LdapNameBuilder.newInstance("uid=" + userRequestDto.getUid() + ",ou=users").build());
    user.setUsername(userRequestDto.getUid());
    user.setFullName(userRequestDto.getFullName());
    user.setLastName(userRequestDto.getLastName());
    user.setPassword(encodePassword(userRequestDto.getPassword()));
    beforeSave(user);
    return userRepository.save(user);
  }

  @Override
  public User update(String uid, UserRequestDto userRequestDto) {

    User existUser = userRepository.findByUsername(uid);

    existUser.setFullName(userRequestDto.getFullName());
    existUser.setLastName(userRequestDto.getLastName());

    return userRepository.save(existUser);
  }

  @Override
  public User get(String uid) {
    // return userRepository.findById(LdapNameBuilder.newInstance("uid="+uid+",ou=users").build()).get();
    User user = userRepository.findByUsername(uid);

    return user;
  }


  @Override
  public AuthResponse login(AuthRequest authRequest) throws JsonProcessingException {

    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    User user = userRepository.findByUsername(authRequest.getUsername());
    if (user == null) {
      throw new BadCredentialsException("用户名或密码错误");
    }

    String encodedPass = new String(user.getPassword(), StandardCharsets.UTF_8);

    boolean isPasswordMatch = passwordEncoder.matches(authRequest.getPassword(), encodedPass);
    if (!isPasswordMatch) {
      throw new BadCredentialsException("用户名或密码错误");
    }

    // 密码已在上方完成 BCrypt 校验，JWT 无状态架构下无需再走 AuthenticationManager
    // （注入它会造成 UserDetailsService -> AuthenticationManager 的循环依赖）
    String accessToken = jwtUtil.generateAccessToken(user);
    AuthResponse response = new AuthResponse(user.getUsername(), accessToken);
    return response;

  }

  @Override
  public void changePassword(String uid, ChangePasswordRequest request, boolean modifyOwn) {
    User user = userRepository.findByUsername(uid);
    if (user == null) {
      throw new UsernameNotFoundException("用户不存在: " + uid);
    }

    String encodedPass = new String(user.getPassword(), StandardCharsets.UTF_8);
    if (modifyOwn) {
      BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
      if (request.getOldPassword() == null
          || !passwordEncoder.matches(request.getOldPassword(), encodedPass)) {
        throw new BadCredentialsException("旧密码不正确");
      }
    }

    user.setPassword(encodePassword(request.getNewPassword()));
    userRepository.save(user);
  }

  @Override
  public void delete(String uid) {
    userRepository.deleteById(LdapNameBuilder.newInstance("uid=" + uid + ",ou=users").build());
  }

  private String encodePassword(String password) {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    return passwordEncoder.encode(password);
  }

  private void beforeSave(User user) {
    ldapTemplate.create(user);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username);
    if (user == null) {
      throw new BadCredentialsException("用户名或密码错误");
    }

    String encodedPass = new String(user.getPassword(), StandardCharsets.UTF_8);
    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
    // 新建用户可能没有 roles 属性（LDAP 未存 o 字段），需判空
    if (user.getRoles() != null) {
      user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
    }

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(), encodedPass, authorities);
  }

}

