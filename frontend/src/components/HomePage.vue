<template>
  <div class="home-page">
    <div class="home-header">
      <div class="home-header-top">
        <h1 class="home-title">图谱构建</h1>
        <div class="home-user-area">
          <div class="home-avatar" @click="$emit('go-profile')" :title="'个人信息'">
            <img v-if="userAvatar" :src="userAvatar" class="home-avatar-img" />
            <span v-else class="home-avatar-text" :style="{ background: avatarBg }">{{ firstChar }}</span>
          </div>
          <span class="home-username">{{ userName }}</span>
        </div>
      </div>
      <p class="home-subtitle">选择一个图谱进入，或创建新的图谱</p>
    </div>

    <!-- 主题选择器 -->
    <div class="theme-picker">
      <span class="theme-picker-label">主题：</span>
      <div
        v-for="t in themes" :key="t.name"
        class="theme-dot"
        :class="{ active: currentTheme === t.name }"
        :style="{ background: t.color }"
        :title="t.label"
        @click="$emit('select-theme', t.name)"
      >
        <span v-if="currentTheme === t.name" class="theme-check">✓</span>
      </div>
    </div>

    <div class="tree-list">
      <div v-for="tree in trees" :key="tree.id" class="tree-card" @click="enterTree(tree)">
        <div class="tree-card-name">{{ tree.name }}</div>
        <div v-if="tree.description" class="tree-card-desc">{{ tree.description }}</div>
        <div class="tree-card-time">
          <el-tag size="small"
            :type="tree.template === 'er' ? 'success' : tree.template === 'aoe' ? 'warning' : 'primary'"
            style="margin-right:6px">
            {{ tree.template === 'er' ? 'ER图' : tree.template === 'aoe' ? 'AOE网' : '族谱' }}
          </el-tag>
          {{ formatDate(tree.createdAt) }}
        </div>
        <div class="tree-card-actions">
          <el-button size="small" type="danger" plain @click.stop="handleDelete(tree)">删除</el-button>
        </div>
      </div>

      <div class="tree-card tree-card-add" @click="showCreate = true">
        <div class="tree-card-add-icon">+</div>
        <div class="tree-card-add-text">创建新图谱</div>
      </div>

      <div class="tree-card tree-card-add tree-card-import" @click="triggerImport">
        <div class="tree-card-add-icon" style="font-size:32px">📂</div>
        <div class="tree-card-add-text">导入 .mygraph</div>
      </div>
    </div>
    <input ref="importInputRef" type="file" accept=".mygraph" style="display:none" @change="handleImport" />

    <el-dialog v-model="showCreate" title="创建新图谱" width="400px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="newTreeName" placeholder="输入图谱名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="模板">
          <el-radio-group v-model="newTreeTemplate">
            <el-radio value="family">
              <span style="font-weight:600">族谱</span>
              <span style="color:#909399;font-size:12px;margin-left:4px">— 自动计算五代内亲属关系</span>
            </el-radio>
            <br />
            <el-radio value="er">
              <span style="font-weight:600">ER图</span>
              <span style="color:#909399;font-size:12px;margin-left:4px">— 自由创建实体与关系，无自动计算</span>
            </el-radio>
            <br />
            <el-radio value="aoe">
              <span style="font-weight:600">AOE网</span>
              <span style="color:#909399;font-size:12px;margin-left:4px">— 关键路径与拓扑排序</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newTreeDesc" placeholder="可选描述" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :disabled="!newTreeName.trim()">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDelete" title="删除图谱" width="380px">
      <p>确定删除图谱「{{ deleteTarget?.name }}」吗？图谱内所有数据将不可恢复。</p>
      <template #footer>
        <el-button @click="showDelete = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确定删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/family.js'

const props = defineProps({
  currentTheme: { type: String, default: 'blue' }
})
const emit = defineEmits(['enter-tree', 'select-theme', 'go-profile'])

const userData = JSON.parse(localStorage.getItem('auth-user') || '{}')
const userName = userData.username || '用户'
const userAvatar = userData.avatar ? '/uploads/avatars/' + userData.avatar : ''

const firstChar = computed(() => {
  const name = userName || '?'
  const c = name.charAt(0)
  return (c >= '一' && c <= '鿿') ? c : c.toUpperCase()
})

const avatarBg = computed(() => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59B6', '#00BCD4']
  let hash = 0
  for (let i = 0; i < userName.length; i++) hash += userName.charCodeAt(i)
  return colors[hash % colors.length]
})

const themes = [
  { name: 'blue', label: '默认蓝', color: '#409EFF' },
  { name: 'green', label: '翠绿', color: '#67C23A' },
  { name: 'orange', label: '暖橙', color: '#E6A23C' },
  { name: 'purple', label: '薰衣草', color: '#9B59B6' },
  { name: 'dark', label: '暗夜', color: '#1a1a2e' }
]

const trees = ref([])
const showCreate = ref(false)
const newTreeName = ref('')
const newTreeDesc = ref('')
const newTreeTemplate = ref('family')
const showDelete = ref(false)
const deleteTarget = ref(null)
const importInputRef = ref(null)
function triggerImport() { importInputRef.value?.click() }
async function handleImport(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const json = JSON.parse(text)
    await api.post('/import-tree', json)
    ElMessage.success('导入成功')
    await loadTrees()
  } catch (err) {
    ElMessage.error('导入失败：' + (err.response?.data?.error || err.message || '格式错误'))
  }
  e.target.value = ''
}

async function loadTrees() {
  try {
    const res = await api.getTrees()
    trees.value = res.data
  } catch (e) {
    ElMessage.error('加载图谱列表失败')
  }
}

function enterTree(tree) {
  emit('enter-tree', tree)
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

async function handleCreate() {
  if (!newTreeName.value.trim()) return
  try {
    const res = await api.createTree(newTreeName.value.trim(), newTreeDesc.value.trim(), newTreeTemplate.value)
    trees.value.push(res.data)
    showCreate.value = false
    newTreeName.value = ''
    newTreeDesc.value = ''
    newTreeTemplate.value = 'family'
    ElMessage.success('创建成功')
  } catch (e) {
    const msg = e.response?.data?.error || e.response?.status || e.message || '未知错误'
    console.error('创建图谱失败:', e)
    ElMessage.error(`创建失败: ${msg}`)
  }
}

function handleDelete(tree) {
  deleteTarget.value = tree
  showDelete.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await api.deleteTree(deleteTarget.value.id)
    trees.value = trees.value.filter(t => t.id !== deleteTarget.value.id)
    showDelete.value = false
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}



onMounted(loadTrees)
</script>

<style scoped>
.home-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 48px 24px;
}

.home-header {
  text-align: center;
  margin-bottom: 32px;
}

.home-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.home-user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.home-avatar {
  width: 40px; height: 40px; border-radius: 50%; overflow: hidden;
  cursor: pointer; transition: transform 0.3s; flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}
.home-avatar:hover { transform: scale(1.15); }

.home-avatar-img { width: 100%; height: 100%; object-fit: cover; }

.home-avatar-text {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: center; font-size: 18px; font-weight: bold; color: #fff;
}

.home-username {
  font-size: 14px; color: var(--text-secondary, #909399);
}

.home-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--text, #303133);
  margin: 0 0 8px;
}

.home-subtitle {
  font-size: 16px;
  color: var(--text-secondary, #909399);
  margin: 0;
}

/* 主题选择器 */
.theme-picker {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 36px;
}

.theme-picker-label {
  font-size: 14px;
  color: var(--text-secondary, #909399);
  margin-right: 4px;
}

.theme-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  border: 3px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
  transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.theme-dot:hover {
  transform: scale(1.2);
  box-shadow: 0 3px 12px rgba(0,0,0,0.2);
}

.theme-dot.active {
  border-color: var(--text, #303133);
  transform: scale(1.15);
}

.theme-check {
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
}

.tree-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.tree-card {
  background: var(--card-bg, #fff);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px var(--shadow, rgba(0,0,0,0.06));
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.tree-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px var(--shadow, rgba(0,0,0,0.1));
  border-color: var(--primary, #409EFF);
}

.tree-card-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text, #303133);
  margin-bottom: 8px;
}

.tree-card-desc {
  font-size: 14px;
  color: var(--text-secondary, #606266);
  margin-bottom: 8px;
}

.tree-card-time {
  font-size: 12px;
  color: var(--text-secondary, #C0C4CC);
}

.tree-card-actions {
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-card:hover .tree-card-actions {
  opacity: 1;
}

.tree-card-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  border: 2px dashed var(--border, #dcdfe6);
  background: var(--card-bg, #fafafa);
}

.tree-card-add:hover {
  border-color: var(--primary, #409EFF);
  background: var(--primary-light, #f0f5ff);
}

.tree-card-add-icon {
  font-size: 48px;
  color: var(--text-secondary, #C0C4CC);
  line-height: 1;
  margin-bottom: 8px;
}

.tree-card-add-text {
  font-size: 16px;
  color: var(--text-secondary, #909399);
}
</style>
