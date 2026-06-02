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

  const AI_SYSTEM_PROMPT = `你是 MoveUp，一个专业的跑步语音助理教练。你的核心职责：

1. 实时跑步指导：用户会告诉你他的当前跑步数据（距离、配速、消耗卡路里、位置）。根据这些数据给出个性化的配速建议或鼓励。
2. 风景导游：根据用户提供的当前位置，搜索周围1公里范围内的知名景点、公园、地标。告诉用户：景点名称、距离多远、跑步过去大概需要几分钟。比如"前方800米是白堤，慢跑过去大约5分钟"。
3. 路线建议：如果附件有适合跑步的路线，给出具体的方向指引，像导航一样。比如"前面路口右转，沿着湖边跑500米就到苏堤了"。
4. 数据解读：如果用户提到他的跑步历史或训练计划，帮他分析进步。
5. 激励机制：用热情、活泼的语气鼓励用户。

回复要求：
- 中文回复，口语化，像语音对话
- 每次回复控制在60字以内
- 每次回复必须包含至少一个附近的景点/地标，并说明距离和预计跑步时间
- 如果没有著名景点，就推荐附近适合跑步的街道或公园`;

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
