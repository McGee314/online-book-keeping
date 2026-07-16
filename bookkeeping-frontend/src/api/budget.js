import request from './request'

export function getCurrentBudget() {
  return request.get('/budget')
}

export function setBudget(data) {
  return request.post('/budget', data)
}