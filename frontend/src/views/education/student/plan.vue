<template>
  <div class="student-plan-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Student Plan</p>
        <h1>个性化学习规划</h1>
        <p class="hero-text">
          学习规划基于系统中的真实作答、薄弱知识点和编程成绩自动生成。
          点击“AI深度分析”后，会在现有规划基础上进一步细化执行顺序、重点任务和风险提醒。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="success" plain :loading="aiPlanLoading" @click="runAiPlanAnalysis">
          {{ aiPlanGenerated ? '重新AI深度分析' : 'AI深度分析' }}
        </el-button>
        <el-button plain @click="router.push('/education/student/pad')">返回学生主页</el-button>
        <el-button type="primary" :loading="loading" @click="loadPlan">刷新规划</el-button>
      </div>
    </section>

    <el-alert
      v-if="bindingMessage"
      :title="bindingMessage"
      :type="legacyBound ? 'success' : 'warning'"
      :closable="false"
      show-icon
      class="notice-card"
    />

    <el-row :gutter="16" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="24" :md="6">
        <el-card class="summary-card">
          <span class="summary-label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <p>{{ card.tip }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="layout-row">
      <el-col :xs="24" :lg="6" class="nav-col">
        <aside class="side-nav">
          <div class="nav-title">学生导航</div>
          <button class="nav-item" @click="router.push('/education/student/history')">
            <strong>历史做题</strong>
            <span>查看每次真实作答记录与得分情况</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/report')">
            <strong>学生诊断</strong>
            <span>查看真实作答支撑下的薄弱点分析</span>
          </button>
          <button class="nav-item active">
            <strong>学习规划</strong>
            <span>把诊断结果转成可执行的学习任务</span>
          </button>
          <button class="nav-item" @click="goToPracticeWithContext">
            <strong>智能刷题</strong>
            <span>基于真实题库与薄弱模块做推荐训练</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/prediction')">
            <strong>成绩预测</strong>
            <span>结合训练模型查看预测结果</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/assistant')">
            <strong>师生AI互动助手</strong>
            <span>进入班级互动与私聊问答</span>
          </button>
          <button class="nav-item" @click="goToRagWithPlanContext">
            <strong>RAG智能问答</strong>
            <span>带着学习规划上下文继续追问</span>
          </button>
        </aside>
      </el-col>

      <el-col :xs="24" :lg="18" class="main-col">
        <el-row :gutter="16" class="content-row">
          <el-col :xs="24" :lg="14">
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>两周学习安排</span>
                  <el-tag effect="plain" :type="stageTag.type">{{ stageTag.label }}</el-tag>
                </div>
              </template>

              <div v-if="aiPlanSummary" class="ai-summary">
                <strong>{{ aiPlanSummary.title }}</strong>
                <p>{{ aiPlanSummary.description }}</p>
                <span>{{ aiPlanSummary.meta }}</span>
              </div>

              <div v-if="deepAdviceCards.length" class="deep-advice-grid">
                <div v-for="item in deepAdviceCards" :key="item.title" class="deep-advice-card">
                  <span class="deep-advice-label">{{ item.title }}</span>
                  <p>{{ item.content }}</p>
                </div>
              </div>

              <div class="timeline-list">
                <div v-for="item in timeline" :key="item.index" class="timeline-item">
                  <div class="timeline-index">{{ item.index }}</div>
                  <div class="timeline-content">
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.description }}</p>
                    <span>{{ item.goal }}</span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :lg="10">
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>本周执行清单</span>
                  <el-tag effect="plain">{{ actionItems.length }} 项</el-tag>
                </div>
              </template>

              <div v-if="aiPlanModules.action" class="ai-inline-note ai-inline-note--compact">
                <span class="ai-inline-label">AI执行建议</span>
                <p>{{ aiPlanModules.action }}</p>
              </div>

              <div v-if="aiPlanModules.risk" class="ai-inline-note ai-inline-note--warning">
                <span class="ai-inline-label">AI风险提醒</span>
                <p>{{ aiPlanModules.risk }}</p>
              </div>

              <div class="action-list">
                <div v-for="item in actionItems" :key="item.title" class="action-item">
                  <div class="action-title-row">
                    <strong>{{ item.title }}</strong>
                    <el-tag :type="item.type" effect="light">{{ item.level }}</el-tag>
                  </div>
                  <p>{{ item.description }}</p>
                </div>
              </div>
            </el-card>

            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>当前重点对象</span>
                </div>
              </template>

              <div v-if="aiPlanModules.focus" class="ai-inline-note ai-inline-note--compact">
                <span class="ai-inline-label">AI重点判断</span>
                <p>{{ aiPlanModules.focus }}</p>
              </div>

              <div class="focus-list">
                <div class="focus-item">
                  <strong>优先模块</strong>
                  <p>{{ focusSummary.chapter }}</p>
                </div>
                <div class="focus-item">
                  <strong>重点知识点</strong>
                  <p>{{ focusSummary.knowledgePoint }}</p>
                </div>
                <div class="focus-item">
                  <strong>编程表现</strong>
                  <p>{{ focusSummary.program }}</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { queryQuestion } from '@/api/education/rag'
import {
  getStudentDiagnosisChapters,
  getStudentDiagnosisOverview,
  getStudentProgramAssignments,
  getStudentProgramOverview,
  getStudentWeakKnowledgePoints
} from '@/api/education/student'

const router = useRouter()
const loading = ref(false)
const aiPlanLoading = ref(false)
const aiPlanGenerated = ref(false)
const aiPlanSummary = ref(null)
const aiPlanModules = ref({
  timeline: '',
  action: '',
  focus: '',
  risk: ''
})

const diagnosisOverview = ref({})
const chapterDiagnosis = ref([])
const knowledgePoints = ref([])
const programOverview = ref({})
const programAssignments = ref([])
const legacyBound = ref(false)
const bindingMessage = ref('')

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function getCurrentTimeText() {
  const now = new Date()
  return `${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}`
}

function normalizeAiText(value) {
  return String(value || '')
    .replace(/```json/gi, '```')
    .replace(/```/g, '')
    .replace(/##+/g, '')
    .replace(/\r/g, '')
    .trim()
}

function normalizeParagraph(value) {
  return String(value || '')
    .replace(/\r/g, '')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function cleanPrefix(line) {
  return String(line || '')
    .replace(/^[\-\d\.\s、]+/, '')
    .replace(/^(总判断|两周优化|本周执行|当前重点|风险提醒|建议一|建议二|建议三)[:：]\s*/u, '')
    .trim()
}

const metrics = computed(() => ({
  answerCount: safeNumber(diagnosisOverview.value.answerCount),
  correctRate: safeNumber(diagnosisOverview.value.correctRate),
  recentAnswerCount: safeNumber(diagnosisOverview.value.recentAnswerCount),
  recentCorrectRate: safeNumber(diagnosisOverview.value.recentCorrectRate),
  programSubmitCount: safeNumber(programOverview.value.submitCount),
  programAverageScore: safeNumber(programOverview.value.averageScore)
}))

const weakestChapter = computed(() => chapterDiagnosis.value[0] || {})
const weakestKnowledgePoint = computed(() => knowledgePoints.value[0] || {})
const weakestProgramAssignment = computed(() => programAssignments.value[0] || {})

const stageTag = computed(() => {
  if (legacyBound.value && metrics.value.correctRate >= 80 && metrics.value.programAverageScore >= 75) {
    return { label: '巩固提升阶段', type: 'success' }
  }
  if (legacyBound.value && metrics.value.correctRate >= 65) {
    return { label: '稳步提升阶段', type: 'warning' }
  }
  return { label: '重点补强阶段', type: 'danger' }
})

const summaryCards = computed(() => [
  {
    label: '历史作答总数',
    value: `${metrics.value.answerCount}`,
    tip: '来自真实题库与历史作答记录'
  },
  {
    label: '整体正确率',
    value: `${metrics.value.correctRate || 0}%`,
    tip: '用于判断当前最需要补强的模块'
  },
  {
    label: '近 7 天做题量',
    value: `${metrics.value.recentAnswerCount}`,
    tip: '反映最近一周的学习节奏'
  },
  {
    label: '编程题均分',
    value: metrics.value.programAverageScore ? `${metrics.value.programAverageScore} 分` : '暂无',
    tip: '来自真实编程题提交与评分结果'
  }
])

const focusSummary = computed(() => ({
  chapter: weakestChapter.value.chapterName || '当前暂无明显薄弱模块',
  knowledgePoint: weakestKnowledgePoint.value.knowledgePoint || '当前暂无明显薄弱知识点',
  program: weakestProgramAssignment.value.assignmentTitle
    ? `${weakestProgramAssignment.value.assignmentTitle}（均分 ${safeNumber(weakestProgramAssignment.value.averageScore)} 分）`
    : '当前暂无明显低分编程任务'
}))

const timeline = computed(() => {
  const chapterName = weakestChapter.value.chapterName || '薄弱模块'
  const knowledgePoint = weakestKnowledgePoint.value.knowledgePoint || '重点知识点'
  const programTitle = weakestProgramAssignment.value.assignmentTitle || '近期编程任务'

  return [
    {
      index: '01',
      title: '第 1-3 天：明确补强范围',
      description: legacyBound.value
        ? `先围绕 ${chapterName} 和 ${knowledgePoint} 回看资料，整理近期错题和错误原因。`
        : '先根据当前真实作答记录梳理自己的薄弱点清单。',
      goal: '目标：形成一份可执行的薄弱点与错题清单。'
    },
    {
      index: '02',
      title: '第 4-7 天：完成针对性练习',
      description: legacyBound.value
        ? `围绕 ${chapterName} 完成一轮章节训练，并优先处理系统推荐题。`
        : '优先完成基础题和中档题，建立稳定的做题节奏。',
      goal: '目标：把“不会做”提升到“能独立完成”。'
    },
    {
      index: '03',
      title: '第 8-10 天：强化综合应用',
      description: legacyBound.value && weakestProgramAssignment.value.assignmentTitle
        ? `复盘 ${programTitle} 的代码思路、失分点和批注反馈，再补做相近题。`
        : '安排一轮综合练习，检验前一阶段补强效果。',
      goal: '目标：提升综合题和应用题的稳定性。'
    },
    {
      index: '04',
      title: '第 11-14 天：复测与再规划',
      description: legacyBound.value
        ? '对照最近正确率和编程题均分变化，判断是否进入下一轮提升。'
        : '对照近一周做题表现，重新安排下一轮学习重点。',
      goal: '目标：形成下一个周期的学习节奏和重点对象。'
    }
  ]
})

const actionItems = computed(() => {
  const items = []

  if (legacyBound.value && weakestChapter.value.chapterName) {
    items.push({
      title: `专项补强：${weakestChapter.value.chapterName}`,
      level: '核心任务',
      type: 'warning',
      description: `该模块正确率约 ${safeNumber(weakestChapter.value.correctRate)}%，建议优先完成模块复盘和专项训练。`
    })
  }

  if (legacyBound.value && weakestKnowledgePoint.value.knowledgePoint) {
    items.push({
      title: `重点知识点：${weakestKnowledgePoint.value.knowledgePoint}`,
      level: '建议执行',
      type: 'primary',
      description: `该知识点正确率约 ${safeNumber(weakestKnowledgePoint.value.correctRate)}%，适合结合 RAG 做针对性讲解。`
    })
  }

  if (metrics.value.programSubmitCount > 0) {
    items.push({
      title: '复盘编程题表现',
      level: '专项巩固',
      type: 'success',
      description: `当前共有 ${metrics.value.programSubmitCount} 次编程题提交，平均分约 ${metrics.value.programAverageScore || 0} 分。`
    })
  }

  if (metrics.value.recentAnswerCount < 10) {
    items.push({
      title: '补足近期训练量',
      level: '学习节奏',
      type: 'danger',
      description: '近 7 天做题量偏少，建议先恢复稳定训练节奏，再追求难题突破。'
    })
  }

  if (!items.length) {
    items.push({
      title: '利用 AI 做错题复盘',
      level: '长期习惯',
      type: 'success',
      description: '把当前最容易出错的模块或知识点带入 RAG，获取更具体的解释与训练建议。'
    })
  }

  return items
})

const deepAdviceCards = computed(() => {
  const cards = [
    { title: 'AI两周优化建议', content: aiPlanModules.value.timeline },
    { title: 'AI本周执行顺序', content: aiPlanModules.value.action },
    { title: 'AI当前重点判断', content: aiPlanModules.value.focus },
    { title: 'AI风险提醒', content: aiPlanModules.value.risk }
  ]
  return cards.filter((item) => item.content)
})

function buildPlanAiQuestion() {
  const courseName = weakestChapter.value.courseName || '当前课程'
  const chapterName = weakestChapter.value.chapterName || '当前模块'
  const knowledgePoint = weakestKnowledgePoint.value.knowledgePoint || '当前知识点'

  return [
    '请基于当前学生学习数据，进一步细化现有学习规划。',
    `课程：${courseName}`,
    `模块：${chapterName}`,
    `知识点：${knowledgePoint}`,
    `总作答：${metrics.value.answerCount}`,
    `正确率：${metrics.value.correctRate}%`,
    `近7天做题量：${metrics.value.recentAnswerCount}`,
    `编程均分：${metrics.value.programAverageScore || 0}`,
    `当前重点：${focusSummary.value.chapter}；${focusSummary.value.knowledgePoint}；${focusSummary.value.program}`,
    '请直接输出5段中文内容，按“总判断 / 两周优化 / 本周执行 / 当前重点 / 风险提醒”展开，每段都要具体，不要空话。'
  ].join('\n')
}

function splitAnswerSections(answerText) {
  const normalized = normalizeAiText(answerText)
  if (!normalized) return []
  return normalized
    .split(/\n+/)
    .map((line) => cleanPrefix(line))
    .filter(Boolean)
}

function buildPlanFallbackResult() {
  const chapter = focusSummary.value.chapter
  const point = focusSummary.value.knowledgePoint
  const program = focusSummary.value.program
  const lowPractice = metrics.value.recentAnswerCount < 10

  return {
    summary: {
      title: 'AI深度学习规划结论',
      description: `当前这份规划最大的问题不是方向不对，而是执行颗粒度不够。接下来最重要的是把 ${chapter} 的补强顺序、${point} 的训练方式，以及 ${program} 的复盘动作拆得更细，避免计划停留在“知道要学什么”，却没有真正落实到每天。`,
      meta: `生成时间 ${getCurrentTimeText()} · 基于当前真实学习数据`
    },
    modules: {
      timeline: `第 1-3 天先只做 ${chapter} 相关基础复盘与错题回看，第 4-7 天集中刷这一模块的专项题，第 8-10 天加入 ${point} 的综合变式训练，第 11-14 天安排一次复测并根据结果决定是否切换到下一模块。这样比平均铺开所有内容更容易看到提升。`,
      action: `本周建议按这个顺序执行：第一，先完成 ${chapter} 的模块复盘并整理错因；第二，围绕 ${point} 连续做 2 到 3 组针对题，把错误类型记清楚；第三，针对 ${program} 做一次专项复盘，确认哪些题是思路问题，哪些题是粗心失分。`,
      focus: `${chapter} 仍然是当前最该优先盯住的对象，因为它直接决定后续训练的稳定性；如果这个模块不先补稳，就算继续刷题，提升也容易发散。同时 ${point} 应作为短期突破口，适合用小范围高频训练来拉升正确率。`,
      risk: lowPractice
        ? '当前最大的风险不是能力不够，而是最近训练量偏少。若继续只看规划不增加实际练习量，AI 给出的再细建议也很难转化成真正提升。'
        : '当前最大的风险是规划看起来完整，但每天练什么、练多少、如何验收仍不够明确。如果继续按旧方式执行，容易出现复习很多、真正得分提升有限的问题。'
    }
  }
}

function parsePlanAiResult(answerText) {
  const fallback = buildPlanFallbackResult()
  const sections = splitAnswerSections(answerText)

  return {
    summary: {
      title: 'AI深度学习规划结论',
      description: sections[0] || fallback.summary.description,
      meta: `生成时间 ${getCurrentTimeText()} · 来自 AI 深度分析`
    },
    modules: {
      timeline: sections[1] || fallback.modules.timeline,
      action: sections[2] || fallback.modules.action,
      focus: sections[3] || fallback.modules.focus,
      risk: sections[4] || fallback.modules.risk
    }
  }
}

async function runAiPlanAnalysis() {
  aiPlanLoading.value = true
  try {
    const response = await queryQuestion(buildPlanAiQuestion(), {
      sourceScene: 'student-plan-analysis',
      courseName: String(weakestChapter.value.courseName || '').trim(),
      chapterCode: String(weakestChapter.value.chapterCode || '').trim(),
      chapterName: String(weakestChapter.value.chapterName || '').trim(),
      knowledgePoint: String(weakestKnowledgePoint.value.knowledgePoint || '').trim()
    })

    const answerText = String(response?.answer || response?.data || response?.msg || '').trim()
    const result = answerText ? parsePlanAiResult(answerText) : buildPlanFallbackResult()
    aiPlanSummary.value = result.summary
    aiPlanModules.value = result.modules
    aiPlanGenerated.value = true
    ElMessage.success(answerText ? '已完成 AI 深度分析' : '已按当前数据生成强化规划')
  } catch (error) {
    const fallback = buildPlanFallbackResult()
    aiPlanSummary.value = fallback.summary
    aiPlanModules.value = fallback.modules
    aiPlanGenerated.value = true
    ElMessage.warning('AI 服务暂不可用，已按当前真实数据生成强化规划')
  } finally {
    aiPlanLoading.value = false
  }
}

async function loadPlan() {
  loading.value = true
  try {
    const [overviewRes, chapterRes, knowledgeRes, programOverviewRes, programAssignmentsRes] = await Promise.all([
      getStudentDiagnosisOverview(),
      getStudentDiagnosisChapters(),
      getStudentWeakKnowledgePoints({ limit: 5 }),
      getStudentProgramOverview(),
      getStudentProgramAssignments({ limit: 5 })
    ])

    diagnosisOverview.value = overviewRes?.data || {}
    chapterDiagnosis.value = chapterRes?.data || []
    knowledgePoints.value = knowledgeRes?.data || []
    programOverview.value = programOverviewRes?.data || {}
    programAssignments.value = programAssignmentsRes?.data || []
    legacyBound.value = overviewRes?.bound !== false
    bindingMessage.value = overviewRes?.message || ''
    aiPlanGenerated.value = false
    aiPlanSummary.value = null
    aiPlanModules.value = {
      timeline: '',
      action: '',
      focus: '',
      risk: ''
    }
  } finally {
    loading.value = false
  }
}

function goToPracticeWithContext() {
  router.push({
    path: '/education/student/practice',
    query: {
      source: 'student-plan',
      courseName: weakestChapter.value.courseName || '',
      chapterCode: weakestChapter.value.chapterCode || '',
      chapterName: weakestChapter.value.chapterName || ''
    }
  })
}

function goToRagWithPlanContext() {
  const question = legacyBound.value
    ? `请结合我当前在${focusSummary.value.chapter}和${focusSummary.value.knowledgePoint}上的表现，给我一份更细的两周学习安排，并说明每天优先做什么。`
    : '请结合我当前的真实作答情况，给我一份基础学习规划建议。'

  router.push({
    path: '/education/rag',
    query: {
      question,
      source: 'student-plan',
      courseName: weakestChapter.value.courseName || '',
      chapterCode: weakestChapter.value.chapterCode || '',
      chapterName: weakestChapter.value.chapterName || ''
    }
  })
}

onMounted(loadPlan)
</script>

<style scoped lang="scss">
.student-plan-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.12), transparent 26%),
    linear-gradient(180deg, #f8fcff 0%, #eef7ff 100%);
}

.hero-card,
.side-nav,
.section-card,
.summary-card {
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 28px;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 32px;
  color: #0f172a;
}

.hero-text {
  margin-top: 12px;
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.side-nav {
  padding: 20px;
  height: fit-content;
}

.nav-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-weight: 800;
  font-size: 22px;
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
  border-color: rgba(37, 99, 235, 0.45);
  background: rgba(239, 246, 255, 0.96);
}

.notice-card,
.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  padding: 20px;
}

.summary-label,
.summary-card p {
  display: block;
  color: #64748b;
}

.summary-card strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.card-header,
.action-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-summary {
  margin-bottom: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(34, 197, 94, 0.08));
  border: 1px solid rgba(14, 165, 233, 0.14);
}

.ai-summary strong {
  display: block;
  font-size: 17px;
  color: #0f172a;
}

.ai-summary p {
  margin: 10px 0 8px;
  color: #334155;
  line-height: 1.75;
  font-size: 15px;
}

.ai-summary span {
  color: #64748b;
  font-size: 13px;
}

.deep-advice-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr;
  margin-bottom: 14px;
}

.deep-advice-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.deep-advice-label {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0369a1;
  background: rgba(224, 242, 254, 0.92);
}

.deep-advice-card p {
  margin: 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.ai-inline-note {
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(239, 246, 255, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.16);
}

.ai-inline-note--compact {
  padding: 12px 14px;
}

.ai-inline-note--warning {
  background: rgba(255, 247, 237, 0.92);
  border-color: rgba(249, 115, 22, 0.16);
}

.ai-inline-label {
  display: inline-flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0369a1;
  background: rgba(224, 242, 254, 0.92);
}

.ai-inline-note--warning .ai-inline-label {
  color: #c2410c;
  background: rgba(255, 237, 213, 0.92);
}

.ai-inline-note p {
  margin: 0;
  color: #334155;
  line-height: 1.75;
  font-size: 14px;
  white-space: pre-wrap;
}

.timeline-list,
.action-list,
.focus-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-item,
.action-item,
.focus-item {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.timeline-index {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #0284c7, #22c55e);
  font-weight: 700;
}

.timeline-content h3,
.focus-item strong {
  margin: 0;
  color: #0f172a;
}

.timeline-content p,
.timeline-content span,
.action-item p,
.focus-item p {
  color: #64748b;
  line-height: 1.8;
}

.layout-row {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.content-row {
  align-items: stretch;
}

.nav-col,
.main-col {
  display: flex;
  flex-direction: column;
  width: auto !important;
  max-width: none !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  min-width: 0;
}

.layout-row > .el-col {
  max-width: none !important;
  flex: initial !important;
}

@media (max-width: 992px) {
  .hero-card {
    flex-direction: column;
  }
}
</style>
