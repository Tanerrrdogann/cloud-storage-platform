import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminPage from './pages/AdminPage';
import ModeratorPage from './pages/ModeratorPage';
import UserPage from './pages/UserPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import VerifyEmailPage from './pages/VerifyEmailPage';

const basePath = process.env.REACT_APP_PUBLIC_BASE_PATH || '';

function useNtworksDemoHeartbeat() {
  React.useEffect(() => {
    const heartbeat = () => {
      fetch('/api/demos/cloud-storage-platform/heartbeat', {
        method: 'POST',
        keepalive: true
      }).catch(() => undefined);
    };

    heartbeat();
    const timer = window.setInterval(heartbeat, 20000);
    window.addEventListener('focus', heartbeat);
    window.addEventListener('pageshow', heartbeat);

    return () => {
      window.clearInterval(timer);
      window.removeEventListener('focus', heartbeat);
      window.removeEventListener('pageshow', heartbeat);
    };
  }, []);
}

const PrivateRoute = ({ children, role }) => {
    const userRole = localStorage.getItem("role");
    const token = localStorage.getItem("accessToken");

    if (!token) return <Navigate to="/" />;
    if (role && userRole !== role) return <Navigate to="/" />;

    return children;
};

const globalStyles = {
    appContainer: {
        background: 'radial-gradient(circle at 18% 10%, rgba(216, 216, 216, 0.34), transparent 30%), #121212',
        minHeight: '100vh',
        width: '100%',
        margin: 0,
        padding: 0,
        overflowX: 'hidden'
    }
};

function App() {
  useNtworksDemoHeartbeat();

  return (
    <div style={globalStyles.appContainer}>
      <Router basename={basePath}>
        <Routes>
          <Route path="/" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/verify" element={<VerifyEmailPage />} />
          
          <Route path="/admin" element={
              <PrivateRoute role="ROLE_ADMIN">
                  <AdminPage />
              </PrivateRoute>
          } />

          <Route path="/moderator" element={
              <PrivateRoute role="ROLE_MODERATOR">
                  <ModeratorPage />
              </PrivateRoute>
          } />

          <Route path="/user" element={
              <PrivateRoute role="ROLE_USER">
                  <UserPage />
              </PrivateRoute>
          } />

          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Router>
    </div>
  );
}

export default App;
