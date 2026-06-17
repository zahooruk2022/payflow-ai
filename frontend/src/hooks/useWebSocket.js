export function useWebSocket({ onConnected }) {
  // payflow-ai runs in demo mode — no live WebSocket
  // onConnected is never called; callers default to disconnected/demo state
}
