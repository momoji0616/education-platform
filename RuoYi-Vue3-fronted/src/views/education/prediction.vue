<template>
  <div class="prediction-container">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration bg-decoration-1"></div>
    <div class="bg-decoration bg-decoration-2"></div>
    <div class="bg-decoration bg-decoration-3"></div>
    
    <div class="prediction-header">
      <h2 class="page-title">学生成绩预测系统</h2>
      <p class="page-description">基于深度学习模型，预测学生的学习成绩表现</p>
    </div>

    <el-card class="main-card">
      <!-- 标签页切换 -->
      <el-tabs v-model="activeTab" class="prediction-tabs">
        <!-- 模型训练标签页 -->
        <el-tab-pane label="1. 模型训练" name="train">
          <div class="train-section">
            <el-alert
              title="训练须知"
              type="info"
              :closable="false"
              class="alert-section"
              show-icon
            >
              请上传CSV文件以训练预测模型。文件格式请参照StudentPerformanceFactors.csv，包含必要的学生特征数据。
            </el-alert>
            
            <el-form :model="trainForm" label-width="120px" class="train-form">
              <el-form-item label="CSV文件上传" prop="file">
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :file-list="fileList"
                  accept=".csv"
                  class="upload-demo"
                >
                  <el-button type="primary" :icon="upload">选择CSV文件</el-button>
                  <template #tip>
                    <div class="el-upload__tip">
                      请上传CSV格式文件，大小不超过5MB<br>
                      文件格式需包含以下字段：Hours_Studied, Attendance, Parental_Involvement, Access_to_Resources, Extracurricular_Activities, Sleep_Hours, Previous_Scores等
                    </div>
                  </template>
                </el-upload>
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="trainingLoading"
                  @click="handleTrainModel"
                  size="large"
                  class="train-button"
                  :disabled="!trainForm.file"
                >
                  <template #loading>
                    <el-icon><Loading /></el-icon> 训练中...
                  </template>
                  <el-icon><Upload /></el-icon> 上传并训练模型
                </el-button>
              </el-form-item>
            </el-form>

            <!-- 训练结果展示 -->
            <div v-if="trainResult" class="train-result">
              <el-divider content-position="left">训练结果</el-divider>
              <div v-if="trainResult.status === 'success'" class="success-result">
                <el-result
                  icon="success"
                  title="模型训练成功"
                  sub-title=""  
                >
                  <template #extra>
                    <el-descriptions border column="2" class="result-details">
                      <el-descriptions-item label="平均绝对误差(MAE)">{{ trainResult?.test_mae?.toFixed(4) }}</el-descriptions-item>
                      <el-descriptions-item label="测试损失(Loss)">{{ trainResult?.test_loss?.toFixed(4) }}</el-descriptions-item>
                      <el-descriptions-item label="特征数量">{{ trainResult?.feature_count }}</el-descriptions-item>
                      <el-descriptions-item label="模型状态">训练完成</el-descriptions-item>
                    </el-descriptions>
                    
                    <el-collapse class="feature-collapse">
                      <el-collapse-item title="查看所有特征" name="1">
                        <div class="feature-list">
                          <el-tag 
                            v-for="(feature, index) in trainResult?.feature_names" 
                            :key="index" 
                            class="feature-tag"
                            effect="plain"
                            type="info"
                          >
                            {{ feature }}
                          </el-tag>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </template>
                </el-result>
              </div>
              
              <div v-else class="error-result">
                <el-result
                  icon="error"
                  title="模型训练失败"
                  :sub-title="trainResult.message || '请检查文件路径是否正确'"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 模型信息标签页 -->
        <el-tab-pane label="2. 模型信息" name="info">
          <div class="info-section">
            <el-button
                  type="primary"
                  :loading="modelInfoLoading"
                  @click="fetchModelInfo"
                  size="large"
                  class="info-button"
                  :icon="modelInfoLoading ? '' : 'el-icon-document-checked'"
                >
                  <template #loading>
                    <el-icon><Loading /></el-icon> 获取中...
                  </template>
                  获取模型信息
                </el-button>
            
            <div v-if="modelInfo" class="model-info-result">
              <el-divider content-position="left">模型详细信息</el-divider>
              <div v-if="modelInfo.status === 'success'" class="success-info">
                <el-card class="model-card">
                  <template #header>
                    <div class="card-header">
                      <span>神经网络结构</span>
                    </div>
                  </template>
                  <div class="model-layers">
                    <div 
                      v-for="(layer, index) in modelInfo?.model_info?.layers" 
                      :key="index" 
                      class="model-layer"
                    >
                      <el-card :body-style="{ padding: '15px' }" class="layer-card">
                        <el-descriptions :column="3" :border="false">
                          <el-descriptions-item label="层名称">{{ layer.name }}</el-descriptions-item>
                          <el-descriptions-item label="神经元数量">{{ layer.units }}</el-descriptions-item>
                          <el-descriptions-item label="激活函数">{{ layer.activation }}</el-descriptions-item>
                        </el-descriptions>
                      </el-card>
                    </div>
                  </div>
                </el-card>
                
                <el-card class="feature-card">
                  <template #header>
                    <div class="card-header">
                      <span>分类特征列表</span>
                    </div>
                  </template>
                  <div class="categorical-features">
                    <el-tag 
                      v-for="(feature, index) in modelInfo?.model_info?.categorical_features" 
                      :key="index" 
                      class="feature-tag"
                      effect="plain"
                      type="success"
                    >
                      {{ feature }}
                    </el-tag>
                  </div>
                </el-card>
              </div>
              
              <div v-else class="error-info">
                <el-result
                  icon="error"
                  title="获取模型信息失败"
                  :sub-title="modelInfo.message || '请确保模型已成功训练'"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 成绩预测标签页 -->
        <el-tab-pane label="3. 成绩预测" name="predict">
          <div class="predict-section">
            <el-alert
              title="预测说明"
              type="info"
              :closable="false"
              class="alert-section"
              show-icon
            >
              请填写学生相关信息，系统将预测其可能的学习成绩。
              <br>
              <span style="color: #67C23A; font-weight: 500;">📝 当前表单已填充基于StudentPerformanceFactors.csv文件的示例数据</span>
            </el-alert>
            
            <el-form :model="predictForm" label-width="180px" class="predict-form">
              <!-- 数值类型输入 -->
              <el-form-item label="学习时长(小时)" prop="Hours_Studied">
                <el-input-number
                  v-model="predictForm.input_data.Hours_Studied"
                  :min="0"
                  :max="100"
                  :step="1"
                  class="number-input"
                />
              </el-form-item>
              
              <el-form-item label="出勤率(%)" prop="Attendance">
                <el-input-number
                  v-model="predictForm.input_data.Attendance"
                  :min="0"
                  :max="100"
                  :step="1"
                  class="number-input"
                />
              </el-form-item>
              
              <el-form-item label="睡眠时长(小时)" prop="Sleep_Hours">
                <el-input-number
                  v-model="predictForm.input_data.Sleep_Hours"
                  :min="0"
                  :max="24"
                  :step="0.5"
                  class="number-input"
                />
              </el-form-item>
              
              <el-form-item label="以往成绩" prop="Previous_Scores">
                <el-input-number
                  v-model="predictForm.input_data.Previous_Scores"
                  :min="0"
                  :max="100"
                  :step="1"
                  class="number-input"
                />
              </el-form-item>
              
              <el-form-item label="辅导次数" prop="Tutoring_Sessions">
                <el-input-number
                  v-model="predictForm.input_data.Tutoring_Sessions"
                  :min="0"
                  :max="50"
                  :step="1"
                  class="number-input"
                />
              </el-form-item>
              
              <el-form-item label="体育活动(小时/周)" prop="Physical_Activity">
                <el-input-number
                  v-model="predictForm.input_data.Physical_Activity"
                  :min="0"
                  :max="40"
                  :step="0.5"
                  class="number-input"
                />
              </el-form-item>
              
              <!-- 下拉选择类型 -->
              <el-form-item label="家长参与度" prop="Parental_Involvement">
                <el-select
                  v-model="predictForm.input_data.Parental_Involvement"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="低" value="Low" />
                  <el-option label="中" value="Medium" />
                  <el-option label="高" value="High" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="资源获取" prop="Access_to_Resources">
                <el-select
                  v-model="predictForm.input_data.Access_to_Resources"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="低" value="Low" />
                  <el-option label="中" value="Medium" />
                  <el-option label="高" value="High" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="课外活动" prop="Extracurricular_Activities">
                <el-radio-group v-model="predictForm.input_data.Extracurricular_Activities" class="radio-group">
                  <el-radio label="Yes">是</el-radio>
                  <el-radio label="No">否</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="学习动力" prop="Motivation_Level">
                <el-select
                  v-model="predictForm.input_data.Motivation_Level"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="低" value="Low" />
                  <el-option label="中" value="Medium" />
                  <el-option label="高" value="High" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="互联网访问" prop="Internet_Access">
                <el-radio-group v-model="predictForm.input_data.Internet_Access" class="radio-group">
                  <el-radio label="Yes">是</el-radio>
                  <el-radio label="No">否</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="家庭收入" prop="Family_Income">
                <el-select
                  v-model="predictForm.input_data.Family_Income"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="低" value="Low" />
                  <el-option label="中" value="Medium" />
                  <el-option label="高" value="High" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="教师质量" prop="Teacher_Quality">
                <el-select
                  v-model="predictForm.input_data.Teacher_Quality"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="低" value="Low" />
                  <el-option label="中" value="Medium" />
                  <el-option label="高" value="High" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="学校类型" prop="School_Type">
                <el-select
                  v-model="predictForm.input_data.School_Type"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="公立" value="Public" />
                  <el-option label="私立" value="Private" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="同伴影响" prop="Peer_Influence">
                <el-select
                  v-model="predictForm.input_data.Peer_Influence"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="积极" value="Positive" />
                  <el-option label="中性" value="Neutral" />
                  <el-option label="消极" value="Negative" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="学习障碍" prop="Learning_Disabilities">
                <el-radio-group v-model="predictForm.input_data.Learning_Disabilities" class="radio-group">
                  <el-radio label="Yes">是</el-radio>
                  <el-radio label="No">否</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="家长教育水平" prop="Parental_Education_Level">
                <el-select
                  v-model="predictForm.input_data.Parental_Education_Level"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="高中" value="High School" />
                  <el-option label="学士" value="Bachelor's" />
                  <el-option label="硕士" value="Master's" />
                  <el-option label="博士" value="PhD" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="家到学校距离" prop="Distance_from_Home">
                <el-select
                  v-model="predictForm.input_data.Distance_from_Home"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="近" value="Near" />
                  <el-option label="中等" value="Medium" />
                  <el-option label="远" value="Far" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="性别" prop="Gender">
                <el-select
                  v-model="predictForm.input_data.Gender"
                  placeholder="请选择"
                  class="select-input"
                >
                  <el-option label="男" value="Male" />
                  <el-option label="女" value="Female" />
                </el-select>
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="predictLoading"
                  @click="handlePredictScore"
                  size="large"
                  class="predict-button"
                  :icon="predictLoading ? '' : 'el-icon-calculator'"
                >
                  <template #loading>
                    <el-icon><Loading /></el-icon> 预测中...
                  </template>
                  预测成绩
                </el-button>
              </el-form-item>
            </el-form>

            <!-- 预测结果展示 -->
            <div v-if="predictResult" class="predict-result">
              <el-divider content-position="left">预测结果</el-divider>
              <div v-if="predictResult.status === 'success'" class="success-predict">
                <el-result
                  icon="success"
                  :title="`预测成绩: ${predictResult?.predicted_score?.toFixed(2)}`"
                  :sub-title="predictResult.message"
                >
                  <template #extra>
                    <el-card class="score-card">
                      <div class="score-display">
                        <div class="score-circle">
                          <div class="score-value">{{ predictResult?.predicted_score?.toFixed(0) }}</div>
                        </div>
                        <div class="score-assessment">
                          <el-tag :type="getScoreLevel(predictResult?.predicted_score)" size="large">
                            {{ getScoreDescription(predictResult?.predicted_score) }}
                          </el-tag>
                        </div>
                      </div>
                    </el-card>
                  </template>
                </el-result>
              </div>
              
              <div v-else class="error-predict">
                <el-result
                  icon="error"
                  title="预测失败"
                  :sub-title="predictResult.message || '请检查输入数据是否完整'"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Upload } from '@element-plus/icons-vue'
import { trainPredictionModel, getModelInfo, predictScore } from '@/api/education/prediction'

// 标签页状态
const activeTab = ref('train')

// 训练相关状态
const trainingLoading = ref(false)
const trainForm = reactive({
  file: null
})
const fileList = ref([])
const uploadRef = ref(null)
const trainResult = ref(null)

// 模型信息状态
const modelInfoLoading = ref(false)
const modelInfo = ref(null)

// 预测相关状态
const predictLoading = ref(false)
const predictForm = reactive({
  input_data: {
    Hours_Studied: 30,
    Attendance: 90,
    Parental_Involvement: "Medium",
    Access_to_Resources: "Medium",
    Extracurricular_Activities: "Yes",
    Sleep_Hours: 8,
    Previous_Scores: 75,
    Motivation_Level: "Medium",
    Internet_Access: "Yes",
    Tutoring_Sessions: 2,
    Family_Income: "Medium",
    Teacher_Quality: "High",
    School_Type: "Public",
    Peer_Influence: "Neutral",
    Physical_Activity: 5,
    Learning_Disabilities: "No",
    Parental_Education_Level: "Bachelor's",
    Distance_from_Home: "Medium",
    Gender: "Female"
  }
})
const predictResult = ref(null)

// 处理文件选择
const handleFileChange = (uploadFile) => {
  // 只保留最新选择的文件
  fileList.value = [uploadFile]
  trainForm.file = uploadFile.raw
}

// 训练模型 - 双层嵌套：response.result
const handleTrainModel = async () => {
  if (!trainForm.file) {
    ElMessage.warning('请选择CSV文件')
    return
  }
  
  // 检查文件类型
  if (trainForm.file.type !== 'text/csv' && !trainForm.file.name.endsWith('.csv')) {
    ElMessage.warning('请上传CSV格式的文件')
    return
  }
  
  trainingLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', trainForm.file)
    const response = await trainPredictionModel(formData)
    trainResult.value = response.result
    if (trainResult.value.status === 'success') {
      ElMessage.success(`✅ 文件上传成功！模型训练完成\n📊 平均绝对误差: ${trainResult.value.test_mae.toFixed(4)}\n📈 特征数量: ${trainResult.value.feature_count}`)
    } else {
      ElMessage.error(trainResult.value.message || '模型训练失败')
    }
  } catch (error) {
    trainResult.value = { status: 'error', message: '网络错误，请检查服务是否运行' }
    ElMessage.error('训练失败，请稍后重试')
  } finally {
    trainingLoading.value = false
    fileList.value = []
    trainForm.file = null
    if(uploadRef.value) uploadRef.value.clearFiles()
  }
}

// 获取模型信息 - 单层结构：直接赋值response
const fetchModelInfo = async () => {
  modelInfoLoading.value = true
  try {
    const response = await getModelInfo()
    modelInfo.value = response
    if (modelInfo.value.status === 'success') {
      ElMessage.success('获取模型信息成功')
    } else {
      ElMessage.error(modelInfo.value.message || '获取模型信息失败')
    }
  } catch (error) {
    modelInfo.value = { status: 'error', message: '网络错误，请检查服务是否运行' }
    ElMessage.error('获取失败，请稍后重试')
  } finally {
    modelInfoLoading.value = false
  }
}

// 预测成绩 - 单层结构：直接赋值response
const handlePredictScore = async () => {
  predictLoading.value = true
  try {
    const response = await predictScore(predictForm)
    predictResult.value = response
    if (predictResult.value.status === 'success') {
      ElMessage.success('预测成功')
    } else {
      ElMessage.error(predictResult.value.message || '预测失败')
    }
  } catch (error) {
    predictResult.value = { status: 'error', message: '网络错误，请检查服务是否运行' }
    ElMessage.error('预测失败，请稍后重试')
  } finally {
    predictLoading.value = false
  }
}

// 获取成绩等级
const getScoreLevel = (score) => {
  if (!score) return 'info'
  if (score >= 90) return 'success'
  if (score >= 80) return 'primary'
  if (score >= 70) return 'warning'
  return 'danger'
}

// 获取成绩描述
const getScoreDescription = (score) => {
  if (!score) return '待预测'
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '中等'
  if (score >= 60) return '及格'
  return '不及格'
}

// 页面挂载时自动获取模型信息（可选功能）
onMounted(() => {
  // fetchModelInfo();
})
</script>

<style scoped>
/* 基础容器样式 */
.prediction-container {
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
.prediction-header {
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

/* 标签页样式 */
.prediction-tabs {
  padding: 30px;
}

/* 覆盖Element Plus标签页样式 */
:deep(.el-tabs__header) {
  margin-bottom: 30px !important;
}

:deep(.el-tabs__item) {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #666 !important;
  transition: all 0.3s ease !important;
}

:deep(.el-tabs__item:hover) {
  color: #667eea !important;
}

:deep(.el-tabs__item.is-active) {
  color: #667eea !important;
}

:deep(.el-tabs__active-bar) {
  background-color: #667eea !important;
  height: 3px !important;
  border-radius: 3px;
}

/* 警告框样式 */
.alert-section {
  margin-bottom: 25px;
  border-radius: 10px;
  background: #f6f9fe;
  border: 1px solid #e3f2fd;
}

/* 表单样式 */
.train-form,
.predict-form {
  margin-bottom: 30px;
  animation: fadeIn 0.5s ease;
}

:deep(.el-form-item) {
  margin-bottom: 25px;
}

:deep(.el-form-item__label) {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.form-input,
.number-input,
.select-input {
  width: 100%;
  max-width: 450px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-input-number) {
  border-radius: 8px !important;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-input-number:hover) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.4) !important;
  border-color: #667eea !important;
}

.input-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  font-style: italic;
}

/* 按钮样式 */
.train-button,
.info-button,
.predict-button {
  width: 220px;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.train-button:hover,
.info-button:hover,
.predict-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
  background: linear-gradient(90deg, #764ba2, #667eea);
}

.train-button:active,
.info-button:active,
.predict-button:active {
  transform: translateY(0);
}

/* 结果区域样式 */
.train-result,
.model-info-result,
.predict-result {
  margin-top: 30px;
  animation: slideIn 0.5s ease;
}

/* 分隔线样式 */
:deep(.el-divider__text) {
  font-size: 18px;
	font-weight: 600;
	color: #333;
	background-color: transparent;
	padding: 0 15px;
}

/* 详情组件样式 */
.result-details {
  margin-bottom: 20px;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
	color: #333;
}

/* 特征标签样式 */
.feature-list,
.categorical-features {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.feature-tag {
  margin-bottom: 8px;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.feature-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

/* 模型层样式 */
.model-layers {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 20px;
}

.layer-card {
  transition: all 0.3s ease;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.layer-card:hover {
  transform: translateY(-3px);
	box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.model-card,
.feature-card {
  margin-bottom: 25px;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.model-card:hover,
.feature-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

/* 单选框组样式 */
.radio-group {
  display: flex;
  gap: 25px;
}

:deep(.el-radio__label) {
  font-size: 15px;
}

:deep(.el-radio.is-checked .el-radio__inner) {
  border-color: #667eea;
  background-color: #667eea;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #667eea;
  font-weight: 600;
}

/* 成绩显示样式 */
.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
}

.score-circle {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 25px;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

.score-assessment {
  margin-top: 15px;
}

:deep(.el-tag--large) {
  font-size: 18px;
  padding: 8px 20px;
  border-radius: 20px;
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

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
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

@keyframes pulse {
  0% {
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  }
  50% {
    box-shadow: 0 8px 35px rgba(102, 126, 234, 0.6);
  }
  100% {
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .prediction-container {
    padding: 15px;
  }
  
  .prediction-tabs {
    padding: 20px 15px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .page-description {
    font-size: 14px;
  }
  
  .form-input,
  .number-input,
  .select-input {
    max-width: 100%;
  }
  
  :deep(.el-form-item__label) {
    font-size: 14px;
  }
  
  .train-button,
  .info-button,
  .predict-button {
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }
  
  .radio-group {
    flex-direction: column;
    gap: 15px;
  }
  
  .score-circle {
    width: 120px;
    height: 120px;
    font-size: 36px;
  }
  
  :deep(.el-tabs__item) {
    font-size: 14px !important;
  }
}

/* 宽屏优化 */
@media (min-width: 1200px) {
  .prediction-container {
    max-width: 1400px;
    margin: 0 auto;
  }
  
  .main-card {
    max-width: 1200px;
    margin: 0 auto;
  }
}
</style>