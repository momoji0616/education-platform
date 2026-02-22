<template>
  <div class="dashboard-container">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration bg-decoration-1"></div>
    <div class="bg-decoration bg-decoration-2"></div>
    <div class="bg-decoration bg-decoration-3"></div>
    
    <div class="page-header">
      <h2 class="page-title">教育管理系统</h2>
      <p class="page-description">欢迎使用智能教育管理平台，请选择您需要的功能</p>
    </div>
    
    <div class="dashboard-cards">
      <el-card class="main-card dashboard-card" @click="navigateTo('/education/index')">
        <div class="card-content">
          <div class="icon-container">
            <el-icon class="card-icon"><data-analysis /></el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">学生成绩管理</h3>
            <p class="card-description">查看和管理学生的各科考试成绩</p>
          </div>
          <el-icon class="arrow-icon"><arrow-right /></el-icon>
        </div>
      </el-card>
      
      <el-card class="main-card dashboard-card" @click="navigateTo('/education/prediction')">
        <div class="card-content">
          <div class="icon-container">
            <el-icon class="card-icon"><trend-charts /></el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">成绩预测分析</h3>
            <p class="card-description">通过机器学习模型预测学生成绩</p>
          </div>
          <el-icon class="arrow-icon"><arrow-right /></el-icon>
        </div>
      </el-card>
      
      <el-card class="main-card dashboard-card" @click="navigateTo('/education/rag')">
        <div class="card-content">
          <div class="icon-container">
            <el-icon class="card-icon"><chat-line-round /></el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">智能问答系统</h3>
            <p class="card-description">基于RAG技术的智能问答系统</p>
          </div>
          <el-icon class="arrow-icon"><arrow-right /></el-icon>
        </div>
      </el-card>
      
      <el-card class="main-card dashboard-card">
        <div class="card-content">
          <div class="icon-container">
            <el-icon class="card-icon"><data-line /></el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">数据分析</h3>
            <p class="card-description">对学生成绩数据进行深度分析</p>
          </div>
          <el-icon class="arrow-icon"><arrow-right /></el-icon>
        </div>
      </el-card>
      
      <el-card class="main-card dashboard-card">
        <div class="card-content">
          <div class="icon-container">
            <el-icon class="card-icon"><setting /></el-icon>
          </div>
          <div class="card-info">
            <h3 class="card-title">系统设置</h3>
            <p class="card-description">管理系统参数和用户权限</p>
          </div>
          <el-icon class="arrow-icon"><arrow-right /></el-icon>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, TrendCharts, ChatLineRound, Setting, ArrowRight, DataLine } from '@element-plus/icons-vue'

const router = useRouter()

// 导航到指定页面
const navigateTo = (path) => {
  try {
    router.push(path)
  } catch (error) {
    console.error('导航失败:', error)
    ElMessage.error('页面导航失败')
  }
}
</script>

<style scoped>
/* 基础容器样式 */
.dashboard-container {
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

/* 卡片网格布局 */
.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 25px;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 单个卡片样式 */
.dashboard-card {
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
}

.dashboard-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
}

/* 卡片内容布局 */
.card-content {
  display: flex;
  align-items: center;
  padding: 25px;
  position: relative;
}

/* 图标容器 */
.icon-container {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  flex-shrink: 0;
}

/* 卡片图标 */
.card-icon {
  font-size: 2em;
  color: white;
  transition: transform 0.3s ease;
}

.dashboard-card:hover .card-icon {
  transform: scale(1.2);
  animation: pulse 1s ease;
}

/* 卡片信息 */
.card-info {
  flex: 1;
}

.card-title {
  font-size: 1.3em;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
  transition: color 0.3s ease;
}

.dashboard-card:hover .card-title {
  color: #667eea;
}

.card-description {
  font-size: 0.95em;
  color: #606266;
  line-height: 1.5;
}

/* 箭头图标 */
.arrow-icon {
  color: #909399;
  font-size: 1.2em;
  transition: all 0.3s ease;
}

.dashboard-card:hover .arrow-icon {
  color: #667eea;
  transform: translateX(5px);
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

@keyframes pulse {
  0% {
    transform: scale(1.2);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1.2);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 15px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .page-description {
    font-size: 14px;
  }
  
  .dashboard-cards {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .card-content {
    padding: 20px;
  }
  
  .icon-container {
    width: 50px;
    height: 50px;
    margin-right: 15px;
  }
  
  .card-icon {
    font-size: 1.5em;
  }
  
  .card-title {
    font-size: 1.2em;
  }
}

/* 宽屏优化 */
@media (min-width: 1200px) {
  .dashboard-container {
    max-width: 1400px;
    margin: 0 auto;
  }
  
  .dashboard-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 中屏优化 */
@media (min-width: 769px) and (max-width: 1199px) {
  .dashboard-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>