"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.UserService = void 0;
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const errors_1 = require("../../utils/errors");
class UserService {
    constructor(userRepository, jwtSecret, jwtExpiresIn) {
        // 模拟验证码存储（生产环境应使用Redis）
        this.verificationCodes = new Map();
        this.userRepository = userRepository;
        this.jwtSecret = jwtSecret;
        this.jwtExpiresIn = jwtExpiresIn;
    }
    async sendVerificationCode(request) {
        const { phone, type } = request;
        // 生成6位验证码
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        const expires = Date.now() + 5 * 60 * 1000; // 5分钟过期
        // 存储验证码（简化版）
        this.verificationCodes.set(`${phone}_${type}`, { code, expires });
        // TODO: 发送短信验证码
        console.log(`Verification code for ${phone} (${type}): ${code}`);
    }
    async login(request) {
        const { phone, code } = request;
        // 验证验证码
        const storedCode = this.verificationCodes.get(`${phone}_login`);
        if (!storedCode || storedCode.code !== code || storedCode.expires < Date.now()) {
            throw new errors_1.AppError('验证码无效或已过期', 400);
        }
        // 清除已使用的验证码
        this.verificationCodes.delete(`${phone}_login`);
        // 查找用户，如果不存在则创建
        let user = await this.userRepository.findByPhone(phone);
        if (!user) {
            // 自动注册新用户
            const createData = {
                phone,
                nickname: `用户${phone.slice(-4)}`, // 默认昵称
            };
            user = await this.userRepository.create(createData);
        }
        // 生成JWT
        const token = jsonwebtoken_1.default.sign({ userId: user.id, phone: user.phone }, this.jwtSecret, { expiresIn: this.jwtExpiresIn });
        // 计算过期时间（秒）
        const expiresIn = this.jwtExpiresIn;
        return {
            token,
            expires_in: expiresIn,
            user: {
                id: user.id,
                nickname: user.nickname,
                avatar: user.avatar,
            },
        };
    }
    async getUserProfile(userId) {
        const user = await this.userRepository.findById(userId);
        if (!user) {
            throw new errors_1.AppError('用户不存在', 404);
        }
        const stats = await this.userRepository.getUserStats(userId);
        // 计算用户等级（简化版）
        const level = Math.floor(stats.total_distance / 100) + 1;
        return {
            id: user.id,
            nickname: user.nickname,
            avatar: user.avatar,
            gender: user.gender,
            birthday: user.birthday,
            height: user.height,
            weight: user.weight,
            total_distance: stats.total_distance,
            total_time: stats.total_time,
            total_runs: stats.total_runs,
            level,
            preferences: {
                target_distance: user.target_distance ? user.target_distance / 1000 : undefined, // 转换为公里
                remind_time: '07:00', // 默认值
                voice_frequency: 'every_km', // 默认值
            },
        };
    }
    async updateUserProfile(userId, updateData) {
        const user = await this.userRepository.update(userId, updateData);
        if (!user) {
            throw new errors_1.AppError('用户不存在', 404);
        }
        return user;
    }
}
exports.UserService = UserService;
