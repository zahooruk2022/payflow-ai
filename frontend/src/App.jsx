import { useState, useRef, useEffect } from 'react'

const SUGGESTED_QUESTIONS = [
  "What's our fraud rate today?",
  "Show me the 5 most recent transactions",
  "Explain the fraud detection rules",
  "Which bank has the highest balance?",
  "Generate a fraud narrative for PF-20261007",
  "Are there any critical risk alerts right now?",
]

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

export default function App() {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: "Hello! I'm PayFlow Intelligence, your AI payment analyst.\n\nI can query transaction data, explain fraud patterns, analyse risk, and generate investigation narratives — all powered by your Tanzu Platform LLM.\n\nWhat would you like to know?" }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [sessionId, setSessionId] = useState(null)
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
    <div className="h-screen flex flex-col bg-gray-950">
      {/* Header */}
      <header className="border-b border-gray-800 px-6 py-3 flex items-center justify-between flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-blue-600 flex items-center justify-center text-white font-bold text-sm">AI</div>
          <div>
            <h1 className="text-sm font-bold text-white">PayFlow Intelligence</h1>
            <p className="text-xs text-gray-500">Tanzu Platform AI · <span className="font-mono text-violet-400">gpt-oss:20b</span></p>
          </div>
        </div>
        {sessionId && (
          <button onClick={() => { setSessionId(null); window.location.reload() }}
            className="text-xs text-gray-500 hover:text-gray-300 transition-colors">
            New session
          </button>
        )}
      </header>

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
