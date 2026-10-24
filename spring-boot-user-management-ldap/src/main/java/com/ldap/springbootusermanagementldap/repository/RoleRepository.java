package com.ldap.springbootusermanagementldap.repository;

import com.ldap.springbootusermanagementldap.model.Role;
import java.util.List;
import org.springframework.data.ldap.repository.LdapRepository;

public interface RoleRepository extends LdapRepository<Role> {

  List<Role> findAllByName(String eRole);

  Role findByName(String eRole);


}
