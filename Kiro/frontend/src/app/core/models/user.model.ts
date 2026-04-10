export interface User {
  id: number;
  employeeId: string;
  fullName: string;
  email: string;
  designation?: string;
  department?: string;
  managerName?: string;
  isActive?: boolean;
  roles: string[];
}

export function getRoles(user: User): string[] {
  if (!user.roles) return [];
  if (Array.isArray(user.roles)) return user.roles;
  return [];
}

export type RoleName = 'EMPLOYEE' | 'MANAGER' | 'HR' | 'ADMIN';

export interface LoginRequest {
  loginIdentifier: string;
  password: string;
}
