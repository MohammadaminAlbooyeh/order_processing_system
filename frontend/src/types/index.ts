export interface User {
  id: number
  email: string
  full_name: string | null
  phone: string | null
  is_active: boolean
}

export interface AuthResponse {
  access_token: string
  token_type: string
  user: User
}

export interface OrderItem {
  product_id: number
  quantity: number
}

export interface Order {
  id: number
  user_id: number
  status: string
  destination: string | null
  weight: number
  items: OrderItem[]
  created_at: string | null
  updated_at: string | null
}

export interface OrderListResponse {
  orders: Order[]
  total: number
  limit: number
  offset: number
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterData {
  email: string
  password: string
  full_name?: string
  phone?: string
}

export interface CreateOrderData {
  destination: string
  weight: number
  items: OrderItem[]
}
