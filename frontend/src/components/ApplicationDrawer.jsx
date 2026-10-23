import { useState, useEffect } from 'react';
import {
  X,
  Building2,
  Briefcase,
  Calendar,
  Clock,
  Mail,
  Save,
  Check,
  RefreshCw,
  History,
  FileText,
} from 'lucide-react';
import { APPLICATION_STATUSES, STATUS_CONFIG, formatDate } from '../types/constants';

export default function ApplicationDrawer({
  application,
  isOpen,
  onClose,
  onUpdateStatus,
  onSaveNotes,
}) {
  const [notes, setNotes] = useState('');
  const [isSavingNotes, setIsSavingNotes] = useState(false);
  const [notesSaved, setNotesSaved] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState('');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  useEffect(() => {
    if (application) {
      setNotes(application.notes || '');
      setSelectedStatus(application.currentStatus || 'APPLIED');
      setNotesSaved(false);
    }
  }, [application]);

  if (!isOpen || !application) return null;

  const currentConfig = STATUS_CONFIG[selectedStatus] || STATUS_CONFIG.APPLIED;
  const CurrentIcon = currentConfig.icon;

  const handleStatusChange = async (e) => {
    const newStatus = e.target.value;
    setSelectedStatus(newStatus);
    setIsUpdatingStatus(true);
    try {
      await onUpdateStatus(application.id, newStatus);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleNotesSubmit = async (e) => {
    e.preventDefault();
    setIsSavingNotes(true);
    setNotesSaved(false);
    try {
      await onSaveNotes(application.id, notes);
      setNotesSaved(true);
      setTimeout(() => setNotesSaved(false), 2500);
    } finally {
      setIsSavingNotes(false);
    }
  };

  const history = application.statusHistory || [];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden" role="dialog" aria-modal="true">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-950/70 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        data-testid="drawer-backdrop"
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-lg bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col justify-between">
          {/* Header */}
          <div className="p-6 border-b border-slate-800">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs uppercase tracking-wider font-semibold text-sky-400">
                  Application Details
                </span>
                <h2 className="text-2xl font-bold text-white mt-1 flex items-center gap-2">
                  <Building2 className="w-6 h-6 text-slate-400" />
                  {application.company}
                </h2>
                <div className="flex items-center gap-2 text-slate-300 text-sm mt-1">
                  <Briefcase className="w-4 h-4 text-slate-500" />
                  <span>{application.role}</span>
                </div>
              </div>

              <button
                onClick={onClose}
                data-testid="drawer-close-btn"
                className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
                aria-label="Close drawer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Quick Status Bar */}
            <div className="mt-5 p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-400">Current Stage:</span>
                <div
                  className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold ${currentConfig.badgeBg} ${currentConfig.badgeText} border ${currentConfig.badgeBorder}`}
                >
                  <CurrentIcon className="w-3.5 h-3.5" />
                  <span>{currentConfig.label}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <select
                  id="status-select"
                  data-testid="status-select"
                  value={selectedStatus}
                  onChange={handleStatusChange}
                  disabled={isUpdatingStatus}
                  className="bg-slate-800 text-slate-200 text-xs font-medium rounded-lg px-2.5 py-1.5 border border-slate-700 focus:outline-none focus:border-sky-500"
                >
                  {APPLICATION_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      Move to {STATUS_CONFIG[status].label}
                    </option>
                  ))}
                </select>
                {isUpdatingStatus && <RefreshCw className="w-3.5 h-3.5 text-sky-400 animate-spin" />}
              </div>
            </div>
          </div>

          {/* Drawer Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {/* Metadata Info */}
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800/60">
                <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>First Discovered</span>
                </div>
                <p className="font-medium text-slate-200">{formatDate(application.firstSeenAt)}</p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800/60">
                <div className="flex items-center gap-1.5 text-slate-400 mb-1">
                  <Clock className="w-3.5 h-3.5" />
                  <span>Last Updated</span>
                </div>
                <p className="font-medium text-slate-200">{formatDate(application.lastUpdatedAt)}</p>
              </div>
            </div>

            {application.sourceEmailId && (
              <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-950/30 border border-slate-800/60 text-xs text-slate-400">
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                <span className="truncate">Source Email ID: {application.sourceEmailId}</span>
              </div>
            )}

            {/* Notes Section */}
            <form onSubmit={handleNotesSubmit} className="space-y-2">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="app-notes"
                  className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5"
                >
                  <FileText className="w-3.5 h-3.5 text-sky-400" />
                  Personal Notes
                </label>
                <span className="text-[11px] text-slate-500">{notes.length}/5000</span>
              </div>

              <textarea
                id="app-notes"
                data-testid="app-notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                maxLength={5000}
                rows={4}
                placeholder="Add salary expectations, interview notes, interviewers, or next steps..."
                className="w-full bg-slate-950/80 border border-slate-800 rounded-xl p-3 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
              />

              <div className="flex justify-end">
                <button
                  type="submit"
                  data-testid="save-notes-btn"
                  disabled={isSavingNotes}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-sky-600 hover:bg-sky-500 text-white font-medium text-xs transition active:scale-95 disabled:opacity-50"
                >
                  {isSavingNotes ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      Saving...
                    </>
                  ) : notesSaved ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-300" />
                      Saved!
                    </>
                  ) : (
                    <>
                      <Save className="w-3.5 h-3.5" />
                      Save Notes
                    </>
                  )}
                </button>
              </div>
            </form>

            {/* Status History Timeline */}
            <div>
              <div className="flex items-center gap-2 mb-4">
                <History className="w-4 h-4 text-purple-400" />
                <h3 className="text-sm font-semibold text-slate-200 uppercase tracking-wider">
                  Status History Timeline
                </h3>
              </div>

              {history.length === 0 ? (
                <p className="text-xs text-slate-500 italic">No previous transitions recorded.</p>
              ) : (
                <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-800">
                  {history.map((item, index) => {
                    const cfg = STATUS_CONFIG[item.status] || STATUS_CONFIG.APPLIED;
                    const Icon = cfg.icon;
                    return (
                      <div key={item.id || index} className="relative group">
                        {/* Node circle */}
                        <div
                          className={`absolute -left-[23px] top-0.5 w-4 h-4 rounded-full border-2 border-slate-900 ${cfg.dotColor} shadow-md`}
                        />

                        <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-3 space-y-1">
                          <div className="flex items-center justify-between">
                            <span
                              className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold ${cfg.badgeBg} ${cfg.badgeText} border ${cfg.badgeBorder}`}
                            >
                              <Icon className="w-3 h-3" />
                              {cfg.label}
                            </span>
                            <span className="text-[11px] text-slate-500">
                              {formatDate(item.changedAt)}
                            </span>
                          </div>
                          {item.sourceEmailId ? (
                            <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-1">
                              <Mail className="w-3 h-3 text-slate-500" />
                              <span>Detected from email</span>
                            </p>
                          ) : (
                            <p className="text-[11px] text-slate-500 italic mt-1">Manual transition</p>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-slate-800 bg-slate-950/40 flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium transition"
            >
              Done
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
