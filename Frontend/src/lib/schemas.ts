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

export type RegisterFormValues = z.infer<typeof registerSchema>;
export type LoginFormValues = z.infer<typeof loginSchema>;
