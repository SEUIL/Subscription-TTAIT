import client from './client';
import type { User, UserProfile } from '../types';

export const getMe = () =>
  client.get<User>('/api/me').then((r) => r.data);

export const updateProfile = (data: UserProfile) =>
  client.put('/api/me/profile', data).then((r) => r.data);
