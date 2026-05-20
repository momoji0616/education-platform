<template>
  <div class="student-ai-page">
    <div class="hero-card">
      <div>
        <p class="eyebrow">Student AI Hub</p>
        <h1>学生 AI 学习台</h1>
        <p class="hero-text">这里只保留学生最常用的 3 个 AI 能力入口，页面更简洁，切换也更直接。</p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="router.push('/education/student/pad')">返回学生首页</el-button>
        <el-button type="primary" @click="router.push('/education/rag')">进入智能问答</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8" v-for="item in cards" :key="item.title">
        <el-card class="feature-card">
          <p class="feature-tag">{{ item.tag }}</p>
          <h2>{{ item.title }}</h2>
          <p class="feature-text">{{ item.description }}</p>
          <div class="feature-points">
            <span v-for="point in item.points" :key="point">{{ point }}</span>
          </div>
          <el-button :type="item.type" plain @click="router.push(item.to)">{{ item.action }}</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const cards = [
  {
    tag: '互动',
    title: '师生 AI 助手',
    description: '进入班级互动页面，查看群聊消息并围绕问题继续追问。',
    points: ['班级互动', '消息查看', '继续追问'],
    to: '/education/student/assistant',
    action: '进入互动助手',
    type: 'info'
  },
  {
    tag: '问答',
    title: 'RAG 智能问答',
    description: '基于知识库、课程资料和题目上下文进行智能问答与解释。',
    points: ['知识库问答', '课程资料', '题目讲解'],
    to: '/education/rag',
    action: '进入智能问答',
    type: 'primary'
  },
  {
    tag: '预测',
    title: '成绩预测',
    description: '上传训练数据并分析不同变量对成绩结果的影响。',
    points: ['上传数据', '训练模型', '结果分析'],
    to: '/education/student/prediction',
    action: '进入成绩预测',
    type: 'success'
  }
]
</script>

<style scoped lang="scss">
.student-ai-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.14), transparent 24%),
    linear-gradient(180deg, #fffaf4 0%, #eef7ff 100%);
}

.hero-card,
.feature-card {
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

.eyebrow,
.feature-tag {
  margin: 0 0 8px;
  color: #c2410c;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0 0 12px;
  color: #0f172a;
}

h1 {
  font-size: 32px;
}

.hero-text,
.feature-text {
  color: #475569;
  line-height: 1.7;
}

.hero-actions,
.feature-points {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.feature-card {
  height: 100%;
  padding: 22px;
}

.feature-points {
  margin: 16px 0;
}

.feature-points span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff2e8;
  color: #c2410c;
  font-size: 13px;
}

@media (max-width: 768px) {
  .student-ai-page {
    padding: 16px;
  }

  .hero-card {
    flex-direction: column;
  }
}
</style>
