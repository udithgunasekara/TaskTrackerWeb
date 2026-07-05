export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  email: string;
  password?: string;
  fullName: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface Task {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  dueDate?: string;
  owner: User;
  createdAt: string;
  updatedAt?: string;
}

export interface TaskRequest {
  title: string;
  description?: string;
  status: TaskStatus;
  dueDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
