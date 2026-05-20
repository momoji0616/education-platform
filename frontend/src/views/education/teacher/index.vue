<template>
  <div class="role-shell teacher-shell">
    <header class="shell-header">
      <div>
        <p class="eyebrow">Teacher Workspace</p>
        <h1>教师教学数据工作台</h1>
        <p class="subtitle">
          从班级掌握度、薄弱章节、AI 互动热点到智能组卷建议，集中呈现教师演示时最能说明价值的真实教学闭环。
        </p>
      </div>
      <div class="header-actions">
        <div class="identity-badge">
          <strong>{{ userStore.name || '--' }}</strong>
          <span>{{ userStore.nickName || userStore.name || '--' }}</span>
        </div>
        <el-button plain @click="router.push('/education/auth?role=student&redirect=/education/student/pad&demo=student')">
          切换学生端
        </el-button>
        <el-button plain @click="router.push('/education/admin')">返回主控端</el-button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="side-nav">
        <div class="nav-title">教师导航</div>
        <button
          v-for="item in navItems"
          :key="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          @click="router.push(item.path)"
        >
          <strong>{{ item.label }}</strong>
          <span>{{ item.desc }}</span>
        </button>
      </aside>

      <main class="content-area" v-loading="dashboardLoading">
        <section class="hero-panel">
          <div class="hero-copy">
            <p class="eyebrow">Class Insight</p>
            <h2>{{ riskSummary.title }}</h2>
            <p>{{ riskSummary.desc }}</p>
          </div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/education/teacher/analysis')">查看学情分析</el-button>
            <el-button plain @click="router.push({ path: '/education/teacher/paper', query: paperQuery })">按薄弱点组卷</el-button>
            <el-button link type="primary" @click="loadDashboard">刷新数据</el-button>
          </div>
        </section>

        <section class="metric-grid">
          <div v-for="item in metricCards" :key="item.label" class="metric-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.tip }}</em>
          </div>
        </section>

        <section class="dashboard-grid">
          <el-card class="dashboard-card dashboard-card-wide" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>薄弱章节优先级</span>
                <small>正确率越低、作答越多，越适合优先讲评</small>
              </div>
            </template>
            <div ref="weakRef" class="chart-box chart-box-large"></div>
          </el-card>

          <el-card class="dashboard-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>AI 互动热点</span>
                <small>学生真实追问集中在哪些模块</small>
              </div>
            </template>
            <div ref="aiHotRef" class="chart-box"></div>
          </el-card>

          <el-card class="dashboard-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>教学能力画像</span>
                <small>覆盖、活跃、稳定性综合评估</small>
              </div>
            </template>
            <div ref="radarRef" class="chart-box"></div>
          </el-card>

          <el-card class="action-panel" shadow="hover">
            <template #header>下一步教学动作</template>
            <div v-for="item in actionItems" :key="item.title" class="action-item" @click="router.push(item.path)">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </el-card>
        </section>

        <section class="module-strip">
          <button v-for="item in moduleCards" :key="item.path" class="module-link" @click="router.push(item.path)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import {
  getTeacherAiAssistantSummary,
  getTeacherAnalysisChapters,
  getTeacherAnalysisOverview
} from '@/api/education/teacher'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const dashboardLoading = ref(false)
const overview = ref({})
const chapters = ref([])
const aiSummary = ref({})
const weakRef = ref(null)
const aiHotRef = ref(null)
const radarRef = ref(null)
let weakChart = null
let aiHotChart = null
let radarChart = null

const navItems = [
  { label: '学生管理', desc: '学生画像、历史作答和班级分层', path: '/education/teacher/students' },
  { label: '学情分析', desc: '课程章节掌握度、薄弱点和讲评依据', path: '/education/teacher/analysis' },
  { label: '智能组卷', desc: '围绕薄弱章节快速生成练习卷', path: '/education/teacher/paper' },
  { label: '成绩预测', desc: '查看预测结果和影响变量', path: '/education/teacher/qa' },
  { label: '师生AI助手', desc: '追踪师生问答、群聊和私聊', path: '/education/teacher/assistant' },
  { label: 'RAG智能问答', desc: '基于课程资料进行检索问答', path: '/education/teacher/rag' }
]

const moduleCards = [
  { title: '学生管理', description: '看学生画像', path: '/education/teacher/students' },
  { title: '学情分析', description: '找薄弱章节', path: '/education/teacher/analysis' },
  { title: '智能组卷', description: '按薄弱点出题', path: '/education/teacher/paper' },
  { title: '成绩预测', description: '预判成绩趋势', path: '/education/teacher/qa' },
  { title: '师生AI助手', description: '跟进学生问题', path: '/education/teacher/assistant' },
  { title: 'RAG智能问答', description: '调用知识库', path: '/education/teacher/rag' }
]

const aiOverview = computed(() => aiSummary.value?.overview || {})

const weakestChapter = computed(() => {
  return [...chapters.value].sort((a, b) => safeNumber(a.correctRate) - safeNumber(b.correctRate))[0] || {}
})

const paperQuery = computed(() => ({
  source: 'teacher-home',
  courseName: weakestChapter.value.courseName || '',
  chapterCode: weakestChapter.value.chapterCode || '',
  chapterName: weakestChapter.value.chapterName || ''
}))

const riskSummary = computed(() => {
  const rate = safeNumber(overview.value.correctRate)
  const weak = weakestChapter.value
  if (!chapters.value.length && !rate) {
    return { title: '等待接入班级学情数据', desc: '当前首页已准备好按真实接口展示，产生作答记录后会自动形成教学建议。' }
  }
  if (safeNumber(weak.correctRate) && safeNumber(weak.correctRate) < 60) {
    return {
      title: `${weak.chapterName || '薄弱章节'} 需要优先讲评`,
      desc: `当前正确率约 ${formatPercent(weak.correctRate)}，建议直接进入智能组卷生成针对性练习。`
    }
  }
  return {
    title: '班级整体掌握较稳定',
    desc: `当前平均正确率约 ${formatPercent(rate)}，可继续关注 AI 追问热点和个别薄弱模块。`
  }
})

const metricCards = computed(() => [
  { label: '班级学生', value: displayNumber(overview.value.studentCount), tip: '可见专业范围' },
  { label: '历史作答', value: displayNumber(overview.value.answerCount), tip: '真实题库记录' },
  { label: '平均正确率', value: formatPercent(overview.value.correctRate), tip: '班级掌握度' },
  { label: '知识点覆盖', value: displayNumber(overview.value.knowledgePointCount), tip: '诊断范围' },
  { label: 'AI 互动', value: displayNumber(aiOverview.value.interactionCount), tip: '学生追问次数' },
  { label: '热点模块', value: displayNumber(aiOverview.value.chapterCount), tip: 'AI 场景聚类' }
])

const actionItems = computed(() => {
  const weak = weakestChapter.value
  const chapterName = weak.chapterName || '当前薄弱章节'
  return [
    { title: `讲评 ${chapterName}`, desc: '进入学情分析查看章节、课程和作答依据', path: { path: '/education/teacher/analysis', query: paperQuery.value } },
    { title: '生成针对性练习', desc: '把薄弱章节直接带入智能组卷', path: { path: '/education/teacher/paper', query: paperQuery.value } },
    { title: '追踪学生提问', desc: '查看 AI 互动热点和具体追问记录', path: '/education/teacher/assistant' }
  ]
})

function isActive(path) {
  return route.path === path
}

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function displayNumber(value) {
  const num = safeNumber(value)
  return num ? String(num) : '--'
}

function formatPercent(value) {
  const num = safeNumber(value)
  return num ? `${Math.round(num)}%` : '--'
}

function unwrapListResponse(res) {
  return Array.isArray(res?.data) ? res.data : []
}

function unwrapMapResponse(res) {
  return res?.data && typeof res.data === 'object' && !Array.isArray(res.data) ? res.data : {}
}

function ensureChart(instance, el) {
  if (!el) return null
  return instance || echarts.init(el)
}

function buildHotRows() {
  const rows = Array.isArray(aiSummary.value.moduleInteractions) ? aiSummary.value.moduleInteractions : []
  if (rows.length) return rows
  const grouped = new Map()
  ;(aiSummary.value.recentInteractions || []).forEach((item) => {
    const name = item.chapterName || item.courseName || '未分类模块'
    const current = grouped.get(name) || { chapterName: name, askCount: 0, studentCount: 0, students: new Set() }
    current.askCount += 1
    if (item.studentNo) current.students.add(item.studentNo)
    current.studentCount = current.students.size || 1
    grouped.set(name, current)
  })
  return Array.from(grouped.values())
}

function radarValues() {
  const stableCount = chapters.value.filter((item) => safeNumber(item.correctRate) >= 75).length
  return [
    Math.min(100, safeNumber(overview.value.correctRate)),
    Math.min(100, safeNumber(overview.value.answerCount) / 10),
    Math.min(100, safeNumber(overview.value.studentCount) * 8),
    Math.min(100, safeNumber(overview.value.knowledgePointCount) * 6),
    chapters.value.length ? Math.round((stableCount / chapters.value.length) * 100) : 0,
    Math.min(100, safeNumber(aiOverview.value.interactionCount) * 6)
  ]
}

function renderCharts() {
  weakChart = ensureChart(weakChart, weakRef.value)
  aiHotChart = ensureChart(aiHotChart, aiHotRef.value)
  radarChart = ensureChart(radarChart, radarRef.value)
  if (!weakChart || !aiHotChart || !radarChart) return

  const weakRows = [...chapters.value]
    .sort((a, b) => safeNumber(a.correctRate) - safeNumber(b.correctRate))
    .slice(0, 8)
    .reverse()
  const weakNames = weakRows.map((item) => item.chapterName || item.chapterCode || '未分类模块')
  const weakRates = weakRows.map((item) => safeNumber(item.correctRate))
  const answerCounts = weakRows.map((item) => safeNumber(item.answerCount))
  const hotRows = buildHotRows().slice(0, 8)

  weakChart.setOption({
    color: ['#ef4444', '#0ea5e9'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { fontSize: 16 } },
    grid: { left: 120, right: 42, top: 48, bottom: 28 },
    xAxis: [
      { type: 'value', max: 100, axisLabel: { formatter: '{value}%', fontSize: 15 } },
      { type: 'value', axisLabel: { fontSize: 15 } }
    ],
    yAxis: { type: 'category', data: weakNames.length ? weakNames : ['暂无数据'], axisLabel: { fontSize: 15 } },
    series: [
      { name: '正确率', type: 'bar', barWidth: 18, data: weakRates.length ? weakRates : [0] },
      { name: '作答量', type: 'line', xAxisIndex: 1, smooth: true, data: answerCounts.length ? answerCounts : [0] }
    ]
  })

  aiHotChart.setOption({
    color: ['#f97316'],
    tooltip: {
      trigger: 'item',
      formatter: (params) => `${params.data[2]}<br/>提问 ${params.data[0]} 次<br/>学生 ${params.data[1]} 人`
    },
    grid: { left: 48, right: 22, top: 24, bottom: 46 },
    xAxis: { name: '提问次数', axisLabel: { fontSize: 15 } },
    yAxis: { name: '学生数', axisLabel: { fontSize: 15 } },
    series: [{
      type: 'scatter',
      symbolSize: (data) => Math.max(18, Math.min(58, data[0] * 8 + data[1] * 4)),
      label: { show: true, formatter: (params) => params.data[2], fontSize: 13, position: 'top' },
      data: hotRows.length
        ? hotRows.map((item) => [safeNumber(item.askCount), safeNumber(item.studentCount), item.chapterName || item.courseName || '未分类'])
        : [[0, 0, '暂无互动']]
    }]
  })

  radarChart.setOption({
    color: ['#0284c7'],
    tooltip: {},
    radar: {
      radius: '62%',
      indicator: [
        { name: '平均掌握', max: 100 },
        { name: '作答活跃', max: 100 },
        { name: '学生覆盖', max: 100 },
        { name: '知识覆盖', max: 100 },
        { name: '稳定模块', max: 100 },
        { name: 'AI 互动', max: 100 }
      ],
      axisName: { fontSize: 15, color: '#334155' }
    },
    series: [{ type: 'radar', areaStyle: { opacity: 0.24 }, data: [{ value: radarValues(), name: '教学画像' }] }]
  })
}

async function loadDashboard() {
  dashboardLoading.value = true
  try {
    const [overviewRes, chapterRes, assistantRes] = await Promise.all([
      getTeacherAnalysisOverview(),
      getTeacherAnalysisChapters(),
      getTeacherAiAssistantSummary({ recentLimit: 100, weakChapterLimit: 8 })
    ])
    overview.value = unwrapMapResponse(overviewRes)
    chapters.value = unwrapListResponse(chapterRes)
    aiSummary.value = unwrapMapResponse(assistantRes)
    await nextTick()
    renderCharts()
  } finally {
    dashboardLoading.value = false
  }
}

function resizeCharts() {
  ;[weakChart, aiHotChart, radarChart].forEach((chart) => chart && chart.resize())
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  ;[weakChart, aiHotChart, radarChart].forEach((chart) => chart && chart.dispose())
})
</script>

<style scoped lang="scss">
.role-shell {
  min-height: 100vh;
  padding: 30px 30px 42px;
}

.teacher-shell {
  background:
    radial-gradient(circle at 10% 0%, rgba(249, 115, 22, 0.16), transparent 26%),
    radial-gradient(circle at 88% 8%, rgba(14, 165, 233, 0.12), transparent 24%),
    linear-gradient(180deg, #fffaf4 0%, #eef6ff 100%);
}

.shell-header,
.hero-panel,
.chart-header,
.hero-actions {
  display: flex;
  align-items: center;
}

.shell-header {
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #c2410c;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #0f172a;
  letter-spacing: 0;
}

h1 {
  font-size: 46px;
  line-height: 1.14;
}

h2 {
  font-size: 34px;
  line-height: 1.25;
}

.subtitle,
.hero-copy p {
  max-width: 920px;
  color: #475569;
  font-size: 20px;
  line-height: 1.8;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.identity-badge {
  display: flex;
  flex-direction: column;
  min-width: 180px;
  padding: 14px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(249, 115, 22, 0.2);
  color: #0f172a;
}

.identity-badge strong {
  font-size: 18px;
}

.identity-badge span {
  color: #64748b;
  font-size: 15px;
}

.shell-body {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  gap: 22px;
}

.side-nav,
.hero-panel,
.metric-card,
.dashboard-card,
.action-panel,
.module-link {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.side-nav {
  padding: 20px;
  border-radius: 22px;
  height: fit-content;
}

.nav-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.nav-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  margin-bottom: 12px;
  cursor: pointer;
}

.nav-item strong {
  display: block;
  color: #0f172a;
  font-size: 21px;
}

.nav-item span {
  color: #64748b;
  font-size: 17px;
  line-height: 1.7;
}

.nav-item.active {
  border-color: rgba(249, 115, 22, 0.5);
  background: rgba(255, 247, 237, 0.95);
}

.content-area {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-panel {
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  border-radius: 24px;
}

.hero-copy p {
  margin: 10px 0 0;
}

.hero-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 310px;
}

.metric-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.metric-card {
  min-height: 126px;
  padding: 18px;
  border-radius: 18px;
}

.metric-card span,
.metric-card em {
  display: block;
  color: #64748b;
  font-size: 15px;
  font-style: normal;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  color: #0f172a;
  font-size: 34px;
  line-height: 1;
}

.dashboard-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-card,
.action-panel {
  min-height: 350px;
  border-radius: 22px;
}

.dashboard-card-wide {
  grid-column: span 2;
}

.chart-header {
  justify-content: space-between;
  gap: 12px;
  font-weight: 800;
}

.chart-header span {
  font-size: 20px;
  color: #0f172a;
}

.chart-header small {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.chart-box {
  width: 100%;
  height: 286px;
}

.chart-box-large {
  height: 300px;
}

.action-item {
  padding: 16px;
  border-radius: 16px;
  background: #fff7ed;
  border: 1px solid rgba(249, 115, 22, 0.18);
  cursor: pointer;
}

.action-item + .action-item {
  margin-top: 12px;
}

.action-item strong,
.module-link strong {
  display: block;
  color: #0f172a;
  font-size: 18px;
}

.action-item span,
.module-link span {
  display: block;
  color: #64748b;
  font-size: 15px;
  line-height: 1.6;
}

.module-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.module-link {
  min-height: 92px;
  padding: 16px;
  text-align: left;
  border-radius: 18px;
  cursor: pointer;
}

@media (max-width: 1500px) {
  .metric-grid,
  .module-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .shell-body,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-card-wide {
    grid-column: span 1;
  }

  .hero-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .module-strip {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 36px;
  }
}
</style>
