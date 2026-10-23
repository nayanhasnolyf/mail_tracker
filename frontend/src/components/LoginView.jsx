import { Mail, Sparkles, Kanban, Clock, ShieldCheck, ArrowRight, Zap } from 'lucide-react';

export default function LoginView({ onStartDemo, error }) {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between selection:bg-sky-500/30 selection:text-sky-200">
      {/* Top Bar */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Mail className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
              Application Tracker
            </span>
          </div>

          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              API v1.0
            </span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl mx-auto px-6 py-16 flex flex-col items-center justify-center">
        {error && (
          <div
            role="alert"
            className="mb-8 max-w-md w-full p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-3"
          >
            <ShieldCheck className="w-5 h-5 text-rose-400 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="text-center max-w-3xl mx-auto mb-12">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs font-semibold mb-6 shadow-sm">
            <Sparkles className="w-4 h-4 text-sky-400" />
            AI & Rule-Driven Job Application Tracker
          </div>
          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white mb-6 leading-tight">
            Stop losing track of your{' '}
            <span className="bg-gradient-to-r from-sky-400 via-cyan-300 to-indigo-400 bg-clip-text text-transparent">
              Job Applications
            </span>
          </h1>
          <p className="text-lg text-slate-400 leading-relaxed max-w-2xl mx-auto">
            Connects securely with your Gmail to detect confirmations, assessments, and interview invites. Organizes your pipeline into an interactive Kanban board automatically.
          </p>
        </div>

        {/* Action Card */}
        <div className="w-full max-w-md p-8 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-2xl backdrop-blur-xl relative group">
          <div className="absolute -inset-0.5 bg-gradient-to-r from-sky-500 to-indigo-500 rounded-2xl blur opacity-20 group-hover:opacity-30 transition duration-500 -z-10" />

          <div className="text-center mb-6">
            <h2 className="text-xl font-bold text-white">Get Started</h2>
            <p className="text-sm text-slate-400 mt-1">Sign in with your Google account to sync emails</p>
          </div>

          <div className="space-y-4">
            <a
              href="/oauth2/authorization/google"
              id="google-signin-btn"
              data-testid="google-signin-btn"
              className="w-full flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl bg-white hover:bg-slate-100 text-slate-900 font-semibold text-sm transition-all duration-200 shadow-md hover:shadow-lg active:scale-[0.99]"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              <span>Sign in with Google</span>
            </a>

            <div className="relative flex py-2 items-center">
              <div className="flex-grow border-t border-slate-800" />
              <span className="flex-shrink mx-4 text-xs uppercase tracking-wider text-slate-500 font-medium">Or</span>
              <div className="flex-grow border-t border-slate-800" />
            </div>

            <button
              onClick={onStartDemo}
              id="demo-mode-btn"
              data-testid="demo-mode-btn"
              className="w-full flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-slate-800/90 hover:bg-slate-800 text-sky-400 hover:text-sky-300 font-medium text-sm border border-slate-700/60 transition-all duration-200 active:scale-[0.99]"
            >
              <Zap className="w-4 h-4" />
              <span>Explore Interactive Demo</span>
              <ArrowRight className="w-4 h-4 ml-1" />
            </button>
          </div>

          <p className="text-xs text-center text-slate-500 mt-6 leading-relaxed">
            Requests read-only access to Gmail (<code className="text-slate-400">gmail.readonly</code>) to parse job application emails. Your emails are never stored or shared.
          </p>
        </div>

        {/* Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl w-full mt-20">
          <div className="p-6 rounded-xl bg-slate-900/50 border border-slate-800/80 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-lg bg-sky-500/10 flex items-center justify-center text-sky-400 mb-4">
              <Kanban className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-white mb-2">Kanban Board</h3>
            <p className="text-sm text-slate-400">
              Drag-and-drop workflow moving applications across Applied, Assessment, Interview, Offer, and Rejected stages with instant backend sync.
            </p>
          </div>

          <div className="p-6 rounded-xl bg-slate-900/50 border border-slate-800/80 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-lg bg-indigo-500/10 flex items-center justify-center text-indigo-400 mb-4">
              <Clock className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-white mb-2">Follow-up Alerts</h3>
            <p className="text-sm text-slate-400">
              Never let an application go cold. Get alerts for applications pending a response past customizable 7, 14, or 30-day thresholds.
            </p>
          </div>

          <div className="p-6 rounded-xl bg-slate-900/50 border border-slate-800/80 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400 mb-4">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-white mb-2">Automated & Private</h3>
            <p className="text-sm text-slate-400">
              OAuth2 with offline refresh tokens and encrypted storage. Only extracts relevant metadata like company name, role, and status.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/60 py-6 text-center text-xs text-slate-500">
        Job Application Tracker • Monorepo (Spring Boot 3 + React + Tailwind)
      </footer>
    </div>
  );
}
