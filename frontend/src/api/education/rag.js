import axios from 'axios'
import request from '@/utils/request'
import { aiApiPrefix } from '@/api/education/ai'
import { getToken } from '@/utils/auth'

export function uploadExcel(file) {
  const formData = new FormData()
  formData.append('files', file.raw || file)
  return request({
    url: `${aiApiPrefix}/rag/upload-excel`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

export function uploadExcelFiles(files = []) {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file.raw || file)
  })
  return request({
    url: `${aiApiPrefix}/rag/upload-excel`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}

export function queryQuestion(question, context = {}) {
  return request({
    url: `${aiApiPrefix}/rag/query`,
    method: 'get',
    params: {
      question,
      sourceScene: context.sourceScene,
      studentNo: context.studentNo,
      studentName: context.studentName,
      courseName: context.courseName,
      chapterCode: context.chapterCode,
      chapterName: context.chapterName,
      knowledgePoint: context.knowledgePoint,
      questionId: context.questionId
    },
    timeout: 60000
  })
}

export function importCurrentSceneToKnowledge(context = {}) {
  const params = {
    sourceScene: context.sourceScene,
    studentNo: context.studentNo,
    studentName: context.studentName,
    courseName: context.courseName,
    chapterCode: context.chapterCode,
    chapterName: context.chapterName
  }

  const currentOrigin = typeof window !== 'undefined' ? window.location.origin : ''
  const hostName = typeof window !== 'undefined' ? window.location.hostname : '127.0.0.1'
  const candidateUrls = [
    `${aiApiPrefix}/rag/import-current-scene`,
    '/prod-api/education/ai/rag/import-current-scene',
    '/dev-api/education/ai/rag/import-current-scene',
    currentOrigin ? `${currentOrigin}/prod-api/education/ai/rag/import-current-scene` : '',
    currentOrigin ? `${currentOrigin}/dev-api/education/ai/rag/import-current-scene` : '',
    `http://${hostName || '127.0.0.1'}:8080/education/ai/rag/import-current-scene`,
    'http://127.0.0.1:8080/education/ai/rag/import-current-scene'
  ].filter(Boolean)

  const tried = new Set()
  const authToken = getToken()
  const headers = authToken
    ? { Authorization: `Bearer ${authToken}` }
    : {}

  const runFallback = async (lastError) => {
    for (const url of candidateUrls) {
      if (tried.has(url)) continue
      tried.add(url)
      try {
        const response = await axios({
          url,
          method: 'post',
          params,
          headers,
          timeout: 120000
        })
        return response.data
      } catch (error) {
        const status = error?.response?.status
        if (status === 404) {
          lastError = error
          continue
        }
        throw error
      }
    }
    throw lastError
  }

  return request({
    url: `${aiApiPrefix}/rag/import-current-scene`,
    method: 'post',
    params,
    timeout: 120000
  }).catch((error) => {
    if (error?.response?.status === 404 || String(error?.message || '').includes('404')) {
      return runFallback(error)
    }
    throw error
  })
}

export function logAiInteraction(data) {
  return request({
    url: `${aiApiPrefix}/interactionLog`,
    method: 'post',
    data
  })
}

export function getDatasets() {
  return request({
    url: `${aiApiPrefix}/rag/datasets`,
    method: 'get',
    timeout: 60000
  })
}

export function deleteDataset(datasetId) {
  return request({
    url: `${aiApiPrefix}/rag/datasets/${datasetId}`,
    method: 'delete',
    timeout: 60000
  })
}

export function getDatasetDetail(datasetId) {
  return request({
    url: `${aiApiPrefix}/rag/datasets/${datasetId}`,
    method: 'get',
    timeout: 60000
  })
}
