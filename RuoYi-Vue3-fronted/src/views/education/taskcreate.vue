<template>
  <div class="app-container">
    <el-form ref="formRef" :model="formData" :rules="rules" size="default" label-width="100px">
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="formData.mobile" placeholder="请输入手机号" :maxlength="11" show-word-limit clearable
          prefix-icon='Cellphone' :style="{width: '100%'}"></el-input>
      </el-form-item>
      <el-form-item label="下拉选择" prop="field101">
        <el-select v-model="formData.field101" placeholder="请选择下拉选择" clearable :style="{width: '100%'}">
          <el-option v-for="(item, index) in field101Options" :key="index" :label="item.label"
            :value="item.value" :disabled="item.disabled"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="开关" prop="field102" required>
        <el-switch v-model="formData.field102"></el-switch>
      </el-form-item>
      <el-form-item label="开关" prop="field103" required>
        <el-switch v-model="formData.field103"></el-switch>
      </el-form-item>
      <el-form-item label="开关" prop="field104" required>
        <el-switch v-model="formData.field104"></el-switch>
      </el-form-item>
      <el-form-item label="级联选择" prop="field105">
        <el-cascader v-model="formData.field105" :options="field105Options" :props="field105Props"
          :style="{width: '100%'}" placeholder="请选择级联选择" clearable></el-cascader>
      </el-form-item>
      <el-form-item label="多行文本" prop="field106">
        <el-input v-model="formData.field106" type="textarea" placeholder="请输入多行文本"
          :autosize="{minRows: 4, maxRows: 4}" :style="{width: '100%'}"></el-input>
      </el-form-item>
      <el-form-item label="按钮" prop="field107">
        <el-button type="primary" icon="Search" size="default"> 主要按钮 </el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script setup>
const {
  proxy
} = getCurrentInstance()
const formRef = ref()
const data = reactive({
  formData: {
    mobile: '',
    field101: undefined,
    field102: false,
    field103: false,
    field104: false,
    field105: [],
    field106: '创建任务',
    field107: undefined,
  },
  rules: {
    mobile: [{
      required: true,
      message: '请输入手机号',
      trigger: 'blur'
    }, {
      pattern: new RegExp(/^1(3|4|5|7|8|9)\d{9}$/),
      message: '手机号格式错误',
      trigger: 'blur'
    }],
    field101: [{
      required: true,
      message: '请选择下拉选择',
      trigger: 'change'
    }],
    field105: [{
      required: true,
      type: 'array',
      message: '请至少选择一个field105',
      trigger: 'change'
    }],
    field106: [{
      required: true,
      message: '请输入多行文本',
      trigger: 'blur'
    }],
  }
})
const {
  formData,
  rules
} = toRefs(data)
const field101Options = ref([{
  "label": "选项一",
  "value": 1
}, {
  "label": "选项二",
  "value": 2
}])
const field105Options = ref([])
// props设置
const field105Props = ref({
  "multiple": false
})

function getField105Options() {
  // TODO 发起请求获取数据
  field105Options.value
}
/**
 * @name: 表单提交
 * @description: 表单提交方法
 * @return {*}
 */
function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return
    // TODO 提交表单
  })
}
/**
 * @name: 表单重置
 * @description: 表单重置方法
 * @return {*}
 */
function resetForm() {
  formRef.value.resetFields()
}

</script>
<style>
</style>
