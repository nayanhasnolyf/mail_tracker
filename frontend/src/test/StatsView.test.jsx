import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import StatsView from '../components/StatsView';

const mockStats = {
  totalApplications: 25,
  activeApplications: 14,
  responseRate: 64.0,
  averageDaysToFirstResponse: 4.8,
  countsByStatus: {
    APPLIED: 8,
    ASSESSMENT: 4,
    INTERVIEW: 2,
    OFFER: 2,
    REJECTED: 9,
  },
  applicationsPerWeek: [
    { week: '2026-W40', startDate: '2026-09-28', count: 5 },
    { week: '2026-W41', startDate: '2026-10-05', count: 9 },
    { week: '2026-W42', startDate: '2026-10-12', count: 11 },
  ],
};

describe('StatsView', () => {
  it('renders loading state when stats is null', () => {
    render(<StatsView stats={null} />);
    expect(screen.getByText(/Loading application statistics/i)).toBeInTheDocument();
  });

  it('renders all KPI cards with accurate values', () => {
    render(<StatsView stats={mockStats} />);

    expect(screen.getByTestId('stats-view')).toBeInTheDocument();

    // Check KPI cards
    expect(screen.getByTestId('kpi-total')).toHaveTextContent('25');
    expect(screen.getByTestId('kpi-response-rate')).toHaveTextContent('64%');
    expect(screen.getByTestId('kpi-avg-days')).toHaveTextContent('4.8d');
    expect(screen.getByTestId('kpi-active')).toHaveTextContent('14');
    expect(screen.getByTestId('kpi-offers')).toHaveTextContent('2');
  });

  it('renders weekly applications chart container and week count pill', () => {
    render(<StatsView stats={mockStats} />);

    const weeklyChartContainer = screen.getByTestId('chart-weekly-applications');
    expect(weeklyChartContainer).toBeInTheDocument();
    expect(screen.getByText('Applications Per Week')).toBeInTheDocument();
    expect(screen.getByText(/3 weeks tracked/i)).toBeInTheDocument();
  });

  it('renders status funnel with stage counts and percentages', () => {
    render(<StatsView stats={mockStats} />);

    const funnelContainer = screen.getByTestId('chart-status-funnel');
    expect(funnelContainer).toBeInTheDocument();
    expect(screen.getByText('Status Funnel')).toBeInTheDocument();

    // Verify stage labels are rendered in the funnel
    const funnelScope = within(funnelContainer);
    expect(funnelScope.getByText('Applied')).toBeInTheDocument();
    expect(funnelScope.getByText('Assessment')).toBeInTheDocument();
    expect(funnelScope.getByText('Interview')).toBeInTheDocument();
    expect(funnelScope.getByText('Offer')).toBeInTheDocument();
    expect(funnelScope.getByText('Rejected')).toBeInTheDocument();
  });

  it('renders pipeline breakdown and response velocity health card', () => {
    render(<StatsView stats={mockStats} />);

    expect(screen.getByTestId('chart-status-distribution')).toBeInTheDocument();
    expect(screen.getByText('Pipeline Breakdown')).toBeInTheDocument();

    const velocityCard = screen.getByTestId('response-velocity-card');
    expect(velocityCard).toBeInTheDocument();
    expect(screen.getByText('Response Velocity & Health')).toBeInTheDocument();
    expect(screen.getByText('4.8')).toBeInTheDocument();
    expect(screen.getByText(/calendar days/i)).toBeInTheDocument();
  });
});
