import { useState, useMemo } from 'react';
import { Search, Filter, ArrowUpDown, Building2, Briefcase, ChevronRight, FileText } from 'lucide-react';
import { APPLICATION_STATUSES, STATUS_CONFIG, formatDate, formatRelativeTime } from '../types/constants';

export default function TableView({ applications = [], onSelectApplication }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('lastUpdated-desc');

  const filteredApplications = useMemo(() => {
    return applications
      .filter((app) => {
        // Status filter
        if (statusFilter !== 'ALL' && app.currentStatus !== statusFilter) {
          return false;
        }

        // Search term filter
        if (searchTerm.trim()) {
          const term = searchTerm.toLowerCase();
          const matchesCompany = app.company?.toLowerCase().includes(term);
          const matchesRole = app.role?.toLowerCase().includes(term);
          const matchesNotes = app.notes?.toLowerCase().includes(term);
          return matchesCompany || matchesRole || matchesNotes;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'lastUpdated-desc') {
          return new Date(b.lastUpdatedAt || 0) - new Date(a.lastUpdatedAt || 0);
        }
        if (sortBy === 'lastUpdated-asc') {
          return new Date(a.lastUpdatedAt || 0) - new Date(b.lastUpdatedAt || 0);
        }
        if (sortBy === 'company-asc') {
          return (a.company || '').localeCompare(b.company || '');
        }
        if (sortBy === 'company-desc') {
          return (b.company || '').localeCompare(a.company || '');
        }
        return 0;
      });
  }, [applications, searchTerm, statusFilter, sortBy]);

  return (
    <div
      data-testid="table-view"
      className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden shadow-xl"
    >
      {/* Controls Bar */}
      <div className="p-4 border-b border-slate-800 flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
          <input
            id="table-search-input"
            data-testid="table-search-input"
            type="text"
            placeholder="Search company, role, or keywords..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition"
          />
        </div>

        {/* Filters */}
        <div className="flex items-center gap-3">
          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-500" />
            <select
              id="table-status-filter"
              data-testid="table-status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs font-medium text-slate-300 focus:outline-none focus:border-sky-500 transition"
            >
              <option value="ALL">All Statuses ({applications.length})</option>
              {APPLICATION_STATUSES.map((status) => {
                const count = applications.filter((a) => a.currentStatus === status).length;
                return (
                  <option key={status} value={status}>
                    {STATUS_CONFIG[status].label} ({count})
                  </option>
                );
              })}
            </select>
          </div>

          {/* Sort By */}
          <div className="flex items-center gap-2">
            <ArrowUpDown className="w-4 h-4 text-slate-500" />
            <select
              id="table-sort-by"
              data-testid="table-sort-by"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs font-medium text-slate-300 focus:outline-none focus:border-sky-500 transition"
            >
              <option value="lastUpdated-desc">Updated (Newest first)</option>
              <option value="lastUpdated-asc">Updated (Oldest first)</option>
              <option value="company-asc">Company (A-Z)</option>
              <option value="company-desc">Company (Z-A)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-800 bg-slate-950/40 text-slate-400 text-xs uppercase tracking-wider font-semibold">
              <th className="py-3 px-4">Company & Role</th>
              <th className="py-3 px-4">Current Status</th>
              <th className="py-3 px-4">Notes</th>
              <th className="py-3 px-4">Discovered</th>
              <th className="py-3 px-4">Last Updated</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {filteredApplications.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-12 text-center text-slate-500 text-sm">
                  No applications match your search criteria.
                </td>
              </tr>
            ) : (
              filteredApplications.map((app) => {
                const cfg = STATUS_CONFIG[app.currentStatus] || STATUS_CONFIG.APPLIED;
                const Icon = cfg.icon;

                return (
                  <tr
                    key={app.id}
                    data-testid={`table-row-${app.id}`}
                    onClick={() => onSelectApplication(app)}
                    className="hover:bg-slate-800/40 cursor-pointer transition"
                  >
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2.5">
                        <div className="p-2 rounded-lg bg-slate-800 border border-slate-700/60 text-slate-400">
                          <Building2 className="w-4 h-4" />
                        </div>
                        <div>
                          <div className="font-bold text-white hover:text-sky-400 transition">
                            {app.company}
                          </div>
                          <div className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                            <Briefcase className="w-3 h-3 text-slate-500" />
                            <span>{app.role}</span>
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="py-4 px-4 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold ${cfg.badgeBg} ${cfg.badgeText} border ${cfg.badgeBorder}`}
                      >
                        <Icon className="w-3.5 h-3.5" />
                        {cfg.label}
                      </span>
                    </td>

                    <td className="py-4 px-4 max-w-xs">
                      {app.notes ? (
                        <p className="text-xs text-slate-400 truncate flex items-center gap-1">
                          <FileText className="w-3 h-3 text-slate-500 flex-shrink-0" />
                          <span>{app.notes}</span>
                        </p>
                      ) : (
                        <span className="text-xs text-slate-600 italic">No notes</span>
                      )}
                    </td>

                    <td className="py-4 px-4 whitespace-nowrap text-xs text-slate-400">
                      {formatDate(app.firstSeenAt)}
                    </td>

                    <td className="py-4 px-4 whitespace-nowrap text-xs text-slate-300">
                      {formatRelativeTime(app.lastUpdatedAt || app.firstSeenAt)}
                    </td>

                    <td className="py-4 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectApplication(app);
                        }}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium transition"
                      >
                        <span>Details</span>
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
