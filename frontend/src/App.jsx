import { useState, useRef, useEffect } from 'react'

const SUGGESTED_QUESTIONS = [
  "What's our fraud rate today?",
  "Show me the 5 most recent transactions",
  "Explain the fraud detection rules",
  "Which bank has the highest balance?",
  "Generate a fraud narrative for PF-20261007",
  "Are there any critical risk alerts right now?",
]

const SEARCH_EXAMPLES = [
  "suspicious late night large payment",
  "rapid succession velocity fraud",
  "corporate treasury settlement",
  "new account first payment",
  "split payment structuring",
]

function RiskBadge({ score }) {
  const cls =
    score >= 80 ? 'bg-red-900/50 text-red-300 border border-red-700' :
    score >= 60 ? 'bg-orange-900/50 text-orange-300 border border-orange-700' :
    score >= 40 ? 'bg-yellow-900/50 text-yellow-300 border border-yellow-700' :
                  'bg-green-900/50 text-green-300 border border-green-700'
  const label = score >= 80 ? 'Critical' : score >= 60 ? 'High' : score >= 40 ? 'Medium' : 'Low'
  return <span className={`text-xs px-2 py-0.5 rounded font-mono ${cls}`}>{label} {score}</span>
}

function StatusBadge({ status }) {
  const cls = status === 'FLAGGED'
    ? 'bg-red-900/40 text-red-300 border border-red-800'
    : 'bg-green-900/40 text-green-300 border border-green-800'
  return <span className={`text-xs px-2 py-0.5 rounded ${cls}`}>{status}</span>
}

function Message({ role, content }) {
  const isUser = role === 'user'
  return (
    <div className={`flex gap-3 ${isUser ? 'flex-row-reverse' : ''}`}>
      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 ${
        isUser ? 'bg-blue-600' : 'bg-violet-700'
      }`}>
        {isUser ? 'U' : 'AI'}
      </div>
      <div className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
        isUser
          ? 'bg-blue-600/20 border border-blue-700/40 text-blue-100'
          : 'bg-gray-800 border border-gray-700 text-gray-200'
      }`}>
        <div className="prose-ai whitespace-pre-wrap">{content}</div>
      </div>
    </div>
  )
}

function ChatTab({ sessionId, setSessionId }) {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: "Hello! I'm PayFlow Intelligence, your AI payment analyst.\n\nI can query transaction data, explain fraud patterns, analyse risk, and generate investigation narratives — all powered by your Tanzu Platform LLM.\n\nWhat would you like to know?" }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

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
      const data = await res.json()
      if (!sessionId) setSessionId(data.sessionId)
      setMessages(prev => [...prev, { role: 'assistant', content: data.response }])
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: '⚠️ Could not reach the AI backend. Is the Spring Boot app running?' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 min-h-0">
        {messages.map((m, i) => <Message key={i} {...m} />)}
        {loading && (
          <div className="flex gap-3">
            <div className="w-8 h-8 rounded-full bg-violet-700 flex items-center justify-center text-sm font-bold">AI</div>
            <div className="bg-gray-800 border border-gray-700 rounded-2xl px-4 py-3 flex gap-1 items-center">
              {[0,1,2].map(i => (
                <div key={i} className="w-2 h-2 bg-violet-400 rounded-full animate-bounce" style={{ animationDelay: `${i*0.15}s` }} />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Suggestions */}
      <div className="px-4 pb-2 flex gap-2 flex-wrap">
        {SUGGESTED_QUESTIONS.map(q => (
          <button key={q} onClick={() => send(q)}
            className="text-xs bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-full px-3 py-1 text-gray-400 hover:text-gray-200 transition-colors">
            {q}
          </button>
        ))}
      </div>

      {/* Input */}
      <div className="p-4 border-t border-gray-800">
        <form onSubmit={e => { e.preventDefault(); send() }} className="flex gap-2">
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            placeholder="Ask anything about payments, fraud, or balances…"
            className="flex-1 bg-gray-800 border border-gray-700 rounded-xl px-4 py-2.5 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500"
          />
          <button type="submit" disabled={!input.trim() || loading}
            className="bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed rounded-xl px-5 py-2.5 text-sm font-semibold transition-colors">
            Send
          </button>
        </form>
      </div>
    </div>
  )
}

function SearchTab() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)

  async function search(q) {
    const text = q || query.trim()
    if (!text || loading) return
    setQuery(text)
    setLoading(true)
    setResults(null)
    try {
      const res = await fetch(`/api/search?q=${encodeURIComponent(text)}&topK=6`)
      setResults(await res.json())
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-full p-4 gap-4">
      <div>
        <h2 className="text-sm font-semibold text-gray-400 mb-1">Semantic Transaction Search</h2>
        <p className="text-xs text-gray-500 mb-3">
          Powered by <span className="text-violet-400 font-mono">nomic-embed-text-v2-moe</span> — finds transactions by meaning, not keyword.
        </p>
        <form onSubmit={e => { e.preventDefault(); search() }} className="flex gap-2">
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="e.g. suspicious large payment from new account"
            className="flex-1 bg-gray-800 border border-gray-700 rounded-xl px-4 py-2.5 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500"
          />
          <button type="submit" disabled={!query.trim() || loading}
            className="bg-violet-600 hover:bg-violet-500 disabled:opacity-40 rounded-xl px-5 py-2.5 text-sm font-semibold transition-colors">
            Search
          </button>
        </form>

        <div className="flex gap-2 mt-2 flex-wrap">
          {SEARCH_EXAMPLES.map(ex => (
            <button key={ex} onClick={() => search(ex)}
              className="text-xs bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-full px-3 py-1 text-gray-400 hover:text-gray-200 transition-colors">
              {ex}
            </button>
          ))}
        </div>
      </div>

      {loading && (
        <div className="flex items-center gap-2 text-sm text-gray-400">
          <div className="w-4 h-4 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
          Embedding query and searching…
        </div>
      )}

      {results && (
        <div className="flex-1 overflow-y-auto space-y-3">
          {results.length === 0 ? (
            <p className="text-gray-500 text-sm">No results found.</p>
          ) : results.map((r, i) => (
            <div key={i} className="bg-gray-800/60 border border-gray-700 rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-mono text-violet-400">{r.txnId}</span>
                <div className="flex items-center gap-2">
                  <StatusBadge status={r.status} />
                  <RiskBadge score={r.riskScore} />
                  <span className="text-xs text-gray-500 font-mono">
                    {(r.similarity * 100).toFixed(1)}% match
                  </span>
                </div>
              </div>
              <p className="text-sm text-gray-300 leading-relaxed">{r.description}</p>
              <div className="mt-2 w-full bg-gray-700 rounded-full h-1">
                <div className="bg-violet-500 h-1 rounded-full transition-all" style={{ width: `${r.similarity * 100}%` }} />
              </div>
            </div>
          ))}
        </div>
      )}

      {!results && !loading && (
        <div className="flex-1 flex items-center justify-center text-gray-600 text-sm text-center">
          <div>
            <div className="text-4xl mb-3">🔍</div>
            <p>Describe what you're looking for in plain English.<br />The embedding model finds semantically similar transactions.</p>
          </div>
        </div>
      )}
    </div>
  )
}

export default function App() {
  const [tab, setTab] = useState('chat')
  const [sessionId, setSessionId] = useState(null)

  return (
    <div className="h-screen flex flex-col bg-gray-950">
      {/* Header */}
      <header className="border-b border-gray-800 px-6 py-3 flex items-center justify-between flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-blue-600 flex items-center justify-center text-white font-bold text-sm">AI</div>
          <div>
            <h1 className="text-sm font-bold text-white">PayFlow Intelligence</h1>
            <p className="text-xs text-gray-500">Tanzu Platform AI · <span className="font-mono text-violet-400">gpt-oss:20b</span> + <span className="font-mono text-blue-400">nomic-embed</span></p>
          </div>
        </div>
        <div className="flex gap-1 bg-gray-800 rounded-lg p-1">
          {[['chat', '💬 Chat Analyst'], ['search', '🔍 Semantic Search']].map(([id, label]) => (
            <button key={id} onClick={() => setTab(id)}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                tab === id ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'
              }`}>
              {label}
            </button>
          ))}
        </div>
        {sessionId && (
          <button onClick={() => { setSessionId(null); window.location.reload() }}
            className="text-xs text-gray-500 hover:text-gray-300 transition-colors">
            New session
          </button>
        )}
      </header>

      {/* Content */}
      <main className="flex-1 min-h-0">
        {tab === 'chat'
          ? <ChatTab sessionId={sessionId} setSessionId={setSessionId} />
          : <SearchTab />}
      </main>
    </div>
  )
}
