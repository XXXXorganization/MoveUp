// src/modules/challenge/types.ts

export interface Badge {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  conditionType: string;
  conditionValue: number;
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
  createdAt: Date;
}

export interface Task {
  id: string;
  name: string;
  description?: string;
  taskType: 'daily' | 'weekly' | 'achievement';
  requirement: { target: number; unit: string };
  rewardPoints: number;
  rewardBadgeId?: string;
  isActive: boolean;
  createdAt: Date;
}

export interface UserTask {
  id: string;
  userId: string;
  taskId: string;
  progress: number;
  isCompleted: boolean;
  completedAt?: Date;
  createdAt: Date;
}

export interface UserTaskDetail extends UserTask {
  task: Task;
  badge?: Badge;
}

export interface UserAchievement {
  id: string;
  userId: string;
  badgeId: string;
  achievedAt: Date;
}

export interface UserAchievementDetail extends UserAchievement {
  badge: Badge;
}

export interface Challenge {
  id: string;
  name: string;
  description?: string;
  challengeType: 'virtual_route' | 'team_relay' | 'distance';
  startTime: Date;
  endTime: Date;
  targetValue: number;
  rewardPoints: number;
  rewardBadgeId?: string;
  createdAt: Date;
}

export interface UserChallenge {
  id: string;
  userId: string;
  challengeId: string;
  progress: number;
  rank?: number;
  isCompleted: boolean;
  joinedAt: Date;
  completedAt?: Date;
}

export interface UserChallengeDetail extends UserChallenge {
  challenge: Challenge;
}

export interface MembershipPlan {
  id: string;
  name: string;
  durationDays: number;
  price: number;
  features: string[];
  isActive: boolean;
  createdAt: Date;
}

export interface UserMembership {
  id: string;
  userId: string;
  planId: string;
  startDate: Date;
  endDate: Date;
  isActive: boolean;
  createdAt: Date;
}

export interface UserMembershipDetail extends UserMembership {
  plan: MembershipPlan;
  daysRemaining: number;
}

export interface PointsLog {
  id: string;
  userId: string;
  pointsChange: number;
  sourceType: string;
  sourceId?: string;
  description?: string;
  createdAt: Date;
}

export interface UpdateTaskProgressRequest {
  taskId: string;
  progress: number;
}
