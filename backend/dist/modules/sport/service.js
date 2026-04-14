"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SportService = void 0;
const errors_1 = require("../../utils/errors");
class SportService {
    constructor(repository) {
        this.repository = repository;
    }
    async startSportRecord(userId) {
        // Check if user has an active record
        const activeRecord = await this.repository.getActiveSportRecordByUserId(userId);
        if (activeRecord) {
            throw new errors_1.AppError('User already has an active sport record', 400);
        }
        const record = {
            userId,
            startTime: new Date(),
            distance: 0,
            duration: 0,
            calories: 0,
            status: 'active',
        };
        return this.repository.createSportRecord(record);
    }
    async updateSportRecord(recordId, data) {
        const record = await this.repository.getSportRecordById(recordId);
        if (!record) {
            throw new errors_1.AppError('Sport record not found', 404);
        }
        if (record.status !== 'active') {
            throw new errors_1.AppError('Sport record is not active', 400);
        }
        // Insert GPS points if provided
        if (data.gpsPoints && data.gpsPoints.length > 0) {
            await this.repository.insertGpsPoints(data.gpsPoints);
        }
        // Insert heart rates if provided
        if (data.heartRates && data.heartRates.length > 0) {
            await this.repository.insertHeartRates(data.heartRates);
        }
        // Recalculate metrics
        const updatedRecord = await this.calculateMetrics(recordId);
        return updatedRecord;
    }
    async stopSportRecord(recordId) {
        const record = await this.repository.getSportRecordById(recordId);
        if (!record) {
            throw new errors_1.AppError('Sport record not found', 404);
        }
        if (record.status !== 'active') {
            throw new errors_1.AppError('Sport record is not active', 400);
        }
        const endTime = new Date();
        const duration = Math.floor((endTime.getTime() - record.startTime.getTime()) / 1000);
        // Final calculation
        const finalRecord = await this.calculateMetrics(recordId);
        const updates = {
            endTime,
            duration,
            status: 'completed',
            distance: finalRecord.distance,
            calories: finalRecord.calories,
            averagePace: finalRecord.averagePace,
            maxHeartRate: finalRecord.maxHeartRate,
            averageHeartRate: finalRecord.averageHeartRate,
        };
        return this.repository.updateSportRecord(recordId, updates);
    }
    async calculateMetrics(recordId) {
        const record = await this.repository.getSportRecordById(recordId);
        if (!record) {
            throw new errors_1.AppError('Sport record not found', 404);
        }
        const gpsPoints = await this.repository.getGpsPointsByRecordId(recordId);
        const heartRates = await this.repository.getHeartRatesByRecordId(recordId);
        // Calculate distance from GPS points
        const distance = this.calculateDistance(gpsPoints);
        // Calculate average pace (seconds per km)
        const duration = record.duration || Math.floor((new Date().getTime() - record.startTime.getTime()) / 1000);
        const averagePace = duration > 0 && distance > 0 ? (duration / (distance / 1000)) : undefined;
        // Calculate heart rate stats
        const maxHeartRate = heartRates.length > 0 ? Math.max(...heartRates.map(hr => hr.heartRate)) : undefined;
        const averageHeartRate = heartRates.length > 0 ? heartRates.reduce((sum, hr) => sum + hr.heartRate, 0) / heartRates.length : undefined;
        // Calculate calories (placeholder - would need user weight, etc.)
        const calories = 0; // TODO: implement calorie calculation
        const updates = {
            distance,
            averagePace,
            maxHeartRate,
            averageHeartRate,
            calories,
        };
        return this.repository.updateSportRecord(recordId, updates);
    }
    calculateDistance(points) {
        if (points.length < 2)
            return 0;
        let totalDistance = 0;
        for (let i = 1; i < points.length; i++) {
            const prev = points[i - 1];
            const curr = points[i];
            totalDistance += this.haversineDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude);
        }
        return totalDistance;
    }
    haversineDistance(lat1, lon1, lat2, lon2) {
        const R = 6371e3; // Earth's radius in meters
        const φ1 = (lat1 * Math.PI) / 180;
        const φ2 = (lat2 * Math.PI) / 180;
        const Δφ = ((lat2 - lat1) * Math.PI) / 180;
        const Δλ = ((lon2 - lon1) * Math.PI) / 180;
        const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    // Placeholder for calorie calculation - would need user data
    calculateCalories(params) {
        // Simplified formula: MET * weight * time (hours)
        // For running, MET ≈ 8.3 for moderate pace
        const met = 8.3;
        const hours = params.duration / 3600;
        return met * params.weight * hours;
    }
    async getSportRecordById(recordId) {
        const record = await this.repository.getSportRecordById(recordId);
        if (!record) {
            throw new errors_1.AppError('Sport record not found', 404);
        }
        return record;
    }
    async getSportRecordsByUserId(userId) {
        return this.repository.getSportRecordsByUserId(userId);
    }
    async getGpsPointsByRecordId(recordId) {
        // Check if record exists and user has access
        await this.getSportRecordById(recordId);
        return this.repository.getGpsPointsByRecordId(recordId);
    }
    async getHeartRatesByRecordId(recordId) {
        // Check if record exists and user has access
        await this.getSportRecordById(recordId);
        return this.repository.getHeartRatesByRecordId(recordId);
    }
}
exports.SportService = SportService;
