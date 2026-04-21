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

const PrivateRoute = ({ children, role }) => {
    const userRole = localStorage.getItem("role");
    const token = localStorage.getItem("accessToken");

    if (!token) return <Navigate to="/" />;
    if (role && userRole !== role) return <Navigate to="/" />;

    return children;
};

const globalStyles = {
    appContainer: {
        backgroundColor: '#0f172a', 
        minHeight: '100vh',
        width: '100%',
        margin: 0,
        padding: 0,
        overflowX: 'hidden'
    }
};

function App() {
  return (
    <div style={globalStyles.appContainer}>
      <Router>
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