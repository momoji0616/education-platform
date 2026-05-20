import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function listStudentHomework() {
  return request({ url: '/education/pad/homework/student', method: 'get' })
}

export function submitHomework(homeworkId, data) {
  return request({ url: `/education/pad/homework/${homeworkId}/submit`, method: 'post', data })
}

export function listStudentHomeworkSubmissions() {
  return request({ url: '/education/pad/homework/submissions/student', method: 'get' })
}

export function listStudentExam() {
  return request({ url: '/education/pad/exam/student', method: 'get' })
}

export function submitExam(examId, data) {
  return request({ url: `/education/pad/exam/${examId}/submit`, method: 'post', data })
}

export function listStudentExamScore() {
  return request({ url: '/education/pad/exam/score/student', method: 'get' })
}

export function listStudentSelfScores() {
  return request({ url: '/education/pad/student/scores', method: 'get' })
}

export function listQuestionCatalogs(params) {
  return request({ url: '/education/pad/question/catalogs', method: 'get', params })
}

export function listStudentHistoryCatalogs(params) {
  return request({ url: '/education/pad/student/history/catalogs', method: 'get', params })
}

export function getStudentDiagnosisOverview() {
  return request({ url: '/education/pad/student/diagnosis/overview', method: 'get' })
}

export function getStudentDiagnosisChapters() {
  return request({ url: '/education/pad/student/diagnosis/chapters', method: 'get' })
}

export function getStudentWrongQuestions(params) {
  return request({ url: '/education/pad/student/diagnosis/wrong-questions', method: 'get', params })
}

export function getStudentWeakKnowledgePoints(params) {
  return request({ url: '/education/pad/student/diagnosis/knowledge-points', method: 'get', params })
}

export function getStudentPracticeRecommendations(params) {
  return request({ url: '/education/pad/student/recommendations/practice', method: 'get', params })
}

export function getStudentProgramOverview() {
  return request({ url: '/education/pad/student/program/overview', method: 'get' })
}

export function getStudentProgramAssignments(params) {
  return request({ url: '/education/pad/student/program/assignments', method: 'get', params })
}

export function getStudentAnswerHistory(params) {
  return request({ url: '/education/pad/student/history/answers', method: 'get', params })
}

export function getStudentRagProfile(params) {
  return axios({
    url: `${import.meta.env.VITE_APP_BASE_API}/education/pad/student/rag/profile`,
    method: 'get',
    params,
    headers: {
      Authorization: getToken() ? `Bearer ${getToken()}` : ''
    },
    timeout: 8000
  }).then((res) => res.data)
}
