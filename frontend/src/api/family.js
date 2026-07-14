import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 自动附加 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('auth-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401 时清除登录状态，跳转登录页
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('auth-token')
      localStorage.removeItem('auth-user')
      window.location.href = '/'
    }
    return Promise.reject(err)
  }
)

// 设置当前家族树 ID（所有请求自动带上 ?treeId=X）
export function setTreeId(treeId) {
  api.defaults.params = api.defaults.params || {}
  api.defaults.params.treeId = treeId || undefined
}

export default {
  // ======= 人物管理 =======
  getPersons(params) {
    return api.get('/persons', params || {})
  },
  getPerson(id) {
    return api.get(`/persons/${id}`)
  },
  createPerson(name, gender, treeId, entityType) {
    return api.post('/persons', { name, gender, treeId, entityType })
  },
  updatePerson(id, data) {
    return api.put(`/persons/${id}`, data)
  },
  deletePerson(id) {
    return api.delete(`/persons/${id}`)
  },
  updateSiblingRank(id, rank) {
    return api.put(`/persons/${id}/sibling-rank`, { rank })
  },
  clearAll() {
    return api.delete('/family/clear')
  },
  recomputeAll() {
    return api.post('/family/recompute')
  },
  createErRelation(entityA, entityB, relName, cardA, cardB, treeId) {
    return api.post('/family/create-er-relation', { entityA, entityB, relName, cardA, cardB, treeId })
  },
  uploadAvatar(personId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/upload-avatar/${personId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAvatar(personId) {
    return api.delete(`/upload-avatar/${personId}`)
  },

  // ======= 关系管理 =======
  getRelationships() {
    return api.get('/relationships')
  },
  createRelationship(person1Id, person2Id, type, customLabel, cardinality, duration) {
    return api.post('/relationships', { person1Id, person2Id, type, customLabel, cardinality, duration })
  },
  deleteRelationship(id) {
    return api.delete(`/relationships/${id}`)
  },

  // ======= 关系计算 =======
  calculateRelationship(p1, p2) {
    return api.get('/family/calculate-relationship', { params: { p1, p2 } })
  },
  getGraphData() {
    return api.get('/family/graph')
  },
  autoConnect(name, gender, connectedPersonId, relationType, treeId) {
    return api.post('/family/auto-connect', { name, gender, connectedPersonId, relationType, treeId })
  },
  updateComputedRelationship(id, label) {
    return api.put(`/family/computed-relationships/${id}`, { label })
  },
  aiQueryRelationship(label) {
    return api.post('/ai/query-relationship', { label })
  },
  // 家族树
  getTrees() {
    return api.get('/trees')
  },
  createTree(name, description, template) {
    return api.post('/trees', { name, description, template })
  },
  deleteTree(id) {
    return api.delete(`/trees/${id}`)
  },
  // 认证
  login(username, password) {
    return api.post('/auth/login', { username, password })
  },
  register(username, password) {
    return api.post('/auth/register', { username, password })
  },
  logout() {
    return api.post('/auth/logout')
  },
  // 直接 HTTP 请求（用于导出导入等无需预定义方法的场景）
  get(url) { return api.get(url) },
  post(url, data) { return api.post(url, data) }
}
