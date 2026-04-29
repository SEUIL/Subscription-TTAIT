export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  username: string;
  email: string;
  phone: string;
  role: Role;
  profileCompleted: boolean;
}

export interface UserProfile {
  age?: number;
  maritalStatus?: string;
  childrenCount?: number;
  regionLevel1?: string;
  regionLevel2?: string;
  minDeposit?: number;
  maxDeposit?: number;
  minMonthlyRent?: number;
  maxMonthlyRent?: number;
  categories?: string[];
}

export interface Announcement {
  announcementId: number;
  title: string;
  provider: string;
  supplyType: string;
  houseType: string;
  regionLevel1: string;
  regionLevel2: string;
  status: 'ACTIVE' | 'CLOSED' | 'UPCOMING';
  recruitmentStartDate: string;
  recruitmentEndDate: string;
  deposit?: number;
  monthlyRent?: number;
  categories: string[];
  imageUrl?: string;
}

export interface AnnouncementDetail extends Announcement {
  description?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  eligibility?: EligibilityInfo[];
  totalUnits?: number;
  supplySchedule?: ScheduleStep[];
}

export interface EligibilityInfo {
  category: string;
  condition: string;
}

export interface ScheduleStep {
  stepName: string;
  date: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AnnouncementFilter {
  regionLevel1?: string;
  regionLevel2?: string;
  supplyType?: string;
  houseType?: string;
  provider?: string;
  status?: string;
  keyword?: string;
  categories?: string[];
  minDeposit?: number;
  maxDeposit?: number;
  minMonthlyRent?: number;
  maxMonthlyRent?: number;
  page?: number;
  size?: number;
}

export interface FilterOptions {
  regions: string[];
  supplyTypes: string[];
  houseTypes: string[];
  providers: string[];
  categories: CategoryOption[];
}

export interface CategoryOption {
  code: string;
  label: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  password: string;
  email: string;
  phone: string;
  age?: number;
  maritalStatus?: string;
  childrenCount?: number;
  categories?: string[];
}
