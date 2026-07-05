import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { taskSchema } from '../lib/schemas';
import type { TaskFormValues } from '../lib/schemas';
import type { Task } from '../types';
import { useCreateTask, useUpdateTask } from '../hooks/useTaskMutations';

interface TaskFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  task?: Task | null;
}

export default function TaskFormModal({ isOpen, onClose, task }: TaskFormModalProps) {
  const { mutateAsync: createTask, isPending: isCreating } = useCreateTask();
  const { mutateAsync: updateTask, isPending: isUpdating } = useUpdateTask();
  const isSubmitting = isCreating || isUpdating;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskSchema),
    defaultValues: {
      title: '',
      description: '',
      status: 'TODO',
      dueDate: '',
    },
  });

  useEffect(() => {
    if (task && isOpen) {
      reset({
        title: task.title,
        description: task.description || '',
        status: task.status,
        dueDate: task.dueDate ? task.dueDate.split('T')[0] : '', // Adjust date format if needed
      });
    } else if (isOpen) {
      reset({
        title: '',
        description: '',
        status: 'TODO',
        dueDate: '',
      });
    }
  }, [task, isOpen, reset]);

  if (!isOpen) return null;

  const onSubmit = async (data: TaskFormValues) => {
    try {
      if (task) {
        await updateTask({ id: task.id, data });
      } else {
        await createTask(data);
      }
      onClose();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to save task');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 className="text-xl font-bold mb-4">{task ? 'Edit Task' : 'New Task'}</h2>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
            <input
              type="text"
              {...register('title')}
              className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 outline-none"
            />
            {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              {...register('description')}
              rows={3}
              className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 outline-none"
            ></textarea>
            {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select
              {...register('status')}
              className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="TODO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="DONE">Done</option>
            </select>
            {errors.status && <p className="text-red-500 text-xs mt-1">{errors.status.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Due Date</label>
            <input
              type="date"
              {...register('dueDate')}
              className="w-full p-2 border rounded focus:ring-2 focus:ring-blue-500 outline-none"
            />
            {errors.dueDate && <p className="text-red-500 text-xs mt-1">{errors.dueDate.message}</p>}
          </div>

          <div className="flex justify-end gap-2 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-gray-600 bg-gray-100 rounded hover:bg-gray-200"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
