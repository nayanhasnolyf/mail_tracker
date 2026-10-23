import { useState, useEffect, useCallback } from 'react';
import {
  api,
  DEMO_USER,
  INITIAL_DEMO_APPLICATIONS,
} from './api/client';
import LoginView from './components/LoginView';
import Header from './components/Header';
import StatsBar from './components/StatsBar';
import KanbanBoard from './components/KanbanBoard';
import TableView from './components/TableView';
import ApplicationDrawer from './components/ApplicationDrawer';
import FollowupsList from './components/FollowupsList';
import NotificationToast from './components/NotificationToast';
import { RefreshCw, AlertCircle } from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(null);
  const [isDemo, setIsDemo] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authError, setAuthError] = useState(null);

  // Dashboard state
  const [viewMode, setViewMode] = useState('kanban'); // 'kanban' | 'table'
  const [applications, setApplications] = useState([]);
  const [stats, setStats] = useState(null);
  const [followups, setFollowups] = useState([]);
  const [followupDays, setFollowupDays] = useState(7);
  const [showFollowups, setShowFollowups] = useState(false);
  const [selectedApplication, setSelectedApplication] = useState(null);
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  // Compute stats locally if backend stats endpoint is empty/in demo
  const computeStats = useCallback((apps) => {
    const total = apps.length;
    const active = apps.filter((a) =>
      ['APPLIED', 'ASSESSMENT', 'INTERVIEW'].includes(a.currentStatus)
    ).length;
    const movedBeyond = apps.filter((a) =>
      ['ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED'].includes(a.currentStatus)
    ).length;
    const responseRate = total > 0 ? (movedBeyond / total) * 100 : 0;

    const countsByStatus = {};
    apps.forEach((a) => {
      countsByStatus[a.currentStatus] = (countsByStatus[a.currentStatus] || 0) + 1;
    });

    return {
      totalApplications: total,
      activeApplications: active,
      responseRate,
      countsByStatus,
      applicationsPerWeek: {},
    };
  }, []);

  // Compute followups locally for demo mode
  const computeDemoFollowups = useCallback((apps, days) => {
    const cutoff = Date.now() - days * 86400000;
    return apps.filter((a) => {
      if (a.currentStatus !== 'APPLIED') return false;
      const lastTime = new Date(a.lastUpdatedAt || a.firstSeenAt).getTime();
      return lastTime <= cutoff;
    });
  }, []);

  // Load dashboard data
  const loadDashboardData = useCallback(async (isDemoMode = false, days = followupDays) => {
    if (isDemoMode) {
      setApplications((currentApps) => {
        const apps = currentApps.length > 0 ? currentApps : INITIAL_DEMO_APPLICATIONS;
        setStats(computeStats(apps));
        setFollowups(computeDemoFollowups(apps, days));
        return apps;
      });
      return;
    }

    try {
      setRefreshing(true);
      const [appsResponse, statsResponse, followupsResponse] = await Promise.allSettled([
        api.getApplications({ size: 100 }),
        api.getStats(),
        api.getFollowups(days),
      ]);

      if (appsResponse.status === 'fulfilled') {
        const items = appsResponse.value.items || appsResponse.value || [];
        setApplications(items);

        if (statsResponse.status === 'fulfilled') {
          setStats(statsResponse.value);
        } else {
          setStats(computeStats(items));
        }
      }

      if (followupsResponse.status === 'fulfilled') {
        setFollowups(followupsResponse.value || []);
      }
    } catch (err) {
      showToast(err.message || 'Error updating dashboard data', 'error');
    } finally {
      setRefreshing(false);
    }
  }, [computeDemoFollowups, computeStats, followupDays]);

  // Initial authentication check
  useEffect(() => {
    const initAuth = async () => {
      setLoading(true);
      try {
        const currentUser = await api.getCurrentUser();
        setUser(currentUser);
        setIsDemo(false);
        await loadDashboardData(false);
      } catch (err) {
        // Not authenticated with session
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, [loadDashboardData]);

  // Start demo mode
  const handleStartDemo = () => {
    setUser(DEMO_USER);
    setIsDemo(true);
    setApplications(INITIAL_DEMO_APPLICATIONS);
    setStats(computeStats(INITIAL_DEMO_APPLICATIONS));
    setFollowups(computeDemoFollowups(INITIAL_DEMO_APPLICATIONS, followupDays));
    showToast('Loaded interactive demo dashboard', 'info');
  };

  // Sign out or exit demo
  const handleSignOut = () => {
    setUser(null);
    setIsDemo(false);
    setApplications([]);
    setSelectedApplication(null);
    setShowFollowups(false);
    if (!isDemo) {
      window.location.href = '/logout';
    }
  };

  // Change followups threshold days
  const handleFollowupDaysChange = async (days) => {
    setFollowupDays(days);
    if (isDemo) {
      setFollowups(computeDemoFollowups(applications, days));
    } else {
      try {
        const result = await api.getFollowups(days);
        setFollowups(result || []);
      } catch (err) {
        showToast('Failed to fetch follow-ups', 'error');
      }
    }
  };

  // Drag and drop or manual status update with optimistic update
  const handleStatusChange = async (applicationId, newStatus) => {
    const originalApp = applications.find((a) => a.id === applicationId);
    if (!originalApp || originalApp.currentStatus === newStatus) return;

    const previousStatus = originalApp.currentStatus;

    // Optimistically update
    const updatedApps = applications.map((a) => {
      if (a.id === applicationId) {
        const newHistoryItem = {
          id: `h-local-${Date.now()}`,
          status: newStatus,
          changedAt: new Date().toISOString(),
          sourceEmailId: null,
        };
        return {
          ...a,
          currentStatus: newStatus,
          lastUpdatedAt: new Date().toISOString(),
          statusHistory: [newHistoryItem, ...(a.statusHistory || [])],
        };
      }
      return a;
    });

    setApplications(updatedApps);
    setStats(computeStats(updatedApps));
    setFollowups(computeDemoFollowups(updatedApps, followupDays));

    if (selectedApplication && selectedApplication.id === applicationId) {
      setSelectedApplication({
        ...selectedApplication,
        currentStatus: newStatus,
        lastUpdatedAt: new Date().toISOString(),
        statusHistory: [
          {
            id: `h-local-${Date.now()}`,
            status: newStatus,
            changedAt: new Date().toISOString(),
            sourceEmailId: null,
          },
          ...(selectedApplication.statusHistory || []),
        ],
      });
    }

    if (isDemo) {
      showToast(`Moved ${originalApp.company} to ${newStatus}`);
      return;
    }

    try {
      const saved = await api.updateApplication(applicationId, { status: newStatus });
      showToast(`Moved ${originalApp.company} to ${newStatus}`);
      if (saved) {
        setApplications((prev) => prev.map((a) => (a.id === applicationId ? { ...a, ...saved } : a)));
        if (selectedApplication?.id === applicationId) {
          setSelectedApplication((prev) => ({ ...prev, ...saved }));
        }
      }
    } catch (err) {
      // Revert optimistic update
      setApplications((prev) =>
        prev.map((a) =>
          a.id === applicationId
            ? { ...a, currentStatus: previousStatus, lastUpdatedAt: originalApp.lastUpdatedAt }
            : a
        )
      );
      setStats(computeStats(applications));
      showToast(`Failed to update status: ${err.message}`, 'error');
    }
  };

  // Update notes
  const handleSaveNotes = async (applicationId, notes) => {
    const targetApp = applications.find((a) => a.id === applicationId);
    if (!targetApp) return;

    if (isDemo) {
      setApplications((prev) =>
        prev.map((a) => (a.id === applicationId ? { ...a, notes } : a))
      );
      if (selectedApplication?.id === applicationId) {
        setSelectedApplication((prev) => ({ ...prev, notes }));
      }
      showToast('Notes saved successfully');
      return;
    }

    try {
      const saved = await api.updateApplication(applicationId, { notes });
      setApplications((prev) =>
        prev.map((a) => (a.id === applicationId ? { ...a, notes, ...saved } : a))
      );
      if (selectedApplication?.id === applicationId) {
        setSelectedApplication((prev) => ({ ...prev, notes, ...saved }));
      }
      showToast('Notes saved successfully');
    } catch (err) {
      showToast(`Failed to save notes: ${err.message}`, 'error');
      throw err;
    }
  };

  // Select application to open drawer
  const handleSelectApplication = async (app) => {
    setSelectedApplication(app);

    // Fetch full application detail with history if not demo
    if (!isDemo && app?.id) {
      try {
        const fullDetail = await api.getApplicationById(app.id);
        setSelectedApplication(fullDetail);
      } catch (err) {
        // Fall back to current data in state
      }
    }
  };

  // Initial loading spinner
  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-400">
        <RefreshCw className="w-8 h-8 text-sky-400 animate-spin mb-4" />
        <p className="text-sm font-medium">Loading Application Tracker...</p>
      </div>
    );
  }

  // Login view if not logged in
  if (!user) {
    return <LoginView onStartDemo={handleStartDemo} error={authError} />;
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-sky-500/30 selection:text-sky-200">
      {/* Header */}
      <Header
        user={user}
        isDemo={isDemo}
        viewMode={viewMode}
        onViewChange={setViewMode}
        followupCount={followups.length}
        showFollowups={showFollowups}
        onToggleFollowups={() => setShowFollowups(!showFollowups)}
        onRefresh={() => loadDashboardData(isDemo)}
        refreshing={refreshing}
        onSignOut={handleSignOut}
      />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6">
        {/* Stats Overview */}
        <StatsBar stats={stats} />

        {/* Needs Follow-up Panel */}
        {showFollowups && (
          <FollowupsList
            followups={followups}
            daysThreshold={followupDays}
            onChangeDays={handleFollowupDaysChange}
            onSelectApplication={handleSelectApplication}
            onClose={() => setShowFollowups(false)}
            loading={refreshing}
          />
        )}

        {/* View content: Kanban vs Table */}
        {viewMode === 'kanban' ? (
          <KanbanBoard
            applications={applications}
            onStatusChange={handleStatusChange}
            onSelectApplication={handleSelectApplication}
          />
        ) : (
          <TableView
            applications={applications}
            onSelectApplication={handleSelectApplication}
          />
        )}
      </main>

      {/* Application Detail Drawer */}
      <ApplicationDrawer
        application={selectedApplication}
        isOpen={Boolean(selectedApplication)}
        onClose={() => setSelectedApplication(null)}
        onUpdateStatus={handleStatusChange}
        onSaveNotes={handleSaveNotes}
      />

      {/* Notifications Toast */}
      <NotificationToast toast={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
