import React, { useState, useCallback, useEffect } from 'react'
import { fetchTransactions, fetchStats, fetchAlerts } from './api/client'
import Dashboard from './components/Dashboard'
import TransactionFeed from './components/TransactionFeed'
import FraudAlerts from './components/FraudAlerts'
import ChatPanel from './components/ChatPanel'
import { Activity, MessageSquare, ShieldAlert, LayoutDashboard, Sun, Moon } from 'lucide-react'

const TABS = [
  { id: 'dashboard',    label: 'Dashboard',    icon: LayoutDashboard },
  { id: 'transactions', label: 'Live Feed',    icon: Activity },
  { id: 'alerts',       label: 'Fraud Alerts', icon: ShieldAlert },
  { id: 'ai',           label: 'AI Analyst',   icon: MessageSquare },
]

function normaliseAlert(a) {
  return {
    id:          a.id,
    senderName:  a.senderName,
    receiverName:a.receiverName,
    amount:      a.amount,
    riskScore:   a.riskScore,
    alertType:   a.alertType,
    description: a.description,
    timestamp:   a.timestamp,
  }
}

export default function App() {
  const [tab, setTab]     = useState('dashboard')
  const [transactions, setTxns]   = useState([])
  const [stats, setStats]         = useState(null)
  const [alerts, setAlerts]       = useState([])
  const [newAlertCount, setNewAlert] = useState(0)

  const [dark, setDark] = useState(() => {
    const stored = localStorage.getItem('payflow-theme')
    return stored ? stored === 'dark' : true
  })

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
    localStorage.setItem('payflow-theme', dark ? 'dark' : 'light')
  }, [dark])

  const loadData = useCallback(() => {
    fetchTransactions(50).then(setTxns).catch(() => {})
    fetchStats().then(setStats).catch(() => {})
    fetchAlerts(20).then(data => setAlerts(data.map(normaliseAlert))).catch(() => {})
  }, [])

  useEffect(() => {
    loadData()
    const id = setInterval(loadData, 30000)
    return () => clearInterval(id)
  }, [loadData])

  const switchTab = (id) => {
    setTab(id)
    if (id === 'alerts') setNewAlert(0)
  }

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-[#0f1729] flex flex-col transition-colors duration-200">

      {/* Header */}
      <header className="border-b border-slate-200 dark:border-white/[0.06] bg-white dark:bg-[#0b1220] px-6 py-3 flex items-center justify-between sticky top-0 z-50 shadow-sm dark:shadow-none">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-blue-600 flex items-center justify-center shadow-lg shadow-violet-900/20">
            <Activity size={16} className="text-white" />
          </div>
          <div>
            <span className="text-slate-900 dark:text-white font-bold text-base tracking-tight">PayFlow</span>
            <span className="ml-2 text-slate-400 text-xs font-medium hidden sm:inline">AI Payment Intelligence</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-xs px-3 py-1.5 rounded-full border border-violet-500/30 bg-violet-500/10 text-violet-600 dark:text-violet-400">
            <MessageSquare size={12} />
            <span className="font-medium font-mono">gpt-oss:20b</span>
            <span className="w-1.5 h-1.5 rounded-full bg-violet-500 dark:bg-violet-400 animate-pulse" />
          </div>

          <button
            onClick={() => setDark(d => !d)}
            className="p-2 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-100 dark:bg-white/5 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
            title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {dark ? <Sun size={15} /> : <Moon size={15} />}
          </button>
        </div>
      </header>

      {/* Nav tabs */}
      <nav className="border-b border-slate-200 dark:border-white/[0.06] bg-white dark:bg-[#0b1220] px-6">
        <div className="flex">
          {TABS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => switchTab(id)}
              className={`relative flex items-center gap-2 px-4 py-3.5 text-sm font-medium border-b-2 transition-all ${
                tab === id
                  ? id === 'ai'
                    ? 'border-violet-500 text-violet-600 dark:text-violet-400'
                    : 'border-blue-500 text-blue-600 dark:text-white'
                  : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              <Icon size={14} />
              {label}
              {id === 'alerts' && newAlertCount > 0 && (
                <span className="animate-slide-in absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {newAlertCount > 9 ? '9+' : newAlertCount}
                </span>
              )}
            </button>
          ))}
        </div>
      </nav>

      {/* Page content */}
      <main className="flex-1 p-6 overflow-auto">
        {tab === 'dashboard'    && <Dashboard stats={stats} transactions={transactions} alerts={alerts} />}
        {tab === 'transactions' && <TransactionFeed transactions={transactions} />}
        {tab === 'alerts'       && <FraudAlerts alerts={alerts} />}
        {tab === 'ai'           && <ChatPanel />}
      </main>

    </div>
  )
}
