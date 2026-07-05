export type Role = 'USER' | 'ADMIN';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface User { id: number; email: string; fullName: string; role: Role; }
export interface Task {
  id: number; title: string; description: string | null;
  status: TaskStatus; dueDate: string | null;
  owner: User; createdAt: string; updatedAt: string;
}
export interface PageResponse<T> {
  content: T[]; page: number; size: number;
  totalElements: number; totalPages: number; last: boolean;
}
export interface AuthResponse extends User { token: string; }
