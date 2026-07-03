import { RxStomp } from '@stomp/rx-stomp';

export const createStompClient = (token: string) => {
  const client = new RxStomp();
  const raw = (import.meta.env.VITE_WS_URL as string) || 'ws://localhost:8082/ws';
  const brokerURL = raw.startsWith('/')
    ? `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}${raw}`
    : raw;
    
  client.configure({
    brokerURL,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
};
