// src/modules/challenge/model.ts
import { Knex } from 'knex';
import { Badge, Task, UserTask, UserAchievement, Challenge, UserChallenge, MembershipPlan, UserMembership, PointsLog } from './types';

export class BadgeModel {
  private tableName = 'badges';
  constructor(private db: Knex) {}

  async findAll(): Promise<Badge[]> {
    const rows = await this.db(this.tableName).orderBy('created_at', 'desc');
    return rows.map(r => this.map(r));
  }

  async findById(id: string): Promise<Badge | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.map(row) : null;
  }

  async findByIds(ids: string[]): Promise<Badge[]> {
    const rows = await this.db(this.tableName).whereIn('id', ids);
    return rows.map(r => this.map(r));
  }

  private map(r: any): Badge {
    return {
      id: r.id, name: r.name, description: r.description, icon: r.icon,
      conditionType: r.condition_type, conditionValue: r.condition_value,
      rarity: r.rarity, createdAt: new Date(r.created_at),
    };
  }
}

export class TaskModel {
  private tableName = 'tasks';
  constructor(private db: Knex) {}

  async findActive(taskType?: string): Promise<Task[]> {
    const q = this.db(this.tableName).where({ is_active: true });
    if (taskType) q.andWhere({ task_type: taskType });
    return (await q).map(r => this.map(r));
  }

  async findById(id: string): Promise<Task | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.map(row) : null;
  }

  private map(r: any): Task {
    return {
      id: r.id, name: r.name, description: r.description,
      taskType: r.task_type, rewardPoints: r.reward_points,
      rewardBadgeId: r.reward_badge_id, isActive: r.is_active,
      requirement: typeof r.requirement === 'string' ? JSON.parse(r.requirement) : (r.requirement ?? {}),
      createdAt: new Date(r.created_at),
    };
  }
}

export class UserTaskModel {
  private tableName = 'user_tasks';
  constructor(private db: Knex) {}

  async findByUserId(userId: string): Promise<UserTask[]> {
    const rows = await this.db(this.tableName).where({ user_id: userId }).orderBy('created_at', 'desc');
    return rows.map(r => this.map(r));
  }

  async findOne(userId: string, taskId: string): Promise<UserTask | null> {
    const row = await this.db(this.tableName).where({ user_id: userId, task_id: taskId }).first();
    return row ? this.map(row) : null;
  }

  async upsert(userId: string, taskId: string, progress: number, isCompleted: boolean): Promise<UserTask> {
    const existing = await this.findOne(userId, taskId);
    if (existing) {
      const [updated] = await this.db(this.tableName)
        .where({ user_id: userId, task_id: taskId })
        .update({ progress, is_completed: isCompleted, completed_at: isCompleted ? new Date() : null })
        .returning('*');
      return this.map(updated);
    }
    const [created] = await this.db(this.tableName)
      .insert({ user_id: userId, task_id: taskId, progress, is_completed: isCompleted, completed_at: isCompleted ? new Date() : null })
      .returning('*');
    return this.map(created);
  }

  private map(r: any): UserTask {
    return {
      id: r.id, userId: r.user_id, taskId: r.task_id,
      progress: r.progress, isCompleted: r.is_completed,
      completedAt: r.completed_at ? new Date(r.completed_at) : undefined,
      createdAt: new Date(r.created_at),
    };
  }
}

export class UserAchievementModel {
  private tableName = 'user_achievements';
  constructor(private db: Knex) {}

  async findByUserId(userId: string): Promise<UserAchievement[]> {
    const rows = await this.db(this.tableName).where({ user_id: userId }).orderBy('achieved_at', 'desc');
    return rows.map(r => this.map(r));
  }

  async findOne(userId: string, badgeId: string): Promise<UserAchievement | null> {
    const row = await this.db(this.tableName).where({ user_id: userId, badge_id: badgeId }).first();
    return row ? this.map(row) : null;
  }

  async create(userId: string, badgeId: string): Promise<UserAchievement> {
    const [created] = await this.db(this.tableName)
      .insert({ user_id: userId, badge_id: badgeId })
      .returning('*');
    return this.map(created);
  }

  private map(r: any): UserAchievement {
    return { id: r.id, userId: r.user_id, badgeId: r.badge_id, achievedAt: new Date(r.achieved_at) };
  }
}

export class ChallengeModel {
  private tableName = 'challenges';
  constructor(private db: Knex) {}

  async findByStatus(status: 'ongoing' | 'upcoming' | 'completed'): Promise<Challenge[]> {
    const now = new Date();
    const q = this.db(this.tableName);
    if (status === 'ongoing') q.where('start_time', '<=', now).andWhere('end_time', '>=', now);
    else if (status === 'upcoming') q.where('start_time', '>', now);
    else q.where('end_time', '<', now);
    return (await q.orderBy('start_time', 'desc')).map(r => this.map(r));
  }

  async findById(id: string): Promise<Challenge | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.map(row) : null;
  }

  private map(r: any): Challenge {
    return {
      id: r.id, name: r.name, description: r.description,
      challengeType: r.challenge_type, targetValue: r.target_value,
      rewardPoints: r.reward_points, rewardBadgeId: r.reward_badge_id,
      startTime: new Date(r.start_time), endTime: new Date(r.end_time),
      createdAt: new Date(r.created_at),
    };
  }
}

export class UserChallengeModel {
  private tableName = 'user_challenges';
  constructor(private db: Knex) {}

  async findByUserId(userId: string): Promise<UserChallenge[]> {
    const rows = await this.db(this.tableName).where({ user_id: userId }).orderBy('joined_at', 'desc');
    return rows.map(r => this.map(r));
  }

  async findOne(userId: string, challengeId: string): Promise<UserChallenge | null> {
    const row = await this.db(this.tableName).where({ user_id: userId, challenge_id: challengeId }).first();
    return row ? this.map(row) : null;
  }

  async create(userId: string, challengeId: string): Promise<UserChallenge> {
    const [created] = await this.db(this.tableName)
      .insert({ user_id: userId, challenge_id: challengeId })
      .returning('*');
    return this.map(created);
  }

  async updateProgress(userId: string, challengeId: string, progress: number, isCompleted: boolean): Promise<UserChallenge | null> {
    const [updated] = await this.db(this.tableName)
      .where({ user_id: userId, challenge_id: challengeId })
      .update({ progress, is_completed: isCompleted, completed_at: isCompleted ? new Date() : null })
      .returning('*');
    return updated ? this.map(updated) : null;
  }

  async getRanking(challengeId: string): Promise<{ userId: string; progress: number; rank: number }[]> {
    const rows = await this.db(this.tableName)
      .where({ challenge_id: challengeId })
      .orderBy('progress', 'desc')
      .select('user_id', 'progress');
    return rows.map((r: any, i: number) => ({ userId: r.user_id, progress: r.progress, rank: i + 1 }));
  }

  private map(r: any): UserChallenge {
    return {
      id: r.id, userId: r.user_id, challengeId: r.challenge_id,
      progress: r.progress, rank: r.rank, isCompleted: r.is_completed,
      joinedAt: new Date(r.joined_at),
      completedAt: r.completed_at ? new Date(r.completed_at) : undefined,
    };
  }
}

export class MembershipPlanModel {
  private tableName = 'membership_plans';
  constructor(private db: Knex) {}

  async findActive(): Promise<MembershipPlan[]> {
    const rows = await this.db(this.tableName).where({ is_active: true });
    return rows.map(r => this.map(r));
  }

  async findById(id: string): Promise<MembershipPlan | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.map(row) : null;
  }

  private map(r: any): MembershipPlan {
    return {
      id: r.id, name: r.name, durationDays: r.duration_days, price: parseFloat(r.price),
      features: typeof r.features === 'string' ? JSON.parse(r.features) : (r.features ?? []),
      isActive: r.is_active, createdAt: new Date(r.created_at),
    };
  }
}

export class UserMembershipModel {
  private tableName = 'user_memberships';
  constructor(private db: Knex) {}

  async findActiveByUserId(userId: string): Promise<UserMembership | null> {
    const row = await this.db(this.tableName)
      .where({ user_id: userId, is_active: true })
      .andWhere('end_date', '>', new Date())
      .orderBy('end_date', 'desc')
      .first();
    return row ? this.map(row) : null;
  }

  async findByUserId(userId: string): Promise<UserMembership[]> {
    const rows = await this.db(this.tableName).where({ user_id: userId }).orderBy('created_at', 'desc');
    return rows.map(r => this.map(r));
  }

  async create(userId: string, planId: string, startDate: Date, endDate: Date): Promise<UserMembership> {
    const [created] = await this.db(this.tableName)
      .insert({ user_id: userId, plan_id: planId, start_date: startDate, end_date: endDate })
      .returning('*');
    return this.map(created);
  }

  private map(r: any): UserMembership {
    return {
      id: r.id, userId: r.user_id, planId: r.plan_id,
      startDate: new Date(r.start_date), endDate: new Date(r.end_date),
      isActive: r.is_active, createdAt: new Date(r.created_at),
    };
  }
}

export class PointsLogModel {
  private tableName = 'points_logs';
  constructor(private db: Knex) {}

  async findByUserId(userId: string, limit = 20, offset = 0): Promise<PointsLog[]> {
    const rows = await this.db(this.tableName)
      .where({ user_id: userId })
      .orderBy('created_at', 'desc')
      .limit(limit).offset(offset);
    return rows.map(r => this.map(r));
  }

  async getTotalPoints(userId: string): Promise<number> {
    const result = await this.db(this.tableName)
      .where({ user_id: userId })
      .sum('points_change as total')
      .first();
    return parseInt(result?.total ?? '0') || 0;
  }

  async create(userId: string, pointsChange: number, sourceType: string, sourceId?: string, description?: string): Promise<PointsLog> {
    const [created] = await this.db(this.tableName)
      .insert({ user_id: userId, points_change: pointsChange, source_type: sourceType, source_id: sourceId, description })
      .returning('*');
    return this.map(created);
  }

  private map(r: any): PointsLog {
    return {
      id: r.id, userId: r.user_id, pointsChange: r.points_change,
      sourceType: r.source_type, sourceId: r.source_id,
      description: r.description, createdAt: new Date(r.created_at),
    };
  }
}
