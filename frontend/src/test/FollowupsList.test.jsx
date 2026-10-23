import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FollowupsList from '../components/FollowupsList';

const mockFollowups = [
  {
    id: 'app-f1',
    company: 'Airbnb',
    role: 'Infrastructure Engineer',
    currentStatus: 'APPLIED',
    lastUpdatedAt: new Date(Date.now() - 86400000 * 10).toISOString(),
  },
  {
    id: 'app-f2',
    company: 'Uber',
    role: 'Platform Engineer',
    currentStatus: 'APPLIED',
    lastUpdatedAt: new Date(Date.now() - 86400000 * 18).toISOString(),
  },
];

describe('FollowupsList', () => {
  it('renders follow-up applications with waiting duration', () => {
    render(
      <FollowupsList
        followups={mockFollowups}
        daysThreshold={7}
        onChangeDays={vi.fn()}
        onSelectApplication={vi.fn()}
      />
    );

    expect(screen.getByText('Needs Follow-Up')).toBeInTheDocument();
    expect(screen.getByText('Airbnb')).toBeInTheDocument();
    expect(screen.getByText('Uber')).toBeInTheDocument();
    expect(screen.getByText(/10 days waiting/i)).toBeInTheDocument();
    expect(screen.getByText(/18 days waiting/i)).toBeInTheDocument();
  });

  it('triggers threshold days change when day button clicked', async () => {
    const handleChangeDays = vi.fn();
    render(
      <FollowupsList
        followups={mockFollowups}
        daysThreshold={7}
        onChangeDays={handleChangeDays}
        onSelectApplication={vi.fn()}
      />
    );

    const button14d = screen.getByText('14d');
    await userEvent.click(button14d);

    expect(handleChangeDays).toHaveBeenCalledWith(14);
  });

  it('calls onSelectApplication when card is clicked', async () => {
    const handleSelect = vi.fn();
    render(
      <FollowupsList
        followups={mockFollowups}
        daysThreshold={7}
        onChangeDays={vi.fn()}
        onSelectApplication={handleSelect}
      />
    );

    const card = screen.getByTestId('followup-card-app-f1');
    await userEvent.click(card);

    expect(handleSelect).toHaveBeenCalledWith(mockFollowups[0]);
  });

  it('renders empty message when no applications need follow-up', () => {
    render(
      <FollowupsList
        followups={[]}
        daysThreshold={7}
        onChangeDays={vi.fn()}
        onSelectApplication={vi.fn()}
      />
    );

    expect(screen.getByText('All caught up!')).toBeInTheDocument();
  });
});
