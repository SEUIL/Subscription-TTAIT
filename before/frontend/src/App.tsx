import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';

import AuthPage from './pages/AuthPage';
import Layout from './components/common/Layout';
import AnnouncementListPage from './pages/AnnouncementListPage';
import AnnouncementDetailPage from './pages/AnnouncementDetailPage';
import MyPageComponent from './pages/MyPage';
import RecommendationsPage from './pages/RecommendationsPage';
import AdminLayout from './pages/admin/AdminLayout';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminReviewListPage from './pages/admin/AdminReviewListPage';
import AdminReviewDetailPage from './pages/admin/AdminReviewDetailPage';
import AdminImportPage from './pages/admin/AdminImportPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* 헤더 없는 페이지 */}
          <Route path="/auth" element={<AuthPage />} />

          {/* 헤더 있는 페이지 */}
          <Route element={<Layout />}>
            <Route path="/" element={<AnnouncementListPage />} />
            <Route path="/announcements/:id" element={<AnnouncementDetailPage />} />
            <Route path="/me" element={
              <ProtectedRoute><MyPageComponent /></ProtectedRoute>
            } />
            <Route path="/recommendations" element={
              <ProtectedRoute><RecommendationsPage /></ProtectedRoute>
            } />

            {/* 어드민 */}
            <Route path="/admin" element={
              <ProtectedRoute adminOnly><AdminLayout /></ProtectedRoute>
            }>
              <Route index element={<AdminDashboardPage />} />
              <Route path="review" element={<AdminReviewListPage />} />
              <Route path="review/:id" element={<AdminReviewDetailPage />} />
              <Route path="import" element={<AdminImportPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
