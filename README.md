# LDAP & OAuth2 学习工作区

一个围绕 **LDAP 认证** 与 **OAuth2 授权** 的 Spring Boot 进阶学习工作区，包含 5 个递进式示例项目和 1 个导航门户。从教程级的 Spring Security LDAP 集成，一路演进到基于最新 Spring Authorization Server 的标准 OAuth2/OIDC 授权服务器。

## 目录结构

```
ldap/
├── ldap-workspace-portal/                              # 工作区导航门户（Vite + Vue 3，纯静态）
├── spring-security-ldap/                # ① Spring Security + 内嵌 LDAP（教程改造）
├── ldap-jwt-auth-microservice/          # ② LDAP + JWT 微服务（PostgreSQL 双写）
├── ldap-auth-service/                   # ③ LDAP 认证服务整合版（UI + JWT + 权限组）
├── spring-boot-user-management-ldap/    # ④ 外部 OpenLDAP 用户管理（Docker）
└── spring-boot-4-oauth-demo/                          # ⑤ OAuth2 授权服务器（最新技术栈 + Vue3 前端）
```

## 项目对比总览

| 项目 | 定位 | 关键技术 | LDAP 方式 | 端口 | 前端 |
|---|---|---|---|---|---|
| `spring-security-ldap` | 教程入门 | Boot 3.4 · Spring Security · Thymeleaf | 内嵌（18889） | 8080 | Thymeleaf UI |
| `ldap-jwt-auth-microservice` | JWT 微服务 | Boot 3.3 · jjwt · PostgreSQL | 内嵌（389）+ 数据库双写 | 8080 | 无（纯 REST） |
| `ldap-auth-service` | 整合版认证服务 | Boot 3.2 · 内嵌 LDAP · 自研 JWT | 内嵌（8389） | 8080 | Thymeleaf UI |
| `spring-boot-user-management-ldap` | 外部 LDAP 对接 | Boot 3.4 · jjwt · Docker OpenLDAP | 外部容器（389） | 1064 | 静态 HTML |
| `spring-boot-4-oauth-demo` | OAuth2 授权服务器 | Boot 4.1 · Authorization Server 7.1 · MyBatis-Plus | —（PostgreSQL） | 9000 | Vite + Vue 3 |

三种 LDAP 集成方式全部覆盖：**内嵌 UnboundID**、**外部 OpenLDAP（Docker）**、**数据库双写**。

## 演进脉络

```
spring-security-ldap          教程改造，吃透 Spring Security + LDAP 集成
        ↓
ldap-jwt-auth-microservice    演进为 JWT 微服务，引入 PostgreSQL 双写
        ↓
ldap-auth-service             整合版：UI + JWT 签发 + 权限组管理一体化
        ↓
spring-boot-4-oauth-demo                    迈向标准 OAuth2 / OIDC 授权服务器
```

---

## 各项目详情

### ① spring-security-ldap —— 教程入门

由 Baeldung 教程独立化改造而来，是学习 Spring Security 与 LDAP 集成的最佳起点。

- **技术栈**：Spring Boot 3.4.5、Spring Security、内嵌 UnboundID LDAP（端口 18889，Base DN `dc=baeldung,dc=com`）、Thymeleaf
- **功能**：表单登录 / 用户注册 / 修改密码 / 权限组管理 / 权限实时刷新 / LDIF 一键固化
- **预置账号**：`admin / adminpass`
- **运行**：

```bash
cd spring-security-ldap
mvn spring-boot:run
# 打开 http://localhost:8080/
```

> ⚠️ 与 `ldap-auth-service` 同为 8080 端口，两者二选一启动。

### ② ldap-jwt-auth-microservice —— JWT 微服务

无前端、纯 REST 的身份微服务，适合作为独立身份提供方（IdP）接入其他系统。

- **技术栈**：Spring Boot 3.3.5、jjwt 0.11.5、PostgreSQL（Docker，映射 `15435:5432`，库 `wonderbankdb`，账号 `cdbdev / dbdev123`）、内嵌 LDAP（389）
- **特点**：注册时 PostgreSQL 与 LDAP **双写**，兼顾关系型查询与目录服务认证
- **接口**：
  - `POST /identity/register` —— 注册
  - `POST /identity/token` —— 签发 JWT
  - `GET /identity/profile` —— 查询当前用户资料
- **运行**：

```bash
cd ldap-jwt-auth-microservice
docker compose up -d        # 启动 PostgreSQL
mvn spring-boot:run
```

### ③ ldap-auth-service —— 整合版认证服务

集大成的一体化认证服务：内嵌 LDAP + 完整管理 UI + JWT 签发。

- **技术栈**：Spring Boot 3.2.4、Java 17、内嵌 UnboundID LDAP（8389）、Thymeleaf、自研 JWT（HS256）
- **功能**：
  - 表单登录 / 注册，美观的浅色靛蓝主题 UI
  - 用户管理（管理员）：新建 / 编辑 / 启停 / 重置密码，全部弹框操作
  - 权限组管理：组成员增删、权限实时刷新（AuthorityRefreshFilter）
  - `POST /oauth/token` 密码模式签发 JWT
  - LDIF 一键固化：将内存中的目录数据导出为 LDIF
- **预置账号**：`admin / admin`
- **运行**：

```bash
cd ldap-auth-service
mvn spring-boot:run
# 打开 http://localhost:8080/
```

`legacy/` 目录保存了旧的 OAuth2 实现代码（已归档，不参与构建）。

### ④ spring-boot-user-management-ldap —— 外部 OpenLDAP 对接

演示如何对接**真实的外部 LDAP 服务器**（Docker 部署的 osixia/openldap），并附带 phpLDAPadmin Web 管理台。

- **技术栈**：Spring Boot 3.4.5、jjwt 0.12.6（HS512）、Docker OpenLDAP + phpLDAPadmin
- **特点**：不使用 LDAP bind 认证，而是读取 LDAP 中的 `userPassword` 属性做 **BCrypt 比对**，再签发 JWT
- **接口/页面**：静态 HTML 前端（`/login.html` 等）、Swagger 文档 `/api/swagger-ui.html`
- **运行**：

```bash
cd spring-boot-user-management-ldap
scripts\start.bat           # Windows 一键启动（含容器与应用）
# 应用 http://localhost:1064/ ，phpLDAPadmin http://localhost/
```

### ⑤ spring-boot-4-oauth-demo —— OAuth2 授权服务器（最新技术栈）

由 `ldap-auth-service/legacy` 中的旧 OAuth2 代码现代化重写而来，全工作区技术栈最新。

- **技术栈**：**Spring Boot 4.1.1**、**Spring Authorization Server 7.1.1**、MyBatis-Plus 3.5.17、PostgreSQL 16（Docker，`5433:5432`，库 `oauthdb`，账号 `oauth / oauth123`）；提供 h2 profile 作为无 Docker 环境的兜底
- **授权模式**：`client_credentials`、`authorization_code` + **PKCE**、`refresh_token`
- **管理接口**：`/api/admin/**` 用户与权限组管理（默认内置管理员和普通用户两个权限组）
- **前端**：`frontend/` 独立 Vite + Vue 3 项目（Vue Router + Pinia，多页面），含自定义美观登录页、API 调试台；退出登录会同时清除授权服务器会话
- **运行**：

```bash
cd spring-boot-4-oauth-demo
docker compose up -d                  # 启动 PostgreSQL 16
mvn spring-boot:run                   # 授权服务器 http://localhost:9000/

cd frontend
npm install && npm run dev            # 前端 http://localhost:5173/
```

---

## 端口分配总表

| 端口 | 用途 | 备注 |
|---|---|---|
| 8080 | `ldap-auth-service` / `spring-security-ldap` / `ldap-jwt-auth-microservice` | 三者同端口，同时只启动一个 |
| 9000 | `spring-boot-4-oauth-demo` 授权服务器 | |
| 1064 | `spring-boot-user-management-ldap` | |
| 5173 | `spring-boot-4-oauth-demo` 前端（Vite dev） | |
| 5200 | `ldap-workspace-portal` 门户（Vite dev） | |
| 8389 / 18889 / 389 | 各项目内嵌 / 外部 LDAP | 389 为外部 OpenLDAP 容器 |
| 5433 / 15435 | PostgreSQL 容器 | spring-boot-4-oauth-demo / jwt-microservice |
| 80 | phpLDAPadmin | 外部 OpenLDAP 的 Web 管理台 |

## 快速开始

环境要求：**JDK 17+**、**Maven 3.8+**、**Docker Desktop**（②④⑤ 需要）、**Node.js 18+**（⑤ 前端与门户需要）。

### JWT 环境变量

②③④ 不再内置 JWT 密钥，启动前必须设置对应变量：

| 项目 | 环境变量 | 密钥要求 |
| --- | --- | --- |
| `ldap-jwt-auth-microservice` | `JWT_SECRET` | 至少 32 字节，建议 64 字节随机值 |
| `ldap-auth-service` | `JWT_SECRET` | HS256，至少 32 字节 |
| `spring-boot-user-management-ldap` | `APP_JWT_SECRET` | HS512，至少 64 字节 |

**Windows PowerShell（生成 64 字节随机密钥）：**

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [BitConverter]::ToString($bytes).Replace('-', '')
$env:APP_JWT_SECRET = [BitConverter]::ToString($bytes).Replace('-', '')
```

**Linux / macOS / Git Bash（需要 OpenSSL）：**

```bash
export JWT_SECRET="$(openssl rand -hex 64 | tr -d '\r\n')"
export APP_JWT_SECRET="$(openssl rand -hex 64 | tr -d '\r\n')"
```

只需设置当前要启动的项目所对应的变量。变量仅在当前终端中有效，详细说明见各项目 README。

### 启动导航门户

```bash
cd ldap-workspace-portal
npm install
npm run dev    # http://localhost:5200/
```

随后按照上方各项目小节进入对应目录并启动服务。注意②和③使用相同端口，④的一键脚本必须在已设置 `APP_JWT_SECRET` 的终端中运行。

建议学习顺序：`spring-security-ldap` → `ldap-jwt-auth-microservice` → `ldap-auth-service` → `spring-boot-user-management-ldap` → `spring-boot-4-oauth-demo`。
