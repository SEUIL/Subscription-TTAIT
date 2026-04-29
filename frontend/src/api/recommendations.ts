import client from './client';
import type { Announcement, Page } from '../types';

export const getRecommendations = (page = 0, size = 12) =>
  client.get<Page<Announcement>>('/api/recommendations', { params: { page, size } }).then((r) => r.data);
