<template>
  <div class="student-page-shell">
    <header class="page-header">
      <div>
        <p class="eyebrow">Student History</p>
        <h1>历史做题情况</h1>
        <p class="subtitle">
          这里展示的是学生真实做过的题目记录，包括课程、模块章节、作答内容、正确与否、分数和解析。
          这是目前最能体现真实学习轨迹的数据页之一。
        </p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/education/student/pad')">返回学生主页</el-button>
        <el-button type="primary" :loading="loading" @click="loadHistory">刷新记录</el-button>
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

    <div class="page-body">
      <aside class="side-nav">
        <div class="nav-title">学生导航</div>
        <button class="nav-item active">
          <strong>历史做题</strong>
          <span>查看每一次真实作答记录</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/report')">
          <strong>学生诊断</strong>
          <span>查看真实作答支撑下的薄弱点诊断</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/plan')">
          <strong>学习规划</strong>
          <span>把诊断结果转成可执行的学习任务</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/practice')">
          <strong>智能刷题</strong>
          <span>根据历史表现继续训练</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/prediction')">
          <strong>成绩预测</strong>
          <span>上传变量数据训练模型并预测成绩</span>
        </button>
        <button class="nav-item" @click="router.push('/education/student/assistant')">
          <strong>师生AI助手</strong>
          <span>进入班级聊天与互动问答</span>
        </button>
        <button class="nav-item" @click="goToRag">
          <strong>RAG智能回答</strong>
          <span>拿着题目去问做题思路</span>
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
                <div class="panel-header">
                  <span>筛选条件</span>
                </div>
              </template>
              <el-form :model="queryForm" label-width="88px">
                <el-form-item label="课程">
                  <el-select v-model="queryForm.courseName" clearable filterable style="width: 100%" @change="handleCourseChange">
                    <el-option v-for="item in courseOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
                <el-form-item label="模块">
                  <el-select v-model="queryForm.chapterCode" clearable filterable style="width: 100%" @change="loadHistory">
                    <el-option
                      v-for="item in chapterOptions"
                      :key="item.chapterCode || item.chapterName"
                      :label="item.chapterName"
                      :value="item.chapterCode"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="题型">
                  <el-select v-model="queryForm.questionType" clearable style="width: 100%" @change="loadHistory">
                    <el-option label="全部" value="" />
                    <el-option label="选择题" value="choice" />
                    <el-option label="编程题" value="program" />
                  </el-select>
                </el-form-item>
                <el-form-item label="记录数">
                  <el-input-number v-model="queryForm.limit" :min="10" :max="100" @change="loadHistory" />
                </el-form-item>
                <el-form-item label="每页显示">
                  <el-select v-model="pageSize" style="width: 100%">
                    <el-option label="5 条" :value="5" />
                    <el-option label="8 条" :value="8" />
                    <el-option label="10 条" :value="10" />
                    <el-option label="15 条" :value="15" />
                  </el-select>
                </el-form-item>
              </el-form>
            </el-card>

          </div>

          <div class="right-column">
            <el-card class="panel-card" shadow="never">
              <template #header>
                <div class="panel-header">
                  <span>真实作答记录</span>
                  <el-tag effect="plain">{{ historyList.length }} 条</el-tag>
                </div>
              </template>

              <div v-if="historyList.length" class="history-list">
                <el-collapse v-model="activeHistoryIds" class="history-collapse">
                  <el-collapse-item
                    v-for="item in pagedHistoryList"
                    :key="item.id"
                    :name="String(item.id)"
                    class="history-collapse-item"
                  >
                    <template #title>
                      <div class="history-collapse-title">
                        <div class="history-title-main">
                          <strong>{{ item.courseName || '未命名课程' }} / {{ item.chapterName || '综合模块' }}</strong>
                          <p>{{ item.questionTypeText }} · {{ item.submitTimeText }}</p>
                        </div>
                        <div class="head-tags">
                          <el-tag :type="item.correctType" effect="light">{{ item.correctText }}</el-tag>
                          <el-tag effect="plain">{{ item.scoreText }}</el-tag>
                        </div>
                      </div>
                    </template>

                    <div class="history-item">
                      <p class="question-text">{{ item.questionStem }}</p>
                      <div class="answer-grid">
                        <div class="answer-box">
                          <span>我的答案</span>
                          <p>{{ item.answerContent || '无记录' }}</p>
                        </div>
                        <div class="answer-box">
                          <span>标准答案</span>
                          <p>{{ item.standardAnswer || '暂无' }}</p>
                        </div>
                      </div>
                      <div v-if="item.analysis" class="analysis-box">
                        <span>题目解析</span>
                        <p>{{ item.analysis }}</p>
                      </div>
                      <div class="history-actions">
                        <el-button size="small" type="primary" plain @click="askAiForItem(item)">问 AI</el-button>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>

                <div class="pagination-wrap">
                  <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    background
                    layout="prev, pager, next, jumper, total"
                    :total="historyList.length"
                    :page-sizes="[5, 8, 10, 15]"
                  />
                </div>
              </div>
              <el-empty v-else description="当前没有可展示的历史做题记录" />
            </el-card>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentAnswerHistory, listStudentHistoryCatalogs } from '@/api/education/student'

const router = useRouter()
const loading = ref(false)
const legacyBound = ref(false)
const bindingMessage = ref('')
const historyList = ref([])
const catalogList = ref([])
const activeHistoryIds = ref([])
const currentPage = ref(1)
const pageSize = ref(8)

const queryForm = reactive({
  courseName: '',
  chapterCode: '',
  questionType: '',
  limit: 24
})

const invalidCatalogValues = ['未分类', '未命名课程', '未命名模块', 'unnamed course', 'unnamed module', 'null', 'undefined', '?']

const courseOptions = computed(() => Array.from(new Set(catalogList.value.map((item) => item.courseName).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'zh-CN')))

const chapterOptions = computed(() => {
  const source = catalogList.value.filter((item) => !queryForm.courseName || item.courseName === queryForm.courseName)
  const map = new Map()
  source.forEach((item) => {
    const key = `${item.chapterCode || ''}-${item.chapterName || ''}`
    if (!map.has(key)) {
      map.set(key, { chapterCode: item.chapterCode || '', chapterName: item.chapterName || '未命名模块' })
    }
  })
  return Array.from(map.values())
})

const overviewCards = computed(() => {
  const total = historyList.value.length
  const correct = historyList.value.filter((item) => item.isCorrect === '1').length
  const programCount = historyList.value.filter((item) => item.questionType === 'program').length
  const rate = total ? Math.round((correct / total) * 100) : 0
  return [
    { label: '当前展示记录', value: `${total} 条`, tip: '只展示清洗后的真实作答记录' },
    { label: '当前正确率', value: `${rate}%`, tip: '基于当前筛选结果计算' },
    { label: '编程题数量', value: `${programCount}`, tip: '当前筛选结果中的编程题记录' },
    { label: '当前课程', value: queryForm.courseName || '全部', tip: '可按课程和模块筛选' }
  ]
})

const pagedHistoryList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return historyList.value.slice(start, start + pageSize.value)
})

function normalizeCatalogItem(item) {
  const courseName = String(item?.courseName || item?.course_name || '').trim()
  const chapterName = String(item?.chapterName || item?.chapter_name || '').trim()
  const chapterCode = String(item?.chapterCode || item?.chapter_code || chapterName).trim()
  const lowerCourse = courseName.toLowerCase()
  const lowerChapter = chapterName.toLowerCase()
  if (!courseName || !chapterName) return null
  if (/^\d+$/.test(courseName) || /^\d+$/.test(chapterName)) return null
  if (invalidCatalogValues.includes(lowerCourse) || invalidCatalogValues.includes(lowerChapter)) return null
  return {
    id: item?.id,
    courseName,
    chapterCode: chapterCode || chapterName,
    chapterName
  }
}

function mergeCatalogs(...groups) {
  const merged = new Map()
  groups.flat().filter(Boolean).forEach((item) => {
    const normalized = normalizeCatalogItem(item)
    if (!normalized) return
    const key = `${normalized.courseName}||${normalized.chapterCode}||${normalized.chapterName}`
    if (!merged.has(key) && (normalized.courseName || normalized.chapterCode || normalized.chapterName)) {
      merged.set(key, normalized)
    }
  })
  return Array.from(merged.values())
}

function formatItem(item) {
  const submitTime = item.submitTime ? new Date(item.submitTime) : null
  const submitTimeText = submitTime && !Number.isNaN(submitTime.getTime())
    ? `${submitTime.getFullYear()}-${String(submitTime.getMonth() + 1).padStart(2, '0')}-${String(submitTime.getDate()).padStart(2, '0')} ${String(submitTime.getHours()).padStart(2, '0')}:${String(submitTime.getMinutes()).padStart(2, '0')}`
    : '时间未知'
  return {
    ...item,
    questionTypeText: item.questionType === 'program' ? '编程题' : '选择题',
    correctText: item.isCorrect === '1' ? '答对' : '答错',
    correctType: item.isCorrect === '1' ? 'success' : 'danger',
    scoreText: item.score ? `${Number(item.score)} 分` : '未计分',
    submitTimeText
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const [catalogRes, historyRes] = await Promise.all([
      listStudentHistoryCatalogs({ courseName: queryForm.courseName || undefined }),
      getStudentAnswerHistory({
        courseName: queryForm.courseName || undefined,
        chapterCode: queryForm.chapterCode || undefined,
        questionType: queryForm.questionType || undefined,
        limit: queryForm.limit
      })
    ])
    catalogList.value = mergeCatalogs(catalogRes?.data || [])
    historyList.value = (historyRes?.data || []).map(formatItem)
    legacyBound.value = historyRes?.bound !== false
    bindingMessage.value = historyRes?.message || catalogRes?.message || ''
    currentPage.value = 1
    activeHistoryIds.value = historyList.value.length ? [String(historyList.value[0].id)] : []
  } finally {
    loading.value = false
  }
}

function goToRag() {
  const latest = historyList.value[0]
  const question = latest?.questionStem
    ? `请帮我分析这道题为什么容易做错，并讲解解题思路：${latest.questionStem}`
    : '请结合我最近的历史做题情况，告诉我应该怎么复盘。'
  router.push({
    path: '/education/rag',
    query: {
      source: 'student-history',
      question,
      courseName: latest?.courseName || queryForm.courseName || '',
      chapterCode: latest?.chapterCode || queryForm.chapterCode || '',
      chapterName: latest?.chapterName || ''
    }
  })
}

function handleCourseChange() {
  const exists = chapterOptions.value.some((item) => item.chapterCode === queryForm.chapterCode || item.chapterName === queryForm.chapterCode)
  if (!exists) {
    queryForm.chapterCode = ''
  }
  loadHistory()
}

watch([currentPage, pageSize, pagedHistoryList], () => {
  const first = pagedHistoryList.value[0]
  activeHistoryIds.value = first ? [String(first.id)] : []
})

function askAiForItem(item) {
  const questionStem = item?.questionStem || '请结合这道题讲解解题思路。'
  const courseName = item?.courseName || queryForm.courseName || ''
  const chapterCode = item?.chapterCode || queryForm.chapterCode || ''
  const chapterName = item?.chapterName || ''
  const knowledgePoint = item?.knowledgePoint || ''
  router.push({
    path: '/education/rag',
    query: {
      source: 'student-question-ask',
      question: `请结合这道题讲解我的错误原因，并给出同模块复习建议：${questionStem}`,
      courseName,
      chapterCode,
      chapterName,
      knowledgePoint,
      questionId: item?.questionId || ''
    }
  })
}

onMounted(loadHistory)
</script>

<style scoped lang="scss">
.student-page-shell { min-height: 100vh; padding: 24px; background: radial-gradient(circle at top left, rgba(59,130,246,.12), transparent 24%), linear-gradient(180deg, #f8fbff 0%, #fffef8 100%); }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; }
.eyebrow { margin: 0 0 8px; color: #2563eb; font-size: 12px; letter-spacing: .16em; text-transform: uppercase; }
h1 { margin: 0; font-size: 32px; color: #0f172a; }
.subtitle { margin-top: 10px; max-width: 760px; color: #475569; line-height: 1.7; }
.header-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.notice-card { margin-bottom: 16px; }
.page-body { display: grid; grid-template-columns: 284px minmax(0, 1fr); gap: 18px; }
.side-nav,.summary-card,.panel-card { border-radius: 22px; border: 1px solid rgba(15,23,42,.08); background: rgba(255,255,255,.94); box-shadow: 0 16px 36px rgba(15,23,42,.08); }
.side-nav { padding: 20px; height: fit-content; }
.nav-title { margin-bottom: 14px; color: #0f172a; font-weight: 800; font-size: 22px; }
.nav-item { width: 100%; text-align: left; border: 1px solid rgba(148,163,184,.18); background: #fff; border-radius: 18px; padding: 18px; margin-bottom: 12px; cursor: pointer; }
.nav-item.active { border-color: rgba(37,99,235,.45); background: rgba(239,246,255,.96); }
.nav-item strong,.history-head strong,.answer-box span,.analysis-box span { display: block; color: #0f172a; }
.nav-item strong { font-size: 21px; }
.nav-item span,.summary-card p,.history-head p,.answer-box p,.analysis-box p { color: #64748b; line-height: 1.7; }
.nav-item span { font-size: 17px; }
.content-area,.left-column,.right-column,.history-list { display: flex; flex-direction: column; gap: 18px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 16px; }
.summary-card { padding: 20px; }
.summary-card span { display: block; color: #2563eb; }
.summary-card strong { display: block; margin: 10px 0 8px; font-size: 28px; color: #0f172a; }
.content-grid { display: grid; grid-template-columns: .9fr 1.1fr; gap: 18px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; }
.history-item,.answer-box,.analysis-box { padding: 14px; border-radius: 16px; background: rgba(248,250,252,.92); border: 1px solid rgba(148,163,184,.16); }
.history-collapse { display: flex; flex-direction: column; gap: 14px; }
.history-collapse-item { border-radius: 18px; border: 1px solid rgba(148,163,184,.16); background: rgba(248,250,252,.92); overflow: hidden; }
.history-collapse-title,.head-tags,.answer-grid { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; width: 100%; }
.history-title-main { min-width: 0; }
.history-title-main p { margin: 6px 0 0; color: #64748b; line-height: 1.7; }
.answer-grid { margin-top: 12px; }
.answer-box { flex: 1; }
.question-text { margin: 12px 0 0; color: #0f172a; line-height: 1.8; }
.analysis-box { margin-top: 12px; }
.history-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.pagination-wrap { display: flex; justify-content: center; padding-top: 4px; }
:deep(.el-collapse) { border-top: 0; border-bottom: 0; }
:deep(.el-collapse-item__header) { height: auto; min-height: 84px; padding: 16px 18px; background: transparent; border-bottom: 0; line-height: normal; }
:deep(.el-collapse-item__wrap) { background: transparent; border-bottom: 0; }
:deep(.el-collapse-item__content) { padding: 0 18px 18px; }
@media (max-width: 992px) {
  .page-header { flex-direction: column; }
  .page-body,.summary-grid,.content-grid,.answer-grid { grid-template-columns: 1fr; display: grid; }
  .history-collapse-title,.head-tags { flex-direction: column; }
}
</style>
