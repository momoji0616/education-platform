import request from '@/utils/request'
import { aiApiPrefix } from '@/api/education/ai'

export function uploadAiReference(file) {
  const formData = new FormData()
  formData.append('file', file.raw || file)
  return request({
    url: `${aiApiPrefix}/grading/reference`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}

export function aiGradeSingle({ file, referenceId = '', rubric = '', maxScore = 100, questionCount = 12 }) {
  const formData = new FormData()
  formData.append('studentFile', file.raw || file)
  formData.append('referenceId', referenceId || '')
  formData.append('rubric', rubric || '')
  formData.append('maxScore', String(maxScore || 100))
  formData.append('questionCount', String(questionCount || 12))
  return request({
    url: `${aiApiPrefix}/grading/single`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}

export function aiGradeBatch({ files = [], referenceId = '', rubric = '', maxScore = 100, questionCount = 12 }) {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file.raw || file)
  })
  formData.append('referenceId', referenceId || '')
  formData.append('rubric', rubric || '')
  formData.append('maxScore', String(maxScore || 100))
  formData.append('questionCount', String(questionCount || 12))
  return request({
    url: `${aiApiPrefix}/grading/batch`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 180000
  })
}

export function fetchAiGradingAsset(url) {
  return request({
    url,
    method: 'get',
    responseType: 'blob',
    timeout: 120000
  })
}
