<template>
  <div class="rag-container">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration bg-decoration-1"></div>
    <div class="bg-decoration bg-decoration-2"></div>
    <div class="bg-decoration bg-decoration-3"></div>
    
    <div class="rag-header">
      <h1>本地RAG问答系统</h1>
      <p class="subtitle">上传Excel文件并基于文件内容进行智能问答</p>
    </div>

    <div class="rag-main">
      <!-- 左侧历史数据集区域 -->
      <div class="history-section">
        <el-card class="main-card">
          <div class="history-header">
            <h3>历史数据集</h3>
            <el-button 
              type="primary" 
              size="small" 
              @click="refreshHistory"
              :loading="loadingHistory"
            >
              刷新
            </el-button>
          </div>
          <div class="history-list" v-if="historyDatasets.length > 0">
            <div 
              v-for="(dataset, index) in historyDatasets" 
              :key="dataset.id || index"
              class="dataset-item"
            >
              <div class="dataset-info">
                <div class="dataset-name">{{ dataset.name }}</div>
                <div class="dataset-time">{{ formatTime(dataset.upload_time) }}</div>
              </div>
              <div class="dataset-actions">
                <el-button 
                  type="text" 
                  size="small" 
                  @click="viewDataset(dataset)"
                  icon="el-icon-view"
                >
                  查看
                </el-button>
                <el-button 
                  type="text" 
                  size="small" 
                  @click="handleDeleteDataset(dataset)"
                  icon="el-icon-delete"
                  danger
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-history">
            <el-empty description="暂无历史数据集" />
          </div>
        </el-card>
      </div>

      <!-- 右侧主内容区域 -->
      <div class="main-content">
        <!-- 上传区域 -->
        <div class="upload-section">
          <el-card class="main-card">
            <div class="upload-content">
              <el-upload
                ref="upload"
                :auto-upload="false"
                :limit="10"
                :file-list="fileList"
                accept=".xlsx,.xls"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                drag
                multiple
                class="upload-demo"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  <em>点击或拖拽文件到此区域上传</em>
                  <div class="upload-hint">仅支持 .xlsx 或 .xls 格式的Excel文件</div>
                </div>
              </el-upload>
              <el-button
                type="primary"
                :loading="uploadLoading"
                :disabled="!fileList.length || uploadLoading"
                @click="submitUpload"
                class="upload-btn"
              >
                上传文件
              </el-button>
            </div>
          </el-card>
        </div>

        <!-- 问答区域 -->
        <div class="chat-section">
          <el-card class="main-card">
            <div class="chat-header">
              <h3>智能问答</h3>
              <p class="chat-hint">输入问题获取智能回答</p>
            </div>
            
            <!-- 聊天记录 -->
            <div class="chat-messages" ref="chatContainer">
              <div v-if="messages.length === 0" class="empty-messages">
                <el-empty description="暂无问答记录" />
              </div>
              <div v-for="(message, index) in messages" :key="index" class="message-item">
                <div class="message-user" v-if="message.type === 'user'">
                  <div class="message-avatar">👤</div>
                  <div class="message-content">
                    <div class="message-label">我</div>
                    <div class="message-text">{{ message.content }}</div>
                  </div>
                </div>
                <div class="message-ai" v-else>
                  <div class="message-avatar">🤖</div>
                  <div class="message-content">
                    <div class="message-label">AI</div>
                    <div class="message-text">{{ message.content }}</div>
                  </div>
                </div>
              </div>
              <div v-if="queryLoading" class="loading-answer">
                <el-skeleton :rows="3" animated />
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="chat-input">
              <el-input
                v-model="question"
                placeholder="请输入您的问题..."
                :rows="2"
                type="textarea"
                :disabled="queryLoading || uploadLoading"
                @keyup.enter.ctrl="submitQuery"
              />
              <div class="input-buttons">
                <el-button
                  type="danger"
                  v-if="queryLoading"
                  @click="abortQuery"
                  class="abort-button"
                >
                  中止
                </el-button>
                <el-button
                  type="primary"
                  :loading="queryLoading"
                  :disabled="!question.trim() || queryLoading || uploadLoading"
                  @click="submitQuery"
                  class="send-button"
                >
                  发送
                </el-button>
              </div>
              <div class="input-hint">
                <span>提示：Ctrl + Enter 快速发送</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadExcel, queryQuestion, getDatasets, deleteDataset, getDatasetDetail } from '@/api/education/rag'
import request from '@/utils/request'

// 历史数据集相关
const historyDatasets = ref([])
const loadingHistory = ref(false)

// 文件上传相关
const fileList = ref([])
const uploadLoading = ref(false)

// 问答相关
const question = ref('')
const messages = ref([])
const queryLoading = ref(false)
const chatContainer = ref(null)
let queryAborted = false

// 处理文件变化
const handleFileChange = (file, uploadFiles) => {
  // 检查文件类型
  if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
    ElMessage.error('请上传Excel文件（.xlsx或.xls）')
    // 从列表中移除不符合要求的文件
    fileList.value = uploadFiles.filter(f => f.name.endsWith('.xlsx') || f.name.endsWith('.xls'))
    return
  }
  // 保留所有符合要求的文件
  fileList.value = uploadFiles.filter(f => f.name.endsWith('.xlsx') || f.name.endsWith('.xls'))
  console.log('文件已添加到列表:', fileList.value.map(f => f.name))
}

// 处理文件超出限制
const handleExceed = () => {
  ElMessage.warning('最多只能上传10个文件')
  fileList.value = fileList.value.slice(0, 10)
}

// 提交上传
  const submitUpload = async () => {
    if (fileList.value.length === 0) {
      ElMessage.warning('请先选择文件')
      return
    }

    uploadLoading.value = true
    
    try {
      console.log('开始上传文件:', fileList.value.map(f => f.name))
      
      // 创建FormData对象，添加所有文件
      const formData = new FormData()
      fileList.value.forEach(file => {
        formData.append('files', file.raw || file)
      })
      
      // 直接使用fetch API上传多文件
      const response = await fetch('/rag-api/upload-excel', {
        method: 'POST',
        body: formData
      })
      
      const data = await response.json()
      console.log('上传响应:', data)
      
      // 处理响应数据
      if (response.ok) {
        let successMessage = '文件上传成功'
        let importedFiles = fileList.value.map(f => f.name).join('、')
        let importDetails = ''
        
        console.log('上传成功响应详情:', data)
        
        // 处理多文件上传的响应
        if (data.results) {
          const successFiles = data.results.filter(r => r.status === 'success').map(r => r.filename)
          const errorFiles = data.results.filter(r => r.status === 'error').map(r => r.filename)
          
          if (successFiles.length > 0) {
            successMessage = `成功导入 ${successFiles.length} 个文件`
            importedFiles = successFiles.join('、')
            // 收集导入详情
            importDetails = data.results.map(r => {
              if (r.status === 'success') {
                return `${r.filename}: ${r.message}`
              } else {
                return `${r.filename}: 失败 - ${r.message}`
              }
            }).join('\n')
          }
          
          if (errorFiles.length > 0) {
            ElMessage.warning(`以下文件导入失败：${errorFiles.join('、')}`)
          }
        } else if (data.message) {
          successMessage = data.message
          importDetails = data.message
        }
        
        // 显示详细的成功提示
        ElMessage.success({
          message: `文件上传成功！${successMessage}`,
          showClose: true,
          duration: 8000
        })
        
        // 清空现有消息并添加新消息
        messages.value = []
        messages.value.push({
          type: 'ai',
          content: `✅ 文件已成功导入：${importedFiles}\n${importDetails ? '导入详情：\n' + importDetails : ''}\n现在您可以提问了，系统会基于所有导入的文件内容回答。`
        })
      } else {
        // 处理错误响应
        const errorMessage = data.detail || '上传失败'
        console.log('上传错误响应详情:', data)
        ElMessage.error({
          message: `文件上传失败：${errorMessage}`,
          showClose: true,
          duration: 8000
        })
        
        // 在消息区域显示错误
        messages.value.push({
          type: 'ai',
          content: `❌ 文件上传失败\n错误信息：${errorMessage}`
        })
      }
      
      // 立即滚动到底部显示新消息
      await nextTick()
      scrollToBottom()
    } catch (error) {
      console.error('上传失败详情:', error)
      
      // 提取错误信息
      let errorMessage = '上传失败'
      if (error.message) {
        errorMessage = error.message
      }
      
      // 显示详细的错误提示
      ElMessage.error({
        message: `文件上传失败：${errorMessage}`,
        showClose: true,
        duration: 8000
      })
      
      // 在消息区域显示错误
      messages.value.push({
        type: 'ai',
        content: `❌ 文件上传失败\n错误信息：${errorMessage}`
      })
      
      // 立即滚动到底部显示错误消息
      await nextTick()
      scrollToBottom()
    } finally {
      uploadLoading.value = false
      console.log('上传操作完成')
    }
  }

// 提交查询
  const submitQuery = async () => {
    if (!question.value.trim()) {
      ElMessage.warning('请输入问题')
      return
    }
    // 移除文件上传检查，允许在没有上传文件的情况下提问

    const userQuestion = question.value.trim()
    messages.value.push({
      type: 'user',
      content: userQuestion
    })
    question.value = ''
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()

    queryLoading.value = true
    queryAborted = false
    
    try {
      console.log('开始查询问题:', userQuestion)
      // 使用fetch API直接调用后端接口
      const response = await fetch(`/rag-api/query?question=${encodeURIComponent(userQuestion)}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const data = await response.json()
      console.log('查询响应:', data)
      
      // 检查是否在请求期间被中止
      if (queryAborted) {
        return
      }
      
      // 处理响应数据
      const answer = data.answer || data.msg || '暂无响应'
      
      // 显示成功提示
      ElMessage.success({
        message: '查询成功',
        showClose: true,
        duration: 3000
      })
      
      messages.value.push({
        type: 'ai',
        content: answer
      })
    } catch (error) {
      console.error('查询失败详情:', error)
      
      // 检查是否是用户主动中止
      if (queryAborted) {
        messages.value.push({
          type: 'ai',
          content: '❌ 查询已被中止'
        })
        return
      }
      
      // 提取错误信息
      let errorMessage = '查询失败'
      if (error.message) {
        errorMessage = error.message
      } else if (error instanceof Error) {
        errorMessage = error.toString()
      }
      
      // 显示错误提示
      ElMessage.error({
        message: `查询失败：${errorMessage}`,
        showClose: true,
        duration: 5000
      })
      
      messages.value.push({
        type: 'ai',
        content: `❌ 查询失败\n错误信息：${errorMessage}`
      })
    } finally {
      queryLoading.value = false
      queryAborted = false
      console.log('查询操作完成')
      await nextTick()
      scrollToBottom()
    }
  }
  
  // 中止查询
  const abortQuery = () => {
    queryAborted = true
    queryLoading.value = false
    ElMessage.info('查询已中止')
  }

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 获取历史数据集
const getHistoryDatasets = async () => {
  loadingHistory.value = true
  try {
    // 调用API获取历史数据集
    const response = await getDatasets()
    const data = response.data || response
    historyDatasets.value = data.datasets || data || []
    console.log('历史数据集:', historyDatasets.value)
  } catch (error) {
    console.error('获取历史数据集失败:', error)
    ElMessage.error('获取历史数据集失败')
    // 模拟数据，用于开发测试
    historyDatasets.value = [
      {
        id: 1,
        name: 'contacts.xlsx',
        upload_time: new Date().toISOString()
      },
      {
        id: 2,
        name: 'products.xlsx',
        upload_time: new Date(Date.now() - 86400000).toISOString()
      }
    ]
  } finally {
    loadingHistory.value = false
  }
}

// 刷新历史数据集
const refreshHistory = () => {
  getHistoryDatasets()
}

// 删除数据集
const handleDeleteDataset = async (dataset) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据集「${dataset.name}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用API删除数据集
    await deleteDataset(dataset.id)
    
    ElMessage.success('数据集删除成功')
    // 刷新数据集列表
    getHistoryDatasets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除数据集失败:', error)
      ElMessage.error('删除数据集失败')
    }
  }
}

// 查看数据集
const viewDataset = async (dataset) => {
  try {
    // 调用API查看数据集详情
    const response = await getDatasetDetail(dataset.id)
    
    const data = response.data || response
    // 显示数据集详情
    ElMessageBox.alert(
      `文件名: ${dataset.name}\n上传时间: ${formatTime(dataset.upload_time)}\n\n${data.description || '暂无额外信息'}`,
      '数据集详情',
      {
        confirmButtonText: '确定'
      }
    )
  } catch (error) {
    console.error('查看数据集失败:', error)
    ElMessage.error('查看数据集失败')
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  // 页面加载时获取历史数据集
  getHistoryDatasets()
})
</script>

<style scoped>
/* 基础容器样式 */
.rag-container {
  max-width: 1200px;
  margin: 0 auto;
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

.rag-main {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.history-section {
  width: 300px;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  min-width: 0; /* 防止flex子元素溢出 */
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.history-header h3 {
  margin: 0;
  font-size: 1.2em;
  color: #303133;
}

.history-list {
  max-height: 500px;
  overflow-y: auto;
}

.dataset-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.2s;
}

.dataset-item:hover {
  background-color: #f5f7fa;
}

.dataset-info {
  flex: 1;
  min-width: 0;
}

.dataset-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dataset-time {
  font-size: 0.85em;
  color: #909399;
}

.dataset-actions {
  display: flex;
  gap: 8px;
}

.empty-history {
  padding: 40px 0;
}

/* 响应式设计调整 */
@media (max-width: 768px) {
  .rag-main {
    flex-direction: column;
  }
  
  .history-section {
    width: 100%;
  }
  
  .history-list {
    max-height: 300px;
  }
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

/* 按钮动画效果 */
:deep(.el-button) {
  transition: all 0.3s ease;
}

:deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 输入框效果 */
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.4) !important;
  border-color: #667eea !important;
}

/* 响应式设计 - 宽屏优化 */
@media (min-width: 1200px) {
  .rag-container {
    max-width: 1400px;
  }
}

.rag-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.rag-header h1 {
  font-size: 2.5em;
  color: white;
  margin-bottom: 10px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  background: linear-gradient(90deg, #ffffff, #e0e0e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  font-size: 1.1em;
  color: rgba(255, 255, 255, 0.9);
}

.upload-section,
.chat-section {
  margin-bottom: 30px;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-demo {
  width: 100%;
  max-width: 600px;
}

.upload-hint {
  color: #909399;
  font-size: 0.9em;
  margin-top: 5px;
}

.upload-btn {
  margin-top: 20px;
}

.chat-header {
  margin-bottom: 20px;
}

.chat-header h3 {
  font-size: 1.5em;
  color: #303133;
  margin-bottom: 5px;
}

.chat-hint {
  color: #909399;
  font-size: 0.9em;
}

.chat-messages {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
  margin-bottom: 20px;
}

.empty-messages {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.message-item {
  margin-bottom: 20px;
  display: flex;
}

.message-user {
  justify-content: flex-end;
}

.message-ai {
  justify-content: flex-start;
}

.message-avatar {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-content {
  max-width: 70%;
  margin: 0 10px;
}

.message-label {
  font-size: 0.9em;
  color: #909399;
  margin-bottom: 5px;
}

.message-text {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  word-wrap: break-word;
  line-height: 1.5;
}

.message-user .message-text {
  background-color: #409eff;
  color: white;
}

.loading-answer {
  padding: 20px;
}

.chat-input {
  margin-top: 20px;
}

.input-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.abort-button {
  min-width: 80px;
}

.send-button {
  min-width: 80px;
}

.input-hint {
  margin-top: 10px;
  color: #909399;
  font-size: 0.85em;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .rag-container {
    padding: 10px;
  }
  
  .rag-header h1 {
    font-size: 2em;
  }
  
  .message-content {
    max-width: 85%;
  }
}
</style>