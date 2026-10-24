package com.cypcode.ldap_jwt_auth_microservice.repository.ldap;

import com.cypcode.ldap_jwt_auth_microservice.domain.ADPrincipal;
import org.springframework.data.ldap.repository.LdapRepository;


public interface ILDAPPrincipalRepository extends LdapRepository<ADPrincipal> {

  ADPrincipal findByCn(String cn);

  ADPrincipal findByCnAndPassword(String cn, String password);
}
