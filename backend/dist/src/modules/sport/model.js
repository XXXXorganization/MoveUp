"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.HeartRateModel = exports.GpsPointModel = exports.SportRecordModel = void 0;
/**
 * 运动记录数据模型
 */
class SportRecordModel {
    constructor(db) {
        this.db = db;
        this.tableName = 'sport_records';
    }
    /**
     * 创建运动记录表
     */
    static createTable(knex) {
        return knex.schema.createTable('sport_records', (table) => {
            table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
            table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
            table.timestamp('start_time').notNullable().defaultTo(knex.fn.now());
            table.timestamp('end_time');
            table.decimal('distance', 10, 2).notNullable().defaultTo(0); // 米
            table.integer('duration').notNullable().defaultTo(0); // 秒
            table.decimal('calories', 8, 2).notNullable().defaultTo(0); // 卡路里
            table.enu('status', ['active', 'completed']).notNullable().defaultTo('active');
            table.decimal('average_pace', 6, 2); // 秒/公里
            table.integer('max_heart_rate'); // 最大心率
            table.decimal('average_heart_rate', 5, 2); // 平均心率
            table.timestamps(true, true);
        });
    }
    /**
     * 根据 ID 查找记录
     */
    async findById(id) {
        const record = await this.db(this.tableName).where({ id }).first();
        return record ? this.mapToModel(record) : null;
    }
    /**
     * 根据用户 ID 查找记录
     */
    async findByUserId(userId) {
        const records = await this.db(this.tableName)
            .where({ user_id: userId })
            .orderBy('start_time', 'desc');
        return records.map(r => this.mapToModel(r));
    }
    /**
     * 查找进行中的记录
     */
    async findActiveByUserId(userId) {
        const record = await this.db(this.tableName)
            .where({ user_id: userId, status: 'active' })
            .first();
        return record ? this.mapToModel(record) : null;
    }
    /**
     * 创建记录
     */
    async create(data) {
        const [created] = await this.db(this.tableName)
            .insert({
            user_id: data.userId,
            start_time: data.startTime,
            distance: data.distance,
            duration: data.duration,
            calories: data.calories,
            status: data.status,
        })
            .returning('*');
        return this.mapToModel(created);
    }
    /**
     * 更新记录
     */
    async update(id, data) {
        const updateData = {};
        if (data.endTime !== undefined)
            updateData.end_time = data.endTime;
        if (data.distance !== undefined)
            updateData.distance = data.distance;
        if (data.duration !== undefined)
            updateData.duration = data.duration;
        if (data.calories !== undefined)
            updateData.calories = data.calories;
        if (data.status !== undefined)
            updateData.status = data.status;
        if (data.averagePace !== undefined)
            updateData.average_pace = data.averagePace;
        if (data.maxHeartRate !== undefined)
            updateData.max_heart_rate = data.maxHeartRate;
        if (data.averageHeartRate !== undefined)
            updateData.average_heart_rate = data.averageHeartRate;
        const [updated] = await this.db(this.tableName)
            .where({ id })
            .update({ ...updateData, updated_at: new Date() })
            .returning('*');
        return updated ? this.mapToModel(updated) : null;
    }
    /**
     * 删除记录
     */
    async delete(id) {
        const deleted = await this.db(this.tableName).where({ id }).del();
        return deleted > 0;
    }
    /**
     * 数据库记录映射到模型
     */
    mapToModel(record) {
        return {
            id: record.id,
            userId: record.user_id,
            startTime: new Date(record.start_time),
            endTime: record.end_time ? new Date(record.end_time) : undefined,
            distance: parseFloat(record.distance),
            duration: record.duration,
            calories: parseFloat(record.calories),
            status: record.status,
            averagePace: record.average_pace ? parseFloat(record.average_pace) : undefined,
            maxHeartRate: record.max_heart_rate,
            averageHeartRate: record.average_heart_rate ? parseFloat(record.average_heart_rate) : undefined,
            created_at: record.created_at,
            updated_at: record.updated_at,
        };
    }
}
exports.SportRecordModel = SportRecordModel;
/**
 * GPS 轨迹点数据模型
 */
class GpsPointModel {
    constructor(db) {
        this.db = db;
        this.tableName = 'gps_points';
    }
    /**
     * 创建 GPS 轨迹点表
     */
    static createTable(knex) {
        return knex.schema.createTable('gps_points', (table) => {
            table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
            table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
            table.decimal('latitude', 10, 8).notNullable();
            table.decimal('longitude', 11, 8).notNullable();
            table.timestamp('timestamp').notNullable();
            table.decimal('speed', 5, 2); // m/s
            table.decimal('altitude', 7, 1); // 米
            table.decimal('accuracy', 6, 2); // 米
            table.timestamps(true, true);
        });
    }
    /**
     * 根据记录 ID 查找轨迹点
     */
    async findByRecordId(recordId) {
        const points = await this.db(this.tableName)
            .where({ record_id: recordId })
            .orderBy('timestamp', 'asc');
        return points.map(p => this.mapToModel(p));
    }
    /**
     * 批量插入轨迹点
     */
    async insertMany(points) {
        const data = points.map(p => ({
            record_id: p.recordId,
            latitude: p.latitude,
            longitude: p.longitude,
            timestamp: p.timestamp,
            speed: p.speed,
            altitude: p.altitude,
            accuracy: p.accuracy,
        }));
        const inserted = await this.db(this.tableName).insert(data).returning('*');
        return inserted.map(p => this.mapToModel(p));
    }
    /**
     * 插入单个轨迹点
     */
    async insert(point) {
        const [inserted] = await this.db(this.tableName)
            .insert({
            record_id: point.recordId,
            latitude: point.latitude,
            longitude: point.longitude,
            timestamp: point.timestamp,
            speed: point.speed,
            altitude: point.altitude,
            accuracy: point.accuracy,
        })
            .returning('*');
        return this.mapToModel(inserted);
    }
    /**
     * 删除记录的所有轨迹点
     */
    async deleteByRecordId(recordId) {
        return this.db(this.tableName).where({ record_id: recordId }).del();
    }
    /**
     * 数据库记录映射到模型
     */
    mapToModel(point) {
        return {
            id: point.id,
            recordId: point.record_id,
            latitude: parseFloat(point.latitude),
            longitude: parseFloat(point.longitude),
            timestamp: new Date(point.timestamp),
            speed: point.speed ? parseFloat(point.speed) : undefined,
            altitude: point.altitude ? parseFloat(point.altitude) : undefined,
            accuracy: point.accuracy ? parseFloat(point.accuracy) : undefined,
        };
    }
}
exports.GpsPointModel = GpsPointModel;
/**
 * 心率数据模型
 */
class HeartRateModel {
    constructor(db) {
        this.db = db;
        this.tableName = 'heart_rates';
    }
    /**
     * 创建心率数据表
     */
    static createTable(knex) {
        return knex.schema.createTable('heart_rates', (table) => {
            table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
            table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
            table.timestamp('timestamp').notNullable();
            table.integer('heart_rate').notNullable(); // bpm
            table.timestamps(true, true);
        });
    }
    /**
     * 根据记录 ID 查找心率数据
     */
    async findByRecordId(recordId) {
        const rates = await this.db(this.tableName)
            .where({ record_id: recordId })
            .orderBy('timestamp', 'asc');
        return rates.map(r => this.mapToModel(r));
    }
    /**
     * 批量插入心率数据
     */
    async insertMany(rates) {
        const data = rates.map(r => ({
            record_id: r.recordId,
            timestamp: r.timestamp,
            heart_rate: r.heartRate,
        }));
        const inserted = await this.db(this.tableName).insert(data).returning('*');
        return inserted.map(r => this.mapToModel(r));
    }
    /**
     * 插入单个心率数据
     */
    async insert(rate) {
        const [inserted] = await this.db(this.tableName)
            .insert({
            record_id: rate.recordId,
            timestamp: rate.timestamp,
            heart_rate: rate.heartRate,
        })
            .returning('*');
        return this.mapToModel(inserted);
    }
    /**
     * 删除记录的所有心率数据
     */
    async deleteByRecordId(recordId) {
        return this.db(this.tableName).where({ record_id: recordId }).del();
    }
    /**
     * 数据库记录映射到模型
     */
    mapToModel(rate) {
        return {
            id: rate.id,
            recordId: rate.record_id,
            timestamp: new Date(rate.timestamp),
            heartRate: rate.heart_rate,
        };
    }
}
exports.HeartRateModel = HeartRateModel;
