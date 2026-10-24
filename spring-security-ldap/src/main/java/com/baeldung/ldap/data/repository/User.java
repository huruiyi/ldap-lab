package com.baeldung.ldap.data.repository;

import javax.naming.Name;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

@Entry(base = "ou=people", objectClasses = {"top", "inetOrgPerson", "person", "organizationalPerson"})
public class User {

  @Id
  private Name id;


  @Attribute(name = "uid")
  private String username;

  @Attribute(name = "cn")
  private String fullName;

  @Attribute(name = "sn")
  private String lastName;


  @Attribute(name = "userPassword")
  private String password;


  public User() {
  }

  public User(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public Name getId() {
    return id;
  }

  public void setId(Name id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return username;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
}
