import { useState, useRef, useEffect } from 'react'
import { MessageSquare } from 'lucide-react'

const SUGGESTED = [
  "What's our fraud rate today?",
  "Show me the 5 most recent transactions",
  "Explain the fraud detection rules",
  "Which bank has the highest balance?",
  "Generate a fraud narrative for PF-20261007",
  "Are there any critical risk alerts right now?",
]

function Bubble({ role, content }) {
  const isUser = role === 'user'
  return (
    <div className={`flex gap-3 ${isUser ? 'flex-row-reverse' : ''}`}>
      <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${
        isUser ? 'bg-blue-600' : 'bg-violet-700'
      }`}>
        {isUser ? 'U' : 'AI'}
      </div>
      <div className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
        isUser
          ? 'bg-blue-600/20 border border-blue-700/40 text-blue-100 dark:text-blue-100'
          : 'bg-white dark:bg-[#141e35] border border-slate-200 dark:border-white/[0.06] text-slate-800 dark:text-slate-200'
      }`}>
        <div className="whitespace-pre-wrap">{content}</div>
      </div>
    </div>
  )
}

export default function ChatPanel() {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: "Hello! I'm PayFlow Intelligence, your AI payment analyst.\n\nI can query transaction data, explain fraud patterns, analyse risk, and generate investigation narratives.\n\nWhat would you like to know?" }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [sessionId, setSessionId] = useState(null)
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  async function send(text) {
    const msg = text || input.trim()
    if (!msg || loading) return
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: msg }])
    setLoading(true)
    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: msg, sessionId }),
      })
      if (!res.ok) {
        const txt = await res.text().catch(() => '')
        setMessages(prev => [...prev, { role: 'assistant', content: `⚠️ Error ${res.status} from AI backend. Check CF logs.\n\n${txt.slice(0, 300)}` }])
        return
      }
      const data = await res.json()
      if (!sessionId) setSessionId(data.sessionId)
      setMessages(prev => [...prev, { role: 'assistant', content: data.response }])
    } catch (e) {
      setMessages(prev => [...prev, { role: 'assistant', content: `⚠️ Network error: ${e.message}` }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl flex flex-col h-[calc(100vh-160px)]">
      {/* Header strip */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <MessageSquare size={16} className="text-violet-500" />
          <span className="text-slate-900 dark:text-white font-semibold text-sm">PayFlow Intelligence</span>
          <span className="text-[10px] font-mono text-violet-500 bg-violet-500/10 border border-violet-500/20 px-2 py-0.5 rounded-full">gpt-oss:20b</span>
        </div>
        {sessionId && (
          <button
            onClick={() => { setSessionId(null); setMessages([{ role: 'assistant', content: "New session started. How can I help?" }]) }}
            className="text-xs text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors"
          >
            New session
          </button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto rounded-xl border border-slate-200 dark:border-white/[0.06] bg-white dark:bg-[#141e35] p-4 space-y-4 min-h-0">
        {messages.map((m, i) => <Bubble key={i} {...m} />)}
        {loading && (
          <div className="flex gap-3">
            <div className="w-7 h-7 rounded-full bg-violet-700 flex items-center justify-center text-xs font-bold">AI</div>
            <div className="bg-white dark:bg-[#141e35] border border-slate-200 dark:border-white/[0.06] rounded-2xl px-4 py-3 flex gap-1 items-center">
              {[0,1,2].map(i => (
                <div key={i} className="w-1.5 h-1.5 bg-violet-400 rounded-full animate-bounce" style={{ animationDelay: `${i*0.15}s` }} />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Suggestions */}
      <div className="mt-3 flex gap-2 flex-wrap">
        {SUGGESTED.map(q => (
          <button key={q} onClick={() => send(q)}
            className="text-xs bg-white dark:bg-white/5 hover:bg-slate-50 dark:hover:bg-white/10 border border-slate-200 dark:border-white/10 rounded-full px-3 py-1 text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
            {q}
          </button>
        ))}
      </div>

      {/* Input */}
      <form onSubmit={e => { e.preventDefault(); send() }} className="mt-3 flex gap-2">
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Ask anything about payments, fraud, or balances…"
          className="flex-1 bg-white dark:bg-[#141e35] border border-slate-200 dark:border-white/[0.06] rounded-xl px-4 py-2.5 text-sm text-slate-900 dark:text-gray-100 placeholder-slate-400 focus:outline-none focus:border-violet-500"
        />
        <button type="submit" disabled={!input.trim() || loading}
          className="bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition-colors">
          Send
        </button>
      </form>
    </div>
  )
}
