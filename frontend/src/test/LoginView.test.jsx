import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginView from '../components/LoginView';

describe('LoginView', () => {
  it('renders sign in with google link with correct oauth2 endpoint', () => {
    render(<LoginView onStartDemo={vi.fn()} />);

    const googleBtn = screen.getByTestId('google-signin-btn');
    expect(googleBtn).toBeInTheDocument();
    expect(googleBtn).toHaveAttribute('href', '/oauth2/authorization/google');
    expect(screen.getByText('Sign in with Google')).toBeInTheDocument();
  });

  it('renders features and demo mode button', async () => {
    const handleStartDemo = vi.fn();
    render(<LoginView onStartDemo={handleStartDemo} />);

    expect(screen.getAllByText(/Application Tracker/i).length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: /Kanban Board/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Follow-up Alerts/i })).toBeInTheDocument();

    const demoBtn = screen.getByTestId('demo-mode-btn');
    expect(demoBtn).toBeInTheDocument();

    await userEvent.click(demoBtn);
    expect(handleStartDemo).toHaveBeenCalledTimes(1);
  });

  it('renders error message when error prop is passed', () => {
    render(<LoginView onStartDemo={vi.fn()} error="Session expired. Please sign in again." />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Session expired. Please sign in again.')).toBeInTheDocument();
  });
});
