package com.demo.ldap.config;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Embedded UnboundID LDAP server (declared explicitly so Spring Boot's
 * EmbeddedLdapAutoConfiguration backs off via @ConditionalOnMissingBean).
 * <p>
 * LDIF loading strategy: prefer data/ldap.ldif in the working directory
 * (the runtime output of the "export / persist" feature, effective on
 * restart); fall back to the classpath ldap.ldif when it does not exist.
 * Note: classpath:ldap.ldif inside a packaged jar is a build-time snapshot —
 * editing the source file without repackaging has no effect.
 */
@Configuration
public class EmbeddedLdapConfig {

  @Bean(destroyMethod = "shutDown")
  public InMemoryDirectoryServer directoryServer(@Value("${ldap.embedded.port:8389}") int port, @Value("${ldap.embedded.admin-dn:cn=admin,dc=example,dc=com}") String adminDn, @Value("${ldap.embedded.admin-password:admin}") String adminPassword) throws Exception {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
    config.setSchema(null); // disable schema validation for LDIF import
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", port));
    config.addAdditionalBindCredentials(adminDn, adminPassword);
    InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);
    server.importFromLDIF(true, ldifLocation());
    server.startListening();
    return server;
  }

  private String ldifLocation() throws Exception {
    Path external = Paths.get("data", "ldap.ldif");
    if (Files.exists(external)) {
      return external.toAbsolutePath().toString();
    }
    return new ClassPathResource("ldap.ldif").getFile().getAbsolutePath();
  }

}
