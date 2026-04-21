// src/modules/coaching/types.ts

export interface TrainingPlan {
  id: string;
  name: string;
  description?: string;
  difficulty: 'beginner' | 'intermediate' | 'advanced';
  durationWeeks: number;
  targetDistance: number; // 米
  schedule: WeekSchedule[];
  created_at?: string;
}

export interface WeekSchedule {
  week: number;
  days: DayTraining[];
}

export interface DayTraining {
  day: number; // 1-7
  type: 'run' | 'rest' | 'cross_train';
  distance?: number; // 米
  duration?: number; // 分钟
  pace?: string; // 如 "5:30-6:00"
  description?: string;
}

export interface UserPlan {
  id: string;
  userId: string;
  planId: string;
  currentWeek: number;
  currentDay: number;
  isActive: boolean;
  startedAt: Date;
  completedAt?: Date;
}

export interface UserPlanDetail extends UserPlan {
  plan: TrainingPlan;
  todayTask?: DayTraining;
  completionRate: number; // 0-100
}

export interface PlanRecommendRequest {
  fitnessLevel: 'beginner' | 'intermediate' | 'advanced';
  targetDistance: number; // 米
  weeklyFrequency: number; // 每周天数
  goalType: 'health' | 'speed' | 'distance' | 'marathon';
}

export interface UpdatePlanProgressRequest {
  currentWeek: number;
  currentDay: number;
}

export interface VoiceGuidanceConfig {
  distanceInterval: number; // 米，如 1000 表示每公里播报
  paceAlertEnabled: boolean;
  heartRateAlertEnabled: boolean;
  paceThreshold?: number; // 秒/公里，超过此配速则提醒
  heartRateThreshold?: number; // bpm，超过则提醒
}

export interface SegmentAdvice {
  currentHeartRate: number;
  heartRateZone: 'warmup' | 'fat_burn' | 'aerobic' | 'anaerobic' | 'max';
  zoneLabel: string;
  advice: string;
  suggestedPace?: string; // 如 "5:30-6:00/km"
  shouldSlowDown: boolean;
}

export interface StretchingExercise {
  name: string;
  duration: number; // 秒
  description: string;
  targetMuscle: string;
}

export interface StretchingGuide {
  runDuration: number; // 本次跑步时长（秒）
  exercises: StretchingExercise[];
}

export interface InjuryPreventionTip {
  category: 'warmup' | 'cooldown' | 'overtraining' | 'footwear' | 'nutrition';
  title: string;
  content: string;
  priority: 'high' | 'medium' | 'low';
}
