/**
 * 教育模块路由入口
 * 主控端、教师 Pad、学生 Pad 按文件拆分，统一在这里聚合导出。
 */

import adminEducationRoutes, { managerRoles } from './admin'
import teacherEducationRoutes, { teacherRoles } from './teacher'
import studentEducationRoutes, { studentRoles } from './student'

const teacherStudentRoles = [...teacherRoles, ...studentRoles]
const educationAccessRoles = [...managerRoles, ...teacherRoles, ...studentRoles]

const educationRoutes = [
  {
    path: '/education',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  },
  {
    path: '/education/pad',
    component: () => import('@/views/education/entry'),
    name: 'EducationPadEntry',
    roles: educationAccessRoles,
    meta: {
      title: '教育端入口',
      noCache: true
    },
    hidden: true
  },
  ...adminEducationRoutes,
  ...teacherEducationRoutes,
  ...studentEducationRoutes,
  {
    path: '/education/dashboard',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  },
  {
    path: '/education/index',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  },
  {
    path: '/education/rag',
    component: () => import('@/views/education/rag'),
    name: 'EducationRag',
    roles: teacherStudentRoles,
    meta: {
      title: 'AI 智能问答',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/prediction',
    component: () => import('@/views/education/prediction'),
    name: 'EducationPrediction',
    roles: teacherStudentRoles,
    meta: {
      title: 'AI 成绩预测',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/:pathMatch(.*)*',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  },
  {
    path: '/pad',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  },
  {
    path: '/pad/:pathMatch(.*)*',
    redirect: '/education/auth?redirect=/education/pad',
    hidden: true
  }
]

export default educationRoutes
