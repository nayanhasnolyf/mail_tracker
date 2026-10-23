import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export default function NotificationToast({ toast, onDismiss }) {
  if (!toast) return null;

  const typeConfig = {
    success: {
      icon: CheckCircle2,
      bgColor: 'bg-emerald-950/90 border-emerald-500/40 text-emerald-200',
      iconColor: 'text-emerald-400',
    },
    error: {
      icon: AlertCircle,
      bgColor: 'bg-rose-950/90 border-rose-500/40 text-rose-200',
      iconColor: 'text-rose-400',
    },
    info: {
      icon: Info,
      bgColor: 'bg-sky-950/90 border-sky-500/40 text-sky-200',
      iconColor: 'text-sky-400',
    },
  }[toast.type || 'info'];

  const Icon = typeConfig.icon;

  return (
    <div
      role="alert"
      className="fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl border backdrop-blur-md shadow-2xl transition-all duration-300 animate-slide-up"
    >
      <div className={`flex items-center gap-3 ${typeConfig.bgColor} px-4 py-2.5 rounded-lg border`}>
        <Icon className={`w-5 h-5 flex-shrink-0 ${typeConfig.iconColor}`} />
        <p className="text-sm font-medium pr-2">{toast.message}</p>
        <button
          onClick={onDismiss}
          className="text-slate-400 hover:text-white p-1 rounded-md transition-colors"
          aria-label="Dismiss notification"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
