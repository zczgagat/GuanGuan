<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)"
    :title="mode === 'er' ? '添加实体' : mode === 'aoe' ? '添加事件' : '添加人物'" width="480px">
    <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">

      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name"
          :placeholder="mode === 'er' ? '输入名称' : mode === 'aoe' ? '输入事件名称' : '请输入姓名'" maxlength="20" />
      </el-form-item>

      <!-- ER 图：元素类型 -->
      <el-form-item v-if="mode === 'er'" label="类型" prop="entityType">
        <el-radio-group v-model="form.entityType">
          <el-radio value="entity">
            <span style="display:inline-block;width:14px;height:14px;border-radius:4px;background:#67C23A;vertical-align:middle;margin-right:4px"></span>
            实体
          </el-radio>
          <el-radio value="attribute">
            <span style="display:inline-block;width:14px;height:14px;border-radius:50%;background:#409EFF;vertical-align:middle;margin-right:4px"></span>
            属性
          </el-radio>
          <el-radio value="relation">
            <span style="display:inline-block;width:14px;height:14px;transform:rotate(45deg);background:#E6A23C;vertical-align:middle;margin-right:4px"></span>
            关系
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 族谱：性别（AOE/ER 无性别） -->
      <el-form-item v-if="mode === 'family'" label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio value="MALE">男</el-radio>
          <el-radio value="FEMALE">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 族谱：关联家人 -->
      <template v-if="mode === 'family'">
        <el-divider />
        <el-form-item label="关联家人">
          <el-select v-model="form.connectedPersonId" filterable placeholder="选一个已存在的家人（可选）" style="width:100%" clearable>
            <el-option v-for="p in persons" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" v-if="form.connectedPersonId">
          <el-select v-model="form.relationType" placeholder="选择关系" style="width:100%">
            <el-option v-for="r in relationTypes" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
          <div style="color:#909399;font-size:12px;margin-top:4px;">选择新人物与所选家人的关系</div>
        </el-form-item>
      </template>

    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ mode === 'er' ? '确定添加' : '确定添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  persons: { type: Array, default: () => [] },
  mode: { type: String, default: 'family' }
})

const emit = defineEmits(['update:visible', 'submit'])

const relationTypes = [
  { value: 'SON', label: '儿子（新人是所选人物的儿子）' },
  { value: 'DAUGHTER', label: '女儿（新人是所选人物的女儿）' },
  { value: 'FATHER', label: '父亲（新人是所选人物的父亲）' },
  { value: 'MOTHER', label: '母亲（新人是所选人物的母亲）' },
  { value: 'HUSBAND', label: '丈夫（新人是所选人物的丈夫）' },
  { value: 'WIFE', label: '妻子（新人是所选人物的妻子）' }
]

const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  name: '',
  gender: 'MALE',
  connectedPersonId: null,
  relationType: '',
  entityType: 'entity'
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = { name: '', gender: 'MALE', connectedPersonId: null, relationType: '', entityType: 'entity' }
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!props.mode === 'er' && form.value.connectedPersonId && !form.value.relationType) {
    ElMessage.warning('请选择与新家人的关系类型')
    return
  }

  submitting.value = true
  try {
    emit('submit', { ...form.value, mode: props.mode })
    form.value = { name: '', gender: 'MALE', connectedPersonId: null, relationType: '' }
    emit('update:visible', false)
  } finally {
    submitting.value = false
  }
}
</script>
