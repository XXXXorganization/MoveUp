// src/modules/coaching/repository.ts
import { Knex } from 'knex';
import { TrainingPlanModel, UserPlanModel } from './model';
import { TrainingPlan, UserPlan } from './types';

export class CoachingRepository {
  private trainingPlanModel: TrainingPlanModel;
  private userPlanModel: UserPlanModel;

  constructor(private db: Knex) {
    this.trainingPlanModel = new TrainingPlanModel(db);
    this.userPlanModel = new UserPlanModel(db);
  }

  // ==================== 训练计划 ====================

  async getAllTrainingPlans(): Promise<TrainingPlan[]> {
    return this.trainingPlanModel.findAll();
  }

  async getTrainingPlanById(id: string): Promise<TrainingPlan | null> {
    return this.trainingPlanModel.findById(id);
  }

  async getTrainingPlansByDifficulty(difficulty: string): Promise<TrainingPlan[]> {
    return this.trainingPlanModel.findByDifficulty(difficulty);
  }

  // ==================== 用户计划 ====================

  async getActivePlanByUserId(userId: string): Promise<UserPlan | null> {
    return this.userPlanModel.findActiveByUserId(userId);
  }

  async getPlansByUserId(userId: string): Promise<UserPlan[]> {
    return this.userPlanModel.findByUserId(userId);
  }

  async getUserPlanById(id: string): Promise<UserPlan | null> {
    return this.userPlanModel.findById(id);
  }

  async createUserPlan(data: Omit<UserPlan, 'id'>): Promise<UserPlan> {
    return this.userPlanModel.create(data);
  }

  async updateUserPlan(id: string, data: Partial<UserPlan>): Promise<UserPlan | null> {
    return this.userPlanModel.update(id, data);
  }

  async deactivateAllUserPlans(userId: string): Promise<void> {
    return this.userPlanModel.deactivateAllByUserId(userId);
  }
}
