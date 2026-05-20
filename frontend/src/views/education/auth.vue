<template>
  <div class="edu-auth-page">
    <div class="bg-glow bg-glow-left"></div>
    <div class="bg-glow bg-glow-right"></div>

    <el-card class="auth-card" shadow="never">
      <div class="auth-header">
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
      </div>

      <el-radio-group v-model="roleType" class="role-switch" size="large">
        <el-radio-button label="admin">管理端</el-radio-button>
        <el-radio-button label="teacher">教师端</el-radio-button>
        <el-radio-button label="student">学生端</el-radio-button>
      </el-radio-group>

      <el-tabs v-model="activeTab" stretch class="auth-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="currentLoginRules" label-position="top">
            <el-form-item label="账号" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入账号" clearable />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item v-if="requiresMajor" label="专业" prop="majorKey">
              <el-select
                v-model="loginForm.majorKey"
                placeholder="请选择或输入专业"
                filterable
                allow-create
                default-first-option
              >
                <el-option
                  v-for="item in majorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button class="submit-btn" type="primary" :loading="loading" @click="handleLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register" :disabled="roleType === 'admin'">
          <el-alert
            v-if="roleType === 'admin'"
            type="warning"
            show-icon
            :closable="false"
            title="管理端账号不开放前台注册，请在系统用户管理中创建。"
            class="role-tip"
          />

          <el-form
            v-else
            ref="registerRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
          >
            <el-form-item label="账号" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入账号" clearable />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入密码"
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item label="专业" prop="majorKey">
              <el-select
                v-model="registerForm.majorKey"
                placeholder="请选择或输入专业"
                filterable
                allow-create
                default-first-option
              >
                <el-option
                  v-for="item in majorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button
                class="submit-btn submit-btn-register"
                type="success"
                :loading="loading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="extra-actions">
        <el-button link @click="router.push('/education/pad')">返回角色入口页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loginEducationUser, registerEducationUser } from '@/api/education/auth'
import { getToken, setToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const majorOptions = [
  { label: '数据科学与大数据技术', value: 'data-science' },
  { label: '网络工程', value: 'network-engineering' }
]

const loginDrafts = {
  admin: { username: 'admin', password: '123456', majorKey: '' },
  teacher: { username: 'zhiyu', password: '123456', majorKey: 'data-science' },
  student: { username: '20231113284', password: '123456', majorKey: 'data-science' }
}

const registerDrafts = {
  teacher: { username: '', password: '', confirmPassword: '', roleKey: 'teacher', majorKey: 'data-science' },
  student: { username: '', password: '', confirmPassword: '', roleKey: 'student', majorKey: 'data-science' }
}

const activeTab = ref('login')
const roleType = ref('teacher')
const redirect = ref('/education/teacher/pad')
const loading = ref(false)
const loginRef = ref()
const registerRef = ref()

const loginForm = reactive({
  username: '',
  password: '',
  majorKey: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  roleKey: 'student',
  majorKey: 'data-science'
})

const requiresMajor = computed(() => roleType.value !== 'admin')

const pageTitle = computed(() => {
  if (roleType.value === 'admin') return '管理端登录'
  return roleType.value === 'teacher' ? '教师端登录' : '学生端登录'
})

const pageDescription = computed(() => {
  if (roleType.value === 'admin') {
    return '进入管理端后可统一管理教师、学生、专业信息以及真实数据导入，不需要选择专业。'
  }
  if (roleType.value === 'teacher') {
    return '教师端用于查看学生表现、布置任务、使用师生 AI 互动助手与 RAG 问答。'
  }
  return '学生端用于查看学习诊断、学习规划、成绩预测，并结合 AI 助手进行针对性提升。'
})

const currentLoginRules = computed(() => {
  const rules = {
    username: [{ required: true, trigger: 'blur', message: '请输入账号' }],
    password: [{ required: true, trigger: 'blur', message: '请输入密码' }]
  }
  if (requiresMajor.value) {
    rules.majorKey = [{ required: true, trigger: 'change', message: '请选择专业' }]
  }
  return rules
})

function validateConfirmPassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const registerRules = {
  username: [
    { required: true, trigger: 'blur', message: '请输入账号' },
    { min: 2, max: 20, trigger: 'blur', message: '账号长度需在 2 到 20 位之间' }
  ],
  password: [
    { required: true, trigger: 'blur', message: '请输入密码' },
    { min: 5, max: 20, trigger: 'blur', message: '密码长度需在 5 到 20 位之间' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  majorKey: [{ required: true, trigger: 'change', message: '请选择专业' }]
}

watch(
  () => route.query,
  (query) => {
    const nextRole = String(query?.role || 'teacher').trim().toLowerCase()
    roleType.value = ['admin', 'teacher', 'student'].includes(nextRole) ? nextRole : 'teacher'
    activeTab.value =
      roleType.value === 'admin'
        ? 'login'
        : String(query?.tab || 'login') === 'register'
          ? 'register'
          : 'login'
    redirect.value = query?.redirect ? String(query.redirect) : getDefaultRedirect(roleType.value)
  },
  { immediate: true }
)

watch(
  roleType,
  async (value, oldValue) => {
    if (oldValue && loginDrafts[oldValue]) {
      loginDrafts[oldValue] = { ...loginForm }
    }
    if (oldValue && registerDrafts[oldValue]) {
      registerDrafts[oldValue] = { ...registerForm }
    }

    Object.assign(loginForm, loginDrafts[value] || { username: '', password: '', majorKey: '' })

    if (value === 'teacher' || value === 'student') {
      Object.assign(registerForm, registerDrafts[value])
      registerForm.roleKey = value
    }

    if (value === 'admin') {
      activeTab.value = 'login'
      loginForm.majorKey = ''
    }

    redirect.value = getDefaultRedirect(value)

    await nextTick()
    loginRef.value?.clearValidate()
    registerRef.value?.clearValidate()
  },
  { immediate: true }
)

function getDefaultRedirect(role) {
  if (role === 'admin') return '/education/admin'
  if (role === 'student') return '/education/student/pad'
  return '/education/teacher/pad'
}

function normalizeRoles(roles = []) {
  return roles.map(role => String(role || '').toLowerCase())
}

function hasRoleByType(roles = []) {
  const roleKeys = normalizeRoles(roles)
  if (roleType.value === 'admin') return roleKeys.includes('admin') || roleKeys.includes('manager')
  if (roleType.value === 'teacher') return roleKeys.includes('teacher')
  return roleKeys.includes('student') || roleKeys.includes('role_default')
}

function resolveTarget() {
  const selectedTarget = getDefaultRedirect(roleType.value)
  const currentRedirect = String(redirect.value || '').trim()
  if (!currentRedirect.startsWith('/education')) return selectedTarget
  if (roleType.value === 'admin' && currentRedirect !== '/education/admin') return selectedTarget
  if (roleType.value === 'teacher' && currentRedirect.startsWith('/education/student')) return selectedTarget
  if (roleType.value === 'student' && (currentRedirect.startsWith('/education/teacher') || currentRedirect === '/education/admin')) {
    return selectedTarget
  }
  if (currentRedirect === '/education/pad' || currentRedirect === '/education') return selectedTarget
  return currentRedirect
}

async function handleLogin() {
  if (!loginRef.value) return
  const valid = await loginRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (getToken()) {
      await userStore.logOut()
    }

    const payload = {
      username: loginForm.username,
      password: loginForm.password
    }

    if (requiresMajor.value) {
      payload.majorKey = loginForm.majorKey
    }

    const res = await loginEducationUser(payload)
    setToken(res.token)
    userStore.token = res.token
    const info = await userStore.getInfo()

    if (!hasRoleByType(info.roles || [])) {
      await userStore.logOut()
      ElMessage.error(`当前账号不是${pageTitle.value}对应角色，请切换账号`)
      return
    }

    ElMessage.success('登录成功')
    router.push(resolveTarget())
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (roleType.value === 'admin' || !registerRef.value) return
  const valid = await registerRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await registerEducationUser({
      username: registerForm.username,
      password: registerForm.password,
      roleKey: roleType.value,
      majorKey: registerForm.majorKey
    })

    await ElMessageBox.alert(
      `${roleType.value === 'teacher' ? '教师' : '学生'}账号注册成功，请登录后进入对应页面。`,
      '注册成功',
      { type: 'success' }
    )

    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
    loginForm.majorKey = registerForm.majorKey
    registerForm.password = ''
    registerForm.confirmPassword = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.edu-auth-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgba(2, 132, 199, 0.18), transparent 40%),
    radial-gradient(circle at 80% 10%, rgba(15, 118, 110, 0.16), transparent 35%),
    linear-gradient(140deg, #f4fbff 0%, #edf7f7 42%, #f7f9ee 100%);
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(48px);
  opacity: 0.4;
}

.bg-glow-left {
  left: -140px;
  bottom: -180px;
  background: #0284c7;
}

.bg-glow-right {
  right: -140px;
  top: -180px;
  background: #0f766e;
}

.auth-card {
  width: 760px;
  max-width: calc(100vw - 32px);
  border-radius: 28px;
  border: 1px solid rgba(44, 62, 80, 0.08);
  position: relative;
  z-index: 2;
  box-shadow: 0 22px 54px rgba(18, 49, 77, 0.12);
  background: rgba(255, 255, 255, 0.94);
}

.auth-card :deep(.el-card__body) {
  padding: 34px 28px 24px;
}

.auth-header {
  margin-bottom: 22px;
}

.auth-header h1 {
  margin: 0;
  font-size: 56px;
  line-height: 1.06;
  color: #17324d;
  letter-spacing: 1px;
}

.auth-header p {
  margin: 14px 0 0;
  font-size: 18px;
  line-height: 1.8;
  color: #556c83;
}

.role-switch {
  width: 100%;
  margin-bottom: 22px;
  display: flex;
}

.role-switch :deep(.el-radio-button) {
  flex: 1;
}

.role-switch :deep(.el-radio-button__inner) {
  width: 100%;
  height: 62px;
  font-size: 22px;
  border-radius: 18px;
  border: 1px solid rgba(44, 62, 80, 0.14);
  color: #66788c;
  box-shadow: none;
}

.role-switch :deep(.el-radio-button:first-child .el-radio-button__inner),
.role-switch :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 18px;
}

.role-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(90deg, #4b95f0, #1c8dcf);
  border-color: transparent;
  box-shadow: 0 14px 24px rgba(28, 141, 207, 0.22);
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.auth-tabs :deep(.el-tabs__item) {
  height: 52px;
  font-size: 24px;
  font-weight: 700;
}

.auth-tabs :deep(.el-tabs__active-bar) {
  height: 4px;
  border-radius: 999px;
}

.auth-card :deep(.el-form-item) {
  margin-bottom: 20px;
}

.auth-card :deep(.el-form-item__label) {
  font-size: 17px;
  font-weight: 700;
  color: #1d2b44;
}

.auth-card :deep(.el-input__wrapper),
.auth-card :deep(.el-select__wrapper) {
  min-height: 58px;
  border-radius: 16px;
}

.auth-card :deep(.el-input__inner),
.auth-card :deep(.el-select__placeholder) {
  font-size: 18px;
}

.submit-btn {
  width: 100%;
  height: 62px;
  font-size: 22px;
  border-radius: 18px;
  border: none;
  background: linear-gradient(90deg, #1b8078, #1a8dcb);
  box-shadow: 0 16px 30px rgba(26, 141, 203, 0.18);
}

.submit-btn-register {
  background: linear-gradient(90deg, #3c98f7, #1a8dcb);
}

.role-tip {
  margin-bottom: 12px;
}

.extra-actions {
  margin-top: 10px;
  text-align: center;
}

.extra-actions :deep(.el-button) {
  font-size: 15px;
  color: #5a6e82;
}

@media (max-width: 768px) {
  .edu-auth-page {
    padding: 14px;
  }

  .auth-card {
    width: calc(100vw - 20px);
    border-radius: 20px;
  }

  .auth-card :deep(.el-card__body) {
    padding: 24px 18px 18px;
  }

  .auth-header h1 {
    font-size: 38px;
  }

  .auth-header p {
    font-size: 16px;
  }

  .role-switch :deep(.el-radio-button__inner) {
    height: 54px;
    font-size: 18px;
  }

  .auth-tabs :deep(.el-tabs__item) {
    font-size: 20px;
  }

  .submit-btn {
    font-size: 20px;
  }
}
</style>
