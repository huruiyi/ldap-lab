# spring-boot-user-management-ldap-jpa

基于 **Spring Boot 3 + Spring Security + LDAP + JWT** 的用户管理系统，带浅色 Web UI（登录 / 注册 / 控制台 / 修改密码），用户与角色存储在 OpenLDAP 中。

> 本项目在 [Muharrem Koç](https://github.com/muharremkoc) 的原始 demo 基础上完成 Spring Boot 2.7 → 3.4 迁移、UI 与 Docker Compose 部署增强。
>
> 参考项目地址：https://github.com/muharremkoc/spring-boot-user-management-ldap-jpa.git

## 技术栈

- Spring Boot 3.4.5（JDK 17+ 语法，实测 JDK 25 可运行）
- Spring Security 6（SecurityFilterChain + @EnableMethodSecurity）
- Spring Data LDAP + spring-ldap-core（ODM 映射）
- jjwt 0.12.6（HS512，密钥 ≥64 字节）
- springdoc-openapi 2.6.0（Swagger UI）
- OpenLDAP（osixia/openldap 1.5.0，Docker）
- phpLDAPadmin（Web 管理界面）

## 快速开始

环境要求：JDK 17+、Maven 3.8+、Docker Desktop 和 Docker Compose。

### 1. 配置 JWT 密钥

HS512 要求密钥至少为 64 字节。应用不再提供默认密钥，启动前必须设置 `APP_JWT_SECRET`。

**Windows PowerShell：**

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [BitConverter]::ToString($bytes).Replace('-', '')
```

**Linux / macOS / Git Bash（需要 OpenSSL）：**

```bash
export APP_JWT_SECRET="$(openssl rand -hex 64 | tr -d '\r\n')"
```

环境变量只对当前终端及其启动的进程生效。使用一键脚本时，也必须在已设置该变量的同一个终端中执行脚本。

### 2. 选择启动方式

#### 方式一：一键脚本（Windows，推荐）

首次需先打出 JAR 包，然后在已配置环境变量的 PowerShell 或 CMD 中执行：

```powershell
mvn package -DskipTests
.\scripts\start.bat
```

- `scripts\start.bat`：启动 Docker 容器 → 等 OpenLDAP 就绪 → 自动导入种子数据 → 停掉旧应用进程 → 后台启动应用 → 等待就绪后打印访问入口
- `scripts\stop.bat`：停止应用进程和全部容器（LDAP 数据保留在 `data\slapd\`，下次启动自动恢复）

#### 方式二：手动启动

先启动 OpenLDAP（含种子数据自动导入）：

```bash
docker compose up -d
```

启动两个服务：

| 容器 | 端口 | 说明 |
|---|---|---|
| `user-management-openldap` | 389 / 636 | LDAP 服务 |
| `user-management-phpldapadmin` | 80 | Web 管理界面（登录 DN：`cn=admin,dc=springframework,dc=org`，密码 `admin`） |

另外有一个一次性的 `seed` 容器：等 OpenLDAP 健康检查后执行 `ldapadd` 导入 `data/bootstrap/seed.ldif`，然后自动退出（`Exited (0)` 是正常状态）。导入是**幂等**的——每次 `compose up` 都会执行，但只有数据库为空时真正写入。

再构建并启动应用：

```bash
mvn package -DskipTests
java -jar target/spring-boot-user-management-ldap-0.0.1-SNAPSHOT.jar
```

应用端口：**1064**

#### 3. 访问页面

| 页面 | 地址 |
|---|---|
| 登录 | http://localhost:1064/login.html |
| 注册 | http://localhost:1064/register.html |
| 控制台 | http://localhost:1064/ （Token 信息、改密、管理员可见用户列表） |
| Swagger UI | http://localhost:1064/api/swagger-ui.html |

## 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | ROLE_ADMIN / ROLE_CREATOR / ROLE_UPDATER / ROLE_DELETER |

种子数据在 `data/bootstrap/seed.ldif`，修改后需重置数据才会生效（见下文）。

## 角色与权限

| 接口 | 方法 | 所需角色 |
|---|---|---|
| `/api/versions/1/login` | POST | 公开 |
| `/api/versions/1/register` | POST | 公开 |
| `/api/versions/1/users` | GET | ROLE_ADMIN |
| `/api/versions/1/user` | POST | ROLE_ADMIN / ROLE_CREATOR |
| `/api/versions/1/user/{username}` | GET | ROLE_ADMIN |
| `/api/versions/1/user/{username}` | PUT | ROLE_ADMIN / ROLE_UPDATER |
| `/api/versions/1/user/{username}` | DELETE | ROLE_ADMIN / ROLE_DELETER |
| `/api/versions/1/user/{username}/password` | PUT | 本人或 ROLE_ADMIN |

改密规则：修改**自己的**密码必须校验旧密码（管理员改自己也不例外）；管理员重置**他人**密码可跳过旧密码。

## 认证流程说明

本项目**不使用 LDAP bind 认证**：登录时由 Java 侧读取 LDAP 中的 `userPassword` 属性做 BCrypt 比对，通过后签发 JWT（有效期 10 分钟，`JwtTokenUtil.getCustomExpirationTime`）。因此：

- 种子里的 `userPassword` 存的是 BCrypt 哈希字符串本身，**不能加 `{CRYPT}` / `{SHA}` 前缀**
- 角色存用户条目的 `o` 属性（多值），同时在 `ou=roles` 下有对应的 `groupOfNames` 组

## 数据持久化与重置

LDAP 数据通过绑定挂载持久化在宿主机：

```
data/slapd/database   →  /var/lib/ldap        （数据）
data/slapd/config     →  /etc/ldap/slapd.d    （配置）
```

**重新初始化（清空全部数据并重新导入种子）：**

```bash
docker compose down
rm -rf data/slapd/database/* data/slapd/config/*
docker compose up -d
```

注意：`docker compose down -v` 不会删除绑定挂载的目录，必须手动删。

## TLS 证书

`data/certificates/` 下的 `server.key`、`server.crt`、`dhparam.pem` 由容器首次启动自动生成（镜像内部用 cfssl 工具链），`springframework.org.ca.crt` 是指向镜像内置 CA 的符号链接（Windows 挂载目录里该链接是悬空的，显示 0 KB 属正常）。整个目录已加入 `.gitignore`，**不入库**（含私钥）。

当前应用走 389 明文端口，未启用 TLS，证书仅供 636 端口备用。如需手动重新生成（例如换域名或证书过期），`scripts/` 目录下提供了两个等效脚本：

| 脚本 | 环境 | 用法 |
|---|---|---|
| `scripts/generate-certs.bat` | Windows（需 openssl.exe 在 PATH，推荐安装 WinUniversalOpenSSL） | 双击或 cmd 执行 |
| `scripts/generate-certs.sh` | Git Bash / Linux | `bash scripts/generate-certs.sh` |

两者都会自建本地 CA 并签发 `CN=openldap`（SAN `DNS:openldap`，365 天有效）的服务器证书，私钥为 EC P-384（与容器生成的一致），另生成 2048 位 DH 参数。CA 私钥保留在 `data/certificates/.ca/` 供续签，同样被 `.gitignore` 覆盖，切勿提交。

## 为什么不直接用镜像自带的 bootstrap 机制

osixia/openldap 支持把 LDIF 放到 `/container/service/slapd/assets/config/bootstrap/ldif/` 下首次启动自动导入，但实测两个坑：

1. 直接挂载到该目录：启动收尾 `rm -rf` 清理时删不掉挂载点 → `Device or resource busy` → 容器退出
2. 用 `LDAP_SEED_INTERNAL_LDIF_PATH` 复制机制：首次导入成功，但清理会把复制目标目录删掉，**第二次启动 cp 失败又崩**

因此改为独立的 `seed` 一次性容器，简单可靠且幂等。

## Spring Boot 2.7 → 3.4 迁移要点（踩坑记录）

1. **Lombok 静默失效**：JDK 23+ 的 javac 默认关闭注解处理。需在 `maven-compiler-plugin` 显式声明 `annotationProcessorPaths`，并升级 lombok 到 1.18.42（支持 JDK 25）。
2. **循环依赖**：`SecurityConfig → JwtTokenFilter → UserServiceImpl → AuthenticationManager` 成环。解法：过滤器改为 `filterChain` 方法参数注入；登录本来就是手动 BCrypt 校验，删掉多余的 `AuthenticationManager` 注入。
3. **路径模式**：Spring 6 的 PathPattern 不允许 `/**/*.html`（`**` 后不能再接段），运行时所有请求 500。白名单需改用合法模式，并加入 `/error`（否则 403 会被错误转发覆盖成 401）。
4. **jjwt 0.9.1 → 0.12.6**：API 全部重写（`Keys.hmacShaKeyFor` + `verifyWith().parseSignedClaims`），HS512 密钥必须 ≥64 字节。
5. **springdoc 1.x → 2.6.0**：`GroupedOpenApi` 包名变为 `org.springdoc.core.models`。
6. **javax → jakarta**：servlet / validation 包名整体替换。

## 目录结构

```
├── docker-compose.yml          # OpenLDAP + phpLDAPadmin + seed 一次性容器
├── scripts/
│   ├── start.bat / stop.bat      # Windows 一键启停脚本
│   └── generate-certs.bat / .sh  # 手动重新生成 TLS 证书（一般用不到）
├── .gitignore                  # 排除 target/、data/slapd/、data/certificates/ 等
├── data/
│   ├── bootstrap/seed.ldif     # 种子数据（ou、admin、4 个角色组，需提交）
│   ├── certificates/           # TLS 证书（容器自动生成，勿提交）
│   └── slapd/                  # LDAP 数据与配置（绑定挂载，勿提交）
├── src/main/java/com/ldap/springbootusermanagementldap/
│   ├── config/                 # SecurityConfig、OpenApiConfig
│   ├── controller/             # UserController
│   ├── dto/                    # 请求/响应对象
│   ├── jwt/                    # JwtTokenUtil、JwtTokenFilter
│   ├── model/                  # User、Role（LDAP ODM 实体）
│   ├── repository/             # Spring Data LDAP Repository
│   └── service/users/          # UserService / UserServiceImpl
└── src/main/resources/
    ├── application.properties
    └── static/                 # UI：login.html / register.html / index.html + css/js
```
