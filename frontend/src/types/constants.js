import { Inbox, FileCode, Users, Award, XCircle } from 'lucide-react';

export const APPLICATION_STATUSES = [
  'APPLIED',
  'ASSESSMENT',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
];

export const STATUS_CONFIG = {
  APPLIED: {
    label: 'Applied',
    icon: Inbox,
    badgeBg: 'bg-blue-500/10',
    badgeText: 'text-blue-400',
    badgeBorder: 'border-blue-500/20',
    columnBorder: 'border-blue-500/30',
    columnHeaderBg: 'bg-blue-500/10',
    columnAccent: 'from-blue-500/20 to-transparent',
    dotColor: 'bg-blue-400',
  },
  ASSESSMENT: {
    label: 'Assessment',
    icon: FileCode,
    badgeBg: 'bg-purple-500/10',
    badgeText: 'text-purple-400',
    badgeBorder: 'border-purple-500/20',
    columnBorder: 'border-purple-500/30',
    columnHeaderBg: 'bg-purple-500/10',
    columnAccent: 'from-purple-500/20 to-transparent',
    dotColor: 'bg-purple-400',
  },
  INTERVIEW: {
    label: 'Interview',
    icon: Users,
    badgeBg: 'bg-amber-500/10',
    badgeText: 'text-amber-400',
    badgeBorder: 'border-amber-500/20',
    columnBorder: 'border-amber-500/30',
    columnHeaderBg: 'bg-amber-500/10',
    columnAccent: 'from-amber-500/20 to-transparent',
    dotColor: 'bg-amber-400',
  },
  OFFER: {
    label: 'Offer',
    icon: Award,
    badgeBg: 'bg-emerald-500/10',
    badgeText: 'text-emerald-400',
    badgeBorder: 'border-emerald-500/20',
    columnBorder: 'border-emerald-500/30',
    columnHeaderBg: 'bg-emerald-500/10',
    columnAccent: 'from-emerald-500/20 to-transparent',
    dotColor: 'bg-emerald-400',
  },
  REJECTED: {
    label: 'Rejected',
    icon: XCircle,
    badgeBg: 'bg-rose-500/10',
    badgeText: 'text-rose-400',
    badgeBorder: 'border-rose-500/20',
    columnBorder: 'border-rose-500/30',
    columnHeaderBg: 'bg-rose-500/10',
    columnAccent: 'from-rose-500/20 to-transparent',
    dotColor: 'bg-rose-400',
  },
};

export function formatRelativeTime(dateString) {
  if (!dateString) return 'Unknown date';
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now - date) / 1000);

  if (diffInSeconds < 60) return 'Just now';
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) return `${diffInHours}h ago`;
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 30) return `${diffInDays}d ago`;
  const diffInMonths = Math.floor(diffInDays / 30);
  if (diffInMonths < 12) return `${diffInMonths}mo ago`;
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export function formatDate(dateString) {
  if (!dateString) return '—';
  const date = new Date(dateString);
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
