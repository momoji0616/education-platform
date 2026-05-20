<template>
  <div class="role-shell student-shell">
    <header class="shell-header">
      <div>
        <p class="eyebrow">Student Workspace</p>
        <h1>学生学习诊断工作台</h1>
        <p class="subtitle">
          首页只展示已有诊断数据：历史作答、章节掌握、考试记录和下一步学习动作，避免用空数据撑图表。
        </p>
      </div>
      <div class="header-actions">
        <div class="identity-badge">
          <strong>{{ userStore.name || '--' }}</strong>
          <span>{{ userStore.nickName || userStore.name || '--' }}</span>
        </div>
        <el-button plain @click="router.push('/education/auth?role=teacher&redirect=/education/teacher/pad&demo=teacher')">
          切换教师端
        </el-button>
        <el-button plain @click="router.push('/education/admin')">返回主控端</el-button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="side-nav">
        <div class="nav-title">学生导航</div>
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
            <p class="eyebrow">Personal Diagnosis</p>
            <h2>{{ learningSummary.title }}</h2>
            <p>{{ learningSummary.desc }}</p>
          </div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/education/student/report')">查看诊断报告</el-button>
            <el-button plain @click="router.push({ path: '/education/student/practice', query: diagnosisQuery })">开始针对练习</el-button>
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
                <span>章节掌握走势</span>
                <small>按诊断接口返回的章节正确率展示</small>
              </div>
            </template>
            <div ref="trendRef" class="chart-box chart-box-large"></div>
          </el-card>

          <el-card class="dashboard-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>章节补强排序</span>
                <small>正确率低的章节优先练习</small>
              </div>
            </template>
            <div ref="chapterRankRef" class="chart-box"></div>
          </el-card>

          <el-card class="dashboard-card" shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>掌握度分布</span>
                <small>统计稳定、待巩固、需补强章节</small>
              </div>
            </template>
            <div ref="masteryRef" class="chart-box"></div>
          </el-card>

          <el-card class="path-panel" shadow="hover">
            <template #header>个性化学习路径</template>
            <div v-for="item in pathItems" :key="item.title" class="path-item" @click="router.push(item.path)">
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
  getStudentDiagnosisChapters,
  getStudentDiagnosisOverview,
  listStudentExamScore
} from '@/api/education/student'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const dashboardLoading = ref(false)
const overview = ref({})
const chapters = ref([])
const examScores = ref([])
const trendRef = ref(null)
const chapterRankRef = ref(null)
const masteryRef = ref(null)
let trendChart = null
let chapterRankChart = null
let masteryChart = null

const navItems = [
  { label: '历史做题', desc: '查看每次作答和得分轨迹', path: '/education/student/history' },
  { label: '学生诊断', desc: '定位薄弱章节和错题依据', path: '/education/student/report' },
  { label: '学习规划', desc: '把诊断结果转成阶段任务', path: '/education/student/plan' },
  { label: '智能刷题', desc: '基于薄弱章节推荐练习', path: '/education/student/practice' },
  { label: '成绩预测', desc: '查看变量对成绩的影响', path: '/education/student/prediction' },
  { label: '师生AI助手', desc: '向老师或 AI 继续追问', path: '/education/student/assistant' },
  { label: 'RAG智能问答', desc: '基于资料进行课程问答', path: '/education/rag' }
]

const moduleCards = [
  { title: '历史做题', description: '回看真实轨迹', path: '/education/student/history' },
  { title: '诊断报告', description: '定位薄弱章节', path: '/education/student/report' },
  { title: '学习规划', description: '生成阶段任务', path: '/education/student/plan' },
  { title: '智能刷题', description: '针对性练习', path: '/education/student/practice' },
  { title: '成绩预测', description: '预判趋势', path: '/education/student/prediction' },
  { title: '师生AI助手', description: '互动问答', path: '/education/student/assistant' },
  { title: 'RAG智能问答', description: '问知识库', path: '/education/rag' }
]

const weakestChapter = computed(() => {
  return [...chapters.value]
    .filter((item) => safeNumber(item.correctRate) > 0)
    .sort((a, b) => safeNumber(a.correctRate) - safeNumber(b.correctRate))[0] || {}
})

const examAverage = computed(() => average(examScores.value.map((item) => item.score)))

const diagnosisQuery = computed(() => ({
  source: 'student-home',
  courseName: weakestChapter.value.courseName || '',
  chapterCode: weakestChapter.value.chapterCode || '',
  chapterName: weakestChapter.value.chapterName || ''
}))

const learningSummary = computed(() => {
  const chapterName = weakestChapter.value.chapterName || ''
  const rate = safeNumber(weakestChapter.value.correctRate)
  if (chapterName && rate && rate < 65) {
    return {
      title: `${chapterName} 是当前优先补强章节`,
      desc: `章节正确率约 ${formatPercent(rate)}，建议先进入智能刷题，再用 RAG 追问错因。`
    }
  }
  if (safeNumber(overview.value.correctRate)) {
    return {
      title: '当前学习状态较稳定',
      desc: `历史正确率约 ${formatPercent(overview.value.correctRate)}，可继续按学习规划推进。`
    }
  }
  return {
    title: '等待生成个人学习诊断',
    desc: '完成作答或考试后，这里会自动形成个性化学习路径。'
  }
})

const metricCards = computed(() => {
  const cards = [
    { label: '历史正确率', value: formatPercent(overview.value.correctRate), tip: '真实作答诊断' },
    { label: '历史作答', value: displayNumber(overview.value.answerCount), tip: '题目轨迹数量' },
    { label: '诊断章节', value: displayNumber(chapters.value.length), tip: '已覆盖模块' },
    { label: '待补强章节', value: displayNumber(weakChapterCount.value), tip: '正确率低于 60%' }
  ]
  if (examAverage.value) {
    cards.push({ label: '考试均分', value: `${Math.round(examAverage.value)}分`, tip: '已评分考试' })
  }
  return cards
})

const weakChapterCount = computed(() =>
  chapters.value.filter((item) => {
    const rate = safeNumber(item.correctRate)
    return rate > 0 && rate < 60
  }).length
)

const pathItems = computed(() => [
  { title: '先看诊断依据', desc: '查看章节正确率、错题和解析明细', path: { path: '/education/student/report', query: diagnosisQuery.value } },
  { title: '生成学习计划', desc: '把薄弱章节转成阶段性学习任务', path: { path: '/education/student/plan', query: diagnosisQuery.value } },
  { title: '立即针对刷题', desc: '围绕薄弱章节做推荐练习', path: { path: '/education/student/practice', query: diagnosisQuery.value } },
  { title: '用 RAG 追问错因', desc: '带着章节上下文进入知识库问答', path: { path: '/education/rag', query: diagnosisQuery.value } }
])

function isActive(path) {
  return route.path === path
}

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function average(values) {
  const nums = values.map((item) => safeNumber(item)).filter((item) => item > 0)
  return nums.length ? nums.reduce((sum, item) => sum + item, 0) / nums.length : 0
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

function settledValue(result, fallback) {
  return result.status === 'fulfilled' ? result.value : fallback
}

function ensureChart(instance, el) {
  if (!el) return null
  return instance || echarts.init(el)
}

function masteryDistribution() {
  const result = [
    { name: '稳定掌握', value: 0 },
    { name: '待巩固', value: 0 },
    { name: '需补强', value: 0 }
  ]
  chapters.value.forEach((item) => {
    const rate = safeNumber(item.correctRate)
    if (rate >= 75) result[0].value += 1
    else if (rate >= 60) result[1].value += 1
    else result[2].value += 1
  })
  return result
}

function renderCharts() {
  trendChart = ensureChart(trendChart, trendRef.value)
  chapterRankChart = ensureChart(chapterRankChart, chapterRankRef.value)
  masteryChart = ensureChart(masteryChart, masteryRef.value)
  if (!trendChart || !chapterRankChart || !masteryChart) return

  const chapterRows = chapters.value.slice(0, 8)
  const chapterNames = chapterRows.map((item, index) => item.chapterName || item.chapterCode || `章节${index + 1}`)
  const chapterRates = chapterRows.map((item) => safeNumber(item.correctRate))
  const answerCounts = chapterRows.map((item) => safeNumber(item.answerCount))
  const rankRows = [...chapters.value]
    .filter((item) => safeNumber(item.correctRate) > 0)
    .sort((a, b) => safeNumber(a.correctRate) - safeNumber(b.correctRate))
    .slice(0, 8)
    .reverse()

  trendChart.setOption({
    color: ['#0284c7', '#f59e0b'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { fontSize: 17 } },
    grid: { left: 46, right: 42, top: 54, bottom: 34 },
    xAxis: { type: 'category', data: chapterNames.length ? chapterNames : ['暂无数据'], axisLabel: { fontSize: 16 } },
    yAxis: [
      { type: 'value', max: 100, axisLabel: { formatter: '{value}%', fontSize: 16 } },
      { type: 'value', axisLabel: { fontSize: 16 } }
    ],
    series: [
      { name: '章节正确率', type: 'line', smooth: true, data: chapterRates.length ? chapterRates : [0], areaStyle: { opacity: 0.18 } },
      { name: '作答量', type: 'bar', yAxisIndex: 1, barWidth: 18, data: answerCounts.length ? answerCounts : [0] }
    ]
  })

  chapterRankChart.setOption({
    color: ['#ef4444'],
    tooltip: { trigger: 'axis', formatter: '{b}<br/>正确率：{c}%' },
    grid: { left: 118, right: 26, top: 22, bottom: 28 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%', fontSize: 16 } },
    yAxis: {
      type: 'category',
      data: rankRows.length ? rankRows.map((item, index) => item.chapterName || item.chapterCode || `章节${index + 1}`) : ['暂无数据'],
      axisLabel: { fontSize: 16 }
    },
    series: [{
      type: 'bar',
      barWidth: 18,
      data: rankRows.length ? rankRows.map((item) => safeNumber(item.correctRate)) : [0],
      markLine: { data: [{ xAxis: 60, name: '补强线' }], label: { fontSize: 14 } }
    }]
  })

  masteryChart.setOption({
    color: ['#0f766e', '#f59e0b', '#ef4444'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 16 } },
    series: [{
      type: 'pie',
      radius: ['44%', '70%'],
      center: ['50%', '44%'],
      label: { fontSize: 16, formatter: '{b}\n{c}章' },
      data: chapters.value.length ? masteryDistribution() : [{ name: '暂无诊断章节', value: 1 }]
    }]
  })
}

async function loadDashboard() {
  dashboardLoading.value = true
  try {
    const [overviewRes, chapterRes, examScoreRes] = await Promise.allSettled([
      getStudentDiagnosisOverview(),
      getStudentDiagnosisChapters(),
      listStudentExamScore()
    ])
    overview.value = unwrapMapResponse(settledValue(overviewRes, {}))
    chapters.value = unwrapListResponse(settledValue(chapterRes, {}))
    examScores.value = unwrapListResponse(settledValue(examScoreRes, {}))
    await nextTick()
    renderCharts()
  } finally {
    dashboardLoading.value = false
  }
}

function resizeCharts() {
  ;[trendChart, chapterRankChart, masteryChart].forEach((chart) => chart && chart.resize())
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  ;[trendChart, chapterRankChart, masteryChart].forEach((chart) => chart && chart.dispose())
})
</script>

<style scoped lang="scss">
.role-shell {
  min-height: 100vh;
  padding: 30px 30px 42px;
}

.student-shell {
  background:
    radial-gradient(circle at 10% 0%, rgba(14, 165, 233, 0.16), transparent 26%),
    radial-gradient(circle at 88% 8%, rgba(20, 184, 166, 0.12), transparent 24%),
    linear-gradient(180deg, #f6fbff 0%, #fffdf5 100%);
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
  color: #0369a1;
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
  border: 1px solid rgba(14, 165, 233, 0.2);
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
.path-panel,
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
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(240, 249, 255, 0.95);
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
.path-panel {
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

.path-item {
  padding: 15px;
  border-radius: 16px;
  background: #f0f9ff;
  border: 1px solid rgba(14, 165, 233, 0.18);
  cursor: pointer;
}

.path-item + .path-item {
  margin-top: 10px;
}

.path-item strong,
.module-link strong {
  display: block;
  color: #0f172a;
  font-size: 18px;
}

.path-item span,
.module-link span {
  display: block;
  color: #64748b;
  font-size: 15px;
  line-height: 1.6;
}

.module-strip {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
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
  .metric-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

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
