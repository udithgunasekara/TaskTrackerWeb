import { z } from 'zod';

export const registerSchema = z.object({
  fullName: z.string().min(1, 'Required').max(100),
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Min 8 characters').max(72),
});

export const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Required'),
});

export const taskSchema = z.object({
  title: z.string().min(1, 'Required').max(150, 'Max 150 characters'),
  description: z.string().max(2000, 'Max 2000 characters').optional().nullable(),
  status: z.enum(['TODO', 'IN_PROGRESS', 'DONE']),
  dueDate: z.string().optional().nullable(),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
export type LoginFormValues = z.infer<typeof loginSchema>;
export type TaskFormValues = z.infer<typeof taskSchema>;
