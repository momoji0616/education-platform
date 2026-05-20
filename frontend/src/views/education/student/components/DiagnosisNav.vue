<template>
  <el-card class="diagnosis-nav-card" shadow="never">
    <template #header>
      <div class="diagnosis-nav-title">诊断导航</div>
    </template>

    <button
      v-for="item in items"
      :key="item.path"
      type="button"
      class="diagnosis-nav-item"
      :class="{ active: isActive(item) }"
      @click="go(item)"
    >
      <strong>{{ item.label }}</strong>
      <span>{{ item.desc }}</span>
    </button>
  </el-card>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  query: {
    type: Object,
    default: () => ({})
  }
})

const router = useRouter()
const route = useRoute()

const items = [
  { label: '学业诊断', desc: '薄弱章节、错题和风险等级', path: '/education/student/report' },
  { label: '学习规划', desc: '把当前诊断转成学习任务', path: '/education/student/plan' },
  { label: '智能刷题', desc: '围绕薄弱章节做推荐训练', path: '/education/student/practice' },
  { label: 'RAG 解释', desc: '带着诊断上下文继续追问', path: '/education/rag' }
]

function isActive(item) {
  return route.path === item.path
}

function go(item) {
  router.push({
    path: item.path,
    query: props.query || {}
  })
}
</script>

<style scoped lang="scss">
.diagnosis-nav-card {
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.92);
}

.diagnosis-nav-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.diagnosis-nav-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 14px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.diagnosis-nav-item:last-child {
  margin-bottom: 0;
}

.diagnosis-nav-item strong {
  font-size: 16px;
  color: #0f172a;
}

.diagnosis-nav-item span {
  color: #64748b;
  line-height: 1.6;
  text-align: left;
}

.diagnosis-nav-item.active {
  border-color: rgba(59, 130, 246, 0.35);
  background: rgba(239, 246, 255, 0.85);
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.12);
}

.diagnosis-nav-item:hover {
  border-color: rgba(59, 130, 246, 0.28);
}
</style>
