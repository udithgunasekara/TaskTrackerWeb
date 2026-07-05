import axiosClient from './axiosClient';
import type { PageResponse, Task, TaskStatus } from '../types';

export interface TaskFilters {
  page?: number;
  size?: number;
  status?: TaskStatus;
  ownerId?: number;
}

export const getTasks = async (params: TaskFilters): Promise<PageResponse<Task>> => {
  const response = await axiosClient.get<PageResponse<Task>>('/api/tasks', { params });
  return response.data;
};

export const getTask = async (id: number): Promise<Task> => {
  const response = await axiosClient.get<Task>(`/api/tasks/${id}`);
  return response.data;
};

export const createTask = async (data: any): Promise<Task> => {
  const response = await axiosClient.post<Task>('/api/tasks', data);
  return response.data;
};

export const updateTask = async (id: number, data: any): Promise<Task> => {
  const response = await axiosClient.put<Task>(`/api/tasks/${id}`, data);
  return response.data;
};

export const deleteTask = async (id: number): Promise<void> => {
  await axiosClient.delete(`/api/tasks/${id}`);
};
