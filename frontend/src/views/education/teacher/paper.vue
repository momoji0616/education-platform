<template>
  <div class="teacher-paper-page">
    <aside class="side-nav">
      <div class="nav-title">教师导航</div>
      <button class="nav-item" @click="router.push('/education/teacher/students')">
        <strong>学生管理</strong>
        <span>查看学生做题与近期学习表现，支持按课程、模块精准筛选。</span>
      </button>
      <button class="nav-item" @click="router.push('/education/teacher/analysis')">
        <strong>学情分析</strong>
        <span>查看课程与模块层面的真实学情，定位班级共性薄弱点。</span>
      </button>
      <button class="nav-item active">
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
      <header class="hero-card">
        <div class="hero-main">
          <p class="eyebrow">Teacher Paper</p>
          <h1>智能题库组卷</h1>
          <p class="hero-text">
            当前草案只使用系统中已导入的真实题库。现在不再按难度过滤，避免把题筛空；你可以直接按课程、模块、题型和组卷模式来生成结果。
          </p>
          <div class="hero-tags">
            <el-tag effect="plain" type="success">真实题库</el-tag>
            <el-tag effect="plain" type="warning">课程模块联动</el-tag>
            <el-tag effect="plain" type="primary">不按难度筛空</el-tag>
          </div>
        </div>

        <div class="hero-actions">
          <el-button plain @click="exportQuestionSet">导出题目</el-button>
          <el-button plain type="success" @click="exportAnswerSet">导出答案解析</el-button>
          <el-button plain type="primary" @click="exportPdfSet">导出 PDF</el-button>
          <el-button plain @click="router.push('/education/teacher/pad')">返回教师首页</el-button>
          <el-button type="primary" :loading="loading" @click="generatePaper">刷新草案</el-button>
        </div>
      </header>

      <el-alert
        :title="contextMessage"
        type="success"
        :closable="false"
        show-icon
        class="notice-card"
      />

      <section class="content-grid">
        <aside class="filter-panel">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>组卷条件</span>
              </div>
            </template>

            <el-form :model="paperForm" label-width="88px">
              <el-form-item label="组卷模式">
                <el-radio-group v-model="paperForm.mode">
                  <el-radio-button label="weakness">补弱卷</el-radio-button>
                  <el-radio-button label="classroom">课堂练习</el-radio-button>
                  <el-radio-button label="exam">阶段测验</el-radio-button>
                </el-radio-group>
                <p class="mode-help">{{ modeHelp }}</p>
              </el-form-item>

              <el-form-item label="课程">
                <el-select
                  v-model="paperForm.courseName"
                  clearable
                  filterable
                  style="width: 100%"
                  placeholder="选择课程"
                  no-data-text="当前暂无课程数据"
                  @change="handleCourseChange"
                >
                  <el-option v-for="item in courseOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>

              <el-form-item label="模块">
                <el-select
                  v-model="paperForm.chapterCode"
                  clearable
                  filterable
                  style="width: 100%"
                  placeholder="选择模块"
                  no-data-text="请先选择课程，或当前课程暂无模块数据"
                  @change="generatePaper"
                >
                  <el-option
                    v-for="item in chapterOptions"
                    :key="`${item.chapterCode}-${item.chapterName}`"
                    :label="item.chapterName"
                    :value="item.chapterCode"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="题型">
                <el-checkbox-group v-model="paperForm.questionTypes" @change="generatePaper">
                  <el-checkbox label="choice">选择题</el-checkbox>
                  <el-checkbox label="program">编程题</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item label="难度">
                <el-input value="已关闭难度筛选" disabled />
              </el-form-item>

              <el-form-item label="总分">
                <el-input-number v-model="paperForm.totalScore" :min="20" :max="150" :step="10" @change="generatePaper" />
              </el-form-item>

              <el-form-item label="题目数量">
                <el-input-number v-model="paperForm.questionCount" :min="5" :max="30" @change="generatePaper" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>模式效果</span>
              </div>
            </template>
            <div class="reason-list">
              <div class="reason-item">
                <strong>当前模式</strong>
                <span>{{ modeLabel }}</span>
              </div>
              <div class="reason-item">
                <strong>模式目标</strong>
                <span>{{ modeDescription }}</span>
              </div>
              <div class="reason-item">
                <strong>默认题量与分值</strong>
                <span>{{ paperForm.questionCount }} 题 / {{ paperForm.totalScore }} 分</span>
              </div>
              <div class="reason-item">
                <strong>难度处理</strong>
                <span>当前已关闭难度过滤，优先保证有题可组。</span>
              </div>
            </div>
          </el-card>

          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>推荐依据</span>
              </div>
            </template>
            <div class="reason-list">
              <div class="reason-item">
                <strong>当前课程</strong>
                <span>{{ paperForm.courseName || '未限定，将从真实题库综合筛选' }}</span>
              </div>
              <div class="reason-item">
                <strong>当前模块</strong>
                <span>{{ selectedChapterName || '未限定模块，将按课程全量筛选' }}</span>
              </div>
              <div class="reason-item">
                <strong>组卷目标</strong>
                <span>{{ modeSummary }}</span>
              </div>
              <div class="reason-item">
                <strong>出题配置</strong>
                <span>{{ questionTypeText }} / 不按难度筛选</span>
              </div>
            </div>
          </el-card>
        </aside>

        <section class="paper-panel">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="panel-header">
                <span>试卷草案</span>
                <el-tag effect="plain">{{ paperSections.length }} 题</el-tag>
              </div>
            </template>

            <div class="paper-overview">
              <div class="overview-item">
                <strong>{{ paperTitle }}</strong>
                <span>总分 {{ paperForm.totalScore }} 分</span>
              </div>
              <div class="overview-item">
                <strong>组卷说明</strong>
                <span>{{ modeSummary }}</span>
              </div>
              <div class="overview-item">
                <strong>出题配置</strong>
                <span>{{ questionTypeText }} / 不按难度筛选</span>
              </div>
            </div>

            <div v-if="paperSections.length" class="paper-list">
              <div v-for="item in paperSections" :key="item.no" class="paper-item">
                <div class="paper-item-head">
                  <div>
                    <strong>{{ item.no }}. {{ item.type }}</strong>
                    <p class="paper-subtitle">{{ item.subtitle }}</p>
                  </div>
                  <el-tag :type="item.tagType" effect="light">{{ item.score }} 分</el-tag>
                </div>
                <p :class="item.questionClass">{{ item.question }}</p>
                <div v-if="item.type === '选择题' && item.options.length" class="option-list">
                  <p v-for="option in item.options" :key="option.key" class="option-item">
                    <strong>{{ option.key }}.</strong> {{ option.text }}
                  </p>
                </div>
                <p :class="item.tipClass">{{ item.tip }}</p>
              </div>
            </div>

            <el-empty
              v-else
              description="当前条件下仍未命中题目，我已关闭难度过滤；如果这里还是为空，说明课程或模块本身没有对应题目。"
            />
          </el-card>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listTeacherPaperQuestions, listTeacherQuestionCatalogs } from '@/api/education/teacher'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const catalogList = ref([])
const paperSections = ref([])

const modePresets = {
  weakness: {
    label: '补弱卷',
    totalScore: 100,
    questionCount: 10,
    description: '优先服务于薄弱知识点补强，适合课后巩固和针对性提升。',
    summary: '适合课后补弱与专项训练。',
    help: '切换到补弱卷后，会默认回到 10 题、100 分，并重新生成更偏向查漏补缺的草案。'
  },
  classroom: {
    label: '课堂练习',
    totalScore: 60,
    questionCount: 8,
    description: '题量更短、更适合课堂快速练习和即时讲评。',
    summary: '适合课堂即时训练与讲评。',
    help: '切换到课堂练习后，会默认压缩题量，适合课堂 10 分钟左右完成。'
  },
  exam: {
    label: '阶段测验',
    totalScore: 100,
    questionCount: 15,
    description: '覆盖更完整，适合章节后或阶段性检测。',
    summary: '适合阶段性检测与能力评估。',
    help: '切换到阶段测验后，会默认增加题量，形成更完整的检测卷。'
  }
}

const paperForm = reactive({
  mode: 'weakness',
  courseName: '',
  chapterCode: '',
  totalScore: 100,
  questionCount: 10,
  questionTypes: ['choice', 'program']
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
  const source = catalogList.value.filter(item => !paperForm.courseName || item.courseName === paperForm.courseName)
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

const selectedChapterName = computed(() => {
  return chapterOptions.value.find(item => item.chapterCode === paperForm.chapterCode)?.chapterName || ''
})

const currentPreset = computed(() => modePresets[paperForm.mode] || modePresets.weakness)
const modeLabel = computed(() => currentPreset.value.label)
const modeDescription = computed(() => currentPreset.value.description)
const modeSummary = computed(() => currentPreset.value.summary)
const modeHelp = computed(() => currentPreset.value.help)

const contextMessage = computed(() => {
  return `当前为“${modeLabel.value}”模式，课程、模块现在直接读取真实目录数据，且已关闭难度过滤，避免把题筛空。`
})

const paperTitle = computed(() => {
  const course = paperForm.courseName || '课程题库'
  const chapter = selectedChapterName.value || '综合模块'
  return `${course} ${chapter} ${modeLabel.value}`
})

const questionTypeText = computed(() => {
  if (!paperForm.questionTypes.length) return '未选择题型'
  if (paperForm.questionTypes.length === 2) return '选择题 + 编程题'
  return paperForm.questionTypes[0] === 'program' ? '编程题' : '选择题'
})

function questionTypeLabel(type) {
  return type === 'program' ? '编程题' : '选择题'
}

function tagTypeByQuestion(item) {
  return item.questionType === 'program' ? 'warning' : 'primary'
}

function scoreFor(index, totalCount) {
  const base = Math.floor(paperForm.totalScore / Math.max(totalCount, 1))
  return index === totalCount - 1 ? paperForm.totalScore - base * (totalCount - 1) : base
}

function decodeHtmlEntities(text = '') {
  return String(text)
    .replace(/&nbsp;/gi, ' ')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/&apos;/gi, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&amp;/gi, '&')
}

function stripHtml(text = '') {
  return decodeHtmlEntities(String(text))
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/div>/gi, '\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<li[^>]*>/gi, '- ')
    .replace(/<pre[^>]*>/gi, '\n')
    .replace(/<\/pre>/gi, '\n')
    .replace(/<[^>]+>/g, ' ')
}

function normalizePlainText(text = '') {
  return stripHtml(text)
    .replace(/\r/g, '')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]{2,}/g, ' ')
    .replace(/^[ \t]+|[ \t]+$/gm, '')
    .trim()
}

function normalizeCodeText(text = '', maxLines = 18) {
  const cleaned = stripHtml(text)
    .replace(/\r/g, '')
    .replace(/\t/g, '  ')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]+$/gm, '')
    .trim()
  if (!cleaned) return ''
  return cleaned
    .split('\n')
    .map(line => line.trimEnd())
    .slice(0, maxLines)
    .join('\n')
}

function truncateText(text = '', maxLength = 260) {
  if (!text || text.length <= maxLength) return text
  return `${text.slice(0, maxLength).trim()}...`
}

function stripCodeBlocks(text = '') {
  return String(text)
    .replace(/<pre[\s\S]*?<\/pre>/gi, ' ')
    .replace(/```[\s\S]*?```/g, ' ')
}

function extractCodeSamples(item = {}) {
  const samples = []
  const answerCode = normalizeCodeText(item.standardAnswer || '', 40)
  const analysisCodeList = String(item.analysis || '').match(/<pre[\s\S]*?<\/pre>/gi) || []
  if (answerCode) samples.push(answerCode)
  analysisCodeList.forEach((block) => {
    const code = normalizeCodeText(block, 40)
    if (code) samples.push(code)
  })
  return samples.filter(Boolean)
}

function inferProgrammingLanguage(item = {}) {
  const explicit = normalizePlainText(item.language || '')
  if (explicit && !/未知|通用/.test(explicit)) return explicit

  const source = [item.questionStem, item.analysis, item.standardAnswer, item.knowledgePoint]
    .map(value => String(value || ''))
    .join('\n')
    .toLowerCase()

  if (/brush:c#|using\s+system|console\.writeline|convert\.toint|int\.parse/.test(source)) return 'C#'
  if (/brush:java|public\s+class|public\s+static\s+void\s+main|system\.out\.println/.test(source)) return 'Java'
  if (/brush:python|def\s+\w+\(|print\(|input\(|import\s+\w+/.test(source)) return 'Python'
  if (/brush:cpp|#include\s*<iostream>|std::|cout\s*<</.test(source)) return 'C++'
  if (/brush:c\b|#include\s*<stdio\.h>|scanf\(|printf\(|main\(\)/.test(source)) return 'C'
  if (/vector<|stringstream|using\s+namespace\s+std/.test(source)) return 'C++'
  return 'C/C++'
}

function cleanProgramDescription(text = '') {
  return normalizePlainText(stripCodeBlocks(text))
    .replace(/^解析[:：]?\s*/i, '')
    .replace(/^题意[:：]?\s*/i, '')
}

function buildSubtitle(item = {}, isProgram = false) {
  const chapterName = item.chapterName || selectedChapterName.value || '综合模块'
  if (isProgram) {
    return `${chapterName} / ${inferProgrammingLanguage(item)}`
  }
  const knowledgePoint = normalizePlainText(item.knowledgePoint || item.pointName || '通用知识点')
  return `${chapterName} / ${knowledgePoint}`
}

function buildProgramQuestionText(item = {}) {
  const title = normalizePlainText(item.questionStem || item.question || '')
  const knowledgePoint = normalizePlainText(item.knowledgePoint || item.pointName || '')
  const sections = [`编程任务：${title || '请根据题意完成程序编写。'}`]
  if (knowledgePoint) {
    sections.push(`涉及知识点：${knowledgePoint}`)
  }
  sections.push('请关注输入输出格式、边界情况和运行结果，按题意完成代码实现。')
  return sections.join('\n')
}

function buildProgramTipText(item = {}) {
  const analysisText = truncateText(normalizePlainText(item.analysis || item.recommendReason || ''), 320)
  if (analysisText) return analysisText
  return '题库未提供完整解析，当前已优先展示题目标题、知识点与参考代码片段，避免原样显示脏富文本内容。'
}

function scoreProgramQuality(item = {}) {
  const title = normalizePlainText(item.questionStem || item.question || '')
  const analysisText = normalizePlainText(item.analysis || '')
  const answerText = normalizeCodeText(item.standardAnswer || '', 24)
  const knowledgePoint = normalizePlainText(item.knowledgePoint || '')
  let score = 0
  score += Math.min(title.length, 60)
  score += Math.min(analysisText.length, 80)
  score += Math.min(answerText.length, 120)
  score += Math.min(knowledgePoint.length * 6, 30)
  if (title.length < 6) score -= 60
  if (!analysisText) score -= 20
  if (!answerText) score -= 20
  return score
}

function buildProgramQuestionTextEnhanced(item = {}) {
  const title = normalizePlainText(item.questionStem || item.question || '') || '请根据题意完成程序编写。'
  const knowledgePoint = normalizePlainText(item.knowledgePoint || item.pointName || '')
  const language = inferProgrammingLanguage(item)
  const description = cleanProgramDescription(item.analysis || '')
  const sections = [`编程任务：${title}`]
  if (description && description.length >= 18) {
    sections.push(`题目介绍：${truncateText(description, 520)}`)
  } else {
    sections.push(`题目介绍：请使用 ${language} 完成本题，结合题目标题设计输入、处理逻辑和输出结果，重点关注边界情况与格式要求。`)
  }
  if (knowledgePoint && !/未知语言/i.test(knowledgePoint)) {
    sections.push(`涉及知识点：${knowledgePoint}`)
  }
  sections.push(`建议语言：${language}`)
  sections.push('请输出完整程序，并确保结果符合题意要求。')
  return sections.join('\n')
}

function buildProgramTipTextEnhanced(item = {}) {
  const description = cleanProgramDescription(item.analysis || item.recommendReason || '')
  if (description && description.length >= 18) {
    return truncateText(description, 240)
  }
  return '当前题库未提供独立解析文本，已根据题目标题、代码特征和语言线索自动整理为可读题面。'
}

function scoreProgramQualityEnhanced(item = {}) {
  const title = normalizePlainText(item.questionStem || item.question || '')
  const description = cleanProgramDescription(item.analysis || '')
  const codeSamples = extractCodeSamples(item).join('\n')
  const knowledgePoint = normalizePlainText(item.knowledgePoint || '')
  let score = 0
  score += Math.min(title.length, 60)
  score += Math.min(description.length, 120)
  score += Math.min(codeSamples.length, 80)
  score += Math.min(knowledgePoint.length * 4, 24)
  if (title.length < 6) score -= 60
  if (!description) score -= 10
  if (!codeSamples) score -= 10
  return score
}

function normalizeQuestion(item = {}) {
  const questionType = item.questionType || 'choice'
  const isProgram = questionType === 'program'
  const question = isProgram
    ? buildProgramQuestionTextEnhanced(item)
    : truncateText(normalizePlainText(item.questionStem || item.question || '请根据当前模块补充题目内容。'), 420)
  const tip = isProgram
    ? buildProgramTipTextEnhanced(item)
    : truncateText(normalizePlainText(item.analysis || item.recommendReason || '命题依据：来自真实题库与当前筛选条件。'), 240)
  return {
    ...item,
    questionType,
    chapterName: item.chapterName || selectedChapterName.value || '综合模块',
    knowledgePoint: normalizePlainText(item.knowledgePoint || item.pointName || '通用知识点'),
    question,
    tip,
    options: parseOptions(item.optionsJson),
    questionClass: isProgram ? 'paper-question program-question' : 'paper-question',
    tipClass: isProgram ? 'paper-tip program-tip' : 'paper-tip',
    qualityScore: isProgram ? scoreProgramQualityEnhanced(item) : 0,
    subtitle: buildSubtitle(item, isProgram),
    languageLabel: isProgram ? inferProgrammingLanguage(item) : ''
  }
}

function parseOptions(raw) {
  if (!raw) return []
  let parsed = raw
  if (typeof raw === 'string') {
    try {
      parsed = JSON.parse(raw)
    } catch (error) {
      return raw
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)
        .map((line, index) => ({
          key: String.fromCharCode(65 + index),
          text: normalizePlainText(line.replace(/^[A-ZＡ-Ｚ][.、:：)\s-]*/i, '').trim())
        }))
    }
  }

  if (Array.isArray(parsed)) {
    return parsed
      .map((item, index) => {
        if (typeof item === 'string') {
          return {
            key: String.fromCharCode(65 + index),
            text: normalizePlainText(item)
          }
        }
        return {
          key: item.key || item.label || String.fromCharCode(65 + index),
          text: normalizePlainText(item.text || item.content || item.value || '')
        }
      })
      .filter((item) => item.text)
  }

  if (typeof parsed === 'object') {
    return Object.keys(parsed)
      .sort()
      .map((key) => ({
        key,
        text: normalizePlainText(parsed[key])
      }))
      .filter((item) => item.text)
  }

  return []
}

function sortPoolByMode(pool) {
  const normalized = pool.map(normalizeQuestion)
  const compareProgramQuality = (a, b) => {
    if (a.questionType === 'program' && b.questionType === 'program' && a.qualityScore !== b.qualityScore) {
      return b.qualityScore - a.qualityScore
    }
    return 0
  }
  if (paperForm.mode === 'classroom') {
    return normalized.sort((a, b) => {
      const qualityResult = compareProgramQuality(a, b)
      if (qualityResult !== 0) return qualityResult
      return String(a.questionType).localeCompare(String(b.questionType), 'zh-CN')
    })
  }
  if (paperForm.mode === 'exam') {
    return normalized.sort((a, b) => {
      const qualityResult = compareProgramQuality(a, b)
      if (qualityResult !== 0) return qualityResult
      return String(a.chapterName).localeCompare(String(b.chapterName), 'zh-CN')
    })
  }
  return normalized.sort((a, b) => {
    const qualityResult = compareProgramQuality(a, b)
    if (qualityResult !== 0) return qualityResult
    const ac = a.chapterName === selectedChapterName.value ? -1 : 0
    const bc = b.chapterName === selectedChapterName.value ? -1 : 0
    if (ac !== bc) return ac - bc
    return String(a.questionType).localeCompare(String(b.questionType), 'zh-CN')
  })
}

function applyModePreset(mode) {
  const preset = modePresets[mode] || modePresets.weakness
  paperForm.totalScore = preset.totalScore
  paperForm.questionCount = preset.questionCount
}

async function loadCatalogs() {
  const res = await listTeacherQuestionCatalogs({
    courseName: paperForm.courseName || undefined
  })
  catalogList.value = normalizeListResponse(res).map(normalizeCatalogItem).filter(Boolean)
  if (paperForm.chapterCode && !chapterOptions.value.some(item => item.chapterCode === paperForm.chapterCode || item.chapterName === paperForm.chapterCode)) {
    paperForm.chapterCode = ''
  }
}

async function handleCourseChange() {
  paperForm.chapterCode = ''
  await loadCatalogs()
  await generatePaper()
}

async function requestQuestions(params) {
  const res = await listTeacherPaperQuestions(params)
  return normalizeListResponse(res)
}

async function generatePaper() {
  if (!paperForm.questionTypes.length) {
    ElMessage.warning('请至少选择一种题型')
    paperSections.value = []
    return
  }

  loading.value = true
  try {
    const chapter = chapterOptions.value.find(item => item.chapterCode === paperForm.chapterCode)
    const limit = Math.max(paperForm.questionCount * 3, 18)

    const exactPool = (
      await Promise.all(
        paperForm.questionTypes.map((type) =>
          requestQuestions({
            courseName: paperForm.courseName || undefined,
            chapterCode: paperForm.chapterCode || undefined,
            chapterName: chapter?.chapterName || undefined,
            questionType: type,
            limit
          })
        )
      )
    ).flat()

    const coursePool = exactPool.length
      ? exactPool
      : (
          await Promise.all(
            paperForm.questionTypes.map((type) =>
              requestQuestions({
                courseName: paperForm.courseName || undefined,
                questionType: type,
                limit
              })
            )
          )
        ).flat()

    const globalPool = coursePool.length
      ? coursePool
      : (
          await Promise.all(
            paperForm.questionTypes.map((type) =>
              requestQuestions({
                questionType: type,
                limit
              })
            )
          )
        ).flat()

    const sortedPool = sortPoolByMode(globalPool)
    paperSections.value = sortedPool.slice(0, paperForm.questionCount).map((item, index, list) => ({
      no: index + 1,
      type: questionTypeLabel(item.questionType),
      chapterName: item.chapterName,
      knowledgePoint: item.knowledgePoint,
      score: scoreFor(index, list.length),
      tagType: tagTypeByQuestion(item),
      question: item.question,
      tip: item.tip,
      subtitle: item.subtitle,
      questionClass: item.questionClass,
      tipClass: item.tipClass,
      options: item.options || []
    }))
  } catch (error) {
    ElMessage.error('生成组卷草案失败，请稍后重试')
  } finally {
    loading.value = false
  }
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

function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function buildPaperExportText(includeAnswers = false) {
  const header = [
    '智能题库组卷导出',
    `课程：${paperForm.courseName || '未限定'}`,
    `模块：${selectedChapterName.value || '未限定'}`,
    `组卷模式：${modeLabel.value}`,
    `题型：${questionTypeText.value}`,
    `题目数量：${paperSections.value.length}`,
    `总分：${paperForm.totalScore}`,
    ''
  ]

  const body = paperSections.value.map((item) => {
    const lines = [
      `${item.no}. ${item.type}`,
      `模块：${item.subtitle || `${item.chapterName} / ${item.knowledgePoint}`}`,
      `题目：${item.question}`
    ]
    if (item.options?.length) {
      item.options.forEach((option) => lines.push(`${option.key} ${option.text}`))
    }
    if (includeAnswers) {
      lines.push(`解析：${item.tip || '暂无解析'}`)
    }
    return lines.join('\n')
  })

  return header.concat(body.join('\n\n')).join('\n')
}

function buildPrintHtml() {
  const itemsHtml = paperSections.value.map((item) => {
    const optionsHtml = item.options?.length
      ? `<div class="options">${item.options.map((option) => `<p><strong>${escapeHtml(option.key)}</strong> ${escapeHtml(option.text)}</p>`).join('')}</div>`
      : ''
    return `
      <section class="item">
        <h3>${item.no}. ${escapeHtml(item.type)}</h3>
        <p class="meta">${escapeHtml(item.subtitle || `${item.chapterName} / ${item.knowledgePoint}`)}</p>
        <p class="question">${escapeHtml(item.question).replace(/\n/g, '<br/>')}</p>
        ${optionsHtml}
        <p class="analysis"><strong>解析：</strong>${escapeHtml(item.tip || '暂无解析')}</p>
      </section>
    `
  }).join('')

  return `
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
      <meta charset="UTF-8" />
      <title>智能题库组卷导出</title>
      <style>
        body { font-family: "Microsoft YaHei", sans-serif; padding: 24px; color: #0f172a; }
        h1 { margin-bottom: 8px; }
        .summary { margin-bottom: 24px; color: #475569; }
        .item { border: 1px solid #cbd5e1; border-radius: 12px; padding: 16px; margin-bottom: 16px; page-break-inside: avoid; }
        .meta { color: #64748b; }
        .options p, .analysis { margin: 6px 0; }
        .question { line-height: 1.8; }
      </style>
    </head>
    <body>
      <h1>${escapeHtml(paperTitle.value)}</h1>
      <p class="summary">组卷模式：${escapeHtml(modeLabel.value)}，题型：${escapeHtml(questionTypeText.value)}，总分：${escapeHtml(paperForm.totalScore)} 分</p>
      ${itemsHtml}
    </body>
    </html>
  `
}

function exportQuestionSet() {
  if (!paperSections.value.length) {
    ElMessage.warning('当前没有可导出的题目')
    return
  }
  downloadTextFile('智能题库组卷-题目.txt', buildPaperExportText(false))
}

function exportAnswerSet() {
  if (!paperSections.value.length) {
    ElMessage.warning('当前没有可导出的答案解析')
    return
  }
  downloadTextFile('智能题库组卷-答案解析.txt', buildPaperExportText(true))
}

function exportPdfSet() {
  if (!paperSections.value.length) {
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

watch(
  () => paperForm.mode,
  async (mode) => {
    applyModePreset(mode)
    await generatePaper()
  }
)

onMounted(async () => {
  if (route.query.courseName) paperForm.courseName = String(route.query.courseName)
  if (route.query.chapterCode) paperForm.chapterCode = String(route.query.chapterCode)

  applyModePreset(paperForm.mode)
  await loadCatalogs()
  await generatePaper()
})
</script>

<style scoped lang="scss">
.teacher-paper-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 284px 1fr;
  gap: 18px;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.15), transparent 26%),
    linear-gradient(180deg, #f8fcf8 0%, #eef6ff 100%);
}

.side-nav,
.hero-card,
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

.nav-item span,
.hero-text,
.reason-item span,
.paper-subtitle,
.paper-tip {
  color: #64748b;
  line-height: 1.7;
}

.nav-item span {
  font-size: 17px;
}

.mode-help {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.nav-item.active {
  border-color: rgba(34, 197, 94, 0.5);
  background: rgba(240, 253, 244, 0.95);
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
  padding: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #15803d;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: 32px;
}

.hero-tags,
.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.content-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
}

.filter-panel,
.paper-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #0f172a;
  font-weight: 700;
}

.reason-list,
.paper-list {
  display: grid;
  gap: 14px;
}

.reason-item strong,
.paper-item-head strong,
.paper-overview strong {
  display: block;
  color: #0f172a;
}

.paper-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-item,
.paper-item {
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.paper-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.paper-question {
  margin: 12px 0 0;
  color: #0f172a;
  line-height: 1.8;
}

.program-question,
.program-tip {
  white-space: pre-line;
}

.option-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.92);
}

.option-item {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.option-item strong {
  color: #0f172a;
}

.program-answer {
  margin: 12px 0 0;
  padding: 14px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
}

@media (max-width: 1200px) {
  .teacher-paper-page,
  .content-grid,
  .paper-overview {
    grid-template-columns: 1fr;
  }
}
</style>
