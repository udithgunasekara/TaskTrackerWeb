import api from '../lib/axios';
import { PageResponse, Task, TaskRequest } from '../types/api';

export const getTasks = async (page = 0, size = 10): Promise<PageResponse<Task>> => {
  const response = await api.get<PageResponse<Task>>(`/api/tasks?page=${page}&size=${size}`);
  return response.data;
};

export const createTask = async (data: TaskRequest): Promise<Task> => {
  const response = await api.post<Task>('/api/tasks', data);
  return response.data;
};

export const updateTask = async (id: number, data: TaskRequest): Promise<Task> => {
  const response = await api.put<Task>(`/api/tasks/${id}`, data);
  return response.data;
};

export const deleteTask = async (id: number): Promise<void> => {
  await api.delete(`/api/tasks/${id}`);
};
