package com.cypcode.ldap_jwt_auth_microservice.service;

import com.cypcode.ldap_jwt_auth_microservice.domain.Role;
import com.cypcode.ldap_jwt_auth_microservice.domain.User;
import com.cypcode.ldap_jwt_auth_microservice.domain.UserRole;
import com.cypcode.ldap_jwt_auth_microservice.domain.dto.UserRegisterRequestDTO;
import com.cypcode.ldap_jwt_auth_microservice.repository.IUserRepository;
import com.cypcode.ldap_jwt_auth_microservice.repository.IUserRoleRepository;
import com.cypcode.ldap_jwt_auth_microservice.security.SecurityPrincipal;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class WUserService implements UserDetailsService {

  private static final Logger LOG = LoggerFactory.getLogger(WUserService.class);

  @Autowired
  private IUserRepository userRepository;

  @Autowired
  private IUserRoleRepository userRoleRepository;

  @Autowired
  WRoleService roleService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private LdapTemplate ldapTemplate;

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username);
    if (user != null) {
      List<UserRole> userRoles = userRoleRepository.findAllByUserId(user.getId());

      Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

      userRoles.forEach(userRole -> {
        if (userRole.getRole() != null && userRole.getRole().getName() != null) {
          authorities.add(new SimpleGrantedAuthority(userRole.getRole().getName()));
        }
      });

      UserDetails principal = new org.springframework.security.core.userdetails.User(user.getUsername(),
          user.getPassword(), authorities);

      return principal;
    }
    return null;
  }

  public User findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public String createUser(UserRegisterRequestDTO request) {
    try {
      String rawPassword = request.getPassword();
      request.setPassword(passwordEncoder.encode(rawPassword));
      User user = (User) dtoMapperRequestDtoToUser(request);

      user = userRepository.save(user);
      if (!request.getRoleList().isEmpty()) {
        for (String role : request.getRoleList()) {
          Role existingRole = roleService.findRoleByName("ROLE_" + role.toUpperCase());
          if (existingRole != null) {
            addUserRole(user, existingRole);
          }
        }
      } else {
        addUserRole(user, null);
      }
      createLdapUser(request, rawPassword);

      return "User successfully created.";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getCause().getMessage();
    }

  }

  public List<User> retrieveAllUserList() {
    return userRepository.findAll();
  }

  public User updateUser(UserRegisterRequestDTO userRequestDTO) {

    User user = (User) dtoMapperRequestDtoToUser(userRequestDTO);

    user = userRepository.save(user);
    addUserRole(user, null);

    return user;
  }

  public User findCurrentUser() {
    return userRepository.findById(SecurityPrincipal.getInstance().getLoggedInPrincipal().getId()).get();

  }

  public Map<String, Object> findCurrentUserProfile() {
    User user = findCurrentUser();
    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("id", user.getId());
    profile.put("uid", user.getUsername());
    profile.put("sn", "");
    profile.put("givenName", "");
    profile.put("cn", "");
    profile.put("mail", "");
    String filter = new EqualsFilter("uid", user.getUsername()).encode();
    List<Map<String, String>> entries = ldapTemplate.search("", filter, (AttributesMapper<Map<String, String>>) attrs -> {
      Map<String, String> values = new LinkedHashMap<>();
      values.put("sn", attributeText(attrs, "sn"));
      values.put("givenName", attributeText(attrs, "givenName"));
      values.put("cn", attributeText(attrs, "cn"));
      values.put("mail", attributeText(attrs, "mail"));
      return values;
    });
    if (!entries.isEmpty()) {
      profile.putAll(entries.get(0));
    }
    return profile;
  }

  private String attributeText(Attributes attributes, String name) throws NamingException {
    Attribute attribute = attributes.get(name);
    if (attribute == null || attribute.get() == null) {
      return "";
    }
    return attribute.get().toString();
  }

  public List<UserRole> findAllCurrentUserRole() {
    return userRoleRepository.findAllByUserId(SecurityPrincipal.getInstance().getLoggedInPrincipal().getId());

  }

  public Optional<User> findUserById(long id) {
    return userRepository.findById(id);
  }

  public void addUserRole(User user, Role role) {

    UserRole userRole = new UserRole();
    userRole.setUser(user);

    if (role == null) {
      role = roleService.findDefaultRole();
    }

    userRole.setRole(role);
    userRoleRepository.save(userRole);
  }

  private void createLdapUser(UserRegisterRequestDTO request, String rawPassword) {
    String uid = request.getUsername();
    try {
      String filter = new EqualsFilter("uid", uid).encode();
      if (!ldapTemplate.search("", filter, (org.springframework.ldap.core.AttributesMapper<String>) attrs -> uid)
          .isEmpty()) {
        return;
      }
      String surname = request.getLastname();
      String givenName = request.getFirstname();
      String commonName = request.getCn();
      if (commonName == null || commonName.isBlank()) {
        commonName = surname + givenName;
      }
      Name dn = LdapNameBuilder.newInstance("ou=users").add("uid", uid).build();
      DirContextAdapter context = new DirContextAdapter(dn);
      context.setAttributeValues("objectClass",
          new String[] {"top", "person", "organizationalPerson", "inetOrgPerson"});
      context.setAttributeValue("uid", uid);
      context.setAttributeValue("cn", commonName);
      context.setAttributeValue("sn", surname);
      context.setAttributeValue("givenName", givenName);
      context.setAttributeValue("mail", request.getEmail());
      context.setAttributeValue("userPassword", rawPassword);
      ldapTemplate.bind(context);
    } catch (Exception exception) {
      LOG.warn("Could not create LDAP user {}", uid, exception);
    }
  }

  private Object dtoMapperRequestDtoToUser(UserRegisterRequestDTO source) {
    User target = new User();
    target.setEntityNo(source.getEntityNo());
    target.setUsername(source.getUsername());
    target.setPassword(source.getPassword());

    return target;
  }

}
