const TOKEN_KEY = "ldap-jwt-token";

const output = document.querySelector("#output");
const tokenState = document.querySelector("#token-state");
const profileCard = document.querySelector("#profile-card");

function token() {
  return sessionStorage.getItem(TOKEN_KEY) || "";
}

function paintTokenState() {
  const loggedIn = Boolean(token());
  document.body.classList.toggle("is-authenticated", loggedIn);
  tokenState.textContent = loggedIn ? "已登录" : "未登录";
  tokenState.classList.toggle("is-in", loggedIn);
}

function show(view) {
  if (token() && view !== "profile") {
    view = "profile";
  }
  document.querySelectorAll(".panel").forEach((panel) => {
    panel.hidden = panel.id !== view;
  });
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.classList.toggle("is-active", tab.dataset.view === view);
  });
  if (view === "profile" && token()) {
    loadProfile();
  }
}

function writeResult(title, body) {
  output.textContent = title + "\n" + (typeof body === "string" ? body : JSON.stringify(body, null, 2));
}

async function callApi(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (token()) {
    headers.Authorization = "Bearer " + token();
  }
  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  let body = text;
  try {
    body = text ? JSON.parse(text) : {};
  } catch (error) {
    body = text;
  }
  return { response, body };
}

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => show(tab.dataset.view));
});

document.querySelector("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const { response, body } = await callApi("/identity/token", {
    method: "POST",
    body: JSON.stringify({
      username: form.get("username"),
      password: form.get("password")
    })
  });
  const jwt = body && body.Data && body.Data.token;
  if (response.ok && jwt) {
    sessionStorage.setItem(TOKEN_KEY, jwt);
    paintTokenState();
    writeResult("登录成功", body);
    show("profile");
    return;
  }
  writeResult("登录失败 " + response.status, body);
});

document.querySelector("#register-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const { response, body } = await callApi("/identity/register", {
    method: "POST",
    body: JSON.stringify({
      username: form.get("username"),
      password: form.get("password"),
      lastname: form.get("lastname"),
      firstname: form.get("firstname"),
      cn: form.get("cn"),
      email: form.get("email"),
      roleList: []
    })
  });
  writeResult((response.ok ? "注册完成 " : "注册失败 ") + response.status, body);
});

async function loadProfile() {
  if (!token()) {
    profileCard.hidden = true;
    writeResult("尚未登录", "请先在登录页获取令牌。");
    return;
  }
  const { response, body } = await callApi("/identity/profile");
  if (!response.ok) {
    profileCard.hidden = true;
    writeResult("读取资料失败 " + response.status, body);
    return;
  }
  const user = body.Data || {};
  profileCard.hidden = false;
  ["uid", "sn", "givenName", "cn", "mail"].forEach((field) => {
    profileCard.querySelector('[data-field="' + field + '"]').textContent = user[field] || "-";
  });
  writeResult("资料", body);
}

document.querySelector("#load-profile").addEventListener("click", loadProfile);
document.querySelector("#logout").addEventListener("click", () => {
  sessionStorage.removeItem(TOKEN_KEY);
  profileCard.hidden = true;
  paintTokenState();
  writeResult("已退出", "本地令牌已清除。");
  show("login");
});

paintTokenState();
show(token() ? "profile" : "login");
