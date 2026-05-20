import request, { download } from '@/utils/request'

export function getManagerOverview() {
  return request({
    url: '/education/pad/manager/overview',
    method: 'get'
  })
}

export function listManagerScores() {
  return request({
    url: '/education/pad/manager/scores',
    method: 'get'
  })
}

export function createTeacherTask(data) {
  return request({
    url: '/education/pad/manager/teacher-task',
    method: 'post',
    data
  })
}

export function createManagerHomework(data) {
  return request({
    url: '/education/pad/homework',
    method: 'post',
    data
  })
}

export function importStudentRealData(file, updateSupport = false) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('updateSupport', updateSupport)
  return request({
    url: '/student/performance/importData',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function downloadStudentRealDataTemplate() {
  return download('/student/performance/importTemplate', {}, '学生真实数据导入模板.xlsx')
}
