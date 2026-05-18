// src/routes/compat.ts
// 兼容 mock 后端路由，桥接前端旧 API 路径到真实后端

import { Router, Request, Response, NextFunction } from 'express';
import bcrypt from 'bcryptjs';
import { UserService } from '../modules/user/service';
import { UserRepository } from '../modules/user/repository';
import { SportService } from '../modules/sport/service';
import { authenticateToken } from '../middleware/auth';

export function createCompatRoutes(
  userService: UserService,
  userRepository: UserRepository,
  sportService: SportService
): Router {
  const router = Router();

  /**
   * POST /v1/auth/register
   * mock 原有接口：{ phone, username, password }
   * 行为：bcrypt 哈希密码入库，后续 login 可直接用密码或验证码
   */
  router.post('/auth/register', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { phone, username, password } = req.body || {};
      if (!phone || !username || !password) {
        return res.json({ code: 400, message: '注册信息不完整' });
      }

      // 检查是否已存在
      const existing = await userRepository.findByPhone(phone);
      if (existing) {
        return res.json({ code: 409, message: '该手机号已注册，请直接登录' });
      }

      // bcrypt 哈希密码
      const passwordHash = bcrypt.hashSync(password, 10);

      // 创建用户
      await userRepository.createWithPassword(phone, username, passwordHash);

      res.json({ code: 200, message: '注册成功' });
    } catch (e) {
      next(e);
    }
  });

  /**
   * GET /v1/runs → GET /v1/sport (获取用户跑步记录列表)
   */
  router.get('/runs', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) return res.status(401).json({ code: 401, message: '请先登录' });

      const records = await sportService.getSportRecordsByUserId(userId);
      const completedRecords = records.filter(r => r.status === 'completed');

      const totalDistance = completedRecords.reduce((s, r) => s + r.distance, 0);
      const totalDuration = completedRecords.reduce((s, r) => s + r.duration, 0);
      const totalRuns = completedRecords.length;
      const avgPace = totalRuns > 0
        ? Math.round(completedRecords.reduce((s, r) => s + (r.averagePace || 0), 0) / totalRuns)
        : 0;

      res.json({
        code: 200,
        message: 'success',
        data: {
          stats: {
            total_distance: (totalDistance / 1000).toFixed(2),
            total_duration_str: `${Math.floor(totalDuration / 3600)}h ${Math.floor((totalDuration % 3600) / 60)}m`,
            total_runs: String(totalRuns),
            avg_pace: avgPace > 0 ? `${Math.floor(avgPace / 60)}'${String(avgPace % 60).padStart(2, '0')}"` : "0'00\"",
          },
          list: completedRecords.map(r => ({
            id: r.id,
            date: r.startTime ? new Date(r.startTime).toISOString().slice(0, 10) : '',
            title: '跑步打卡',
            duration_str: `${Math.floor(r.duration / 60)}.${String(r.duration % 60).padStart(2, '0')}`,
            pace: r.averagePace
              ? `${Math.floor(r.averagePace / 60)}'${String(Math.round(r.averagePace % 60)).padStart(2, '0')}"`
              : "0'00\"",
            distance: `${(r.distance / 1000).toFixed(2)} Km`,
          })),
        },
      });
    } catch (e) {
      next(e);
    }
  });

  /**
   * POST /v1/runs/start → POST /v1/sport/start
   */
  router.post('/runs/start', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) return res.status(401).json({ code: 401, message: '请先登录' });

      const record = await sportService.startSportRecord(userId);
      res.json({
        code: 200,
        message: 'success',
        data: {
          run_id: record.id,
          start_time: record.startTime,
        },
      });
    } catch (e) {
      next(e);
    }
  });

  return router;
}
