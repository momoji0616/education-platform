const studentRoles = ['student', 'ROLE_DEFAULT']

const studentEducationRoutes = [
  {
    path: '/education/student/pad',
    component: () => import('@/views/education/student/index'),
    name: 'EducationStudentPad',
    roles: studentRoles,
    meta: {
      title: '学生主页',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/ai',
    component: () => import('@/views/education/student/ai'),
    name: 'EducationStudentAi',
    roles: studentRoles,
    meta: {
      title: '学生 AI 学习台',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/prediction',
    component: () => import('@/views/education/student/prediction'),
    name: 'EducationStudentPrediction',
    roles: studentRoles,
    meta: {
      title: '成绩预测',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/history',
    component: () => import('@/views/education/student/history'),
    name: 'EducationStudentHistory',
    roles: studentRoles,
    meta: {
      title: '历史做题',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/assistant',
    component: () => import('@/views/education/student/assistant'),
    name: 'EducationStudentAssistant',
    roles: studentRoles,
    meta: {
      title: '师生 AI 互动助手',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/materials',
    component: () => import('@/views/education/student/materials'),
    name: 'EducationStudentMaterials',
    roles: studentRoles,
    meta: {
      title: '资料整理',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/practice',
    component: () => import('@/views/education/student/practice'),
    name: 'EducationStudentPractice',
    roles: studentRoles,
    meta: {
      title: '智能刷题',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/plan',
    component: () => import('@/views/education/student/plan'),
    name: 'EducationStudentPlan',
    roles: studentRoles,
    meta: {
      title: '学习规划',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/student/report',
    component: () => import('@/views/education/student/report'),
    name: 'EducationStudentReport',
    roles: studentRoles,
    meta: {
      title: '学业诊断',
      noCache: true
    },
    hidden: true
  }
]

export { studentRoles }
export default studentEducationRoutes
