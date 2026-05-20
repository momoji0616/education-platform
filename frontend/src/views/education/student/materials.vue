<template>
  <div class="student-materials-page">
    <section class="hero-card">
      <div class="hero-main">
        <p class="eyebrow">Learning Materials</p>
        <h1>课程资料智能整理与知识点提取</h1>
        <p class="hero-text">
          这一页不只是展示你上传过哪些资料，而是把课程资料变成可提问、可复习、可转化为练习的学习资源。
          你可以先看资料结构和知识点提取建议，再一键进入 RAG 做章节梳理、考点总结和针对性追问。
        </p>

        <div class="hero-tags">
          <el-tag effect="plain" type="success">RAG 资料底座</el-tag>
          <el-tag effect="plain" type="warning">知识点提取</el-tag>
          <el-tag effect="plain" type="primary">联动诊断与刷题</el-tag>
        </div>
      </div>

      <div class="hero-actions">
        <el-button plain @click="router.push('/education/student/pad')">返回学生 Pad</el-button>
        <el-button type="primary" :loading="loading" @click="loadDatasets">刷新资料</el-button>
      </div>
    </section>

    <el-row :gutter="16" class="overview-row">
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <span>资料数量</span>
          <strong>{{ datasetSummary.count }}</strong>
          <p>{{ datasetSummary.countTip }}</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <span>资料结构</span>
          <strong>{{ datasetSummary.typeText }}</strong>
          <p>{{ datasetSummary.typeTip }}</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="overview-card">
          <span>当前阶段</span>
          <strong>{{ datasetSummary.stage }}</strong>
          <p>{{ datasetSummary.stageTip }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>已整理资料</span>
              <el-tag effect="plain">{{ datasetRows.length }} 份</el-tag>
            </div>
          </template>

          <div v-if="datasetRows.length" class="dataset-list">
            <div v-for="item in datasetRows" :key="item.key" class="dataset-item">
              <div class="dataset-main">
                <strong>{{ item.name }}</strong>
                <p>{{ item.tip }}</p>
              </div>
              <div class="dataset-side">
                <el-tag :type="item.typeTag" effect="light">{{ item.fileType }}</el-tag>
                <span>{{ item.timeText }}</span>
              </div>
            </div>
          </div>

          <el-empty
            v-else
            description="还没有课程资料，建议先上传讲义、笔记或数据表格，再利用 RAG 进行知识梳理。"
            :image-size="88"
          />
        </el-card>

        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>知识点提取建议</span>
            </div>
          </template>

          <div class="keyword-grid">
            <div v-for="item in extractedTopics" :key="item.title" class="keyword-card">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
              <span>{{ item.action }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>推荐提问模板</span>
            </div>
          </template>

          <div class="prompt-list">
            <div v-for="item in prompts" :key="item.title" class="prompt-item">
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.prompt }}</p>
              </div>
              <el-button text type="primary" @click="askWithPrompt(item.prompt)">带入问答</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>下一步动作</span>
            </div>
          </template>

          <div class="next-actions">
            <el-button type="primary" plain @click="goToRagWithPrompt('请根据我上传的课程资料，按章节整理核心知识点、定义、公式和常见题型。')">
              去 RAG 梳理资料
            </el-button>
            <el-button type="success" plain @click="router.push('/education/student/plan')">结合学习规划安排复习</el-button>
            <el-button type="warning" plain @click="router.push('/education/student/report')">结合诊断锁定薄弱点</el-button>
            <el-button type="info" plain @click="router.push('/education/student/practice')">把资料转成练习任务</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDatasets } from '@/api/education/rag'

const router = useRouter()
const loading = ref(false)
const datasets = ref([])

function parseFileType(name = '') {
  const lower = String(name).toLowerCase()
  if (lower.endsWith('.xlsx') || lower.endsWith('.xls')) return '表格资料'
  if (lower.endsWith('.txt')) return '文本资料'
  return '课程资料'
}

function fileTypeTag(type) {
  if (type === '表格资料') return 'success'
  if (type === '文本资料') return 'warning'
  return 'info'
}

function formatTime(value) {
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未知'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const datasetRows = computed(() =>
  (datasets.value || []).map((item, index) => {
    const name = item.file_name || item.filename || item.name || `资料 ${index + 1}`
    const fileType = parseFileType(name)
    return {
      key: item.id || item.dataset_id || `${name}-${index}`,
      name,
      fileType,
      typeTag: fileTypeTag(fileType),
      timeText: formatTime(item.upload_time || item.created_at),
      tip: fileType === '表格资料'
        ? '适合整理知识结构、样本数据、任务安排或对照关系。'
        : fileType === '文本资料'
          ? '适合提取概念、原理、定义、重点考点和易错点。'
          : '可进一步进入 RAG 做知识梳理、考点归纳和问题解释。'
    }
  })
)

const datasetSummary = computed(() => {
  const count = datasetRows.value.length
  const hasSheet = datasetRows.value.some(item => item.fileType === '表格资料')
  const hasText = datasetRows.value.some(item => item.fileType === '文本资料')
  let typeText = '尚未上传'
  if (hasSheet && hasText) typeText = '表格 + 文本'
  else if (hasSheet) typeText = '表格为主'
  else if (hasText) typeText = '文本为主'

  const stage = count >= 3 ? '适合深度整理' : count > 0 ? '可先做基础提取' : '等待补充资料'
  return {
    count,
    countTip: count ? '资料越完整，RAG 回答越稳定，也更适合知识点归纳。' : '建议先上传讲义、笔记、课程说明或数据表格。',
    typeText,
    typeTip: count ? '不同资料结构适合不同提取方式，可组合使用。' : '当前还没有可分析的资料结构。',
    stage,
    stageTip: count >= 3
      ? '已经具备做章节总结、重点提取和专题问答的基础。'
      : count > 0
        ? '可以先做概念提取和资料梳理，再逐步补充。'
        : '至少上传一份课程资料后再开始问答与提取。'
  }
})

const extractedTopics = computed(() => {
  if (!datasetRows.value.length) {
    return [
      {
        title: '先建立资料底座',
        description: '建议优先上传讲义、课堂笔记、课程要求或表格资料，形成 RAG 的知识基础。',
        action: '完成上传后，这里会自动给出知识点提取和提问建议。'
      }
    ]
  }

  const topics = [
    {
      title: '核心概念梳理',
      description: '优先按章节提取定义、原理、公式、算法步骤和重点术语，帮助你快速建立知识框架。',
      action: '建议提问：请按章节帮我整理核心概念与关键知识点。'
    },
    {
      title: '高频考点归纳',
      description: '把资料中重复出现的重点内容整理成“常考点 + 易错点”卡片，便于考前复习。',
      action: '建议提问：请总结这批资料中的高频考点和常见误区。'
    },
    {
      title: '资料到练习的转化',
      description: '把资料中的知识点继续转成练习方向，形成“资料整理 -> 智能刷题”的闭环。',
      action: '建议提问：请根据这些资料生成一组基础练习和提高练习。'
    }
  ]

  if (datasetRows.value.some(item => item.fileType === '表格资料')) {
    topics.push({
      title: '结构化对照分析',
      description: '表格资料适合进一步做字段对照、样本规律识别和异常项归纳。',
      action: '建议提问：请按表格字段帮我总结重点、异常项和可解释结论。'
    })
  }

  return topics
})

const prompts = computed(() => [
  {
    title: '章节梳理',
    prompt: '请根据我上传的课程资料，按章节整理核心知识点、定义、公式和典型题型。'
  },
  {
    title: '重点提炼',
    prompt: '请帮我总结这些资料中的高频考点、常见误区，以及考前复习顺序。'
  },
  {
    title: '学习转化',
    prompt: '请根据这些资料生成一份适合我当前阶段的复习提纲，并给出后续练习建议。'
  }
])

async function loadDatasets() {
  loading.value = true
  try {
    const response = await getDatasets()
    const raw = response?.data || response
    datasets.value = raw?.datasets || raw?.data || raw || []
  } finally {
    loading.value = false
  }
}

function goToRagWithPrompt(prompt) {
  router.push({
    path: '/education/rag',
    query: {
      source: 'student-materials',
      question: prompt
    }
  })
}

function askWithPrompt(prompt) {
  goToRagWithPrompt(prompt)
}

onMounted(() => {
  loadDatasets()
})
</script>

<style scoped lang="scss">
.student-materials-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.16), transparent 26%),
    linear-gradient(180deg, #fff9f1 0%, #eef7ff 100%);
}

.hero-card,
.overview-card,
.panel-card,
.keyword-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  margin-bottom: 16px;
}

.hero-main {
  max-width: 760px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #b45309;
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
  color: #475569;
  line-height: 1.75;
}

.hero-tags,
.hero-actions,
.next-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-tags {
  margin-top: 16px;
}

.overview-row {
  margin-bottom: 16px;
}

.overview-card {
  padding: 20px;
}

.overview-card span {
  display: block;
  color: #b45309;
  font-size: 13px;
  margin-bottom: 10px;
}

.overview-card strong {
  display: block;
  font-size: 28px;
  color: #0f172a;
}

.overview-card p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.panel-card {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dataset-list,
.prompt-list,
.keyword-grid,
.next-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dataset-item,
.prompt-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  background: #fdfaf5;
}

.dataset-main p,
.prompt-item p,
.keyword-card p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
}

.dataset-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
  color: #64748b;
  font-size: 13px;
}

.keyword-card {
  padding: 16px;
}

.keyword-card span {
  display: block;
  margin-top: 8px;
  color: #b45309;
  font-size: 13px;
}

@media (max-width: 768px) {
  .student-materials-page {
    padding: 16px;
  }

  .hero-card,
  .dataset-item,
  .prompt-item {
    flex-direction: column;
  }

  .dataset-side {
    align-items: flex-start;
  }
}
</style>
