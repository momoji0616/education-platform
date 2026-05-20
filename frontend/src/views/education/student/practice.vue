<template>
  <div class="student-practice-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Student Practice</p>
        <h1>智能刷题推荐</h1>
        <p class="hero-text">
          系统会基于你的真实作答记录、课程模块掌握情况和题库数据进行规则推荐；如果当前账号历史数据不足，会自动切换为基础训练模板。
        </p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="router.push('/education/student/pad')">返回学生 Pad</el-button>
        <el-button type="primary" :loading="loading" @click="loadPracticeData">刷新推荐</el-button>
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

    <el-row :gutter="16" class="layout-row">
      <el-col :xs="24" :lg="6" class="nav-col">
        <aside class="side-nav practice-nav-card">
          <div class="nav-title">学生导航</div>
          <button class="nav-item" @click="router.push('/education/student/history')">
            <strong>历史做题</strong>
            <span>查看每次真实作答记录与得分情况</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/report')">
            <strong>学生诊断</strong>
            <span>查看真实作答支撑下的薄弱点诊断</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/plan')">
            <strong>学习规划</strong>
            <span>把诊断结果转成可执行的学习任务</span>
          </button>
          <button class="nav-item active">
            <strong>智能刷题</strong>
            <span>基于真实题库与薄弱模块做推荐训练</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/prediction')">
            <strong>成绩预测</strong>
            <span>上传变量数据训练模型并预测成绩</span>
          </button>
          <button class="nav-item" @click="router.push('/education/student/assistant')">
            <strong>师生AI助手</strong>
            <span>进入班级互动与私聊问答</span>
          </button>
          <button class="nav-item" @click="goToRagWithPracticeContext">
            <strong>RAG智能问答</strong>
            <span>带着刷题上下文继续追问</span>
          </button>
        </aside>

        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>推荐条件</span>
            </div>
          </template>

          <el-form :model="practiceForm" label-width="88px" :class="{ 'ai-mode': practiceForm.recommendMode === 'ai' }">
            <el-form-item label="推荐方式" class="recommend-mode-item">
              <el-select v-model="practiceForm.recommendMode" style="width: 100%" @change="handleRecommendModeChange">
                <el-option label="按练习重点筛选" value="manual" />
                <el-option label="AI自动推荐目前最需要刷的题目" value="ai" />
              </el-select>
            </el-form-item>

            <div v-if="practiceForm.recommendMode === 'ai'" class="mode-note">
              <span class="mode-note-label">AI推荐</span>
              <p>系统会优先把当前最需要补强、最值得马上练的题目排在前面。</p>
            </div>
            <el-form-item label="课程">
              <el-select
                v-model="practiceForm.courseName"
                clearable
                filterable
                style="width: 100%"
                placeholder="请选择课程"
                no-data-text="暂无课程数据"
                @change="handleCourseChange"
              >
                <el-option v-for="item in courseOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>

            <el-form-item label="模块">
              <el-select
                v-model="practiceForm.chapterCode"
                clearable
                filterable
                style="width: 100%"
                placeholder="请选择模块"
                no-data-text="暂无模块数据"
                @change="loadPracticeData"
              >
                <el-option
                  v-for="item in chapterOptions"
                  :key="`${item.chapterCode}-${item.chapterName}`"
                  :label="item.chapterName"
                  :value="item.chapterCode"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="练习重点">
              <el-select v-model="practiceForm.goal" style="width: 100%" @change="loadPracticeData">
                <el-option label="薄弱模块补强" value="薄弱模块补强" />
                <el-option label="高频错题重练" value="高频错题重练" />
                <el-option label="章节巩固训练" value="章节巩固训练" />
              </el-select>
            </el-form-item>

            <el-form-item label="题目数量">
              <el-input-number v-model="practiceForm.limit" :min="4" :max="20" @change="loadPracticeData" />
            </el-form-item>
          </el-form>
        </el-card>

      </el-col>

      <el-col :xs="24" :lg="18" class="main-col">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <div class="panel-header-title">
                <el-tag v-if="practiceForm.recommendMode === 'ai'" type="success" effect="light">AI智能推荐</el-tag>
                <span>推荐练习清单</span>
                <el-tag effect="plain">{{ practiceList.length }} 题</el-tag>
              </div>
              <div class="export-actions">
                <el-button size="small" plain @click="exportQuestionSet">导出题目</el-button>
                <el-button size="small" plain type="success" @click="exportAnswerSet">导出答案解析</el-button>
                <el-button size="small" plain type="primary" @click="exportPdfSet">导出 PDF</el-button>
              </div>
            </div>
          </template>

          <div v-if="practiceList.length" class="practice-list">
            <div v-for="item in practiceList" :key="`${item.no}-${item.questionId || item.question}`" class="practice-item">
              <div class="practice-head">
                <div class="practice-title-row">
                  <strong class="practice-title">{{ item.no }}. {{ item.type }}</strong>
                  <span class="practice-subtitle">
                    {{ item.courseName || practiceForm.courseName || '未分类课程' }} / {{ item.chapterName || '综合模块' }}
                  </span>
                </div>
              </div>

              <p class="practice-question">{{ item.question }}</p>

              <div v-if="item.options.length" class="option-list">
                <div v-for="option in item.options" :key="option.key" class="option-item">
                  <span class="option-key">{{ option.key }}</span>
                  <span class="option-text">{{ option.text }}</span>
                </div>
              </div>

              <div class="practice-actions">
                <el-button class="ask-ai-button" type="primary" plain @click="askAiForPracticeItem(item)">问AI</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="当前没有可用推荐，请先刷新或切换课程/模块" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  getStudentPracticeRecommendations,
  listStudentExamScore,
  listStudentHistoryCatalogs
} from '@/api/education/student'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const examScoreList = ref([])
const catalogList = ref([])
const recommendationList = ref([])
const legacyBound = ref(false)
const bindingMessage = ref('')

const practiceForm = reactive({
  recommendMode: 'manual',
  courseName: '',
  chapterCode: '',
  goal: '薄弱模块补强',
  limit: 10
})

const invalidCatalogValues = ['未分类', '未命名课程', '未命名模块', 'unnamed course', 'unnamed module', 'null', 'undefined', '?']

const fallbackQuestionTemplates = [
  '围绕当前课程的核心概念设计一组基础辨析题，重点检查定义理解是否准确。',
  '围绕最近容易做错的模块，安排一组同知识点再训练题目。',
  '基于当前课程模块，设计一组综合训练题，帮助串联知识点。',
  '围绕高频错题对应的知识点，安排一组纠错训练题。',
  '针对最近正确率偏低的模块，设计一组分层训练题。',
  '围绕当前章节关键概念，安排一组概念辨析与应用题。',
  '针对常见易错点，设计一组对比训练题。',
  '围绕考试高频考点，安排一组基础到进阶训练题。',
  '针对最近练习薄弱点，补一组巩固练习。',
  '围绕当前课程核心知识链路，安排一组综合复盘题。'
]

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function difficultyTag(label) {
  if (['鎻愰珮', '杈冮毦', 'hard'].includes(label)) return 'danger'
  if (['基础', '较易', 'easy'].includes(label)) return 'success'
  return 'warning'
}

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

function normalizeListResponse(res) {
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.rows)) return res.rows
  return []
}

function buildCatalogsFromRecommendations(list) {
  return (Array.isArray(list) ? list : []).map((item) => ({
    courseName: item.courseName || item.course_name || '',
    chapterCode: item.chapterCode || item.chapter_code || '',
    chapterName: item.chapterName || item.chapter_name || '未命名模块'
  }))
}

function mergeRecommendationItems(...groups) {
  const merged = new Map()
  groups.flat().filter(Boolean).forEach((item) => {
    const questionId = String(item?.questionId || item?.id || '').trim()
    const stem = String(item?.questionStem || item?.question || '').trim()
    const key = questionId || `${item?.courseName || ''}||${item?.chapterCode || item?.chapterName || ''}||${stem}`
    if (!key) return
    if (!merged.has(key)) {
      merged.set(key, item)
    }
  })
  return Array.from(merged.values())
}

function normalizeOptionItem(option, index) {
  if (typeof option === 'string') {
    return {
      key: `${String.fromCharCode(65 + index)}.`,
      text: option.trim()
    }
  }

  if (!option || typeof option !== 'object') {
    return null
  }

  const rawKey = option.key || option.label || option.optionKey || option.name || String.fromCharCode(65 + index)
  const rawText = option.text || option.content || option.value || option.optionText || ''
  const key = String(rawKey).replace(/[.、。．:]$/g, '').trim()
  const text = String(rawText).trim()
  if (!text) return null

  return {
    key: `${key || String.fromCharCode(65 + index)}.`,
    text
  }
}

function parseOptionsJson(rawValue) {
  if (!rawValue) return []

  let parsedValue = rawValue
  if (typeof rawValue === 'string') {
    try {
      parsedValue = JSON.parse(rawValue)
    } catch (error) {
      return []
    }
  }

  if (Array.isArray(parsedValue)) {
    return parsedValue.map(normalizeOptionItem).filter(Boolean)
  }

  if (parsedValue && typeof parsedValue === 'object') {
    if (Array.isArray(parsedValue.options)) {
      return parsedValue.options.map(normalizeOptionItem).filter(Boolean)
    }
    if (Array.isArray(parsedValue.items)) {
      return parsedValue.items.map(normalizeOptionItem).filter(Boolean)
    }
    return Object.keys(parsedValue)
      .filter((key) => /^[A-H]$/i.test(key))
      .sort()
      .map((key) => normalizeOptionItem({ key, text: parsedValue[key] }))
      .filter(Boolean)
  }

  return []
}

function parseQuestionContent(rawText) {
  const text = String(rawText || '').replace(/\r/g, '').trim()
  if (!text) {
    return { question: '请结合当前模块完成本题训练。', options: [] }
  }

  const normalized = text
    .replace(/([A-D])[.、。．]/g, '.')
    .replace(/([A-D])\s*[:：]/g, '. ')
    .replace(/\s+/g, ' ')
    .trim()

  const optionRegex = /([A-D])\.\s*(.*?)(?=\s+[A-D]\.\s*|$)/g
  const options = []
  let match
  while ((match = optionRegex.exec(normalized)) !== null) {
    options.push({ key: `${match[1]}.`, text: match[2].trim() })
  }

  if (!options.length) {
    return { question: text, options: [] }
  }

  const firstOption = normalized.search(/[A-D]\.\s*/)
  const question = firstOption > 0 ? normalized.slice(0, firstOption).trim() : text
  return { question, options }
}

function buildQuestionDisplay(item) {
  const parsedFromStem = parseQuestionContent(item.questionStem || item.question)
  const parsedOptions = parseOptionsJson(item.optionsJson || item.options_json)
  return {
    question: parsedFromStem.question,
    options: parsedOptions.length ? parsedOptions : parsedFromStem.options
  }
}

function resolveReasonModel() {
  if (!legacyBound.value) {
    return '当前是规则推荐，不是机器学习打分模型。系统会用基础模板和当前筛选条件生成训练清单。'
  }
  return '当前是规则排序推荐，不是独立大模型判题。系统会综合课程、模块、历史作答次数、正确次数和练习重点进行排序。'
}

function buildRecommendReason(item) {
  if (!legacyBound.value) {
    return '当前账号历史做题数据不足，系统先按课程、模块和练习重点生成一组基础训练题。'
  }

  const attemptCount = safeNumber(item.attemptCount)
  const correctCount = safeNumber(item.correctCount)
  const wrongCount = Math.max(attemptCount - correctCount, 0)
  const accuracy = attemptCount > 0 ? Math.round((correctCount / attemptCount) * 100) : 0
  const parts = []

  if (practiceForm.goal === '薄弱模块补强') {
    parts.push('当前练习重点：薄弱模块补强')
  } else if (practiceForm.goal === '高频错题重练') {
    parts.push('当前练习重点：高频错题重练')
  } else {
    parts.push('当前练习重点：章节巩固训练')
  }

  if (item.chapterName) {
    parts.push(`题目归属模块：${item.chapterName}`)
  }

  if (attemptCount === 0) {
    parts.push('你在这道题上还没有历史作答记录，适合补齐该模块练习覆盖。')
  } else if (wrongCount > 0) {
    parts.push(`历史作答 ${attemptCount} 次，其中错了 ${wrongCount} 次，当前正确率约 ${accuracy}%`)
  } else {
    parts.push(`历史已作答 ${attemptCount} 次且表现较稳定，适合作为巩固题继续复盘。`)
  }

  if (item.knowledgePoint) {
    parts.push(`瑕嗙洊鐭ヨ瘑鐐癸細${item.knowledgePoint}`)
  }

  return parts.join('；') + '。'
}

function buildAiPriorityScore(item) {
  const attemptCount = safeNumber(item.attemptCount)
  const correctCount = safeNumber(item.correctCount)
  const wrongCount = Math.max(attemptCount - correctCount, 0)
  const accuracy = attemptCount > 0 ? Math.round((correctCount / attemptCount) * 100) : 0
  const zeroAttemptBonus = attemptCount === 0 ? 30 : 0
  const wrongBonus = wrongCount * 22
  const lowAccuracyBonus = attemptCount > 0 ? Math.max(0, 100 - accuracy) : 18
  const chapterMatchBonus = practiceForm.chapterCode && item.chapterCode === practiceForm.chapterCode ? 16 : 0
  const knowledgePointBonus = item.knowledgePoint ? 8 : 0

  let goalBonus = 0
  if (practiceForm.goal === '薄弱模块补强') {
    goalBonus = lowAccuracyBonus + wrongCount * 6
  } else if (practiceForm.goal === '高频错题重练') {
    goalBonus = wrongCount * 16 + attemptCount * 2
  } else {
    goalBonus = chapterMatchBonus + 12
  }

  return zeroAttemptBonus + wrongBonus + lowAccuracyBonus + chapterMatchBonus + knowledgePointBonus + goalBonus
}

function buildAiModeReason(item) {
  const attemptCount = safeNumber(item.attemptCount)
  const correctCount = safeNumber(item.correctCount)
  const wrongCount = Math.max(attemptCount - correctCount, 0)
  const accuracy = attemptCount > 0 ? Math.round((correctCount / attemptCount) * 100) : 0
  const parts = ['当前为 AI 自动推荐模式']

  if (attemptCount === 0) {
    parts.push('这道题你还没有做过，适合优先补齐覆盖')
  }
  if (wrongCount > 0) {
    parts.push(`你历史作答 ${attemptCount} 次，其中错了 ${wrongCount} 次`)
    parts.push(`当前正确率约 ${accuracy}%`)
  }

  if (practiceForm.goal === '薄弱模块补强') {
    parts.push('系统优先拉高薄弱项权重')
  } else if (practiceForm.goal === '高频错题重练') {
    parts.push('系统优先拉高错题重练权重')
  } else {
    parts.push('系统优先保证当前章节训练连续性')
  }

  if (item.chapterName) {
    parts.push(`所属模块：${item.chapterName}`)
  }

  if (item.knowledgePoint) {
    parts.push(`涉及知识点：${item.knowledgePoint}`)
  }

  if (attemptCount === 0 && !item.knowledgePoint && !item.chapterName) {
    parts.push('用于补充当前题单的基础覆盖')
  }

  return parts.join('；') + '。'
}

function selectAiPracticeItems(list, limit) {
  const sorted = [...list]
    .map((item) => ({ ...item, aiPriorityScore: buildAiPriorityScore(item) }))
    .sort((a, b) => b.aiPriorityScore - a.aiPriorityScore)

  const selected = []
  const chapterCountMap = new Map()
  const knowledgePointSet = new Set()

  for (const item of sorted) {
    if (selected.length >= limit) break
    const chapterKey = `${item.courseName || ''}::${item.chapterCode || item.chapterName || '综合模块'}`
    const knowledgeKey = String(item.knowledgePoint || '').trim()
    const chapterCount = chapterCountMap.get(chapterKey) || 0
    const alreadyUsedKnowledge = knowledgeKey && knowledgePointSet.has(knowledgeKey)

    if (chapterCount >= 2 && alreadyUsedKnowledge) {
      continue
    }

    selected.push(item)
    chapterCountMap.set(chapterKey, chapterCount + 1)
    if (knowledgeKey) {
      knowledgePointSet.add(knowledgeKey)
    }
  }

  if (selected.length < limit) {
    const selectedIds = new Set(selected.map((item) => String(item.questionId || item.id || item.questionStem || item.question || '')))
    for (const item of sorted) {
      if (selected.length >= limit) break
      const key = String(item.questionId || item.id || item.questionStem || item.question || '')
      if (selectedIds.has(key)) continue
      selected.push(item)
      selectedIds.add(key)
    }
  }

  return selected.slice(0, limit)
}

function createPracticeItem(item, index) {
  const parsed = buildQuestionDisplay(item)
  const isChoiceQuestion = (item.questionType || '').toLowerCase() !== 'program'
  return {
    no: index + 1,
    type: isChoiceQuestion ? '选择题' : '编程题',
    questionId: item.questionId,
    courseName: item.courseName || practiceForm.courseName || '',
    chapterCode: item.chapterCode || practiceForm.chapterCode || '',
    chapterName: item.chapterName || '综合模块',
    knowledgePoint: item.knowledgePoint || '',
    difficulty: item.difficultyLevel || (isChoiceQuestion ? '基础' : '综合'),
    tagType: difficultyTag(item.difficultyLevel || ''),
    question: parsed.question,
    options: parsed.options,
    isChoiceQuestion,
    standardAnswer: item.standardAnswer || '',
    analysis: item.analysis || '',
    attemptCount: safeNumber(item.attemptCount),
    correctCount: safeNumber(item.correctCount),
    reasonModel: resolveReasonModel(),
    recommendReasonText: buildRecommendReason(item)
  }
}

const courseOptions = computed(() => {
  const values = Array.from(new Set(catalogList.value.map((item) => item.courseName).filter(Boolean)))
  return values.sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const chapterOptions = computed(() => {
  const source = catalogList.value.filter((item) => !practiceForm.courseName || item.courseName === practiceForm.courseName)
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

const practiceList = computed(() => {
  if (recommendationList.value.length) {
    const finalList = practiceForm.recommendMode === 'ai'
      ? selectAiPracticeItems(recommendationList.value, practiceForm.limit)
      : recommendationList.value.slice(0, practiceForm.limit)
    return finalList.map((item, index) => {
      const practiceItem = createPracticeItem(item, index)
      if (practiceForm.recommendMode === 'ai') {
        practiceItem.reasonModel = 'AI智能推荐'
        practiceItem.recommendReasonText = buildAiModeReason(item)
      }
      return practiceItem
    })
  }

  return fallbackQuestionTemplates.slice(0, practiceForm.limit).map((question, index) => ({
    no: index + 1,
    type: index % 3 === 2 ? '综合训练' : '章节训练',
    questionId: '',
    courseName: practiceForm.courseName || '当前课程',
    chapterCode: practiceForm.chapterCode || '',
    chapterName: chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode)?.chapterName || '综合模块',
    knowledgePoint: '',
    difficulty: index % 3 === 0 ? '基础' : '巩固',
    tagType: index % 3 === 0 ? 'success' : 'warning',
    question,
    options: [],
    isChoiceQuestion: false,
    standardAnswer: '',
    analysis: '',
    attemptCount: 0,
    correctCount: 0,
    reasonModel: resolveReasonModel(),
    recommendReasonText: '当前账号尚未绑定足够历史做题数据，因此先按课程、模块和练习重点生成基础训练模板。'
  }))
})

const overviewCards = computed(() => {
  const examScores = examScoreList.value.map((item) => safeNumber(item.score)).filter((item) => item > 0)
  const examAvg = examScores.length ? Math.round(examScores.reduce((sum, item) => sum + item, 0) / examScores.length) : 0
  return [
    {
      label: '推荐题量',
      value: `${practiceList.value.length} 题`,
      tip: legacyBound.value ? '默认展示 10 道，可按需调整' : '当前按基础模板兜底生成'
    },
    {
      label: '考试均分',
      value: examAvg ? `${examAvg} 分` : '暂无',
      tip: '来自当前业务考试成绩'
    }
  ]
})

function applyRouteContext() {
  if (route.query.courseName) {
    practiceForm.courseName = String(route.query.courseName)
  }
  if (route.query.chapterCode) {
    practiceForm.chapterCode = String(route.query.chapterCode)
  }
}

function syncSelectedCatalog(recommendationRes) {
  const recommendedCourse = recommendationRes?.courseName || ''
  const recommendedChapterCode = recommendationRes?.chapterCode || ''
  const recommendedChapterName = recommendationRes?.chapterName || ''

  if (!practiceForm.courseName && recommendedCourse) {
    practiceForm.courseName = recommendedCourse
  }

  if (recommendedCourse || recommendedChapterCode || recommendedChapterName) {
    catalogList.value = mergeCatalogs(catalogList.value, [{
      courseName: recommendedCourse || practiceForm.courseName,
      chapterCode: recommendedChapterCode,
      chapterName: recommendedChapterName
    }])
  }

  if (!practiceForm.courseName && courseOptions.value.length) {
    practiceForm.courseName = courseOptions.value[0]
  }

  const chapterExists = chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode || item.chapterName === practiceForm.chapterCode)
  if (!chapterExists && recommendedChapterCode) {
    practiceForm.chapterCode = recommendedChapterCode
  }

  if (!practiceForm.chapterCode && chapterOptions.value.length) {
    practiceForm.chapterCode = chapterOptions.value[0].chapterCode
  }
}

async function loadPracticeData() {
  loading.value = true
  try {
    const isAiMode = practiceForm.recommendMode === 'ai'
    const scopedLimit = isAiMode ? Math.max(practiceForm.limit * 2, 20) : practiceForm.limit
    const broadLimit = Math.max(practiceForm.limit * 4, 36)

    const requestTasks = [
      listStudentExamScore(),
      listStudentHistoryCatalogs({ courseName: practiceForm.courseName || undefined }),
      getStudentPracticeRecommendations({
        courseName: practiceForm.courseName || undefined,
        chapterCode: practiceForm.chapterCode || undefined,
        limit: scopedLimit
      })
    ]

    if (isAiMode) {
      requestTasks.push(
        getStudentPracticeRecommendations({
          courseName: practiceForm.courseName || undefined,
          limit: broadLimit
        }),
        getStudentPracticeRecommendations({
          limit: broadLimit
        })
      )
    }

    const responses = await Promise.all(requestTasks)
    const [examRes, historyCatalogRes, recommendationRes, courseWideRes, globalWideRes] = responses

    const examList = normalizeListResponse(examRes)
    const historyCatalogData = normalizeListResponse(historyCatalogRes)
    const recommendationData = normalizeListResponse(recommendationRes)
    const courseWideData = normalizeListResponse(courseWideRes)
    const globalWideData = normalizeListResponse(globalWideRes)
    const mergedRecommendationData = isAiMode
      ? mergeRecommendationItems(recommendationData, courseWideData, globalWideData)
      : recommendationData

    examScoreList.value = examList
    catalogList.value = mergeCatalogs(
      historyCatalogData,
      buildCatalogsFromRecommendations(mergedRecommendationData)
    )
    recommendationList.value = mergedRecommendationData
    legacyBound.value = recommendationRes?.bound !== false
    bindingMessage.value = recommendationRes?.message || ''
    syncSelectedCatalog(recommendationRes)
  } finally {
    loading.value = false
  }
}

function handleCourseChange() {
  if (!chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode || item.chapterName === practiceForm.chapterCode)) {
    practiceForm.chapterCode = ''
  }
  loadPracticeData()
}

function handleRecommendModeChange() {
  loadPracticeData()
}

function askAiForPracticeItem(item) {
  const questionText = [item.question, ...item.options.map((option) => `${option.key} ${option.text}`)].join('\n')
  router.push({
    path: '/education/rag',
    query: {
      source: 'student-question-ask',
      question: questionText,
      courseName: practiceForm.courseName || item.courseName || '',
      chapterCode: practiceForm.chapterCode || item.chapterCode || '',
      chapterName: item.chapterName || '',
      knowledgePoint: item.knowledgePoint || '',
      questionId: item.questionId || ''
    }
  })
}

function goToRagWithPracticeContext() {
  const chapter = chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode)
  const question = legacyBound.value
    ? `请解释${practiceForm.courseName || '当前课程'}${chapter?.chapterName ? `的${chapter.chapterName}` : ''}为什么值得优先练习，并给出答题思路。`
    : `请结合${practiceForm.courseName || '当前课程'}，给我一份基础刷题建议和做题顺序。`

  router.push({
    path: '/education/rag',
    query: {
      question,
      source: 'student-practice',
      courseName: practiceForm.courseName || '',
      chapterCode: practiceForm.chapterCode || '',
      chapterName: chapter?.chapterName || ''
    }
  })
}

function downloadTextFile(filename, content) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

function buildQuestionText(includeAnswers = false) {
  const header = [
    '智能刷题推荐导出',
    `课程：${practiceForm.courseName || '未限定'}`,
    `模块：${chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode)?.chapterName || '未限定'}`,
    `练习重点：${practiceForm.goal}`,
    `题目数量：${practiceList.value.length}`,
    ''
  ]

  const body = practiceList.value.map((item) => {
    const lines = [
      `${item.no}. ${item.type}`,
      `课程/模块：${item.courseName || '未分类课程'} / ${item.chapterName || '综合模块'}`,
      `题目：${item.question}`
    ]
    if (item.options.length) {
      item.options.forEach((option) => lines.push(`${option.key} ${option.text}`))
    }
    lines.push(`推荐机制：${item.reasonModel}`)
    lines.push(`推荐原因：${item.recommendReasonText}`)
    if (includeAnswers) {
      lines.push(`标准答案：${item.standardAnswer || '暂无'}`)
      lines.push(`解析：${item.analysis || '暂无'}`)
    }
    return lines.join('\n')
  })

  return header.concat(body.join('\n\n')).join('\n')
}

function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function buildPrintHtml() {
  const itemsHtml = practiceList.value.map((item) => {
    const optionsHtml = item.options.length
      ? `<div class="options">${item.options.map((option) => `<p><strong>${escapeHtml(option.key)}</strong> ${escapeHtml(option.text)}</p>`).join('')}</div>`
      : ''
    const answerHtml = `<div class="answer-box"><p><strong>标准答案：</strong>${escapeHtml(item.standardAnswer || '暂无')}</p><p><strong>解析：</strong>${escapeHtml(item.analysis || '暂无')}</p></div>`
    return `
      <section class="item">
        <h3>${item.no}. ${escapeHtml(item.type)}</h3>
        <p class="meta">${escapeHtml(item.courseName || '未分类课程')} / ${escapeHtml(item.chapterName || '综合模块')}</p>
        <p>${escapeHtml(item.question)}</p>
        ${optionsHtml}
        <p><strong>推荐机制：</strong>${escapeHtml(item.reasonModel)}</p>
        <p><strong>推荐原因：</strong>${escapeHtml(item.recommendReasonText)}</p>
        ${answerHtml}
      </section>
    `
  }).join('')

  return `
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
      <meta charset="UTF-8" />
      <title>智能刷题推荐导出</title>
      <style>
        body { font-family: "Microsoft YaHei", sans-serif; padding: 24px; color: #0f172a; }
        h1 { margin-bottom: 8px; }
        .summary { margin-bottom: 24px; color: #475569; }
        .item { border: 1px solid #cbd5e1; border-radius: 12px; padding: 16px; margin-bottom: 16px; page-break-inside: avoid; }
        .meta { color: #64748b; }
        .options p, .answer-box p { margin: 6px 0; }
      </style>
    </head>
    <body>
      <h1>智能刷题推荐</h1>
      <p class="summary">课程：${escapeHtml(practiceForm.courseName || '未限定')}，模块：${escapeHtml(chapterOptions.value.find((item) => item.chapterCode === practiceForm.chapterCode)?.chapterName || '未限定')}，练习重点：${escapeHtml(practiceForm.goal)}</p>
      ${itemsHtml}
    </body>
    </html>
  `
}

function exportQuestionSet() {
  if (!practiceList.value.length) {
    ElMessage.warning('当前没有可导出的题目')
    return
  }
  downloadTextFile('智能刷题推荐-题目.txt', buildQuestionText(false))
}

function exportAnswerSet() {
  if (!practiceList.value.length) {
    ElMessage.warning('当前没有可导出的答案解析')
    return
  }
  downloadTextFile('智能刷题推荐-答案解析.txt', buildQuestionText(true))
}

function exportPdfSet() {
  if (!practiceList.value.length) {
    ElMessage.warning('当前没有可导出的 PDF 内容')
    return
  }
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器拦截了新窗口，请允许弹窗后重试')
    return
  }
  printWindow.document.open()
  printWindow.document.write(buildPrintHtml())
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 300)
}

watch(() => route.fullPath, applyRouteContext)

onMounted(() => {
  applyRouteContext()
  loadPracticeData()
})
</script>

<style scoped lang="scss">
.student-practice-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.18), transparent 34%),
    linear-gradient(180deg, #eef6ff 0%, #f8fbff 52%, #ffffff 100%);
}

.hero-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 30px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin: 0 0 10px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: #0369a1;
}

.hero-card h1 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.hero-text {
  margin: 14px 0 0;
  max-width: 760px;
  color: #475569;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.notice-card,
.overview-row,
.panel-card {
  margin-top: 18px;
}

.overview-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.overview-card span {
  display: block;
  color: #64748b;
}

.overview-card strong {
  display: block;
  margin-top: 14px;
  font-size: 28px;
  color: #0f172a;
}

.overview-card p {
  margin: 14px 0 0;
  color: #64748b;
}

.panel-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.side-nav {
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
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

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-header-title > .el-tag:nth-of-type(2) {
  display: none;
}

.export-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.practice-nav-card {
  margin-bottom: 18px;
}

.layout-row {
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
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

.practice-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.practice-item {
  padding: 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.practice-head {
  display: flex;
  gap: 12px;
}

.practice-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.practice-title {
  font-size: 18px;
  line-height: 1.4;
  color: #0f172a;
}

.practice-subtitle {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1.4;
  color: #64748b;
  background: rgba(226, 232, 240, 0.6);
}

.practice-question {
  margin: 14px 0 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 500;
  line-height: 1.9;
}

.option-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.option-key {
  min-width: 26px;
  font-weight: 700;
  color: #0369a1;
}

.option-text {
  color: #0f172a;
  line-height: 1.7;
}

.mode-note {
  margin: 10px 0 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(236, 253, 245, 0.92);
  border: 1px solid rgba(34, 197, 94, 0.18);
}

.mode-note-label {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #15803d;
  background: rgba(220, 252, 231, 0.95);
}

.mode-note p {
  margin: 8px 0 0;
  color: #334155;
  line-height: 1.7;
}

:deep(.el-form > .el-form-item:nth-of-type(2)) {
  display: none;
}

.ai-mode :deep(.el-form-item:nth-of-type(5)) {
  display: none;
}

.practice-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.ask-ai-button {
  min-height: 42px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 992px) {
  .student-practice-page {
    padding: 16px;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
