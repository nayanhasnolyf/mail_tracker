import { useState, useEffect } from 'react';
import { Bell, Clock, Building2, ChevronRight, X } from 'lucide-react';

export default function FollowupsList({
  followups = [],
  daysThreshold = 7,
  onChangeDays,
  onSelectApplication,
  onClose,
  loading = false,
}) {
  const [selectedDays, setSelectedDays] = useState(daysThreshold);

  useEffect(() => {
    setSelectedDays(daysThreshold);
  }, [daysThreshold]);

  const handleDaysChange = (days) => {
    setSelectedDays(days);
    if (onChangeDays) {
      onChangeDays(days);
    }
  };

  const getDaysWaiting = (dateStr) => {
    if (!dateStr) return 0;
    const date = new Date(dateStr);
    const now = new Date();
    return Math.max(1, Math.floor((now - date) / (1000 * 60 * 60 * 24)));
  };

  return (
    <div
      data-testid="followups-panel"
      className="p-5 rounded-2xl bg-slate-900/90 border border-amber-500/30 shadow-2xl backdrop-blur-md mb-6"
    >
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              Needs Follow-Up
              <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                {followups.length}
              </span>
            </h3>
            <p className="text-xs text-slate-400">
              Applications still in Applied status with no update received
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Threshold selector */}
          <div className="flex items-center bg-slate-950/80 border border-slate-800 p-1 rounded-xl text-xs">
            <span className="text-slate-400 px-2 font-medium">Stale after:</span>
            {[7, 14, 30].map((days) => (
              <button
                key={days}
                onClick={() => handleDaysChange(days)}
                className={`px-2.5 py-1 rounded-lg font-medium transition ${
                  selectedDays === days
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {days}d
              </button>
            ))}
          </div>

          {onClose && (
            <button
              onClick={onClose}
              data-testid="close-followups-btn"
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
              aria-label="Dismiss followups panel"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="pt-4">
        {loading ? (
          <div className="py-8 text-center text-slate-400 text-sm">Loading follow-ups...</div>
        ) : followups.length === 0 ? (
          <div className="py-8 text-center flex flex-col items-center justify-center">
            <div className="w-10 h-10 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-2">
              <Clock className="w-5 h-5" />
            </div>
            <p className="text-sm font-medium text-slate-300">All caught up!</p>
            <p className="text-xs text-slate-500 mt-0.5">
              No applications have been waiting over {selectedDays} days without response.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {followups.map((app) => {
              const daysWaiting = getDaysWaiting(app.lastUpdatedAt || app.firstSeenAt);
              const isUrgent = daysWaiting >= 14;

              return (
                <div
                  key={app.id}
                  onClick={() => onSelectApplication(app)}
                  data-testid={`followup-card-${app.id}`}
                  className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80 hover:border-amber-500/40 hover:bg-slate-950 transition cursor-pointer flex items-center justify-between group"
                >
                  <div className="space-y-1 min-w-0 pr-2">
                    <div className="flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                      <span className="font-semibold text-sm text-white truncate">{app.company}</span>
                    </div>
                    <p className="text-xs text-slate-400 truncate">{app.role}</p>
                    <div className="flex items-center gap-2 pt-1">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold ${
                          isUrgent
                            ? 'bg-rose-500/10 text-rose-300 border border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
                        }`}
                      >
                        <Clock className="w-3 h-3" />
                        {daysWaiting} days waiting
                      </span>
                    </div>
                  </div>

                  <div className="p-1.5 rounded-lg bg-slate-800 text-slate-400 group-hover:text-amber-400 group-hover:bg-amber-500/10 transition">
                    <ChevronRight className="w-4 h-4" />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
