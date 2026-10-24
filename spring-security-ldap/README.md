# Spring Security LDAP Demo

基于 Spring Boot 3 + Spring Security 6 + 内嵌 LDAP（UnboundID）的表单登录示例，在原 Baeldung 教程模块基础上做了独立化改造和功能扩展。

## 功能

- **首页** `/` — 公开访问，显示当前用户与角色
- **登录** `/login` — LDAP bind 认证（`ou=people` 下按 `uid` 搜索）
- **注册** `/register` — 新用户写入 LDAP 目录（inetOrgPerson 条目），并自动加入 `cn=user` 组
- **受保护页** `/secure` — 登录后可见，展示角色（来自 LDAP `ou=groups` 组映射）
- **修改密码** `/password` — 验证旧密码后更新 LDAP 条目（登录后可见）
- **权限组管理** `/admin/groups` — 仅 ROLE_ADMIN 可见：查看所有用户的组成员关系，授予/收回管理员权限（LDAP modifyAttributes 实时生效）
- **权限实时刷新** — `AuthorityRefreshFilter` 在每个请求上重新查询 LDAP 组关系，角色变化即重建 `Authentication`：授权/撤权对在线用户**下一次请求即生效，无需重新登录、不踢下线**
- **固化到 LDIF** — 管理页一键把当前目录内容（用户 + 组成员关系）导出为 LDIF，写入 `data/users.ldif`（重启即生效）和 `src/main/resources/users.ldif`（随源码保存，`mvn clean` 后仍是初始数据）

内置账号（当前 `src/main/resources/users.ldif` 固化状态）：

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `adminpass` | ROLE_ADMIN | 内置超级管理员，权限固定不可收回；同时是 LDAP 写入（注册/改密/授权）的绑定账号 |
| `baeldung` | `password` | ROLE_USER | 演示用户（cn=user 组），可用于演示「授权 → 立即生效」 |
| `persist1` | `keep1234` | ROLE_ADMIN、ROLE_USER | LDIF 固化持久化演示账号 |
| `persist2` | `keep5678` | ROLE_ADMIN、ROLE_USER | 同上 |

> **数据持久化**：内嵌 LDAP 是内存态。启动时优先加载工作目录下 `data/users.ldif`（「固化」功能的输出），不存在则回退到 classpath 里的 `users.ldif`。注意 classpath 资源是打包时刻的快照——只改 `src/main/resources/users.ldif` 而不重新打包，重启不会生效；运行时变更请用管理页的「固化」按钮。

## 快速开始

```bash
# 需要 JDK 17+（本项目实测在 JDK 25 上运行）
mvn spring-boot:run
# 或
mvn package -DskipTests && java -jar target/spring-security-ldap.jar
```

启动后访问 <http://localhost:8080/>（端口 8080）。

## 架构

```
浏览器 ──表单登录──> Spring Security (LdapBindAuthenticationManagerFactory)
                          │  bind 认证 + 组权限装配 (DefaultLdapAuthoritiesPopulator)
                          ▼
                   内嵌 UnboundID LDAP (端口 18889)
                   root: dc=baeldung,dc=com
                   ├── ou=people   用户条目 (uid=...)
                   └── ou=groups   组条目 (groupOfNames, member → ROLE_xxx)
```

关键配置：

- `SecurityConfig` — `UnboundIdContainer`（端口 18889，启动加载 `users.ldif`）、URL 授权、`/login` 表单登录、组权限装配、`AuthorityRefreshFilter` 挂载（`SecurityContextHolderFilter` 之后）
- `config/AuthorityRefreshFilter` — 请求级权限刷新：每请求重查 LDAP 组关系，角色变化即重建 `Authentication` 放回 `SecurityContext`（请求结束自动写回会话）；连远程 LDAP 时可给 `loadAuthorities` 加 10~30 秒 Caffeine 短缓存
- `ldap/javaconfig/AppConfig` — `LdapContextSource`（管理账号绑定，提供写权限）、`LdapTemplate`
- `ldap/service/RegistrationService` — 注册（`DirContextAdapter` + `bind`）、验证/修改密码（`authenticate` / `modifyAttributes`）
- `ldap/service/GroupAdminService` — 权限组管理：读取/修改 `ou=groups` 组条目的 member 列表，内置 `admin` 账号权限固定不可修改
- `ldap/service/LdapExportService` — 固化：把内存目录重新生成 LDIF 写回文件

## 与原教程的差异（踩坑记录）

1. **parent POM**：原模块依赖 Baeldung 私有 `parent-boot-3`，已换成官方 `spring-boot-starter-parent 3.4.5`，war → jar。
2. **移除 ApacheDS**：`apacheds-server-jndi 1.5.5` 在 JDK 17+ 跑不起来，且实际生效的是 UnboundID 方案；同时删除了 `org.apache.directory...ArrayMarshaller` 补丁类。
3. **组权限装配**：原代码里 `LdapAuthoritiesPopulator` Bean 注入了但没接进认证工厂（登录后无角色），已补 `factory.setLdapAuthoritiesPopulator(authorities)`。
4. **密码明文存储**：UnboundID 内嵌服务器 bind 时按字面值比对密码，存 `{SHA}` 摘要会登录失败——注册用户必须存明文（生产环境请换外部 LDAP 并启用密码策略）。
5. **匿名绑定没有写权限**：注册/改密需要管理账号绑定（`uid=admin` 条目）。
6. `/secure` 原代码里的 `userService.modify("user2", ...)` 是遗留实验代码（user2 不存在会报错），已注释并附正确用法。
7. **classpath 资源是打包快照**：`classpath:users.ldif` 打进 jar 后，改源文件不重新打包不会生效——所以「固化」功能会同时写 `data/users.ldif`（启动优先加载）和源码文件。
8. **会话角色固化**：表单登录的角色在登录那一刻写进会话，之后权限组改动要等对方重新登录才生效。传统解法是踢下线（`SessionRegistry` 失效会话），但体验差；本项目改用 `AuthorityRefreshFilter` 请求级刷新，授权/撤权对在线用户立即生效且会话不受干扰（实测：授权后原会话 403 → 200，撤权后 200 → 403，普通页面访问不受影响）。

## 参考

- [Intro to Spring Security LDAP](https://www.baeldung.com/spring-security-ldap)
- [Spring LDAP Overview](https://www.baeldung.com/spring-ldap)
- [Guide to Spring Data LDAP](https://www.baeldung.com/spring-data-ldap)
- 注册写入思路参考：`spring/ldap-jwt-auth-microservice`（`WUserService#createLdapUser`）
