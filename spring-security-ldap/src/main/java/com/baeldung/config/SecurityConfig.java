package com.baeldung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.ldap.server.UnboundIdContainer;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Security Configuration - LDAP and HTTP Authorizations.
 */
@Configuration
// @ImportResource({ "classpath:webSecurityConfig.xml" }) //=> uncomment to use equivalent xml config
public class SecurityConfig {

    @Bean
    UnboundIdContainer  ldapContainer() throws Exception {
        // LDIF 加载策略：优先读工作目录下的 data/users.ldif（「固化」功能的运行时输出，
        // 重启即生效）；不存在则回退到 classpath 里的初始 users.ldif。
        // 注意：classpath:users.ldif 在 jar 包里是打包时刻的快照，只改源文件不重新打包是不会生效的。
        UnboundIdContainer container = new UnboundIdContainer("dc=baeldung,dc=com", ldifLocation());
        container.setPort(18889);
        return container;
    }

    private String ldifLocation() {
        java.nio.file.Path external = java.nio.file.Paths.get("data", "users.ldif");
        if (java.nio.file.Files.exists(external)) {
            return "file:" + external.toAbsolutePath();
        }
        return "classpath:users.ldif";
    }

    @Bean
    LdapAuthoritiesPopulator authorities(BaseLdapPathContextSource contextSource) {
        String groupSearchBase = "ou=groups";
        DefaultLdapAuthoritiesPopulator authorities = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        authorities.setGroupSearchFilter("(member={0})");
        return authorities;
    }

    @Bean
    AuthenticationManager authenticationManager(BaseLdapPathContextSource contextSource, LdapAuthoritiesPopulator authorities) {
        LdapBindAuthenticationManagerFactory factory = new LdapBindAuthenticationManagerFactory(contextSource);
        factory.setUserSearchBase("ou=people");
        factory.setUserSearchFilter("(uid={0})");
        // 关键：必须把组权限装配进去，否则登录成功后 authorities 为空
        factory.setLdapAuthoritiesPopulator(authorities);
        return factory.createAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthorityRefreshFilter authorityRefreshFilter) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/register", "/css/**")
                .permitAll()
                // 权限组管理仅 ROLE_ADMIN（LDAP cn=admin 组成员）可见
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest()
                .authenticated())
            // 会话恢复后刷新角色：权限组改动对在线用户下一次请求即生效
            .addFilterAfter(authorityRefreshFilter, SecurityContextHolderFilter.class)
            .formLogin(httpSecurityFormLoginConfigurer ->
                httpSecurityFormLoginConfigurer.loginPage("/login").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/")).build();
    }
}
