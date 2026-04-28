"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.UserController = void 0;
class UserController {
    constructor(userService) {
        this.userService = userService;
    }
    async sendVerificationCode(req, res, next) {
        try {
            const request = req.body;
            await this.userService.sendVerificationCode(request);
            res.json({ code: 200, message: '验证码发送成功' });
        }
        catch (error) {
            next(error);
        }
    }
    async login(req, res, next) {
        try {
            const request = req.body;
            const result = await this.userService.login(request);
            res.json({ code: 200, message: '登录成功', data: result });
        }
        catch (error) {
            next(error);
        }
    }
    async getUserProfile(req, res, next) {
        try {
            const userId = req.user.userId; // 从JWT中间件获取
            const profile = await this.userService.getUserProfile(userId);
            res.json({ code: 200, message: 'success', data: profile });
        }
        catch (error) {
            next(error);
        }
    }
    async updateUserProfile(req, res, next) {
        try {
            const userId = req.user.userId;
            const updateData = req.body;
            const user = await this.userService.updateUserProfile(userId, updateData);
            res.json({ code: 200, message: '更新成功', data: user });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.UserController = UserController;
