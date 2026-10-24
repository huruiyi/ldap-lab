<template>
  <div class="page-head">
    <h1>用户管理</h1>
    <button class="btn btn-primary" @click="openCreate">+ 新建用户</button>
  </div>

  <div v-if="error" class="alert alert-error">{{ error }}</div>

  <div class="card" style="padding: 0; overflow: hidden">
    <table class="table">
      <thead>
        <tr>
          <th>用户名</th>
          <th>显示名</th>
          <th>状态</th>
          <th>权限组</th>
          <th style="text-align: right">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in users" :key="u.username">
          <td>
            <b>{{ u.username }}</b>
            <span v-if="u.protected" class="badge">内置</span>
          </td>
          <td>{{ u.displayName || '—' }}</td>
          <td>
            <span class="badge" :class="u.enabled ? 'success' : 'danger'">
              {{ u.enabled ? '启用' : '禁用' }}
            </span>
          </td>
          <td>
            <span v-for="g in u.groups" :key="g" class="badge">{{ g }}</span>
            <span v-if="!u.groups.length" class="muted">（无）</span>
          </td>
          <td>
            <div class="row-actions">
              <button class="btn btn-outline btn-sm" @click="openEdit(u)">编辑</button>
              <button class="btn btn-outline btn-sm" @click="openPassword(u)">重置密码</button>
              <button v-if="!u.protected" class="btn btn-danger btn-sm" @click="openDelete(u)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 新建用户 -->
  <div v-if="createModal" class="modal-mask" @click.self="createModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>新建用户</h3>
        <button class="modal-close" @click="createModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <div class="form-field">
          <label>用户名</label>
          <input v-model="createForm.username" class="input" placeholder="登录账号，字母开头" />
        </div>
        <div class="form-field">
          <label>密码</label>
          <input v-model="createForm.password" type="password" class="input" placeholder="至少 4 位" />
        </div>
        <div class="form-field">
          <label>显示名</label>
          <input v-model="createForm.displayName" class="input" placeholder="可选" />
        </div>
        <div class="form-field">
          <label>权限组</label>
          <label v-for="g in allGroups" :key="g.name" style="display: inline-flex; align-items: center; gap: 6px; margin-right: 16px; font-weight: 400">
            <input type="checkbox" :value="g.name" v-model="createForm.groups" /> {{ g.name }}
          </label>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="createModal = false">取消</button>
        <button class="btn btn-primary" :disabled="busy" @click="submitCreate">创建</button>
      </div>
    </div>
  </div>

  <!-- 编辑用户 -->
  <div v-if="editModal" class="modal-mask" @click.self="editModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>编辑用户 · {{ currentUser?.username }}</h3>
        <button class="modal-close" @click="editModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <div class="form-field">
          <label>显示名</label>
          <input v-model="editForm.displayName" class="input" />
        </div>
        <div class="form-field">
          <label style="display: inline-flex; align-items: center; gap: 6px; font-weight: 400">
            <input type="checkbox" v-model="editForm.enabled" :disabled="currentUser?.protected" /> 启用账号
          </label>
        </div>
        <div class="form-field">
          <label>权限组</label>
          <label v-for="g in allGroups" :key="g.name" style="display: inline-flex; align-items: center; gap: 6px; margin-right: 16px; font-weight: 400">
            <input type="checkbox" :value="g.name" v-model="editForm.groups" /> {{ g.name }}
          </label>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="editModal = false">取消</button>
        <button class="btn btn-primary" :disabled="busy" @click="submitEdit">保存</button>
      </div>
    </div>
  </div>

  <!-- 重置密码 -->
  <div v-if="passwordModal" class="modal-mask" @click.self="passwordModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>重置密码 · {{ currentUser?.username }}</h3>
        <button class="modal-close" @click="passwordModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <div class="form-field">
          <label>新密码</label>
          <input v-model="newPassword" type="password" class="input" placeholder="至少 4 位" />
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="passwordModal = false">取消</button>
        <button class="btn btn-primary" :disabled="busy" @click="submitPassword">重置</button>
      </div>
    </div>
  </div>

  <!-- 删除确认 -->
  <div v-if="deleteModal" class="modal-mask" @click.self="deleteModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>删除用户</h3>
        <button class="modal-close" @click="deleteModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <p>确定删除用户 <b>{{ currentUser?.username }}</b> 吗？其权限组成员关系会一并移除，此操作不可恢复。</p>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="deleteModal = false">取消</button>
        <button class="btn btn-danger" :disabled="busy" @click="submitDelete">确认删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const users = ref([])
const allGroups = ref([])
const error = ref('')
const modalError = ref('')
const busy = ref(false)

const createModal = ref(false)
const editModal = ref(false)
const passwordModal = ref(false)
const deleteModal = ref(false)
const currentUser = ref(null)
const createForm = ref({ username: '', password: '', displayName: '', groups: [] })
const editForm = ref({ displayName: '', enabled: true, groups: [] })
const newPassword = ref('')

onMounted(load)

async function load() {
  error.value = ''
  const [u, g] = await Promise.all([
    auth.apiFetch('/api/admin/users'),
    auth.apiFetch('/api/admin/groups')
  ])
  if (u.status === 200 && g.status === 200) {
    users.value = u.data
    allGroups.value = g.data
  } else {
    error.value = u.status === 403 ? '需要管理员组权限' : '加载失败'
  }
}

function openCreate() {
  createForm.value = { username: '', password: '', displayName: '', groups: ['USER'] }
  modalError.value = ''
  createModal.value = true
}

function openEdit(u) {
  currentUser.value = u
  editForm.value = { displayName: u.displayName || '', enabled: u.enabled, groups: [...u.groups] }
  modalError.value = ''
  editModal.value = true
}

function openPassword(u) {
  currentUser.value = u
  newPassword.value = ''
  modalError.value = ''
  passwordModal.value = true
}

function openDelete(u) {
  currentUser.value = u
  modalError.value = ''
  deleteModal.value = true
}

async function submit(path, options, onOk) {
  busy.value = true
  modalError.value = ''
  try {
    const r = await auth.apiFetch(path, options)
    if (r.status === 200) {
      onOk()
      await load()
    } else {
      modalError.value = r.data?.message || `操作失败（HTTP ${r.status}）`
    }
  } finally {
    busy.value = false
  }
}

function submitCreate() {
  submit('/api/admin/users', {
    method: 'POST',
    body: JSON.stringify(createForm.value)
  }, () => { createModal.value = false })
}

function submitEdit() {
  submit(`/api/admin/users/${currentUser.value.username}`, {
    method: 'PUT',
    body: JSON.stringify(editForm.value)
  }, () => { editModal.value = false })
}

function submitPassword() {
  submit(`/api/admin/users/${currentUser.value.username}/password`, {
    method: 'POST',
    body: JSON.stringify({ password: newPassword.value })
  }, () => { passwordModal.value = false })
}

function submitDelete() {
  submit(`/api/admin/users/${currentUser.value.username}`, {
    method: 'DELETE'
  }, () => { deleteModal.value = false })
}
</script>
