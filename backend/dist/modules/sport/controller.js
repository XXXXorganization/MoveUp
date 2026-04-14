"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SportController = void 0;
const errors_1 = require("../../utils/errors");
class SportController {
    constructor(service) {
        this.service = service;
    }
    async startSportRecord(req, res, next) {
        try {
            const userId = req.user?.id; // Assuming auth middleware sets req.user
            if (!userId) {
                throw new errors_1.AppError('User not authenticated', 401);
            }
            const record = await this.service.startSportRecord(userId);
            res.status(201).json({
                code: 200,
                message: 'Sport record started successfully',
                data: record,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async updateSportRecord(req, res, next) {
        try {
            const { recordId } = req.params;
            const updateData = req.body;
            if (updateData.recordId !== recordId) {
                throw new errors_1.AppError('Record ID mismatch', 400);
            }
            const record = await this.service.updateSportRecord(recordId, updateData);
            res.json({
                code: 200,
                message: 'Sport record updated successfully',
                data: record,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async stopSportRecord(req, res, next) {
        try {
            const { recordId } = req.params;
            const record = await this.service.stopSportRecord(recordId);
            res.json({
                code: 200,
                message: 'Sport record stopped successfully',
                data: record,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async getSportRecord(req, res, next) {
        try {
            const { recordId } = req.params;
            const record = await this.service.getSportRecordById(recordId);
            res.json({
                code: 200,
                message: 'Sport record retrieved successfully',
                data: record,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async getSportRecords(req, res, next) {
        try {
            const userId = req.user?.id;
            if (!userId) {
                throw new errors_1.AppError('User not authenticated', 401);
            }
            const records = await this.service.getSportRecordsByUserId(userId);
            res.json({
                code: 200,
                message: 'Sport records retrieved successfully',
                data: records,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async getGpsPoints(req, res, next) {
        try {
            const { recordId } = req.params;
            const points = await this.service.getGpsPointsByRecordId(recordId);
            res.json({
                code: 200,
                message: 'GPS points retrieved successfully',
                data: points,
            });
        }
        catch (error) {
            next(error);
        }
    }
    async getHeartRates(req, res, next) {
        try {
            const { recordId } = req.params;
            const rates = await this.service.getHeartRatesByRecordId(recordId);
            res.json({
                code: 200,
                message: 'Heart rates retrieved successfully',
                data: rates,
            });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.SportController = SportController;
