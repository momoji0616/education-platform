<template>
  <div class="education-entry" v-loading="redirecting">
    <span class="education-entry-text">正在进入教育工作台...</span>
  </div>
</template>

<script setup>
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const redirecting = ref(true)

function normalizeRoles(roles = []) {
  return roles.map((role) => String(role || '').toLowerCase())
}

function buildTargetPath() {
  const roleKeys = normalizeRoles(userStore.roles || [])
  const query = { ...route.query }
  delete query.redirect

  if (roleKeys.includes('admin') || roleKeys.includes('manager')) {
    return { path: '/education/admin', query }
  }
  if (roleKeys.includes('teacher')) {
    return { path: '/education/teacher/pad', query }
  }
  if (roleKeys.includes('student') || roleKeys.includes('role_default')) {
    return { path: '/education/student/pad', query }
  }
  return { path: '/education/auth', query: { redirect: route.fullPath } }
}

onMounted(() => {
  router.replace(buildTargetPath())
})
</script>

<style scoped>
.education-entry {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f4fbff 0%, #edf7f7 42%, #f7f9ee 100%);
}

.education-entry-text {
  color: #35566e;
  font-size: 16px;
}
</style>
