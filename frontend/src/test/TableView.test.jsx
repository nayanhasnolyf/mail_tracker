import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TableView from '../components/TableView';

const mockApplications = [
  {
    id: 'app-1',
    company: 'Stripe',
    role: 'Staff Engineer',
    currentStatus: 'APPLIED',
    notes: 'Referral by friend',
    firstSeenAt: '2026-03-01T10:00:00Z',
    lastUpdatedAt: '2026-03-01T10:00:00Z',
  },
  {
    id: 'app-2',
    company: 'Figma',
    role: 'Frontend Architect',
    currentStatus: 'ASSESSMENT',
    notes: 'Take-home coding challenge',
    firstSeenAt: '2026-03-05T10:00:00Z',
    lastUpdatedAt: '2026-03-10T10:00:00Z',
  },
  {
    id: 'app-3',
    company: 'Netflix',
    role: 'Backend Specialist',
    currentStatus: 'INTERVIEW',
    notes: 'Final round scheduled',
    firstSeenAt: '2026-03-12T10:00:00Z',
    lastUpdatedAt: '2026-03-15T10:00:00Z',
  },
];

describe('TableView', () => {
  it('renders all rows and columns', () => {
    render(<TableView applications={mockApplications} onSelectApplication={vi.fn()} />);

    expect(screen.getByTestId('table-row-app-1')).toBeInTheDocument();
    expect(screen.getByTestId('table-row-app-2')).toBeInTheDocument();
    expect(screen.getByTestId('table-row-app-3')).toBeInTheDocument();

    expect(screen.getByText('Stripe')).toBeInTheDocument();
    expect(screen.getByText('Figma')).toBeInTheDocument();
    expect(screen.getByText('Netflix')).toBeInTheDocument();
  });

  it('filters rows by search term input', async () => {
    render(<TableView applications={mockApplications} onSelectApplication={vi.fn()} />);

    const searchInput = screen.getByTestId('table-search-input');
    await userEvent.type(searchInput, 'Figma');

    expect(screen.getByText('Figma')).toBeInTheDocument();
    expect(screen.queryByText('Stripe')).not.toBeInTheDocument();
    expect(screen.queryByText('Netflix')).not.toBeInTheDocument();
  });

  it('filters rows by status dropdown', async () => {
    render(<TableView applications={mockApplications} onSelectApplication={vi.fn()} />);

    const filterSelect = screen.getByTestId('table-status-filter');
    await userEvent.selectOptions(filterSelect, 'INTERVIEW');

    expect(screen.getByText('Netflix')).toBeInTheDocument();
    expect(screen.queryByText('Stripe')).not.toBeInTheDocument();
    expect(screen.queryByText('Figma')).not.toBeInTheDocument();
  });

  it('calls onSelectApplication when row is clicked', async () => {
    const handleSelect = vi.fn();
    render(<TableView applications={mockApplications} onSelectApplication={handleSelect} />);

    const row = screen.getByTestId('table-row-app-2');
    await userEvent.click(row);

    expect(handleSelect).toHaveBeenCalledWith(mockApplications[1]);
  });
});
