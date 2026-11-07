import { Navigate, Route, Routes } from 'react-router';
import { RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { HomePage } from './pages/HomePage';
import { CvPage } from './pages/CvPage';
import { CvReviewPage } from './pages/CvReviewPage';
import { ProfilePage } from './pages/ProfilePage';
import { PreferencesPage } from './pages/PreferencesPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="cv" element={<CvPage />} />
        <Route path="cv/:id/review" element={<CvReviewPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="preferences" element={<PreferencesPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
