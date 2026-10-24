<template>
  <div class="page-head">
    <h1>权限组管理</h1>
    <button class="btn btn-primary" @click="openCreate">+ 新建权限组</button>
  </div>

  <div v-if="error" class="alert alert-error">{{ error }}</div>

  <div class="card" style="padding: 0; overflow: hidden">
    <table class="table">
      <thead>
        <tr>
          <th>组名</th>
          <th>说明</th>
          <th>成员</th>
          <th style="text-align: right">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="g in groups" :key="g.name">
          <td>
            <b>{{ g.name }}</b>
            <span v-if="g.builtIn" class="badge">内置</span>
          </td>
          <td class="muted">{{ g.description || '—' }}</td>
          <td>
            <span v-for="m in g.members" :key="m" class="badge">{{ m }}</span>
            <span v-if="!g.members.length" class="muted">（空）</span>
          </td>
          <td>
            <div class="row-actions">
              <button class="btn btn-outline btn-sm" @click="openMembers(g)">成员管理</button>
              <button v-if="!g.builtIn" class="btn btn-danger btn-sm" @click="openDelete(g)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 新建组 -->
  <div v-if="createModal" class="modal-mask" @click.self="createModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>新建权限组</h3>
        <button class="modal-close" @click="createModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <div class="form-field">
          <label>组名</label>
          <input v-model="createForm.name" class="input" placeholder="如 OPS（自动转大写）" />
        </div>
        <div class="form-field">
          <label>说明</label>
          <input v-model="createForm.description" class="input" placeholder="可选" />
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="createModal = false">取消</button>
        <button class="btn btn-primary" :disabled="busy" @click="submitCreate">创建</button>
      </div>
    </div>
  </div>

  <!-- 成员管理 -->
  <div v-if="membersModal" class="modal-mask" @click.self="membersModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>成员管理 · {{ currentGroup?.name }}</h3>
        <button class="modal-close" @click="membersModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <div v-for="m in currentGroup?.members || []" :key="m" class="member-row">
          <span class="name">{{ m }}</span>
          <button class="btn btn-danger btn-sm" :disabled="busy" @click="removeMember(m)">移出</button>
        </div>
        <p v-if="!(currentGroup?.members || []).length" class="muted">暂无成员</p>
        <div class="form-field" style="margin-top: 16px">
          <label>添加成员</label>
          <div style="display: flex; gap: 8px">
            <select v-model="selectedUser" class="input">
              <option value="" disabled>选择用户</option>
              <option v-for="u in addableUsers" :key="u.username" :value="u.username">
                {{ u.username }}（{{ u.displayName || u.username }}）
              </option>
            </select>
            <button class="btn btn-primary btn-sm" :disabled="!selectedUser || busy" @click="addMember">添加</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 删除确认 -->
  <div v-if="deleteModal" class="modal-mask" @click.self="deleteModal = false">
    <div class="modal">
      <div class="modal-header">
        <h3>删除权限组</h3>
        <button class="modal-close" @click="deleteModal = false">×</button>
      </div>
      <div class="modal-body">
        <div v-if="modalError" class="alert alert-error">{{ modalError }}</div>
        <p>确定删除权限组 <b>{{ currentGroup?.name }}</b> 吗？组内 {{ currentGroup?.members.length || 0 }} 个成员关系会一并移除，用户本身保留。</p>
      </div>
      <div class="modal-footer">
        <button class="btn btn-outline" @click="deleteModal = false">取消</button>
        <button class="btn btn-danger" :disabled="busy" @click="submitDelete">确认删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const groups = ref([])
const users = ref([])
const error = ref('')
const modalError = ref('')
const busy = ref(false)

const createModal = ref(false)
const membersModal = ref(false)
const deleteModal = ref(false)
const currentGroup = ref(null)
const createForm = ref({ name: '', description: '' })
const selectedUser = ref('')

const addableUsers = computed(() =>
  users.value.filter((u) => !(currentGroup.value?.members || []).includes(u.username))
)

onMounted(load)

async function load() {
  error.value = ''
  const [g, u] = await Promise.all([
    auth.apiFetch('/api/admin/groups'),
    auth.apiFetch('/api/admin/users')
  ])
  if (g.status === 200 && u.status === 200) {
    groups.value = g.data
    users.value = u.data
    // keep the open members modal in sync after mutations
    if (membersModal.value && currentGroup.value) {
      currentGroup.value = g.data.find((x) => x.name === currentGroup.value.name) || null
    }
  } else {
    error.value = g.status === 403 ? '需要管理员组权限' : '加载失败'
  }
}

function openCreate() {
  createForm.value = { name: '', description: '' }
  modalError.value = ''
  createModal.value = true
}

function openMembers(g) {
  currentGroup.value = g
  selectedUser.value = ''
  modalError.value = ''
  membersModal.value = true
}

function openDelete(g) {
  currentGroup.value = g
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
  submit('/api/admin/groups', {
    method: 'POST',
    body: JSON.stringify(createForm.value)
  }, () => { createModal.value = false })
}

function addMember() {
  submit(`/api/admin/groups/${currentGroup.value.name}/members`, {
    method: 'POST',
    body: JSON.stringify({ username: selectedUser.value })
  }, () => { selectedUser.value = '' })
}

function removeMember(username) {
  submit(`/api/admin/groups/${currentGroup.value.name}/members/${username}`, {
    method: 'DELETE'
  }, () => {})
}

function submitDelete() {
  submit(`/api/admin/groups/${currentGroup.value.name}`, {
    method: 'DELETE'
  }, () => { deleteModal.value = false })
}
</script>
