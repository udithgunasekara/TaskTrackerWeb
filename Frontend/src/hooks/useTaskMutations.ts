import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createTask, updateTask, deleteTask } from '../api/taskApi';
import { useToastStore } from '../stores/toastStore';

export const useCreateTask = () => {
  const queryClient = useQueryClient();
  const addToast = useToastStore((state) => state.addToast);
  return useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      addToast('Task created');
    },
  });
};

export const useUpdateTask = () => {
  const queryClient = useQueryClient();
  const addToast = useToastStore((state) => state.addToast);
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => updateTask(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['task', variables.id] });
      addToast('Task updated');
    },
  });
};

export const useDeleteTask = () => {
  const queryClient = useQueryClient();
  const addToast = useToastStore((state) => state.addToast);
  return useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      addToast('Task deleted');
    },
  });
};
