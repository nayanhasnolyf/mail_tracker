import { useState, useEffect } from 'react';
import { Mail, CheckCircle2, AlertCircle, RefreshCw, Layers, ShieldCheck, Database, Inbox, Sparkles, ArrowRight } from 'lucide-react';

export default function App() {
  const [healthStatus, setHealthStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const checkHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/health');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const data = await response.json();
      setHealthStatus(data);
    } catch (err) {
      setError(err.message || 'Unable to connect to backend service');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkHealth();
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-sky-500/30 selection:text-sky-200">
      {/* Top Navbar */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-cyan-400 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Mail className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
                Application Tracker
              </span>
              <span className="hidden sm:inline-block ml-2 text-xs uppercase px-2 py-0.5 rounded-full bg-sky-950 text-sky-400 border border-sky-800/50 font-medium">
                Monorepo
              </span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 text-xs">
              <span className="text-slate-400">Backend API:</span>
              {loading ? (
                <span className="flex items-center gap-1.5 text-yellow-400">
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  Connecting...
                </span>
              ) : error ? (
                <span className="flex items-center gap-1.5 text-rose-400 font-medium">
                  <AlertCircle className="w-3.5 h-3.5" />
                  Offline
                </span>
              ) : healthStatus ? (
                <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  {healthStatus.status}
                </span>
              ) : (
                <span className="text-slate-500">Idle</span>
              )}
            </div>

            <button
              id="refresh-health-btn"
              onClick={checkHealth}
              disabled={loading}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 active:scale-95 text-slate-300 hover:text-white transition-all disabled:opacity-50"
              title="Ping GET /api/health"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1 max-w-7xl mx-auto px-6 py-12 flex flex-col justify-center">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs font-medium mb-6">
            <Sparkles className="w-3.5 h-3.5 text-sky-400" />
            AI & Rule-Driven Job Application Assistant
          </div>
          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white mb-6 leading-tight">
            Automate your job search directly from your{' '}
            <span className="bg-gradient-to-r from-sky-400 via-cyan-300 to-indigo-400 bg-clip-text text-transparent">
              Gmail Inbox
            </span>
          </h1>
          <p className="text-lg text-slate-400 leading-relaxed max-w-2xl mx-auto">
            Reads job confirmation and update emails, automatically classifies application statuses, and visualizes your job hunt in an intuitive dashboard.
          </p>
        </div>

        {/* Live Service Status Card */}
        <div className="max-w-2xl mx-auto w-full mb-16">
          <div className="relative group">
            <div className="absolute -inset-0.5 bg-gradient-to-r from-sky-500 to-cyan-500 rounded-2xl blur opacity-30 group-hover:opacity-50 transition duration-500"></div>
            <div className="relative rounded-2xl bg-slate-900/90 border border-slate-800 p-6 shadow-2xl backdrop-blur-xl">
              <div className="flex items-center justify-between mb-4 pb-4 border-b border-slate-800">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 rounded-full bg-emerald-400 animate-pulse"></div>
                  <h2 className="text-sm font-semibold tracking-wider uppercase text-slate-300">
                    Health Endpoint Live Check (`GET /api/health`)
                  </h2>
                </div>
                <span className="text-xs font-mono text-slate-500">Spring Boot 3 + Java 21</span>
              </div>

              {loading ? (
                <div className="flex items-center justify-center py-6 gap-3 text-slate-400 text-sm">
                  <RefreshCw className="w-5 h-5 animate-spin text-sky-400" />
                  Pinging backend health endpoint...
                </div>
              ) : error ? (
                <div className="rounded-xl bg-rose-500/10 border border-rose-500/20 p-4 text-rose-300 text-sm flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold mb-1">Backend service not reached via proxy</p>
                    <p className="text-xs text-rose-400/80 font-mono">{error}</p>
                    <p className="text-xs text-slate-400 mt-2">
                      Start the Spring Boot backend (`cd backend && ./mvnw spring-boot:run`) on port 8080.
                    </p>
                  </div>
                </div>
              ) : healthStatus ? (
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                    <span className="text-xs text-slate-400">Response Status:</span>
                    <span className="text-xs font-mono font-semibold text-emerald-400 px-2 py-0.5 rounded bg-emerald-950/50 border border-emerald-800/50">
                      {healthStatus.status}
                    </span>
                  </div>
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                    <span className="text-xs text-slate-400">Message:</span>
                    <span className="text-xs text-slate-200 font-medium">{healthStatus.message}</span>
                  </div>
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800/80">
                    <span className="text-xs text-slate-400">Timestamp:</span>
                    <span className="text-xs font-mono text-slate-400">{healthStatus.timestamp}</span>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        </div>

        {/* Architecture Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400 mb-4">
              <Inbox className="w-5 h-5" />
            </div>
            <h3 className="text-base font-semibold text-white mb-2">Gmail Integration</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Connects with secure OAuth2 client credentials using minimum readonly scope (<code className="text-sky-300 text-xs">gmail.readonly</code>) to fetch job communications safely.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4">
              <Layers className="w-5 h-5" />
            </div>
            <h3 className="text-base font-semibold text-white mb-2">Spring Boot 3 + Java 21</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Robust layered backend with Spring Web, Data JPA, PostgreSQL persistence, Flyway migrations, and comprehensive JUnit 5/Testcontainers tests.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 hover:border-slate-700 transition">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 mb-4">
              <Database className="w-5 h-5" />
            </div>
            <h3 className="text-base font-semibold text-white mb-2">PostgreSQL & Docker</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Local containerized database with Docker Compose, automated health checks, persistent volumes, and versioned migrations.
            </p>
          </div>
        </div>

        {/* Tech Stack Pills */}
        <div className="flex flex-wrap items-center justify-center gap-2 pt-4 border-t border-slate-800/60 text-xs text-slate-400">
          <span className="font-semibold text-slate-300">Stack:</span>
          {['Java 21', 'Spring Boot 3', 'Spring Security OAuth2', 'PostgreSQL 16', 'Flyway', 'Testcontainers', 'React 19', 'Vite', 'Tailwind CSS'].map((tech) => (
            <span key={tech} className="px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-slate-300">
              {tech}
            </span>
          ))}
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/60 py-6 text-center text-xs text-slate-500">
        Application Tracker Monorepo &bull; Built with Spring Boot 3 &amp; React Vite
      </footer>
    </div>
  );
}
