/**
 * 教育管理模块路由
 */
import Layout from '@/layout'

// 教育管理模块路由数组 - 完全扁平化结构
const educationRoutes = [
  {
    path: '/education/dashboard',
    component: () => import('@/views/education/dashboard'),
    name: 'EducationDashboard',
    meta: {
      title: '教育管理首页',
      icon: 'el-icon-home',
      noCache: true
    }
  },
  {
    path: '/education/index',
    component: () => import('@/views/education/index'),
    name: 'StudentPerformance',
    meta: {
      title: '学生成绩管理',
      icon: 'el-icon-document',
      noCache: true
    }
  },
  {
    path: '/education/rag',
    component: () => import('@/views/education/rag'),
    name: 'RagIndex',
    meta: {
      title: '智能问答',
      icon: 'chat-line-round'
    }
  },
  {
    path: '/education/prediction',
    component: () => import('@/views/education/prediction'),
    name: 'ScorePrediction',
    meta: {
      title: '成绩预测',
      icon: 'bar-chart-3'
    }
  }
]

export default educationRoutes