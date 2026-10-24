# ldap-auth-service

参考项目地址：https://github.com/devrishal/LdapAndOauthSpringBoot.git

基于 Spring Boot 3 的 LDAP 认证服务：内嵌 LDAP + 表单登录 Web UI + JWT 令牌接口 + 用户与权限组管理。

## 功能

- **LDAP 认证**：用户名密码经 LDAP 目录验证（默认使用内嵌 UnboundID 服务器，无需外部依赖）
- **Web UI**：登录页、注册页、用户首页、用户管理页、权限组管理页、403/404 错误页（Thymeleaf）
- **用户注册**：新用户写入 LDAP 目录并自动加入 `user` 组，可立即登录
- **用户管理**：管理员查看全部用户（姓名/邮箱/角色）、新建用户、重置密码、删除用户（自动移出所有组）
- **权限组管理**：管理员在页面上授予/收回管理员权限，支持新建/删除自定义权限组和任意组成员管理，改动对在线用户下一次请求即生效
- **数据固化**：管理页一键把当前目录（用户 + 组成员）导出为 LDIF，重启后不丢数据
- **JWT 令牌**：`POST /oauth/token`（密码模式）签发 JWT，API 客户端凭 Bearer token 访问受保护接口

## 快速开始

环境要求：JDK 17+。项目附带 Maven Wrapper，无需单独安装 Maven。

JWT 密钥不再提供默认值，启动前必须通过 `JWT_SECRET` 设置。建议使用至少 32 字节的随机密钥；以下示例生成 64 字节随机值。

**Windows PowerShell：**

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [BitConverter]::ToString($bytes).Replace('-', '')
.\mvnw.cmd spring-boot:run
```

**Linux / macOS / Git Bash（需要 OpenSSL）：**

```bash
export JWT_SECRET="$(openssl rand -hex 64 | tr -d '\r\n')"
./mvnw spring-boot:run
```

环境变量只对当前终端及其启动的进程生效；重新打开终端后需要重新设置。生产环境请使用密钥管理服务，不要将真实密钥写入源码或提交到 Git。

访问 http://localhost:8080/ ，预置账号：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin` | ADMIN + USER（超级管理员，权限不可修改） |
| `demo` | `demo` | USER |

## 页面与接口

| 路径 | 说明 | 权限 |
| --- | --- | --- |
| `/login` | 登录页 | 公开 |
| `/register` | 注册页 | 公开 |
| `/home` | 用户首页（显示当前用户与角色） | 登录 |
| `/admin/users` | 用户管理：列表、新建、重置密码、删除 | ROLE_ADMIN |
| `/admin/groups` | 权限组管理 | ROLE_ADMIN |
| `POST /admin/export-ldif` | 固化：把当前目录导出为 LDIF（管理页按钮触发） | ROLE_ADMIN |
| `POST /oauth/token` | 签发 JWT（参数 `grant_type=password&username&password`） | 公开 |
| `GET /api/me` | 返回当前用户信息（Session 或 Bearer token 均可） | 登录 |

JWT 使用示例：

```bash
curl -X POST http://localhost:8080/oauth/token \
  -d "grant_type=password" -d "username=admin" -d "password=admin"

curl -H "Authorization: Bearer <access_token>" http://localhost:8080/api/me
```

## 目录结构

```
com.demo.ldap
├── LdapAuthServiceApplication     启动类
├── config/     SecurityConfig（安全规则）、JwtConfig（JWT 编解码）、EmbeddedLdapConfig（内嵌 LDAP 服务器）
├── controller/ AuthController（登录/首页）、RegistrationController（注册）、
│               AdminController（组管理/固化）、TokenController（发 token）、ApiController（示例 API）
├── security/   CustomAuthenticationManager、CustomLdapAuthenticator（LDAP 认证）、JwtTokenService
├── service/    LdapUserServiceImpl（JNDI 认证）、RegistrationService（注册写入）、
│               GroupAdminService（组管理）、LdapGroupService（组→角色映射）、LdapExportService（固化）
├── filter/     CORSFilter、AuthorityRefreshFilter（每请求刷新角色）
└── exception/  CustomException
```

## 数据固化

内嵌 LDAP 数据在内存中，注册的新用户和组变更默认重启即丢。管理页的「固化数据」按钮（或 `POST /admin/export-ldif`）会把当前目录导出为 LDIF，同时写两个位置：

- `data/ldap.ldif` — 运行时位置，启动时**优先加载**，重启即生效（已加入 .gitignore）
- `src/main/resources/ldap.ldif` — 随源码保存，作为全新环境（`mvn clean` 后）的初始数据

启动加载策略由 `config/EmbeddedLdapConfig` 实现：`data/ldap.ldif` 存在则用它，否则回退到 classpath 里的初始 `ldap.ldif`。注意：打进 jar 后 classpath 里的 LDIF 是打包时刻的快照，只改源文件不重新打包不会生效。

## 角色机制

- LDAP 目录中 `ou=groups` 下的 `groupOfNames` 组即角色来源：组 `cn` 大写加 `ROLE_` 前缀（`cn=admin` → `ROLE_ADMIN`）
- 用户 DN 出现在组的 `member` 属性里即拥有该角色
- 登录时加载角色；`AuthorityRefreshFilter` 每个请求重新查询，授权/撤权实时生效

## 配置（application.properties）

| 配置 | 说明 |
| --- | --- |
| `ldap.embedded.port/admin-dn/admin-password` | 内嵌 LDAP 服务器（EmbeddedLdapConfig）：端口 8389、管理账号 |
| `spring.ldap.urls/username/password` | `LdapTemplate` 连接（注册、组管理需要写权限） |
| `ldap.*` | 应用认证用的 JNDI 配置，接真实 LDAP 时改这里 |
| `jwt.secret` / `jwt.expires-in` | JWT 签名密钥（HS256，至少 32 字符）与有效期（秒） |

接真实 LDAP 服务器时，把 `ldap.providerURL` 和 `spring.ldap.urls` 指向同一服务器，并删除 `EmbeddedLdapConfig`（不再启动内嵌服务器）。

## legacy/ 目录

`legacy/` 保存了旧版实现（`OauthAuthorisationServer`、`OauthResourceServer` 基于已废弃的 `spring-security-oauth2`，`ImpalaDataSourceConfig` 依赖商业驱动），仅作参考存档，不参与编译。旧版 OAuth2 密码模式已由 `TokenController` + JWT 替代。
