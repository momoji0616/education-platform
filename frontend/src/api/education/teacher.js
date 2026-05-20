import request from '@/utils/request'

export function createHomework(data) {
  return request({ url: '/education/pad/homework', method: 'post', data })
}

export function listTeacherHomework() {
  return request({ url: '/education/pad/homework/teacher', method: 'get' })
}

export function uploadHomeworkAttachment(file) {
  const formData = new FormData()
  formData.append('file', file.raw || file)
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      repeatSubmit: false
    },
    timeout: 120000,
    repeatSubmit: false
  })
}

export function listTeacherHomeworkSubmissions() {
  return request({ url: '/education/pad/homework/submissions', method: 'get' })
}

export function scoreHomework(data) {
  return request({ url: '/education/pad/homework/score', method: 'post', data })
}

export function createExam(data) {
  return request({ url: '/education/pad/exam', method: 'post', data })
}

export function listTeacherExam() {
  return request({ url: '/education/pad/exam/teacher', method: 'get' })
}

export function scoreExam(data) {
  return request({ url: '/education/pad/exam/score', method: 'post', data })
}

export function listTeacherExamScore() {
  return request({ url: '/education/pad/exam/score/teacher', method: 'get' })
}

export function listTeacherTasks() {
  return request({ url: '/education/pad/teacher/tasks', method: 'get' })
}

export function listTeacherScores() {
  return request({ url: '/education/pad/teacher/scores', method: 'get' })
}

export function aiSuggestReview(data) {
  return request({ url: '/education/pad/review/ai-suggest', method: 'post', data })
}

export function getTeacherAnalysisOverview() {
  return request({ url: '/education/pad/teacher/analysis/overview', method: 'get' })
}

export function getTeacherAnalysisChapters() {
  return request({ url: '/education/pad/teacher/analysis/chapters', method: 'get' })
}

export function getTeacherHotWrongQuestions(params) {
  return request({ url: '/education/pad/teacher/analysis/hot-wrong-questions', method: 'get', params })
}

export function getTeacherKnowledgePoints(params) {
  return request({ url: '/education/pad/teacher/analysis/knowledge-points', method: 'get', params })
}

export function getTeacherAssignmentOverview() {
  return request({ url: '/education/pad/teacher/analysis/assignment-overview', method: 'get' })
}

export function getTeacherAssignmentSummaries(params) {
  return request({ url: '/education/pad/teacher/analysis/assignment-summaries', method: 'get', params })
}

export function getTeacherStudentModules(params) {
  return request({ url: '/education/pad/teacher/analysis/student-modules', method: 'get', params })
}

export function listTeacherQuestionCatalogs(params) {
  return request({ url: '/education/pad/teacher/question/catalogs', method: 'get', params })
}

export function listTeacherPaperQuestions(params) {
  return request({ url: '/education/pad/teacher/paper/questions', method: 'get', params })
}

export function getTeacherStudentManagementPage(params) {
  return request({ url: '/education/pad/teacher/students/page', method: 'get', params })
}

export function getTeacherStudentHistory(params) {
  return request({ url: '/education/pad/teacher/students/history', method: 'get', params })
}

export function getTeacherAiAssistantSummary(params) {
  return request({ url: '/education/pad/teacher/assistantSummary', method: 'get', params })
}

export function getTeacherStudentCatalogs(params) {
  return request({ url: '/education/pad/teacher/question/catalogs', method: 'get', params })
}

export function getTeacherRagProfile(params) {
  return request({ url: '/education/pad/teacher/rag/profile', method: 'get', params })
}
