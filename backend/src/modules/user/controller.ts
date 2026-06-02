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
      const { phone, code } = req.body;
      const isNumericCode = /^\d{6}$/.test(code);

      if (isNumericCode) {
        // 6位数字 → 优先走验证码登录，失败则尝试密码登录
        try {
          const request: LoginRequest = { phone, code };
          const result = await this.userService.login(request);
          res.json({ code: 200, message: '登录成功', data: result });
          return;
        } catch {
          // 验证码失败，尝试密码登录
        }
      }

      // 非6位数字或验证码失败 → 走密码登录
      const result = await this.userService.loginWithPassword(phone, code);
      res.json({ code: 200, message: '登录成功', data: result });
    } catch (error) {
      next(error);
    }
  }

  async registerWithPassword(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { phone, username, password } = req.body;
      if (!phone || !username || !password) {
        res.status(400).json({ code: 400, message: '手机号、用户名和密码不能为空', data: null });
        return;
      }
      const result = await this.userService.registerWithPassword(phone, username, password);
      res.json({ code: 200, message: '注册成功', data: result });
    } catch (error) {
      next(error);
    }
  }

  async getUserProfile(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.userId;
      const profile = await this.userService.getUserProfile(userId);
      res.json({
        code: 200,
        message: 'success',
        data: {
          ...profile,
          username: profile.nickname, // Android 兼容别名
          password: '********',
        },
      });
    } catch (error) {
      next(error);
    }
  }

  async updateUserProfile(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.userId;
      const updateData: UpdateUserRequest = req.body;
      // Android 兼容: username -> nickname, 并移除未在数据库中的字段
      if ((updateData as any).username) {
        updateData.nickname = (updateData as any).username;
        delete (updateData as any).username;
      }
      delete (updateData as any).email;
      delete (updateData as any).password;
      delete (updateData as any).user_id;
      const user = await this.userService.updateUserProfile(userId, updateData);
      res.json({ code: 200, message: '更新成功', data: user });
    } catch (error) {
      next(error);
    }
  }
}