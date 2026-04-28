// src/routes/user.ts
import { Router } from 'express';
import { UserController } from '../modules/user/controller';
import { authenticateToken } from '../middleware/auth';

export const createUserRoutes = (userController: UserController): Router => {
  const router = Router();

  // 发送验证码
  router.post('/auth/code', userController.sendVerificationCode.bind(userController));

  // 用户登录/注册
  router.post('/auth/login', userController.login.bind(userController));

  // 获取用户资料（需要认证）
  router.get('/user/profile', authenticateToken, userController.getUserProfile.bind(userController));

  // 更新用户资料（需要认证）
  router.put('/user/profile', authenticateToken, userController.updateUserProfile.bind(userController));

  return router;
};