# LDAP JWT Auth Microservice

基于 Spring Boot 的身份认证示例项目，将 **LDAP 目录认证、PostgreSQL 用户与角色管理、JWT 无状态鉴权** 组合在同一个微服务中。项目同时提供一个简单的静态页面，可直接体验注册、登录、查询个人资料和退出登录。

参考项目地址：https://github.com/cypcodestudio/ldap-jwt-auth-microservice.git

## 功能特性

- 用户注册：将账号及角色关系保存到 PostgreSQL，并同步创建 LDAP 用户。
- 用户登录：优先尝试 LDAP 认证；不存在对应 LDAP 账号时回退到数据库密码认证。
- JWT 鉴权：登录后返回访问令牌和刷新令牌，受保护接口通过 `Bearer Token` 访问。
- 角色权限：支持 `ROLE_USER`、`ROLE_ADMIN` 等 Spring Security 角色。
- 个人资料：根据当前登录用户查询数据库 ID 和 LDAP 属性。
- 内嵌 LDAP：开发环境自动加载 LDIF 示例数据，无需单独安装 LDAP 服务。
- 静态演示页：内置注册、登录和资料查询界面。

## 技术栈

| 技术 | 用途 |
| --- | --- |
| Java 17 | 运行环境 |
| Spring Boot 3.3.5 | 应用框架 |
| Spring Security | 认证、授权和密码加密 |
| Spring Data JPA | PostgreSQL 数据访问 |
| Spring Data LDAP / Spring LDAP | LDAP 查询、认证和用户创建 |
| UnboundID LDAP SDK | 内嵌 LDAP 服务 |
| JJWT 0.11.5 | JWT 生成与校验 |
| PostgreSQL 16 | 用户及角色数据持久化 |
| Maven | 项目构建和依赖管理 |

## 认证流程

```text
客户端
  │  POST /identity/token
  ▼
IdentityController
  │  按 uid 或 cn 查询 LDAP
  ├─ 找到 LDAP 账号 ──> LDAP 密码认证
  └─ 未找到         ──> PostgreSQL 用户 + BCrypt 密码认证
  │
  ▼
加载数据库用户及角色
  │
  ▼
签发 Access Token 和 Refresh Token

访问受保护接口时：
Authorization: Bearer <token>
  │
  ▼
JWTRequestFilter 校验签名、有效期和用户名
  │
  ▼
写入 Spring Security 上下文并执行接口授权
```

> 完整的登录和资料查询流程依赖数据库中的用户及角色记录。因此，仅存在于 LDIF 中、没有对应数据库记录的 LDAP 示例账号，不能直接完成整个登录流程。建议通过注册接口创建测试用户。

## 项目结构

```text
ldap-jwt-auth-microservice/
├── docker-compose.yml                 # PostgreSQL 开发环境
├── pom.xml                            # Maven 配置
└── src/main/
    ├── java/com/cypcode/ldap_jwt_auth_microservice/
    │   ├── Application.java           # 启动类、BCrypt 配置
    │   ├── configuration/             # Spring Security 配置
    │   ├── controller/                # 注册、登录、资料接口
    │   ├── domain/                    # JPA 实体和 DTO
    │   ├── repository/                # 数据库及 LDAP 仓储
    │   ├── security/                  # JWT 工具和请求过滤器
    │   └── service/                   # 用户、角色业务逻辑
    └── resources/
        ├── application.properties     # 应用配置
        ├── ldap-demo.ldif             # 内嵌 LDAP 初始化数据
        └── static/                    # 内置演示页面
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+
- Docker 和 Docker Compose（用于启动 PostgreSQL；也可使用已有 PostgreSQL）

## 快速启动

### 1. 启动 PostgreSQL

在项目根目录执行：

```bash
docker compose up -d
```

Compose 会创建以下开发数据库：

| 配置项 | 默认值 |
| --- | --- |
| 主机 | `localhost` |
| 端口 | `15435` |
| 数据库 | `wonderbankdb` |
| 用户名 | `cdbdev` |
| 密码 | `dbdev123` |

数据库数据保存在 Docker 卷 `wonderbank-pgdata` 中，容器重启后不会丢失。

### 2. 启动应用

```bash
mvn spring-boot:run
```

应用默认监听 `http://localhost:8080`。启动时会自动：

- 连接 PostgreSQL，并通过 JPA 创建或更新数据表；
- 在 `389` 端口启动内嵌 LDAP；
- 加载 `src/main/resources/ldap-demo.ldif`。

如果本机 `389` 端口已被占用，需要修改内嵌 LDAP 端口及 `spring.ldap.urls` 后再启动。

### 3. 打开演示页面

浏览器访问：

```text
http://localhost:8080/
```

注册一个新用户后，使用该账号登录即可查看个人资料。

## 配置说明

默认配置位于 `src/main/resources/application.properties`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `jwt.secret` | 项目内置开发密钥 | JWT HMAC 签名密钥 |
| `spring.datasource.url` | `jdbc:postgresql://localhost:15435/wonderbankdb` | PostgreSQL 连接地址 |
| `spring.datasource.username` | `cdbdev` | 数据库用户名 |
| `spring.datasource.password` | `dbdev123` | 数据库密码 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动创建或更新数据表 |
| `spring.ldap.urls` | `ldap://127.0.0.1:389` | LDAP 服务地址 |
| `spring.ldap.base` | `dc=xl,dc=com` | LDAP Base DN |
| `spring.ldap.username` | `cn=admin,dc=xl,dc=com` | LDAP 管理账号 |
| `spring.ldap.password` | `password` | LDAP 管理密码 |
| `spring.ldap.embedded.port` | `389` | 内嵌 LDAP 端口 |
| `spring.ldap.embedded.ldif` | `classpath:ldap-demo.ldif` | LDAP 初始化文件 |

可以使用 Spring Boot 环境变量覆盖默认配置，例如：

```bash
export JWT_SECRET='replace-with-a-long-random-secret'
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:15435/wonderbankdb'
export SPRING_DATASOURCE_USERNAME='cdbdev'
export SPRING_DATASOURCE_PASSWORD='dbdev123'
mvn spring-boot:run
```

Windows PowerShell 示例：

```powershell
$env:JWT_SECRET = 'replace-with-a-long-random-secret'
$env:SPRING_DATASOURCE_PASSWORD = 'dbdev123'
mvn spring-boot:run
```

## API 使用说明

所有接口均返回统一结构：

```json
{
  "TimeStamp": "2026-01-01T00:00:00.000+00:00",
  "Message": "Authentication",
  "Status": 200,
  "Data": {}
}
```

### 注册用户

```http
POST /identity/register
Content-Type: application/json
```

请求示例：

```bash
curl -X POST http://localhost:8080/identity/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "lisi",
    "password": "your-password",
    "entityNo": "demo",
    "firstname": "si",
    "lastname": "li",
    "cn": "李四",
    "email": "lisi@example.com",
    "roleList": []
  }'
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `username` | 是 | 登录名，同时作为 LDAP `uid` |
| `password` | 是 | 原始密码；数据库保存 BCrypt 哈希 |
| `entityNo` | 否 | 用户实体编号 |
| `firstname` | 是 | LDAP `givenName` |
| `lastname` | 是 | LDAP `sn` |
| `cn` | 否 | LDAP 通用名；为空时由姓名拼接 |
| `email` | 否 | LDAP `mail` |
| `roleList` | 否 | 角色名列表，例如 `["USER"]`；空列表使用默认角色 |

> 用户名应避免与 `ldap-demo.ldif` 中已有条目重复。若 LDAP 中已存在同名 `uid`，注册逻辑不会覆盖该 LDAP 条目。

### 登录并获取令牌

```http
POST /identity/token
Content-Type: application/json
```

```bash
curl -X POST http://localhost:8080/identity/token \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "lisi",
    "password": "your-password"
  }'
```

成功响应中的 `Data`：

```json
{
  "token": "<access-token>",
  "refreshToken": "<refresh-token>"
}
```

- Access Token 有效期：**5 小时**。
- Refresh Token 有效期：**10 小时**。
- 当前版本只负责签发 Refresh Token，**尚未提供刷新令牌接口**。

### 查询当前用户资料

```http
GET /identity/profile
Authorization: Bearer <access-token>
```

```bash
curl http://localhost:8080/identity/profile \
  -H 'Authorization: Bearer <access-token>'
```

响应中的 `Data` 包含：

```json
{
  "id": 1,
  "uid": "lisi",
  "sn": "li",
  "givenName": "si",
  "cn": "李四",
  "mail": "lisi@example.com"
}
```

### 接口权限摘要

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `POST` | `/identity/register` | 公开 |
| `POST` | `/identity/token` | 公开 |
| `GET` | `/identity/profile` | 需要有效 JWT |
| `GET` | `/` | 公开，静态演示页 |

## 构建与测试

执行测试：

```bash
mvn test
```

构建可执行 JAR：

```bash
mvn clean package
java -jar target/ldap-jwt-auth-microservice-0.0.1-SNAPSHOT.jar
```

停止开发数据库：

```bash
docker compose down
```

如需同时删除数据库数据卷：

```bash
docker compose down -v
```

## 开发注意事项

当前项目适合学习和本地演示，不应直接按默认配置部署到生产环境：

1. 仓库中的数据库密码、LDAP 管理密码和 JWT 密钥均为开发默认值，生产环境必须通过安全的配置或密钥管理机制替换。
2. JWT 密钥必须具有足够长度和随机性，并避免提交到版本控制。
3. 当前 CORS 配置允许所有来源、请求头和请求方法，生产环境应限制为可信来源。
4. 当前使用 `spring.jpa.hibernate.ddl-auto=update`，生产环境建议使用正式数据库迁移工具。
5. 注册流程中的数据库写入与 LDAP 写入不是跨系统原子事务，接入生产环境前需要补充一致性及失败补偿处理。
6. Refresh Token 刷新、注销和服务端撤销机制尚未实现。
