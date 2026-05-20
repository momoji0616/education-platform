import request from '@/utils/request'
import { aiApiPrefix } from '@/api/education/ai'

export function trainPredictionModel(data) {
  return request({
    url: `${aiApiPrefix}/prediction/train`,
    method: 'post',
    data,
    timeout: 180000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getModelInfo() {
  return request({
    url: `${aiApiPrefix}/prediction/model-info`,
    method: 'get',
    timeout: 120000
  })
}

export function predictScore(data) {
  return request({
    url: `${aiApiPrefix}/prediction/predict`,
    method: 'post',
    data,
    timeout: 120000
  })
}

export function predictScoreWithAi(data) {
  return request({
    url: `${aiApiPrefix}/prediction/ai-enhanced`,
    method: 'post',
    data,
    timeout: 180000
  })
}
