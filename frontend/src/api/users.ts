import { apiGet, apiPost, apiPatch } from './client'
import type { User, AuthResponse, LoginCredentials, RegisterData } from '../types'

export function login(credentials: LoginCredentials): Promise<AuthResponse> {
  return apiPost<AuthResponse>('/api/users/login', credentials)
}

export function register(data: RegisterData): Promise<AuthResponse> {
  return apiPost<AuthResponse>('/api/users/register', data)
}

export function getUser(id: number): Promise<User> {
  return apiGet<User>(`/api/users/${id}`)
}

export function getProfile(): Promise<User> {
  return apiGet<User>('/api/users/me')
}

export function updateProfile(data: {
  full_name?: string
  phone?: string
}): Promise<User> {
  return apiPatch<User>('/api/users/me', data)
}
