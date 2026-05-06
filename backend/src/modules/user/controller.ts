// src/modules/user/controller.ts
import { Request, Response, NextFunction } from 'express';
import { UserService } from './service';
import { LoginRequest, SendCodeRequest, UpdateUserRequest } from './types';
//import { AppError } from '../../utils/errors';

export class UserController {
  private userService: UserService;

  constructor(userService: UserService) {
    this.userService = userService;
  }

  async sendVerificationCode(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const request: SendCodeRequest = req.body;
      await this.userService.sendVerificationCode(request);
      res.json({ code: 200, message: '验证码发送成功' });
    } catch (error) {
      next(error);
    }
  }

  async login(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const request: LoginRequest = req.body;
      const result = await this.userService.login(request);
      res.json({ code: 200, message: '登录成功', data: result });
    } catch (error) {
      next(error);
    }
  }

  async getUserProfile(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.userId; // 从JWT中间件获取
      const profile = await this.userService.getUserProfile(userId);
      res.json({ code: 200, message: 'success', data: profile });
    } catch (error) {
      next(error);
    }
  }

  async updateUserProfile(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.userId;
      const updateData: UpdateUserRequest = req.body;
      const user = await this.userService.updateUserProfile(userId, updateData);
      res.json({ code: 200, message: '更新成功', data: user });
    } catch (error) {
      next(error);
    }
  }
}