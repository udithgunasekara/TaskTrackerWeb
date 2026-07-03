import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { IMessage } from '@stomp/stompjs';
import { useAuthStore } from '../stores/authStore';
import { useToastStore } from '../stores/toastStore';
import { createStompClient } from '../ws/stompClient';
import type { TaskEvent } from '../types';

export function useRealtime() {
  const { token, user } = useAuthStore();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!token || !user) return;
    const client = createStompClient(token);

    const handle = (msg: IMessage) => {
      const event: TaskEvent = JSON.parse(msg.body);
      
      // Invalidate queries so UI reflects latest data
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['task', event.task.id] });
      
      const action = event.type === 'CREATED' ? 'created' : event.type === 'UPDATED' ? 'updated' : 'deleted';
      useToastStore.getState().addToast(`Task '${event.task.title}' was ${action}`);
    };

    const subs = [client.watch('/user/queue/tasks').subscribe(handle)];
    if (user.role === 'ADMIN') {
      subs.push(client.watch('/topic/admin/tasks').subscribe(handle));
    }

    return () => {
      subs.forEach(s => s.unsubscribe());
      client.deactivate();
    };
  }, [token, user?.id, user?.role, queryClient]);
}
