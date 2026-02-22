<template>
  <div class="score-container">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration bg-decoration-1"></div>
    <div class="bg-decoration bg-decoration-2"></div>
    <div class="bg-decoration bg-decoration-3"></div>
    
    <div class="page-header">
      <h2 class="page-title">学生成绩管理</h2>
      <p class="page-description">管理和查看学生的各科考试成绩信息</p>
    </div>

    <el-card class="main-card">
      <!-- 搜索表单 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="search-form">
        <el-form-item label="学生ID" prop="id">
          <el-input
            v-model="queryParams.id"
            placeholder="请输入学生ID"
            clearable
            class="search-input"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery" class="search-button">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery" class="reset-button">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <el-row :gutter="10" class="mb8 action-buttons">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
          >新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="Delete"
            @click="handleDelete"
            :disabled="multipleSelection.length === 0"
          >批量删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="studentPerformanceList"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        class="score-table"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" width="80" type="index" />
        <el-table-column prop="id" label="学生ID" align="center" width="100" />
        <el-table-column prop="gender" label="性别" align="center" width="100" />
        <el-table-column prop="hoursStudied" label="学习时长(h)" align="center" width="120" />
        <el-table-column prop="attendance" label="出勤率(%)" align="center" width="120" />
        <el-table-column prop="parentalInvolvement" label="家长参与度" align="center" width="140" />
        <el-table-column prop="accessToResources" label="资源获取" align="center" width="140" />
        <el-table-column prop="extracurricularActivities" label="课外活动" align="center" width="140" />
        <el-table-column prop="sleepHours" label="睡眠时间(h)" align="center" width="140" />
        <el-table-column prop="previousScores" label="历史成绩" align="center" width="120" />
        <el-table-column prop="motivationLevel" label="学习动力" align="center" width="120" />
        <el-table-column prop="internetAccess" label="网络访问" align="center" width="120" />
        <el-table-column prop="tutoringSessions" label="辅导次数" align="center" width="120" />
        <el-table-column prop="familyIncome" label="家庭收入" align="center" width="120" />
        <el-table-column prop="teacherQuality" label="教师质量" align="center" width="120" />
        <el-table-column prop="schoolType" label="学校类型" align="center" width="120" />
        <el-table-column prop="peerInfluence" label="同伴影响" align="center" width="120" />
        <el-table-column prop="physicalActivity" label="体育活动(h)" align="center" width="140" />
        <el-table-column prop="learningDisabilities" label="学习障碍" align="center" width="120" />
        <el-table-column prop="parentalEducationLevel" label="家长教育水平" align="center" width="160" />
        <el-table-column prop="distanceFromHome" label="家校距离" align="center" width="120" />
        <el-table-column prop="examScore" label="考试成绩" align="center" width="120">
          <template #default="scope">
            <el-tag :type="getScoreLevel(scope.row.examScore)" effect="light" class="score-tag">{{ scope.row.examScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" align="center" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              class="table-button"
            ></el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row.id)"
              class="table-button"
            ></el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
        class="pagination"
      />
    </el-card>
  
    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增学生成绩"
      width="600px"
      :before-close="cancelForm"
      class="score-dialog"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" class="form-container">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学习时长(h)" prop="hoursStudied">
              <el-input v-model="formData.hoursStudied" placeholder="请输入学习时长" type="number" min="0" max="24" class="form-input" />
            </el-form-item>
            <el-form-item label="出勤率(%)" prop="attendance">
              <el-input v-model="formData.attendance" placeholder="请输入出勤率" type="number" min="0" max="100" class="form-input" />
            </el-form-item>
            <el-form-item label="家长参与度" prop="parentalInvolvement">
              <el-select v-model="formData.parentalInvolvement" placeholder="请选择家长参与度" class="form-input">
                <el-option label="低" value="Low" />
                <el-option label="中" value="Medium" />
                <el-option label="高" value="High" />
              </el-select>
            </el-form-item>
            <el-form-item label="资源获取" prop="accessToResources">
              <el-select v-model="formData.accessToResources" placeholder="请选择资源获取情况" class="form-input">
                <el-option label="低" value="Low" />
                <el-option label="中" value="Medium" />
                <el-option label="高" value="High" />
              </el-select>
            </el-form-item>
            <el-form-item label="课外活动" prop="extracurricularActivities">
              <el-select v-model="formData.extracurricularActivities" placeholder="请选择是否参加课外活动" class="form-input">
                <el-option label="是" value="Yes" />
                <el-option label="否" value="No" />
              </el-select>
            </el-form-item>
            <el-form-item label="睡眠时间(h)" prop="sleepHours">
              <el-input v-model="formData.sleepHours" placeholder="请输入睡眠时间" type="number" min="0" max="24" class="form-input" />
            </el-form-item>
            <el-form-item label="历史成绩" prop="previousScores">
              <el-input v-model="formData.previousScores" placeholder="请输入历史成绩" type="number" min="0" max="100" class="form-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学习动力" prop="motivationLevel">
              <el-select v-model="formData.motivationLevel" placeholder="请选择学习动力" class="form-input">
                <el-option label="低" value="Low" />
                <el-option label="中" value="Medium" />
                <el-option label="高" value="High" />
              </el-select>
            </el-form-item>
            <el-form-item label="网络访问" prop="internetAccess">
              <el-select v-model="formData.internetAccess" placeholder="请选择是否有网络访问" class="form-input">
                <el-option label="是" value="Yes" />
                <el-option label="否" value="No" />
              </el-select>
            </el-form-item>
            <el-form-item label="辅导次数" prop="tutoringSessions">
              <el-input v-model="formData.tutoringSessions" placeholder="请输入辅导次数" type="number" min="0" class="form-input" />
            </el-form-item>
            <el-form-item label="家庭收入" prop="familyIncome">
              <el-select v-model="formData.familyIncome" placeholder="请选择家庭收入水平" class="form-input">
                <el-option label="低" value="Low" />
                <el-option label="中" value="Medium" />
                <el-option label="高" value="High" />
              </el-select>
            </el-form-item>
            <el-form-item label="教师质量" prop="teacherQuality">
              <el-select v-model="formData.teacherQuality" placeholder="请选择教师质量" class="form-input">
                <el-option label="低" value="Low" />
                <el-option label="中" value="Medium" />
                <el-option label="高" value="High" />
              </el-select>
            </el-form-item>
            <el-form-item label="学校类型" prop="schoolType">
              <el-select v-model="formData.schoolType" placeholder="请选择学校类型" class="form-input">
                <el-option label="公立" value="Public" />
                <el-option label="私立" value="Private" />
              </el-select>
            </el-form-item>
            <el-form-item label="同伴影响" prop="peerInfluence">
              <el-select v-model="formData.peerInfluence" placeholder="请选择同伴影响" class="form-input">
                <el-option label="负面" value="Negative" />
                <el-option label="中性" value="Neutral" />
                <el-option label="正面" value="Positive" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="体育活动(h)" prop="physicalActivity">
              <el-input v-model="formData.physicalActivity" placeholder="请输入体育活动时长" type="number" min="0" max="24" class="form-input" />
            </el-form-item>
            <el-form-item label="学习障碍" prop="learningDisabilities">
              <el-select v-model="formData.learningDisabilities" placeholder="请选择是否有学习障碍" class="form-input">
                <el-option label="是" value="Yes" />
                <el-option label="否" value="No" />
              </el-select>
            </el-form-item>
            <el-form-item label="家长教育水平" prop="parentalEducationLevel">
              <el-select v-model="formData.parentalEducationLevel" placeholder="请选择家长教育水平" class="form-input">
                <el-option label="高中" value="HighSchool" />
                <el-option label="本科" value="Bachelor" />
                <el-option label="硕士" value="Master" />
                <el-option label="博士" value="Doctor" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家校距离" prop="distanceFromHome">
              <el-select v-model="formData.distanceFromHome" placeholder="请选择家校距离" class="form-input">
                <el-option label="近" value="Near" />
                <el-option label="中" value="Moderate" />
                <el-option label="远" value="Far" />
              </el-select>
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-select v-model="formData.gender" placeholder="请选择性别" class="form-input">
                <el-option label="男" value="Male" />
                <el-option label="女" value="Female" />
              </el-select>
            </el-form-item>
            <el-form-item label="考试成绩" prop="examScore">
              <el-input v-model="formData.examScore" placeholder="请输入考试成绩" type="number" min="0" max="100" class="form-input" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelForm" class="dialog-button">取消</el-button>
          <el-button type="primary" @click="submitForm" class="dialog-button dialog-primary-button">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listStudentPerformance, getStudentPerformance, addStudentPerformance, delStudentPerformance, batchDelStudentPerformance } from '@/api/education/index'
import { parseTime } from '@/utils/ruoyi'
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'

// 表格数据
const studentPerformanceList = ref([])
const loading = ref(false)
const total = ref(0)
const multipleSelection = ref([])
const showSearch = ref(true)

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  id: undefined
})

// 新增表单数据
const formData = reactive({
  hoursStudied: undefined,
  attendance: undefined,
  parentalInvolvement: 'Medium',
  accessToResources: 'High',
  extracurricularActivities: 'Yes',
  sleepHours: undefined,
  previousScores: 85,
  motivationLevel: 'High',
  internetAccess: 'Yes',
  tutoringSessions: undefined,
  familyIncome: 'Medium',
  teacherQuality: 'High',
  schoolType: 'Public',
  peerInfluence: 'Positive',
  physicalActivity: undefined,
  learningDisabilities: 'No',
  parentalEducationLevel: 'Bachelor',
  distanceFromHome: 'Near',
  gender: 'Male',
  examScore: 90
})

// 表单验证规则
const rules = {
  hoursStudied: [{ required: true, message: '请输入学习时长', trigger: 'blur' }],
  attendance: [{ required: true, message: '请输入出勤率', trigger: 'blur' }],
  sleepHours: [{ required: true, message: '请输入睡眠时间', trigger: 'blur' }],
  tutoringSessions: [{ required: true, message: '请输入辅导次数', trigger: 'blur' }],
  physicalActivity: [{ required: true, message: '请输入体育活动时长', trigger: 'blur' }],
  examScore: [{ required: true, message: '请输入考试成绩', trigger: 'blur' }]
}

// 新增弹窗相关
const dialogVisible = ref(false)
const formRef = ref(null)

// 获取成绩等级
const getScoreLevel = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  return 'danger'
}

// 获取数据列表
const getList = async () => {
  loading.value = true
  try {
    const res = await listStudentPerformance(queryParams)
    studentPerformanceList.value = res.rows
    total.value = res.total
  } catch (error) {
    ElMessage.error('获取数据失败')
    console.error('获取学生成绩列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleQuery = async () => {
  queryParams.pageNum = 1
  
  // 如果填写了id，则根据id查询单个数据
  if (queryParams.id) {
    loading.value = true
    try {
      const res = await getStudentPerformance(queryParams.id)
      // 检查响应结构，确保获取到正确的数据
      if (res && res.data) {
        studentPerformanceList.value = [res.data]
        total.value = 1
      } else {
        // 如果响应结构不符合预期，使用res作为数据
        studentPerformanceList.value = [res]
        total.value = 1
      }
    } catch (error) {
      ElMessage.error('获取数据失败')
      console.error('获取学生成绩失败:', error)
      // 搜索失败时，清空列表
      studentPerformanceList.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  } else {
    // 否则查询列表
    getList()
  }
}

// 重置
const resetQuery = () => {
  Object.assign(queryParams, {
    id: undefined
  })
  getList()
}

// 多选
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 新增
const handleAdd = () => {
  // 重置表单数据
  Object.assign(formData, {
    hoursStudied: undefined,
    attendance: undefined,
    parentalInvolvement: 'Medium',
    accessToResources: 'High',
    extracurricularActivities: 'Yes',
    sleepHours: undefined,
    previousScores: 85,
    motivationLevel: 'High',
    internetAccess: 'Yes',
    tutoringSessions: undefined,
    familyIncome: 'Medium',
    teacherQuality: 'High',
    schoolType: 'Public',
    peerInfluence: 'Positive',
    physicalActivity: undefined,
    learningDisabilities: 'No',
    parentalEducationLevel: 'Bachelor',
    distanceFromHome: 'Near',
    gender: 'Male',
    examScore: 90
  })
  
  // 重置表单验证
  if (formRef.value) {
    formRef.value.resetFields()
  }
  
  // 显示对话框
  dialogVisible.value = true
}

// 提交新增表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    // 提交数据到后端
    await addStudentPerformance(formData)
    
    ElMessage.success('新增成功')
    dialogVisible.value = false
    
    // 重新加载数据列表
    getList()
  } catch (error) {
    if (error === false) {
      // 表单验证失败，不显示错误消息
      return
    }
    ElMessage.error('新增失败，请稍后重试')
    console.error('新增学生成绩失败:', error)
  } finally {
    loading.value = false
  }
}

// 取消新增
const cancelForm = () => {
  dialogVisible.value = false
}

// 修改
const handleUpdate = (row) => {
  // 保留修改功能，后续可实现
  ElMessage.info('修改功能待实现')
}

// 删除
const handleDelete = async (id) => {
  // 保留删除功能，后续可实现
  ElMessage.info('删除功能待实现')
}

// 批量删除
const handleBatchDelete = async () => {
  // 保留批量删除功能，后续可实现
  ElMessage.info('批量删除功能待实现')
}

// 初始化
onMounted(() => {
  getList()
})
</script>

<style scoped>
/* 基础容器样式 */
.score-container {
  padding: 20px;
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 装饰性背景元素 */
.bg-decoration {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.2;
  z-index: 0;
}

.bg-decoration-1 {
  width: 300px;
  height: 300px;
  background: #4facfe;
  top: 10%;
  left: 5%;
  animation: float 15s ease-in-out infinite;
}

.bg-decoration-2 {
  width: 400px;
  height: 400px;
  background: #00f2fe;
  bottom: 10%;
  right: 5%;
  animation: float 18s ease-in-out infinite reverse;
}

.bg-decoration-3 {
  width: 250px;
  height: 250px;
  background: #fe4a49;
  top: 50%;
  right: 20%;
  animation: float 20s ease-in-out infinite;
}

/* 头部样式 */
.page-header {
  margin-bottom: 30px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin-bottom: 12px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  background: linear-gradient(90deg, #ffffff, #e0e0e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-description {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  max-width: 600px;
  margin: 0 auto;
}

/* 主卡片样式 */
.main-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
  overflow: hidden;
  animation: fadeIn 0.5s ease;
}

/* 添加卡片装饰条 */
.main-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2, #f093fb, #f5576c);
}

/* 搜索表单样式 */
.search-form {
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  margin-bottom: 20px;
}

.search-input {
  width: 240px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.search-button, .reset-button {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.search-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 操作按钮区域 */
.action-buttons {
  padding: 0 20px 20px;
}

/* 表格样式 */
.score-table {
  padding: 0 20px;
  margin-bottom: 20px;
  animation: fadeIn 0.5s ease;
}

:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table__header-wrapper) {
  border-radius: 12px 12px 0 0;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  font-weight: 600;
}

/* 成绩标签样式 */
.score-tag {
  border-radius: 16px;
  padding: 4px 12px;
  font-weight: 600;
}

/* 表格内按钮样式 */
.table-button {
  margin: 0 4px;
  transition: all 0.3s ease;
}

.table-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

/* 分页样式 */
.pagination {
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

/* 对话框样式 */
.score-dialog {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(90deg, #667eea, #764ba2);
  color: white;
  padding: 20px;
}

:deep(.el-dialog__title) {
  color: white;
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-dialog__close) {
  color: white;
}

:deep(.el-dialog__body) {
  padding: 25px;
}

/* 表单样式 */
.form-container {
  animation: fadeIn 0.5s ease;
}

.form-input {
  border-radius: 8px;
  transition: all 0.3s ease;
  width: 100%;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.4) !important;
  border-color: #667eea !important;
}

/* 对话框按钮样式 */
.dialog-button {
  border-radius: 8px;
  transition: all 0.3s ease;
  padding: 8px 20px;
}

.dialog-primary-button {
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s ease;
}

.dialog-primary-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  background: linear-gradient(90deg, #764ba2, #667eea);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, -20px);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .score-container {
    padding: 15px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .page-description {
    font-size: 14px;
  }
  
  .search-form {
    padding: 15px;
  }
  
  .search-input {
    width: 100%;
    margin-bottom: 10px;
  }
  
  :deep(.el-form--inline .el-form-item) {
    margin-right: 0;
    margin-bottom: 15px;
    width: 100%;
  }
  
  .action-buttons {
    padding: 0 15px 15px;
  }
  
  .score-table {
    padding: 0 15px;
    overflow-x: auto;
  }
  
  .pagination {
    padding: 15px;
  }
  
  :deep(.el-dialog__header),
  :deep(.el-dialog__body) {
    padding: 15px;
  }
  
  :deep(.el-col) {
    width: 100% !important;
  }
}

/* 宽屏优化 */
@media (min-width: 1200px) {
  .score-container {
    max-width: 1400px;
    margin: 0 auto;
  }
  
  .main-card {
    max-width: 1200px;
    margin: 0 auto;
  }
}
</style>