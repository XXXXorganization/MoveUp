// src/modules/user/model.ts
import knex from 'knex';
import { User } from './types';

export class UserModel {
  private db: any;

  constructor(db: any) {
    this.db = db;
  }

  async findById(id: string): Promise<User | undefined> {
    return this.db('users').where({ id }).first();
  }

  async findByPhone(phone: string): Promise<User | undefined> {
    return this.db('users').where({ phone }).first();
  }

  async create(userData: Omit<User, 'id' | 'created_at' | 'updated_at'>): Promise<User> {
    const [newUser] = await this.db('users').insert(userData).returning('*');
    return newUser;
  }

  async update(id: string, userData: Partial<Omit<User, 'id' | 'created_at' | 'updated_at'>>): Promise<User | undefined> {
    const [updatedUser] = await this.db('users')
      .where({ id })
      .update({ ...userData, updated_at: this.db.fn.now() })
      .returning('*');
    return updatedUser;
  }

  async getUserStats(userId: string): Promise<{ total_distance: number; total_time: number; total_runs: number }> {
    const result = await this.db('sport_records')
      .where({ user_id: userId })
      .select(
        this.db.raw('COALESCE(SUM(distance), 0) as total_distance'),
        this.db.raw('COALESCE(SUM(duration), 0) as total_time'),
        this.db.raw('COUNT(*) as total_runs')
      )
      .first();

    return {
      total_distance: parseFloat(result.total_distance) / 1000, // 转换为公里
      total_time: Math.floor(parseInt(result.total_time) / 60), // 转换为分钟
      total_runs: parseInt(result.total_runs),
    };
  }
}