package com.cypcode.ldap_jwt_auth_microservice.repository;

import com.cypcode.ldap_jwt_auth_microservice.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IUserRoleRepository extends JpaRepository<UserRole, Long> {

  List<UserRole> findAllByUserId(Long id);

}
