import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getTask } from '../api/taskApi';
import { useDeleteTask } from '../hooks/useTaskMutations';
import TaskFormModal from '../components/TaskFormModal';

export default function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const taskId = parseInt(id as string, 10);

  const [isModalOpen, setIsModalOpen] = useState(false);

  const { data: task, isLoading, isError } = useQuery({
    queryKey: ['task', taskId],
    queryFn: () => getTask(taskId),
    enabled: !isNaN(taskId),
  });

  const { mutateAsync: deleteTask, isPending: isDeleting } = useDeleteTask();

  if (isNaN(taskId)) {
    return <div className="text-center py-10 text-red-500 font-bold">Invalid Task ID</div>;
  }

  if (isLoading) return <div className="text-center py-10">Loading task details...</div>;
  if (isError || !task) return (
    <div className="text-center py-10">
      <h2 className="text-xl font-bold text-red-600 mb-4">Task not found</h2>
      <Link to="/" className="text-blue-600 hover:underline">← Back to Dashboard</Link>
    </div>
  );

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await deleteTask(task.id);
        navigate('/');
      } catch (err) {
        alert('Failed to delete task');
      }
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'TODO': return 'bg-gray-200 text-gray-800';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-800';
      case 'DONE': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <Link to="/" className="text-blue-600 hover:underline mb-6 inline-block">
        ← Back to Dashboard
      </Link>

      <div className="bg-white rounded-lg shadow-md p-6 md:p-8">
        <div className="flex justify-between items-start mb-6 border-b pb-4">
          <div>
            <h1 className="text-2xl font-bold mb-2">{task.title}</h1>
            <span className={`text-sm px-3 py-1 rounded-full font-medium ${getStatusColor(task.status)}`}>
              {task.status.replace('_', ' ')}
            </span>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setIsModalOpen(true)}
              className="px-4 py-2 bg-blue-50 text-blue-600 rounded hover:bg-blue-100"
            >
              Edit
            </button>
            <button
              onClick={handleDelete}
              disabled={isDeleting}
              className="px-4 py-2 bg-red-50 text-red-600 rounded hover:bg-red-100 disabled:opacity-50"
            >
              {isDeleting ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="md:col-span-2">
            <h3 className="text-lg font-semibold mb-2">Description</h3>
            {task.description ? (
              <div className="text-gray-700 whitespace-pre-wrap">{task.description}</div>
            ) : (
              <p className="text-gray-400 italic">No description provided.</p>
            )}
          </div>

          <div className="bg-gray-50 p-4 rounded-lg space-y-4">
            <div>
              <p className="text-sm text-gray-500 font-medium">Due Date</p>
              <p className="text-gray-900">{task.dueDate || 'None'}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500 font-medium">Owner</p>
              <p className="text-gray-900">{task.owner.fullName}</p>
              <p className="text-xs text-gray-500">{task.owner.email}</p>
            </div>
            <div className="pt-4 border-t border-gray-200">
              <p className="text-xs text-gray-500">
                Created: {new Date(task.createdAt).toLocaleString()}
              </p>
              {task.updatedAt && (
                <p className="text-xs text-gray-500 mt-1">
                  Updated: {new Date(task.updatedAt).toLocaleString()}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      <TaskFormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        task={task}
      />
    </div>
  );
}
