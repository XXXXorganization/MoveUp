// src/modules/user/types.ts

export interface User {
  id: string; // UUID
  phone: string;
  nickname: string;
  avatar?: string;
  gender?: number; // 0-未知，1-男，2-女
  birthday?: string; // DATE
  height?: number; // cm
  weight?: number; // kg
  target_distance?: number; // 米
  target_time?: number; // 分钟
  role: string; // 'user' | 'admin'
  created_at: string;
  updated_at: string;
}

export interface CreateUserRequest {
  phone: string;
  nickname: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
  height?: number;
  weight?: number;
  target_distance?: number;
  target_time?: number;
}

export interface UpdateUserRequest {
  nickname?: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
  height?: number;
  weight?: number;
  target_distance?: number;
  target_time?: number;
}

export interface LoginRequest {
  phone: string;
  code: string;
}

export interface LoginResponse {
  token: string;
  expires_in: number;
  user: {
    id: string;
    nickname: string;
    avatar?: string;
  };
}

export interface SendCodeRequest {
  phone: string;
  type: 'login' | 'register' | 'reset';
}

export interface UserProfileResponse {
  id: string;
  nickname: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
  height?: number;
  weight?: number;
  total_distance?: number; // 需要计算
  total_time?: number; // 需要计算
  total_runs?: number; // 需要计算
  level?: number; // 需要计算
  preferences: {
    target_distance?: number;
    remind_time?: string;
    voice_frequency?: string;
  };
}