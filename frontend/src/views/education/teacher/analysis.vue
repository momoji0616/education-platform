<template>
  <div class="analysis-shell">
    <aside class="side-nav">
      <div class="nav-title">教师导航</div>
      <button class="nav-item" @click="router.push('/education/teacher/students')">
        <strong>学生管理</strong>
        <span>查看学生做题与近期学习表现</span>
      </button>
      <button class="nav-item active">
        <strong>学情分析</strong>
        <span>查看课程与模块层面的真实学情</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/paper')">
        <strong>智能组卷</strong>
        <span>按课程、模块与题型生成试卷草案</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/qa')">
        <strong>成绩预测</strong>
        <span>上传变量数据训练模型并预测成绩</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/assistant')">
        <strong>师生AI助手</strong>
        <span>查看互动记录、高频提问和薄弱点</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/rag')">
        <strong>RAG智能问答</strong>
        <span>基于 RAG 做课程资料智能问答</span>
      </button>
    </aside>

    <main class="content-area">
      <section class="hero-card">
        <div>
          <p class="eyebrow">Major Insight</p>
          <h1>教师学情分析</h1>
          <p class="hero-text">
            这里只保留班级概览、AI 互动概况和共性薄弱点，去掉冗长表格与动作区，让信息更聚焦。
          </p>
        </div>
        <div class="hero-actions">
          <el-button plain @click="router.push('/education/teacher/pad')">返回教师首页</el-button>
          <el-button type="primary" :loading="loading" @click="loadAnalysis">刷新分析</el-button>
        </div>
      </section>

      <el-alert
        v-if="bindingMessage"
        :title="bindingMessage"
        :type="legacyBound ? 'success' : 'warning'"
        :closable="false"
        class="notice-card"
        show-icon
      />

      <section class="overview-grid">
        <el-card v-for="card in overviewCards" :key="card.label" class="overview-card" shadow="never">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <p>{{ card.tip }}</p>
        </el-card>
      </section>

      <section class="ai-summary-grid">
        <el-card v-for="card in aiSummaryCards" :key="card.label" class="summary-card" shadow="never">
          <span class="summary-label">{{ card.label }}</span>
          <strong class="summary-value">{{ card.value }}</strong>
          <p class="summary-tip">{{ card.tip }}</p>
          <el-button type="primary" link class="summary-link" @click="openSummaryDetail(card.key)">查看详情</el-button>
        </el-card>
      </section>

      <section class="content-grid compact-grid">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>专业风险诊断</span>
              <el-tag :type="riskSummary.type" effect="dark">{{ riskSummary.label }}</el-tag>
            </div>
          </template>
          <p class="panel-note">规则诊断：综合平均正确率、最薄弱模块、互动热度与覆盖学生数生成，不是机器学习模型。</p>
          <div class="risk-list">
            <div v-for="item in riskItems" :key="item.title" class="risk-item">
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <el-tag :type="item.type" effect="light">{{ item.level }}</el-tag>
            </div>
          </div>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>共性薄弱点</span>
              <el-button plain size="small" @click="loadAnalysis">刷新数据</el-button>
            </div>
          </template>
          <div class="weak-list">
            <article v-for="item in weakPointCards" :key="item.key" class="weak-card">
              <div class="weak-top">
                <strong>{{ item.title }}</strong>
                <span>{{ item.rateText }}</span>
              </div>
              <p>{{ item.desc }}</p>
              <div class="weak-actions">
                <el-button type="primary" link @click="goToStudentsWithChapter(item.raw)">查看详情</el-button>
                <el-button type="warning" link @click="goToPaperWithRow(item.raw)">去组卷</el-button>
              </div>
            </article>
          </div>
        </el-card>
      </section>
    </main>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="760px">
      <el-table
        v-if="detailType === 'interactions'"
        :data="recentInteractionRows"
        size="small"
        max-height="420"
        :empty-text="detailEmptyText"
      >
        <el-table-column prop="displayName" label="学生" width="110" />
        <el-table-column prop="courseName" label="课程" min-width="120" />
        <el-table-column prop="chapterName" label="模块" min-width="140" />
        <el-table-column prop="askedQuestion" label="提问内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>

      <el-table
        v-else-if="detailType === 'students'"
        :data="interactionStudentRows"
        size="small"
        max-height="420"
        :empty-text="detailEmptyText"
      >
        <el-table-column prop="displayName" label="学生" min-width="120" />
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="interactionCount" label="互动次数" width="100" />
        <el-table-column prop="latestAskTime" label="最近互动时间" width="180" />
      </el-table>

      <el-table
        v-else
        :data="moduleDetailRows"
        size="small"
        max-height="420"
        :empty-text="detailEmptyText"
      >
        <el-table-column prop="courseName" label="课程" min-width="150" />
        <el-table-column prop="chapterName" label="模块" min-width="180" />
        <el-table-column prop="askCount" label="提问次数" width="110" />
        <el-table-column prop="studentCount" label="涉及学生" width="110" />
        <el-table-column prop="latestAskTime" label="最近提问时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getTeacherAiAssistantSummary,
  getTeacherAnalysisChapters,
  getTeacherAnalysisOverview
} from '@/api/education/teacher'

const router = useRouter()

const loading = ref(false)
const legacyBound = ref(false)
const bindingMessage = ref('')
const legacyOverview = ref({})
const chapterAnalysis = ref([])
const aiSummary = ref({})
const detailVisible = ref(false)
const detailType = ref('interactions')

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function formatPercent(value) {
  return `${safeNumber(value).toFixed(0)}%`
}

function unwrapListResponse(res) {
  return Array.isArray(res?.data) ? res.data : []
}

function unwrapMapResponse(res) {
  return res?.data && typeof res.data === 'object' && !Array.isArray(res.data) ? res.data : {}
}

const overviewCards = computed(() => [
  {
    label: '专业学生数',
    value: safeNumber(legacyOverview.value.studentCount) || '暂无',
    tip: '按当前教师可见专业范围聚合'
  },
  {
    label: '历史答题总量',
    value: safeNumber(legacyOverview.value.answerCount) || '暂无',
    tip: '来自真实题库与历史答题记录'
  },
  {
    label: '平均正确率',
    value: legacyOverview.value.correctRate ? formatPercent(legacyOverview.value.correctRate) : '暂无',
    tip: '用于判断整体掌握情况'
  },
  {
    label: '覆盖知识点数',
    value: safeNumber(legacyOverview.value.knowledgePointCount) || '暂无',
    tip: '当前学情分析已覆盖的知识点范围'
  }
])

const aiOverview = computed(() => aiSummary.value.overview || {})

const aiSummaryCards = computed(() => [
  {
    key: 'interactions',
    label: '互动总数',
    value: safeNumber(aiOverview.value.interactionCount),
    tip: '学生在 AI 助手中的累计互动次数'
  },
  {
    key: 'students',
    label: '互动学生',
    value: safeNumber(aiOverview.value.studentCount),
    tip: '有过 AI 互动记录的学生人数'
  },
  {
    key: 'modules',
    label: '涉及模块',
    value: safeNumber(aiOverview.value.chapterCount),
    tip: '当前互动涉及到的课程模块数'
  }
])

const recentInteractionRows = computed(() =>
  Array.isArray(aiSummary.value.recentInteractions) ? aiSummary.value.recentInteractions : []
)

const interactionStudentRows = computed(() => {
  const grouped = new Map()
  recentInteractionRows.value.forEach(item => {
    const key = item.studentNo || item.displayName || 'unknown'
    const current = grouped.get(key) || {
      displayName: item.displayName || '未命名学生',
      studentNo: item.studentNo || '--',
      interactionCount: 0,
      latestAskTime: item.createTime || '--'
    }
    current.interactionCount += 1
    if ((item.createTime || '') > current.latestAskTime) current.latestAskTime = item.createTime
    grouped.set(key, current)
  })
  return Array.from(grouped.values()).sort((a, b) => b.interactionCount - a.interactionCount)
})

const moduleDetailRows = computed(() => {
  const rows = Array.isArray(aiSummary.value.moduleInteractions) ? aiSummary.value.moduleInteractions : []
  if (rows.length) {
    return rows.map(item => ({
      courseName: item.courseName || '未分类课程',
      chapterName: item.chapterName || '未分类模块',
      askCount: safeNumber(item.askCount),
      studentCount: safeNumber(item.studentCount),
      latestAskTime: item.latestAskTime || '--'
    }))
  }

  const grouped = new Map()
  recentInteractionRows.value.forEach(item => {
    const courseName = item.courseName || '未分类课程'
    const chapterName = item.chapterName || '未分类模块'
    const key = `${courseName}__${chapterName}`
    const current = grouped.get(key) || {
      courseName,
      chapterName,
      askCount: 0,
      studentSet: new Set(),
      latestAskTime: item.createTime || '--'
    }
    current.askCount += 1
    if (item.studentNo) current.studentSet.add(item.studentNo)
    if ((item.createTime || '') > current.latestAskTime) current.latestAskTime = item.createTime
    grouped.set(key, current)
  })

  return Array.from(grouped.values())
    .map(item => ({
      courseName: item.courseName,
      chapterName: item.chapterName,
      askCount: item.askCount,
      studentCount: item.studentSet.size,
      latestAskTime: item.latestAskTime
    }))
    .sort((a, b) => b.askCount - a.askCount || String(b.latestAskTime).localeCompare(String(a.latestAskTime)))
})

const chapterRows = computed(() =>
  chapterAnalysis.value.slice(0, 10).map(item => ({
    ...item,
    correctRateText: formatPercent(item.correctRate)
  }))
)

const weakPointCards = computed(() =>
  chapterRows.value.slice(0, 5).map((item, index) => ({
    key: `${item.courseName || 'course'}-${item.chapterName || 'chapter'}-${index}`,
    title: `${item.courseName || '未分类'} / ${item.chapterName || item.chapterCode || '未分类'}`,
    rateText: `正确率 ${item.correctRateText}`,
    desc: `共 ${safeNumber(item.studentCount)} 名学生、${safeNumber(item.answerCount)} 次作答集中暴露这一薄弱点，建议优先讲评与巩固。`,
    raw: item
  }))
)

const riskScore = computed(() => {
  let score = 0
  const avgRate = safeNumber(legacyOverview.value.correctRate)
  if (avgRate > 0 && avgRate < 65) score += 2
  if (chapterRows.value[0] && safeNumber(chapterRows.value[0].correctRate) < 55) score += 2
  if (chapterRows.value[1] && safeNumber(chapterRows.value[1].correctRate) < 60) score += 1
  if (safeNumber(aiOverview.value.interactionCount) >= 10) score += 1
  return score
})

const riskSummary = computed(() => {
  if (riskScore.value >= 5) return { label: '高关注', type: 'danger' }
  if (riskScore.value >= 3) return { label: '中关注', type: 'warning' }
  return { label: '状态稳定', type: 'success' }
})

const riskItems = computed(() => {
  const items = []
  const avgRate = safeNumber(legacyOverview.value.correctRate)
  const weakest = chapterRows.value[0]
  const secondWeakest = chapterRows.value[1]
  const hotspot = moduleDetailRows.value[0]

  if (!legacyBound.value && bindingMessage.value) {
    items.push({
      title: '当前专业尚未绑定可展示的真实数据',
      level: '待处理',
      type: 'warning',
      description: bindingMessage.value
    })
  }

  if (avgRate > 0) {
    items.push({
      title: '整体掌握情况评估',
      level: avgRate < 60 ? '高' : avgRate < 75 ? '中' : '低',
      type: avgRate < 60 ? 'danger' : avgRate < 75 ? 'warning' : 'success',
      description: `当前整体平均正确率为 ${formatPercent(avgRate)}，${avgRate < 60 ? '班级整体基础偏弱，需要优先补基础。' : avgRate < 75 ? '班级处于中等掌握区间，需要继续压薄弱点。' : '班级整体掌握较稳，可重点抓个别薄弱模块。'}`
    })
  }

  if (weakest) {
    items.push({
      title: '最薄弱模块需优先补强',
      level: safeNumber(weakest.correctRate) < 50 ? '高' : '中',
      type: safeNumber(weakest.correctRate) < 50 ? 'danger' : 'warning',
      description: `${weakest.courseName} 的 ${weakest.chapterName} 当前正确率为 ${weakest.correctRateText}，共 ${safeNumber(weakest.answerCount)} 次作答暴露出集中问题。`
    })
  }

  if (secondWeakest) {
    items.push({
      title: '第二薄弱模块需持续跟进',
      level: '中',
      type: 'info',
      description: `${secondWeakest.chapterName} 也处于偏弱状态，建议和第一薄弱模块一起复盘，避免只补一处后其他模块继续掉队。`
    })
  }

  if (hotspot) {
    items.push({
      title: 'AI 互动热点模块',
      level: safeNumber(hotspot.askCount) >= 3 ? '中' : '低',
      type: safeNumber(hotspot.askCount) >= 3 ? 'warning' : 'success',
      description: `${hotspot.courseName} 的 ${hotspot.chapterName} 被提问 ${safeNumber(hotspot.askCount)} 次，说明这一模块是当前学生最常追问的焦点。`
    })
  }

  if (!items.length) {
    items.push({
      title: '当前专业状态平稳',
      level: '低',
      type: 'success',
      description: '目前没有明显的高风险章节，可继续按课程节奏推进。'
    })
  }

  return items
})

const detailTitle = computed(() => {
  if (detailType.value === 'interactions') return '互动总数详情'
  if (detailType.value === 'students') return '互动学生详情'
  return '涉及模块详情'
})

const detailEmptyText = computed(() => {
  if (detailType.value === 'modules' && safeNumber(aiOverview.value.chapterCount) > 0) {
    return '模块总数已统计到，但当前服务暂未返回明细，页面已按互动记录自动回算。若仍为空，请先产生新的互动记录后再刷新。'
  }
  return '暂无数据'
})

function resolveBinding(responses = []) {
  const firstBound = responses.find(item => typeof item?.bound === 'boolean')
  legacyBound.value = Boolean(firstBound?.bound)
  bindingMessage.value = responses.map(item => item?.msg).find(Boolean) || ''
}

async function loadAnalysis() {
  loading.value = true
  try {
    const [overviewRes, chapterRes, assistantRes] = await Promise.all([
      getTeacherAnalysisOverview(),
      getTeacherAnalysisChapters(),
      getTeacherAiAssistantSummary({ recentLimit: 100, weakChapterLimit: 8 })
    ])

    resolveBinding([overviewRes, chapterRes, assistantRes])
    legacyOverview.value = unwrapMapResponse(overviewRes)
    chapterAnalysis.value = unwrapListResponse(chapterRes)
    aiSummary.value = unwrapMapResponse(assistantRes)
  } finally {
    loading.value = false
  }
}

function openSummaryDetail(type) {
  detailType.value = type
  detailVisible.value = true
}

function goToPaperWithRow(row) {
  router.push({
    path: '/education/teacher/paper',
    query: {
      source: 'teacher-analysis',
      courseName: row?.courseName || '',
      chapterCode: row?.chapterCode || '',
      chapterName: row?.chapterName || ''
    }
  })
}

function goToStudentsWithChapter(row) {
  router.push({
    path: '/education/teacher/students',
    query: {
      courseName: row?.courseName || '',
      chapterCode: row?.chapterCode || ''
    }
  })
}

onMounted(() => {
  loadAnalysis()
})
</script>

<style scoped lang="scss">
.analysis-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 284px 1fr;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.14), transparent 22%),
    linear-gradient(180deg, #fffaf4 0%, #eef6ff 100%);
}

.side-nav,
.hero-card,
.overview-card,
.summary-card,
.panel-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.side-nav {
  padding: 20px;
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

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #c2410c;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: 32px;
}

.hero-text {
  margin-top: 10px;
  max-width: 760px;
  color: #475569;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-grid,
.ai-summary-grid,
.compact-grid {
  display: grid;
  gap: 16px;
}

.overview-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.ai-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.compact-grid {
  grid-template-columns: 0.9fr 1.1fr;
}

.overview-card,
.summary-card {
  padding: 18px;
}

.overview-card span,
.summary-label {
  color: #64748b;
}

.overview-card strong,
.summary-value {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f172a;
}

.overview-card p,
.summary-tip,
.panel-note {
  margin: 10px 0 0;
  color: #94a3b8;
  line-height: 1.7;
}

.summary-link {
  margin-top: 12px;
  padding-left: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.risk-list,
.weak-list {
  display: grid;
  gap: 12px;
}

.risk-item,
.weak-card {
  padding: 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.9);
}

.risk-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.weak-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.weak-top strong,
.risk-item strong {
  display: block;
  color: #0f172a;
}

.weak-top span {
  color: #2563eb;
  white-space: nowrap;
}

.weak-card p,
.risk-item p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.weak-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .analysis-shell,
  .overview-grid,
  .ai-summary-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
