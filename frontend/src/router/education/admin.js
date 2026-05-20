const managerRoles = ['admin', 'manager']

const adminEducationRoutes = [
  {
    path: '/education/admin',
    component: () => import('@/views/education/admin/index.vue'),
    name: 'EducationAdmin',
    roles: managerRoles,
    meta: {
      title: '主控端',
      noCache: true
    },
    hidden: true
  }
]

export { managerRoles }
export default adminEducationRoutes
