const teacherRoles = ['teacher']

const teacherEducationRoutes = [
  {
    path: '/education/teacher/pad',
    component: () => import('@/views/education/teacher/index'),
    name: 'EducationTeacherPad',
    roles: teacherRoles,
    meta: {
      title: '教师首页',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/ai',
    component: () => import('@/views/education/teacher/ai'),
    name: 'EducationTeacherAi',
    roles: teacherRoles,
    meta: {
      title: '师生 AI 互动助手',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/qa',
    component: () => import('@/views/education/teacher/qa'),
    name: 'EducationTeacherQa',
    roles: teacherRoles,
    meta: {
      title: '成绩预测',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/rag',
    component: () => import('@/views/education/teacher/rag'),
    name: 'EducationTeacherRag',
    roles: teacherRoles,
    meta: {
      title: '智能问答',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/assistant',
    component: () => import('@/views/education/teacher/assistant'),
    name: 'EducationTeacherAssistant',
    roles: teacherRoles,
    meta: {
      title: '师生 AI 互动助手',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/paper',
    component: () => import('@/views/education/teacher/paper'),
    name: 'EducationTeacherPaper',
    roles: teacherRoles,
    meta: {
      title: '智能组卷',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/students',
    component: () => import('@/views/education/teacher/students'),
    name: 'EducationTeacherStudents',
    roles: teacherRoles,
    meta: {
      title: '学生管理',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/analysis',
    component: () => import('@/views/education/teacher/analysis'),
    name: 'EducationTeacherAnalysis',
    roles: teacherRoles,
    meta: {
      title: '班级学情分析',
      noCache: true
    },
    hidden: true
  },
  {
    path: '/education/teacher/grading',
    component: () => import('@/views/education/teacher/grading'),
    name: 'EducationTeacherGrading',
    roles: teacherRoles,
    meta: {
      title: 'AI 智能批改',
      noCache: true
    },
    hidden: true
  }
]

export { teacherRoles }
export default teacherEducationRoutes
