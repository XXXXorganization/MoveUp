"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createUserRoutes = void 0;
// src/routes/user.ts
const express_1 = require("express");
const auth_1 = require("../middleware/auth");
const createUserRoutes = (userController) => {
    const router = (0, express_1.Router)();
    // 发送验证码
    router.post('/auth/code', userController.sendVerificationCode.bind(userController));
    // 用户登录/注册
    router.post('/auth/login', userController.login.bind(userController));
    // 获取用户资料（需要认证）
    router.get('/user/profile', auth_1.authenticateToken, userController.getUserProfile.bind(userController));
    // 更新用户资料（需要认证）
    router.put('/user/profile', auth_1.authenticateToken, userController.updateUserProfile.bind(userController));
    return router;
};
exports.createUserRoutes = createUserRoutes;
