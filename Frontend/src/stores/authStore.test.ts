import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';
import type { User } from '../types';

describe('authStore', () => {
  const mockUser: User = {
    id: 1,
    email: 'test@example.com',
    fullName: 'Test User',
    role: 'USER',
  };

  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null });
  });

  it('initial state should be empty', () => {
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
  });

  it('login should set token and user', () => {
    useAuthStore.getState().login('mock-token', mockUser);
    
    const state = useAuthStore.getState();
    expect(state.token).toBe('mock-token');
    expect(state.user).toEqual(mockUser);
  });

  it('logout should clear token and user', () => {
    useAuthStore.getState().login('mock-token', mockUser);
    useAuthStore.getState().logout();
    
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
  });
});
