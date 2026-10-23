import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ApplicationDrawer from '../components/ApplicationDrawer';

const mockApp = {
  id: 'app-100',
  company: 'Datadog',
  role: 'Staff SRE',
  currentStatus: 'ASSESSMENT',
  notes: 'Take-home project due Friday',
  sourceEmailId: 'msg-datadog-99',
  firstSeenAt: '2026-03-01T12:00:00Z',
  lastUpdatedAt: '2026-03-05T15:30:00Z',
  statusHistory: [
    {
      id: 'h-1',
      status: 'ASSESSMENT',
      changedAt: '2026-03-05T15:30:00Z',
      sourceEmailId: 'msg-datadog-99',
    },
    {
      id: 'h-2',
      status: 'APPLIED',
      changedAt: '2026-03-01T12:00:00Z',
      sourceEmailId: 'msg-datadog-01',
    },
  ],
};

describe('ApplicationDrawer', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ApplicationDrawer
        application={mockApp}
        isOpen={false}
        onClose={vi.fn()}
        onUpdateStatus={vi.fn()}
        onSaveNotes={vi.fn()}
      />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders application details and status history timeline', () => {
    render(
      <ApplicationDrawer
        application={mockApp}
        isOpen={true}
        onClose={vi.fn()}
        onUpdateStatus={vi.fn()}
        onSaveNotes={vi.fn()}
      />
    );

    expect(screen.getByText('Datadog')).toBeInTheDocument();
    expect(screen.getByText('Staff SRE')).toBeInTheDocument();
    expect(screen.getByText(/Source Email ID: msg-datadog-99/i)).toBeInTheDocument();
    expect(screen.getByText('Status History Timeline')).toBeInTheDocument();

    const notesInput = screen.getByTestId('app-notes');
    expect(notesInput).toHaveValue('Take-home project due Friday');
  });

  it('calls onUpdateStatus when status selector changed', async () => {
    const handleUpdateStatus = vi.fn().mockResolvedValue({});
    render(
      <ApplicationDrawer
        application={mockApp}
        isOpen={true}
        onClose={vi.fn()}
        onUpdateStatus={handleUpdateStatus}
        onSaveNotes={vi.fn()}
      />
    );

    const select = screen.getByTestId('status-select');
    await userEvent.selectOptions(select, 'INTERVIEW');

    expect(handleUpdateStatus).toHaveBeenCalledWith('app-100', 'INTERVIEW');
  });

  it('calls onSaveNotes when notes form submitted', async () => {
    const handleSaveNotes = vi.fn().mockResolvedValue({});
    render(
      <ApplicationDrawer
        application={mockApp}
        isOpen={true}
        onClose={vi.fn()}
        onUpdateStatus={vi.fn()}
        onSaveNotes={handleSaveNotes}
      />
    );

    const notesInput = screen.getByTestId('app-notes');
    await userEvent.clear(notesInput);
    await userEvent.type(notesInput, 'Passed assessment with 100%!');

    const saveBtn = screen.getByTestId('save-notes-btn');
    await userEvent.click(saveBtn);

    expect(handleSaveNotes).toHaveBeenCalledWith('app-100', 'Passed assessment with 100%!');
  });

  it('calls onClose when close button clicked', async () => {
    const handleClose = vi.fn();
    render(
      <ApplicationDrawer
        application={mockApp}
        isOpen={true}
        onClose={handleClose}
        onUpdateStatus={vi.fn()}
        onSaveNotes={vi.fn()}
      />
    );

    const closeBtn = screen.getByTestId('drawer-close-btn');
    await userEvent.click(closeBtn);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});
