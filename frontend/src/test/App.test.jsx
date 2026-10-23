import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../App';
import { api } from '../api/client';

describe('App Integration', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders login view when unauthenticated', async () => {
    vi.spyOn(api, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'));

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('google-signin-btn')).toBeInTheDocument();
    });
    expect(screen.getByTestId('demo-mode-btn')).toBeInTheDocument();
  });

  it('enters demo mode and renders dashboard with Kanban cards', async () => {
    vi.spyOn(api, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'));

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('demo-mode-btn')).toBeInTheDocument();
    });

    const demoBtn = screen.getByTestId('demo-mode-btn');
    await userEvent.click(demoBtn);

    // Dashboard should now be loaded with demo badge, kanban board, and stats
    await waitFor(() => {
      expect(screen.getByTestId('demo-badge')).toBeInTheDocument();
      expect(screen.getByTestId('kanban-board')).toBeInTheDocument();
    });

    expect(screen.getByText('Stripe')).toBeInTheDocument();
    expect(screen.getByText('Total Applications')).toBeInTheDocument();
  });

  it('switches between Kanban and Table views in dashboard', async () => {
    vi.spyOn(api, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'));

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('demo-mode-btn')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('demo-mode-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('kanban-board')).toBeInTheDocument();
    });

    // Switch to Table view
    const tableViewBtn = screen.getByTestId('view-table-btn');
    await userEvent.click(tableViewBtn);

    expect(screen.getByTestId('table-view')).toBeInTheDocument();
    expect(screen.queryByTestId('kanban-board')).not.toBeInTheDocument();

    // Switch back to Kanban
    const kanbanViewBtn = screen.getByTestId('view-kanban-btn');
    await userEvent.click(kanbanViewBtn);

    expect(screen.getByTestId('kanban-board')).toBeInTheDocument();
  });

  it('toggles follow-ups list', async () => {
    vi.spyOn(api, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'));

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('demo-mode-btn')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('demo-mode-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('toggle-followups-btn')).toBeInTheDocument();
    });

    const toggleBtn = screen.getByTestId('toggle-followups-btn');
    await userEvent.click(toggleBtn);

    expect(screen.getByTestId('followups-panel')).toBeInTheDocument();
  });
});
