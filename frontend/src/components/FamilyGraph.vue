<template>
  <div ref="cyContainer" class="cy-container"></div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'
import cytoscape from 'cytoscape'

const props = defineProps({
  elements: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  selectedPersonId: {
    type: String,
    default: null
  },
  template: {
    type: String,
    default: 'family'
  }
})

const emit = defineEmits(['node-click', 'node-double-click', 'edge-click', 'edge-double-click'])

const cyContainer = ref(null)
let cy = null
let currentSelectedId = null

function initCy() {
  if (!cyContainer.value) return

  const isER = props.template === 'er'

  // ER 图专属样式
  const erStyles = isER ? [
    // 实体（默认）- 圆角矩形 绿色
    {
      selector: 'node',
      style: {
        'label': 'data(label)',
        'text-valign': 'center', 'text-halign': 'center',
        'color': '#fff', 'font-size': '14px', 'font-weight': 'bold',
        'background-color': '#67C23A',
        'width': '90px', 'height': '60px',
        'shape': 'round-rectangle',
        'border-width': '2px', 'border-color': '#529b2e',
        'shadow-blur': '8px', 'shadow-color': 'rgba(103,194,58,0.3)',
        'shadow-offset-x': '0', 'shadow-offset-y': '2px'
      }
    },
    // 属性 - 椭圆 蓝色
    {
      selector: 'node[entityType = "attribute"]',
      style: {
        'shape': 'ellipse',
        'background-color': '#409EFF',
        'border-color': '#337ecc',
        'width': '80px', 'height': '50px',
        'font-size': '13px',
        'shadow-color': 'rgba(64,158,255,0.3)'
      }
    },
    // 关系 - 菱形 橙色
    {
      selector: 'node[entityType = "relation"]',
      style: {
        'shape': 'diamond',
        'background-color': '#E6A23C',
        'border-color': '#b88230',
        'width': '70px', 'height': '70px',
        'shadow-color': 'rgba(230,162,60,0.3)'
      }
    },
    {
      selector: 'edge',
      style: {
        'width': 2, 'line-color': '#909399',
        'curve-style': 'bezier',
        'label': '',  // 关系名称在菱形中，线上无文字
        'target-arrow-shape': 'none',
        'source-arrow-shape': 'none',
        'arrow-scale': 1,
        // 基数标注（在 updateElements 中动态设置）
        'source-font-size': '13px',
        'source-color': '#E6A23C',
        'source-font-weight': 'bold',
        'source-text-offset': '16px',
        'source-background-color': '#ffffff',
        'source-background-opacity': 1,
        'source-background-padding': '2px',
        'target-font-size': '13px',
        'target-color': '#E6A23C',
        'target-font-weight': 'bold',
        'target-text-offset': '16px',
        'target-background-color': '#ffffff',
        'target-background-opacity': 1,
        'target-background-padding': '2px'
      }
    }
  ] : [];

  // AOE 网样式
  const isAOE = props.template === 'aoe'
  const aoeStyles = isAOE ? [
    { selector: 'node', style: {
      'label': 'data(label)', 'text-valign': 'center', 'text-halign': 'center',
      'color': '#fff', 'font-size': '14px', 'font-weight': 'bold',
      'background-color': '#67C23A', 'width': '60px', 'height': '60px',
      'shape': 'ellipse',
      'border-width': '2px', 'border-color': '#529b2e',
      'shadow-blur': '6px', 'shadow-color': 'rgba(103,194,58,0.3)',
      'shadow-offset-x': '0', 'shadow-offset-y': '2px'
    } },
    { selector: 'edge', style: {
      'width': 2, 'line-color': '#909399', 'curve-style': 'bezier',
      'target-arrow-shape': 'triangle', 'target-arrow-color': '#909399',
      'arrow-scale': 1.2,
      'label': '',  // 在 updateElements 中通过 duration 动态设置
      'font-size': '14px', 'color': '#E6A23C', 'font-weight': 'bold',
      'text-background-color': '#ffffff', 'text-background-opacity': 1,
      'text-background-padding': '4px', 'text-rotation': 'none'
    } }
  ] : [];

  // 族谱专用样式
  const familyStyles = props.template === 'family' ? [
    {
      selector: 'node',
      style: {
        'label': 'data(label)',
        'text-valign': 'bottom',
        'text-halign': 'center',
        'color': '#333',
        'font-size': '15px',
        'font-weight': 'bold',
        'background-color': '#f0f2f5',
        'background-image': 'data(avatar)',
        'background-fit': 'cover',
        'background-clip': 'node',
        'width': '60px',
        'height': '60px',
        'shape': 'ellipse',
        'transition-property': 'all',
        'transition-duration': '0.6s',
        'transition-timing-function': 'ease-out',
        'border-width': '2px',
        'border-color': '#fff',
        'shadow-blur': '8px',
        'shadow-color': 'rgba(0,0,0,0.1)',
        'shadow-offset-x': '0',
        'shadow-offset-y': '2px',
        'padding-left': '2px',
        'padding-right': '2px'
      }
    },
    {
      selector: 'node[gender = "FEMALE"]',
      style: { 'background-color': '#F56C6C' }
    },
    {
      selector: 'edge',
      style: {
        'width': 2.5,
        'line-color': '#409EFF',
        'target-arrow-color': '#409EFF',
        'curve-style': 'bezier',
        'label': '',
        'font-size': '14px',
        'color': '#303133',
        'text-background-color': '#ffffff',
        'text-background-opacity': 1,
        'text-background-padding': '4px',
        'text-rotation': 'none',
        'font-weight': 'bold'
      }
    },
    {
      selector: 'edge[type = "SPOUSE"]',
      style: { 'line-style': 'dashed', 'width': 1.5, 'line-color': '#C0C4CC' }
    },
    {
      selector: 'edge[type = "COMPUTED"]',
      style: {
        'line-style': 'dotted', 'width': 1, 'line-color': '#409EFF', 'label': '',
        'curve-style': 'unbundled-bezier', 'control-point-distances': '30px'
      }
    },
    {
      selector: 'edge[type = "CUSTOM"]',
      style: {
        'line-style': 'dashed', 'width': 1.5, 'line-color': '#909399', 'label': '',
        'font-size': '13px', 'color': '#909399',
        'text-background-color': '#ffffff', 'text-background-opacity': 0.9,
        'text-background-padding': '3px', 'curve-style': 'bezier'
      }
    }
  ] : [];

  cy = cytoscape({
    container: cyContainer.value,
    style: [
      ...familyStyles,
      ...erStyles, ...aoeStyles,
      {
        selector: ':selected',
        style: {
          'border-width': 3,
          'border-color': '#E6A23C',
          'shadow-blur': '12px',
          'shadow-color': 'rgba(230,162,60,0.5)'
        }
      }
    ],
    layout: {
      name: 'breadthfirst',
      directed: true,
      spacingFactor: 1.5,
      maximalAdjustments: 2
    },
    wheelSensitivity: 0.3,
    minZoom: 0.3,
    maxZoom: 3
  })

  // 应用主题文字颜色到节点标签（仅族谱模式）
  function applyThemeTextColor() {
    if (props.template === 'er' || props.template === 'aoe') return
    const el = document.getElementById('app-container')
    if (!el) return
    const style = getComputedStyle(el)
    const textColor = style.getPropertyValue('--text').trim() || '#303133'
    cy.nodes().forEach(n => n.style('color', textColor))
  }
  applyThemeTextColor()

  // 主题切换时自动更新文字颜色
  const observer = new MutationObserver(() => applyThemeTextColor())
  const appEl = document.getElementById('app-container')
  if (appEl && props.template !== 'er') observer.observe(appEl, { attributes: true, attributeFilter: ['class'] })

  // 事件处理
  cy.on('click', 'node', (evt) => {
    const node = evt.target
    if (currentSelectedId === node.id()) {
      // 点击已选中的节点取消选中
      currentSelectedId = null
      emit('node-click', null)
      updateEdgesForSelection(null)
    } else {
      currentSelectedId = node.id()
      emit('node-click', node.id())
      updateEdgesForSelection(node.id())
    }
  })

  // 节点悬停动效（通过边框+阴影营造放大感，避免图片重绘卡顿）
  cy.on('mouseover', 'node', (evt) => {
    const node = evt.target
    node.style('border-width', 4)
    node.style('border-color', '#E6A23C')
    node.style('border-opacity', 1)
    node.style('shadow-blur', 24)
    node.style('shadow-color', 'rgba(230,162,60,0.45)')
    node.style('shadow-offset-x', 0)
    node.style('shadow-offset-y', 4)
  })
  cy.on('mouseout', 'node', (evt) => {
    const node = evt.target
    node.style('border-width', 2)
    node.style('border-color', '#fff')
    node.style('border-opacity', 1)
    node.style('shadow-blur', 8)
    node.style('shadow-color', 'rgba(0,0,0,0.1)')
    node.style('shadow-offset-x', 0)
    node.style('shadow-offset-y', 2)
  })

  // 边悬停高亮
  cy.on('mouseover', 'edge', (evt) => {
    const edge = evt.target
    if (edge.data('type') !== 'COMPUTED') {
      edge.style('line-color', '#E6A23C')
      edge.style('target-arrow-color', '#E6A23C')
      edge.style('width', 3)
    }
  })
  cy.on('mouseout', 'edge', (evt) => {
    const edge = evt.target
    edge.style('line-color', '#409EFF')
    edge.style('target-arrow-color', '#409EFF')
    edge.style('width', 2)
    // 如有选中节点，由 updateEdgesForSelection 覆盖
    if (currentSelectedId) {
      updateEdgesForSelection(currentSelectedId)
    }
  })

  cy.on('dblclick', 'node', (evt) => {
    const node = evt.target
    emit('node-double-click', node.id())
  })

  // 双击边编辑关系（仅计算关系边可编辑）
  cy.on('dblclick', 'edge', (evt) => {
    const edge = evt.target
    const data = edge.data()
    if (data.type === 'COMPUTED') {
      emit('edge-double-click', {
        id: data.id,
        source: data.source,
        target: data.target,
        label: data.label,
        category: data.category
      })
    }
  })

  // 边点击
  cy.on('click', 'edge', (evt) => {
    const edge = evt.target
    const data = edge.data()
    currentSelectedId = null
    emit('node-click', null)
    updateEdgesForSelection(null)
    emit('edge-click', {
      label: data.label || '',
      reverseLabel: data.reverseLabel || '',
      source: data.source,
      target: data.target,
      type: data.type || '',
      category: data.category || '',
      cardinality: data.cardinality || ''
    })
  })

  // 点击空白区域取消选中
  cy.on('click', (evt) => {
    if (evt.target === cy) {
      currentSelectedId = null
      emit('node-click', null)
      updateEdgesForSelection(null)
      emit('edge-click', null)
    }
  })
}

// 根据选中的节点更新边的样式
function updateEdgesForSelection(selectedId) {
  if (!cy) return

  // ER 图模式：边不显示文字（关系名在菱形内），只保留基数标注
  if (props.template === 'er') {
    cy.edges().forEach(edge => {
      const data = edge.data()
      edge.style('label', '')
      if (selectedId && (data.source === selectedId || data.target === selectedId)) {
        edge.style('line-color', '#E6A23C')
        edge.style('width', 3)
      } else {
        edge.style('line-color', '#909399')
        edge.style('width', 2)
      }
    })
    return
  }

  // AOE 网：始终显示工期数字
  if (props.template === 'aoe') {
    cy.edges().forEach(edge => {
      const data = edge.data()
      const dur = data.duration ? String(data.duration) : ''
      edge.style('label', dur)
      if (selectedId && (data.source === selectedId || data.target === selectedId)) {
        edge.style('line-color', '#E6A23C')
        edge.style('width', 3)
      } else {
        edge.style('line-color', '#909399')
        edge.style('width', 2)
      }
    })
    return
  }

  cy.edges().forEach(edge => {
    const data = edge.data()
    if (!selectedId) {
      // 未选中：所有边蓝色无文字
      edge.style('label', '')
      edge.style('line-color', '#409EFF')
      edge.style('target-arrow-color', '#409EFF')
      edge.style('width', data.type === 'COMPUTED' ? 1 : 2)
    } else if (data.source === selectedId) {
      // 选中的人是这条边的起点：显示正向标签
      edge.style('label', data.label || '')
      edge.style('line-color', '#E6A23C')
      edge.style('target-arrow-color', '#E6A23C')
      edge.style('width', 3)
    } else if (data.target === selectedId) {
      // 选中的人是这条边的终点：显示反向标签
      edge.style('label', data.reverseLabel || data.label || '')
      edge.style('line-color', '#E6A23C')
      edge.style('target-arrow-color', '#E6A23C')
      edge.style('width', 3)
    } else {
      // 其他边：无文字
      edge.style('label', '')
      edge.style('line-color', '#d0d5dd')
      edge.style('target-arrow-color', '#d0d5dd')
      edge.style('width', data.type === 'COMPUTED' ? 0.5 : 1)
    }
  })
}

// 更新元素
function updateElements() {
  if (!cy) return
  const { nodes = [], edges = [] } = props.elements

  if (nodes.length === 0 && edges.length === 0) {
    cy.elements().remove()
    return
  }

  const existingNodeIds = new Set(cy.nodes().map(n => n.id()))
  const newNodeIds = new Set(nodes.map(n => n.data.id))

  // 移除不存在的节点
  for (const id of existingNodeIds) {
    if (!newNodeIds.has(id)) {
      cy.getElementById(id).remove()
    }
  }

  // 获取当前主题的文字颜色（从 CSS 变量）
  function getTextColor() {
    const el = document.getElementById('app-container')
    if (el) {
      const style = getComputedStyle(el)
      return style.getPropertyValue('--text').trim() || '#303133'
    }
    return '#303133'
  }
  const textColor = getTextColor()

  // 添加或更新节点
  for (const n of nodes) {
    const existing = cy.getElementById(n.data.id)
    if (existing.length) {
      existing.data(n.data)
      if (props.template === 'family') {
        existing.style('background-image', n.data.avatar || 'none')
        existing.style('color', textColor)
      }
    } else {
      cy.add(n)
    }
  }

  // 应用 ER 图基数样式到边（基数数字始终靠近实体侧）
  function applyErCardinality(edge, data) {
    if (props.template === 'er' && data.cardinality) {
      // 判断哪一端是实体（entityType="entity"）
      const srcNode = cy.getElementById(data.source)
      const tgtNode = cy.getElementById(data.target)
      const srcIsEntity = srcNode.length && srcNode.data('entityType') === 'entity'
      const tgtIsEntity = tgtNode.length && tgtNode.data('entityType') === 'entity'

      const parts = data.cardinality.split(':')
      if (parts.length === 2) {
        // 双端基数 1:1 / 1:N / N:M（旧式直连，保留兼容）
        edge.style('source-label', parts[0])
        edge.style('target-label', parts[1])
        edge.style('source-arrow-shape', parts[0] !== '1' ? 'triangle-tee' : 'none')
        edge.style('source-arrow-color', '#E6A23C')
        edge.style('target-arrow-shape', parts[1] !== '1' ? 'triangle-tee' : 'none')
        edge.style('target-arrow-color', '#E6A23C')
      } else if (srcIsEntity && !tgtIsEntity) {
        // 实体→关系：基数显示在源端（实体侧）
        edge.style('source-label', data.cardinality)
        edge.style('target-label', '')
        edge.style('source-arrow-shape', data.cardinality !== '1' ? 'triangle-tee' : 'none')
        edge.style('source-arrow-color', '#E6A23C')
        edge.style('target-arrow-shape', 'none')
      } else if (!srcIsEntity && tgtIsEntity) {
        // 关系→实体：基数显示在目标端（实体侧）
        edge.style('source-label', '')
        edge.style('target-label', data.cardinality)
        edge.style('source-arrow-shape', 'none')
        edge.style('target-arrow-shape', data.cardinality !== '1' ? 'triangle-tee' : 'none')
        edge.style('target-arrow-color', '#E6A23C')
      } else {
        // 无法判断，默认显示在目标端
        edge.style('source-label', '')
        edge.style('target-label', data.cardinality)
        edge.style('source-arrow-shape', 'none')
        edge.style('target-arrow-shape', data.cardinality !== '1' ? 'triangle-tee' : 'none')
        edge.style('target-arrow-color', '#E6A23C')
      }
    } else {
      edge.style('source-label', '')
      edge.style('target-label', '')
    }
  }

  // 添加或更新边
  for (const e of edges) {
    const existing = cy.getElementById(e.data.id)
    if (existing.length) {
      existing.data(e.data)
      applyErCardinality(existing, e.data)
    } else {
      cy.add(e)
      const added = cy.getElementById(e.data.id)
      if (added.length) applyErCardinality(added, e.data)
    }
    // AOE：显示工期数字，不显示活动名
    if (props.template === 'aoe') {
      const edge = existing.length ? existing : cy.getElementById(e.data.id)
      if (edge.length && e.data.duration) {
        edge.style('label', String(e.data.duration))
      }
    }
  }

  // 应用选中状态
  if (currentSelectedId) {
    updateEdgesForSelection(currentSelectedId)
  }

  // 重新布局
  if (nodes.length > 0) {
    try {
      cy.layout({
        name: 'breadthfirst',
        directed: true,
        spacingFactor: 1.5,
        maximalAdjustments: 2,
        fit: true,
        padding: 50
      }).run()
    } catch (e) {
      cy.layout({ name: 'grid', fit: true, padding: 50 }).run()
    }
  }
}

// 监听外部选中变化
watch(() => props.selectedPersonId, (newId) => {
  currentSelectedId = newId
  if (cy) {
    updateEdgesForSelection(newId)
  }
})

// 监听数据变化
watch(() => props.elements, () => {
  nextTick(updateElements)
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initCy()
    updateElements()
  })
})

onBeforeUnmount(() => {
  if (cy) {
    cy.destroy()
    cy = null
  }
})
</script>

<style scoped>
.cy-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
