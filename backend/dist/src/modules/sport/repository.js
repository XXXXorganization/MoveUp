"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SportRepository = void 0;
const model_1 = require("./model");
/**
 * 运动数据仓储类
 * 封装数据库操作，提供给 Service 层使用
 */
class SportRepository {
    constructor(db) {
        this.db = db;
        this.sportRecordModel = new model_1.SportRecordModel(db);
        this.gpsPointModel = new model_1.GpsPointModel(db);
        this.heartRateModel = new model_1.HeartRateModel(db);
    }
    // ==================== 运动记录 ====================
    /**
     * 创建运动记录
     */
    async createSportRecord(record) {
        return this.sportRecordModel.create(record);
    }
    /**
     * 根据 ID 获取运动记录
     */
    async getSportRecordById(id) {
        return this.sportRecordModel.findById(id);
    }
    /**
     * 根据用户 ID 获取运动记录列表
     */
    async getSportRecordsByUserId(userId) {
        return this.sportRecordModel.findByUserId(userId);
    }
    /**
     * 获取用户进行中的运动记录
     */
    async getActiveSportRecordByUserId(userId) {
        return this.sportRecordModel.findActiveByUserId(userId);
    }
    /**
     * 更新运动记录
     */
    async updateSportRecord(id, updates) {
        return this.sportRecordModel.update(id, updates);
    }
    /**
     * 删除运动记录
     */
    async deleteSportRecord(id) {
        return this.sportRecordModel.delete(id);
    }
    // ==================== GPS 轨迹点 ====================
    /**
     * 根据记录 ID 获取 GPS 轨迹点
     */
    async getGpsPointsByRecordId(recordId) {
        return this.gpsPointModel.findByRecordId(recordId);
    }
    /**
     * 批量插入 GPS 轨迹点
     */
    async insertGpsPoints(points) {
        return this.gpsPointModel.insertMany(points);
    }
    /**
     * 删除记录的所有 GPS 轨迹点
     */
    async deleteGpsPointsByRecordId(recordId) {
        return this.gpsPointModel.deleteByRecordId(recordId);
    }
    // ==================== 心率数据 ====================
    /**
     * 根据记录 ID 获取心率数据
     */
    async getHeartRatesByRecordId(recordId) {
        return this.heartRateModel.findByRecordId(recordId);
    }
    /**
     * 批量插入心率数据
     */
    async insertHeartRates(rates) {
        return this.heartRateModel.insertMany(rates);
    }
    /**
     * 删除记录的所有心率数据
     */
    async deleteHeartRatesByRecordId(recordId) {
        return this.heartRateModel.deleteByRecordId(recordId);
    }
}
exports.SportRepository = SportRepository;
