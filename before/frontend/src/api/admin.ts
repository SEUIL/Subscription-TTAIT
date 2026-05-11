import client from './client';

export interface AdminStats {
  total: number;
  pending: number;
  approved: number;
  corrected: number;
  rejected: number;
  todayProcessed: number;
}

export interface AdminReviewItem {
  announcementId: number;
  title: string;
  provider: string;
  status: string;
  parseReviewStatus: string;
  createdAt: string;
}

export interface AdminReviewDetail {
  announcementId: number;
  title: string;
  provider: string;
  parseReviewStatus: string;
  rawPdfText?: string;
  parsedResult?: Record<string, unknown>;
}

export interface ManualAnnouncementRequest {
  title: string;
  provider: string;
  supplyType: string;
  houseType: string;
  regionLevel1: string;
  regionLevel2?: string;
  recruitmentStartDate: string;
  recruitmentEndDate: string;
  deposit?: number;
  monthlyRent?: number;
}

export const getAdminStats = () =>
  client.get<AdminStats>('/api/admin/review/stats').then((r) => r.data);

export const getAdminReviewList = (status?: string, page = 0, size = 20) =>
  client.get('/api/admin/review', { params: { status, page, size } }).then((r) => r.data);

export const getAdminReviewDetail = (id: number) =>
  client.get<AdminReviewDetail>(`/api/admin/review/${id}`).then((r) => r.data);

export const submitReview = (id: number, action: string, correctedData?: Record<string, unknown>) =>
  client.post(`/api/admin/review/${id}`, { action, correctedData }).then((r) => r.data);

export const deleteAnnouncement = (id: number) =>
  client.delete(`/api/admin/announcements/${id}`).then((r) => r.data);

export const createAnnouncement = (data: ManualAnnouncementRequest) =>
  client.post('/api/admin/announcements', data).then((r) => r.data);

export const importLh = (page = 1, size = 10) =>
  client.post('/api/admin/import/lh', null, { params: { page, size } }).then((r) => r.data);
