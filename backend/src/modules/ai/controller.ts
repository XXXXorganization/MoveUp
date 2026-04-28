// src/modules/ai/controller.ts
import { Request, Response, NextFunction } from 'express';
import { AIService } from './service';
import { AppError } from '../../utils/errors';

export class AIController {
  constructor(private service: AIService) {}

  private getUserId(req: Request): string {
    const userId = (req as any).user?.userId;
    if (!userId) throw new AppError('请先登录', 401);
    return userId;
  }

  /**
   * 单次运动 AI 总结
   * POST /v1/ai/sport-summary
   */
  async summarizeSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      this.getUserId(req);
      const { distance, duration, calories, avgPace, avgHeartRate, maxHeartRate, date } = req.body;

      if (distance === undefined || duration === undefined || calories === undefined) {
        throw new AppError('缺少必要参数：distance、duration、calories', 400);
      }

      const result = await this.service.summarizeSportRecord({
        distance, duration, calories, avgPace, avgHeartRate, maxHeartRate, date,
      });

      res.json({ code: 200, message: 'AI 总结生成成功', data: result });
    } catch (e) { next(e); }
  }

  /**
   * 历史运动 AI 总结
   * POST /v1/ai/history-summary
   */
  async summarizeHistory(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      this.getUserId(req);
      const { records, periodLabel } = req.body;

      if (!Array.isArray(records) || records.length === 0) {
        throw new AppError('records 不能为空', 400);
      }

      const result = await this.service.summarizeHistory({
        records,
        periodLabel: periodLabel || '近期',
      });

      res.json({ code: 200, message: 'AI 历史总结生成成功', data: result });
    } catch (e) { next(e); }
  }
}
