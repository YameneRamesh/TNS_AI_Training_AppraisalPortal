export interface User {
  id: number;
  employeeId: string;
  fullName: string;
  email: string;
  designation?: string;
  department?: string;
  managerId?: number;
  managerName?: string;
  isActive: boolean;
  roles: Role[];
  createdAt: string;
  updatedAt: string;
}

export interface Role {
  id: number;
  name: RoleName;
}

export type RoleName = 'EMPLOYEE' | 'MANAGER' | 'HR' | 'ADMIN';

export interface LoginRequest {
  loginIdentifier: string;
  password: string;
}

export interface UserProfile {
  id: number;
  employeeId: string;
  fullName: string;
  email: string;
  designation?: string;
  department?: string;
  managerName?: string;
  roles: string[];
}
