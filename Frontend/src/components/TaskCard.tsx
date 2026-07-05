import { Link } from 'react-router-dom';
import type { Task } from '../types';
import { useDeleteTask } from '../hooks/useTaskMutations';
import { useAuthStore } from '../stores/authStore';

interface TaskCardProps {
  task: Task;
  onEdit: (task: Task) => void;
}

export default function TaskCard({ task, onEdit }: TaskCardProps) {
  const { user } = useAuthStore();
  const { mutateAsync: deleteTask, isPending: isDeleting } = useDeleteTask();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'TODO': return 'bg-gray-200 text-gray-800';
      case 'IN_PROGRESS': return 'bg-amber-100 text-amber-800';
      case 'DONE': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'TODO': return 'To Do';
      case 'IN_PROGRESS': return 'In Progress';
      case 'DONE': return 'Done';
      default: return status;
    }
  };

  const handleDelete = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (window.confirm('Delete this task?')) {
      try {
        await deleteTask(task.id);
      } catch (err) {
        alert('Failed to delete task');
      }
    }
  };

  const handleEdit = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onEdit(task);
  };

  return (
    <Link 
      to={`/tasks/${task.id}`}
      className="block bg-white rounded-lg shadow border border-gray-200 p-4 hover:shadow-md transition cursor-pointer"
    >
      <div className="flex justify-between items-start mb-2">
        <h3 className="font-semibold text-lg line-clamp-1">{task.title}</h3>
        <span className={`text-xs px-2 py-1 rounded-full whitespace-nowrap ${getStatusColor(task.status)}`}>
          {getStatusLabel(task.status)}
        </span>
      </div>
      
      {task.description && (
        <p className="text-gray-600 text-sm mb-4 line-clamp-2">{task.description}</p>
      )}

      <div className="flex flex-col gap-2 mt-auto">
        <div className="flex justify-between text-xs text-gray-500">
          <span>{task.dueDate ? `Due: ${task.dueDate}` : 'No due date'}</span>
          {user?.role === 'ADMIN' && <span>Owner: {task.owner.fullName}</span>}
        </div>
        
        <div className="flex justify-end gap-2 mt-2 pt-2 border-t border-gray-100">
          <button
            onClick={handleEdit}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium px-2 py-1"
          >
            Edit
          </button>
          <button
            onClick={handleDelete}
            disabled={isDeleting}
            className="text-red-600 hover:text-red-800 text-sm font-medium px-2 py-1 disabled:opacity-50"
          >
            {isDeleting ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </Link>
  );
}
