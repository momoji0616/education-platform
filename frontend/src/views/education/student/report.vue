<template>
  <div class="report-shell student-shell">
    <header class="shell-header">
      <div>
        <p class="eyebrow">Student Diagnosis</p>
        <h1>学生诊断报告</h1>
        <p class="subtitle">
          基于学生历史作答、章节正确率、错题记录、知识点掌握情况和编程作业表现生成诊断结果。
        </p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/education/student/pad')">返回学生首页</el-button>
        <el-button type="primary" :loading="loading" @click="loadReport">刷新诊断</el-button>
      </div>
    </header>

    <el-alert
      v-if="bindingMessage"
      :title="bindingMessage"
      :type="legacyBound ? 'success' : 'warning'"
      :closable="false"
      show-icon
      class="notice-card"
    />

    <div class="shell-body">
      <aside class="side-nav">
        <div class="nav-title">学生导航</div>
        <button class="nav-item" @click="router.push('/education/student/history')">
          <strong>历史做题</strong>
          <span>查看每次真实作答记录与得分情况</span>
        </button>
        <button class="nav-item active">
          <strong>学生诊断</strong>
          <span>查看真实作答支撑下的学情诊断结果</span>
        </button>
        <button class="nav-item" @click="goToPlanWithContext">
          <strong>学习规划</strong>
          <span>把诊断结果转成可执行的学习任务</span>
        </button>
        <button class="nav-item" @click="goToPracticeWithDiagnosis">
          <strong>智能刷题</strong>
          <span>围绕薄弱章节开展针对性训练</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/prediction')">
          <strong>成绩预测</strong>
          <span>查看成绩预测与趋势分析</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/assistant')">
          <strong>师生 AI 助手</strong>
          <span>进入群聊、私聊和问答互动</span>
        </button>
        <button class="nav-item" @click="goToRagWithDiagnosis">
          <strong>RAG 智能问答</strong>
          <span>带着当前诊断结果继续追问</span>
        </button>
      </aside>

      <main class="content-area">
        <section class="summary-grid">
          <el-card v-for="card in overviewCards" :key="card.label" class="summary-card" shadow="never">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <p>{{ card.tip }}</p>
          </el-card>
        </section>

        <section class="content-grid">
          <div class="left-column">
            <el-card class="panel-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>风险诊断</span>
                  <el-tag :type="riskTag.type" effect="dark">{{ riskTag.label }}</el-tag>
                </div>
              </template>
              <div class="risk-list">
                <div v-for="item in riskItems" :key="item.title" class="info-item row-item">
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
                <div class="card-header">
                  <span>薄弱章节表现</span>
                </div>
              </template>
              <el-table :data="chapterRows" size="small">
                <el-table-column prop="courseName" label="课程" min-width="120" />
                <el-table-column prop="chapterName" label="章节" min-width="180" />
                <el-table-column prop="answerCount" label="作答数" width="90" />
                <el-table-column prop="correctRateText" label="正确率" width="90" />
                <el-table-column prop="summary" label="诊断结论" min-width="220" show-overflow-tooltip />
              </el-table>
            </el-card>
          </div>

          <div class="right-column">
            <el-card class="panel-card" shadow="never">
              <template #header>
                <div class="card-header card-header--stack">
                  <div class="header-main">
                    <span>提升建议</span>
                    <p>点击按钮后会结合当前页面的真实学情数据发起 AI 分析。</p>
                  </div>
                  <div class="header-tools">
                    <el-tag v-if="aiAnalysisGenerated" type="success" effect="light">已调用 AI</el-tag>
                    <el-button type="primary" plain :loading="aiAnalysisLoading" @click="runAiAdviceAnalysis">
                      {{ aiAnalysisGenerated ? '重新 AI 分析' : 'AI 分析建议' }}
                    </el-button>
                  </div>
                </div>
              </template>

              <div v-if="aiAnalysisSummary" class="ai-summary">
                <strong>{{ aiAnalysisSummary.title }}</strong>
                <p>{{ aiAnalysisSummary.description }}</p>
                <span>{{ aiAnalysisSummary.meta }}</span>
              </div>

              <el-empty
                v-if="!aiAnalysisSummary"
                description="点击“AI 分析建议”后生成本次学习建议"
                :image-size="72"
              />
            </el-card>

            <el-card v-if="legacyBound" class="panel-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>编程题表现</span>
                </div>
              </template>
              <div class="metric-grid">
                <div class="metric-box">
                  <span>提交次数</span>
                  <strong>{{ stats.programSubmitCount }}</strong>
                </div>
                <div class="metric-box">
                  <span>涉及作业</span>
                  <strong>{{ safeNumber(programOverview.assignmentCount) }}</strong>
                </div>
                <div class="metric-box">
                  <span>编程均分</span>
                  <strong>{{ stats.programAverageScore ? `${stats.programAverageScore} 分` : '暂无' }}</strong>
                </div>
              </div>
              <div v-if="programAssignments.length" class="list-stack">
                <div v-for="item in programAssignments" :key="item.assignmentSourceId" class="compact-item">
                  <strong>{{ item.assignmentTitle || '未命名编程作业' }}</strong>
                  <p>平均分 {{ safeNumber(item.averageScore) }} 分，提交 {{ safeNumber(item.submitCount) }} 次。</p>
                </div>
              </div>
              <el-empty v-else description="当前暂无编程题作答记录" :image-size="72" />
            </el-card>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { queryQuestion } from '@/api/education/rag'
import {
  getStudentDiagnosisChapters,
  getStudentDiagnosisOverview,
  getStudentProgramAssignments,
  getStudentProgramOverview,
  getStudentWeakKnowledgePoints,
  getStudentWrongQuestions,
  listStudentExamScore,
  listStudentSelfScores
} from '@/api/education/student'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const aiAnalysisLoading = ref(false)
const aiAnalysisGenerated = ref(false)
const aiAnalysisSummary = ref(null)
const examScoreList = ref([])
const selfScoreList = ref([])
const diagnosisOverview = ref({})
const chapterDiagnosis = ref([])
const wrongQuestions = ref([])
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
  const hours = `${now.getHours()}`.padStart(2, '0')
  const minutes = `${now.getMinutes()}`.padStart(2, '0')
  return `${hours}:${minutes}`
}

function normalizeAiText(value) {
  return String(value || '')
    .replace(/```json/gi, '```')
    .replace(/```/g, '')
    .trim()
}

function extractJsonObject(text) {
  const cleaned = normalizeAiText(text)
  const firstBrace = cleaned.indexOf('{')
  const lastBrace = cleaned.lastIndexOf('}')
  if (firstBrace === -1 || lastBrace === -1 || lastBrace <= firstBrace) return null
  const candidate = cleaned.slice(firstBrace, lastBrace + 1)
  try {
    return JSON.parse(candidate)
  } catch (error) {
    return null
  }
}

function buildFallbackAiResult(answerText) {
  const weakestChapter = chapterDiagnosis.value[0]
  const chapterName = weakestChapter?.chapterName || ''
  const focusTitle = chapterName ? `AI 建议优先关注 ${chapterName}` : 'AI 已返回分析结果'
  return {
    summary: {
      title: focusTitle,
      description: answerText || 'AI 已返回分析结果，请结合当前诊断数据查看。',
      meta: `生成时间 ${getCurrentTimeText()} · 来自真实 AI 分析`
    },
    items: [
      {
        title: chapterName ? `当前重点章节：${chapterName}` : 'AI 分析结果',
        description: answerText || '暂无可展示的详细结果。'
      }
    ]
  }
}

function parseAiAdviceResult(answerText) {
  const parsed = extractJsonObject(answerText)
  if (!parsed) return buildFallbackAiResult(answerText)

  const cards = Array.isArray(parsed.cards) ? parsed.cards : []
  return {
    summary: {
      title: parsed.summaryTitle || 'AI 分析完成',
      description: parsed.summaryDescription || 'AI 已返回本次学习建议。',
      meta: `生成时间 ${getCurrentTimeText()} · 来自真实 AI 分析`
    },
    items: cards
      .filter((item) => item && (item.title || item.description))
      .map((item) => ({
        title: String(item.title || 'AI 建议'),
        description: String(item.description || '')
      }))
  }
}

const stats = computed(() => {
  const examScores = examScoreList.value.map((item) => safeNumber(item.score)).filter((item) => item > 0)
  const examAvg = examScores.length ? Math.round(examScores.reduce((sum, item) => sum + item, 0) / examScores.length) : 0
  return {
    examAvg,
    answerCount: safeNumber(diagnosisOverview.value.answerCount),
    correctRate: safeNumber(diagnosisOverview.value.correctRate),
    programSubmitCount: safeNumber(programOverview.value.submitCount),
    programAverageScore: safeNumber(programOverview.value.averageScore)
  }
})

const riskTag = computed(() => {
  let score = 0
  if (legacyBound.value && stats.value.correctRate > 0 && stats.value.correctRate < 65) score += 2
  if (stats.value.examAvg > 0 && stats.value.examAvg < 75) score += 2
  if (chapterDiagnosis.value[0] && safeNumber(chapterDiagnosis.value[0].correctRate) < 60) score += 1
  if (score >= 4) return { label: '高风险', type: 'danger' }
  if (score >= 2) return { label: '中风险', type: 'warning' }
  return { label: '低风险', type: 'success' }
})

const overviewCards = computed(() => [
  {
    label: '历史正确率',
    value: `${stats.value.correctRate || 0}%`,
    tip: legacyBound.value ? '来自真实历史做题数据' : '当前暂无可用历史做题数据'
  },
  {
    label: '历史作答数',
    value: `${stats.value.answerCount}`,
    tip: legacyBound.value ? '已接入的真实做题记录' : '未绑定历史数据时记为 0'
  },
  {
    label: '编程均分',
    value: stats.value.programAverageScore ? `${stats.value.programAverageScore} 分` : '暂无',
    tip: legacyBound.value ? '来自真实编程作业记录' : '当前暂无编程分析数据'
  }
])

const riskItems = computed(() => {
  const items = []
  if (!legacyBound.value && bindingMessage.value) {
    items.push({
      title: '当前专业暂未绑定历史做题数据',
      level: '等待补充',
      type: 'warning',
      description: bindingMessage.value
    })
  }
  if (chapterDiagnosis.value[0]) {
    const weakestChapter = chapterDiagnosis.value[0]
    items.push({
      title: `当前最薄弱章节：${weakestChapter.chapterName || '待识别'}`,
      level: '优先补强',
      type: 'danger',
      description: `该章节当前正确率约 ${safeNumber(weakestChapter.correctRate)}%，建议优先安排刷题和 RAG 解释。`
    })
  }
  if (wrongQuestions.value.length) {
    items.push({
      title: '存在高频错题',
      level: '需要复盘',
      type: 'warning',
      description: `当前已识别 ${wrongQuestions.value.length} 道高频错题，适合直接做针对性重练。`
    })
  }
  if (!items.length) {
    items.push({
      title: '当前学习状态稳定',
      level: '继续保持',
      type: 'success',
      description: '当前没有明显高风险项，建议继续通过阶段练习和资料问答保持学习节奏。'
    })
  }
  return items
})

const chapterRows = computed(() =>
  chapterDiagnosis.value.slice(0, 8).map((item) => {
    const rate = safeNumber(item.correctRate)
    return {
      ...item,
      correctRateText: `${rate}%`,
      summary: rate < 60 ? '掌握较弱，建议优先补强。' : rate < 75 ? '需要继续巩固。' : '整体表现较稳。'
    }
  })
)

function buildAiQuestion() {
  const weakestChapter = chapterDiagnosis.value[0]
  const topKnowledgePoint = knowledgePoints.value[0]
  const topProgramAssignment = programAssignments.value[0]
  return [
    '你是学生学情诊断助手，请根据下面这份真实学情数据给出提升建议。',
    '要求：',
    '1. 不要说空话，不要只写“加强薄弱章节”这类泛话，必须点出具体章节名或知识点名；如果没有具体章节，就明确说明暂无可定位章节。',
    '2. 如果编程作业或相关得分已经接近满分或等于 100 分，不要再建议复盘，要改成保持节奏或转向其他短板。',
    '3. 输出必须是 JSON，不要输出任何额外解释。',
    '4. JSON 格式固定为：{"summaryTitle":"","summaryDescription":"","cards":[{"title":"","description":""},{"title":"","description":""},{"title":"","description":""}]}',
    '5. cards 最多 3 条，每条都要具体、可执行。',
    `历史正确率：${safeNumber(stats.value.correctRate)}%`,
    `历史作答数：${safeNumber(stats.value.answerCount)}`,
    `考试均分：${safeNumber(stats.value.examAvg) || '暂无'}`,
    `高频错题数：${wrongQuestions.value.length}`,
    `最薄弱章节：${weakestChapter?.chapterName || '暂无可定位章节'}`,
    `薄弱章节课程：${weakestChapter?.courseName || '暂无'}`,
    `薄弱章节正确率：${weakestChapter ? `${safeNumber(weakestChapter.correctRate)}%` : '暂无'}`,
    `重点知识点：${topKnowledgePoint?.knowledgePoint || '暂无可定位知识点'}`,
    `知识点正确率：${topKnowledgePoint ? `${safeNumber(topKnowledgePoint.correctRate)}%` : '暂无'}`,
    `最近编程作业：${topProgramAssignment?.assignmentTitle || '暂无'}`,
    `最近编程作业均分：${topProgramAssignment ? `${safeNumber(topProgramAssignment.averageScore)}分` : '暂无'}`,
    `最近编程作业提交次数：${topProgramAssignment ? safeNumber(topProgramAssignment.submitCount) : '暂无'}`,
    `编程总体均分：${safeNumber(stats.value.programAverageScore) || '暂无'}`
  ].join('\n')
}

async function runAiAdviceAnalysis() {
  aiAnalysisLoading.value = true
  try {
    const weakestChapter = chapterDiagnosis.value[0] || {}
    const response = await queryQuestion(buildAiQuestion(), {
      sourceScene: 'student-report-analysis',
      courseName: String(weakestChapter.courseName || '').trim(),
      chapterCode: String(weakestChapter.chapterCode || '').trim(),
      chapterName: String(weakestChapter.chapterName || '').trim(),
      knowledgePoint: String(knowledgePoints.value[0]?.knowledgePoint || '').trim()
    })

    const answerText = String(response?.answer || response?.data || response?.msg || '').trim()
    if (!answerText) {
      throw new Error('AI 未返回有效内容')
    }

    const result = parseAiAdviceResult(answerText)
    aiAnalysisSummary.value = result.summary
    aiAnalysisGenerated.value = true
    ElMessage.success('已完成 AI 分析')
  } catch (error) {
    ElMessage.error(error?.message || 'AI 分析失败')
  } finally {
    aiAnalysisLoading.value = false
  }
}

async function loadReport() {
  loading.value = true
  try {
    const [examScoreRes, selfScoresRes, overviewRes, chapterRes, wrongRes, knowledgeRes, programOverviewRes, programAssignmentsRes] = await Promise.all([
      listStudentExamScore(),
      listStudentSelfScores(),
      getStudentDiagnosisOverview(),
      getStudentDiagnosisChapters(),
      getStudentWrongQuestions({ limit: 5 }),
      getStudentWeakKnowledgePoints({ limit: 5 }),
      getStudentProgramOverview(),
      getStudentProgramAssignments({ limit: 3 })
    ])

    examScoreList.value = examScoreRes?.data || []
    selfScoreList.value = selfScoresRes?.data || []
    diagnosisOverview.value = overviewRes?.data || {}
    chapterDiagnosis.value = chapterRes?.data || []
    wrongQuestions.value = wrongRes?.data || []
    knowledgePoints.value = knowledgeRes?.data || []
    programOverview.value = programOverviewRes?.data || {}
    programAssignments.value = programAssignmentsRes?.data || []
    legacyBound.value = !!overviewRes?.bound
    bindingMessage.value = overviewRes?.message || (legacyBound.value ? '当前专业已接入真实历史做题与编程分析数据。' : '')
    aiAnalysisGenerated.value = false
    aiAnalysisSummary.value = null
  } finally {
    loading.value = false
  }
}

function buildDiagnosisContext() {
  const weakest = chapterDiagnosis.value[0] || {}
  return {
    courseName: weakest.courseName || '',
    chapterCode: weakest.chapterCode || '',
    chapterName: weakest.chapterName || ''
  }
}

function goToPlanWithContext() {
  router.push({ path: '/education/student/plan', query: { ...buildDiagnosisContext(), source: 'student-report' } })
}

function goToPracticeWithDiagnosis() {
  router.push({ path: '/education/student/practice', query: { ...buildDiagnosisContext(), source: 'student-report' } })
}

function goToRagWithDiagnosis() {
  const context = buildDiagnosisContext()
  const question = context.chapterName
    ? `我在${context.chapterName}这一章正确率偏低，请结合我的诊断结果给我一个补强建议。`
    : '请结合我的学习诊断结果给我一个补强建议。'
  router.push({ path: '/education/rag', query: { ...context, source: 'student-report', question } })
}

onMounted(() => {
  loadReport()
  if (route.query && route.query.refresh === '1') loadReport()
})
</script>

<style scoped lang="scss">
.report-shell { min-height: 100vh; padding: 24px; }

.student-shell {
  background: radial-gradient(circle at top left, rgba(14, 165, 233, .14), transparent 26%), linear-gradient(180deg, #f6fbff 0%, #fffdf5 100%);
}

.shell-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #0284c7;
  font-size: 12px;
  letter-spacing: .16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 32px;
  color: #0f172a;
}

.subtitle {
  margin-top: 10px;
  max-width: 760px;
  color: #475569;
  line-height: 1.7;
  font-size: 15px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.notice-card { margin-bottom: 16px; }

.shell-body {
  display: grid;
  grid-template-columns: 284px 1fr;
  gap: 18px;
}

.side-nav,
.summary-card,
.panel-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, .08);
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, .08);
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
  border: 1px solid rgba(148, 163, 184, .18);
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  margin-bottom: 12px;
  cursor: pointer;
}

.nav-item.active {
  border-color: rgba(14, 165, 233, .5);
  background: rgba(240, 249, 255, .95);
}

.nav-item strong,
.info-item strong,
.metric-box strong,
.compact-item strong {
  display: block;
  color: #0f172a;
  font-size: 21px;
}

.nav-item span,
.summary-card p,
.info-item p,
.compact-item p {
  color: #64748b;
  line-height: 1.7;
  font-size: 15px;
}

.nav-item span {
  font-size: 17px;
}

.content-area,
.left-column,
.right-column,
.risk-list,
.advice-list,
.list-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card { padding: 20px; }

.summary-card span {
  display: block;
  color: #0284c7;
  margin-bottom: 10px;
  font-size: 15px;
}

.summary-card strong {
  display: block;
  font-size: 28px;
  color: #0f172a;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.15fr .95fr;
  gap: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-header--stack {
  align-items: flex-start;
}

.header-main span {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.header-main p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ai-summary {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(14, 165, 233, .08), rgba(34, 197, 94, .08));
  border: 1px solid rgba(14, 165, 233, .14);
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

.info-item,
.compact-item {
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, .92);
  border: 1px solid rgba(148, 163, 184, .16);
}

.row-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.column-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.metric-box {
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, .92);
}

.metric-box span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 15px;
}

:deep(.el-table th),
:deep(.el-table td) {
  font-size: 15px;
}

@media (max-width: 992px) {
  .shell-header { flex-direction: column; }
  .shell-body,
  .summary-grid,
  .content-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }
  .row-item,
  .item-head,
  .card-header,
  .card-header--stack {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-tools {
    justify-content: flex-start;
  }
}
</style>
