<template>
  <div class="rag-shell">
    <aside class="kb-sidebar">
      <div class="session-header">
        <div>
          <h2>历史对话</h2>
          <p>保留学生与教师的上下文追问记录</p>
        </div>
        <el-button size="small" type="primary" plain @click="startNewConversation">新建对话</el-button>
      </div>

      <el-scrollbar class="session-list">
        <div
          v-for="item in conversations"
          :key="item.id"
          :class="['session-item', { active: item.id === activeConversationId }]"
          @click="setActiveConversation(item.id)"
        >
          <div class="session-main">
            <div class="session-title" :title="item.title">{{ item.title }}</div>
            <div class="session-time">{{ formatTime(item.updatedAt) }}</div>
          </div>
          <el-button
            text
            size="small"
            type="danger"
            class="session-delete"
            @click.stop="deleteConversation(item.id)"
          >
            删除
          </el-button>
        </div>
      </el-scrollbar>

      <div class="kb-header">
        <div>
          <h2>知识库</h2>
          <p>上传课程资料后，系统会基于 RAG 检索增强回答</p>
        </div>
        <el-button text :loading="loadingHistory" @click="refreshHistory">刷新</el-button>
      </div>

      <el-scrollbar class="kb-list">
        <div v-if="historyDatasets.length === 0" class="kb-empty">
          <el-empty description="暂无知识库数据" :image-size="72" />
        </div>

        <div
          v-for="(dataset, index) in historyDatasets"
          :key="datasetKey(dataset, index)"
          class="kb-item"
        >
          <div class="kb-item-main">
            <div class="kb-name" :title="datasetName(dataset)">{{ datasetName(dataset) }}</div>
            <div class="kb-time">{{ formatTime(datasetTime(dataset)) }}</div>
          </div>
          <div class="kb-actions">
            <el-button text size="small" @click="viewDataset(dataset)">查看</el-button>
            <el-button text size="small" type="danger" @click="handleDeleteDataset(dataset)">删除</el-button>
          </div>
        </div>
      </el-scrollbar>
    </aside>

    <section class="chat-panel">
      <header class="chat-topbar">
        <div class="chat-title-wrap">
          <h1>AI 智能问答助手</h1>
        </div>

        <div class="topbar-actions">
          <div class="user-badge">
            <strong>{{ currentIdentity.displayName }}</strong>
            <span>{{ currentIdentity.role }} / {{ currentIdentity.account }}</span>
          </div>

          <div class="upload-inline">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              accept=".xlsx,.xls,.txt"
              multiple
              :limit="5"
            >
              <el-button size="small" plain>选择资料</el-button>
            </el-upload>
            <span class="selected-files">{{ selectedFilesText }}</span>
            <el-button
              size="small"
              type="success"
              plain
              :loading="sceneImportLoading"
              @click="handleImportCurrentScene"
            >
              导入前序数据到知识库
            </el-button>
            <el-button
              size="small"
              type="primary"
              :loading="uploadLoading"
              :disabled="!fileList.length || uploadLoading"
              @click="submitUpload"
            >
              上传并入库
            </el-button>
            <el-button size="small" plain @click="router.push(backHomePath)">
              返回主页
            </el-button>
          </div>
        </div>
      </header>

      <div v-if="isTeacherView" class="teacher-scene-toolbar">
        <div class="teacher-scene-grid">
          <el-select
            v-model="teacherScene.courseName"
            filterable
            clearable
            placeholder="选择课程"
            @change="handleTeacherCourseChange"
          >
            <el-option v-for="item in teacherCourseOptions" :key="item" :label="item" :value="item" />
          </el-select>

          <el-select
            v-model="teacherScene.chapterCode"
            filterable
            clearable
            :disabled="!teacherScene.courseName"
            placeholder="选择模块"
            @change="handleTeacherChapterChange"
          >
            <el-option
              v-for="item in teacherChapterOptions"
              :key="`${item.chapterCode}-${item.chapterName}`"
              :label="item.chapterName"
              :value="item.chapterCode"
            />
          </el-select>
        </div>

        <div class="teacher-scene-actions">
          <el-button type="primary" plain @click="applyTeacherScene">应用当前教师场景</el-button>
          <el-button plain @click="resetTeacherScene">重置</el-button>
        </div>
      </div>

      <div v-if="sceneHint.title" :class="['scene-context-card', { 'is-expanded': sceneContextExpanded }]">
        <button class="scene-context-summary" type="button" @click="sceneContextExpanded = !sceneContextExpanded">
          <span class="scene-toggle-arrow">{{ sceneContextExpanded ? '⌃' : '⌄' }}</span>
          <span class="scene-context-main">
            <strong>{{ sceneHint.title }}</strong>
            <span>{{ sceneHint.description }}</span>
          </span>
          <span class="scene-context-status">
            <el-tag type="success" effect="light">已携带上下文</el-tag>
            <span v-if="!isTeacherView && studentRagProfile?.latestPrediction?.predictedScore" class="scene-context-score">
              最新预测 {{ studentRagProfile.latestPrediction.predictedScore }} 分
            </span>
          </span>
        </button>

        <div v-if="sceneContextExpanded" class="scene-context-detail">
          <div class="scene-context-copy">
            <strong>{{ isTeacherView ? '教师分析上下文' : '当前分析将优先结合本场景数据' }}</strong>
            <p>{{ isTeacherView ? teacherProfileTip : (studentRagProfile?.predictionTip || '会结合刷题诊断、薄弱章节、错题记录和编程表现进行分析。') }}</p>
            <div class="scene-analysis-tags">
              <template v-if="!isTeacherView">
                <el-tag type="success" effect="light">场景已带入</el-tag>
                <el-tag v-if="studentRagProfile?.predictionReady" type="success" effect="light">最新预测已接入</el-tag>
                <el-tag v-else type="warning" effect="light">请先做成绩预测</el-tag>
                <el-tag effect="plain">刷题诊断</el-tag>
                <el-tag effect="plain">薄弱章节</el-tag>
                <el-tag effect="plain">错题记录</el-tag>
                <el-tag effect="plain">编程表现</el-tag>
              </template>
              <template v-else>
                <el-tag type="success" effect="light">教师分析模式</el-tag>
                <el-tag v-if="teacherRagProfile?.studentScoped" type="warning" effect="light">学生定向画像</el-tag>
                <el-tag v-else effect="plain">班级整体画像</el-tag>
                <el-tag effect="plain">模块表现</el-tag>
                <el-tag effect="plain">错题热点</el-tag>
                <el-tag effect="plain">教学建议</el-tag>
              </template>
            </div>
            <div class="scene-analysis-actions scene-analysis-actions-inline">
              <el-button size="small" type="primary" plain @click="applyPrompt(sceneHint.prompt)">带入当前场景</el-button>
              <el-button size="small" type="primary" :loading="sceneImportLoading" @click="handleImportCurrentScene">带入当前场景到知识库</el-button>
            </div>
          </div>
          <div v-if="!isTeacherView && studentRagProfile?.latestPrediction?.predictedScore" class="scene-analysis-side">
            <span>最新预测</span>
            <strong>{{ studentRagProfile.latestPrediction.predictedScore }} 分</strong>
            <small>{{ studentRagProfile.latestPrediction.createTime }}</small>
          </div>
        </div>
      </div>

      <div v-if="isTeacherView" class="profile-strip">
        <div class="profile-card teacher-profile-card">
          <div class="profile-head">
            <strong>教师综合分析画像</strong>
            <el-tag v-if="teacherRagProfile?.studentScoped" type="warning" effect="light">当前聚焦单个学生</el-tag>
            <el-tag v-else type="success" effect="light">当前聚焦班级整体</el-tag>
          </div>
          <p class="profile-tip">{{ teacherProfileTip }}</p>
          <div class="profile-tags">
            <el-tag effect="plain">班级学情</el-tag>
            <el-tag effect="plain">学生画像</el-tag>
            <el-tag effect="plain">模块表现</el-tag>
            <el-tag effect="plain">高频错题</el-tag>
            <el-tag effect="plain">教学策略</el-tag>
          </div>
          <div class="profile-meta">
            <span v-if="teacherScene.studentName">当前学生：{{ teacherScene.studentName }}</span>
            <span v-else>当前学生：未指定，默认按班级整体分析</span>
            <span v-if="teacherScene.courseName">课程：{{ teacherScene.courseName }}</span>
            <span v-if="teacherScene.chapterName">模块：{{ teacherScene.chapterName }}</span>
          </div>
        </div>
      </div>

      <div v-if="!isTeacherView" class="profile-strip">
        <div class="profile-card">
          <div class="profile-head">
            <strong>综合分析画像</strong>
            <el-tag v-if="studentRagProfile?.predictionReady" type="success" effect="light">已接入最新预测</el-tag>
            <el-tag v-else type="warning" effect="light">缺少最新预测</el-tag>
          </div>
          <p class="profile-tip">
            {{ studentRagProfile?.predictionTip || 'RAG 会自动结合成绩预测、刷题、错题与编程题数据进行分析。' }}
          </p>
          <div class="profile-tags">
            <el-tag effect="plain">刷题诊断</el-tag>
            <el-tag effect="plain">薄弱章节</el-tag>
            <el-tag effect="plain">错题记录</el-tag>
            <el-tag effect="plain">编程表现</el-tag>
            <el-tag effect="plain">考试成绩</el-tag>
          </div>
          <div v-if="studentRagProfile?.latestPrediction?.predictedScore" class="profile-meta">
            最新预测：{{ studentRagProfile.latestPrediction.predictedScore }} 分
            <span>{{ studentRagProfile.latestPrediction.createTime }}</span>
          </div>
          <div v-else class="profile-meta warning-text">
            还没有最新预测结果，提问学情分析时会先提醒去做一次成绩预测。
          </div>
        </div>
      </div>

      <section v-if="!isTeacherView" class="student-ai-context">
        <div class="context-metrics">
          <div
            v-for="item in studentContextCards"
            :key="item.label"
            class="context-metric"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.tip }}</small>
          </div>
        </div>
        <div class="context-questions">
          <div class="context-question-head">
            <strong>可以这样问</strong>
            <span>{{ studentRagProfile?.predictionTip || '会结合预测、刷题和薄弱模块回答。' }}</span>
          </div>
          <div class="context-module-switch">
            <span>分析模块</span>
            <el-select
              :model-value="selectedStudentChapterKey"
              clearable
              filterable
              placeholder="自动识别薄弱模块"
              @change="handleStudentChapterChange"
            >
              <el-option label="自动识别薄弱模块" value="" />
              <el-option
                v-for="item in studentChapterOptions"
                :key="item.key"
                :label="item.label"
                :value="item.key"
              />
            </el-select>
          </div>
          <div class="context-question-list">
            <button
              v-for="item in studentQuickQuestions"
              :key="item"
              type="button"
              @click="applyPrompt(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>
      </section>

      <main class="chat-stream" ref="chatContainer">
        <div v-if="messages.length === 0" class="chat-empty">
          <el-empty description="从一个真实教学问题开始提问" :image-size="90" />

          <div class="prompt-grid">
            <button
              v-for="item in promptCards"
              :key="item.title"
              class="prompt-card"
              @click="applyPrompt(item.prompt)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
            </button>
          </div>

          <div class="example-strip">
            <span>示例问题：</span>
            <el-tag
              v-for="example in exampleQuestions"
              :key="example"
              effect="plain"
              class="example-tag"
              @click="applyPrompt(example)"
            >
              {{ example }}
            </el-tag>
          </div>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['chat-row', message.type === 'user' ? 'is-user' : 'is-ai']"
        >
          <div class="chat-bubble" v-html="formatMessageContent(message)"></div>
        </div>

        <div v-if="queryLoading" class="chat-row is-ai">
          <div class="chat-bubble loading-bubble">
            <el-skeleton :rows="2" animated />
          </div>
        </div>

        <div ref="latestMessageAnchor" class="latest-message-anchor"></div>
      </main>

      <footer class="composer">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请输入你的问题，Enter 发送，Shift + Enter 换行。"
          :disabled="queryLoading || uploadLoading"
          @keydown.enter.exact.prevent="submitQuery"
        />

        <div class="composer-actions">
          <div class="composer-buttons">
            <el-button v-if="queryLoading" type="danger" plain @click="abortQuery">停止</el-button>
            <el-button
              type="primary"
              :loading="queryLoading"
              :disabled="!question.trim() || queryLoading || uploadLoading"
              @click="submitQuery"
            >
              发送问题
            </el-button>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDataset, getDatasetDetail, getDatasets, importCurrentSceneToKnowledge, queryQuestion, uploadExcelFiles } from '@/api/education/rag'
import { getStudentDiagnosisChapters, getStudentRagProfile } from '@/api/education/student'
import { getTeacherRagProfile, getTeacherStudentCatalogs } from '@/api/education/teacher'
import useUserStore from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const historyDatasets = ref([])
const loadingHistory = ref(false)
const fileList = ref([])
const uploadLoading = ref(false)

const question = ref('')
const messages = ref([])
const queryLoading = ref(false)
const chatContainer = ref(null)
const latestMessageAnchor = ref(null)
let queryAborted = false

const conversations = ref([])
const activeConversationId = ref('')
const studentRagProfile = ref(null)
const teacherRagProfile = ref(null)
const ragProfileLoading = ref(false)
const sceneImportLoading = ref(false)
const sceneContextExpanded = ref(false)
const teacherCatalogList = ref([])
const studentChapterCatalog = ref([])
const teacherScene = ref({
  studentNo: '',
  studentName: '',
  courseName: '',
  chapterCode: '',
  chapterName: ''
})

const MAX_CONVERSATIONS = 30

function looksLikeMojibake(text) {
  const value = String(text || '').trim()
  if (!value) return false
  return /[鍀-鿿]/.test(value) || /Ã|Â|�/.test(value)
}

function safeIdentityText(primary, fallback = '--') {
  const value = String(primary || '').trim()
  if (!value || looksLikeMojibake(value)) {
    return String(fallback || '--').trim() || '--'
  }
  return value
}

const historyStorageKey = computed(() => {
  const roleKey = Array.isArray(userStore.roles) && userStore.roles.includes('teacher') ? 'teacher' : 'student'
  const userKey = userStore.name || 'anonymous'
  return `education_rag_chat_history_v2_${roleKey}_${userKey}`
})

const currentIdentity = computed(() => ({
  account: safeIdentityText(userStore.name, '--'),
  displayName: safeIdentityText(userStore.nickName, userStore.name || '--'),
  role: Array.isArray(userStore.roles) && userStore.roles.includes('teacher') ? '教师' : '学生'
}))
const isTeacherView = computed(() => Array.isArray(userStore.roles) && userStore.roles.includes('teacher'))
const backHomePath = computed(() => (isTeacherView.value ? '/education/teacher/pad' : '/education/student/pad'))

const sceneHint = computed(() => {
  const studentName = String(route.query.studentName || '').trim()
  const courseName = String(route.query.courseName || '').trim()
  const chapterName = String(route.query.chapterName || '').trim()
  const source = String(route.query.source || '').trim()
  const questionText = String(route.query.question || '').trim()

  if (!studentName && !courseName && !chapterName && !questionText && !source) {
    return { title: '', description: '', prompt: '' }
  }

  const titleMap = {
    'teacher-analysis': '教师学情分析场景',
    'teacher-students': '教师学生画像场景',
    'teacher-paper': '教师智能组卷场景',
    'teacher-rag': '教师综合分析场景',
    'student-report': '学生学业诊断场景',
    'student-practice': '学生错题训练场景',
    'student-history': '学生历史做题场景',
    'student-question-ask': '学生题目追问场景'
  }
  const title = titleMap[source] || '课程智能问答场景'
  const sceneText = [studentName, courseName, chapterName].filter(Boolean).join(' / ') || '当前学习内容'
  const prompt =
    questionText ||
    `请结合${sceneText}，先给出结论，再说明依据，最后给出下一步最值得执行的学习或教学动作。`

  return {
    title,
    description: `当前已携带场景上下文：${sceneText}。你可以直接追问原因、建议、练习安排或教学策略。`,
    prompt
  }
})

const promptCards = computed(() => {
  const courseName = String(route.query.courseName || '').trim()
  const chapterName = String(route.query.chapterName || '').trim()
  const scene = [courseName, chapterName].filter(Boolean).join(' / ') || '当前内容'
  return [
    {
      title: '学生学习建议',
      description: '生成可执行的补强顺序和今日任务',
      prompt: `请基于${scene}，给我一份 3 天学习计划，每天列出重点任务、推荐练习量和预期目标。`
    },
    {
      title: '错题原因分析',
      description: '解释为什么会错，以及怎么纠正',
      prompt: `请结合${scene}，分析学生在这一部分最容易出现的错误，并给出针对性的纠正建议。`
    },
    {
      title: '教师讲评提纲',
      description: '围绕薄弱章节生成课堂讲评逻辑',
      prompt: `请围绕${scene}生成一份课堂讲评提纲，包括重点讲解、常见错误和课后建议。`
    },
    {
      title: '针对性练习建议',
      description: '把问答结果转化为下一步练习安排',
      prompt: `请基于${scene}推荐一组针对性练习，并说明每类练习要解决什么问题。`
    }
  ]
})

const exampleQuestions = computed(() => {
  const chapterName = String(route.query.chapterName || '').trim()
  const chapterPart = chapterName ? `${chapterName}这一章` : '这部分内容'
  return [
    `我为什么总在${chapterPart}出错？`,
    '请根据我当前情况给出一份今天的学习任务。',
    '请帮我生成一段教师课堂讲评提纲。'
  ]
})

const studentContextCards = computed(() => {
  const profile = studentRagProfile.value || {}
  const prediction = profile.latestPrediction || {}
  const overview = profile.diagnosisOverview || {}
  const predictionScore = prediction.predictedScore ? `${prediction.predictedScore} 分` : '--'
  const predictionTip = prediction.createTime ? `预测时间 ${prediction.createTime}` : '可先完成一次成绩预测'
  const analysis = currentStudentAnalysisChapter.value
  const answerCount = overview.answerCount || '--'
  const recentCount = overview.recentAnswerCount || 0
  const rate = overview.correctRate ? `${overview.correctRate}%` : '--'

  return [
    { label: '最新预测成绩', value: predictionScore, tip: predictionTip },
    { label: '当前分析模块', value: analysis.value || '--', tip: analysis.tip },
    { label: '历史作答轨迹', value: `${answerCount} 题`, tip: `累计正确率 ${rate}，近7天 ${recentCount} 题` }
  ]
})

const studentQuickQuestions = computed(() => {
  const profile = studentRagProfile.value || {}
  const weakest = profile.weakestChapter || {}
  const routeChapter = String(route.query.chapterName || '').trim()
  const chapterName = String(currentStudentAnalysisChapter.value.chapterName || weakest.chapterName || routeChapter || '当前模块').trim()
  const prediction = profile.latestPrediction || {}
  const scoreText = prediction.predictedScore ? `我目前预测成绩是 ${prediction.predictedScore} 分，` : ''
  return [
    `${scoreText}请给我一份今天的学习任务。`,
    `我在${chapterName}哪里最容易失分？`,
    `请基于${chapterName}推荐一组针对性练习。`
  ]
})

const selectedFilesText = computed(() => {
  if (!fileList.value.length) return '尚未选择资料'
  if (fileList.value.length === 1) return fileList.value[0].name
  return `已选择 ${fileList.value.length} 个文件`
})

const teacherCourseOptions = computed(() => {
  const values = Array.from(new Set((teacherCatalogList.value || []).map((item) => item.courseName).filter(Boolean)))
  return values.sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const studentChapterOptions = computed(() => {
  const source = studentChapterCatalog.value.length
    ? studentChapterCatalog.value
    : (studentRagProfile.value?.chapterDiagnosis || [])
  const map = new Map()
  source.forEach((item) => {
    const normalized = normalizeChapterOption(item)
    if (normalized && !map.has(normalized.key)) {
      map.set(normalized.key, normalized)
    }
  })
  return Array.from(map.values())
})

const selectedStudentChapterKey = computed(() => {
  const chapterCode = String(route.query.chapterCode || '').trim()
  const chapterName = String(route.query.chapterName || '').trim()
  if (!chapterCode && !chapterName) return ''
  return buildChapterKey(chapterCode, chapterName)
})

const currentStudentAnalysisChapter = computed(() => {
  const profile = studentRagProfile.value || {}
  const weakest = profile.weakestChapter || {}
  const routeCourse = String(route.query.courseName || '').trim()
  const routeChapter = String(route.query.chapterName || '').trim()
  const routeChapterCode = String(route.query.chapterCode || '').trim()
  const hasRouteTarget = Boolean(routeCourse || routeChapter || routeChapterCode)
  if (hasRouteTarget) {
    const value = profile.analysisTarget || [routeCourse, routeChapter || routeChapterCode].filter(Boolean).join(' / ')
    return {
      value: value || '--',
      chapterName: routeChapter || routeChapterCode,
      tip: '已按当前选择模块分析'
    }
  }
  const value = [weakest.courseName, weakest.chapterName].filter(Boolean).join(' / ') || profile.analysisTarget || '--'
  return {
    value,
    chapterName: weakest.chapterName || '',
    tip: weakest.correctRate ? `自动识别最低正确率：${weakest.correctRate}%` : '未选择时默认取最薄弱模块'
  }
})

const teacherChapterOptions = computed(() => {
  if (!teacherScene.value.courseName) return []
  const map = new Map()
  teacherCatalogList.value
    .filter((item) => item.courseName === teacherScene.value.courseName)
    .forEach((item) => {
      const key = `${item.chapterCode || ''}-${item.chapterName || ''}`
      if (!map.has(key)) {
        map.set(key, {
          chapterCode: item.chapterCode || item.chapterName || '',
          chapterName: item.chapterName || '未命名模块'
        })
      }
    })
  return Array.from(map.values())
})

const teacherProfileTip = computed(() => {
  if (!isTeacherView.value) return ''
  if (teacherRagProfile.value?.missingReason) return teacherRagProfile.value.missingReason
  if (teacherRagProfile.value?.studentScoped && teacherScene.value.studentName) {
    return `后续会优先按学生 ${teacherScene.value.studentName} 的真实刷题、错题和模块表现做联合分析。`
  }
  return '后续会优先按教师当前可见范围内的班级、模块、错题热点和学生表现做联合分析。'
})

function unwrapResponse(response) {
  if (response && typeof response === 'object' && 'data' in response && response.data !== undefined) {
    return response.data
  }
  return response
}

function normalizeDatasetList(response) {
  const payload = unwrapResponse(response)
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.datasets)) return payload.datasets
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.list)) return payload.list
  if (Array.isArray(payload?.data?.datasets)) return payload.data.datasets
  return []
}

function normalizeUploadResults(response) {
  const payload = unwrapResponse(response)
  if (Array.isArray(payload?.results)) return payload.results
  if (Array.isArray(payload?.data?.results)) return payload.data.results
  if (Array.isArray(payload?.files)) return payload.files
  if (Array.isArray(payload)) return payload
  if (payload?.status === 'success' || payload?.filename || payload?.fileName) {
    return [{
      status: payload.status || 'success',
      filename: payload.filename || payload.fileName || payload.name || '已上传资料',
      message: payload.message || payload.msg || ''
    }]
  }
  return []
}

function datasetId(dataset = {}) {
  return dataset.id || dataset.dataset_id || ''
}

function datasetKey(dataset = {}, index = 0) {
  return datasetId(dataset) || `${datasetName(dataset)}-${index}`
}

function datasetName(dataset = {}) {
  return dataset.file_name || dataset.filename || dataset.name || dataset.dataset_id || '--'
}

function datasetTime(dataset = {}) {
  return dataset.upload_time || dataset.created_at || ''
}

function handleFileChange(file, uploadFiles) {
  const pool = [...fileList.value, ...uploadFiles]
  const validFiles = pool.filter((item) => {
    const name = String(item.name || '').toLowerCase()
    return name.endsWith('.xlsx') || name.endsWith('.xls') || name.endsWith('.txt')
  })

  if (validFiles.length !== pool.length) {
    ElMessage.warning('仅支持 .xlsx、.xls、.txt 文件。')
  }

  const deduped = []
  const seen = new Set()
  for (const item of validFiles) {
    const key = `${item.name || ''}_${item.size || 0}`
    if (seen.has(key)) continue
    seen.add(key)
    deduped.push(item)
  }

  if (deduped.length > 5) {
    ElMessage.warning('最多上传 5 个文件。')
  }

  fileList.value = deduped.slice(0, 5)
}

function handleExceed() {
  ElMessage.warning('最多上传 5 个文件。')
}

async function submitUpload() {
  if (!fileList.value.length) {
    ElMessage.warning('请先选择资料。')
    return
  }

  uploadLoading.value = true
  try {
    const response = await uploadExcelFiles(fileList.value)
    const results = normalizeUploadResults(response)
    const successFiles = results
      .filter((item) => item.status === 'success' || item.success === true || !item.status)
      .map((item) => item.filename || item.fileName || item.name || '已上传资料')
    const failedFiles = results.filter((item) => item.status && item.status !== 'success' && item.success !== true)

    if (successFiles.length) {
      ElMessage.success(`成功导入 ${successFiles.length} 个文件。`)
      appendMessage('ai', `已导入：${successFiles.join('、')}。现在你可以围绕这些资料继续提问。`)
      await getHistoryDatasets()
    }

    if (failedFiles.length) {
      ElMessage.warning(`有 ${failedFiles.length} 个文件导入失败。`)
      appendMessage(
        'ai',
        `以下文件导入失败：\n${failedFiles
          .map((item) => `${item.filename}：${item.message || '处理失败'}`)
          .join('\n')}`
      )
    }

    if (!results.length) {
      ElMessage.warning('未收到导入结果，请检查 AI 服务状态。')
      await getHistoryDatasets()
    }
  } finally {
    uploadLoading.value = false
    fileList.value = []
    await nextTick()
    focusLatestMessage()
  }
}

async function submitQuery() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题。')
    return
  }

  const userQuestion = question.value.trim()
  question.value = ''
  appendMessage('user', userQuestion)
  updateConversationTitleByQuestion(userQuestion)

  await nextTick()
  focusLatestMessage()

  queryLoading.value = true
  queryAborted = false

  try {
    if (isTeacherView.value) {
      if (!teacherRagProfile.value && !ragProfileLoading.value) {
        await loadTeacherRagProfile()
      }
    } else if (!studentRagProfile.value && !ragProfileLoading.value) {
      await loadStudentRagProfile()
    }
    const response = await queryQuestion(userQuestion, {
      sourceScene: String(route.query.source || '').trim(),
      studentNo: String(route.query.studentNo || '').trim(),
      studentName: String(route.query.studentName || '').trim(),
      courseName: String(route.query.courseName || '').trim(),
      chapterCode: String(route.query.chapterCode || '').trim(),
      chapterName: String(route.query.chapterName || '').trim(),
      knowledgePoint: String(route.query.knowledgePoint || '').trim(),
      questionId: route.query.questionId ? Number(route.query.questionId) : undefined
    })
    if (queryAborted) return

    const answer = response.answer || response.data || response.msg || '当前暂无回答。'
    const sources = Array.isArray(response.sources) ? response.sources : []
    const mode = String(response.mode || '')
    const matchedCount = Number(response.matchedCount || sources.length || 0)
    const matchedDatasetCount = Number(response.matchedDatasetCount || 0)
    let finalText = String(answer)
    const retrievalTip = buildRetrievalTip(mode, matchedCount, sources, matchedDatasetCount)

    if (retrievalTip && !finalText.includes('检索状态：')) {
      finalText = `${retrievalTip}\n\n${finalText}`
    }

    if (sources.length && !finalText.includes('参考资料：')) {
      const fileCount = matchedDatasetCount || new Set(sources.map((item) => item.fileName || '').filter(Boolean)).size
      const fileTip = fileCount ? `，涉及 ${fileCount} 个文件` : ''
      finalText += `\n\n参考资料：共命中 ${sources.length} 条知识片段${fileTip}。`
    }

    appendMessage('ai', finalText)
  } catch (error) {
    if (queryAborted) {
      appendMessage('ai', '本次查询已中止。')
      return
    }
    const errorMessage = resolveQueryErrorMessage(error)
    ElMessage.error(errorMessage)
    appendMessage('ai', errorMessage)
  } finally {
    queryLoading.value = false
    queryAborted = false
    await nextTick()
    focusLatestMessage()
  }
}

function abortQuery() {
  queryAborted = true
  queryLoading.value = false
  ElMessage.info('已中止本次查询。')
}

function resolveQueryErrorMessage(error) {
  const status = error?.response?.status
  const responseData = error?.response?.data || {}
  const rawMessage = String(responseData.msg || responseData.message || responseData.detail || error?.message || error || '').trim()
  if (status === 401 || rawMessage.includes('401') || rawMessage.includes('认证失败') || rawMessage.includes('无效的会话')) {
    return '登录状态已过期，请重新进入学生端登录后再提问。'
  }
  if (rawMessage.includes('timeout') || rawMessage.includes('请求超时')) {
    return 'AI 问答耗时较长，请稍后重试或缩短问题。'
  }
  if (rawMessage.includes('Network Error') || rawMessage.includes('连接异常')) {
    return '后端或 AI 服务连接异常，请检查服务是否启动。'
  }
  return rawMessage || '查询失败，请检查后端或 AI 服务是否正常。'
}

async function loadStudentRagProfile() {
  if (isTeacherView.value) return
  ragProfileLoading.value = true
  try {
    const response = await getStudentRagProfile({
      courseName: String(route.query.courseName || '').trim(),
      chapterCode: String(route.query.chapterCode || '').trim(),
      chapterName: String(route.query.chapterName || '').trim()
    })
    studentRagProfile.value = response.data || response || null
  } catch (error) {
    studentRagProfile.value = null
  } finally {
    ragProfileLoading.value = false
  }
}

function normalizeTeacherCatalogItem(item = {}) {
  const courseName = String(item.courseName || '').trim()
  const chapterName = String(item.chapterName || '').trim()
  const chapterCode = String(item.chapterCode || chapterName).trim()
  if (!courseName || !chapterName) return null
  return {
    ...item,
    courseName,
    chapterName,
    chapterCode: chapterCode || chapterName
  }
}

async function loadTeacherCatalogs() {
  if (!isTeacherView.value) return
  try {
    const response = await getTeacherStudentCatalogs({
      courseName: teacherScene.value.courseName || undefined
    })
    const rows = Array.isArray(response?.data) ? response.data : []
    teacherCatalogList.value = rows.map(normalizeTeacherCatalogItem).filter(Boolean)
  } catch (error) {
    teacherCatalogList.value = []
  }
}

function syncTeacherSceneFromRoute() {
  teacherScene.value = {
    studentNo: String(route.query.studentNo || '').trim(),
    studentName: String(route.query.studentName || '').trim(),
    courseName: String(route.query.courseName || '').trim(),
    chapterCode: String(route.query.chapterCode || '').trim(),
    chapterName: String(route.query.chapterName || '').trim()
  }
}

async function loadTeacherRagProfile() {
  if (!isTeacherView.value) return
  ragProfileLoading.value = true
  try {
    const response = await getTeacherRagProfile({
      studentNo: String(route.query.studentNo || '').trim(),
      studentName: String(route.query.studentName || '').trim(),
      courseName: String(route.query.courseName || '').trim(),
      chapterCode: String(route.query.chapterCode || '').trim(),
      chapterName: String(route.query.chapterName || '').trim()
    })
    teacherRagProfile.value = response?.data || response || null
  } catch (error) {
    teacherRagProfile.value = null
  } finally {
    ragProfileLoading.value = false
  }
}

function buildChapterKey(chapterCode, chapterName) {
  return `${String(chapterCode || '').trim()}||${String(chapterName || '').trim()}`
}

function normalizeChapterOption(item = {}) {
  const courseName = String(item.courseName || '').trim()
  const chapterName = String(item.chapterName || '').trim()
  const chapterCode = String(item.chapterCode || chapterName).trim()
  if (!chapterCode && !chapterName) return null
  const label = [courseName, chapterName || chapterCode].filter(Boolean).join(' / ')
  return {
    courseName,
    chapterCode,
    chapterName,
    key: buildChapterKey(chapterCode, chapterName),
    label: label || chapterName || chapterCode
  }
}

async function loadStudentChapterCatalog() {
  if (isTeacherView.value) return
  try {
    const response = await getStudentDiagnosisChapters()
    const rows = Array.isArray(response?.data) ? response.data : []
    studentChapterCatalog.value = rows.map(normalizeChapterOption).filter(Boolean)
  } catch (error) {
    studentChapterCatalog.value = []
  }
}

function handleTeacherCourseChange(value) {
  teacherScene.value.courseName = value || ''
  teacherScene.value.chapterCode = ''
  teacherScene.value.chapterName = ''
  loadTeacherCatalogs()
}

function handleTeacherChapterChange(value) {
  const target = teacherChapterOptions.value.find((item) => item.chapterCode === value)
  teacherScene.value.chapterName = target?.chapterName || ''
}

function handleStudentChapterChange(value) {
  const selected = studentChapterOptions.value.find((item) => item.key === value)
  const query = { ...route.query }
  if (!selected) {
    delete query.courseName
    delete query.chapterCode
    delete query.chapterName
  } else {
    query.courseName = selected.courseName || undefined
    query.chapterCode = selected.chapterCode || undefined
    query.chapterName = selected.chapterName || undefined
  }
  router.replace({ path: route.path, query })
}

function applyTeacherScene() {
  const chapter = teacherChapterOptions.value.find((item) => item.chapterCode === teacherScene.value.chapterCode)
  const chapterName = teacherScene.value.chapterName || chapter?.chapterName || ''
  router.replace({
    query: {
      ...route.query,
      source: String(route.query.source || 'teacher-rag').trim() || 'teacher-rag',
      studentNo: undefined,
      studentName: undefined,
      courseName: teacherScene.value.courseName || undefined,
      chapterCode: teacherScene.value.chapterCode || undefined,
      chapterName: chapterName || undefined
    }
  })
}

function resetTeacherScene() {
  teacherScene.value = {
    studentNo: '',
    studentName: '',
    courseName: '',
    chapterCode: '',
    chapterName: ''
  }
  router.replace({
    query: {
      ...route.query,
      source: 'teacher-rag',
      studentNo: undefined,
      studentName: undefined,
      courseName: undefined,
      chapterCode: undefined,
      chapterName: undefined
    }
  })
}

async function handleImportCurrentScene() {
  applyPrompt(sceneHint.value.prompt)
  sceneImportLoading.value = true
  try {
    const response = await importCurrentSceneToKnowledge({
      sourceScene: String(route.query.source || (isTeacherView.value ? 'teacher-rag' : 'student-rag')).trim(),
      studentNo: String(route.query.studentNo || '').trim(),
      studentName: String(route.query.studentName || '').trim(),
      courseName: String(route.query.courseName || '').trim(),
      chapterCode: String(route.query.chapterCode || '').trim(),
      chapterName: String(route.query.chapterName || '').trim()
    })
    await getHistoryDatasets()
    if (isTeacherView.value) {
      await loadTeacherRagProfile()
    } else {
      await loadStudentRagProfile()
    }
    const data = unwrapResponse(response)
    const importedName = String(data?.sceneFileName || data?.fileName || data?.filename || 'current_scene_profile.txt')
    ElMessage.success('当前场景已打包入知识库。')
    appendMessage('ai', `已将当前场景相关数据入库：${importedName}。后续回答会结合这份场景知识库与原有资料一起分析。`)
  } catch (error) {
    const detail =
      error?.response?.data?.msg ||
      error?.response?.data?.message ||
      error?.response?.data?.detail ||
      error?.message ||
      '请稍后重试'
    ElMessage.error(`带入当前场景失败：${detail}`)
  } finally {
    sceneImportLoading.value = false
  }
}

function buildRetrievalTip(mode, matchedCount, sources, matchedDatasetCount = 0) {
  const fileCount = matchedDatasetCount || new Set((sources || []).map((item) => item.fileName || '').filter(Boolean)).size
  const fileText = fileCount ? `，来自 ${fileCount} 个文件` : ''
  if (mode === 'strong') {
    return `已检索到相关知识库内容，共 ${matchedCount} 条${fileText}。`
  }
  if (mode === 'fallback') {
    return `已检索到部分相关知识库内容，共 ${matchedCount} 条${fileText}。`
  }
  return ''
}

function scrollToLatestMessage() {
  if (latestMessageAnchor.value?.scrollIntoView) {
    latestMessageAnchor.value.scrollIntoView({ block: 'end' })
    return
  }
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

async function focusLatestMessage() {
  await nextTick()
  scrollToLatestMessage()
  window.requestAnimationFrame(() => scrollToLatestMessage())
}

async function getHistoryDatasets() {
  loadingHistory.value = true
  try {
    const response = await getDatasets()
    historyDatasets.value = normalizeDatasetList(response)
  } catch (error) {
    ElMessage.error('获取知识库列表失败。')
    historyDatasets.value = []
  } finally {
    loadingHistory.value = false
  }
}

function refreshHistory() {
  getHistoryDatasets()
}

async function handleDeleteDataset(dataset) {
  const id = datasetId(dataset)
  if (!id) {
    ElMessage.warning('当前知识库缺少数据集 ID，无法删除。')
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除“${datasetName(dataset)}”吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDataset(id)
    ElMessage.success('删除成功。')
    await getHistoryDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败。')
    }
  }
}

async function viewDataset(dataset) {
  const id = datasetId(dataset)
  const baseText = `文件名：${datasetName(dataset)}\n上传时间：${formatTime(datasetTime(dataset))}`

  if (!id) {
    ElMessageBox.alert(baseText, '知识库详情', { confirmButtonText: '确定' })
    return
  }

  try {
    const response = await getDatasetDetail(id)
    const data = unwrapResponse(response)
    ElMessageBox.alert(
      `${baseText}\n\n${data.description || '当前没有更多数据集描述。'}`,
      '知识库详情',
      { confirmButtonText: '确定' }
    )
  } catch (error) {
    ElMessage.error('查看知识库详情失败。')
  }
}

function formatTime(timeStr) {
  if (!timeStr) return '--'
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function createConversation(title = '新对话') {
  const now = new Date().toISOString()
  return {
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    title,
    createdAt: now,
    updatedAt: now,
    messages: []
  }
}

function normalizeConversation(item = {}) {
  return {
    id: String(item.id || ''),
    title: String(item.title || '新对话'),
    createdAt: String(item.createdAt || new Date().toISOString()),
    updatedAt: String(item.updatedAt || item.createdAt || new Date().toISOString()),
    messages: Array.isArray(item.messages)
      ? item.messages.map((message) => ({ type: message.type, content: String(message.content || '') }))
      : []
  }
}

function saveConversations() {
  try {
    const trimmed = conversations.value.slice(0, MAX_CONVERSATIONS)
    localStorage.setItem(historyStorageKey.value, JSON.stringify(trimmed))
  } catch (error) {
    console.warn('保存会话失败', error)
  }
}

function loadConversations() {
  try {
    const raw = localStorage.getItem(historyStorageKey.value)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.map((item) => normalizeConversation(item)).filter((item) => item.id)
  } catch (error) {
    return []
  }
}

function touchActiveConversation() {
  const id = activeConversationId.value
  if (!id) return
  const idx = conversations.value.findIndex((item) => item.id === id)
  if (idx < 0) return
  conversations.value[idx].messages = messages.value.map((message) => ({ type: message.type, content: message.content }))
  conversations.value[idx].updatedAt = new Date().toISOString()
  const active = conversations.value[idx]
  conversations.value.splice(idx, 1)
  conversations.value.unshift(active)
  activeConversationId.value = active.id
  saveConversations()
}

function appendMessage(type, content) {
  messages.value.push({ type, content })
  touchActiveConversation()
}

function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeAssistantContent(text) {
  let value = String(text || '').replace(/\r\n/g, '\n').trim()
  if (!value) return ''

  value = value
    .replace(/<\/?[^>]+>/g, '')
    .replace(/\*\*/g, '')
    .replace(/^[ \t]*#{1,6}[ \t]*/gm, '')
    .replace(/^[ \t]*[-*][ \t]+/gm, '• ')
    .replace(/^[ \t]*•[ \t]*/gm, '• ')
    .replace(/^[ \t]*---+[ \t]*$/gm, '')
    .replace(/\n{3,}/g, '\n\n')

  return value
}

function formatMessageContent(message) {
  const raw = message?.content || ''
  const content = message?.type === 'ai' ? normalizeAssistantContent(raw) : String(raw)
  const escaped = escapeHtml(content)
  return escaped
    .split('\n\n')
    .map((block) => `<p>${block.replace(/\n/g, '<br/>')}</p>`)
    .join('')
}

function setActiveConversation(id) {
  if (!id) return
  touchActiveConversation()
  const target = conversations.value.find((item) => item.id === id)
  if (!target) return
  activeConversationId.value = target.id
  messages.value = (target.messages || []).map((message) => ({ type: message.type, content: message.content }))
  focusLatestMessage()
}

function startNewConversation() {
  touchActiveConversation()
  const newSession = createConversation()
  conversations.value.unshift(newSession)
  if (conversations.value.length > MAX_CONVERSATIONS) {
    conversations.value = conversations.value.slice(0, MAX_CONVERSATIONS)
  }
  activeConversationId.value = newSession.id
  messages.value = []
  saveConversations()
}

function deleteConversation(id) {
  const idx = conversations.value.findIndex((item) => item.id === id)
  if (idx < 0) return
  const deletingActive = activeConversationId.value === id
  conversations.value.splice(idx, 1)

  if (!conversations.value.length) {
    const nextSession = createConversation()
    conversations.value = [nextSession]
    activeConversationId.value = nextSession.id
    messages.value = []
    saveConversations()
    return
  }

  if (deletingActive) {
    activeConversationId.value = conversations.value[0].id
    messages.value = (conversations.value[0].messages || []).map((message) => ({
      type: message.type,
      content: message.content
    }))
    focusLatestMessage()
  }

  saveConversations()
}

function updateConversationTitleByQuestion(questionText) {
  const id = activeConversationId.value
  if (!id) return
  const idx = conversations.value.findIndex((item) => item.id === id)
  if (idx < 0) return
  const current = conversations.value[idx]
  if (current.title && current.title !== '新对话') return
  conversations.value[idx].title = String(questionText || '').trim().slice(0, 20) || '新对话'
  saveConversations()
}

function hydrateQuestionFromRoute() {
  const queryQuestionText = String(route.query.question || '').trim()
  if (!queryQuestionText) return
  question.value = queryQuestionText
}

function applyPrompt(prompt) {
  question.value = String(prompt || '').trim()
}

onMounted(() => {
  const history = loadConversations()
  if (history.length) {
    conversations.value = history
    activeConversationId.value = history[0].id
    messages.value = (history[0].messages || []).map((message) => ({ type: message.type, content: message.content }))
    focusLatestMessage()
  } else {
    startNewConversation()
  }
  hydrateQuestionFromRoute()
  getHistoryDatasets()
  syncTeacherSceneFromRoute()
  if (isTeacherView.value) {
    loadTeacherCatalogs()
    loadTeacherRagProfile()
  } else {
    loadStudentChapterCatalog()
    loadStudentRagProfile()
  }
})

watch(
  () => historyStorageKey.value,
  () => {
    const history = loadConversations()
    if (history.length) {
      conversations.value = history
      activeConversationId.value = history[0].id
      messages.value = (history[0].messages || []).map((message) => ({ type: message.type, content: message.content }))
      focusLatestMessage()
      return
    }
    conversations.value = []
    activeConversationId.value = ''
    messages.value = []
    startNewConversation()
  }
)

watch(
  () => route.query.question,
  () => {
    hydrateQuestionFromRoute()
  }
)

watch(
  () => [route.query.courseName, route.query.chapterCode, route.query.chapterName],
  () => {
    if (isTeacherView.value) {
      syncTeacherSceneFromRoute()
      loadTeacherCatalogs()
      loadTeacherRagProfile()
      return
    }
    loadStudentRagProfile()
  }
)

watch(
  () => [route.query.studentNo, route.query.studentName],
  () => {
    if (!isTeacherView.value) return
    syncTeacherSceneFromRoute()
    loadTeacherRagProfile()
  }
)
</script>

<style scoped>
.rag-shell {
  height: 100vh;
  padding: 18px;
  display: grid;
  grid-template-columns: 284px 1fr;
  gap: 16px;
  overflow: hidden;
  box-sizing: border-box;
  background:
    radial-gradient(circle at top left, rgba(16, 163, 127, 0.08), transparent 26%),
    linear-gradient(180deg, #f6f7f9 0%, #eef1f5 100%);
  color: #1f2937;
}

.kb-sidebar {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 36px);
}

.session-header,
.kb-header {
  padding: 14px;
  border-bottom: 1px solid #eef0f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.session-header h2,
.kb-header h2 {
  margin: 0;
  font-size: 18px;
}

.session-header p,
.kb-header p {
  margin: 4px 0 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.session-list {
  padding: 8px;
  max-height: 130px;
  border-bottom: 1px solid #eef0f3;
}

.session-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-item.active {
  border-color: #10a37f;
  background: #f0fdf9;
}

.session-main {
  min-width: 0;
  flex: 1;
}

.session-delete {
  flex: none;
}

.session-title {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.kb-list {
  padding: 8px;
  flex: 1;
  min-height: 0;
}

.kb-item {
  border: 1px solid #eef0f3;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 8px;
  background: #fff;
}

.kb-item-main {
  min-width: 0;
}

.kb-name {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-time {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.kb-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.kb-empty {
  padding-top: 24px;
}

.chat-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 36px);
  overflow: hidden;
}

.chat-topbar {
  padding: 14px 16px;
  border-bottom: 1px solid #eef0f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-title-wrap h1 {
  margin: 0;
  font-size: 46px;
  line-height: 1.14;
}

.chat-title-wrap span {
  display: block;
  margin-top: 4px;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.user-badge {
  display: flex;
  flex-direction: column;
  min-width: 170px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #dbe3ef;
}

.user-badge strong {
  color: #0f172a;
  font-size: 16px;
}

.user-badge span {
  color: #64748b;
  font-size: 13px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.upload-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.upload-inline :deep(.el-button),
.scene-actions :deep(.el-button) {
  min-height: 40px;
  padding: 0 18px;
  font-size: 15px;
}

.selected-files {
  max-width: 220px;
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scene-context-card {
  margin: 12px 14px 0;
  border: 1px solid #d8f0e7;
  background: linear-gradient(135deg, #f0fdf7 0%, #ecfeff 100%);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.08);
  overflow: hidden;
}

.scene-context-summary {
  width: 100%;
  min-height: 58px;
  padding: 10px 14px;
  border: 0;
  background: transparent;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  text-align: left;
  cursor: pointer;
}

.scene-toggle-arrow {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #ffffff;
  color: #0f766e;
  font-size: 20px;
  line-height: 28px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(15, 118, 110, 0.12);
}

.scene-context-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.scene-context-main strong {
  color: #0f172a;
  font-size: 17px;
  white-space: nowrap;
}

.scene-context-main span {
  color: #475569;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-context-status {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.scene-context-score {
  color: #0f766e;
  font-weight: 700;
  font-size: 15px;
}

.scene-context-detail {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 14px 14px 58px;
}

.scene-context-copy {
  flex: 1;
  min-width: 0;
}

.scene-context-copy strong {
  display: block;
  color: #0f172a;
  font-size: 17px;
}

.scene-context-copy p {
  margin: 8px 0 10px;
  color: #475569;
  font-size: 16px;
  line-height: 1.65;
}

.scene-banner {
  margin: 14px 14px 0;
  padding: 12px 14px;
  border: 1px solid #d8f0e7;
  background: linear-gradient(135deg, #f0fdf7 0%, #ecfeff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.teacher-scene-toolbar {
  margin: 14px 14px 0;
  padding: 14px;
  border: 1px solid #e4ecf7;
  background: linear-gradient(135deg, #fbfdff 0%, #f5f9ff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.teacher-scene-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.teacher-scene-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.teacher-scene-grid :deep(.el-select) {
  width: 100%;
}

.scene-banner strong {
  display: block;
  font-size: 16px;
}

.scene-banner p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.scene-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-strip {
  display: none;
}

.profile-card {
  border: 1px solid #dbe7f4;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f6fbff 100%);
  padding: 14px 16px;
}

.profile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.profile-head strong {
  font-size: 16px;
}

.profile-tip {
  margin: 8px 0 10px;
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 14px;
  color: #475569;
}

.teacher-profile-card .profile-meta span {
  display: inline-flex;
  align-items: center;
}

.warning-text {
  color: #b45309;
}

.student-ai-context {
  margin: 14px 14px 0;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(240, 253, 250, 0.94) 0%, rgba(248, 250, 252, 0.96) 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 1.15fr;
  gap: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.context-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.context-metric {
  min-width: 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.context-metric span,
.context-metric small {
  display: block;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.context-metric strong {
  display: block;
  margin: 6px 0;
  color: #0f172a;
  font-size: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-questions {
  min-width: 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(14, 165, 233, 0.16);
}

.context-question-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.context-question-head strong {
  color: #0f172a;
  font-size: 18px;
  white-space: nowrap;
}

.context-question-head span {
  color: #64748b;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-module-switch {
  margin-bottom: 10px;
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr);
  gap: 10px;
  align-items: center;
}

.context-module-switch > span {
  color: #475569;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.context-module-switch :deep(.el-select) {
  width: 100%;
}

.context-question-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.context-question-list button {
  border: 1px solid rgba(14, 116, 144, 0.24);
  border-radius: 999px;
  padding: 8px 12px;
  background: #ffffff;
  color: #0e7490;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(14, 116, 144, 0.08);
}

.context-question-list button:hover {
  border-color: rgba(14, 116, 144, 0.5);
  background: #ecfeff;
}

.scene-analysis-banner {
  margin: 14px 14px 0;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid #dbe7f4;
  background: linear-gradient(135deg, #f8fbff 0%, #fdfefe 100%);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.scene-analysis-main {
  flex: 1;
  min-width: 0;
}

.scene-analysis-main strong {
  display: block;
  font-size: 16px;
  color: #0f172a;
}

.scene-analysis-main p {
  margin: 8px 0 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.scene-analysis-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scene-analysis-actions {
  margin-top: 12px;
}


.scene-analysis-side {
  min-width: 148px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: right;
}

.scene-analysis-side span,
.scene-analysis-side small {
  color: #64748b;
  font-size: 13px;
}

.scene-analysis-side strong {
  font-size: 24px;
  color: #0f766e;
}

.chat-stream {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 18px;
  background: #f7f8fa;
}

.latest-message-anchor {
  height: 1px;
}

.chat-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 16px;
}

.prompt-grid {
  width: min(820px, 100%);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.prompt-card {
  text-align: left;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt-card:hover {
  border-color: #10a37f;
  box-shadow: 0 10px 22px rgba(16, 163, 127, 0.08);
  transform: translateY(-1px);
}

.prompt-card strong {
  display: block;
  font-size: 16px;
  margin-bottom: 6px;
}

.prompt-card span {
  display: block;
  font-size: 14px;
  line-height: 1.6;
  color: #6b7280;
}

.example-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  width: min(820px, 100%);
  font-size: 14px;
  color: #6b7280;
}

.example-tag {
  cursor: pointer;
}

.chat-row {
  display: flex;
  margin-bottom: 12px;
}

.chat-row.is-user {
  justify-content: flex-end;
}

.chat-row.is-ai {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 80%;
  padding: 10px 12px;
  border-radius: 12px;
  line-height: 1.7;
  word-break: break-word;
  font-size: 16px;
}

.chat-bubble :deep(p) {
  margin: 0 0 10px;
}

.chat-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-row.is-user .chat-bubble {
  background: #10a37f;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-row.is-ai .chat-bubble {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  color: #111827;
  border-bottom-left-radius: 4px;
}

.loading-bubble {
  width: min(560px, 80%);
}

.composer {
  flex: none;
  position: sticky;
  bottom: 0;
  z-index: 2;
  padding: 12px;
  border-top: 1px solid #eef0f3;
  background: #fff;
}

.composer-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
}

.composer-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

@media (max-width: 960px) {
  .rag-shell {
    grid-template-columns: 1fr;
    padding: 12px;
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .kb-sidebar,
  .chat-panel {
    min-height: auto;
    height: auto;
  }

  .kb-list {
    height: 260px;
  }

  .chat-panel {
    min-height: calc(100vh - 330px);
  }

  .selected-files {
    max-width: 120px;
  }

  .student-ai-context,
  .context-metrics {
    grid-template-columns: 1fr;
  }

  .context-question-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .context-question-head span {
    white-space: normal;
  }

  .context-module-switch {
    grid-template-columns: 1fr;
  }

  .scene-context-summary {
    grid-template-columns: 32px minmax(0, 1fr);
  }

  .scene-context-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .scene-context-status {
    grid-column: 2;
  }

  .scene-context-detail {
    flex-direction: column;
    padding-left: 14px;
  }

  .scene-banner,
  .composer-actions,
  .teacher-scene-toolbar,
  .scene-analysis-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .teacher-scene-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .teacher-scene-actions {
    width: 100%;
    flex-direction: column;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }
}
</style>

