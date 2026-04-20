import { getUserAuth } from './auth';

export type RealtimeEvent = {
  type?: string;
  data?: unknown;
  timestamp?: number;
};

type Listener = (event: RealtimeEvent) => void;

let socket: WebSocket | null = null;
let reconnectTimer: number | null = null;
let activeToken = '';
let activeListener: Listener | null = null;
let manualClose = false;

function buildUrl(token: string) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/realtime?token=${encodeURIComponent(token)}`;
}

function clearReconnectTimer() {
  if (reconnectTimer !== null) {
    window.clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
}

function openSocket() {
  if (!activeToken) {
    return;
  }
  manualClose = false;
  clearReconnectTimer();

  const currentSocket = new WebSocket(buildUrl(activeToken));
  socket = currentSocket;

  currentSocket.onmessage = (event) => {
    if (!activeListener) {
      return;
    }
    try {
      activeListener(JSON.parse(event.data) as RealtimeEvent);
    } catch {
      // Ignore malformed messages.
    }
  };

  currentSocket.onclose = () => {
    if (socket === currentSocket) {
      socket = null;
    }
    if (!manualClose && activeToken) {
      clearReconnectTimer();
      reconnectTimer = window.setTimeout(openSocket, 3000);
    }
  };

  currentSocket.onerror = () => {
    currentSocket.close();
  };
}

export function startRealtime(listener: Listener) {
  activeListener = listener;
  const auth = getUserAuth();
  if (!auth.token) {
    stopRealtime();
    return;
  }

  if (auth.token === activeToken && socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return;
  }

  stopRealtime(false);
  activeToken = auth.token;
  openSocket();
}

export function stopRealtime(clearToken = true) {
  manualClose = true;
  clearReconnectTimer();
  if (socket) {
    socket.close();
    socket = null;
  }
  if (clearToken) {
    activeToken = '';
  }
}