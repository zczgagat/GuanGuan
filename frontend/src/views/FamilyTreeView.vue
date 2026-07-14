<template>
  <div class="family-tree-view">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button text @click="$emit('back')" style="margin-right:8px;font-size:16px">
        ← 返回
      </el-button>
      <div class="toolbar-title">图谱构建</div>
      <div class="toolbar-actions">
        <!-- 族谱模式 -->
        <template v-if="isFamily">
          <el-button type="primary" @click="showAddPerson = true">
            <el-icon style="margin-right:4px"><Plus /></el-icon>添加人物
          </el-button>
          <el-button type="success" @click="showAddRelation = true" :disabled="persons.length < 2">
            <el-icon style="margin-right:4px"><Link /></el-icon>添加关系
          </el-button>
          <el-button type="warning" @click="showDeletePerson = true" :disabled="persons.length === 0">
            <el-icon style="margin-right:4px"><Delete /></el-icon>删除
          </el-button>
          <el-button type="danger" @click="handleClearAll" :disabled="persons.length === 0" plain>
            清空
          </el-button>
          <el-button type="info" @click="handleRefreshRelations" :disabled="persons.length < 2" plain>
            <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新关系
          </el-button>
        </template>
        <!-- ER 图模式 -->
        <template v-if="isEr">
          <el-button type="primary" @click="showAddPerson = true">
            <el-icon style="margin-right:4px"><Plus /></el-icon>添加实体
          </el-button>
          <el-button type="success" @click="showAddRelation = true" :disabled="persons.length < 2">
            <el-icon style="margin-right:4px"><Link /></el-icon>添加连线
          </el-button>
          <el-button type="warning" @click="showDeletePerson = true" :disabled="persons.length === 0">
            <el-icon style="margin-right:4px"><Delete /></el-icon>删除
          </el-button>
          <el-button type="danger" @click="handleClearAll" :disabled="persons.length === 0" plain>
            清空
          </el-button>
        </template>
        <!-- AOE 网模式 -->
        <template v-if="isAoe">
          <el-button type="primary" @click="showAddPerson = true">
            <el-icon style="margin-right:4px"><Plus /></el-icon>添加事件
          </el-button>
          <el-button type="success" @click="showAddRelation = true" :disabled="persons.length < 2">
            <el-icon style="margin-right:4px"><Link /></el-icon>添加活动
          </el-button>
          <el-button type="warning" @click="showDeletePerson = true" :disabled="persons.length === 0">
            <el-icon style="margin-right:4px"><Delete /></el-icon>删除
          </el-button>
          <el-button type="danger" @click="handleClearAll" :disabled="persons.length === 0" plain>
            清空
          </el-button>
          <el-button type="info" @click="calculateAoe" :disabled="persons.length < 2" plain>
            <el-icon style="margin-right:4px"><Refresh /></el-icon>计算关键路径
          </el-button>
        </template>
        <el-button type="success" @click="handleExport" plain :disabled="persons.length === 0">
          导出 .mygraph
        </el-button>
        <el-button type="warning" @click="triggerImport" plain>
          导入
        </el-button>
      </div>
      <input ref="importInputRef" type="file" accept=".mygraph" style="display:none" @change="handleImport" />
    </div>

    <!-- 主区域 -->
    <div class="main-area">
      <!-- 图谱区 -->
      <div class="graph-container" ref="graphContainer">
        <FamilyGraph
          :elements="graphElements"
          :selected-person-id="selectedPersonIdStr"
          :template="treeTemplate"
          @node-click="onNodeClick"
          @node-double-click="onNodeDoubleClick"
          @edge-click="onEdgeClick"
          @edge-double-click="onEdgeDoubleClick"
        />
        <!-- 空状态提示 -->
        <div v-if="persons.length === 0" class="empty-hint">
          <el-empty :description="isAoe ? '还没有事件，点击右上角「添加事件」开始' : isEr ? '还没有实体，点击右上角「添加实体」开始' : '还没有人物，点击右上角「添加人物」开始'" />
        </div>
      </div>

      <!-- 右侧信息面板 -->
      <Transition name="panel-slide">
        <div class="info-panel" v-if="selectedPerson || relationshipResult || edgeSelected || (isAoe && aoeData)">
        <!-- 人物信息 -->
        <RelationshipInfo
          v-if="!edgeSelected"
          :selected-person="selectedPerson"
          :relationship-result="relationshipResult"
          :persons="persons"
          :mode="treeTemplate"
          @show-calc="showRelationshipCalc = true"
          @avatar-changed="loadData"
          @person-updated="loadData"
        />
        <!-- 关系边信息 -->
        <div v-if="edgeSelected" class="edge-info-panel">
          <div class="section-title">关系信息</div>

          <!-- 方向 A → B -->
          <div class="edge-direction-card">
            <div class="edge-direction-row">
              <span class="edge-direction-person">{{ getPersonName(edgeSelected.source) }}</span>
              <!-- 双端基数 1:1 / 1:N / N:M -->
              <template v-if="edgeSelected.cardinality && edgeSelected.cardinality.includes(':')">
                <span class="edge-direction-cardinality">{{ edgeSelected.cardinality.split(':')[0] }}</span>
                <span class="edge-direction-arrow">→</span>
                <span class="edge-direction-cardinality">{{ edgeSelected.cardinality.split(':')[1] }}</span>
              </template>
              <!-- 单端基数 1 / N / M -->
              <template v-else>
                <span class="edge-direction-arrow">→</span>
                <span class="edge-direction-cardinality" v-if="edgeSelected.cardinality">{{ edgeSelected.cardinality }}</span>
              </template>
              <span class="edge-direction-person">{{ getPersonName(edgeSelected.target) }}</span>
              <span class="edge-direction-label">：{{ edgeSelected.label }}</span>
            </div>
            <el-button size="small" type="primary" plain
              @click="queryEdgeAI(edgeSelected.label)"
              :loading="edgeAiLoading === edgeSelected.label"
              style="margin-top:6px;width:100%">
              <el-icon style="margin-right:4px"><Monitor /></el-icon>
              AI 查询「{{ edgeSelected.label }}」
            </el-button>
            <div v-if="edgeAiResult && edgeAiFor === edgeSelected.label" class="edge-ai-result">
              {{ edgeAiResult }}
            </div>
          </div>

          <!-- 方向 B → A（如有反向标签） -->
          <div v-if="edgeSelected.reverseLabel" class="edge-direction-card" style="margin-top:12px">
            <div class="edge-direction-row">
              <span class="edge-direction-person">{{ getPersonName(edgeSelected.target) }}</span>
              <span class="edge-direction-arrow">→</span>
              <span class="edge-direction-person">{{ getPersonName(edgeSelected.source) }}</span>
              <span class="edge-direction-label">：{{ edgeSelected.reverseLabel }}</span>
            </div>
            <el-button size="small" type="primary" plain
              @click="queryEdgeAI(edgeSelected.reverseLabel)"
              :loading="edgeAiLoading === edgeSelected.reverseLabel"
              style="margin-top:6px;width:100%">
              <el-icon style="margin-right:4px"><Monitor /></el-icon>
              AI 查询「{{ edgeSelected.reverseLabel }}」
            </el-button>
            <div v-if="edgeAiResult && edgeAiFor === edgeSelected.reverseLabel" class="edge-ai-result">
              {{ edgeAiResult }}
            </div>
          </div>
        </div>
        <!-- AOE 网信息面板 -->
        <div v-if="isAoe && aoeData && !edgeSelected" class="aoe-panel">
          <div class="section-title">AOE 网络分析</div>
          <div class="aoe-summary">
            <div class="aoe-stat"><span>工期</span><b>{{ aoeData.projectDuration }}</b></div>
            <div class="aoe-stat"><span>事件数</span><b>{{ aoeData.nodes?.length || 0 }}</b></div>
            <div class="aoe-stat"><span>活动数</span><b>{{ aoeData.edges?.length || 0 }}</b></div>
          </div>
          <el-divider style="margin:12px 0" />
          <div class="aoe-section-title">关键路径</div>
          <div class="aoe-cp">{{ aoeData.criticalPath?.join(' → ') || '—' }}</div>
          <el-divider style="margin:12px 0" />
          <div class="aoe-section-title">拓扑排序</div>
          <div class="aoe-tp">{{ aoeData.topoSort?.join(' → ') || '—' }}</div>
          <el-divider style="margin:12px 0" />
          <div class="aoe-section-title">事件时间表</div>
          <div class="aoe-table-wrap">
            <table class="aoe-table">
              <thead><tr><th>事件</th><th>ES</th><th>EF</th><th>LS</th><th>LF</th><th>差</th></tr></thead><tbody>
              <tr v-for="n in aoeData.nodes" :key="n.id"
                :style="n.isCritical ? 'background:#fff3e0;font-weight:bold' : ''">
                <td>{{ n.name }}</td><td>{{ n.es }}</td><td>{{ n.ef }}</td>
                <td>{{ n.ls }}</td><td>{{ n.lf }}</td>
                <td>{{ n.slack }}</td>
              </tr>
              </tbody>
            </table>
          </div>
          <el-divider style="margin:12px 0" />
          <div class="aoe-section-title">活动表</div>
          <div class="aoe-table-wrap">
            <table class="aoe-table">
              <thead><tr><th>活动</th><th>起点</th><th>终点</th><th>耗时</th><th>关键</th></tr></thead>
              <tbody>
              <tr v-for="e in aoeData.edges" :key="e.id"
                :style="e.isCritical ? 'background:#fff3e0;font-weight:bold' : ''">
                <td>{{ e.label }}</td><td>{{ e.from }}</td><td>{{ e.to }}</td>
                <td>{{ e.duration }}</td>
                <td>{{ e.isCritical ? '✓' : '' }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      </Transition>
    </div>

    <!-- 关系计算弹窗 -->
    <el-dialog v-model="showRelationshipCalc" title="查看两人关系" width="450px">
      <el-form label-width="100px">
        <el-form-item label="选择人物A">
          <el-select v-model="calcPersonA" filterable placeholder="请选择" style="width:100%">
            <el-option v-for="p in persons" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择人物B">
          <el-select v-model="calcPersonB" filterable placeholder="请选择" style="width:100%">
            <el-option v-for="p in persons" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算结果" v-if="calcResult">
          <el-tag type="success" size="large" style="font-size:16px">
            {{ getPersonName(calcPersonA) }} 是 {{ getPersonName(calcPersonB) }} 的 <b>{{ calcResult }}</b>
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRelationshipCalc = false">取消</el-button>
        <el-button type="primary" @click="doCalculate" :disabled="!calcPersonA || !calcPersonB">
          计算
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加人物弹窗 -->
    <PersonDialog
      v-model:visible="showAddPerson"
      :persons="persons"
      :mode="treeTemplate"
      @submit="handleAddPerson"
    />

    <!-- 添加关系弹窗 -->
    <RelationshipDialog
      v-model:visible="showAddRelation"
      :persons="persons"
      :mode="treeTemplate"
      @submit="handleAddRelation"
    />

    <!-- 删除弹窗 -->
    <el-dialog v-model="showDeletePerson"
      :title="isAoe ? '删除事件' : isEr ? '删除元素' : '删除人物'" width="400px">
      <el-form>
        <el-form-item label="选择人物">
          <el-select v-model="deletePersonId" filterable placeholder="请选择" style="width:100%">
            <el-option v-for="p in persons" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeletePerson = false">取消</el-button>
        <el-button type="danger" @click="handleDeletePerson" :disabled="!deletePersonId">
          删除
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑出生排名弹窗 -->
    <el-dialog v-model="editRelDialogVisible" title="设置兄弟姐妹出生排序" width="420px">
      <p style="margin-bottom:16px;color:#909399;font-size:13px;">
        按出生顺序设置排名（1=老大，2=老二...）。系统将根据排名自动判定哥哥/弟弟/姐姐/妹妹。
      </p>
      <div v-for="sib in editRelSiblings" :key="sib.id" class="sibling-rank-row">
        <span class="sibling-rank-name">{{ sib.name }}</span>
        <span class="sibling-rank-gender">{{ sib.gender === 'MALE' ? '男' : '女' }}</span>
        <el-input-number v-model="sib.rank" :min="1" :max="20" size="small" controls-position="right" />
        <span class="sibling-rank-label">老{{ sib.rank || '?' }}</span>
      </div>
      <template #footer>
        <el-button @click="editRelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditRelSave">保存排名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Link, Delete, Monitor, Refresh } from '@element-plus/icons-vue'
import FamilyGraph from '../components/FamilyGraph.vue'
import PersonDialog from '../components/PersonDialog.vue'
import RelationshipDialog from '../components/RelationshipDialog.vue'
import RelationshipInfo from '../components/RelationshipInfo.vue'
import api from '../api/family.js'

const props = defineProps({
  treeId: { type: Number, default: null },
  treeTemplate: { type: String, default: 'family' }
})
const emit = defineEmits(['back'])

const isFamily = computed(() => props.treeTemplate === 'family')
const isEr = computed(() => props.treeTemplate === 'er')
const isAoe = computed(() => props.treeTemplate === 'aoe')

// 数据状态
const persons = ref([])
const graphElements = ref({ nodes: [], edges: [] })
const selectedPerson = ref(null)
const selectedPersonIdStr = computed(() => selectedPerson.value ? String(selectedPerson.value.id) : null)
const relationshipResult = ref(null)
const showRelationshipCalc = ref(false)
const calcPersonA = ref(null)
const calcPersonB = ref(null)
const calcResult = ref('')
const showAddPerson = ref(false)
const showAddRelation = ref(false)
const showDeletePerson = ref(false)
const deletePersonId = ref(null)

// AOE 网
const aoeData = ref(null)
async function calculateAoe() {
  if (!props.treeId) { ElMessage.warning('请先进入图谱'); return }
  try {
    const res = await api.get(`/family/aoe/${props.treeId}`)
    aoeData.value = res.data
    ElMessage.success('计算完成')
  } catch (e) {
    ElMessage.error('计算失败')
  }
}

// 编辑出生排名
const editRelDialogVisible = ref(false)
const editRelSiblings = ref([])

// 关系边选择
const edgeSelected = ref(null)
const edgeAiResult = ref('')
const edgeAiLoading = ref('')   // 当前正在加载的关系标签名，空=未加载
const edgeAiFor = ref('')       // 当前结果显示的是哪个标签的

function onEdgeClick(edgeData) {
  edgeSelected.value = edgeData
  edgeAiResult.value = ''
  // 点击空白清除 AOE 面板
  if (!edgeData && isAoe.value) {
    aoeData.value = null
  }
}

async function queryEdgeAI(label) {
  if (!label) return
  edgeAiLoading.value = label
  edgeAiResult.value = ''
  edgeAiFor.value = ''
  try {
    const res = await api.aiQueryRelationship(label)
    edgeAiResult.value = res.data.result || '暂无介绍'
    edgeAiFor.value = label
  } catch (e) {
    edgeAiResult.value = 'AI 查询服务暂时不可用，请稍后重试。'
    edgeAiFor.value = label
  } finally {
    edgeAiLoading.value = ''
  }
}

// 找到某人的所有兄弟姐妹（共享父母）
function findSiblings(personId) {
  // 从 graphElements 中找到 parent-child 边
  const edges = graphElements.value.edges || []
  const nodes = graphElements.value.nodes || []

  // 找到这个人的父母（即边 target=personId 的边，类型 PARENT_CHILD）
  const parentEdges = edges.filter(e =>
    e.data.target === personId && e.data.type === 'PARENT_CHILD'
  )
  const parentIds = parentEdges.map(e => e.data.source)

  // 找到这些父母的所有孩子
  const childIds = new Set()
  for (const e of edges) {
    if (e.data.type === 'PARENT_CHILD' && parentIds.includes(e.data.source)) {
      childIds.add(e.data.target)
    }
  }
  childIds.add(personId) // 包含自己

  // 转为对象列表
  return Array.from(childIds).map(id => {
    const p = persons.value.find(p => p.id.toString() === id)
    return {
      id,
      name: p?.name || '未知',
      gender: p?.gender || 'MALE',
      rank: p?.siblingRank || 1
    }
  }).sort((a, b) => a.rank - b.rank)
}

// 边双击 — 编辑兄弟姐妹排名
function onEdgeDoubleClick(edgeData) {
  const label = edgeData.label
  // 只处理兄弟姐妹关系
  if (!['哥哥', '弟弟', '姐姐', '妹妹', '堂哥', '堂弟', '堂姐', '堂妹', '表哥', '表弟', '表姐', '表妹'].includes(label) &&
      !label.endsWith('哥') && !label.endsWith('弟') && !label.endsWith('姐') && !label.endsWith('妹')) {
    return
  }

  // 找到两人的所有兄弟姐妹
  const sourceSibs = findSiblings(edgeData.source)
  const targetSibs = findSiblings(edgeData.target)

  // 合并去重
  const merged = new Map()
  for (const s of [...sourceSibs, ...targetSibs]) {
    if (!merged.has(s.id)) merged.set(s.id, s)
  }

  editRelSiblings.value = Array.from(merged.values())
  editRelDialogVisible.value = true
}

async function handleEditRelSave() {
  try {
    // 更新每个兄弟姐妹的排名
    for (const sib of editRelSiblings.value) {
      await api.updateSiblingRank(sib.id, sib.rank)
    }
    ElMessage.success('排名已保存')
    editRelDialogVisible.value = false
    await loadData()
  } catch (e) {
    const msg = e.response?.data?.error || e.response?.data?.message || e.message || '未知错误'
    console.error('保存排名失败:', e)
    ElMessage.error(`保存失败: ${msg}`)
  }
}

// 加载数据
async function loadData() {
  try {
    const params = props.treeId ? { params: { treeId: props.treeId } } : {}
    const [personsRes, graphRes] = await Promise.all([
      api.getPersons(params),
      api.getGraphData()
    ])
    persons.value = personsRes.data
    graphElements.value = graphRes.data.elements
    // 刷新 selectedPerson 引用，确保头像等最新数据同步
    if (selectedPerson.value) {
      const updated = persons.value.find(p => p.id === selectedPerson.value.id)
      if (updated) selectedPerson.value = updated
    }
  } catch (e) {
    console.error('加载数据失败:', e)
    ElMessage.error('加载数据失败')
  }
}

// 节点点击（null表示取消选中）
function onNodeClick(nodeId) {
  if (!nodeId) {
    selectedPerson.value = null
    relationshipResult.value = null
    return
  }
  const person = persons.value.find(p => p.id.toString() === nodeId)
  selectedPerson.value = person || null
  relationshipResult.value = null
}

// 节点双击 — 编辑人物
async function onNodeDoubleClick(nodeId) {
  const person = persons.value.find(p => p.id.toString() === nodeId)
  if (!person) return
  try {
    const { value: form } = await ElMessageBox.prompt('编辑姓名', '编辑人物', {
      inputValue: person.name,
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (form) {
      await api.updatePerson(person.id, { name: form })
      ElMessage.success('修改成功')
      await loadData()
    }
  } catch {}
}

// 添加人物（带自动关联）
async function handleAddPerson(data) {
  try {
    if (props.treeTemplate === 'aoe') {
      await api.createPerson(data.name, 'MALE', props.treeId)
      ElMessage.success(`事件「${data.name}」已添加`)
    } else if (props.treeTemplate === 'er') {
      const typeMap = { entity: '实体', attribute: '属性', relation: '关系' }
      await api.createPerson(data.name, 'MALE', props.treeId, data.entityType || 'entity')
      ElMessage.success(`${typeMap[data.entityType] || '元素'}「${data.name}」已添加`)
    } else if (data.connectedPersonId) {
      await api.autoConnect(data.name, data.gender, data.connectedPersonId, data.relationType, props.treeId)
      ElMessage.success(`添加成功！${data.name} 已关联到家庭`)
    } else {
      await api.createPerson(data.name, data.gender, props.treeId)
      ElMessage.success('添加成功')
    }
  } catch (e) {
    const msg = e.response?.data?.error || e.response?.data?.message || e.message || '未知错误'
    console.error('添加失败:', e)
    ElMessage.error(`添加失败: ${msg}`)
    return
  }
  try {
    await loadData()
  } catch (e) {
    console.error('刷新图谱失败:', e)
    ElMessage.warning('人物已添加，但刷新图谱失败，请刷新页面')
  }
}

// 添加基础关系
async function handleAddRelation(data) {
  try {
    const p1 = persons.value.find(p => p.id === data.person1Id)
    const p2 = persons.value.find(p => p.id === data.person2Id)
    const isEr = props.treeTemplate === 'er'
    const bothEntities = p1?.entityType === 'entity' && p2?.entityType === 'entity'
    const isPairCard = data.cardinality && data.cardinality.includes(':')

    if (isEr && bothEntities && isPairCard) {
      // 实体↔实体：自动生成菱形关系节点 + 两条边（原子操作）
      const parts = data.cardinality.split(':')
      const relName = data.customLabel || data.type || '关系'
      await api.createErRelation(data.person1Id, data.person2Id, relName, parts[0], parts[1], props.treeId)
      ElMessage.success('关系已创建')
    } else {
      await api.createRelationship(data.person1Id, data.person2Id, data.type, data.customLabel, data.cardinality, data.duration)
      ElMessage.success('连接添加成功')
    }
  } catch (e) {
    const msg = e.response?.data?.error || e.response?.data?.message || e.message || '未知错误'
    console.error('添加关系失败:', e)
    ElMessage.error(`添加关系失败: ${msg}`)
    return
  }
  try {
    await loadData()
  } catch (e) {
    ElMessage.warning('关系已添加，但刷新图谱失败，请刷新页面')
  }
}

// 删除人物
async function handleDeletePerson() {
  try {
    await api.deletePerson(deletePersonId.value)
    ElMessage.success('删除成功')
    showDeletePerson.value = false
    deletePersonId.value = null
    selectedPerson.value = null
    await loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 清空所有数据
async function handleClearAll() {
  try {
    await ElMessageBox.confirm('确定要清空所有人物和关系吗？此操作不可恢复。', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.clearAll()
    ElMessage.success('已清空所有数据')
    selectedPerson.value = null
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// 刷新关系（清除所有计算关系，基于基础关系重新计算）
async function handleRefreshRelations() {
  try {
    await ElMessageBox.confirm(
      '将基于现有的基础关系（父亲、母亲、儿子、女儿、丈夫、妻子）重新计算所有衍生关系。',
      '刷新关系确认',
      { confirmButtonText: '确定刷新', cancelButtonText: '取消', type: 'info' }
    )
    ElMessage.info('正在重新计算所有关系...')
    await api.recomputeAll()
    ElMessage.success('关系刷新完成')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('刷新失败')
    }
  }
}

const importInputRef = ref(null)
function triggerImport() { importInputRef.value?.click() }

async function handleExport() {
  if (!props.treeId || persons.value.length === 0) { ElMessage.warning('图谱为空'); return }
  try {
    const res = await api.get(`/export-tree/${props.treeId}`)
    const data = res.data
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (data.treeName || '图谱') + '.mygraph'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    const msg = e.response?.data?.error || e.response?.status || e.message || '未知错误'
    console.error('导出失败:', e)
    ElMessage.error('导出失败: ' + msg)
  }
}

async function handleImport(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const json = JSON.parse(text)
    console.log('[Import] 解析数据:', {
      treeName: json.treeName,
      template: json.template,
      persons: json.persons?.length,
      rels: json.relationships?.length
    })
    // 记录第一条关系数据示例
    if (json.relationships?.length > 0) {
      console.log('[Import] 示例关系:', json.relationships[0])
    }
    const res = await api.post('/import-tree', json)
    console.log('[Import] 导入响应:', res.data)
    ElMessage.success(`导入成功（${res.data.persons || '?'}人, ${res.data.rels || '?'}关系）`)
    emit('back')
  } catch (err) {
    ElMessage.error('导入失败：' + (err.response?.data?.error || err.message || '格式错误'))
  }
  e.target.value = ''
}

// 计算关系
async function doCalculate() {
  if (!calcPersonA.value || !calcPersonB.value) return
  try {
    const res = await api.calculateRelationship(calcPersonA.value, calcPersonB.value)
    calcResult.value = res.data.label || '无关系'
  } catch (e) {
    calcResult.value = '计算失败'
  }
}

function getPersonName(id) {
  const p = persons.value.find(p => String(p.id) === String(id))
  return p ? p.name : '未知'
}

onMounted(loadData)
</script>

<style scoped>
.family-tree-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: var(--card-bg, #fff);
  border-bottom: 1px solid var(--border, #e4e7ed);
  box-shadow: 0 1px 4px var(--shadow, rgba(0,0,0,0.04));
  z-index: 10;
}

.toolbar-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text, #303133);
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.main-area {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.graph-container {
  flex: 1;
  position: relative;
  background: var(--card-bg, #fff);
  margin: 8px;
  border-radius: 8px;
  box-shadow: 0 1px 4px var(--shadow, rgba(0,0,0,0.06));
  overflow: hidden;
}

.empty-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 信息面板滑入动画 */
.panel-slide-enter-active {
  transition: transform 0.35s ease-out, opacity 0.35s ease-out;
}
.panel-slide-leave-active {
  transition: transform 0.25s ease-in, opacity 0.25s ease-in;
}
.panel-slide-enter-from {
  transform: translateX(30px);
  opacity: 0;
}
.panel-slide-leave-to {
  transform: translateX(30px);
  opacity: 0;
}

.info-panel {
  width: 360px;
  margin: 8px 8px 8px 0;
  background: var(--card-bg, #fff);
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow-y: auto;
}

.sibling-rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.sibling-rank-name {
  font-weight: 600;
  min-width: 60px;
  color: #303133;
}

.sibling-rank-gender {
  font-size: 12px;
  color: #909399;
  min-width: 20px;
}

.sibling-rank-label {
  font-size: 13px;
  color: #909399;
  min-width: 40px;
}

/* 关系边信息面板 */
.edge-info-panel {
  padding: 4px 0;
}

.edge-direction-card {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 14px;
  transition: all 0.2s;
}
.edge-direction-card:hover {
  background: #f0f2f5;
}

.edge-direction-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.edge-direction-person {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.edge-direction-arrow {
  color: #409EFF;
  font-weight: bold;
  font-size: 18px;
  margin: 0 4px;
}

.edge-direction-label {
  font-size: 16px;
  font-weight: bold;
  color: #E6A23C;
}

.edge-direction-cardinality {
  font-size: 14px;
  font-weight: bold;
  color: #E6A23C;
  background: #fdf6ec;
  border-radius: 4px;
  padding: 0 6px;
  margin: 0 2px;
}

.edge-ai-result {
  font-size: 15px;
  line-height: 1.6;
  color: #303133;
  background: #f0f5ff;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  border-left: 3px solid #409EFF;
}

/* AOE 面板 */
.aoe-panel { padding: 12px 4px; }
.aoe-summary { display:flex; gap:12px; margin:8px 0; }
.aoe-stat { flex:1; text-align:center; padding:10px 4px; background:#f8f9fa; border-radius:8px; }
.aoe-stat span { display:block; font-size:12px; color:#909399; }
.aoe-stat b { display:block; font-size:22px; color:#E6A23C; margin-top:4px; }
.aoe-section-title { font-size:14px; font-weight:600; color:var(--text,#303133); margin-bottom:6px; }
.aoe-cp, .aoe-tp { font-size:14px; color:#E6A23C; font-weight:bold; line-height:1.6; padding:4px 0; }
.aoe-table-wrap { max-height:260px; overflow-y:auto; }
.aoe-table { width:100%; border-collapse:collapse; font-size:13px; }
.aoe-table th { background:#f5f7fa; padding:6px 4px; text-align:center; font-weight:600; border-bottom:2px solid #e4e7ed; }
.aoe-table td { padding:5px 4px; text-align:center; border-bottom:1px solid #f0f0f0; }
</style>
