import React, { useEffect, useState } from 'react'
import { Server, Brain, RefreshCw, MessageSquare } from 'lucide-react'

const SERVICES = [
  { key: 'api',   label: 'Spring Boot API', icon: Server },
  { key: 'ai',    label: 'AI Model',        icon: Brain },
]

export default function SystemStatus() {
  const [health, setHealth]   = useState({})
  const [loading, setLoading] = useState(true)
  const [lastChecked, setLastChecked] = useState(null)

  const check = () => {
    setLoading(true)
    fetch('/actuator/health')
      .then(r => r.json())
      .then(data => {
        setHealth({
          api: data.status ?? 'UNKNOWN',
          ai:  'Demo',
        })
        setLastChecked(new Date())
      })
      .catch(() => setHealth({ api: 'DOWN', ai: 'Demo' }))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    check()
    const id = setInterval(check, 15000)
    return () => clearInterval(id)
  }, [])

  const dot = (status) => {
    if (status === 'UP')    return 'bg-emerald-500'
    if (status === 'DOWN')  return 'bg-red-500'
    if (status === 'Demo')  return 'bg-violet-500'
    return 'bg-amber-400 animate-pulse'
  }

  const labelCls = (status) => {
    if (status === 'UP')    return 'text-emerald-600 dark:text-emerald-400'
    if (status === 'DOWN')  return 'text-red-600 dark:text-red-400'
    if (status === 'Demo')  return 'text-violet-600 dark:text-violet-400'
    return 'text-amber-600 dark:text-amber-400'
  }

  const fmt = (d) => d ? d.toLocaleTimeString('en-GB') : '—'

  return (
    <div className="rounded-xl border border-slate-200 dark:border-white/[0.06] bg-white dark:bg-[#141e35] p-5 transition-colors duration-200">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-slate-900 dark:text-white font-semibold text-sm flex items-center gap-2">
          <Server size={14} className="text-slate-400" />
          System Health
        </h3>
        <div className="flex items-center gap-2">
          <span className="text-slate-400 text-[10px]">Checked {fmt(lastChecked)}</span>
          <button onClick={check} className="text-slate-400 hover:text-slate-700 dark:hover:text-white transition-colors p-1 rounded">
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      <div className="space-y-2.5">
        {SERVICES.map(({ key, label: name, icon: Icon }) => {
          const status = health[key] ?? 'UNKNOWN'
          return (
            <div key={key} className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Icon size={13} className="text-slate-400" />
                <span className="text-slate-600 dark:text-slate-300 text-xs font-medium">{name}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className={`text-[10px] font-bold uppercase ${labelCls(status)}`}>{status}</span>
                <span className={`w-2 h-2 rounded-full ${dot(status)}`} />
              </div>
            </div>
          )
        })}
      </div>

      <div className="mt-4 pt-3 border-t border-slate-100 dark:border-white/[0.04]">
        <a
          href="https://rmq-4bf2a6d7-619b-4011-bcdc-44ede175fd93.sys.tpcf.tnz-field-epc.lvn.broadcom.net"
          target="_blank"
          rel="noreferrer"
          className="text-blue-500 hover:text-blue-400 text-[10px] font-medium flex items-center gap-1"
        >
          <MessageSquare size={10} /> RabbitMQ Console →
        </a>
        <p className="text-violet-500 dark:text-violet-400 text-[10px] font-medium mt-2">Demo mode · Mock data · AI chat live</p>
      </div>
    </div>
  )
}
