// src/modules/coaching/model.ts
import { Knex } from 'knex';
import { TrainingPlan, UserPlan } from './types';

export class TrainingPlanModel {
  private tableName = 'training_plans';

  constructor(private db: Knex) {}

  async findAll(): Promise<TrainingPlan[]> {
    const rows = await this.db(this.tableName).orderBy('created_at', 'desc');
    return rows.map(r => this.mapToModel(r));
  }

  async findById(id: string): Promise<TrainingPlan | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.mapToModel(row) : null;
  }

  async findByDifficulty(difficulty: string): Promise<TrainingPlan[]> {
    const rows = await this.db(this.tableName).where({ difficulty });
    return rows.map(r => this.mapToModel(r));
  }

  private mapToModel(row: any): TrainingPlan {
    return {
      id: row.id,
      name: row.name,
      description: row.description,
      difficulty: row.difficulty,
      durationWeeks: row.duration_weeks,
      targetDistance: row.target_distance,
      schedule: typeof row.schedule === 'string' ? JSON.parse(row.schedule) : (row.schedule ?? []),
      created_at: row.created_at,
    };
  }
}

export class UserPlanModel {
  private tableName = 'user_plans';

  constructor(private db: Knex) {}

  async findActiveByUserId(userId: string): Promise<UserPlan | null> {
    const row = await this.db(this.tableName).where({ user_id: userId, is_active: true }).first();
    return row ? this.mapToModel(row) : null;
  }

  async findByUserId(userId: string): Promise<UserPlan[]> {
    const rows = await this.db(this.tableName).where({ user_id: userId }).orderBy('started_at', 'desc');
    return rows.map(r => this.mapToModel(r));
  }

  async findById(id: string): Promise<UserPlan | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.mapToModel(row) : null;
  }

  async create(data: Omit<UserPlan, 'id'>): Promise<UserPlan> {
    const [created] = await this.db(this.tableName)
      .insert({
        user_id: data.userId,
        plan_id: data.planId,
        current_week: data.currentWeek,
        current_day: data.currentDay,
        is_active: data.isActive,
        started_at: data.startedAt,
      })
      .returning('*');
    return this.mapToModel(created);
  }

  async update(id: string, data: Partial<UserPlan>): Promise<UserPlan | null> {
    const updateData: any = {};
    if (data.currentWeek !== undefined) updateData.current_week = data.currentWeek;
    if (data.currentDay !== undefined) updateData.current_day = data.currentDay;
    if (data.isActive !== undefined) updateData.is_active = data.isActive;
    if (data.completedAt !== undefined) updateData.completed_at = data.completedAt;

    const [updated] = await this.db(this.tableName).where({ id }).update(updateData).returning('*');
    return updated ? this.mapToModel(updated) : null;
  }

  async deactivateAllByUserId(userId: string): Promise<void> {
    await this.db(this.tableName).where({ user_id: userId, is_active: true }).update({ is_active: false });
  }

  private mapToModel(row: any): UserPlan {
    return {
      id: row.id,
      userId: row.user_id,
      planId: row.plan_id,
      currentWeek: row.current_week,
      currentDay: row.current_day,
      isActive: row.is_active,
      startedAt: new Date(row.started_at),
      completedAt: row.completed_at ? new Date(row.completed_at) : undefined,
    };
  }
}
