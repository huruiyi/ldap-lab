// 未登录一律回登录页
if (!isLoggedIn()) {
  window.location.href = '/login.html';
}

const uid = TokenStore.getUid();
const roles = currentRoles();

document.getElementById('username').textContent = uid;
document.getElementById('roles').innerHTML =
  roles.length
    ? roles.map(r => '<span class="badge">' + r + '</span>').join(' ')
    : '<span class="badge">普通用户</span>';

// 展示 token 摘要
const token = TokenStore.get();
const payload = parseJwt(token);
document.getElementById('tokenBox').textContent =
  'uid: ' + uid +
  '\nroles: ' + JSON.stringify(roles) +
  '\n过期时间: ' + new Date(payload.exp * 1000).toLocaleString() +
  '\n\ntoken: ' + token;

// 管理员加载用户列表
if (roles.includes('ROLE_ADMIN')) {
  document.getElementById('userListPanel').style.display = 'block';
  loadUsers();
}

const ALL_ROLES = ['ROLE_ADMIN', 'ROLE_CREATOR', 'ROLE_UPDATER', 'ROLE_DELETER'];

async function loadUsers() {
  const tbody = document.getElementById('userTableBody');
  const msg = document.getElementById('message');
  try {
    const resp = await api('/api/versions/1/users');
    if (!resp.ok) {
      showMessage(msg, '加载失败（HTTP ' + resp.status + '）', 'error');
      return;
    }
    const users = await resp.json();
    if (!users.length) {
      document.getElementById('emptyTip').style.display = 'block';
      return;
    }
    tbody.innerHTML = users.map(u => {
      const userRoles = u.roles || [];
      const chips = ALL_ROLES.map(r => {
        const active = userRoles.includes(r);
        return '<span class="role-chip' + (active ? ' active' : '') + '" title="' +
          (active ? '点击撤权 ' : '点击授权 ') + r + '" onclick="toggleRole(\'' +
          escapeHtml(u.uid) + '\',\'' + r + '\',' + active + ')">' +
          r.replace('ROLE_', '') + '</span>';
      }).join('');
      return '<tr><td>' + escapeHtml(u.uid) + '</td><td>' + escapeHtml(u.fullName || '') +
        '</td><td>' + escapeHtml(u.lastName || '') + '</td><td class="role-cell">' + chips + '</td></tr>';
    }).join('');
  } catch (e) {
    if (e.message !== '登录已过期') showMessage(msg, '网络错误', 'error');
  }
}

/* ===== 授权 / 撤权 ===== */
async function toggleRole(targetUid, role, currentlyActive) {
  const msg = document.getElementById('message');

  // 防误操作：不允许撤销自己的 ROLE_ADMIN，否则当前管理员会把自己锁死
  if (targetUid === uid && role === 'ROLE_ADMIN' && currentlyActive) {
    showMessage(msg, '不能撤销自己的 ROLE_ADMIN 权限', 'error');
    return;
  }

  try {
    const resp = await api('/api/versions/1/roles?eRole=' + role, {
      method: currentlyActive ? 'DELETE' : 'PUT',
      body: JSON.stringify({ userDns: ['uid=' + targetUid + ',ou=users'] })
    });
    if (resp.ok) {
      showMessage(msg, (currentlyActive ? '已撤销 ' : '已授权 ') + targetUid + ' 的 ' + role + '，对方重新登录后生效', 'success');
      loadUsers();
    } else {
      showMessage(msg, '操作失败（HTTP ' + resp.status + '）', 'error');
    }
  } catch (e) {
    if (e.message !== '登录已过期') showMessage(msg, '网络错误', 'error');
  }
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/* ===== 修改密码 ===== */
function openPasswordModal() {
  hideMessage(document.getElementById('pwdMessage'));
  document.getElementById('oldPassword').value = '';
  document.getElementById('newPassword').value = '';
  document.getElementById('confirmNewPassword').value = '';
  document.getElementById('passwordModal').classList.add('show');
}

function closePasswordModal() {
  document.getElementById('passwordModal').classList.remove('show');
}

async function submitPassword() {
  const msg = document.getElementById('pwdMessage');
  const btn = document.getElementById('pwdSubmitBtn');
  hideMessage(msg);

  const oldPassword = document.getElementById('oldPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  const confirmNewPassword = document.getElementById('confirmNewPassword').value;

  if (!oldPassword) { showMessage(msg, '请输入旧密码', 'error'); return; }
  if (newPassword.length < 6) { showMessage(msg, '新密码长度至少 6 位', 'error'); return; }
  if (newPassword !== confirmNewPassword) { showMessage(msg, '两次输入的新密码不一致', 'error'); return; }

  btn.disabled = true;
  try {
    const resp = await api('/api/versions/1/user/' + encodeURIComponent(uid) + '/password', {
      method: 'PUT',
      body: JSON.stringify({ oldPassword, newPassword })
    });
    if (resp.status === 204) {
      showMessage(msg, '修改成功！请使用新密码重新登录', 'success');
      setTimeout(() => logout(), 1500);
    } else if (resp.status === 401) {
      showMessage(msg, '旧密码不正确', 'error');
    } else {
      showMessage(msg, '修改失败（HTTP ' + resp.status + '）', 'error');
    }
  } catch (e) {
    if (e.message !== '登录已过期') showMessage(msg, '网络错误', 'error');
  } finally {
    btn.disabled = false;
  }
}
