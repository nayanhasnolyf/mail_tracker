import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import KanbanBoard from '../components/KanbanBoard';

const mockApplications = [
  {
    id: 'app-1',
    company: 'Stripe',
    role: 'Staff Engineer',
    currentStatus: 'APPLIED',
    notes: 'Submitted resume',
    lastUpdatedAt: new Date().toISOString(),
  },
  {
    id: 'app-2',
    company: 'Google',
    role: 'Software Engineer',
    currentStatus: 'INTERVIEW',
    notes: 'Phone screen done',
    lastUpdatedAt: new Date().toISOString(),
  },
  {
    id: 'app-3',
    company: 'Apple',
    role: 'iOS Engineer',
    currentStatus: 'OFFER',
    notes: 'Offer letter received',
    lastUpdatedAt: new Date().toISOString(),
  },
];

describe('KanbanBoard', () => {
  it('renders all status columns with counts', () => {
    render(
      <KanbanBoard
        applications={mockApplications}
        onStatusChange={vi.fn()}
        onSelectApplication={vi.fn()}
      />
    );

    expect(screen.getByTestId('kanban-column-APPLIED')).toBeInTheDocument();
    expect(screen.getByTestId('kanban-column-ASSESSMENT')).toBeInTheDocument();
    expect(screen.getByTestId('kanban-column-INTERVIEW')).toBeInTheDocument();
    expect(screen.getByTestId('kanban-column-OFFER')).toBeInTheDocument();
    expect(screen.getByTestId('kanban-column-REJECTED')).toBeInTheDocument();

    expect(screen.getByTestId('column-count-APPLIED')).toHaveTextContent('1');
    expect(screen.getByTestId('column-count-ASSESSMENT')).toHaveTextContent('0');
    expect(screen.getByTestId('column-count-INTERVIEW')).toHaveTextContent('1');
    expect(screen.getByTestId('column-count-OFFER')).toHaveTextContent('1');
  });

  it('renders application card details', () => {
    render(
      <KanbanBoard
        applications={mockApplications}
        onStatusChange={vi.fn()}
        onSelectApplication={vi.fn()}
      />
    );

    expect(screen.getByText('Stripe')).toBeInTheDocument();
    expect(screen.getByText('Staff Engineer')).toBeInTheDocument();
    expect(screen.getByText('Submitted resume')).toBeInTheDocument();
  });

  it('calls onSelectApplication when card is clicked', async () => {
    const handleSelect = vi.fn();
    render(
      <KanbanBoard
        applications={mockApplications}
        onStatusChange={vi.fn()}
        onSelectApplication={handleSelect}
      />
    );

    const card = screen.getByTestId('application-card-app-1');
    await userEvent.click(card);

    expect(handleSelect).toHaveBeenCalledWith(mockApplications[0]);
  });

  it('handles drag and drop to trigger onStatusChange', () => {
    const handleStatusChange = vi.fn();
    render(
      <KanbanBoard
        applications={mockApplications}
        onStatusChange={handleStatusChange}
        onSelectApplication={vi.fn()}
      />
    );

    const interviewColumn = screen.getByTestId('kanban-column-INTERVIEW');

    // Simulate drop of app-1 into INTERVIEW column
    fireEvent.drop(interviewColumn, {
      dataTransfer: {
        getData: (type) => (type === 'text/plain' ? 'app-1' : ''),
      },
    });

    expect(handleStatusChange).toHaveBeenCalledWith('app-1', 'INTERVIEW');
  });
});
