"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.UserRepository = void 0;
// src/modules/user/repository.ts
const model_1 = require("./model");
class UserRepository {
    constructor(db) {
        this.userModel = new model_1.UserModel(db);
    }
    async findById(id) {
        return this.userModel.findById(id);
    }
    async findByPhone(phone) {
        return this.userModel.findByPhone(phone);
    }
    async create(userData) {
        const defaultUserData = {
            ...userData,
            role: 'user',
        };
        return this.userModel.create(defaultUserData);
    }
    async update(id, userData) {
        return this.userModel.update(id, userData);
    }
    async getUserStats(userId) {
        return this.userModel.getUserStats(userId);
    }
}
exports.UserRepository = UserRepository;
