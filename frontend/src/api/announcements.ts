import client from './client';
import type { Announcement, AnnouncementDetail, AnnouncementFilter, Page } from '../types';

export const getAnnouncements = (params: AnnouncementFilter) =>
  client.get<Page<Announcement>>('/api/announcements', { params }).then((r) => r.data);

export const getAnnouncementDetail = (id: number) =>
  client.get<AnnouncementDetail>(`/api/announcements/${id}`).then((r) => r.data);

export const getFilterRegions = () =>
  client.get<string[]>('/api/filters/regions').then((r) => r.data);

export const getFilterRegionsLevel2 = (level1?: string) =>
  client.get<string[]>('/api/filters/regions/level2', { params: { level1 } }).then((r) => r.data);

export const getFilterSupplyTypes = () =>
  client.get<string[]>('/api/filters/supply-types').then((r) => r.data);

export const getFilterHouseTypes = () =>
  client.get<string[]>('/api/filters/house-types').then((r) => r.data);

export const getFilterProviders = () =>
  client.get<string[]>('/api/filters/providers').then((r) => r.data);

export const getFilterCategories = () =>
  client.get<{ code: string; label: string }[]>('/api/filters/categories').then((r) => r.data);
