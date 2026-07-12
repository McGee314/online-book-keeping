import request from './request'

export function getTransactions(params) {
  return request.get('/transactions', { params })
}

export function createTransaction(data) {
  return request.post('/transactions', data)
}

export function updateTransaction(id, data) {
  return request.put(`/transactions/${id}`, data)
}

export function deleteTransaction(id) {
  return request.delete(`/transactions/${id}`)
}