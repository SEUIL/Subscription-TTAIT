import client from './client';
import type { AuthResponse, LoginRequest, SignupRequest } from '../types';

export const login = (data: LoginRequest) =>
  client.post<AuthResponse>('/api/auth/login', data).then((r) => r.data);

export const signup = (data: SignupRequest) =>
  client.post<AuthResponse>('/api/auth/signup', data).then((r) => r.data);
