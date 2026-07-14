<template>
  <div class="info-panel-content">
    <div v-if="selectedPerson" class="section">
      <div class="section-title">人物信息</div>

      <!-- 头像区域 - 族谱模式 -->
      <template v-if="!isER">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerUpload">
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
            <div v-else class="avatar" :class="selectedPerson.gender === 'MALE' ? 'male' : 'female'">
              {{ firstChar }}
            </div>
            <div class="avatar-overlay">
              <el-icon style="color:#fff;font-size:22px"><Camera /></el-icon>
              <span style="color:#fff;font-size:14px;margin-top:4px">更换头像</span>
            </div>
          </div>
          <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
          <el-button v-if="avatarUrl" text size="small" type="danger" @click="handleRemoveAvatar" style="margin-top:6px">删除头像</el-button>
        </div>
      </template>

      <!-- 编辑表单 -->
      <el-form :model="form" label-width="70px" size="small" class="info-form">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <!-- ER 模式只有名称 -->
        <template v-if="!isER">
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthDate" type="date" placeholder="选择日期" format="YYYY-MM-DD"
              value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="年龄">
            <el-input :model-value="calculatedAge" disabled />
          </el-form-item>
          <el-form-item label="爱好">
            <el-input v-model="form.hobby" placeholder="兴趣爱好" />
          </el-form-item>
          <el-form-item label="学历">
            <el-select v-model="form.education" placeholder="选择学历" style="width:100%">
              <el-option label="博士" value="博士" />
              <el-option label="硕士" value="硕士" />
              <el-option label="本科" value="本科" />
              <el-option label="大专" value="大专" />
              <el-option label="高中" value="高中" />
              <el-option label="初中" value="初中" />
              <el-option label="小学" value="小学" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="职业">
            <el-input v-model="form.profession" placeholder="职业" />
          </el-form-item>
          <el-form-item label="家庭住址">
            <el-input v-model="form.address" placeholder="家庭住址" />
          </el-form-item>
        </template>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" size="small" @click="handleSave" :loading="saving" style="width:100%">
          保存修改
        </el-button>
      </div>
    </div>

    <!-- 关系结果 -->
    <div v-if="relationshipResult" class="section">
      <div class="section-title">关系信息</div>
      <div class="relation-card">
        <div class="relation-label">{{ relationshipResult.label }}</div>
        <div class="relation-meta">
          <el-tag size="small" :type="tagType">{{ categoryLabel }}</el-tag>
          <el-tag size="small" v-if="relationshipResult.generationDiff !== 0" type="info">
            {{ genDiffLabel }}
          </el-tag>
        </div>
      </div>
    </div>

    <div v-if="!selectedPerson && !relationshipResult" class="section hint">
      <p>点击图谱中的人物查看信息</p>
      <p style="margin-top:8px;font-size:14px;">或使用「查看关系」计算任意两人间的关系</p>
    </div>

    <div class="actions">
      <el-button type="primary" size="small" @click="$emit('show-calc')" plain style="width:100%">
        查看任意两人关系
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import api from '../api/family.js'

const props = defineProps({
  selectedPerson: { type: Object, default: null },
  relationshipResult: { type: Object, default: null },
  persons: { type: Array, default: () => [] },
  mode: { type: String, default: 'family' }
})

const emit = defineEmits(['show-calc', 'avatar-changed', 'person-updated'])
const isER = computed(() => props.mode === 'er')

const fileInputRef = ref(null)
const saving = ref(false)
const form = ref({
  name: '',
  gender: 'MALE',
  birthDate: null,
  hobby: '',
  education: '',
  profession: '',
  address: '',
  avatar: null
})

// 同步选中人物到表单
watch(() => props.selectedPerson, (p) => {
  if (p) {
    form.value = {
      name: p.name || '',
      gender: p.gender || 'MALE',
      birthDate: p.birthDate || null,
      hobby: p.hobby || '',
      education: p.education || '',
      profession: p.profession || '',
      address: p.address || '',
      avatar: p.avatar || null
    }
  }
}, { immediate: true, deep: true })

const firstChar = computed(() => {
  const name = props.selectedPerson?.name || ''
  if (!name) return '?'
  const c = name.charAt(0)
  if (c >= '一' && c <= '鿿') return c
  return c.toUpperCase()
})

const avatarUrl = computed(() => {
  if (props.selectedPerson?.avatar) {
    return '/uploads/avatars/' + props.selectedPerson.avatar
  }
  return ''
})

const calculatedAge = computed(() => {
  const bd = form.value.birthDate
  if (!bd) return ''
  const birth = new Date(bd)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--
  return age + '岁'
})

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    await api.uploadAvatar(props.selectedPerson.id, file)
    ElMessage.success('头像已更新')
    emit('avatar-changed')
  } catch (err) {
    ElMessage.error('上传失败')
  }
  e.target.value = ''
}

async function handleRemoveAvatar() {
  try {
    await api.deleteAvatar(props.selectedPerson.id)
    ElMessage.success('头像已删除')
    emit('avatar-changed')
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    await api.updatePerson(props.selectedPerson.id, {
      name: form.value.name,
      gender: form.value.gender,
      birthDate: form.value.birthDate,
      hobby: form.value.hobby,
      education: form.value.education,
      profession: form.value.profession,
      address: form.value.address
    })
    ElMessage.success('保存成功')
    emit('person-updated')
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const categoryLabel = computed(() => {
  if (!props.relationshipResult) return ''
  switch (props.relationshipResult.category) {
    case 'BLOOD': return '血亲'
    case 'IN_LAW': return '姻亲'
    case 'SPOUSE': return '配偶'
    case 'SELF': return '自己'
    default: return '其他'
  }
})

const tagType = computed(() => {
  switch (props.relationshipResult?.category) {
    case 'BLOOD': return 'danger'
    case 'IN_LAW': return 'warning'
    case 'SPOUSE': return 'success'
    default: return 'info'
  }
})

const genDiffLabel = computed(() => {
  if (!props.relationshipResult) return ''
  const d = props.relationshipResult.generationDiff
  if (d > 0) return `长${d}辈`
  if (d < 0) return `晚${-d}辈`
  return '同辈'
})
</script>

<style scoped>
.info-panel-content {
  padding: 16px;
}

.section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  color: #909399;
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #f0f0f0;
}

.hint {
  color: #909399;
  text-align: center;
  padding: 32px 0;
  font-size: 15px;
}

/* 头像 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}
.avatar-wrapper:hover {
  transform: scale(1.08);
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  font-weight: bold;
  color: #fff;
}
.avatar.male { background: linear-gradient(135deg, #409EFF, #337ecc); }
.avatar.female { background: linear-gradient(135deg, #F56C6C, #d94a4a); }

.avatar-overlay {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  border-radius: 50%;
  background: rgba(0,0,0,0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s, transform 0.3s;
  transform: scale(0.8);
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
  transform: scale(1);
}

/* 表单 */
.info-form {
  margin-bottom: 12px;
}
.info-form .el-form-item {
  margin-bottom: 12px;
}

.form-actions {
  margin-bottom: 16px;
}

/* 关系卡片 */
.relation-card {
  padding: 16px;
  background: #f0f9eb;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
  cursor: default;
}
.relation-card:hover {
  background: #e6f7e6;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transform: translateY(-1px);
}

.relation-label {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.relation-meta {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.actions {
  margin-top: 16px;
}
.actions .el-button {
  transition: all 0.3s !important;
}
.actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64,158,255,0.3);
}
</style>
