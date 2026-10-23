import { useMemo } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import {
  Briefcase,
  Zap,
  Percent,
  Clock,
  Award,
  TrendingUp,
  Filter,
  PieChart as PieIcon,
  Calendar,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { STATUS_CONFIG, APPLICATION_STATUSES } from '../types/constants';

export default function StatsView({ stats }) {
  const total = stats?.totalApplications ?? 0;
  const active = stats?.activeApplications ?? 0;
  const responseRate = stats?.responseRate ?? 0;
  const avgDays = stats?.averageDaysToFirstResponse ?? 0;
  const counts = stats?.countsByStatus;

  // Funnel data: progression through stages
  const funnelData = useMemo(() => {
    const c = counts || {};
    const applied = c.APPLIED || 0;
    const assessment = c.ASSESSMENT || 0;
    const interview = c.INTERVIEW || 0;
    const offer = c.OFFER || 0;
    const rejected = c.REJECTED || 0;

    // Funnel stages: total reaching each milestone or current count
    const stages = [
      {
        stage: 'Applied',
        count: applied,
        color: STATUS_CONFIG.APPLIED.hexColor,
        pct: total > 0 ? Math.round((applied / total) * 100) : 0,
      },
      {
        stage: 'Assessment',
        count: assessment,
        color: STATUS_CONFIG.ASSESSMENT.hexColor,
        pct: total > 0 ? Math.round((assessment / total) * 100) : 0,
      },
      {
        stage: 'Interview',
        count: interview,
        color: STATUS_CONFIG.INTERVIEW.hexColor,
        pct: total > 0 ? Math.round((interview / total) * 100) : 0,
      },
      {
        stage: 'Offer',
        count: offer,
        color: STATUS_CONFIG.OFFER.hexColor,
        pct: total > 0 ? Math.round((offer / total) * 100) : 0,
      },
      {
        stage: 'Rejected',
        count: rejected,
        color: STATUS_CONFIG.REJECTED.hexColor,
        pct: total > 0 ? Math.round((rejected / total) * 100) : 0,
      },
    ];

    return stages;
  }, [counts, total]);

  // Status distribution for Donut Chart
  const statusPieData = useMemo(() => {
    const c = counts || {};
    return APPLICATION_STATUSES.map((status) => ({
      name: STATUS_CONFIG[status].label,
      value: c[status] || 0,
      color: STATUS_CONFIG[status].hexColor,
    })).filter((d) => d.value > 0);
  }, [counts]);

  // Weekly applications line/area data
  const weeklyData = useMemo(() => {
    const rawList = stats?.applicationsPerWeek || [];
    if (rawList.length === 0) {
      // Fallback display if no history yet
      return [
        { week: 'Current', count: total, startDate: 'This week' },
      ];
    }
    return rawList.map((item) => ({
      week: item.week || item.weekKey || 'Week',
      count: item.count || 0,
      startDate: item.startDate || '',
    }));
  }, [stats?.applicationsPerWeek, total]);

  if (!stats) {
    return (
      <div className="py-20 text-center text-slate-400">
        <p>Loading application statistics...</p>
      </div>
    );
  }

  // KPI cards
  const kpis = [
    {
      title: 'Total Applications',
      value: total,
      subtext: 'Tracked from Gmail',
      icon: Briefcase,
      color: 'text-sky-400',
      bg: 'bg-sky-500/10',
      border: 'border-sky-500/20',
      testId: 'kpi-total',
    },
    {
      title: 'Response Rate',
      value: `${Math.round(responseRate)}%`,
      subtext: 'Reached test, interview, offer or outcome',
      icon: Percent,
      color: 'text-purple-400',
      bg: 'bg-purple-500/10',
      border: 'border-purple-500/20',
      testId: 'kpi-response-rate',
    },
    {
      title: 'Avg Days to First Response',
      value: `${avgDays}d`,
      subtext: 'Time between applying & first reply',
      icon: Clock,
      color: 'text-amber-400',
      bg: 'bg-amber-500/10',
      border: 'border-amber-500/20',
      testId: 'kpi-avg-days',
    },
    {
      title: 'Active in Pipeline',
      value: active,
      subtext: 'Applied, test, or interview',
      icon: Zap,
      color: 'text-cyan-400',
      bg: 'bg-cyan-500/10',
      border: 'border-cyan-500/20',
      testId: 'kpi-active',
    },
    {
      title: 'Offers Received',
      value: counts.OFFER || 0,
      subtext: 'Congratulations!',
      icon: Award,
      color: 'text-emerald-400',
      bg: 'bg-emerald-500/10',
      border: 'border-emerald-500/20',
      testId: 'kpi-offers',
    },
  ];

  return (
    <div data-testid="stats-view" className="space-y-6 pb-12">
      {/* KPI Cards Row */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
        {kpis.map((kpi) => {
          const Icon = kpi.icon;
          return (
            <div
              key={kpi.title}
              data-testid={kpi.testId}
              className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md flex flex-col justify-between"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-slate-400">{kpi.title}</span>
                <div className={`p-2 rounded-xl ${kpi.bg} border ${kpi.border}`}>
                  <Icon className={`w-4 h-4 ${kpi.color}`} />
                </div>
              </div>
              <div>
                <span className="text-2xl sm:text-3xl font-extrabold text-white">{kpi.value}</span>
                <p className="text-[11px] text-slate-500 mt-1 truncate">{kpi.subtext}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Charts Grid: Applications Per Week & Status Funnel */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Applications Per Week */}
        <div
          data-testid="chart-weekly-applications"
          className="lg:col-span-7 p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md flex flex-col justify-between"
        >
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-400">
                <TrendingUp className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Applications Per Week</h3>
                <p className="text-xs text-slate-400">Volume of job submissions over time</p>
              </div>
            </div>
            <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-slate-800 text-slate-300 border border-slate-700/60">
              {weeklyData.length} weeks tracked
            </span>
          </div>

          <div className="h-64 sm:h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={weeklyData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="weeklyGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#38bdf8" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#38bdf8" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                <XAxis
                  dataKey="week"
                  stroke="#64748b"
                  tick={{ fontSize: 11, fill: '#94a3b8' }}
                  axisLine={{ stroke: '#334155' }}
                />
                <YAxis
                  allowDecimals={false}
                  stroke="#64748b"
                  tick={{ fontSize: 11, fill: '#94a3b8' }}
                  axisLine={{ stroke: '#334155' }}
                />
                <Tooltip
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      const data = payload[0].payload;
                      return (
                        <div className="p-3 rounded-xl bg-slate-900 border border-slate-700 shadow-xl text-xs">
                          <p className="font-bold text-white mb-1">{data.week}</p>
                          {data.startDate && (
                            <p className="text-slate-400 mb-1 flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              Week starting: {data.startDate}
                            </p>
                          )}
                          <p className="text-sky-400 font-semibold text-sm">
                            {data.count} application{data.count !== 1 ? 's' : ''}
                          </p>
                        </div>
                      );
                    }
                    return null;
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="#38bdf8"
                  strokeWidth={2.5}
                  fillOpacity={1}
                  fill="url(#weeklyGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Funnel */}
        <div
          data-testid="chart-status-funnel"
          className="lg:col-span-5 p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md flex flex-col justify-between"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
                <Filter className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Status Funnel</h3>
                <p className="text-xs text-slate-400">Stage volume and conversion distribution</p>
              </div>
            </div>
          </div>

          {/* Funnel Progress Bars */}
          <div className="space-y-3.5 my-auto">
            {funnelData.map((item) => (
              <div key={item.stage} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-300">{item.stage}</span>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-white">{item.count}</span>
                    <span className="text-[11px] text-slate-400 font-medium">({item.pct}%)</span>
                  </div>
                </div>
                <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{
                      width: `${Math.max(item.pct, item.count > 0 ? 4 : 0)}%`,
                      backgroundColor: item.color,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>

          <div className="pt-4 border-t border-slate-800/80 text-[11px] text-slate-400 flex items-center justify-between">
            <span>Overall Conversion to Interview:</span>
            <span className="font-bold text-amber-400">
              {total > 0 ? Math.round(((counts.INTERVIEW || 0) / total) * 100) : 0}%
            </span>
          </div>
        </div>
      </div>

      {/* Second Row: Status Distribution & Velocity Insights */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Status Distribution Donut Chart */}
        <div
          data-testid="chart-status-distribution"
          className="lg:col-span-6 p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md"
        >
          <div className="flex items-center gap-2.5 mb-4">
            <div className="p-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <PieIcon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Pipeline Breakdown</h3>
              <p className="text-xs text-slate-400">Distribution across active and closed stages</p>
            </div>
          </div>

          <div className="h-64 w-full flex items-center justify-center">
            {statusPieData.length === 0 ? (
              <p className="text-xs text-slate-500">No applications recorded yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={statusPieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {statusPieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} stroke="#0f172a" strokeWidth={2} />
                    ))}
                  </Pie>
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0];
                        const pct = total > 0 ? Math.round((data.value / total) * 100) : 0;
                        return (
                          <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs shadow-xl">
                            <span className="font-semibold text-white">{data.name}: </span>
                            <span className="font-bold text-sky-400">{data.value}</span> ({pct}%)
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    iconType="circle"
                    formatter={(val) => <span className="text-xs text-slate-300 font-medium">{val}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Velocity & Response Rate Insights Card */}
        <div
          data-testid="response-velocity-card"
          className="lg:col-span-6 p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md flex flex-col justify-between"
        >
          <div>
            <div className="flex items-center gap-2.5 mb-6">
              <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Response Velocity & Health</h3>
                <p className="text-xs text-slate-400">Key responsiveness indicators and benchmarks</p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80">
                <span className="text-xs text-slate-400 font-medium">Response Rate</span>
                <div className="flex items-baseline gap-2 mt-1">
                  <span className="text-2xl font-bold text-white">{Math.round(responseRate)}%</span>
                  <span className="text-[11px] text-emerald-400 font-medium">
                    {total > 0 ? Math.round((responseRate * total) / 100) : 0} of {total} replied
                  </span>
                </div>
                <div className="w-full h-1.5 rounded-full bg-slate-800 mt-3 overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-sky-400 to-indigo-500 rounded-full"
                    style={{ width: `${Math.min(100, Math.max(0, responseRate))}%` }}
                  />
                </div>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80">
                <span className="text-xs text-slate-400 font-medium">Avg. Days to First Response</span>
                <div className="flex items-baseline gap-2 mt-1">
                  <span className="text-2xl font-bold text-white">{avgDays}</span>
                  <span className="text-xs text-slate-400">calendar days</span>
                </div>
                <p className="text-[11px] text-slate-500 mt-2">
                  {avgDays <= 7
                    ? '⚡ Fast response velocity (< 7 days)'
                    : avgDays <= 14
                    ? '⏱️ Moderate response timeline'
                    : '⏳ Longer turnaround cycle'}
                </p>
              </div>
            </div>
          </div>

          <div className="p-3.5 rounded-xl bg-sky-950/30 border border-sky-800/40 text-xs text-sky-200 flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 text-sky-400 flex-shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              <strong>Tip:</strong> If an application has been in <code className="text-sky-300">Applied</code> for more than 7 days without a response, use the <strong>Needs Follow-Up</strong> panel in the header to take action.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
