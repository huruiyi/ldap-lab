# oauth-demo

从 `ldap-auth-service/legacy` 中提取的 OAuth2 代码现代化重写 demo。

- Spring Boot **4.1.1**（当前最新稳定版）
- Spring Authorization Server **7.1.1**（官方授权服务器，替代已废弃的 spring-security-oauth2）
- Java 17

## 新旧对照

| 旧代码（legacy，spring-security-oauth2） | 本项目（Spring Authorization Server） |
|---|---|
| `@EnableAuthorizationServer` + `AuthorizationServerConfigurerAdapter` | `OAuth2AuthorizationServerConfigurer` + `SecurityFilterChain` |
| `JdbcClientDetailsService`（JDBC 客户端存储） | `JdbcRegisteredClientRepository`（H2，`oauth2_registered_client` 表） |
| `JdbcTokenStore`（JDBC 令牌存储） | `JdbcOAuth2AuthorizationService`（`oauth2_authorization` 表） |
| `DefaultTokenServices`（refresh token 开关） | 框架内置，`TokenSettings` 配置令牌有效期 |
| `@EnableResourceServer` + `ResourceServerConfigurerAdapter` | `oauth2ResourceServer().jwt()` + `SecurityFilterChain` |
| password 授权模式 | **OAuth 2.1 已移除**，改用 authorization_code + PKCE |
| `ImpalaDataSourceConfig`（Kerberos + Impala JDBC） | 用 H2 内存库替代做演示；真实环境换 `spring.datasource` 指向目标库即可 |

## 运行

```bash
docker compose up -d      # 启动 PostgreSQL 16 (postgres:16-alpine, 主机端口 5433)
mvn spring-boot:run
```

> 主机 5432 已被本机安装的 PostgreSQL 占用，容器映射到 **5433**。

端口 **9000**。首次启动自动建表并写入演示客户端 `demo-client / demo-secret`、公共客户端 `vue-client`；授权码流程的登录用户为 `demo / demo`（普通用户组）、`admin / admin`（管理员组）。

没有 Docker 时可用 H2 兜底运行（数据不持久化）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## 权限组

用户、权限组、成员关系三张表（`app_users` / `permission_groups` / `group_members`），数据访问层使用 **MyBatis-Plus 3.5.17**（`mybatis-plus-spring-boot4-starter`，注意 ServiceImpl 在 3.5.14+ 迁到了 `com.baomidou.mybatisplus.spring.service.impl` 包）；首次启动由 `DataInitializer` 写入：

| 权限组 | 说明 | 默认成员 |
|---|---|---|
| `ADMIN` | 管理员组 | admin |
| `USER` | 普通用户组 | admin、demo |

- 用户登录后，JWT 携带 `groups` 声明（如 `["ADMIN","USER"]`）
- 资源服务器把 `groups` 映射回 `ROLE_*` 权限，`@PreAuthorize("hasRole('ADMIN')")` 生效
- `GET /api/admin/stats` 仅管理员组可访问（demo 403，admin 200）
- client_credentials 的令牌没有 `groups` 声明，访问组受限接口会得到 403

## 验证流程

### 1. client_credentials（服务间调用）

```bash
curl -u demo-client:demo-secret \
  -d "grant_type=client_credentials&scope=api.read api.write" \
  http://localhost:9000/oauth2/token
```

拿到 `access_token` 后访问受保护资源：

```bash
curl -H "Authorization: Bearer <token>" http://localhost:9000/api/hello
curl -X POST -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"msg":"hi"}' http://localhost:9000/api/echo
```

无 token 访问返回 401；secret 错误返回 `invalid_client`。

### 2. authorization_code + PKCE（用户授权，OAuth 2.1 强制 PKCE）

浏览器访问：

```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=demo-client&redirect_uri=http://127.0.0.1:9000/callback&scope=api.read&code_challenge=<S256挑战码>&code_challenge_method=S256
```

跳到登录页用 `demo / demo` 登录，授权后重定向到 `/callback?code=...`，页面会显示授权码和换令牌的 curl 命令。

```bash
curl -u demo-client:demo-secret \
  -d "grant_type=authorization_code&code=<code>&redirect_uri=http://127.0.0.1:9000/callback&code_verifier=<原始verifier>" \
  http://localhost:9000/oauth2/token
```

返回 `access_token` + `refresh_token`；此 token 的 subject 是登录用户（`demo`），区别于 client_credentials 的 `demo-client`。

### 3. refresh_token

```bash
curl -u demo-client:demo-secret \
  -d "grant_type=refresh_token&refresh_token=<refresh_token>" \
  http://localhost:9000/oauth2/token
```

## 端点一览

| 端点 | 说明 |
|---|---|
| `POST /oauth2/token` | 令牌端点（三种授权模式） |
| `GET /oauth2/authorize` | 授权端点（浏览器） |
| `GET /oauth2/jwks` | JWT 公钥（资源服务器验签用） |
| `GET /api/hello` | 受保护资源（需 `api.read` scope） |
| `POST /api/echo` | 受保护资源（需 `api.write` scope） |
| `GET /callback` | 授权码回调展示页 |

## 前端（frontend/）

Vite + Vue 3 + Vue Router + Pinia 的多页面 SPA，演示标准的前端 OAuth2 集成方式：SPA 作为**公共客户端**（`vue-client`，无密钥），走 authorization_code + PKCE。

```bash
cd frontend
npm install
npm run dev    # 默认 5173 端口
```

页面：

| 路由 | 说明 |
|---|---|
| `/login` | 登录页，点击跳转授权服务器 |
| `/callback` | 授权码回调，自动换令牌 |
| `/` | 首页：令牌信息、JWT 内容、会话状态 |
| `/api-test` | API 测试：调用受保护资源、验证 401 |
| 其他 | 404 页 |

要点：

- Pinia `auth` store 管理令牌（localStorage 持久化），路由守卫拦截未登录访问
- `redirect_uri` 按 `window.location.origin` 动态生成，后端已注册 5173 / 7100 端口
- 后端为 5173/7100 来源开了 CORS（仅 `Authorization`、`Content-Type` 头）
- SAS 不向公共客户端签发 refresh_token（OAuth 2.1 安全实践），令牌过期后重新登录；refresh_token 流程用机密客户端 `demo-client` 演示

## 注意事项

- RSA 签名密钥每次启动随机生成，重启后旧 token 全部失效；生产环境应持久化 JWK。
- `ImpalaDataSourceConfig` 未移植编译：Cloudera Impala JDBC 驱动不在 Maven 中央仓库，且依赖 Kerberos 环境。迁移要点：`javax.*` → `jakarta.*`，`DataSourceBuilder` 仍可沿用，HikariCP 配置不变；需要时把 `spring.datasource` 指向 Impala 即可复用 JDBC 客户端/令牌存储。
- 已知环境兼容问题：本机 Maven 3.8.1 的依赖解析会丢失 spring-core 7 声明的 commons-logging，已在 pom 中显式声明 workaround。
