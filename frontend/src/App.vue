<template>
  <div id="app-container" :class="'theme-' + currentTheme" @mousemove="onMouseMove" @click="onClick">
    <!-- 鼠标光晕 -->
    <div class="mouse-glow" :style="glowStyle"></div>

    <!-- 注册页 -->
    <RegisterPage v-if="!authUser && showRegister" @register-success="showRegister = false" @switch-to-login="showRegister = false" />

    <!-- 登录页 -->
    <LoginPage v-else-if="!authUser" @login-success="onLogin" @switch-to-register="showRegister = true" />

    <!-- 已登录：主页或图谱或个人页 -->
    <template v-else>
      <UserProfile v-if="showProfile" :current-tree-id="currentTreeId" @back="showProfile = false" @logout="handleLogout" />
      <HomePage v-else-if="!currentTreeId" :current-theme="currentTheme" @enter-tree="enterTree" @select-theme="selectTheme" @go-profile="showProfile = true" />
      <FamilyTreeView v-else :tree-id="currentTreeId" :tree-template="currentTreeTemplate" @back="goBack" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import LoginPage from './components/LoginPage.vue'
import RegisterPage from './components/RegisterPage.vue'
import HomePage from './components/HomePage.vue'
import FamilyTreeView from './views/FamilyTreeView.vue'
import UserProfile from './components/UserProfile.vue'
import { setTreeId } from './api/family.js'

const authUser = ref(null)
const showRegister = ref(false)
const showProfile = ref(false)
const currentTreeId = ref(null)
const currentTheme = ref('blue')

// 鼠标光晕
const mouseX = ref(0)
const mouseY = ref(0)
const glowStyle = computed(() => ({
  left: mouseX.value + 'px',
  top: mouseY.value + 'px'
}))

function onMouseMove(e) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

// 点击火花四溅特效
function onClick(e) {
  const canvas = document.createElement('canvas')
  canvas.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:99998'
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  document.body.appendChild(canvas)

  const ctx = canvas.getContext('2d')
  const cx = e.clientX, cy = e.clientY

  // 彩虹色
  const RAINBOW = ['#FF0000','#FF7F00','#FFFF00','#00FF00','#0000FF','#4B0082','#8B00FF']
  const burstColor = RAINBOW[Math.floor(Math.random() * RAINBOW.length)] // 每次点击随机一种纯色
  // 生成随机火花粒子
  const count = 30 + Math.floor(Math.random() * 30) // 30~60 粒
  const sparks = []
  for (let i = 0; i < count; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 15 + Math.random() * 35        // 15~50px 飞行速度
    const curve = (Math.random() - 0.5) * 0.3
    const size = 1 + Math.random() * 2
    const delay = Math.random() * 0.08
    sparks.push({ angle, speed, curve, size, delay })
  }

  // 中心闪光
  const flash = document.createElement('div')
  flash.style.cssText = `position:fixed;left:${cx-6}px;top:${cy-6}px;width:12px;height:12px;border-radius:50%;background:${burstColor};pointer-events:none;z-index:99999;opacity:0.8;transition:all 0.25s ease-out`
  document.body.appendChild(flash)
  setTimeout(() => { flash.style.transform = 'scale(3)'; flash.style.opacity = '0' }, 10)
  setTimeout(() => { if (flash.parentNode) flash.parentNode.removeChild(flash) }, 300)

  let start = null

  function draw(timestamp) {
    if (!start) start = timestamp
    const elapsed = (timestamp - start) / 280  // 0.28s 更快
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (const s of sparks) {
      const t = Math.max(0, Math.min(1, (elapsed - s.delay) / (1 - s.delay)))
      if (t <= 0) continue

      // 迸发弧线
      const a = s.angle + s.curve * t * Math.PI
      // 先快后慢（ease-out 感觉）
      const ease = 1 - Math.pow(1 - t, 1.5)
      const r = ease * s.speed
      const x = cx + Math.cos(a) * r
      const y = cy + Math.sin(a) * r - ease * ease * 10

      const alpha = Math.max(0, (1 - t) * 1.2)
      const radius = s.size * (1 - t * 0.2)

      // 外发光
      ctx.beginPath()
      ctx.arc(x, y, radius + 2, 0, Math.PI * 2)
      ctx.fillStyle = burstColor + Math.round(alpha * 0.25 * 255).toString(16).padStart(2,'0')
      ctx.fill()

      // 核心亮点
      ctx.beginPath()
      ctx.arc(x, y, radius, 0, Math.PI * 2)
      ctx.fillStyle = burstColor + Math.round(alpha * 255).toString(16).padStart(2,'0')
      ctx.fill()

      // 拖尾细线
      if (t > 0.05) {
        const pt = t - 0.05
        const pa = s.angle + s.curve * pt * Math.PI
        const pr = pt * s.speed
        const px = cx + Math.cos(pa) * pr
        const py = cy + Math.sin(pa) * pr - pt * pt * 15
        ctx.beginPath()
        ctx.moveTo(px, py)
        ctx.lineTo(x, y)
        ctx.strokeStyle = burstColor + Math.round(alpha * 0.4 * 255).toString(16).padStart(2,'0')
        ctx.lineWidth = 0.8
        ctx.stroke()
      }
    }

    if (elapsed < 1) {
      requestAnimationFrame(draw)
    } else {
      document.body.removeChild(canvas)
    }
  }
  requestAnimationFrame(draw)
}

onMounted(() => {
  const saved = localStorage.getItem('auth-user')
  if (saved) {
    try { authUser.value = JSON.parse(saved) } catch {}
  }
  const theme = localStorage.getItem('family-tree-theme')
  if (theme) currentTheme.value = theme
})

function onLogin(userData) {
  authUser.value = userData
  showRegister.value = false
}

function handleLogout() {
  authUser.value = null
  showProfile.value = false
}

function enterTree(tree) {
  setTreeId(tree.id)
  currentTreeId.value = tree.id
  currentTreeTemplate.value = tree.template || 'family'
}

const currentTreeTemplate = ref('')

function goBack() {
  setTreeId(null)
  currentTreeId.value = null
}

function selectTheme(name) {
  currentTheme.value = name
  localStorage.setItem('family-tree-theme', name)
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow-x: hidden;
}

/* 主题变量 */
.theme-blue  { --primary: #409EFF; --primary-dark: #337ecc; --primary-light: #e6f0ff; --bg: #f0f5ff; --card-bg: #fff; --text: #303133; --text-secondary: #909399; --border: #e4e7ed; --shadow: rgba(0,0,0,0.06); }
.theme-green { --primary: #67C23A; --primary-dark: #529b2e; --primary-light: #e8f8e0; --bg: #f0f9eb; --card-bg: #fff; --text: #303133; --text-secondary: #909399; --border: #e4e7ed; --shadow: rgba(0,0,0,0.06); }
.theme-orange { --primary: #E6A23C; --primary-dark: #b88230; --primary-light: #fdf0d9; --bg: #fdf6ec; --card-bg: #fff; --text: #303133; --text-secondary: #909399; --border: #e4e7ed; --shadow: rgba(0,0,0,0.06); }
.theme-purple { --primary: #9B59B6; --primary-dark: #7d3c98; --primary-light: #f0e6f5; --bg: #f5f0fa; --card-bg: #fff; --text: #303133; --text-secondary: #909399; --border: #e4e7ed; --shadow: rgba(0,0,0,0.06); }
.theme-dark { --primary: #409EFF; --primary-dark: #337ecc; --primary-light: #1a3a5c; --bg: #1a1a2e; --card-bg: #16213e; --text: #e0e0e0; --text-secondary: #a0a0a0; --border: #2a2a4a; --shadow: rgba(0,0,0,0.2); }

#app-container {
  width: 100vw; height: 100vh; overflow: hidden;
  background: var(--bg); color: var(--text);
  transition: background 0.4s, color 0.4s;
  position: relative;
}

/* 鼠标光晕 */
.mouse-glow {
  position: fixed;
  pointer-events: none;
  z-index: 99999;
  width: 200px; height: 200px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(103,194,58,0.25) 0%, rgba(103,194,58,0.08) 30%, transparent 60%);
  transition: none;
}
</style>
