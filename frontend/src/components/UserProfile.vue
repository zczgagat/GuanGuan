<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="profile-header">
        <div class="profile-avatar-wrapper" @click="triggerUpload">
          <img v-if="avatarUrl" :src="avatarUrl" class="profile-avatar-img" />
          <div v-else class="profile-avatar-text" :style="{ background: avatarBg }">{{ firstChar }}</div>
          <div class="profile-avatar-overlay">更换</div>
        </div>
        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFile" />
        <div class="profile-name">{{ form.username }}</div>
      </div>

      <el-form label-width="100px" size="large">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="原密码">
          <el-input v-model="form.oldPassword" type="password" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" placeholder="至少3位" />
        </el-form-item>
      </el-form>

      <div class="profile-actions">
        <el-button type="primary" @click="handleSave" :loading="saving" style="width:100%">保存修改</el-button>
      </div>

      <el-divider />

      <div class="profile-section-title">图谱导入导出</div>
      <div class="import-export-row">
        <el-button type="success" @click="handleExport" :disabled="!currentTreeId" plain>导出当前图谱 .mygraph</el-button>
        <el-button type="warning" @click="triggerImport" plain>导入 .mygraph 文件</el-button>
      </div>
      <p v-if="exportStatus" class="import-export-status">{{ exportStatus }}</p>
      <input ref="importInput" type="file" accept=".mygraph" style="display:none" @change="handleImport" />

      <el-divider />

      <el-button text type="danger" @click="handleLogout" style="width:100%">退出登录</el-button>
      <el-button text @click="$emit('back')" style="width:100%;margin-top:8px">← 返回主页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/family.js'

const emit = defineEmits(['back', 'logout'])

const props = defineProps({
  currentTreeId: { type: Number, default: null }
})

const fileInput = ref(null)
const importInput = ref(null)
const saving = ref(false)
const exportStatus = ref('')

const userData = JSON.parse(localStorage.getItem('auth-user') || '{}')

const form = ref({
  username: userData.username || '',
  oldPassword: '',
  newPassword: ''
})

const avatarUrl = ref(userData.avatar ? '/uploads/avatars/' + userData.avatar : '')

const firstChar = computed(() => {
  const name = form.value.username || '?'
  const c = name.charAt(0)
  return (c >= '一' && c <= '鿿') ? c : c.toUpperCase()
})

const avatarBg = computed(() => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59B6', '#00BCD4']
  let hash = 0
  for (let i = 0; i < (form.value.username || '').length; i++) hash += form.value.username.charCodeAt(i)
  return colors[hash % colors.length]
})

function triggerUpload() { fileInput.value?.click() }
function triggerImport() { importInput.value?.click() }

async function handleFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post('/auth/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    avatarUrl.value = '/uploads/avatars/' + res.data.avatar
    userData.avatar = res.data.avatar
    localStorage.setItem('auth-user', JSON.stringify(userData))
    ElMessage.success('头像已更新')
  } catch { ElMessage.error('上传失败') }
  e.target.value = ''
}

async function handleSave() {
  saving.value = true
  try {
    // 修改用户名
    if (form.value.username !== userData.username) {
      const res = await api.put('/auth/username', { username: form.value.username })
      userData.username = res.data.username
      localStorage.setItem('auth-user', JSON.stringify(userData))
    }
    // 修改密码
    if (form.value.oldPassword && form.value.newPassword) {
      await api.put('/auth/password', { oldPassword: form.value.oldPassword, newPassword: form.value.newPassword })
      form.value.oldPassword = ''
      form.value.newPassword = ''
    }
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  } finally { saving.value = false }
}

async function handleExport() {
  if (!props.currentTreeId) { ElMessage.warning('请先进入一个图谱'); return }
  exportStatus.value = '正在导出...'
  try {
    const res = await api.get(`/export-tree/${props.currentTreeId}`)
    const data = res.data
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (data.treeName || '图谱') + '.mygraph'
    a.click()
    URL.revokeObjectURL(url)
    exportStatus.value = '导出成功！'
  } catch { exportStatus.value = '导出失败' }
}

async function handleImport(e) {
  const file = e.target.files?.[0]
  if (!file) return
  exportStatus.value = '正在导入...'
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    await api.post('/import-tree', data)
    ElMessage.success('导入成功')
    exportStatus.value = '导入成功！'
    emit('back') // 返回主页刷新列表
  } catch (err) {
    exportStatus.value = '导入失败：' + (err.response?.data?.error || err.message || '格式错误')
  }
  e.target.value = ''
}

function handleLogout() {
  localStorage.removeItem('auth-token')
  localStorage.removeItem('auth-user')
  emit('logout')
}
</script>

<style scoped>
.profile-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg, #f5f7fa);
}

.profile-card {
  background: var(--card-bg, #fff);
  border-radius: 16px; padding: 40px; width: 480px;
  box-shadow: 0 4px 20px var(--shadow, rgba(0,0,0,0.08));
}

.profile-header {
  display: flex; flex-direction: column; align-items: center;
  margin-bottom: 28px;
}

.profile-avatar-wrapper {
  width: 80px; height: 80px; border-radius: 50%; overflow: hidden;
  cursor: pointer; position: relative; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  transition: transform 0.3s;
}
.profile-avatar-wrapper:hover { transform: scale(1.08); }

.profile-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.profile-avatar-text {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: center; font-size: 32px; font-weight: bold; color: #fff;
}

.profile-avatar-overlay {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.5); color: #fff; font-size: 13px;
  text-align: center; padding: 4px 0; opacity: 0; transition: opacity 0.3s;
}
.profile-avatar-wrapper:hover .profile-avatar-overlay { opacity: 1; }

.profile-name { font-size: 20px; font-weight: 600; color: var(--text, #303133); }

.profile-actions { margin-top: 8px; }

.profile-section-title {
  font-size: 15px; font-weight: 600; color: var(--text, #303133); margin-bottom: 12px;
}

.import-export-row { display: flex; gap: 8px; flex-wrap: wrap; }
.import-export-status { font-size: 13px; color: var(--text-secondary, #909399); margin-top: 8px; }
</style>
