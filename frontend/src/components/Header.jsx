import {
  Mail,
  Kanban,
  Table as TableIcon,
  BarChart3,
  Bell,
  LogOut,
  RefreshCw,
  AlertTriangle,
  Zap,
} from 'lucide-react';

export default function Header({
  user,
  isDemo,
  viewMode,
  onViewChange,
  followupCount = 0,
  showFollowups,
  onToggleFollowups,
  onRefresh,
  refreshing,
  onSignOut,
}) {
  return (
    <header className="border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-md sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
            <Mail className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="font-bold text-base sm:text-lg tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
              Application Tracker
            </span>
          </div>
          {isDemo && (
            <span
              data-testid="demo-badge"
              className="hidden sm:inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/30"
            >
              <Zap className="w-3 h-3" />
              Demo Mode
            </span>
          )}
        </div>

        {/* View Switcher */}
        <div className="flex items-center bg-slate-950/80 border border-slate-800 p-1 rounded-xl">
          <button
            id="view-kanban-btn"
            data-testid="view-kanban-btn"
            onClick={() => onViewChange('kanban')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              viewMode === 'kanban'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Kanban className="w-3.5 h-3.5" />
            <span>Kanban</span>
          </button>
          <button
            id="view-table-btn"
            data-testid="view-table-btn"
            onClick={() => onViewChange('table')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              viewMode === 'table'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <TableIcon className="w-3.5 h-3.5" />
            <span>Table</span>
          </button>
          <button
            id="view-stats-btn"
            data-testid="view-stats-btn"
            onClick={() => onViewChange('stats')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              viewMode === 'stats'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <BarChart3 className="w-3.5 h-3.5" />
            <span>Analytics</span>
          </button>
        </div>

        {/* Right Controls */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Follow-up button */}
          <button
            id="toggle-followups-btn"
            data-testid="toggle-followups-btn"
            onClick={onToggleFollowups}
            className={`relative flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
              showFollowups
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                : 'bg-slate-800/80 text-slate-300 border-slate-700/60 hover:bg-slate-800 hover:text-white'
            }`}
            title="Needs Follow-up Applications"
          >
            <Bell className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden md:inline">Follow-ups</span>
            {followupCount > 0 && (
              <span
                data-testid="followup-badge"
                className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-amber-500 text-slate-950 ml-0.5"
              >
                {followupCount}
              </span>
            )}
          </button>

          {/* Refresh button */}
          <button
            onClick={onRefresh}
            disabled={refreshing}
            className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-700/60 transition disabled:opacity-50"
            title="Refresh dashboard data"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin text-sky-400' : ''}`} />
          </button>

          {/* Reauth warning banner if needed */}
          {user?.reauthRequired && (
            <div
              className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs"
              title="Google refresh token expired. Re-authentication required."
            >
              <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
              <span className="hidden lg:inline font-medium">Reauth Required</span>
            </div>
          )}

          {/* User profile & sign out */}
          <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
            <div className="hidden sm:flex flex-col text-right">
              <span className="text-xs font-medium text-slate-200 truncate max-w-[140px]">
                {user?.email || 'User'}
              </span>
              <span className="text-[10px] text-slate-400">
                {isDemo ? 'Demo Session' : 'Google Connected'}
              </span>
            </div>

            <button
              onClick={onSignOut}
              className="p-2 rounded-lg bg-slate-800/80 hover:bg-rose-500/20 text-slate-400 hover:text-rose-300 border border-slate-700/60 transition"
              title={isDemo ? 'Exit Demo' : 'Sign Out'}
              aria-label="Sign Out"
            >
              <LogOut className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
