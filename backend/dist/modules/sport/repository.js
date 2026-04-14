"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SportRepository = void 0;
class SportRepository {
    constructor(db) {
        this.db = db;
    }
    async createSportRecord(record) {
        const [created] = await this.db('sport_records').insert(record).returning('*');
        return created;
    }
    async getSportRecordById(id) {
        return this.db('sport_records').where({ id }).first();
    }
    async getSportRecordsByUserId(userId) {
        return this.db('sport_records').where({ user_id: userId }).orderBy('start_time', 'desc');
    }
    async updateSportRecord(id, updates) {
        const [updated] = await this.db('sport_records').where({ id }).update(updates).returning('*');
        return updated || null;
    }
    async deleteSportRecord(id) {
        const deleted = await this.db('sport_records').where({ id }).del();
        return deleted > 0;
    }
    async insertGpsPoints(points) {
        return this.db('gps_points').insert(points).returning('*');
    }
    async getGpsPointsByRecordId(recordId) {
        return this.db('gps_points').where({ record_id: recordId }).orderBy('timestamp');
    }
    async insertHeartRates(rates) {
        return this.db('heart_rates').insert(rates).returning('*');
    }
    async getHeartRatesByRecordId(recordId) {
        return this.db('heart_rates').where({ record_id: recordId }).orderBy('timestamp');
    }
    async getActiveSportRecordByUserId(userId) {
        return this.db('sport_records').where({ user_id: userId, status: 'active' }).first();
    }
}
exports.SportRepository = SportRepository;
