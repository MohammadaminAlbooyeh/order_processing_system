import { apiGet, apiPost, apiPut, apiDelete } from './client'
import type { Order, OrderListResponse, CreateOrderData } from '../types'

export function listOrders(params?: {
  status?: string
  limit?: number
  offset?: number
}): Promise<OrderListResponse> {
  const query = new URLSearchParams()
  if (params?.status) query.set('status', params.status)
  if (params?.limit) query.set('limit', String(params.limit))
  if (params?.offset) query.set('offset', String(params.offset))
  const qs = query.toString()
  return apiGet<OrderListResponse>(`/api/orders${qs ? '?' + qs : ''}`)
}

export function getOrder(id: number): Promise<Order> {
  return apiGet<Order>(`/api/orders/${id}`)
}

export function createOrder(data: CreateOrderData): Promise<Order> {
  return apiPost<Order>('/api/orders', data)
}

export function updateOrder(
  id: number,
  data: { status?: string; destination?: string }
): Promise<Order> {
  return apiPut<Order>(`/api/orders/${id}`, data)
}

export function cancelOrder(id: number): Promise<{ detail: string; order_id: number }> {
  return apiDelete(`/api/orders/${id}`)
}
