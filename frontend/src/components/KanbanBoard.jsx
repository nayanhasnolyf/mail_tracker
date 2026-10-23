import { useState } from 'react';
import {
  APPLICATION_STATUSES,
  STATUS_CONFIG,
  formatRelativeTime,
} from '../types/constants';
import { Building2, Briefcase, Clock, FileText, MoveRight } from 'lucide-react';

export default function KanbanBoard({
  applications = [],
  onStatusChange,
  onSelectApplication,
}) {
  const [draggedAppId, setDraggedAppId] = useState(null);
  const [dragOverColumn, setDragOverColumn] = useState(null);

  // Group applications by current status
  const columns = APPLICATION_STATUSES.map((status) => {
    return {
      status,
      config: STATUS_CONFIG[status],
      items: applications.filter((app) => app.currentStatus === status),
    };
  });

  const handleDragStart = (e, appId) => {
    e.dataTransfer.setData('text/plain', appId);
    e.dataTransfer.effectAllowed = 'move';
    setDraggedAppId(appId);
  };

  const handleDragEnd = () => {
    setDraggedAppId(null);
    setDragOverColumn(null);
  };

  const handleDragOver = (e, status) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (dragOverColumn !== status) {
      setDragOverColumn(status);
    }
  };

  const handleDragLeave = (e, status) => {
    if (dragOverColumn === status) {
      setDragOverColumn(null);
    }
  };

  const handleDrop = (e, newStatus) => {
    e.preventDefault();
    setDragOverColumn(null);
    const appId = e.dataTransfer.getData('text/plain') || draggedAppId;
    if (appId) {
      const app = applications.find((a) => a.id === appId);
      if (app && app.currentStatus !== newStatus) {
        onStatusChange(appId, newStatus);
      }
    }
    setDraggedAppId(null);
  };

  return (
    <div
      data-testid="kanban-board"
      className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 items-start pb-8"
    >
      {columns.map(({ status, config, items }) => {
        const Icon = config.icon;
        const isDropTarget = dragOverColumn === status;

        return (
          <div
            key={status}
            data-testid={`kanban-column-${status}`}
            onDragOver={(e) => handleDragOver(e, status)}
            onDragLeave={(e) => handleDragLeave(e, status)}
            onDrop={(e) => handleDrop(e, status)}
            className={`flex flex-col rounded-2xl bg-slate-900/50 border transition-all duration-200 min-h-[480px] ${
              isDropTarget
                ? 'border-sky-500/80 bg-slate-900/90 ring-2 ring-sky-500/30 shadow-xl'
                : 'border-slate-800/80'
            }`}
          >
            {/* Column Header */}
            <div
              className={`p-4 border-b border-slate-800 flex items-center justify-between rounded-t-2xl bg-gradient-to-b ${config.columnAccent}`}
            >
              <div className="flex items-center gap-2">
                <div className={`p-1.5 rounded-lg ${config.badgeBg} ${config.badgeText}`}>
                  <Icon className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white tracking-wide">{config.label}</h3>
              </div>
              <span
                data-testid={`column-count-${status}`}
                className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-300 border border-slate-700/60"
              >
                {items.length}
              </span>
            </div>

            {/* Droppable Card Container */}
            <div className="flex-1 p-3 space-y-3 overflow-y-auto max-h-[70vh]">
              {items.length === 0 ? (
                <div className="h-32 flex flex-col items-center justify-center text-slate-600 border border-dashed border-slate-800/80 rounded-xl text-xs">
                  <span>No applications</span>
                  <span className="text-[10px] mt-0.5">Drag cards here</span>
                </div>
              ) : (
                items.map((app) => {
                  const isBeingDragged = draggedAppId === app.id;

                  return (
                    <div
                      key={app.id}
                      draggable
                      data-testid={`application-card-${app.id}`}
                      onDragStart={(e) => handleDragStart(e, app.id)}
                      onDragEnd={handleDragEnd}
                      onClick={() => onSelectApplication(app)}
                      className={`p-4 rounded-xl bg-slate-950/80 border border-slate-800/90 hover:border-slate-700 shadow-md cursor-grab active:cursor-grabbing transition duration-150 group hover:-translate-y-0.5 ${
                        isBeingDragged ? 'opacity-40 scale-95 border-dashed border-sky-400' : ''
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-2">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <Building2 className="w-4 h-4 text-slate-400 flex-shrink-0" />
                          <h4 className="font-bold text-sm text-white truncate group-hover:text-sky-300 transition">
                            {app.company}
                          </h4>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 text-xs text-slate-300 mb-3">
                        <Briefcase className="w-3.5 h-3.5 text-slate-500 flex-shrink-0" />
                        <span className="truncate">{app.role}</span>
                      </div>

                      {app.notes && (
                        <p className="text-[11px] text-slate-400 line-clamp-2 bg-slate-900/60 p-2 rounded-lg border border-slate-800/60 mb-3 flex items-start gap-1.5">
                          <FileText className="w-3 h-3 text-slate-500 mt-0.5 flex-shrink-0" />
                          <span>{app.notes}</span>
                        </p>
                      )}

                      <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-500">
                        <div className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          <span>{formatRelativeTime(app.lastUpdatedAt || app.firstSeenAt)}</span>
                        </div>

                        <span className="text-[10px] text-slate-500 group-hover:text-sky-400 font-medium transition flex items-center gap-0.5">
                          Details
                          <MoveRight className="w-2.5 h-2.5 opacity-0 group-hover:opacity-100 transition" />
                        </span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
