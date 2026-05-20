<template>
  <div class="teacher-grading-page">
    <div class="hero-card">
      <div>
        <p class="eyebrow">AI Grading</p>
        <h1>AI 作业 / 实验报告智能批改</h1>
        <p class="hero-text">
          教师可上传参考样卷后，选择单张精批或批量批改。
          系统会返回分数、教师反馈、学生反馈和标注结果，帮助老师更快完成作业与实验报告评阅。
        </p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="router.push('/education/teacher/pad?teacherTab=review')">返回教师 Pad</el-button>
        <el-button type="success" plain @click="resetResult">清空结果</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>参考样卷</span>
            </div>
          </template>
          <el-upload
            class="upload-box"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*,.pdf"
            :on-change="handleReferenceChange"
          >
            <div class="upload-tip">
              <strong>{{ referenceFileName || '拖拽或点击上传参考样卷' }}</strong>
              <p>支持图片或 PDF，上传后可作为 AI 批改参考。</p>
            </div>
          </el-upload>
          <el-button
            type="primary"
            :loading="referenceUploading"
            :disabled="!referenceFile"
            @click="submitReference"
          >
            上传参考样卷
          </el-button>
          <el-alert
            v-if="referenceId"
            type="success"
            :closable="false"
            show-icon
            class="status-alert"
            title="参考样卷已就绪，可继续上传学生文件进行批改。"
          />
        </el-card>

        <el-card class="panel-card quick-card">
          <template #header>
            <div class="panel-header">
              <span>批改参数</span>
            </div>
          </template>
          <el-form :model="gradingForm" label-width="92px">
            <el-form-item label="批改模式">
              <el-radio-group v-model="gradingForm.batchMode">
                <el-radio-button :label="false">单张精批</el-radio-button>
                <el-radio-button :label="true">批量批改</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="批改对象">
              <el-select v-model="gradingForm.mode" style="width: 100%">
                <el-option label="作业图片" value="homework" />
                <el-option label="实验报告" value="report" />
                <el-option label="试卷答题卡" value="exam" />
              </el-select>
            </el-form-item>
            <el-form-item label="总分">
              <el-input-number v-model="gradingForm.maxScore" :min="10" :max="200" />
            </el-form-item>
            <el-form-item label="题目数量">
              <el-input-number v-model="gradingForm.questionCount" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="评分标准">
              <el-input
                v-model="gradingForm.rubric"
                type="textarea"
                :rows="5"
                placeholder="如：概念正确、过程完整、结论准确；实验报告还需关注结构、数据分析和结论表达。"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>{{ gradingForm.batchMode ? '批量文件上传' : '学生文件上传' }}</span>
              <el-tag effect="plain">{{ gradingForm.batchMode ? '最多 10 份' : '单张结果预览' }}</el-tag>
            </div>
          </template>

          <el-upload
            v-if="!gradingForm.batchMode"
            class="upload-box"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*,.pdf"
            :on-change="handleStudentFileChange"
          >
            <div class="upload-tip">
              <strong>{{ studentFileName || '拖拽或点击上传学生作业 / 实验报告' }}</strong>
              <p>支持图片或 PDF，上传后可直接触发 AI 批改。</p>
            </div>
          </el-upload>

          <el-upload
            v-else
            class="upload-box"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*,.pdf"
            multiple
            :limit="10"
            :on-change="handleBatchFileChange"
          >
            <div class="upload-tip">
              <strong>{{ batchFileLabel }}</strong>
              <p>支持图片或 PDF 批量上传，系统会逐份返回批改结果。</p>
            </div>
          </el-upload>

          <div class="action-row">
            <el-button
              type="primary"
              :loading="gradingLoading"
              :disabled="gradingDisabled"
              @click="submitGrading"
            >
              {{ gradingForm.batchMode ? '开始批量批改' : '开始 AI 批改' }}
            </el-button>
            <el-button plain @click="router.push('/education/teacher/analysis')">先看学情分析</el-button>
            <el-button plain @click="router.push('/education/teacher/qa')">前往教学问答</el-button>
          </div>
        </el-card>

        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>批改结果</span>
              <el-tag v-if="!gradingForm.batchMode && resultSummary.scoreText" type="success" effect="plain">
                {{ resultSummary.scoreText }}
              </el-tag>
              <el-tag v-if="gradingForm.batchMode && batchResultList.length" type="success" effect="plain">
                共 {{ batchResultList.length }} 份
              </el-tag>
            </div>
          </template>

          <template v-if="!gradingForm.batchMode">
            <div v-if="resultSummary.hasResult" class="result-grid">
              <div class="result-box">
                <span>批改对象</span>
                <strong>{{ resultSummary.modeText }}</strong>
              </div>
              <div class="result-box">
                <span>AI 评分</span>
                <strong>{{ resultSummary.scoreText }}</strong>
              </div>
              <div class="result-box">
                <span>结果状态</span>
                <strong>{{ resultSummary.statusText }}</strong>
              </div>
            </div>
            <el-empty
              v-else
              description="上传学生文件后，这里会显示 AI 批改结果。"
              :image-size="84"
            />

            <div v-if="resultSummary.hasResult" class="feedback-grid">
              <div class="feedback-card">
                <h3>教师反馈</h3>
                <p>{{ resultSummary.teacherFeedback }}</p>
              </div>
              <div class="feedback-card">
                <h3>学生反馈</h3>
                <p>{{ resultSummary.studentFeedback }}</p>
              </div>
            </div>

            <el-image
              v-if="resultImageUrl"
              :src="resultImageUrl"
              fit="contain"
              class="result-image"
              :preview-src-list="[resultImageUrl]"
            />
          </template>

          <template v-else>
            <el-empty
              v-if="!batchResultList.length"
              description="开始批量批改后，这里会显示每份文件的结果。"
              :image-size="84"
            />
            <div v-else class="batch-result-list">
              <div
                v-for="item in batchResultList"
                :key="item.filename"
                class="batch-item"
              >
                <div class="batch-head">
                  <strong>{{ item.filename }}</strong>
                  <el-tag :type="item.status === 'success' ? 'success' : 'danger'" effect="plain">
                    {{ item.status === 'success' ? '成功' : '失败' }}
                  </el-tag>
                </div>
                <p class="batch-meta" v-if="item.status === 'success'">
                  分数：{{ item.score ?? '--' }} / {{ gradingForm.maxScore }}
                </p>
                <p class="batch-meta" v-else>
                  {{ item.message || '批改失败' }}
                </p>
                <p class="batch-feedback" v-if="item.teacherFeedback || item.feedback">
                  {{ item.teacherFeedback || item.feedback }}
                </p>
              </div>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { aiGradeBatch, aiGradeSingle, fetchAiGradingAsset, uploadAiReference } from '@/api/education/aiGrading'

const router = useRouter()
const referenceFile = ref(null)
const referenceFileName = ref('')
const referenceUploading = ref(false)
const referenceId = ref('')

const studentFile = ref(null)
const studentFileName = ref('')
const batchFiles = ref([])
const gradingLoading = ref(false)
const gradingResult = ref(null)
const batchResultList = ref([])
const resultImageUrl = ref('')
let previewObjectUrl = ''

const gradingForm = reactive({
  batchMode: false,
  mode: 'homework',
  rubric: '请优先关注答案正确性、过程完整性、表达清晰度，并区分教师反馈和学生反馈。',
  maxScore: 100,
  questionCount: 12
})

const modeMap = {
  homework: '作业图片',
  report: '实验报告',
  exam: '试卷答题卡'
}

const resultSummary = computed(() => {
  const raw = gradingResult.value || {}
  const score = Number(raw.score)
  const teacherFeedback = raw.teacher_feedback || raw.teacherFeedback || raw.feedback || '暂无教师反馈'
  const studentFeedback = raw.student_feedback || raw.studentFeedback || '暂无学生反馈'
  const statusText = raw.status || raw.message || '已完成批改'
  return {
    hasResult: Boolean(Object.keys(raw).length),
    modeText: modeMap[gradingForm.mode] || '学生作业',
    scoreText: Number.isFinite(score) ? `${score} / ${gradingForm.maxScore}` : '',
    statusText,
    teacherFeedback,
    studentFeedback
  }
})

const batchFileLabel = computed(() => {
  if (!batchFiles.value.length) return '拖拽或点击上传多份学生文件'
  if (batchFiles.value.length === 1) return batchFiles.value[0].name || '已选择 1 份文件'
  return `已选择 ${batchFiles.value.length} 份文件`
})

const gradingDisabled = computed(() => {
  return gradingForm.batchMode ? batchFiles.value.length === 0 : !studentFile.value
})

function handleReferenceChange(file) {
  referenceFile.value = file.raw || file
  referenceFileName.value = file.name || referenceFile.value?.name || ''
}

function handleStudentFileChange(file) {
  studentFile.value = file.raw || file
  studentFileName.value = file.name || studentFile.value?.name || ''
}

function handleBatchFileChange(file, files) {
  const deduped = []
  const seen = new Set()
  files.forEach((item) => {
    const raw = item.raw || item
    const key = `${item.name || raw?.name || ''}_${item.size || raw?.size || 0}`
    if (seen.has(key)) return
    seen.add(key)
    deduped.push(raw)
  })
  batchFiles.value = deduped.slice(0, 10)
}

async function submitReference() {
  if (!referenceFile.value) {
    ElMessage.warning('请先选择参考样卷')
    return
  }
  referenceUploading.value = true
  try {
    const res = await uploadAiReference(referenceFile.value)
    referenceId.value = res?.referenceId || res?.reference_id || res?.id || ''
    ElMessage.success('参考样卷上传成功')
  } finally {
    referenceUploading.value = false
  }
}

async function submitGrading() {
  gradingLoading.value = true
  try {
    if (gradingForm.batchMode) {
      await submitBatchGrading()
    } else {
      await submitSingleGrading()
    }
  } finally {
    gradingLoading.value = false
  }
}

async function submitSingleGrading() {
  if (!studentFile.value) {
    ElMessage.warning('请先上传学生文件')
    return
  }
  const res = await aiGradeSingle({
    file: studentFile.value,
    referenceId: referenceId.value,
    rubric: buildRubric(),
    maxScore: gradingForm.maxScore,
    questionCount: gradingForm.questionCount
  })
  batchResultList.value = []
  gradingResult.value = res || {}
  await setResultImage(res?.annotatedImageUrl || res?.annotated_image_url || res?.reviewImageUrl || res?.image_url || '')
  ElMessage.success('AI 批改完成')
}

async function submitBatchGrading() {
  if (!batchFiles.value.length) {
    ElMessage.warning('请先选择批量文件')
    return
  }
  clearPreviewObjectUrl()
  gradingResult.value = null
  const res = await aiGradeBatch({
    files: batchFiles.value,
    referenceId: referenceId.value,
    rubric: buildRubric(),
    maxScore: gradingForm.maxScore,
    questionCount: gradingForm.questionCount
  })
  batchResultList.value = Array.isArray(res?.results) ? res.results : []
  ElMessage.success(`批量批改完成，共返回 ${batchResultList.value.length} 份结果`)
}

function buildRubric() {
  return `${gradingForm.mode === 'report' ? '实验报告场景。' : ''}${gradingForm.rubric}`
}

async function setResultImage(url) {
  clearPreviewObjectUrl()
  if (!url) {
    resultImageUrl.value = ''
    return
  }
  const blob = await fetchAiGradingAsset(url)
  previewObjectUrl = URL.createObjectURL(blob)
  resultImageUrl.value = previewObjectUrl
}

function resetResult() {
  gradingResult.value = null
  batchResultList.value = []
  clearPreviewObjectUrl()
  studentFile.value = null
  studentFileName.value = ''
  batchFiles.value = []
}

function clearPreviewObjectUrl() {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl)
    previewObjectUrl = ''
  }
  resultImageUrl.value = ''
}

onBeforeUnmount(() => {
  clearPreviewObjectUrl()
})
</script>

<style scoped lang="scss">
.teacher-grading-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 26%),
    linear-gradient(180deg, #f7fbff 0%, #f7fff9 100%);
}

.hero-card,
.panel-card,
.result-box,
.feedback-card,
.batch-item {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
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
  color: #1d4ed8;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0 0 12px;
  font-size: 32px;
  color: #0f172a;
}

.hero-text {
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
}

.hero-actions,
.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-header,
.batch-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.upload-box {
  margin-bottom: 16px;
}

.upload-tip {
  text-align: center;
  color: #475569;
}

.upload-tip strong {
  display: block;
  margin-bottom: 6px;
  color: #0f172a;
}

.status-alert,
.quick-card {
  margin-top: 16px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.result-box,
.feedback-card,
.batch-item {
  padding: 16px;
}

.result-box span,
.batch-meta {
  display: block;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.result-box strong {
  color: #0f172a;
  font-size: 24px;
}

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.feedback-card h3 {
  margin: 0 0 10px;
  color: #0f172a;
}

.feedback-card p,
.batch-feedback {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

.batch-result-list {
  display: grid;
  gap: 12px;
}

.result-image {
  width: 100%;
  max-height: 360px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

@media (max-width: 768px) {
  .teacher-grading-page {
    padding: 16px;
  }

  .hero-card,
  .result-grid,
  .feedback-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
