<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">注册账号</h1>
      <p class="auth-subtitle">创建你的图谱构建账号</p>

      <el-form :model="form" label-width="0" size="large" @submit.prevent="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（至少3位）" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="btn-glow" style="width:100%">注册</el-button>
        </el-form-item>
      </el-form>

      <div class="auth-link">
        已有账号？<a href="#" @click.prevent="$emit('switch-to-login')">登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../api/family.js'

const emit = defineEmits(['register-success', 'switch-to-login'])

const form = ref({ username: '', password: '', confirm: '' })
const loading = ref(false)

async function handleRegister() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.password.length < 3) {
    ElMessage.warning('密码至少3位')
    return
  }
  if (form.value.password !== form.value.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await api.register(form.value.username, form.value.password)
    ElMessage.success('注册成功，请登录')
    emit('register-success')
  } catch (e) {
    const msg = e.response?.data?.error || e.message || '注册失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.auth-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 28px 80px rgba(0,0,0,0.2);
}

.auth-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.auth-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 32px;
}

.auth-link {
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.auth-link a {
  color: #409EFF;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s;
}

.auth-link a:hover {
  color: #67C23A;
  text-decoration: underline;
}

.btn-glow {
  transition: all 0.3s ease !important;
}
.btn-glow:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64,158,255,0.4);
}

:deep(.el-input__wrapper) {
  transition: box-shadow 0.3s ease !important;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}
</style>
