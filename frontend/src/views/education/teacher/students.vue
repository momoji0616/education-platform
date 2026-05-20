<template>
  <div class="teacher-students-page">
    <aside class="side-nav">
      <div class="nav-title">教师导航</div>
      <button class="nav-item active">
        <strong>学生管理</strong>
        <span>查看学生做题与近期学习表现，支持按课程、模块精准筛选。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/analysis')">
        <strong>学情分析</strong>
        <span>查看课程与模块层面的真实学情，定位班级共性薄弱点。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/paper')">
        <strong>智能组卷</strong>
        <span>按课程、模块与题型生成试卷草案，支持不同组卷模式。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/qa')">
        <strong>成绩预测</strong>
        <span>上传变量数据训练模型，并预测最终成绩表现。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/assistant')">
        <strong>师生AI助手</strong>
        <span>查看互动学生、涉及模块和互动总数等辅助信息。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/rag')">
        <strong>RAG智能问答</strong>
        <span>基于 RAG 做课程资料、题目与学情相关的智能问答。</span>
      </button>
    </aside>

    <main class="content-area">
      <header class="page-header">
        <div>
          <p class="eyebrow">Teacher Students</p>
          <h1>学生管理与真实作答记录</h1>
          <p class="subtitle">
            这里展示已导入系统的真实学生做题数据，支持按学生、课程、模块筛选，并查看单个学生最近的历史作答详情。
          </p>
        </div>
        <div class="header-actions">
          <el-button plain @click="router.push('/education/teacher/pad')">返回教师首页</el-button>
          <el-button type="primary" :loading="loading" @click="loadAll">刷新数据</el-button>
        </div>
      </header>

      <section class="overview-grid">
        <el-card class="overview-card" shadow="never">
          <span>真实学生数</span>
          <strong>{{ totalStudents }}</strong>
          <p>当前已导入并可在系统中分析的学生规模</p>
        </el-card>
        <el-card class="overview-card" shadow="never">
          <span>真实作答记录</span>
          <strong>{{ totalAnswers }}</strong>
          <p>历史做题与提交记录总量</p>
        </el-card>
        <el-card class="overview-card" shadow="never">
          <span>覆盖知识点数</span>
          <strong>{{ totalQuestions }}</strong>
          <p>当前统计到的真实知识点覆盖范围</p>
        </el-card>
      </section>

      <section class="content-grid">
        <aside class="filter-panel">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>筛选条件</span>
              </div>
            </template>

            <el-form :model="queryParams" label-width="76px">
              <el-form-item label="学生">
                <el-input v-model="queryParams.studentName" placeholder="输入学生姓名" clearable />
              </el-form-item>

              <el-form-item label="课程">
                <el-select
                  v-model="queryParams.courseName"
                  clearable
                  filterable
                  style="width: 100%"
                  placeholder="按课程筛选"
                  no-data-text="当前暂无课程数据"
                  @change="handleCourseChange"
                >
                  <el-option v-for="item in courseOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>

              <el-form-item label="模块">
                <el-select
                  v-model="queryParams.chapterCode"
                  :disabled="!queryParams.courseName"
                  clearable
                  filterable
                  style="width: 100%"
                  placeholder="请先选择课程，再按模块筛选"
                  no-data-text="请先选择课程，再查看该课程下的模块"
                >
                  <el-option
                    v-for="item in chapterOptions"
                    :key="`${item.chapterCode}-${item.chapterName}`"
                    :label="item.chapterName"
                    :value="item.chapterCode"
                  />
                </el-select>
              </el-form-item>
            </el-form>

            <div class="filter-actions">
              <el-button type="primary" plain @click="handleQuery">查询</el-button>
              <el-button plain @click="resetQuery">重置</el-button>
            </div>
          </el-card>

          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>筛选说明</span>
              </div>
            </template>
            <div class="tips">
              <p>课程用于限定学生在哪门课里的作答记录，模块用于进一步缩小到课程下的具体章节或知识点。</p>
              <p>课程和模块下拉数据来自教师题库目录接口，不再是手动输入，因此不会再出现固定“无数据”的空筛选框。</p>
              <p>点击“查看详情”可展开该学生最近的真实作答记录，并继续进入 RAG 智能问答做针对性分析。</p>
            </div>
          </el-card>
        </aside>

        <section class="table-panel">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>学生模块作答情况</span>
                <el-tag effect="plain">{{ total }} 条</el-tag>
              </div>
            </template>

            <el-table :data="tableData" v-loading="loading" class="student-table" height="680">
              <el-table-column prop="studentName" label="学生" width="120" />
              <el-table-column prop="courseName" label="课程" min-width="150" />
              <el-table-column prop="chapterName" label="模块/章节" min-width="180" />
              <el-table-column prop="answerCount" label="做题数" width="90" />
              <el-table-column prop="correctRateText" label="正确率" width="100" />
              <el-table-column prop="latestSubmitTimeText" label="最近作答" min-width="160" />
              <el-table-column prop="judgment" label="表现判断" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="110" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openHistory(row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager-wrap">
              <el-pagination
                v-model:current-page="queryParams.pageNum"
                v-model:page-size="queryParams.pageSize"
                :page-sizes="[10, 20, 30, 50]"
                :total="total"
                layout="total, sizes, prev, pager, next"
                @size-change="loadTableData"
                @current-change="loadTableData"
              />
            </div>
          </el-card>
        </section>
      </section>
    </main>

    <el-drawer v-model="historyVisible" :title="historyTitle" size="48%">
      <div class="drawer-toolbar">
        <el-tag type="info" effect="plain">{{ historyList.length }} 条最近记录</el-tag>
        <el-button type="success" plain :disabled="!selectedStudent" @click="goToRagWithStudent">
          用 AI 分析该学生
        </el-button>
      </div>

      <div v-if="historyLoading" class="drawer-empty">正在加载该学生的历史作答记录...</div>
      <div v-else-if="!historyList.length" class="drawer-empty">当前没有可展示的历史作答记录。</div>
      <div v-else class="history-list">
        <div v-for="item in historyList" :key="item.id" class="history-item">
          <div class="history-head">
            <div>
              <strong>{{ item.courseName || '未命名课程' }} / {{ item.chapterName || '综合模块' }}</strong>
              <p>{{ item.questionTypeText }} · {{ item.submitTimeText }}</p>
            </div>
            <div class="head-tags">
              <el-tag :type="item.correctType" effect="light">{{ item.correctText }}</el-tag>
              <el-tag effect="plain">{{ item.scoreText }}</el-tag>
            </div>
          </div>
          <p class="question-text">{{ item.questionStem || '暂无题干' }}</p>
          <div class="answer-grid">
            <div class="answer-box">
              <span>学生答案</span>
              <p>{{ item.answerContent || '暂无记录' }}</p>
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
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTeacherAnalysisOverview, getTeacherStudentCatalogs, getTeacherStudentHistory, getTeacherStudentManagementPage } from '@/api/education/teacher'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const catalogList = ref([])

const totalStudents = ref(0)
const totalAnswers = ref(0)
const totalQuestions = ref(0)

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref([])
const selectedStudent = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  studentName: '',
  courseName: '',
  chapterCode: ''
})

const invalidCatalogValues = ['未分类', '未命名课程', '未命名模块', 'unnamed course', 'unnamed module', 'null', 'undefined', '?']

function normalizeListResponse(res) {
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res?.rows)) return res.rows
  if (Array.isArray(res)) return res
  return []
}

function normalizeCatalogItem(item = {}) {
  const courseName = String(item.courseName || item.course_name || '').trim()
  const chapterName = String(item.chapterName || item.chapter_name || '').trim()
  const chapterCode = String(item.chapterCode || item.chapter_code || chapterName).trim()
  const lowerCourse = courseName.toLowerCase()
  const lowerChapter = chapterName.toLowerCase()
  if (!courseName || !chapterName) return null
  if (/^\d+$/.test(courseName) || /^\d+$/.test(chapterName)) return null
  if (invalidCatalogValues.includes(lowerCourse) || invalidCatalogValues.includes(lowerChapter)) return null
  return {
    ...item,
    courseName,
    chapterCode: chapterCode || chapterName,
    chapterName
  }
}

const courseOptions = computed(() => {
  const values = Array.from(new Set(catalogList.value.map(item => item.courseName).filter(Boolean)))
  return values.sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const chapterOptions = computed(() => {
  if (!queryParams.courseName) return []
  const source = catalogList.value.filter(item => item.courseName === queryParams.courseName)
  const map = new Map()
  source.forEach((item) => {
    const key = `${item.chapterCode || ''}-${item.chapterName || ''}`
    if (!map.has(key)) {
      map.set(key, {
        chapterCode: item.chapterCode || '',
        chapterName: item.chapterName || '未命名模块'
      })
    }
  })
  return Array.from(map.values())
})

const historyTitle = computed(() => {
  if (!selectedStudent.value) return '学生历史作答详情'
  return `${selectedStudent.value.studentName} 的历史作答详情`
})

function formatDateTime(value) {
  if (!value) return '暂无'
  const time = new Date(value)
  if (Number.isNaN(time.getTime())) return String(value)
  const y = time.getFullYear()
  const m = String(time.getMonth() + 1).padStart(2, '0')
  const d = String(time.getDate()).padStart(2, '0')
  const hh = String(time.getHours()).padStart(2, '0')
  const mm = String(time.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function mapTableRow(item = {}) {
  const rate = Number(item.correctRate || 0)
  return {
    ...item,
    correctRateText: `${rate}%`,
    latestSubmitTimeText: formatDateTime(item.latestSubmitTime),
    judgment: rate < 50 ? '建议重点补强' : rate < 75 ? '需要继续巩固' : '当前表现较稳'
  }
}

function mapHistoryRow(item = {}) {
  return {
    ...item,
    questionTypeText: item.questionType === 'program' ? '编程题' : '选择题',
    correctText: item.isCorrect === '1' ? '答对' : '答错',
    correctType: item.isCorrect === '1' ? 'success' : 'danger',
    scoreText: item.score !== null && item.score !== undefined ? `${Number(item.score)} 分` : '未计分',
    submitTimeText: formatDateTime(item.submitTime)
  }
}

async function loadCatalogs() {
  const res = await getTeacherStudentCatalogs({
    courseName: queryParams.courseName || undefined
  })
  catalogList.value = normalizeListResponse(res).map(normalizeCatalogItem).filter(Boolean)
  if (queryParams.chapterCode && !chapterOptions.value.some(item => item.chapterCode === queryParams.chapterCode || item.chapterName === queryParams.chapterCode)) {
    queryParams.chapterCode = ''
  }
}

async function loadOverview() {
  const res = await getTeacherAnalysisOverview()
  const data = res?.data || {}
  totalStudents.value = Number(data.studentCount || 0)
  totalAnswers.value = Number(data.answerCount || 0)
  totalQuestions.value = Number(data.knowledgePointCount || 0)
}

async function loadTableData() {
  loading.value = true
  try {
    const res = await getTeacherStudentManagementPage(queryParams)
    total.value = Number(res?.total || 0)
    tableData.value = Array.isArray(res?.rows) ? res.rows.map(mapTableRow) : []
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await loadOverview()
  await loadCatalogs()
  await loadTableData()
}

async function openHistory(row) {
  selectedStudent.value = row
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await getTeacherStudentHistory({
      studentNo: row.studentNo,
      limit: 30
    })
    historyList.value = normalizeListResponse(res).map(mapHistoryRow)
  } finally {
    historyLoading.value = false
  }
}

function goToRagWithStudent() {
  if (!selectedStudent.value) return
  router.push({
    path: '/education/teacher/rag',
    query: {
      source: 'teacher-students',
      studentNo: selectedStudent.value.studentNo || '',
      studentName: selectedStudent.value.studentName || '',
      question: `请结合学生 ${selectedStudent.value.studentName} 在 ${selectedStudent.value.courseName || '当前课程'} ${selectedStudent.value.chapterName || '当前模块'} 的表现，分析主要薄弱点并给出教学建议。`,
      courseName: selectedStudent.value.courseName || '',
      chapterCode: selectedStudent.value.chapterCode || '',
      chapterName: selectedStudent.value.chapterName || ''
    }
  })
}

async function handleCourseChange() {
  queryParams.chapterCode = ''
  await loadCatalogs()
}

function handleQuery() {
  queryParams.pageNum = 1
  loadTableData()
}

function resetQuery() {
  queryParams.pageNum = 1
  queryParams.pageSize = 20
  queryParams.studentName = ''
  queryParams.courseName = ''
  queryParams.chapterCode = ''
  loadAll()
}

onMounted(() => {
  if (route.query.studentName) queryParams.studentName = String(route.query.studentName)
  if (route.query.courseName) queryParams.courseName = String(route.query.courseName)
  if (route.query.chapterCode) queryParams.chapterCode = String(route.query.chapterCode)
  loadAll()
})
</script>

<style scoped lang="scss">
.teacher-students-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 284px 1fr;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.12), transparent 24%),
    linear-gradient(180deg, #fffaf4 0%, #eef6ff 100%);
}

.side-nav,
.overview-card,
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

.nav-item strong,
.history-head strong,
.answer-box span,
.analysis-box span {
  display: block;
  color: #0f172a;
}

.nav-item strong {
  font-size: 21px;
}

.nav-item span,
.overview-card p,
.tips p,
.history-head p,
.answer-box p,
.analysis-box p {
  color: #64748b;
  line-height: 1.7;
}

.nav-item span {
  font-size: 17px;
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
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

.subtitle {
  margin-top: 10px;
  max-width: 780px;
  color: #475569;
  line-height: 1.7;
}

.header-actions,
.filter-actions,
.drawer-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  padding: 22px;
}

.overview-card span {
  display: block;
  color: #ea580c;
  font-size: 15px;
}

.overview-card strong {
  display: block;
  margin: 12px 0 10px;
  color: #0f172a;
  font-size: 38px;
}

.content-grid {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #0f172a;
  font-weight: 700;
}

.student-table {
  :deep(.el-table__header-wrapper th.el-table__cell) {
    font-size: 17px;
    padding-top: 16px;
    padding-bottom: 16px;
  }

  :deep(.el-table__body-wrapper td.el-table__cell) {
    font-size: 15px;
    padding-top: 15px;
    padding-bottom: 15px;
  }

  :deep(.cell) {
    line-height: 1.6;
  }
}

.tips {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.drawer-empty {
  color: #64748b;
  padding: 18px 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.history-item {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.history-head,
.head-tags,
.answer-grid {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.answer-grid {
  margin-top: 12px;
}

.answer-box,
.analysis-box {
  flex: 1;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
}

.question-text {
  margin: 12px 0 0;
  color: #0f172a;
  line-height: 1.7;
}

.analysis-box {
  margin-top: 12px;
}

@media (max-width: 1200px) {
  .teacher-students-page,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
