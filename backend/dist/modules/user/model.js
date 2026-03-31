"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.UserModel = void 0;
class UserModel {
    constructor(db) {
        this.db = db;
    }
    async findById(id) {
        return this.db('users').where({ id }).first();
    }
    async findByPhone(phone) {
        return this.db('users').where({ phone }).first();
    }
    async create(userData) {
        const [newUser] = await this.db('users').insert(userData).returning('*');
        return newUser;
    }
    async update(id, userData) {
        const [updatedUser] = await this.db('users')
            .where({ id })
            .update({ ...userData, updated_at: this.db.fn.now() })
            .returning('*');
        return updatedUser;
    }
    async getUserStats(userId) {
        const result = await this.db('sport_records')
            .where({ user_id: userId })
            .select(this.db.raw('COALESCE(SUM(distance), 0) as total_distance'), this.db.raw('COALESCE(SUM(duration), 0) as total_time'), this.db.raw('COUNT(*) as total_runs'))
            .first();
        return {
            total_distance: parseFloat(result.total_distance) / 1000, // 转换为公里
            total_time: Math.floor(parseInt(result.total_time) / 60), // 转换为分钟
            total_runs: parseInt(result.total_runs),
        };
    }
}
exports.UserModel = UserModel;
