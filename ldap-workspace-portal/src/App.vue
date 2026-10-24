<script setup>
const projects = [
  {
    icon: '🔐',
    name: 'ldap-auth-service',
    tagline: 'LDAP 认证服务 · 整合版',
    desc: '内嵌 UnboundID LDAP + Thymeleaf UI + 自研 JWT 的一体化认证服务。支持表单登录/注册、用户与权限组管理、密码模式签发 JWT、权限实时刷新与 LDIF 一键固化。',
    badges: ['Spring Boot 3.2', '内嵌 LDAP', 'Thymeleaf', 'JWT'],
    port: 'HTTP 8080 · LDAP 8389',
    run: 'mvn spring-boot:run',
    link: 'http://localhost:8080/',
    linkLabel: '打开应用',
  },
  {
    icon: '⚡',
    name: 'ldap-jwt-auth-microservice',
    tagline: 'LDAP + JWT 微服务',
    desc: '纯 REST 微服务：PostgreSQL 与内嵌 LDAP 双写，jjwt 签发令牌。提供注册、令牌签发、资料查询接口，适合作为独立身份提供方接入其他系统。',
    badges: ['Spring Boot 3.3', 'PostgreSQL', 'jjwt', 'REST API'],
    port: 'HTTP 8080 · LDAP 389 · PG 15435',
    run: 'docker compose up -d && mvn spring-boot:run',
  },
  {
    icon: '🛡️',
    name: 'oauth-demo',
    tagline: 'OAuth2 授权服务器 · 现代化重写',
    desc: '基于最新 Spring Boot 4 + Spring Authorization Server 7 + MyBatis-Plus，支持 client_credentials、authorization_code + PKCE、refresh_token 三种授权模式，自带用户/权限组管理接口与 Vue3 前端。',
    badges: ['Spring Boot 4.1', 'Auth Server 7.1', 'MyBatis-Plus', 'Vue 3'],
    badgeClass: 'green',
    port: 'HTTP 9000 · PG 5433 · 前端 5173',
    run: 'docker compose up -d && mvn spring-boot:run',
    link: 'http://localhost:9000/',
    linkLabel: '打开授权服务器',
    extraLink: 'http://localhost:5173/',
    extraLinkLabel: '打开前端',
  },
  {
    icon: '🐳',
    name: 'spring-boot-user-management-ldap',
    tagline: '外部 OpenLDAP 用户管理',
    desc: '对接 Docker 部署的外部 OpenLDAP（含 phpLDAPadmin 管理台），读取 LDAP 中的 userPassword 做 BCrypt 比对并签发 JWT。自带静态 HTML 前端与 Swagger 文档。',
    badges: ['Spring Boot 3.4', '外部 OpenLDAP', 'jjwt', 'Swagger'],
    port: 'HTTP 1064 · LDAP 389 · phpLDAPadmin 80',
    run: 'scripts\\start.bat 一键启动',
    link: 'http://localhost:1064/login.html',
    linkLabel: '打开应用',
  },
  {
    icon: '📘',
    name: 'spring-security-ldap',
    tagline: 'Spring Security LDAP · 教程改造',
    desc: '由 Baeldung 教程独立化改造而来：内嵌 LDAP + Spring Security 表单认证，提供登录/注册/改密/权限组管理等完整 UI，是学习 Spring Security 与 LDAP 集成的最佳起点。',
    badges: ['Spring Boot 3.4', 'Spring Security', '内嵌 LDAP', 'Thymeleaf'],
    port: 'HTTP 8080 · LDAP 18889',
    run: 'mvn spring-boot:run',
    link: 'http://localhost:8080/',
    linkLabel: '打开应用',
    warn: '与 ldap-auth-service 同端口，二选一启动',
  },
]

const timeline = [
  { step: 'Step 1', name: 'spring-security-ldap', desc: '教程改造，吃透 Spring Security + LDAP 集成' },
  { step: 'Step 2', name: 'ldap-jwt-auth-microservice', desc: '演进为 JWT 微服务，引入数据库双写' },
  { step: 'Step 3', name: 'ldap-auth-service', desc: '整合版：UI + JWT + 权限组管理一体化' },
  { step: 'Step 4', name: 'oauth-demo', desc: '迈向标准 OAuth2 / OIDC 授权服务器' },
]

const ports = [
  { port: '8080', service: 'ldap-auth-service / spring-security-ldap', note: '两者同端口，二选一启动' },
  { port: '9000', service: 'oauth-demo 授权服务器', note: '' },
  { port: '1064', service: 'spring-boot-user-management-ldap', note: '' },
  { port: '5173', service: 'oauth-demo 前端（Vite dev）', note: '' },
  { port: '8389 / 18889 / 389', service: '各项目内嵌或外部 LDAP', note: '389 为外部 OpenLDAP 容器' },
  { port: '5433 / 15435', service: 'PostgreSQL 容器', note: 'oauth-demo / jwt-microservice' },
  { port: '80', service: 'phpLDAPadmin', note: '外部 OpenLDAP 的 Web 管理台' },
]
</script>

<template>
  <div>
    <!-- Hero -->
    <header class="hero">
      <div class="wrap hero-inner">
        <div class="hero-badge">🗂️ 工作区导航</div>
        <h1>LDAP &amp; OAuth2 学习工作区</h1>
        <p class="subtitle">
          从内嵌 LDAP 到标准 OAuth2 授权服务器——五个递进式 Spring Boot 项目，
          覆盖内嵌 UnboundID、外部 OpenLDAP、数据库双写三种 LDAP 集成方式。
        </p>
        <div class="hero-stats">
          <div class="hero-stat"><div class="num">5</div><div class="label">示例项目</div></div>
          <div class="hero-stat"><div class="num">3</div><div class="label">LDAP 集成方式</div></div>
          <div class="hero-stat"><div class="num">3</div><div class="label">OAuth2 授权模式</div></div>
          <div class="hero-stat"><div class="num">2</div><div class="label">前端技术栈</div></div>
        </div>
      </div>
    </header>

    <main class="wrap">
      <!-- 演进脉络 -->
      <section class="section">
        <div class="section-title">
          <h2>演进脉络</h2>
          <span class="hint">按学习路径递进</span>
        </div>
        <div class="timeline">
          <template v-for="(t, i) in timeline" :key="t.name">
            <div class="tl-item">
              <div class="tl-step">{{ t.step }}</div>
              <div class="tl-name">{{ t.name }}</div>
              <div class="tl-desc">{{ t.desc }}</div>
            </div>
            <div v-if="i < timeline.length - 1" class="tl-arrow">→</div>
          </template>
        </div>
      </section>

      <!-- 项目卡片 -->
      <section class="section">
        <div class="section-title">
          <h2>项目一览</h2>
          <span class="hint">点击卡片按钮直达运行中的应用</span>
        </div>
        <div class="grid">
          <div v-for="p in projects" :key="p.name" class="card">
            <div class="card-head">
              <div class="card-icon">{{ p.icon }}</div>
              <div class="card-title">
                <h3>{{ p.name }}</h3>
                <div class="tagline">{{ p.tagline }}</div>
              </div>
            </div>
            <p class="card-desc">{{ p.desc }}</p>
            <div class="badges">
              <span
                v-for="b in p.badges"
                :key="b"
                class="badge"
                :class="p.badgeClass"
              >{{ b }}</span>
            </div>
            <div class="card-meta">
              <div class="row"><span class="k">端口</span><span>{{ p.port }}</span></div>
              <div class="row"><span class="k">运行</span><code>{{ p.run }}</code></div>
              <div v-if="p.warn" class="row"><span class="k">注意</span><span style="color: var(--amber)">{{ p.warn }}</span></div>
            </div>
            <div class="card-actions">
              <a v-if="p.link" class="btn btn-primary" :href="p.link" target="_blank" rel="noopener">
                {{ p.linkLabel }} ↗
              </a>
              <a v-if="p.extraLink" class="btn btn-ghost" :href="p.extraLink" target="_blank" rel="noopener">
                {{ p.extraLinkLabel }} ↗
              </a>
              <span v-if="!p.link" class="btn btn-disabled">纯后端服务</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 端口速查 -->
      <section class="section">
        <div class="section-title">
          <h2>端口速查</h2>
          <span class="hint">启动前先对照，避免端口冲突</span>
        </div>
        <table class="port-table">
          <thead>
            <tr><th>端口</th><th>用途</th><th>备注</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in ports" :key="r.port">
              <td><code>{{ r.port }}</code></td>
              <td>{{ r.service }}</td>
              <td><span v-if="r.note" class="warn">{{ r.note }}</span><span v-else style="color: var(--text-3)">—</span></td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>

    <footer class="footer">
      <div class="wrap">LDAP &amp; OAuth2 学习工作区 · 详细文档见根目录 README.md</div>
    </footer>
  </div>
</template>
