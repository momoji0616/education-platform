<template>
  <div :class="['prediction-page', isTeacherView ? 'teacher-shell' : 'student-shell']">
    <aside v-if="showSideNav" class="side-nav">
      <div class="nav-title">{{ navTitle }}</div>
      <button
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: route.path === item.path }"
        @click="router.push(item.path)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </button>
    </aside>

    <main class="content-area">
      <section class="hero-card">
        <div>
          <p class="eyebrow">Prediction Lab</p>
          <h1>成绩预测</h1>
          <p class="hero-text">先用模型预测分数，再由 AI 结合历史做题、学生诊断和学习规划给出决定性结论与提升方案。</p>
        </div>
        <div class="hero-actions">
          <div class="hero-action-group">
            <el-button class="hero-action" plain @click="router.push(backHomePath)">{{ backHomeText }}</el-button>
            <el-button class="hero-action" plain @click="activeTab = 'train'">模型训练</el-button>
            <el-button class="hero-action hero-action--primary" type="primary" @click="activeTab = 'predict'">
              {{ isStudentView ? '模型 + AI 联合预测' : '模型预测' }}
            </el-button>
          </div>
        </div>
      </section>

      <el-alert
        v-if="isStudentView && profileTip"
        :title="profileTip"
        :type="studentPredictionProfile?.predictionReady ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="profile-alert"
      />

      <el-card class="panel-card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="1. 模型训练" name="train">
            <div class="panel-block">
              <el-alert title="请上传 CSV 数据集训练模型" type="info" :closable="false" show-icon>
                建议字段与 `StudentPerformanceFactors.csv` 一致，至少包含学习时长、出勤、过往成绩等关键变量。
              </el-alert>

              <el-form :model="trainForm" label-width="120px">
                <el-form-item label="训练文件">
                  <el-upload ref="uploadRef" :auto-upload="false" :on-change="handleFileChange" :file-list="fileList" accept=".csv">
                    <el-button type="primary" :icon="Upload">选择 CSV 文件</el-button>
                    <template #tip>
                      <div class="el-upload__tip">仅支持 `.csv`，建议文件大小不超过 5MB。</div>
                    </template>
                  </el-upload>
                  <el-alert v-if="selectedFileNotice" :title="selectedFileNotice" type="success" :closable="false" show-icon class="upload-success" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="trainingLoading" :disabled="!trainForm.file" @click="handleTrainModel">
                    <template #loading><el-icon><Loading /></el-icon> 训练中</template>
                    上传并训练模型
                  </el-button>
                </el-form-item>
              </el-form>

              <el-result
                v-if="trainResult"
                :icon="trainResult.status === 'success' ? 'success' : 'error'"
                :title="trainResult.status === 'success' ? '模型训练成功' : '模型训练失败'"
                :sub-title="trainResult.status === 'success' ? '当前模型已经可以用于成绩预测' : trainResult.message"
              >
                <template v-if="trainResult.status === 'success'" #extra>
                  <el-descriptions border :column="2">
                    <el-descriptions-item label="平均绝对误差">{{ formatMetric(trainResult.test_mae) }}</el-descriptions-item>
                    <el-descriptions-item label="测试损失">{{ formatMetric(trainResult.test_loss) }}</el-descriptions-item>
                    <el-descriptions-item label="特征数量">{{ trainResult.feature_count || 0 }}</el-descriptions-item>
                    <el-descriptions-item label="模型状态">训练完成</el-descriptions-item>
                  </el-descriptions>
                </template>
              </el-result>
            </div>
          </el-tab-pane>

          <el-tab-pane label="2. 模型信息" name="info">
            <div class="panel-block">
              <div class="toolbar">
                <el-button type="primary" :loading="modelInfoLoading" @click="fetchModelInfo">
                  <template #loading><el-icon><Loading /></el-icon> 获取中</template>
                  获取模型信息
                </el-button>
              </div>

              <div v-if="modelInfo" class="model-info-wrap">
                <el-result
                  v-if="modelInfo.status !== 'success'"
                  icon="error"
                  title="获取模型信息失败"
                  :sub-title="modelInfo.message || '请先完成模型训练'"
                />
                <template v-else>
                  <el-card class="inner-card" shadow="never">
                    <template #header><span>网络结构</span></template>
                    <div class="layer-list">
                      <div v-for="layer in modelInfo?.model_info?.layers || []" :key="layer.name" class="layer-item">
                        <strong>{{ layer.name }}</strong>
                        <span>神经元：{{ layer.units }}</span>
                        <span>激活函数：{{ layer.activation }}</span>
                      </div>
                    </div>
                  </el-card>
                </template>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isStudentView ? '3. 模型 + AI 预测' : '3. 成绩预测'" name="predict">
            <div class="panel-block">
              <el-alert :title="isStudentView ? '填写变量后发起模型 + AI 联合预测' : '填写变量后即可预测成绩'" type="info" :closable="false" show-icon>
                学生端会优先读取系统里的真实学习画像做联合分析，给出更有针对性的结果。
              </el-alert>

              <el-form :model="predictForm" label-width="160px" class="predict-grid">
                <el-form-item label="学习时长（小时）"><el-input-number v-model="predictForm.input_data.Hours_Studied" :min="0" :max="100" /></el-form-item>
                <el-form-item label="出勤率（%）"><el-input-number v-model="predictForm.input_data.Attendance" :min="0" :max="100" /></el-form-item>
                <el-form-item label="睡眠时长（小时）"><el-input-number v-model="predictForm.input_data.Sleep_Hours" :min="0" :max="24" :step="0.5" /></el-form-item>
                <el-form-item label="以往成绩"><el-input-number v-model="predictForm.input_data.Previous_Scores" :min="0" :max="100" /></el-form-item>
                <el-form-item label="辅导次数"><el-input-number v-model="predictForm.input_data.Tutoring_Sessions" :min="0" :max="50" /></el-form-item>
                <el-form-item label="体育活动（小时/周）"><el-input-number v-model="predictForm.input_data.Physical_Activity" :min="0" :max="40" :step="0.5" /></el-form-item>
                <el-form-item label="家长参与度">
                  <el-select v-model="predictForm.input_data.Parental_Involvement">
                    <el-option label="低" value="Low" />
                    <el-option label="中" value="Medium" />
                    <el-option label="高" value="High" />
                  </el-select>
                </el-form-item>
                <el-form-item label="资源获取">
                  <el-select v-model="predictForm.input_data.Access_to_Resources">
                    <el-option label="低" value="Low" />
                    <el-option label="中" value="Medium" />
                    <el-option label="高" value="High" />
                  </el-select>
                </el-form-item>
                <el-form-item label="学习动力">
                  <el-select v-model="predictForm.input_data.Motivation_Level">
                    <el-option label="低" value="Low" />
                    <el-option label="中" value="Medium" />
                    <el-option label="高" value="High" />
                  </el-select>
                </el-form-item>
                <el-form-item label="家庭收入">
                  <el-select v-model="predictForm.input_data.Family_Income">
                    <el-option label="低" value="Low" />
                    <el-option label="中" value="Medium" />
                    <el-option label="高" value="High" />
                  </el-select>
                </el-form-item>
                <el-form-item label="教师质量">
                  <el-select v-model="predictForm.input_data.Teacher_Quality">
                    <el-option label="低" value="Low" />
                    <el-option label="中" value="Medium" />
                    <el-option label="高" value="High" />
                  </el-select>
                </el-form-item>
                <el-form-item label="学校类型">
                  <el-select v-model="predictForm.input_data.School_Type">
                    <el-option label="公立" value="Public" />
                    <el-option label="私立" value="Private" />
                  </el-select>
                </el-form-item>
                <el-form-item label="同伴影响">
                  <el-select v-model="predictForm.input_data.Peer_Influence">
                    <el-option label="积极" value="Positive" />
                    <el-option label="中性" value="Neutral" />
                    <el-option label="消极" value="Negative" />
                  </el-select>
                </el-form-item>
              </el-form>

              <div class="toolbar">
                <el-button type="primary" :loading="predictLoading" @click="handlePredictScore">
                  <template #loading><el-icon><Loading /></el-icon> 预测中</template>
                  {{ isStudentView ? '开始模型 + AI 联合预测' : '开始成绩预测' }}
                </el-button>
              </div>

              <div v-if="predictResult" class="result-wrap">
                <el-result
                  v-if="predictResult.status === 'success'"
                  icon="success"
                  :title="`预测成绩：${formatScore(predictResult.predicted_score)}`"
                  :sub-title="predictResult.message"
                >
                  <template #extra>
                    <div class="score-card">
                      <div class="score-circle">{{ formatScore(predictResult.predicted_score, 0) }}</div>
                      <el-tag :type="getScoreLevel(predictResult.predicted_score)" size="large">{{ getScoreDescription(predictResult.predicted_score) }}</el-tag>
                    </div>

                    <div v-if="isStudentView && predictResult.base_predicted_score !== undefined" class="prediction-ai-summary">
                      <div class="ai-score-card">
                        <span>基础模型分数</span>
                        <strong>{{ formatScore(predictResult.base_predicted_score) }}</strong>
                      </div>
                      <div class="ai-score-card ai-score-card--primary">
                        <span>最终判断分数</span>
                        <strong>{{ formatScore(predictResult.predicted_score) }}</strong>
                      </div>
                      <div class="ai-score-card">
                        <span>AI 置信度</span>
                        <strong>{{ predictResult.confidence || '中' }}</strong>
                      </div>
                    </div>

                    <div v-if="isStudentView && decisiveView" class="decisive-panel">
                      <div class="decisive-card">
                        <div class="decisive-badge">决定性结论</div>
                        <h2>{{ decisiveView.title }}</h2>
                        <p>{{ decisiveView.summary }}</p>
                        <div class="decisive-meta">
                          <span>{{ decisiveView.reason }}</span>
                          <strong>{{ decisiveView.target }}</strong>
                        </div>
                      </div>

                      <div class="decisive-actions">
                        <div v-for="item in decisiveView.actions" :key="item.index" class="strong-action-card">
                          <span class="action-index">{{ item.index }}</span>
                          <div>
                            <h3>{{ item.title }}</h3>
                            <p>{{ item.desc }}</p>
                          </div>
                        </div>
                      </div>

                      <div class="signal-strip">
                        <span class="signal-label">关键依据</span>
                        <p>{{ decisiveView.signal }}</p>
                      </div>
                    </div>
                  </template>
                </el-result>

                <el-result v-else icon="error" title="预测失败" :sub-title="predictResult.message || '请检查输入数据是否完整'" />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Upload } from '@element-plus/icons-vue'
import { trainPredictionModel, getModelInfo, predictScore, predictScoreWithAi } from '@/api/education/prediction'
import { getStudentRagProfile } from '@/api/education/student'

const route = useRoute()
const router = useRouter()

const activeTab = ref('train')
const trainingLoading = ref(false)
const modelInfoLoading = ref(false)
const predictLoading = ref(false)
const trainResult = ref(null)
const modelInfo = ref(null)
const predictResult = ref(null)
const studentPredictionProfile = ref(null)
const studentPredictionProfileLoading = ref(false)
const uploadRef = ref(null)
const fileList = ref([])
const trainForm = reactive({ file: null })
const predictForm = reactive({
  input_data: {
    Hours_Studied: 30,
    Attendance: 90,
    Sleep_Hours: 8,
    Previous_Scores: 75,
    Tutoring_Sessions: 2,
    Physical_Activity: 5,
    Parental_Involvement: 'Medium',
    Access_to_Resources: 'Medium',
    Motivation_Level: 'Medium',
    Family_Income: 'Medium',
    Teacher_Quality: 'High',
    School_Type: 'Public',
    Peer_Influence: 'Neutral',
    Extracurricular_Activities: 'Yes',
    Internet_Access: 'Yes',
    Learning_Disabilities: 'No',
    Parental_Education_Level: "Bachelor's",
    Distance_from_Home: 'Medium',
    Gender: 'Female'
  }
})

const isTeacherView = computed(() => route.path.includes('/education/teacher/'))
const isStudentView = computed(() => route.path.includes('/education/student/'))
const showSideNav = computed(() => isTeacherView.value || isStudentView.value)
const navTitle = computed(() => (isTeacherView.value ? '教师导航' : '学生导航'))
const backHomePath = computed(() => (isTeacherView.value ? '/education/teacher/pad' : '/education/student/pad'))
const backHomeText = computed(() => (isTeacherView.value ? '返回首页' : '返回学生首页'))

const navItems = computed(() => {
  if (isTeacherView.value) {
    return [
      { path: '/education/teacher/students', title: '学生管理', desc: '查看学生近期表现' },
      { path: '/education/teacher/analysis', title: '学情分析', desc: '按课程查看整体学情' },
      { path: '/education/teacher/paper', title: '智能组卷', desc: '生成针对性试卷' },
      { path: '/education/teacher/qa', title: '成绩预测', desc: '训练模型并预测成绩' },
      { path: '/education/teacher/assistant', title: '师生AI助手', desc: '进入教师问答场景' },
      { path: '/education/teacher/rag', title: 'RAG智能问答', desc: '结合知识库进行问答' }
    ]
  }
  return [
    { path: '/education/student/history', title: '历史做题', desc: '查看真实作答记录' },
    { path: '/education/student/report', title: '学生诊断', desc: '查看薄弱章节与知识点' },
    { path: '/education/student/plan', title: '学习规划', desc: '把诊断结果转成计划' },
    { path: '/education/student/practice', title: '智能刷题', desc: '围绕薄弱点练习' },
    { path: '/education/student/prediction', title: '成绩预测', desc: '模型 + AI 联合预测' },
    { path: '/education/student/assistant', title: '师生AI助手', desc: '进入互动问答场景' },
    { path: '/education/rag', title: 'RAG智能问答', desc: '拿着题目去问做题思路' }
  ]
})

const selectedFileNotice = computed(() => (trainForm.file ? `CSV 文件已选择：${trainForm.file.name}` : ''))
const profileTip = computed(() => {
  if (!isStudentView.value) return ''
  if (studentPredictionProfileLoading.value) return '正在加载学生端联合参考数据...'
  return studentPredictionProfile.value?.predictionTip || studentPredictionProfile.value?.missingReason || ''
})

const studentSignals = computed(() => {
  const profile = studentPredictionProfile.value || {}
  const diagnosis = profile.diagnosisOverview || {}
  const weakestChapter = profile.weakestChapter || {}
  const weakKnowledgePoint = Array.isArray(profile.weakKnowledgePoints) ? profile.weakKnowledgePoints[0] || {} : {}
  const practiceRecommendation = Array.isArray(profile.practiceRecommendations) ? profile.practiceRecommendations[0] || {} : {}
  const latestPrediction = profile.latestPrediction || {}
  return {
    correctRate: safeNumber(diagnosis.correctRate),
    recentCorrectRate: safeNumber(diagnosis.recentCorrectRate),
    chapterName: weakestChapter.chapterName || '当前薄弱章节',
    chapterRate: safeNumber(weakestChapter.correctRate),
    knowledgePoint: weakKnowledgePoint.knowledgePoint || '',
    knowledgeRate: safeNumber(weakKnowledgePoint.correctRate),
    recommendation: `${practiceRecommendation.chapterName || weakestChapter.chapterName || '当前薄弱模块'}${practiceRecommendation.knowledgePoint ? ` / ${practiceRecommendation.knowledgePoint}` : ''}`,
    latestPredictedScore: latestPrediction.predictedScore || ''
  }
})

const decisiveView = computed(() => {
  if (!isStudentView.value || predictResult.value?.status !== 'success') return null
  const score = safeNumber(predictResult.value?.predicted_score)
  const recentRate = studentSignals.value.recentCorrectRate
  const chapterRate = studentSignals.value.chapterRate
  const chapterName = studentSignals.value.chapterName
  const recommendation = studentSignals.value.recommendation
  const weakPoint = studentSignals.value.knowledgePoint

  let title = '当前成绩还有明显上升空间'
  let summary = `AI 判断你当前最该优先补的是“${chapterName}”，这会直接影响接下来成绩能不能稳住并继续提升。`
  let reason = `最近正确率 ${recentRate}% ，薄弱章节正确率 ${chapterRate}%`
  let target = `先把 ${chapterName} 提到 80% 左右`

  if (score >= 85) {
    title = '当前基础较稳，重点转向稳分和冲高'
    summary = `AI 判断你现在不是大面积补基础，而是要盯住“${chapterName}”这类失分点，避免高分段回落。`
    target = `把薄弱点失分压到最少，稳定冲向更高分`
  } else if (score < 70) {
    title = '当前最需要先补核心薄弱点'
    summary = `AI 判断当前失分不是零散问题，而是“${chapterName}”这类核心模块还没有补稳，先补这里最有效。`
    target = `先稳住基础分，再追求总分提升`
  }

  return {
    title,
    summary,
    reason,
    target,
    signal: [
      `最终预测 ${formatScore(score)} 分`,
      recentRate ? `近 7 天正确率 ${recentRate}%` : '',
      chapterRate ? `${chapterName} 正确率 ${chapterRate}%` : '',
      weakPoint ? `薄弱知识点：${weakPoint}` : '',
      studentSignals.value.latestPredictedScore ? `系统上一条预测记录 ${studentSignals.value.latestPredictedScore} 分` : ''
    ].filter(Boolean).join('，'),
    actions: [
      { index: '01', title: '先补最薄弱章节', desc: `优先围绕 ${recommendation} 做专项训练，不要同时铺开太多模块。` },
      { index: '02', title: '立刻复盘最近错题', desc: '把近期错题按同类错误集中复盘，再刷一轮同类型题验证是否真正改正。' },
      { index: '03', title: '带着问题继续问 AI', desc: `针对 ${chapterName}${weakPoint ? ` 和 ${weakPoint}` : ''}，继续让 RAG 解释概念、考法、易错点和解题步骤。` }
    ]
  }
})

function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function formatMetric(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(4) : '-'
}

function formatScore(value, digits = 2) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(digits) : '--'
}

function resolveErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.msg || error?.response?.data?.message || error?.response?.data?.detail || error?.message || fallback
}

function handleFileChange(uploadFile) {
  fileList.value = [uploadFile]
  trainForm.file = uploadFile.raw
  if (uploadFile?.raw?.name) ElMessage.success(`CSV 文件已选择：${uploadFile.raw.name}`)
}

async function loadStudentPredictionProfile() {
  if (!isStudentView.value) return
  studentPredictionProfileLoading.value = true
  try {
    const response = await getStudentRagProfile({})
    studentPredictionProfile.value = response?.data || response || null
  } catch (error) {
    studentPredictionProfile.value = null
  } finally {
    studentPredictionProfileLoading.value = false
  }
}

async function handleTrainModel() {
  if (!trainForm.file) return ElMessage.warning('请选择 CSV 文件')
  if (trainForm.file.type !== 'text/csv' && !trainForm.file.name.endsWith('.csv')) return ElMessage.warning('请上传 CSV 格式文件')
  trainingLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', trainForm.file)
    const response = await trainPredictionModel(formData)
    trainResult.value = response?.result || response || { status: 'error', message: '模型训练未返回结果' }
    trainResult.value.status === 'success' ? ElMessage.success('模型训练成功') : ElMessage.error(trainResult.value.message || '模型训练失败')
  } catch (error) {
    const message = resolveErrorMessage(error, '模型训练失败，请稍后重试')
    trainResult.value = { status: 'error', message }
    ElMessage.error(message)
  } finally {
    trainingLoading.value = false
    fileList.value = []
    trainForm.file = null
    if (uploadRef.value) uploadRef.value.clearFiles()
  }
}

async function fetchModelInfo() {
  modelInfoLoading.value = true
  try {
    const response = await getModelInfo()
    modelInfo.value = response
    modelInfo.value.status === 'success' ? ElMessage.success('获取模型信息成功') : ElMessage.error(modelInfo.value.message || '获取模型信息失败')
  } catch (error) {
    const message = resolveErrorMessage(error, '获取模型信息失败，请稍后重试')
    modelInfo.value = { status: 'error', message }
    ElMessage.error(message)
  } finally {
    modelInfoLoading.value = false
  }
}

async function handlePredictScore() {
  predictLoading.value = true
  try {
    const response = isStudentView.value ? await predictScoreWithAi(predictForm.input_data) : await predictScore(predictForm)
    predictResult.value = response
    if (predictResult.value.status === 'success') {
      ElMessage.success('预测成功')
      await loadStudentPredictionProfile()
    } else {
      ElMessage.error(predictResult.value.message || '预测失败')
    }
  } catch (error) {
    const message = resolveErrorMessage(error, '成绩预测失败，请稍后重试')
    predictResult.value = { status: 'error', message }
    ElMessage.error(message)
  } finally {
    predictLoading.value = false
  }
}

function getScoreLevel(score) {
  const num = Number(score)
  if (!Number.isFinite(num)) return 'info'
  if (num >= 90) return 'success'
  if (num >= 80) return 'primary'
  if (num >= 70) return 'warning'
  return 'danger'
}

function getScoreDescription(score) {
  const num = Number(score)
  if (!Number.isFinite(num)) return '待预测'
  if (num >= 90) return '优秀'
  if (num >= 80) return '良好'
  if (num >= 70) return '中等'
  if (num >= 60) return '及格'
  return '需提升'
}

onMounted(() => {
  if (isStudentView.value) loadStudentPredictionProfile()
})
</script>

<style scoped lang="scss">
.prediction-page { min-height: 100vh; display: grid; grid-template-columns: 284px minmax(0, 1fr); gap: 18px; padding: 24px; background: radial-gradient(circle at 14% 12%, rgba(2, 132, 199, 0.12) 0, transparent 30%), radial-gradient(circle at 86% 10%, rgba(15, 118, 110, 0.1) 0, transparent 28%), linear-gradient(150deg, #f3f7fb 0%, #edf6f4 52%, #f6f8ef 100%); }
.side-nav, .hero-card, .panel-card, .inner-card, .ai-score-card, .decisive-card, .strong-action-card, .signal-strip { border-radius: 22px; background: rgba(255,255,255,0.92); border: 1px solid rgba(15,23,42,0.08); box-shadow: 0 14px 36px rgba(15,23,42,0.08); }
.side-nav { position: sticky; top: 24px; align-self: start; padding: 20px; }
.nav-title { margin-bottom: 14px; font-size: 22px; font-weight: 800; color: #0f172a; }
.nav-item { width: 100%; display: flex; flex-direction: column; align-items: flex-start; gap: 8px; padding: 18px; margin-bottom: 12px; border: 1px solid transparent; border-radius: 18px; background: #f8fafc; cursor: pointer; }
.nav-item strong { font-size: 21px; color: #0f172a; }
.nav-item span { font-size: 17px; line-height: 1.55; color: #64748b; text-align: left; }
.nav-item:hover, .nav-item.active { border-color: rgba(15,118,110,0.18); background: linear-gradient(135deg, rgba(15,118,110,0.08), rgba(2,132,199,0.06)); }
.content-area { display: flex; flex-direction: column; gap: 18px; }
.hero-card { display: flex; justify-content: space-between; gap: 24px; padding: 28px; }
.eyebrow { margin: 0 0 10px; font-size: 18px; letter-spacing: 0.18em; text-transform: uppercase; color: #0f766e; }
.hero-card h1 { margin: 0; font-size: 46px; line-height: 1.14; color: #0f172a; }
.hero-text { margin: 14px 0 0; max-width: 960px; font-size: 22px; line-height: 1.8; color: #475569; }
.hero-action-group { display: flex; gap: 12px; padding: 8px; border-radius: 18px; background: rgba(248,250,252,0.95); }
.hero-action { min-width: 140px; height: 52px; border-radius: 14px; font-size: 20px; font-weight: 700; }
.hero-action--primary { min-width: 190px; }
.panel-card :deep(.el-card__body) { padding: 24px; }
.panel-block { display: flex; flex-direction: column; gap: 22px; }
.predict-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 20px; }
.toolbar { display: flex; justify-content: flex-end; }
.prediction-page :deep(.el-tabs__item) { height: 54px; font-size: 21px; font-weight: 700; }
.prediction-page :deep(.el-alert__title) { font-size: 20px; line-height: 1.55; }
.prediction-page :deep(.el-alert__description) { font-size: 17px; line-height: 1.6; }
.prediction-page :deep(.el-form-item__label) { font-size: 20px; font-weight: 700; color: #1f2937; }
.prediction-page :deep(.el-input__inner),
.prediction-page :deep(.el-select__placeholder),
.prediction-page :deep(.el-input-number__decrease),
.prediction-page :deep(.el-input-number__increase) { font-size: 18px; }
.prediction-page :deep(.el-button) { min-height: 46px; padding: 0 22px; font-size: 19px; font-weight: 700; }
.prediction-page :deep(.el-upload__tip) { margin-top: 14px; font-size: 17px; line-height: 1.6; color: #64748b; }
.model-info-wrap, .result-wrap { margin-top: 8px; }
.layer-list { display: flex; flex-direction: column; gap: 12px; }
.layer-item { display: flex; flex-wrap: wrap; gap: 18px; padding: 14px 16px; border-radius: 16px; background: #f8fafc; }
.score-card { display: flex; align-items: center; justify-content: center; gap: 18px; margin-bottom: 18px; }
.score-circle { width: 116px; height: 116px; display: grid; place-items: center; border-radius: 50%; background: linear-gradient(135deg, #0f766e, #0284c7); color: #fff; font-size: 38px; font-weight: 700; }
.prediction-ai-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin: 18px 0 20px; }
.ai-score-card { padding: 18px; text-align: left; }
.ai-score-card span { display: block; margin-bottom: 8px; font-size: 13px; color: #64748b; }
.ai-score-card strong { font-size: 30px; color: #0f172a; }
.ai-score-card--primary { background: linear-gradient(135deg, rgba(15,118,110,0.1), rgba(2,132,199,0.08)); border-color: rgba(15,118,110,0.16); }
.decisive-panel { display: flex; flex-direction: column; gap: 18px; margin-top: 8px; }
.decisive-card { padding: 28px; text-align: left; background: linear-gradient(135deg, #f8fffd, #f5fbff); border-color: rgba(15,118,110,0.18); }
.decisive-badge { display: inline-flex; padding: 6px 12px; border-radius: 999px; background: rgba(15,118,110,0.12); color: #0f766e; font-size: 13px; font-weight: 700; }
.decisive-card h2 { margin: 16px 0 10px; font-size: 32px; line-height: 1.3; color: #0f172a; }
.decisive-card p { margin: 0; font-size: 18px; line-height: 1.8; color: #334155; }
.decisive-meta { display: flex; justify-content: space-between; gap: 16px; margin-top: 18px; padding-top: 18px; border-top: 1px solid rgba(15,23,42,0.08); align-items: center; }
.decisive-meta span { font-size: 14px; color: #64748b; }
.decisive-meta strong { font-size: 18px; color: #0f766e; }
.decisive-actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.strong-action-card { display: grid; grid-template-columns: 56px 1fr; gap: 14px; padding: 22px; text-align: left; }
.action-index { width: 56px; height: 56px; display: grid; place-items: center; border-radius: 16px; background: linear-gradient(135deg, #0f766e, #0284c7); color: #fff; font-size: 18px; font-weight: 700; }
.strong-action-card h3 { margin: 2px 0 8px; font-size: 20px; color: #0f172a; }
.strong-action-card p { margin: 0; font-size: 15px; line-height: 1.8; color: #475569; }
.signal-strip { padding: 18px 22px; text-align: left; }
.signal-label { display: inline-block; margin-bottom: 8px; font-size: 13px; font-weight: 700; color: #0f766e; }
.signal-strip p { margin: 0; font-size: 15px; line-height: 1.8; color: #475569; }
@media (max-width: 1200px) { .prediction-page { grid-template-columns: 1fr; } .side-nav { position: static; } .predict-grid, .prediction-ai-summary, .decisive-actions { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .prediction-page { padding: 14px; } .hero-card { flex-direction: column; } .hero-card h1 { font-size: 30px; } .hero-action-group { width: 100%; flex-wrap: wrap; } .hero-action, .hero-action--primary { width: 100%; } .decisive-card h2 { font-size: 24px; } .decisive-card p { font-size: 16px; } .decisive-meta { flex-direction: column; align-items: flex-start; } .strong-action-card { grid-template-columns: 1fr; } }
</style>
