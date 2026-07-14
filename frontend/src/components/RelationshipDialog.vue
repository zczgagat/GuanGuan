<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)"
    :title="mode === 'er' ? '添加连线' : '添加基础关系'" width="500px">

    <p v-if="mode !== 'er'" style="color:#909399;margin-bottom:16px;font-size:13px;">
      定义两个人之间的基础关系。关系语义：<b>人物A</b> 是 <b>人物B</b> 的____。
    </p>
    <p v-else style="color:#909399;margin-bottom:16px;font-size:13px;">
      定义两个实体之间的连线关系。<b>实体A</b> 到 <b>实体B</b> 的____。
    </p>

    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">

      <el-form-item :label="mode === 'er' ? '实体A（起点）' : '人物A（主体）'" prop="person1Id">
        <el-select v-model="form.person1Id" filterable placeholder="请选择" style="width:100%">
          <el-option v-for="p in availablePersons" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="关系" prop="type">
        <el-select v-model="form.type" :placeholder="mode === 'er' ? '输入关系类型' : '选择关系'" style="width:100%">
          <el-option v-for="r in relationTypes" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
      </el-form-item>

      <el-form-item :label="mode === 'er' ? '实体B（终点）' : '人物B（客体）'" prop="person2Id">
        <el-select v-model="form.person2Id" filterable placeholder="请选择" style="width:100%">
          <el-option v-for="p in availablePersons" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>

      <!-- ER 图基数标注 -->
      <el-form-item v-if="mode === 'er' && cardinalityType !== 'none'" label="基数">
        <!-- 实体↔实体：双端基数 -->
        <el-radio-group v-model="form.cardinality" v-if="cardinalityType === 'pair'">
          <el-radio value="1:1">1:1 — 一对一</el-radio>
          <br/>
          <el-radio value="1:N">1:N — 一对多</el-radio>
          <br/>
          <el-radio value="N:M">N:M — 多对多</el-radio>
        </el-radio-group>
        <!-- 实体↔关系：单端基数 -->
        <el-radio-group v-model="form.cardinality" v-else-if="cardinalityType === 'single'">
          <el-radio value="1">1</el-radio>
          <el-radio value="N">N（多）</el-radio>
          <el-radio value="M">M（多）</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- AOE 网活动名称与工期 -->
      <el-form-item v-if="mode === 'aoe'" label="活动名">
        <el-input v-model="form.customLabel" placeholder="输入活动名称" maxlength="20" />
      </el-form-item>
      <el-form-item v-if="mode === 'aoe'" label="工期">
        <el-input-number v-model="form.duration" :min="1" :max="999" size="small" controls-position="right" />
      </el-form-item>

      <el-form-item label="关系示例" v-if="form.person1Id && form.person2Id && form.type">
        <el-tag type="info">
          {{ getPersonName(form.person1Id) }} → {{ getPersonName(form.person2Id) }}
          <b>：{{ getTypeLabel(form.type) }}</b>
          <span v-if="form.cardinality" style="margin-left:6px;color:#E6A23C">[{{ form.cardinality }}]</span>
        </el-tag>
      </el-form-item>

    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  persons: { type: Array, default: () => [] },
  mode: { type: String, default: 'family' }
})

const emit = defineEmits(['update:visible', 'submit'])

// ER / AOE 模式只有自定义关系
const relationTypes = computed(() => {
  if (props.mode === 'er' || props.mode === 'aoe') {
    return [{ value: 'CUSTOM', label: '自定义关系' }]
  }
  return [
    { value: 'FATHER', label: '父亲（A是B的父亲）' },
    { value: 'MOTHER', label: '母亲（A是B的母亲）' },
    { value: 'SON', label: '儿子（A是B的儿子）' },
    { value: 'DAUGHTER', label: '女儿（A是B的女儿）' },
    { value: 'HUSBAND', label: '丈夫（A是B的丈夫）' },
    { value: 'WIFE', label: '妻子（A是B的妻子）' },
    { value: 'CUSTOM', label: '自定义关系' }
  ]
})

const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  person1Id: null,
  person2Id: null,
  type: 'CUSTOM',
  customLabel: '',
  cardinality: '1:1',
  duration: 1
})

const rules = {
  person1Id: [{ required: true, message: '请选择', trigger: 'change' }],
  person2Id: [{ required: true, message: '请选择', trigger: 'change' }],
  type: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

const availablePersons = computed(() => props.persons)

watch(() => props.visible, (val) => {
  if (val) {
    form.value = {
      person1Id: null, person2Id: null,
      type: props.mode === 'family' ? '' : 'CUSTOM',
      customLabel: '',
      cardinality: '1:1',
      duration: props.mode === 'aoe' ? 1 : undefined
    }
  }
})

// 判断基数类型：entity↔entity 用双端，entity↔关系/属性 用单端，含属性时不需要
const cardinalityType = computed(() => {
  if (props.mode !== 'er' || !form.value.person1Id || !form.value.person2Id) return 'pair'
  const getType = (id) => {
    const p = props.persons.find(p => p.id === id)
    return p ? (p.entityType || 'entity') : 'entity'
  }
  const t1 = getType(form.value.person1Id)
  const t2 = getType(form.value.person2Id)
  // 任何一端是 attribute 就不需要基数
  if (t1 === 'attribute' || t2 === 'attribute') return 'none'
  return (t1 === 'entity' && t2 === 'entity') ? 'pair' : 'single'
})

// 切换选择时重置基数
watch(() => [form.value.person1Id, form.value.person2Id], () => {
  const ct = cardinalityType.value
  form.value.cardinality = ct === 'pair' ? '1:1' : ct === 'single' ? '1' : ''
})

function getPersonName(id) {
  const p = props.persons.find(p => p.id === id)
  return p ? p.name : '未知'
}

function getTypeLabel(type) {
  if (type === 'CUSTOM') return '自定义'
  const r = relationTypes.value.find(r => r.value === type)
  return r ? r.label.split('（')[0] : type
}

async function handleSubmit() {
  if (form.value.person1Id === form.value.person2Id) {
    ElMessage.warning('不能自己和自己建立关系')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const submitData = {
      person1Id: form.value.person1Id,
      person2Id: form.value.person2Id,
      type: form.value.type,
      customLabel: form.value.type === 'CUSTOM' ? (form.value.customLabel || form.value.type) : '',
      cardinality: form.value.cardinality,
      duration: form.value.duration
    }
    emit('submit', submitData)
    emit('update:visible', false)
  } finally {
    submitting.value = false
  }
}
</script>
