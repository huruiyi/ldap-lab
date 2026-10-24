package com.cypcode.ldap_jwt_auth_microservice.controller;

import com.cypcode.ldap_jwt_auth_microservice.domain.User;
import com.cypcode.ldap_jwt_auth_microservice.domain.dto.AuthenticationRequest;
import com.cypcode.ldap_jwt_auth_microservice.domain.dto.AuthenticationResponse;
import com.cypcode.ldap_jwt_auth_microservice.domain.dto.EntityResponse;
import com.cypcode.ldap_jwt_auth_microservice.domain.dto.UserRegisterRequestDTO;
import com.cypcode.ldap_jwt_auth_microservice.security.JWTTokenUtil;
import com.cypcode.ldap_jwt_auth_microservice.service.WUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("identity")
public class IdentityController {

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  WUserService userService;

  @Autowired
  private JWTTokenUtil jwtTokenUtil;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Autowired
  private LdapTemplate ldapTemplate;

  @PostMapping("/token")
  public ResponseEntity<Object> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest)
      throws Exception {

    try {
      authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
    } catch (Exception e) {
      return EntityResponse.generateResponse("Authentication", HttpStatus.UNAUTHORIZED,
          "Invalid credentials, please check details and try again.");
    }
    final UserDetails userDetails = userService.loadUserByUsername(authenticationRequest.getUsername());
    if (userDetails == null) {
      return EntityResponse.generateResponse("Authentication", HttpStatus.UNAUTHORIZED,
          "Invalid credentials, please check details and try again.");
    }

    final String token = jwtTokenUtil.generateToken(userDetails);
    final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

    return EntityResponse.generateResponse("Authentication", HttpStatus.OK,
        new AuthenticationResponse(token, refreshToken));

  }

  private void authenticate(String username, String password) {
    OrFilter filter = new OrFilter();
    filter.or(new EqualsFilter("uid", username));
    filter.or(new EqualsFilter("cn", username));
    String encoded = filter.encode();
    boolean directoryAccount = !ldapTemplate.search("", encoded, (AttributesMapper<String>) attrs -> username).isEmpty();
    if (directoryAccount) {
      if (!ldapTemplate.authenticate("", encoded, password)) {
        throw new BadCredentialsException("Invalid credentials");
      }
      return;
    }
    User user = userService.findByUsername(username);
    if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }
  }

  @PostMapping("register")
  public ResponseEntity<Object> register(@RequestBody UserRegisterRequestDTO request) {
    return EntityResponse.generateResponse("Regsiter User", HttpStatus.OK, userService.createUser(request));
  }

  @GetMapping("profile")
  public ResponseEntity<Object> retrieveUserProfile() {
    return EntityResponse.generateResponse("User Profile", HttpStatus.OK, userService.findCurrentUserProfile());
  }
}
