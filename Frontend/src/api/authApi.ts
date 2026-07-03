import axiosClient from './axiosClient';
import type { AuthResponse, User } from '../types';

export const register = async (data: any): Promise<AuthResponse> => {
  const response = await axiosClient.post<AuthResponse>('/api/auth/register', data);
  return response.data;
};

export const login = async (data: any): Promise<AuthResponse> => {
  const response = await axiosClient.post<AuthResponse>('/api/auth/login', data);
  return response.data;
};

export const me = async (): Promise<User> => {
  const response = await axiosClient.get<User>('/api/auth/me');
  return response.data;
};
