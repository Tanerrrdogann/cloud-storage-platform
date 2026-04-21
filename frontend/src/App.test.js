import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('react-router-dom', () => {
  const React = require('react');

  return {
    BrowserRouter: ({ children }) => React.createElement('div', null, children),
    Routes: ({ children }) => React.createElement('div', null, children),
    Route: ({ element }) => React.createElement('div', null, element),
    Navigate: ({ to }) => React.createElement('div', { 'data-testid': 'navigate', 'data-to': to }),
  };
}, { virtual: true });

jest.mock('./pages/LoginPage', () => () => <div>Login Page</div>);
jest.mock('./pages/RegisterPage', () => () => <div>Register Page</div>);
jest.mock('./pages/AdminPage', () => () => <div>Admin Page</div>);
jest.mock('./pages/ModeratorPage', () => () => <div>Moderator Page</div>);
jest.mock('./pages/UserPage', () => () => <div>User Page</div>);
jest.mock('./pages/ForgotPasswordPage', () => () => <div>Forgot Password Page</div>);
jest.mock('./pages/ResetPasswordPage', () => () => <div>Reset Password Page</div>);
jest.mock('./pages/VerifyEmailPage', () => () => <div>Verify Email Page</div>);

test('renders the auth application routes', () => {
  render(<App />);
  expect(screen.getByText(/login page/i)).toBeInTheDocument();
});
