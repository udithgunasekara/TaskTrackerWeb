import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getTasks } from '../api/taskApi';
import type { TaskFilters } from '../api/taskApi';

export const useTasks = (filters: TaskFilters) =>
  useQuery({
    queryKey: ['tasks', filters],
    queryFn: () => getTasks(filters),
    placeholderData: keepPreviousData,
  });
