<template>
  <div class="admin-page">
    <section class="hero-card">
      <div class="hero-main">
        <p class="hero-eyebrow">Management Console</p>
        <h1>{{ text.pageTitle }}</h1>
        <p class="hero-desc">{{ text.pageDesc }}</p>
      </div>

      <div class="hero-actions">
        <el-button type="primary" @click="goUserManage('teacher', targetMajorName)">{{ text.manageTeacher }}</el-button>
        <el-button type="success" @click="goUserManage('student', targetMajorName)">{{ text.manageStudent }}</el-button>
        <el-button plain @click="downloadTemplate">{{ text.downloadTemplate }}</el-button>
        <el-button plain @click="handleLogout">{{ text.logout }}</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span>{{ text.majorCount }}</span>
        <strong>{{ displayOverview.majorCount }}</strong>
      </div>
      <div class="summary-card">
        <span>{{ text.teacherCount }}</span>
        <strong>{{ displayOverview.teacherCount }}</strong>
      </div>
      <div class="summary-card">
        <span>{{ text.studentCount }}</span>
        <strong>{{ displayOverview.studentCount }}</strong>
      </div>
      <div class="summary-card">
        <span>{{ text.realDataCount }}</span>
        <strong>{{ displayOverview.answerStudentCount }}</strong>
      </div>
    </section>

    <el-card class="upload-card" shadow="never">
      <div class="section-head">
        <div>
          <h2>{{ text.importTitle }}</h2>
          <p>{{ text.importDesc }}</p>
        </div>
      </div>

      <div class="upload-row">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept=".xlsx,.xls"
        >
          <el-button plain>{{ text.selectExcel }}</el-button>
        </el-upload>
        <div class="upload-name">{{ selectedFileName || text.noFile }}</div>
        <el-button type="primary" :disabled="!selectedFile" :loading="uploading" @click="submitImport">
          {{ text.importButton }}
        </el-button>
      </div>
    </el-card>

    <el-card class="major-card" shadow="never">
      <div class="section-head">
        <div>
          <h2>{{ text.currentMajorTitle }}</h2>
          <p>{{ text.currentMajorDesc }}</p>
        </div>
        <el-button plain :loading="loading" @click="loadOverview">{{ text.refresh }}</el-button>
      </div>

      <div v-if="!currentMajor && !loading" class="empty-wrap">
        <el-empty :description="text.emptyMajor" :image-size="88" />
      </div>

      <div v-else class="major-grid" v-loading="loading">
        <el-card class="major-item" shadow="hover">
          <div class="major-head">
            <div>
              <h3>{{ currentMajor?.className || targetMajorName }}</h3>
              <p>{{ text.majorCardDesc }}</p>
            </div>
            <el-tag type="info" effect="light">{{ currentMajor?.className || targetMajorName }}</el-tag>
          </div>

          <div class="major-metrics">
            <div class="metric-box">
              <span>{{ text.teacherShort }}</span>
              <strong>{{ currentMajor?.teacherCount || 0 }}</strong>
            </div>
            <div class="metric-box">
              <span>{{ text.studentShort }}</span>
              <strong>{{ currentMajor?.studentCount || 0 }}</strong>
            </div>
            <div class="metric-box">
              <span>{{ text.realDataShort }}</span>
              <strong>{{ currentMajor?.answerStudentCount || 0 }}</strong>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import {
  downloadStudentRealDataTemplate,
  getManagerOverview,
  importStudentRealData
} from '@/api/education/admin'

const router = useRouter()
const userStore = useUserStore()

const targetMajorName = '\u6570\u636e\u79d1\u5b66\u4e0e\u5927\u6570\u636e\u6280\u672f'

const text = {
  pageTitle: '\u7ba1\u7406\u7aef\u4e3b\u63a7\u53f0',
  pageDesc: '\u5f53\u524d\u9875\u9762\u4ec5\u5c55\u793a\u201c\u6570\u636e\u79d1\u5b66\u4e0e\u5927\u6570\u636e\u6280\u672f\u201d\u7684\u5b9e\u65f6\u6c47\u603b\u6570\u636e\uff0c\u4fbf\u4e8e\u76f4\u63a5\u67e5\u770b\u5f53\u524d\u4e13\u4e1a\u7684\u6559\u5e08\u6570\uff0c\u6ce8\u518c\u8d26\u53f7\u5b66\u751f\u6570\u4e0e\u771f\u5b9e\u7b54\u9898\u8bb0\u5f55\u5b66\u751f\u4eba\u6570\u3002',
  manageTeacher: '\u7ba1\u7406\u6559\u5e08',
  manageStudent: '\u7ba1\u7406\u5b66\u751f',
  downloadTemplate: '\u4e0b\u8f7d\u5bfc\u5165\u6a21\u677f',
  logout: '\u9000\u51fa\u767b\u5f55',
  majorCount: '\u4e13\u4e1a\u6570\u91cf',
  teacherCount: '\u6559\u5e08\u603b\u6570',
  studentCount: '\u6ce8\u518c\u8d26\u53f7\u5b66\u751f\u6570',
  realDataCount: '\u771f\u5b9e\u7b54\u9898\u8bb0\u5f55\u5b66\u751f\u4eba\u6570',
  importTitle: '\u771f\u5b9e\u6570\u636e\u5bfc\u5165',
  importDesc: '\u652f\u6301\u7edf\u4e00\u5bfc\u5165\u5b66\u751f\u771f\u5b9e\u6210\u7ee9\u6570\u636e\uff0c\u5bfc\u5165\u6210\u529f\u540e\u4f1a\u81ea\u52a8\u5237\u65b0\u5f53\u524d\u4e13\u4e1a\u7684\u7edf\u8ba1\u7ed3\u679c\u3002',
  selectExcel: '\u9009\u62e9 Excel \u6587\u4ef6',
  noFile: '\u5c1a\u672a\u9009\u62e9\u6587\u4ef6',
  importButton: '\u5bfc\u5165\u771f\u5b9e\u6570\u636e',
  currentMajorTitle: '\u5f53\u524d\u4e13\u4e1a\u5b9e\u65f6\u6570\u636e',
  currentMajorDesc: '\u4ec5\u4fdd\u7559\u201c\u6570\u636e\u79d1\u5b66\u4e0e\u5927\u6570\u636e\u6280\u672f\u201d\u7684\u5f53\u524d\u6570\u636e\uff0c\u5de6\u4fa7\u5c55\u793a\u6ce8\u518c\u8d26\u53f7\u5b66\u751f\u6570\uff0c\u53f3\u4fa7\u5c55\u793a\u771f\u5b9e\u7b54\u9898\u8bb0\u5f55\u5b66\u751f\u4eba\u6570\u3002',
  refresh: '\u5237\u65b0',
  emptyMajor: '\u5f53\u524d\u6682\u65e0\u201c\u6570\u636e\u79d1\u5b66\u4e0e\u5927\u6570\u636e\u6280\u672f\u201d\u7684\u53ef\u5c55\u793a\u6570\u636e',
  majorCardDesc: '\u6309\u5f53\u524d\u4e13\u4e1a\u6c47\u603b\u6559\u5e08\u6570\uff0c\u6ce8\u518c\u8d26\u53f7\u5b66\u751f\u6570\u4e0e\u771f\u5b9e\u7b54\u9898\u8bb0\u5f55\u5b66\u751f\u4eba\u6570\u3002',
  teacherShort: '\u6559\u5e08',
  studentShort: '\u6ce8\u518c\u8d26\u53f7\u5b66\u751f\u6570',
  realDataShort: '\u771f\u5b9e\u7b54\u9898\u8bb0\u5f55\u5b66\u751f\u4eba\u6570',
  needFile: '\u8bf7\u5148\u9009\u62e9\u8981\u5bfc\u5165\u7684 Excel \u6587\u4ef6',
  importSuccess: '\u771f\u5b9e\u6570\u636e\u5bfc\u5165\u6210\u529f'
}

const loading = ref(false)
const uploading = ref(false)
const selectedFile = ref(null)
const selectedFileName = ref('')
const overview = ref({
  majorCount: 0,
  majors: []
})

const majors = computed(() => overview.value.majors || [])

const currentMajor = computed(() => {
  return majors.value.find((item) => String(item.className || '').trim() === targetMajorName) || null
})

const displayOverview = computed(() => {
  const major = currentMajor.value
  if (!major) {
    return {
      majorCount: 0,
      teacherCount: 0,
      studentCount: 0,
      answerStudentCount: 0
    }
  }
  return {
    majorCount: 1,
    teacherCount: Number(major.teacherCount || 0),
    studentCount: Number(major.studentCount || 0),
    answerStudentCount: Number(major.answerStudentCount || 0)
  }
})

async function loadOverview() {
  loading.value = true
  try {
    const res = await getManagerOverview()
    overview.value = res.data || { majorCount: 0, majors: [] }
  } finally {
    loading.value = false
  }
}

function handleFileChange(file) {
  selectedFile.value = file.raw
  selectedFileName.value = file.name
}

async function submitImport() {
  if (!selectedFile.value) {
    ElMessage.warning(text.needFile)
    return
  }
  uploading.value = true
  try {
    await importStudentRealData(selectedFile.value)
    ElMessage.success(text.importSuccess)
    selectedFile.value = null
    selectedFileName.value = ''
    await loadOverview()
  } finally {
    uploading.value = false
  }
}

function downloadTemplate() {
  downloadStudentRealDataTemplate()
}

function goUserManage(roleType, className = '') {
  router.push({
    path: '/system/user',
    query: {
      eduRole: roleType,
      className
    }
  })
}

async function handleLogout() {
  await userStore.logOut()
  router.push('/education/auth?role=admin')
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped lang="scss">
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(145deg, #f4f8fc 0%, #eef6f4 48%, #f7f8ef 100%);
}

.hero-card,
.upload-card,
.major-card {
  border-radius: 24px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  margin-bottom: 18px;
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0f766e;
}

.hero-main h1 {
  margin: 0;
  font-size: 38px;
  color: #0f172a;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 760px;
  font-size: 16px;
  line-height: 1.9;
  color: #516174;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 22px 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.summary-card span {
  display: block;
  font-size: 15px;
  color: #64748b;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  color: #0f172a;
}

.upload-card,
.major-card {
  padding: 24px;
  margin-bottom: 18px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 0 0 10px;
  font-size: 28px;
  color: #0f172a;
}

.section-head p {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #64748b;
}

.upload-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
}

.upload-name {
  min-height: 52px;
  padding: 0 16px;
  border-radius: 16px;
  border: 1px dashed rgba(15, 23, 42, 0.12);
  background: #f8fafc;
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #475569;
}

.empty-wrap {
  padding: 26px 0 8px;
}

.major-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.major-item {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.major-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.major-head h3 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #0f172a;
}

.major-head p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.major-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-box {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(145deg, #f8fbff, #f3faf7);
}

.metric-box span {
  display: block;
  font-size: 14px;
  color: #64748b;
}

.metric-box strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f172a;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-page {
    padding: 14px;
  }

  .hero-card,
  .upload-row,
  .summary-grid,
  .major-metrics {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .section-head,
  .major-head {
    flex-direction: column;
  }

  .hero-main h1 {
    font-size: 30px;
  }
}
</style>
