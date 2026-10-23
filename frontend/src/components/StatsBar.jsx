import { Briefcase, Zap, Percent, Award, Users } from 'lucide-react';

export default function StatsBar({ stats }) {
  if (!stats) return null;

  const total = stats.totalApplications ?? 0;
  const active = stats.activeApplications ?? 0;
  const responseRate = stats.responseRate !== undefined ? `${Math.round(stats.responseRate)}%` : '0%';
  const offers = stats.countsByStatus?.OFFER ?? 0;
  const interviews = stats.countsByStatus?.INTERVIEW ?? 0;

  const cards = [
    {
      label: 'Total Applications',
      value: total,
      icon: Briefcase,
      color: 'text-sky-400',
      bg: 'bg-sky-500/10',
      border: 'border-sky-500/20',
    },
    {
      label: 'Active Pipeline',
      value: active,
      subtext: 'Applied, tests, or interviews',
      icon: Zap,
      color: 'text-purple-400',
      bg: 'bg-purple-500/10',
      border: 'border-purple-500/20',
    },
    {
      label: 'Response Rate',
      value: responseRate,
      subtext: 'Moved beyond Applied',
      icon: Percent,
      color: 'text-indigo-400',
      bg: 'bg-indigo-500/10',
      border: 'border-indigo-500/20',
    },
    {
      label: 'Interviews',
      value: interviews,
      icon: Users,
      color: 'text-amber-400',
      bg: 'bg-amber-500/10',
      border: 'border-amber-500/20',
    },
    {
      label: 'Offers',
      value: offers,
      icon: Award,
      color: 'text-emerald-400',
      bg: 'bg-emerald-500/10',
      border: 'border-emerald-500/20',
    },
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 mb-6">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <div
            key={card.label}
            className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm flex items-center justify-between"
          >
            <div>
              <p className="text-xs font-medium text-slate-400">{card.label}</p>
              <p className="text-xl sm:text-2xl font-bold text-white mt-1">{card.value}</p>
            </div>
            <div className={`p-2.5 rounded-lg ${card.bg} border ${card.border}`}>
              <Icon className={`w-4 h-4 ${card.color}`} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
