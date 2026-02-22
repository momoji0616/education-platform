import request from '@/utils/request'

// 训练预测模型
export function trainPredictionModel(data) {
  return request({
    url: '/rag-api/train-prediction-model',
    method: 'post',
    data: data,
    baseURL: '', // 不使用默认的baseURL，避免添加dev-api前缀
    timeout: 60000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取模型信息
export function getModelInfo() {
  return request({
    url: '/rag-api/model-info',
    method: 'get',
    baseURL: '', // 不使用默认的baseURL，避免添加dev-api前缀
    timeout: 60000
  })
}

// 预测成绩
export function predictScore(data) {
  return request({
    url: '/rag-api/predict-score',
    method: 'post',
    data: data,
    baseURL: '', // 不使用默认的baseURL，避免添加dev-api前缀
    timeout: 60000
  })
}