// src/routes/compatibility.ts
// Android 前端兼容路由 — 将旧的 /v1/runs/* 路径映射到新的 /v1/sport/*
// 后续 Android 重构完成后可移除此文件

import { Router, Request, Response, NextFunction } from 'express';
import { SportService } from '../modules/sport/service';
import { authenticateToken } from '../middleware/auth';
import { aiChat } from '../utils/llm';

function formatDuration(seconds: number | undefined): string {
  if (!seconds) return '00:00.00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  const ms = Math.floor((seconds % 1) * 100);
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}.${String(ms).padStart(2, '0')}`;
}

function formatPace(secondsPerKm: number | null | undefined): string {
  if (!secondsPerKm || secondsPerKm <= 0) return "0'00\"";
  const mins = Math.floor(secondsPerKm / 60);
  const secs = Math.floor(secondsPerKm % 60);
  return `${mins}'${String(secs).padStart(2, '0')}"`;
}

export function createCompatibilityRoutes(sportService: SportService, db?: any): Router {
  const router = Router();

  // POST /v1/runs/start → 开始运动
  router.post('/runs/start', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.user!.userId;
      const record = await sportService.startSportRecord(userId);
      res.json({
        code: 200,
        message: 'success',
        data: {
          run_id: record.id,
          start_time: record.startTime,
        },
      });
    } catch (error) {
      next(error);
    }
  });

  // POST /v1/runs/:recordId/points → 上传 GPS 轨迹点
  router.post('/runs/:recordId/points', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { recordId } = req.params;
      const { points } = req.body;

      if (!points || !Array.isArray(points)) {
        res.status(400).json({ code: 400, message: 'GPS 点数据格式错误', data: null });
        return;
      }

      // 字段映射: Android {lat, lng, altitude, speed, timestamp} → 后端 {latitude, longitude, altitude, speed, timestamp}
      const gpsPoints = points.map((p: any) => ({
        recordId,
        latitude: p.lat,
        longitude: p.lng,
        timestamp: p.timestamp || new Date().toISOString(),
        speed: p.speed,
        altitude: p.altitude,
      }));

      await sportService.batchUploadGpsPoints(recordId, gpsPoints);
      res.json({ code: 200, message: 'success' });
    } catch (error) {
      next(error);
    }
  });

  // POST /v1/runs/finish → 结束运动（查找用户当前活跃记录）
  router.post('/runs/finish', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.user!.userId;
      const { run_id } = req.body;

      let recordId = run_id;
      if (!recordId) {
        // Android 没传 run_id，查找用户活跃记录
        const records = await sportService.getSportRecordsByUserId(userId);
        const active = records.find(r => r.status === 'active');
        if (!active) {
          res.status(400).json({ code: 400, message: '没有进行中的运动记录', data: null });
          return;
        }
        recordId = active.id;
      }

      const record = await sportService.stopSportRecord(recordId);
      res.json({
        code: 200,
        message: 'success',
        data: {
          id: record.id,
          distance: (record.distance / 1000).toFixed(2) + ' Km',
          duration_str: formatDuration(record.duration),
          pace: formatPace(record.averagePace),
          calories: String(Math.round(record.calories)),
        },
      });
    } catch (error) {
      next(error);
    }
  });

  // GET /v1/runs → 获取运动记录列表（格式匹配 Android 期望）
  router.get('/runs', authenticateToken, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.user!.userId;
      const records = await sportService.getSportRecordsByUserId(userId);

      const list = records.map(r => ({
        id: r.id,
        date: r.startTime ? new Date(r.startTime).toISOString().split('T')[0] : '未知时间',
        title: 'Outdoor Run',
        duration_str: formatDuration(r.duration),
        pace: formatPace(r.averagePace),
        distance: (r.distance / 1000).toFixed(2) + ' Km',
      }));

      res.json({
        code: 200,
        message: 'success',
        data: { list },
      });
    } catch (error) {
      next(error);
    }
  });

  // ========== 计划兼容路由 ==========

  // GET /v1/plan/total_distance → 返回本周实际跑步总里程
  router.get('/plan/total_distance', authenticateToken, async (req: Request, res: Response) => {
    if (!db) { res.json({ code: 200, data: { total_distance: 0 } }); return; }
    // 计算本周一到现在
    const now = new Date();
    const dayOfWeek = now.getDay();
    const monday = new Date(now);
    monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
    monday.setHours(0, 0, 0, 0);
    const result = await db('sport_records')
      .where({ user_id: req.user!.userId, status: 'completed' })
      .where('start_time', '>=', monday)
      .sum('distance as total')
      .first();
    const totalMeters = parseFloat(result?.total || 0);
    const totalKm = Math.round(totalMeters / 10) / 100; // 米→公里，保留两位
    res.json({ code: 200, data: { total_distance: totalKm } });
  });

  // GET /v1/plan/details?day=MONDAY → 获取某天的计划
  router.get('/plan/details', authenticateToken, async (req: Request, res: Response) => {
    if (!db) { res.json({ code: 200, data: { list: [] } }); return; }
    const day = (req.query.day as string) || 'MONDAY';
    const items = await db('user_plan_items')
      .where({ user_id: req.user!.userId, day_of_week: day })
      .orderBy('sort_order', 'asc');
    const list = items.map((item: any) => ({
      id: item.id,
      time: item.start_time || '',
      distance: item.distance_km ? parseFloat(item.distance_km) : 0,
      is_completed: !!item.is_completed,
    }));
    res.json({ code: 200, data: { day, list } });
  });

  // POST /v1/plan/details → 添加计划项
  router.post('/plan/details', authenticateToken, async (req: Request, res: Response) => {
    if (!db) { res.json({ code: 500, message: 'DB not available' }); return; }
    const { day, start_time, end_time, distance } = req.body;
    const distNum = parseFloat(distance) || 0;
    await db('user_plan_items').insert({
      user_id: req.user!.userId,
      day_of_week: day,
      start_time,
      end_time,
      distance_km: distNum,
    });
    res.json({ code: 200, message: 'success' });
  });

  // POST /v1/plan/details/delete → 删除计划项
  router.post('/plan/details/delete', authenticateToken, async (req: Request, res: Response) => {
    if (!db) { res.json({ code: 500, message: 'DB not available' }); return; }
    const { day, index } = req.body;
    const items = await db('user_plan_items')
      .where({ user_id: req.user!.userId, day_of_week: day })
      .orderBy('sort_order', 'asc');
    if (items[index]) {
      await db('user_plan_items').where({ id: items[index].id }).del();
    }
    res.json({ code: 200, message: 'success' });
  });

  // PUT /v1/plan/toggle_complete → 切换任务完成状态
  router.put('/plan/toggle_complete', authenticateToken, async (req: Request, res: Response) => {
    if (!db) { res.json({ code: 500, message: 'DB not available' }); return; }
    const { day, index } = req.body;
    const items = await db('user_plan_items')
      .where({ user_id: req.user!.userId, day_of_week: day })
      .orderBy('sort_order', 'asc');
    if (items[index]) {
      const newStatus = !items[index].is_completed;
      await db('user_plan_items').where({ id: items[index].id }).update({ is_completed: newStatus });
      res.json({ code: 200, data: { is_completed: newStatus } });
    } else {
      res.status(404).json({ code: 404, message: 'Not found' });
    }
  });

  // ========== AI 兼容路由 ==========

  const AI_SYSTEM_PROMPT = `你是一个名叫 MoveUp 的专业跑步助理教练。你的职责是：
1. 热情地为用户提供跑步建议、训练计划制定、跑步数据分析
2. 根据用户的位置信息，介绍附近的跑步路线和景点
3. 鼓励用户坚持跑步，提供正向的情绪价值
4. 回答关于跑步技巧、装备选择、伤病预防等问题
5. 用中文回复，语气活泼亲切，像朋友一样交流
请用简短的段落回复，每次回复控制在100字以内。`;

  // POST /v1/ai/chat → AI 跑步语音助手（通义千问）
  router.post('/ai/chat', authenticateToken, async (req: Request, res: Response) => {
    try {
      const { chat_history } = req.body;

      const messages = [
        { role: 'system' as const, content: AI_SYSTEM_PROMPT },
        ...(chat_history || []).map((m: any) => ({
          role: m.role || 'user',
          content: m.content || '',
        })),
      ];

      const reply = await aiChat(messages);
      res.json({ code: 200, message: 'success', data: { reply } });
    } catch (error) {
      console.error('AI chat error:', error);
      res.json({
        code: 200,
        message: 'success',
        data: { reply: '抱歉，我现在有点累，请稍后再问我吧！' },
      });
    }
  });

  return router;
}
