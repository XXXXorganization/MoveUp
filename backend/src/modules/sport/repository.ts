// src/modules/sport/repository.ts
import { Knex } from 'knex';
import { SportRecordModel, GpsPointModel, HeartRateModel } from './model';
import { SportRecord, GpsPoint, HeartRateData } from './types';

/**
 * 运动数据仓储类
 * 封装数据库操作，提供给 Service 层使用
 */
export class SportRepository {
  private sportRecordModel: SportRecordModel;
  private gpsPointModel: GpsPointModel;
  private heartRateModel: HeartRateModel;

  constructor(private db: Knex) {
    this.sportRecordModel = new SportRecordModel(db);
    this.gpsPointModel = new GpsPointModel(db);
    this.heartRateModel = new HeartRateModel(db);
  }

  // ==================== 运动记录 ====================

  /**
   * 创建运动记录
   */
  async createSportRecord(record: Omit<SportRecord, 'id' | 'created_at' | 'updated_at'>): Promise<SportRecord> {
    return this.sportRecordModel.create(record);
  }

  /**
   * 根据 ID 获取运动记录
   */
  async getSportRecordById(id: string): Promise<SportRecord | null> {
    return this.sportRecordModel.findById(id);
  }

  /**
   * 根据用户 ID 获取运动记录列表
   */
  async getSportRecordsByUserId(userId: string): Promise<SportRecord[]> {
    return this.sportRecordModel.findByUserId(userId);
  }

  /**
   * 获取用户进行中的运动记录
   */
  async getActiveSportRecordByUserId(userId: string): Promise<SportRecord | null> {
    return this.sportRecordModel.findActiveByUserId(userId);
  }

  /**
   * 更新运动记录
   */
  async updateSportRecord(id: string, updates: Partial<SportRecord>): Promise<SportRecord | null> {
    return this.sportRecordModel.update(id, updates);
  }

  /**
   * 删除运动记录
   */
  async deleteSportRecord(id: string): Promise<boolean> {
    return this.sportRecordModel.delete(id);
  }

  // ==================== GPS 轨迹点 ====================

  /**
   * 根据记录 ID 获取 GPS 轨迹点
   */
  async getGpsPointsByRecordId(recordId: string): Promise<GpsPoint[]> {
    return this.gpsPointModel.findByRecordId(recordId);
  }

  /**
   * 批量插入 GPS 轨迹点
   */
  async insertGpsPoints(points: Omit<GpsPoint, 'id'>[]): Promise<GpsPoint[]> {
    return this.gpsPointModel.insertMany(points);
  }

  /**
   * 删除记录的所有 GPS 轨迹点
   */
  async deleteGpsPointsByRecordId(recordId: string): Promise<number> {
    return this.gpsPointModel.deleteByRecordId(recordId);
  }

  // ==================== 心率数据 ====================

  /**
   * 根据记录 ID 获取心率数据
   */
  async getHeartRatesByRecordId(recordId: string): Promise<HeartRateData[]> {
    return this.heartRateModel.findByRecordId(recordId);
  }

  /**
   * 批量插入心率数据
   */
  async insertHeartRates(rates: Omit<HeartRateData, 'id'>[]): Promise<HeartRateData[]> {
    return this.heartRateModel.insertMany(rates);
  }

  /**
   * 删除记录的所有心率数据
   */
  async deleteHeartRatesByRecordId(recordId: string): Promise<number> {
    return this.heartRateModel.deleteByRecordId(recordId);
  }
}
