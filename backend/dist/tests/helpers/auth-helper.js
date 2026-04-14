"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.createTestUserAndGetToken = createTestUserAndGetToken;
exports.cleanupTestUser = cleanupTestUser;
// tests/helpers/auth-helper.ts
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const database_1 = require("../../src/config/database");
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
const JWT_EXPIRES_IN = parseInt(process.env.JWT_EXPIRES_IN || '7200');
/**
 * 生成 UUID v4
 */
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}
/**
 * 测试辅助函数：创建测试用户并返回 token
 */
async function createTestUserAndGetToken(phone = '13800138001') {
    // 查找或创建用户
    let user = await (0, database_1.db)('users').where({ phone }).first();
    if (!user) {
        // 创建测试用户
        const userId = generateUUID();
        await (0, database_1.db)('users').insert({
            id: userId,
            phone,
            nickname: `测试用户${phone.slice(-4)}`,
            created_at: new Date(),
            updated_at: new Date(),
        });
        user = await (0, database_1.db)('users').where({ phone }).first();
    }
    // 生成 JWT token
    const token = jsonwebtoken_1.default.sign({ userId: user.id, phone: user.phone }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
    return token;
}
/**
 * 测试辅助函数：清理测试数据
 */
async function cleanupTestUser(phone) {
    await (0, database_1.db)('users').where({ phone }).del();
}
