<template>
  <div class="pad-page">
    <el-card class="pad-header">
      <div class="pad-header-main">
        <div>
          <h2>{{ isTeacher ? '教师 Pad 工作台' : '学生 Pad 工作台' }}</h2>
          <p>{{ isTeacher ? '围绕学情分析、智能组卷、AI 批改和教学问答展开，突出教师教学减负与决策支持。' : '围绕学业诊断、学习规划、智能刷题和 RAG 问答展开，突出学生个性化学习支持。' }}</p>
        </div>
        <div class="pad-header-actions">
          <el-button @click="openProfileDialog">个人信息</el-button>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </el-card>

    <TeacherWorkspace
      v-if="isTeacher"
      v-model:active-tab="teacherActiveTab"
      v-model:publish-type="publishType"
      v-model:chat-mode="chatMode"
      v-model:chat-keyword="chatKeyword"
      v-model:chat-input="chatInput"
      :homework-form="homeworkForm"
      :exam-form="examForm"
      :class-options="classOptions"
      :homework-upload-types="homeworkUploadTypes"
      :teacher-homework="teacherHomework"
      :teacher-exam="teacherExam"
      :teacher-submissions="teacherSubmissions"
      :teacher-exam-scores="teacherExamScores"
      :teacher-tasks="teacherTasks"
      :teacher-scores="teacherScores"
      :teacher-exam-trend-ref-setter="setTeacherExamTrendRef"
      :teacher-homework-dist-ref-setter="setTeacherHomeworkDistRef"
      :teacher-task-pie-ref-setter="setTeacherTaskPieRef"
      :teacher-radar-ref-setter="setTeacherRadarRef"
      :chat-contact-count="chatContactCount"
      :filtered-chat-contacts="filteredChatContacts"
      :filtered-chat-groups="filteredChatGroups"
      :active-chat-target-type="activeChatTargetType"
      :active-chat-peer-id="activeChatPeerId"
      :active-chat-group-id="activeChatGroupId"
      :active-chat-target-label="activeChatTargetLabel"
      :chat-list-loading="chatListLoading"
      :chat-messages="chatMessages"
      :chat-sending="chatSending"
      :chat-body-ref-setter="setChatBodyRef"
      :load-teacher-publish-data="loadTeacherPublishData"
      :handle-create-homework="handleCreateHomework"
      :handle-create-exam="handleCreateExam"
      :has-homework-attachment="hasHomeworkAttachment"
      :open-homework-attachment="openHomeworkAttachment"
      :load-teacher-homework-submissions="loadTeacherHomeworkSubmissions"
      :extract-teacher-reason-from-feedback="extractTeacherReasonFromFeedback"
      :extract-review-image-from-feedback="extractReviewImageFromFeedback"
      :open-review-image="openReviewImage"
      :open-homework-review="openHomeworkReview"
      :load-teacher-exam-scores="loadTeacherExamScores"
      :open-exam-review="openExamReview"
      :load-teacher-scores="loadTeacherScores"
      :refresh-teacher-visual="refreshTeacherVisual"
      :load-chat-data="loadChatData"
      :handle-chat-mode-change="handleChatModeChange"
      :select-chat-contact="selectChatContact"
      :select-chat-group="selectChatGroup"
      :is-self-chat-message="isSelfChatMessage"
      :open-private-from-group="openPrivateFromGroup"
      :send-chat="sendChat"
    />

    <StudentWorkspace
      v-if="isStudent"
      v-model:active-tab="studentActiveTab"
      v-model:chat-mode="chatMode"
      v-model:chat-keyword="chatKeyword"
      v-model:chat-input="chatInput"
      :student-homework-merged="studentHomeworkMerged"
      :student-exam-merged="studentExamMerged"
      :class-score-stats="classScoreStats"
      :student-exam-trend-ref-setter="setStudentExamTrendRef"
      :student-homework-bar-ref-setter="setStudentHomeworkBarRef"
      :student-completion-gauge-ref-setter="setStudentCompletionGaugeRef"
      :student-radar-ref-setter="setStudentRadarRef"
      :chat-contact-count="chatContactCount"
      :filtered-chat-contacts="filteredChatContacts"
      :filtered-chat-groups="filteredChatGroups"
      :active-chat-target-type="activeChatTargetType"
      :active-chat-peer-id="activeChatPeerId"
      :active-chat-group-id="activeChatGroupId"
      :active-chat-target-label="activeChatTargetLabel"
      :chat-list-loading="chatListLoading"
      :chat-messages="chatMessages"
      :chat-sending="chatSending"
      :chat-body-ref-setter="setChatBodyRef"
      :load-student-submissions="loadStudentSubmissions"
      :open-review-image="openReviewImage"
      :has-homework-attachment="hasHomeworkAttachment"
      :open-homework-attachment="openHomeworkAttachment"
      :open-submit="openSubmit"
      :refresh-student-exam-merged="refreshStudentExamMerged"
      :open-exam-submit="openExamSubmit"
      :refresh-student-visual="refreshStudentVisual"
      :load-chat-data="loadChatData"
      :handle-chat-mode-change="handleChatModeChange"
      :select-chat-contact="selectChatContact"
      :select-chat-group="selectChatGroup"
      :is-self-chat-message="isSelfChatMessage"
      :open-private-from-group="openPrivateFromGroup"
      :send-chat="sendChat"
    />

    <el-dialog v-model="submitDialog" title="提交作业" width="520px">
      <el-form :model="submitForm" label-width="88px">
        <el-form-item label="作业 ID"><el-input v-model="submitForm.homeworkId" disabled /></el-form-item>
        <el-form-item label="作答内容"><el-input v-model="submitForm.answerContent" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="答案图片">
          <FileUpload v-model="submitForm.answerImageUrl" :limit="1" :file-size="10" :file-type="answerImageTypes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitHomework">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="examSubmitDialog" title="提交考试作答" width="560px">
      <el-form :model="examSubmitForm" label-width="88px">
        <el-form-item label="考试 ID"><el-input v-model="examSubmitForm.examId" disabled /></el-form-item>
        <el-form-item label="考试名称"><el-input v-model="examSubmitForm.examTitle" disabled /></el-form-item>
        <el-form-item label="作答内容"><el-input v-model="examSubmitForm.answerContent" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="答案图片">
          <FileUpload v-model="examSubmitForm.answerImageUrl" :limit="1" :file-size="10" :file-type="answerImageTypes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="examSubmitDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitExam">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialog" :title="reviewType === 'exam' ? '考试评分' : '作业批改'" width="980px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form :model="reviewForm" label-width="96px">
            <el-form-item v-if="reviewType === 'homework'" label="提交记录 ID"><el-input v-model="reviewForm.submissionId" disabled /></el-form-item>
            <el-form-item v-if="reviewType === 'exam'" label="成绩记录 ID"><el-input v-model="reviewForm.scoreId" disabled /></el-form-item>
            <el-form-item v-if="reviewType === 'exam'" label="考试 ID"><el-input v-model="reviewForm.examId" disabled /></el-form-item>
            <el-form-item label="学生姓名"><el-input v-model="reviewForm.studentName" disabled /></el-form-item>
            <el-form-item label="标题"><el-input v-model="reviewForm.title" disabled /></el-form-item>
            <el-form-item label="作答内容"><el-input v-model="reviewForm.answerContent" type="textarea" :rows="6" /></el-form-item>
            <el-form-item label="答案图片">
              <el-image
                v-if="reviewAnswerImageUrl"
                :src="reviewAnswerImageUrl"
                fit="contain"
                style="width: 100%; height: 160px; border: 1px solid #dbe2ea; border-radius: 8px;"
                :preview-src-list="[reviewAnswerImageUrl]"
              />
              <span v-else style="color: #94a3b8;">当前没有可预览的答案图片</span>
            </el-form-item>
            <el-form-item label="上传批改图"><input type="file" accept="image/*" @change="handleReviewImageUpload" /></el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-form :model="reviewForm" label-width="108px">
            <el-form-item label="参考答案"><el-input v-model="reviewForm.exampleAnswer" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="参考分数"><el-input-number v-model="reviewForm.exampleScore" :min="0" :max="reviewForm.maxScore || 100" /></el-form-item>
            <el-form-item label="参考评语"><el-input v-model="reviewForm.exampleFeedback" type="textarea" :rows="3" /></el-form-item>
            <el-form-item>
              <el-button type="primary" plain @click="handleAiSuggestReview">AI 评语建议</el-button>
              <el-button type="success" plain :loading="aiImageGrading" @click="handleAiImageGrade">AI 图像批改</el-button>
            </el-form-item>
            <el-form-item label="最终得分"><el-input-number v-model="reviewForm.score" :min="0" :max="reviewForm.maxScore || 100" /></el-form-item>
            <el-form-item label="教师评语"><el-input v-model="reviewForm.feedback" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="批改图导出">
              <el-button size="small" @click="exportReviewCanvas">导出批改图</el-button>
              <span v-if="reviewForm.reviewImageUrl" style="margin-left: 8px; color: #64748b;">已存在历史批改图</span>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>

      <div class="review-canvas-panel">
        <div class="review-toolbar">
          <el-radio-group v-model="reviewCanvasTool" size="small">
            <el-radio-button label="check">对勾</el-radio-button>
            <el-radio-button label="cross">叉号</el-radio-button>
            <el-radio-button label="text">文字</el-radio-button>
          </el-radio-group>
          <div>
            <el-button size="small" @click="clearReviewCanvasMarks">清空标记</el-button>
            <el-button size="small" type="primary" plain @click="exportReviewCanvas">导出画布</el-button>
          </div>
        </div>
        <div class="review-canvas-wrap">
          <canvas ref="reviewCanvasRef" width="900" height="360" class="review-canvas" @click="onReviewCanvasClick" />
        </div>
      </div>

      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReview">提交批改</el-button>
      </template>
    </el-dialog>

    <div class="bottom-actions" v-if="isTeacher || isStudent">
      <template v-if="isTeacher">
        <el-button type="success" plain @click="goTo('/education/teacher/analysis')">学情分析</el-button>
        <el-button type="info" plain @click="goTo('/education/teacher/qa')">教学问答</el-button>
        <el-button type="warning" plain @click="goTo('/education/teacher/paper')">试卷生成</el-button>
        <el-button type="primary" plain @click="goTo('/education/teacher/grading')">AI 批改</el-button>
        <el-button plain @click="goTo('/education/teacher/ai')">教师 AI 入口</el-button>
      </template>
      <template v-if="isStudent">
        <el-button type="info" plain @click="goTo('/education/student/report')">学业诊断</el-button>
        <el-button type="primary" plain @click="goTo('/education/student/plan')">学习规划</el-button>
        <el-button type="success" plain @click="goTo('/education/student/practice')">智能刷题</el-button>
        <el-button type="warning" plain @click="goTo('/education/student/materials')">资料整理</el-button>
        <el-button plain @click="goTo('/education/student/ai')">学生 AI 入口</el-button>
      </template>
    </div>

    <el-dialog v-model="profileDialogVisible" title="个人信息" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ profileInfo.userName }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ profileInfo.nickName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ profileInfo.roles }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="profileDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import { getToken } from '@/utils/auth'
import {
  createHomework,
  listTeacherHomework,
  uploadHomeworkAttachment,
  listTeacherHomeworkSubmissions,
  scoreHomework as scoreHomeworkApi,
  createExam,
  listTeacherExam,
  scoreExam,
  listTeacherExamScore,
  listTeacherTasks,
  listTeacherScores,
  aiSuggestReview
} from '@/api/education/teacher'
import {
  listStudentHomework,
  submitHomework,
  listStudentHomeworkSubmissions,
  listStudentExam as listStudentExamApi,
  submitExam,
  listStudentExamScore,
  listStudentSelfScores
} from '@/api/education/student'
import { listManagerScores, createTeacherTask } from '@/api/education/admin'
import {
  listChatContacts,
  listChatMessages,
  sendChatMessage,
  listChatGroups,
  listGroupChatMessages,
  sendGroupChatMessage
} from '@/api/education/chat'
import { aiGradeSingle } from '@/api/education/aiGrading'
import TeacherWorkspace from '@/views/education/teacher/TeacherWorkspace.vue'
import StudentWorkspace from '@/views/education/student/StudentWorkspace.vue'

const props = defineProps({
  forceRoleView: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const roles = computed(() => (userStore.roles || []).map(r => String(r).toLowerCase()))
const normalizedForcedRoleView = computed(() => String(props.forceRoleView || '').trim().toLowerCase())
const hasTeacherRole = computed(() => roles.value.includes('teacher'))
const hasStudentRole = computed(() => roles.value.includes('student') || roles.value.includes('role_default'))
const isTeacher = computed(() => normalizedForcedRoleView.value ? normalizedForcedRoleView.value === 'teacher' : hasTeacherRole.value)
const isStudent = computed(() => normalizedForcedRoleView.value ? normalizedForcedRoleView.value === 'student' : hasStudentRole.value)
const teacherActiveTab = ref('publish')
const studentActiveTab = ref('homework')
const publishType = ref('homework')

const homeworkForm = reactive({ title: '', className: '', content: '', publishMode: 'text', fileUrl: '' })
const examForm = reactive({ title: '', className: '', totalScore: 100 })
const classOptions = ['数据科学与大数据技术', '网络工程']

const teacherHomework = ref([])
const teacherSubmissions = ref([])
const teacherExam = ref([])
const teacherExamScores = ref([])
const teacherTasks = ref([])
const teacherScores = ref([])

const studentHomework = ref([])
const studentSubmissions = ref([])
const studentExams = ref([])
const studentExamScores = ref([])
const studentPerfScores = ref([])
const classScoreStats = computed(() => {
  const values = (studentPerfScores.value || [])
    .map(row => Number(row.exam_score))
    .filter(num => Number.isFinite(num))
  if (!values.length) {
    return { avg: null, max: null, min: null, avgText: '--', maxText: '--', minText: '--' }
  }
  const sum = values.reduce((a, b) => a + b, 0)
  const avg = sum / values.length
  const max = Math.max(...values)
  const min = Math.min(...values)
  return {
    avg,
    max,
    min,
    avgText: avg.toFixed(1),
    maxText: max.toFixed(0),
    minText: min.toFixed(0)
  }
})
const studentExamMerged = computed(() => {
  const scoreMap = new Map()
  ;(studentExamScores.value || []).forEach((item) => {
    const key = Number(item.exam_id || item.examId)
    if (!Number.isFinite(key)) return
    scoreMap.set(key, item)
  })
  const merged = (studentExams.value || []).map((exam) => {
    const examId = Number(exam.examId || exam.exam_id)
    const scoreRow = scoreMap.get(examId)
    const myScoreRaw = scoreRow?.score
    const myScore = myScoreRaw === null || myScoreRaw === undefined ? null : Number(myScoreRaw)
    return {
      examId,
      title: exam.title || '',
      className: exam.className || exam.class_name || '',
      totalScore: exam.totalScore || exam.total_score || 100,
      myScore: Number.isFinite(myScore) ? myScore : null,
      remark: scoreRow?.remark || '--',
      done: Number.isFinite(myScore)
    }
  })
  return merged
})
const studentHomeworkMerged = computed(() => {
  const submissionMap = new Map()
  ;(studentSubmissions.value || []).forEach((item) => {
    const key = item.homework_id || item.homeworkId
    if (!key || submissionMap.has(key)) return
    submissionMap.set(key, item)
  })

  return (studentHomework.value || []).map((homework) => {
    const key = homework.homeworkId || homework.homework_id
    const submission = submissionMap.get(key)
    const rawFeedback = String(submission?.feedback || '')
    const feedback = extractStudentResultFromFeedback(rawFeedback) || (submission ? '已提交，待老师批改' : '')
    const reviewImageUrl = extractReviewImageFromFeedback(rawFeedback)
    const isRejected = /\u9000\u56de|\u9a73\u56de|\u91cd\u505a/i.test(feedback)
    let statusLabel = '未提交'
    if (submission) {
      statusLabel = isRejected ? '待重做' : '已提交'
    }
    return {
      ...homework,
      score: submission?.score,
      feedback,
      reviewImageUrl,
      answer_content: submission?.answer_content || '',
      statusLabel
    }
  })
})

const chatContacts = ref([])
const chatGroups = ref([])
const chatKeyword = ref('')
const chatMode = ref('dm')
const activeChatTargetType = ref('dm')
const activeChatPeerId = ref('')
const activeChatGroupId = ref('')
const chatMessages = ref([])
const chatInput = ref('')
const chatListLoading = ref(false)
const chatSending = ref(false)
const chatBodyRef = ref(null)
const chatContactCount = ref(0)
const profileDialogVisible = ref(false)
const profileInfo = reactive({
  userName: '',
  nickName: '',
  roles: ''
})
const teacherExamTrendRef = ref(null)
const teacherHomeworkDistRef = ref(null)
const teacherTaskPieRef = ref(null)
const teacherRadarRef = ref(null)
const studentExamTrendRef = ref(null)
const studentHomeworkBarRef = ref(null)
const studentCompletionGaugeRef = ref(null)
const studentRadarRef = ref(null)
let teacherExamTrendChart = null
let teacherHomeworkDistChart = null
let teacherTaskPieChart = null
let teacherRadarChart = null
let studentExamTrendChart = null
let studentHomeworkBarChart = null
let studentCompletionGaugeChart = null
let studentRadarChart = null

const setTeacherExamTrendRef = (el) => { teacherExamTrendRef.value = el }
const setTeacherHomeworkDistRef = (el) => { teacherHomeworkDistRef.value = el }
const setTeacherTaskPieRef = (el) => { teacherTaskPieRef.value = el }
const setTeacherRadarRef = (el) => { teacherRadarRef.value = el }
const setStudentExamTrendRef = (el) => { studentExamTrendRef.value = el }
const setStudentHomeworkBarRef = (el) => { studentHomeworkBarRef.value = el }
const setStudentCompletionGaugeRef = (el) => { studentCompletionGaugeRef.value = el }
const setStudentRadarRef = (el) => { studentRadarRef.value = el }
const setChatBodyRef = (el) => { chatBodyRef.value = el }

const submitDialog = ref(false)
const submitForm = reactive({ homeworkId: '', answerContent: '', answerImageUrl: '' })
const reviewDialog = ref(false)
const reviewType = ref('homework')
const reviewForm = reactive({
  submissionId: '',
  scoreId: '',
  examId: '',
  studentId: '',
  studentName: '',
  title: '',
  answerContent: '',
  score: 0,
  maxScore: 100,
  feedback: '',
  reviewImageUrl: '',
  studentResultText: '',
  exampleAnswer: '',
  exampleScore: 85,
  exampleFeedback: ''
})
const reviewCanvasRef = ref(null)
const reviewCanvasTool = ref('check')
const reviewImageUrl = ref('')
let reviewCanvas = null
let reviewCanvasCtx = null
let reviewCanvasImage = null
const reviewCanvasMarks = ref([])
const examSubmitDialog = ref(false)
const examSubmitForm = reactive({ examId: '', examTitle: '', answerContent: '', answerImageUrl: '' })

const goTo = (path) => router.push(path)
const baseApi = import.meta.env.VITE_APP_BASE_API
const normalizedBase = String(baseApi || '').replace(/\/$/, '')
const aiFileProxyPrefix = '/education/ai'
const homeworkUploadTypes = computed(() => {
  if (homeworkForm.publishMode === 'word') return ['doc', 'docx']
  if (homeworkForm.publishMode === 'pdf') return ['pdf']
  return ['txt']
})
const answerImageTypes = ['png', 'jpg', 'jpeg', 'bmp', 'gif']
const reviewAnswerImageUrl = computed(() => {
  const reviewedUrl = resolveFileUrl(reviewForm.reviewImageUrl)
  if (reviewedUrl) return reviewedUrl
  return extractImageUrl(reviewForm.answerContent)
})
const REVIEW_IMAGE_MARKER = '[REVIEW_IMAGE]'
const TEACHER_REASON_START = '[TEACHER_REASON]'
const TEACHER_REASON_END = '[/TEACHER_REASON]'
const STUDENT_RESULT_START = '[STUDENT_RESULT]'
const STUDENT_RESULT_END = '[/STUDENT_RESULT]'
const aiImageGrading = ref(false)

const filteredChatContacts = computed(() => {
  const keyword = String(chatKeyword.value || '').trim().toLowerCase()
  if (!keyword) return chatContacts.value
  return (chatContacts.value || []).filter((item) => {
    const name = String(item?.nick_name || item?.user_name || item?.user_id || '').toLowerCase()
    return name.includes(keyword)
  })
})

const filteredChatGroups = computed(() => {
  const keyword = String(chatKeyword.value || '').trim().toLowerCase()
  if (!keyword) return chatGroups.value
  return (chatGroups.value || []).filter((item) => {
    const name = String(item?.groupName || item?.group_name || item?.groupId || '').toLowerCase()
    return name.includes(keyword)
  })
})
const activeChatTargetLabel = computed(() => {
  if (activeChatTargetType.value === 'group') {
    const target = (chatGroups.value || []).find((g) => String(g.groupId || g.group_id) === activeChatGroupId.value)
    return target ? String(target.groupName || target.group_name || '班级群聊') : ''
  }
  const peer = (chatContacts.value || []).find((item) => normalizeUserId(item.user_id) === activeChatPeerId.value)
  return peer ? String(peer.nick_name || `用户${peer.user_id}`) : ''
})

function buildHomeworkContent() {
  if (homeworkForm.publishMode === 'text') {
    return String(homeworkForm.content || '').trim()
  }
  const fileUrl = String(homeworkForm.fileUrl || '').split(',')[0]
  if (!fileUrl) return ''
  const modeLabel = homeworkForm.publishMode === 'word' ? 'WORD' : 'PDF'
  return `[${modeLabel}附件] ${fileUrl}`
}

function parseHomeworkAttachment(content) {
  const value = String(content || '').trim()
  const match = value.match(/^\[(WORD|PDF)附件\]\s+(.+)$/i)
  if (!match) return null
  return { type: String(match[1]).toUpperCase(), url: match[2] }
}

function hasHomeworkAttachment(content) {
  return !!parseHomeworkAttachment(content)
}
function openHomeworkAttachment(content) {
  const attachment = parseHomeworkAttachment(content)
  if (!attachment) {
    ElMessage.warning('当前作业没有可查看的附件')
    return
  }
  const url = resolveFileUrl(attachment.url)
  window.open(url, '_blank')
}

function resolveFileUrl(url) {
  const value = String(url || '').trim()
  if (!value) return ''
  if (value.startsWith('http')) return value
  if (value.startsWith('//')) return `${window.location.protocol}${value}`
  // 宸茬粡鍖呭惈缃戝叧鍓嶇紑鏃讹紝涓嶅啀閲嶅鎷兼帴
  if (/^\/(dev-api|prod-api|stage-api)\b/.test(value)) return value
  if (/^\/education\/ai\/files\//.test(value)) return value
  // AI 批改图片统一走 Spring Boot 代理
  if (/^\/?files\//.test(value)) {
    const normalizedAiPath = value.startsWith('/') ? value : `/${value}`
    return `${aiFileProxyPrefix}${normalizedAiPath}`
  }
  const normalizedPath = value.startsWith('/') ? value : `/${value}`
  if (normalizedBase && normalizedPath.startsWith(`${normalizedBase}/`)) {
    return normalizedPath
  }
  return `${normalizedBase}${normalizedPath}`
}
function openReviewImage(rawUrl, scene = 'unknown') {
  const resolved = resolveFileUrl(rawUrl)
  console.info('[Pad][ReviewImage] open', {
    scene,
    rawUrl: String(rawUrl || ''),
    resolvedUrl: resolved
  })
  if (!resolved) {
    ElMessage.warning('未找到批改图片，无法打开')
    return
  }
  window.open(resolved, '_blank')
}

function extractImageUrl(text) {
  const value = String(text || '')
  const markdownMatch = value.match(/!\[[^\]]*]\(([^)\s]+)\)/i)
  if (markdownMatch && markdownMatch[1]) {
    return resolveFileUrl(markdownMatch[1])
  }
  const plainMatch = value.match(/((https?:\/\/|\/)[^\s]+?\.(png|jpg|jpeg|gif|webp))/i)
  if (plainMatch && plainMatch[1]) {
    return resolveFileUrl(plainMatch[1])
  }
  return ''
}

function extractReviewImageFromFeedback(feedback) {
  const value = String(feedback || '')
  const reviewMarker = value.match(/\[REVIEW_IMAGE\]\(([^)\s]+)\)/i)
  if (reviewMarker?.[1]) return reviewMarker[1]
  const markdownImage = value.match(/!\[[^\]]*]\(([^)\s]+)\)/i)
  if (markdownImage?.[1]) return markdownImage[1]
  const filePath = value.match(/((https?:\/\/|\/)files\/[^\s)\]]+)/i)
  if (filePath?.[1]) return filePath[1]
  return ''
}

function stripReviewImageFromFeedback(feedback) {
  return String(feedback || '')
    .replace(/\n?\[REVIEW_IMAGE\]\(([^)\s]+)\)\s*/ig, '')
    .replace(/\n?!\[[^\]]*]\(([^)\s]+)\)\s*/ig, '')
    .trim()
}

function extractBlock(text, startTag, endTag) {
  const value = String(text || '')
  const start = value.indexOf(startTag)
  if (start < 0) return ''
  const from = start + startTag.length
  const end = value.indexOf(endTag, from)
  if (end < 0) return value.slice(from).trim()
  return value.slice(from, end).trim()
}

function stripBlock(text, startTag, endTag) {
  const value = String(text || '')
  const start = value.indexOf(startTag)
  if (start < 0) return value
  const end = value.indexOf(endTag, start + startTag.length)
  if (end < 0) return value.slice(0, start).trim()
  return `${value.slice(0, start)}${value.slice(end + endTag.length)}`.trim()
}

function extractTeacherReasonFromFeedback(feedback) {
  const reason = extractBlock(feedback, TEACHER_REASON_START, TEACHER_REASON_END)
  if (reason) return reason
  const plain = stripReviewImageFromFeedback(feedback)
  return stripBlock(stripBlock(plain, STUDENT_RESULT_START, STUDENT_RESULT_END), TEACHER_REASON_START, TEACHER_REASON_END)
}

function extractStudentResultFromFeedback(feedback) {
  return extractBlock(feedback, STUDENT_RESULT_START, STUDENT_RESULT_END)
}

function mergeTeacherStudentFeedback(teacherReason, studentResult) {
  const reason = String(teacherReason || '').trim()
  const student = String(studentResult || '').trim()
  const parts = []
  if (reason) parts.push(`${TEACHER_REASON_START}${reason}${TEACHER_REASON_END}`)
  if (student) parts.push(`${STUDENT_RESULT_START}${student}${STUDENT_RESULT_END}`)
  return parts.join('\n')
}

function mergeFeedbackWithReviewImage(feedback, reviewImageUrl) {
  const plain = String(feedback || '').trim()
  if (!reviewImageUrl) return plain
  return `${plain}${plain ? '\n' : ''}${REVIEW_IMAGE_MARKER}(${reviewImageUrl})`
}

function dataUrlToFile(dataUrl, fileName) {
  const parts = String(dataUrl || '').split(',')
  if (parts.length < 2) return null
  const mimeMatch = parts[0].match(/:(.*?);/)
  const mime = mimeMatch?.[1] || 'image/png'
  const byteString = window.atob(parts[1])
  const byteNumbers = new Array(byteString.length)
  for (let i = 0; i < byteString.length; i += 1) {
    byteNumbers[i] = byteString.charCodeAt(i)
  }
  const byteArray = new Uint8Array(byteNumbers)
  return new File([byteArray], fileName, { type: mime })
}

async function uploadReviewCanvasImage() {
  if (!reviewCanvas) return ''
  const dataUrl = reviewCanvas.toDataURL('image/png')
  const file = dataUrlToFile(dataUrl, `review-${Date.now()}.png`)
  if (!file) return ''
  const res = await uploadHomeworkAttachment(file)
  return String(res?.fileName || res?.url || '').trim()
}
async function fetchImageAsFile(imageUrl) {
  const url = resolveFileUrl(imageUrl)
  if (!url) return null
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const resp = await fetch(url, { headers })
  if (!resp.ok) throw new Error(`获取图片失败：${resp.status}`)
  const contentType = String(resp.headers.get('content-type') || '').toLowerCase()
  if (!contentType.startsWith('image/')) {
    throw new Error(`返回内容不是图片：${contentType || 'unknown'}`)
  }
  const blob = await resp.blob()
  if (!blob || blob.size <= 0) {
    throw new Error('图片内容为空，无法继续批改')
  }
  const ext = blob.type?.includes('png') ? 'png' : 'jpg'
  return new File([blob], `student-answer-${Date.now()}.${ext}`, { type: blob.type || 'image/jpeg' })
}
async function handleAiImageGrade() {
  const imageUrl = reviewAnswerImageUrl.value
  if (!imageUrl) {
    ElMessage.warning('当前没有可用于 AI 批改的答题图片')
    return
  }
  aiImageGrading.value = true
  try {
    const studentFile = await fetchImageAsFile(imageUrl)
    if (!studentFile) {
      ElMessage.warning('未能读取学生答题图片')
      return
    }
    const res = await aiGradeSingle({
      file: studentFile,
      rubric: reviewForm.exampleFeedback || reviewForm.exampleAnswer || '',
      maxScore: reviewForm.maxScore || 100
    })
    const teacherFeedback = String(res?.teacherFeedback || res?.feedback || '').trim()
    const studentFeedback = String(res?.studentFeedback || '').trim()
    reviewForm.feedback = teacherFeedback
    reviewForm.studentResultText = studentFeedback
    reviewForm.score = Number(res?.score ?? reviewForm.score)
    reviewForm.reviewImageUrl = String(res?.annotatedImageUrl || '').trim()
    if (reviewForm.reviewImageUrl) {
      reviewImageUrl.value = resolveFileUrl(reviewForm.reviewImageUrl)
      loadReviewCanvasImage(reviewImageUrl.value)
    }
    ElMessage.success('AI 图片批改已完成，请检查评分与评语')
  } catch (error) {
    ElMessage.error(error?.message || 'AI 图片批改失败')
  } finally {
    aiImageGrading.value = false
  }
}

async function handleAiSuggestReview() {
  const targetAnswer = String(reviewForm.answerContent || '').trim()
  if (!targetAnswer) {
    ElMessage.warning('请先填写学生作答内容')
    return
  }
  const res = await aiSuggestReview({
    exampleAnswer: reviewForm.exampleAnswer,
    exampleScore: reviewForm.exampleScore,
    exampleFeedback: reviewForm.exampleFeedback,
    targetAnswer,
    maxScore: reviewForm.maxScore || 100
  })
  reviewForm.score = Number(res.suggestedScore ?? reviewForm.score)
  reviewForm.feedback = res.suggestedFeedback || reviewForm.feedback
  ElMessage.success(`AI 已生成批改建议，相似度 ${Number(res.similarity || 0).toFixed(2)}`)
}

function initReviewCanvas() {
  reviewCanvas = reviewCanvasRef.value
  if (!reviewCanvas) return
  reviewCanvasCtx = reviewCanvas.getContext('2d')
  drawReviewCanvas()
}
function drawReviewCanvas() {
  if (!reviewCanvas || !reviewCanvasCtx) return
  reviewCanvasCtx.clearRect(0, 0, reviewCanvas.width, reviewCanvas.height)
  reviewCanvasCtx.fillStyle = '#f8fafc'
  reviewCanvasCtx.fillRect(0, 0, reviewCanvas.width, reviewCanvas.height)
  if (reviewCanvasImage) {
    reviewCanvasCtx.drawImage(reviewCanvasImage, 0, 0, reviewCanvas.width, reviewCanvas.height)
  } else {
    reviewCanvasCtx.fillStyle = '#64748b'
    reviewCanvasCtx.font = '16px sans-serif'
    reviewCanvasCtx.fillText('当前没有批改图，完成 AI 批改后会显示标注结果', 24, 36)
  }
  for (const mark of reviewCanvasMarks.value) {
    drawReviewMark(mark)
  }
}
function drawReviewMark(mark) {
  if (!reviewCanvasCtx) return
  const { x, y, type, text } = mark
  reviewCanvasCtx.lineWidth = 3
  if (type === 'check') {
    reviewCanvasCtx.strokeStyle = '#16a34a'
    reviewCanvasCtx.beginPath()
    reviewCanvasCtx.moveTo(x - 10, y)
    reviewCanvasCtx.lineTo(x - 2, y + 10)
    reviewCanvasCtx.lineTo(x + 14, y - 10)
    reviewCanvasCtx.stroke()
    return
  }
  if (type === 'cross') {
    reviewCanvasCtx.strokeStyle = '#dc2626'
    reviewCanvasCtx.beginPath()
    reviewCanvasCtx.moveTo(x - 10, y - 10)
    reviewCanvasCtx.lineTo(x + 10, y + 10)
    reviewCanvasCtx.moveTo(x + 10, y - 10)
    reviewCanvasCtx.lineTo(x - 10, y + 10)
    reviewCanvasCtx.stroke()
    return
  }
  reviewCanvasCtx.fillStyle = '#0f172a'
  reviewCanvasCtx.font = '16px sans-serif'
  reviewCanvasCtx.fillText(text || '批注', x, y)
}
function onReviewCanvasClick(event) {
  if (!reviewCanvas) return
  const rect = reviewCanvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  if (reviewCanvasTool.value === 'text') {
    const text = window.prompt('请输入批注内容')
    if (!text) return
    reviewCanvasMarks.value.push({ type: 'text', x, y, text })
  } else {
    reviewCanvasMarks.value.push({ type: reviewCanvasTool.value, x, y })
  }
  drawReviewCanvas()
}

function clearReviewCanvasMarks() {
  reviewCanvasMarks.value = []
  drawReviewCanvas()
}

function exportReviewCanvas() {
  if (!reviewCanvas) return
  const link = document.createElement('a')
  link.download = `review-${Date.now()}.png`
  link.href = reviewCanvas.toDataURL('image/png')
  link.click()
}

function loadReviewCanvasImage(url) {
  if (!url) return
  const image = new Image()
  image.crossOrigin = 'anonymous'
  image.onload = () => {
    reviewCanvasImage = image
    drawReviewCanvas()
  }
  image.onerror = () => {
    reviewCanvasImage = null
    drawReviewCanvas()
  }
  image.src = url
}

function handleReviewImageUpload(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    reviewImageUrl.value = String(reader.result || '')
    loadReviewCanvasImage(reviewImageUrl.value)
  }
  reader.readAsDataURL(file)
}

function loadTeacherPublishData() {
  if (publishType.value === 'exam') {
    loadTeacherExam()
    return
  }
  loadTeacherHomework()
}

async function handleCreateHomework() {
  const title = String(homeworkForm.title || '').trim()
  const className = String(homeworkForm.className || '').trim()
  const content = buildHomeworkContent()
  if (!title || !className) {
    ElMessage.warning('请填写作业标题和班级名称')
    return
  }
  if (!content) {
    ElMessage.warning(homeworkForm.publishMode === 'text' ? '请输入作业内容' : '请先上传作业附件')
    return
  }
  await createHomework({ title, className, content })
  ElMessage.success('作业发布成功')
  Object.assign(homeworkForm, { title: '', className: '', content: '', publishMode: 'text', fileUrl: '' })
  loadTeacherHomework()
}

async function loadTeacherHomework() {
  const res = await listTeacherHomework()
  teacherHomework.value = res.data || []
}

async function loadStudentHomework() {
  const res = await listStudentHomework()
  studentHomework.value = res.data || []
}

async function loadTeacherHomeworkSubmissions() {
  const res = await listTeacherHomeworkSubmissions()
  teacherSubmissions.value = res.data || []
}

function openHomeworkReview(row) {
  reviewType.value = 'homework'
  reviewForm.submissionId = row.submission_id
  reviewForm.scoreId = ''
  reviewForm.examId = ''
  reviewForm.studentId = row.student_id
  reviewForm.studentName = row.student_name || ''
  reviewForm.title = row.homework_title || ''
  reviewForm.answerContent = row.answer_content || ''
  reviewForm.score = Number(row.score ?? 0)
  reviewForm.maxScore = 100
  reviewForm.feedback = extractTeacherReasonFromFeedback(row.feedback || '')
  reviewForm.studentResultText = extractStudentResultFromFeedback(row.feedback || '')
  reviewForm.reviewImageUrl = extractReviewImageFromFeedback(row.feedback || '')
  reviewForm.exampleAnswer = row.answer_content || ''
  reviewForm.exampleScore = Number(row.score ?? 85)
  reviewForm.exampleFeedback = extractTeacherReasonFromFeedback(row.feedback || '')
  openReviewDialogWithAnswer()
}

async function loadStudentSubmissions() {
  const res = await listStudentHomeworkSubmissions()
  studentSubmissions.value = res.data || []
}

function openSubmit(row) {
  submitForm.homeworkId = row.homeworkId
  submitForm.answerContent = ''
  submitForm.answerImageUrl = ''
  submitDialog.value = true
}

function buildAnswerContentWithImage(textValue, imageValue) {
  const text = String(textValue || '').trim()
  const imageUrl = String(imageValue || '').split(',')[0]
  if (!imageUrl) return text
  if (!text) return `![答案图片](${imageUrl})`
  return `${text}\n![答案图片](${imageUrl})`
}

async function handleSubmitHomework() {
  const answerContent = buildAnswerContentWithImage(submitForm.answerContent, submitForm.answerImageUrl)
  console.info('[Pad][Homework] submit-start', {
    homeworkId: submitForm.homeworkId,
    answerTextLength: String(submitForm.answerContent || '').length,
    answerImageUrl: submitForm.answerImageUrl,
    payloadLength: String(answerContent || '').length
  })
  if (!answerContent) {
    ElMessage.warning('请填写作业答案或上传答题图片后再提交')
    return
  }
  try {
    const res = await submitHomework(submitForm.homeworkId, { answerContent })
    console.info('[Pad][Homework] submit-success', {
      homeworkId: submitForm.homeworkId,
      response: res
    })
    ElMessage.success('作业提交成功')
    submitDialog.value = false
    loadStudentSubmissions()
  } catch (error) {
    console.error('[Pad][Homework] submit-failed', {
      homeworkId: submitForm.homeworkId,
      answerImageUrl: submitForm.answerImageUrl,
      error
    })
    ElMessage.error(error?.message || '作业提交失败，请稍后重试')
  }
}

async function handleCreateExam() {
  await createExam(examForm)
  ElMessage.success('考试创建成功')
  Object.assign(examForm, { title: '', className: '', totalScore: 100 })
  loadTeacherExam()
}

async function loadTeacherExam() {
  const res = await listTeacherExam()
  teacherExam.value = res.data || []
}

async function loadTeacherExamScores() {
  const res = await listTeacherExamScore()
  teacherExamScores.value = res.data || []
}

function openExamReview(row) {
  reviewType.value = 'exam'
  reviewForm.submissionId = ''
  reviewForm.scoreId = row.score_id
  reviewForm.examId = row.exam_id
  reviewForm.studentId = row.student_id
  reviewForm.studentName = row.student_name || ''
  reviewForm.title = row.exam_title || ''
  reviewForm.answerContent = row.remark || ''
  reviewForm.maxScore = Number(row.total_score || 100)
  reviewForm.score = Number(row.score ?? 0)
  reviewForm.feedback = ''
  reviewForm.studentResultText = ''
  reviewForm.reviewImageUrl = ''
  reviewForm.exampleAnswer = row.remark || ''
  reviewForm.exampleScore = Number(row.score ?? Math.min(85, reviewForm.maxScore))
  reviewForm.exampleFeedback = ''
  openReviewDialogWithAnswer()
}

function openReviewDialogWithAnswer() {
  reviewDialog.value = true
  reviewCanvasMarks.value = []
  reviewImageUrl.value = ''
  reviewCanvasImage = null
  const reviewedImageUrl = resolveFileUrl(reviewForm.reviewImageUrl)
  const answerImageUrl = extractImageUrl(reviewForm.answerContent)
  const maybeImageUrl = reviewedImageUrl || answerImageUrl
  console.info('[Pad][Review] open-dialog', {
    type: reviewType.value,
    submissionId: reviewForm.submissionId,
    scoreId: reviewForm.scoreId,
    storedReviewImage: reviewForm.reviewImageUrl,
    reviewedImageUrl,
    answerImageUrl,
    baseCanvasImage: maybeImageUrl
  })
  nextTick(() => {
    initReviewCanvas()
    if (maybeImageUrl) {
      loadReviewCanvasImage(maybeImageUrl)
    } else {
      drawReviewCanvas()
    }
  })
}

async function handleSubmitReview() {
  if (reviewType.value === 'homework') {
    if (!reviewForm.submissionId) {
      ElMessage.warning('?????????????????')
      return
    }
    const hasCanvasDrawMarks = reviewCanvasMarks.value.length > 0
    const hasLocalCanvasImage = String(reviewImageUrl.value || '').startsWith('data:image/')
    let uploadedReviewImageUrl = String(reviewForm.reviewImageUrl || '').trim()
    if ((hasCanvasDrawMarks || hasLocalCanvasImage) && reviewCanvas) {
      uploadedReviewImageUrl = await uploadReviewCanvasImage()
    }
    const roleSeparatedFeedback = mergeTeacherStudentFeedback(reviewForm.feedback, reviewForm.studentResultText)
    const mergedFeedback = mergeFeedbackWithReviewImage(roleSeparatedFeedback, uploadedReviewImageUrl)
    console.info('[Pad][Review] submit-homework', {
      submissionId: reviewForm.submissionId,
      score: reviewForm.score,
      hasCanvasDrawMarks,
      hasLocalCanvasImage,
      uploadedReviewImageUrl,
      mergedFeedbackLength: String(mergedFeedback || '').length
    })
    await scoreHomeworkApi({
      submissionId: reviewForm.submissionId,
      score: reviewForm.score,
      feedback: mergedFeedback
    })
    ElMessage.success('作业批改成功')
    reviewDialog.value = false
    await loadTeacherHomeworkSubmissions()
    await loadChatData()
    return
  }
  if (!reviewForm.examId || !reviewForm.studentId) {
    ElMessage.warning('缺少考试或学生信息，无法提交评分')
    return
  }
  const remark = reviewForm.feedback
    ? `学生答案：${reviewForm.answerContent}\n教师评语：${reviewForm.feedback}`
    : reviewForm.answerContent
  await scoreExam({
    examId: reviewForm.examId,
    studentId: reviewForm.studentId,
    studentName: reviewForm.studentName,
    score: reviewForm.score,
    remark
  })
  ElMessage.success('考试评分成功')
  reviewDialog.value = false
  await loadTeacherExamScores()
}

async function loadStudentExamScore() {
  const res = await listStudentExamScore()
  studentExamScores.value = res.data || []
}

async function loadStudentExamList() {
  const res = await listStudentExamApi()
  studentExams.value = res.data || []
}

async function refreshStudentExamMerged() {
  await Promise.all([loadStudentExamList(), loadStudentExamScore(), loadStudentSelfScores()])
}

function openExamSubmit(row) {
  examSubmitForm.examId = row.examId
  examSubmitForm.examTitle = row.title
  examSubmitForm.answerContent = ''
  examSubmitForm.answerImageUrl = ''
  examSubmitDialog.value = true
}

async function handleSubmitExam() {
  const answerContent = buildAnswerContentWithImage(examSubmitForm.answerContent, examSubmitForm.answerImageUrl)
  if (!examSubmitForm.examId || !answerContent) {
    ElMessage.warning('请先完善考试答案内容')
    return
  }
  await submitExam(examSubmitForm.examId, { answerContent })
  ElMessage.success('考试提交成功')
  examSubmitDialog.value = false
  await refreshStudentExamMerged()
}

async function loadStudentSelfScores() {
  const res = await listStudentSelfScores()
  studentPerfScores.value = res.performanceScores || []
}

async function loadTeacherTasks() {
  const res = await listTeacherTasks()
  teacherTasks.value = res.data || []
}

async function loadTeacherScores() {
  const res = await listTeacherScores()
  teacherScores.value = res.data || []
}

function safeNum(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function ensureChart(instance, el) {
  if (!el) return null
  if (!instance) return echarts.init(el)
  return instance
}

function renderTeacherCharts() {
  teacherExamTrendChart = ensureChart(teacherExamTrendChart, teacherExamTrendRef.value)
  teacherHomeworkDistChart = ensureChart(teacherHomeworkDistChart, teacherHomeworkDistRef.value)
  teacherTaskPieChart = ensureChart(teacherTaskPieChart, teacherTaskPieRef.value)
  teacherRadarChart = ensureChart(teacherRadarChart, teacherRadarRef.value)
  if (!teacherExamTrendChart || !teacherHomeworkDistChart || !teacherTaskPieChart || !teacherRadarChart) return

  const examNames = (teacherExam.value || []).map((row, idx) => row.title || `考试${idx + 1}`)
  const examTotals = (teacherExam.value || []).map(row => safeNum(row.totalScore))

  const scoreBins = { '0-59': 0, '60-79': 0, '80-89': 0, '90+': 0 }
  ;(teacherSubmissions.value || []).forEach(row => {
    const s = safeNum(row.score)
    if (s >= 90) scoreBins['90+'] += 1
    else if (s >= 80) scoreBins['80-89'] += 1
    else if (s >= 60) scoreBins['60-79'] += 1
    else scoreBins['0-59'] += 1
  })

  const taskStatusMap = {}
  ;(teacherTasks.value || []).forEach(row => {
    const key = String(row.status || 'UNKNOWN').toUpperCase()
    taskStatusMap[key] = (taskStatusMap[key] || 0) + 1
  })
  const taskPieData = Object.keys(taskStatusMap).map(k => ({ name: k, value: taskStatusMap[k] }))

  const avgScore = (() => {
    if (!teacherScores.value.length) return 0
    const total = teacherScores.value.reduce((sum, row) => sum + safeNum(row.exam_score), 0)
    return total / teacherScores.value.length
  })()
  const doneTaskRate = teacherTasks.value.length
    ? Math.round((teacherTasks.value.filter(t => String(t.status || '').toUpperCase() === 'DONE').length / teacherTasks.value.length) * 100)
    : 0
  const gradedRate = teacherSubmissions.value.length
    ? Math.round((teacherSubmissions.value.filter(s => s.score !== null && s.score !== undefined).length / teacherSubmissions.value.length) * 100)
    : 0

  teacherExamTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: examNames.length ? examNames : ['暂无考试发布数据'] },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: examTotals.length ? examTotals : [0], areaStyle: {} }]
  })

  teacherHomeworkDistChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(scoreBins) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', barWidth: 26, data: Object.values(scoreBins) }]
  })

  teacherTaskPieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: taskPieData.length ? taskPieData : [{ name: '暂无任务', value: 1 }]
    }]
  })

  teacherRadarChart.setOption({
    radar: {
      indicator: [
        { name: '平均成绩', max: 100 },
        { name: '批改完成率', max: 100 },
        { name: '任务完成率', max: 100 },
        { name: '考试活跃度', max: 100 },
        { name: '作业活跃度', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          Number(avgScore.toFixed(1)),
          gradedRate,
          doneTaskRate,
          Math.min(100, teacherExam.value.length * 10),
          Math.min(100, teacherSubmissions.value.length * 5)
        ]
      }]
    }]
  })
}

function renderStudentCharts() {
  studentExamTrendChart = ensureChart(studentExamTrendChart, studentExamTrendRef.value)
  studentHomeworkBarChart = ensureChart(studentHomeworkBarChart, studentHomeworkBarRef.value)
  studentCompletionGaugeChart = ensureChart(studentCompletionGaugeChart, studentCompletionGaugeRef.value)
  studentRadarChart = ensureChart(studentRadarChart, studentRadarRef.value)
  if (!studentExamTrendChart || !studentHomeworkBarChart || !studentCompletionGaugeChart || !studentRadarChart) return

  const examNames = (studentExamScores.value || []).map((row, idx) => row.exam_title || `考试${idx + 1}`)
  const examScores = (studentExamScores.value || []).map(row => safeNum(row.score))
  const hwNames = (studentSubmissions.value || []).map((row, idx) => row.homework_title || `作业${idx + 1}`)
  const hwScores = (studentSubmissions.value || []).map(row => safeNum(row.score))

  const completionRate = studentHomework.value.length
    ? Math.round((studentSubmissions.value.length / studentHomework.value.length) * 100)
    : 0
  const avgExam = examScores.length ? examScores.reduce((a, b) => a + b, 0) / examScores.length : 0
  const avgHomework = hwScores.length ? hwScores.reduce((a, b) => a + b, 0) / hwScores.length : 0
  const perfAvg = studentPerfScores.value.length
    ? studentPerfScores.value.reduce((sum, row) => sum + safeNum(row.exam_score), 0) / studentPerfScores.value.length
    : 0

  studentExamTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: examNames.length ? examNames : ['暂无考试数据'] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'line', smooth: true, data: examScores.length ? examScores : [0], areaStyle: {} }]
  })

  studentHomeworkBarChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hwNames.length ? hwNames : ['暂无作业数据'] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: hwScores.length ? hwScores : [0] }]
  })

  studentCompletionGaugeChart.setOption({
    series: [{
      type: 'gauge',
      min: 0,
      max: 100,
      detail: { formatter: '{value}%' },
      data: [{ value: completionRate, name: '完成率' }]
    }]
  })

  studentRadarChart.setOption({
    radar: {
      indicator: [
        { name: '考试平均分', max: 100 },
        { name: '作业平均分', max: 100 },
        { name: '作业完成率', max: 100 },
        { name: '历史成绩均分', max: 100 },
        { name: '综合表现', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          Number(avgExam.toFixed(1)),
          Number(avgHomework.toFixed(1)),
          completionRate,
          Number(perfAvg.toFixed(1)),
          Math.min(100, (studentSubmissions.value.length + studentExamScores.value.length) * 8)
        ]
      }]
    }]
  })
}

async function refreshTeacherVisual() {
  await Promise.all([loadTeacherHomeworkSubmissions(), loadTeacherExam(), loadTeacherTasks(), loadTeacherScores()])
  await nextTick()
  renderTeacherCharts()
}

async function refreshStudentVisual() {
  await Promise.all([loadStudentHomework(), loadStudentSubmissions(), loadStudentExamScore(), loadStudentSelfScores()])
  await nextTick()
  renderStudentCharts()
}

async function handleTeacherTabChange(name) {
  if (name === 'score') {
    await loadTeacherExamScores()
  }
  if (name === 'visual') {
    await refreshTeacherVisual()
  }
  if (name === 'message') {
    await loadChatData()
  }
}

async function handleStudentTabChange(name) {
  if (name === 'exam') {
    await refreshStudentExamMerged()
  }
  if (name === 'visual') {
    await refreshStudentVisual()
  }
  if (name === 'message') {
    await loadChatData()
  }
}

function applyTabFromQuery() {
  const studentTab = String(route.query.studentTab || '').trim()
  if (isStudent.value && ['homework', 'exam', 'visual', 'message'].includes(studentTab)) {
    studentActiveTab.value = studentTab
  }
  const teacherTab = String(route.query.teacherTab || '').trim()
  if (isTeacher.value && ['publish', 'review', 'score', 'task', 'studentScore', 'visual', 'message'].includes(teacherTab)) {
    teacherActiveTab.value = teacherTab
  }
}

function normalizeUserId(value) {
  return String(value === null || value === undefined ? '' : value)
}

function isSelfChatMessage(item) {
  return Number(item?.sender_id || item?.senderId || 0) === Number(userStore.id || 0)
}

function scrollChatToBottom() {
  nextTick(() => {
    if (!chatBodyRef.value) return
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  })
}

async function loadChatData() {
  if (!(isTeacher.value || isStudent.value)) {
    chatContacts.value = []
    chatGroups.value = []
    activeChatPeerId.value = ''
    activeChatGroupId.value = ''
    chatMessages.value = []
    chatContactCount.value = 0
    activeChatTargetType.value = 'dm'
    return
  }
  try {
    chatListLoading.value = true
    const [contactRes, groupRes] = await Promise.all([listChatContacts(), listChatGroups()])
    chatContacts.value = contactRes.data || []
    chatGroups.value = groupRes.data || []
    chatContactCount.value = chatContacts.value.length
    const dmExists = chatContacts.value.some(item => normalizeUserId(item.user_id) === activeChatPeerId.value)
    const gpExists = chatGroups.value.some(item => String(item.groupId || item.group_id) === activeChatGroupId.value)
    if (!dmExists) {
      activeChatPeerId.value = chatContacts.value.length ? normalizeUserId(chatContacts.value[0].user_id) : ''
    }
    if (!gpExists) {
      activeChatGroupId.value = chatGroups.value.length ? String(chatGroups.value[0].groupId || chatGroups.value[0].group_id) : ''
    }
    if (chatMode.value === 'group' && activeChatGroupId.value) {
      activeChatTargetType.value = 'group'
    } else if (activeChatPeerId.value) {
      activeChatTargetType.value = 'dm'
    } else if (activeChatGroupId.value) {
      activeChatTargetType.value = 'group'
    }
    await loadChatMessageList()
  } catch (error) {
    chatContacts.value = []
    chatGroups.value = []
    activeChatPeerId.value = ''
    activeChatGroupId.value = ''
    chatMessages.value = []
    chatContactCount.value = 0
    ElMessage.error('聊天数据加载失败')
  } finally {
    chatListLoading.value = false
  }
}

function handleChatModeChange(mode) {
  if (mode === 'group') {
    activeChatTargetType.value = 'group'
    if (!activeChatGroupId.value && chatGroups.value.length) {
      activeChatGroupId.value = String(chatGroups.value[0].groupId || chatGroups.value[0].group_id)
    }
  } else {
    activeChatTargetType.value = 'dm'
    if (!activeChatPeerId.value && chatContacts.value.length) {
      activeChatPeerId.value = normalizeUserId(chatContacts.value[0].user_id)
    }
  }
  loadChatMessageList()
}

async function selectChatContact(contact) {
  const peerId = normalizeUserId(contact?.user_id)
  if (!peerId) return
  activeChatPeerId.value = peerId
  activeChatTargetType.value = 'dm'
  chatMode.value = 'dm'
  await loadChatMessageList()
}

async function selectChatGroup(group) {
  const groupId = String(group?.groupId || group?.group_id || '')
  if (!groupId) return
  activeChatGroupId.value = groupId
  activeChatTargetType.value = 'group'
  chatMode.value = 'group'
  await loadChatMessageList()
}

async function loadChatMessageList() {
  if (activeChatTargetType.value === 'group' && !activeChatGroupId.value) {
    chatMessages.value = []
    return
  }
  if (activeChatTargetType.value !== 'group' && !activeChatPeerId.value) {
    chatMessages.value = []
    return
  }
  chatListLoading.value = true
  try {
    const res = activeChatTargetType.value === 'group'
      ? await listGroupChatMessages(activeChatGroupId.value)
      : await listChatMessages(activeChatPeerId.value)
    chatMessages.value = res.data || []
    scrollChatToBottom()
  } catch (error) {
    chatMessages.value = []
    ElMessage.error('消息列表加载失败')
  } finally {
    chatListLoading.value = false
  }
}

async function sendChat() {
  const content = String(chatInput.value || '').trim()
  if (!activeChatTargetLabel.value) {
    ElMessage.warning('请先选择聊天对象')
    return
  }
  if (!content) {
    ElMessage.warning('请输入要发送的消息内容')
    return
  }
  chatSending.value = true
  try {
    if (activeChatTargetType.value === 'group') {
      await sendGroupChatMessage({
        groupId: activeChatGroupId.value,
        content
      })
    } else {
      await sendChatMessage({
        peerUserId: Number(activeChatPeerId.value),
        content
      })
    }
    chatInput.value = ''
  } catch (error) {
    ElMessage.error(error?.message || '消息发送失败')
  } finally {
    chatSending.value = false
  }
}


function openPrivateFromGroup(message) {
  const senderId = normalizeUserId(message?.sender_id || message?.senderId)
  if (!senderId || senderId === String(userStore.id || '')) return
  const target = (chatContacts.value || []).find((item) => normalizeUserId(item.user_id) === senderId)
  if (!target) return
  selectChatContact(target)
}

function openProfileDialog() {
  profileInfo.userName = userStore.name || ''
  profileInfo.nickName = userStore.nickName || ''
  profileInfo.roles = (roles.value || []).join(', ')
  profileDialogVisible.value = true
}
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }
  await userStore.logOut()
  router.push('/education/auth?redirect=/education/pad')
}

onMounted(() => {
  applyTabFromQuery()
  if (isTeacher.value) {
    loadTeacherHomework()
    loadTeacherHomeworkSubmissions()
    loadTeacherExam()
    loadTeacherExamScores()
    loadTeacherTasks()
    loadTeacherScores()
    loadChatData()
  }
  if (isStudent.value) {
    loadStudentHomework()
    loadStudentSubmissions()
    refreshStudentExamMerged()
    loadChatData()
  }
  window.addEventListener('resize', handleChartResize)
})

function handleChartResize() {
  ;[
    teacherExamTrendChart,
    teacherHomeworkDistChart,
    teacherTaskPieChart,
    teacherRadarChart,
    studentExamTrendChart,
    studentHomeworkBarChart,
    studentCompletionGaugeChart,
    studentRadarChart
  ].forEach(chart => chart && chart.resize())
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize)
  ;[
    teacherExamTrendChart,
    teacherHomeworkDistChart,
    teacherTaskPieChart,
    teacherRadarChart,
    studentExamTrendChart,
    studentHomeworkBarChart,
    studentCompletionGaugeChart,
    studentRadarChart
  ].forEach(chart => chart && chart.dispose())
})
</script>

<style scoped>
.pad-page { padding: 16px 16px 92px; }
.pad-header { margin-bottom: 16px; }
.pad-header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.pad-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.chat-header-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  min-height: 480px;
}
.chat-contact-pane {
  border: 1px solid #d7e7f3;
  border-radius: 10px;
  padding: 10px;
  overflow-y: auto;
  max-height: 560px;
  background: #f8fcff;
}
.chat-mode-bar {
  margin-bottom: 8px;
}
.chat-search-input {
  margin-bottom: 8px;
}
.chat-contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.chat-contact-item:hover {
  background: #edf7ff;
}
.chat-contact-item.active {
  background: #e3f1ff;
  border: 1px solid #b8d8f4;
}
.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #265d7f;
  background: linear-gradient(145deg, #e6f4ff 0%, #d7ecff 100%);
}
.chat-contact-text strong {
  display: block;
  color: #1f3346;
  font-size: 14px;
}
.chat-contact-text span {
  display: block;
  margin-top: 2px;
  color: #6a879d;
  font-size: 12px;
}
.chat-main-pane {
  border: 1px solid #d7e7f3;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  background: #f6fbff;
}
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #69859b;
}
.chat-message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  min-height: 300px;
}
.chat-main-top {
  padding: 10px 12px;
  border-bottom: 1px solid #d7e7f3;
  color: #35566e;
  font-weight: 600;
  background: #f9fcff;
}
.chat-message-row {
  display: flex;
  margin-bottom: 10px;
}
.chat-message-row.self {
  justify-content: flex-end;
}
.chat-bubble {
  max-width: 72%;
  background: #fff;
  border: 1px solid #dbe8f4;
  border-radius: 12px;
  padding: 8px 10px;
}
.chat-message-row.self .chat-bubble {
  background: #dff0ff;
  border-color: #b9d9f5;
}
.chat-bubble p {
  margin: 0;
  white-space: pre-wrap;
  color: #243c52;
  line-height: 1.45;
}
.chat-sender-name {
  display: inline-block;
  margin-bottom: 4px;
  color: #1884d8;
  cursor: pointer;
}
.chat-bubble span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #6b879d;
}
.chat-editor {
  border-top: 1px solid #d7e7f3;
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 86px;
  gap: 10px;
  align-items: end;
  background: #fff;
  border-radius: 0 0 10px 10px;
}
.visual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.chart-card {
  min-height: 320px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-box {
  height: 250px;
}
.review-canvas-panel {
  margin-top: 8px;
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  background: #ffffff;
}
.review-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #e5eaf3;
}
.review-canvas-wrap {
  padding: 12px;
}
.review-canvas {
  width: 100%;
  max-width: 100%;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  cursor: crosshair;
}
.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.94);
  border-top: 1px solid #e5e7eb;
  backdrop-filter: blur(8px);
}
@media (max-width: 992px) {
  .visual-grid {
    grid-template-columns: 1fr;
  }
  .chat-layout {
    grid-template-columns: 1fr;
  }
  .chat-contact-pane {
    max-height: 220px;
  }
}
</style>

