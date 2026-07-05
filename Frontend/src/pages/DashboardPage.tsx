import { useState } from 'react';
import { useAuthStore } from '../stores/authStore';
import { useTasks } from '../hooks/useTasks';
import type { TaskFilters } from '../api/taskApi';
import type { TaskStatus, Task } from '../types';
import TaskCard from '../components/TaskCard';
import TaskFormModal from '../components/TaskFormModal';

export default function DashboardPage() {
  const { user } = useAuthStore();
  
  const [filters, setFilters] = useState<TaskFilters>({ page: 0, size: 10 });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);

  const { data, isLoading, isError, refetch } = useTasks(filters);

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const status = e.target.value === 'ALL' ? undefined : (e.target.value as TaskStatus);
    setFilters(prev => ({ ...prev, status, page: 0 }));
  };

  const handleOwnerChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const ownerId = e.target.value ? parseInt(e.target.value, 10) : undefined;
    setFilters(prev => ({ ...prev, ownerId, page: 0 }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters(prev => ({ ...prev, page: newPage }));
  };

  const handleCreateClick = () => {
    setEditingTask(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (task: Task) => {
    setEditingTask(task);
    setIsModalOpen(true);
  };

  return (
    <div>
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
        <h1 className="text-2xl font-bold">Tasks</h1>
        
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-gray-700">Status:</label>
            <select
              value={filters.status || 'ALL'}
              onChange={handleStatusChange}
              className="p-2 border rounded outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">All</option>
              <option value="TODO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="DONE">Done</option>
            </select>
          </div>

          {user?.role === 'ADMIN' && (
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Owner ID:</label>
              <input
                type="number"
                min="1"
                placeholder="Any"
                value={filters.ownerId || ''}
                onChange={handleOwnerChange}
                className="p-2 border rounded w-24 outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}

          <button
            onClick={handleCreateClick}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 w-full sm:w-auto"
          >
            New Task
          </button>
        </div>
      </div>

      {isLoading && <div className="text-center py-10">Loading tasks...</div>}
      
      {isError && (
        <div className="text-center py-10 text-red-600">
          <p>Failed to load tasks.</p>
          <button onClick={() => refetch()} className="mt-2 text-blue-600 underline">Retry</button>
        </div>
      )}

      {data && data.content.length === 0 && !isLoading && !isError && (
        <div className="text-center py-10 bg-white rounded-lg border border-gray-200">
          <p className="text-gray-500">No tasks found.</p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {data.content.map(task => (
              <TaskCard key={task.id} task={task} onEdit={handleEditClick} />
            ))}
          </div>

          <div className="flex justify-between items-center mt-6 p-4 bg-white rounded-lg border border-gray-200">
            <button
              disabled={data.page === 0}
              onClick={() => handlePageChange(data.page - 1)}
              className="px-4 py-2 border rounded disabled:opacity-50 hover:bg-gray-50"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">
              Page {data.page + 1} of {data.totalPages === 0 ? 1 : data.totalPages}
            </span>
            <button
              disabled={data.last}
              onClick={() => handlePageChange(data.page + 1)}
              className="px-4 py-2 border rounded disabled:opacity-50 hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        </>
      )}

      <TaskFormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        task={editingTask}
      />
    </div>
  );
}
