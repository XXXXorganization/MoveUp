// src/modules/coaching/controller.ts
import { Request, Response, NextFunction } from 'express';
import { CoachingService } from './service';
import { PlanRecommendRequest, VoiceGuidanceConfig } from './types';
import { AppError } from '../../utils/errors';

export class CoachingController {
  constructor(private service: CoachingService) {}

  /**
   * 获取所有训练计划
   * GET /v1/coaching/plans
   */
  async getPlans(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { difficulty } = req.query;
      const plans = await this.service.getAllPlans(difficulty as string | undefined);
      res.json({ code: 200, message: '获取训练计划成功', data: plans });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取单个训练计划详情
   * GET /v1/coaching/plans/:planId
   */
  async getPlanById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const plan = await this.service.getPlanById(req.params.planId);
      res.json({ code: 200, message: '获取计划详情成功', data: plan });
    } catch (error) {
      next(error);
    }
  }

  /**
   * AI 推荐训练计划
   * POST /v1/coaching/plans/recommend
   */
  async recommendPlan(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      const body: PlanRecommendRequest = req.body;
      if (!body.fitnessLevel || !body.targetDistance || !body.goalType) {
        throw new AppError('缺少必要参数', 400);
      }

      const plans = await this.service.recommendPlan(userId, body);
      res.json({ code: 200, message: '推荐计划获取成功', data: plans });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 采用训练计划
   * POST /v1/coaching/plans/:planId/adopt
   */
  async adoptPlan(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      const detail = await this.service.adoptPlan(userId, req.params.planId);
      res.status(201).json({ code: 200, message: '已开始训练计划', data: detail });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取当前进行中的计划
   * GET /v1/coaching/my-plan
   */
  async getActivePlan(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      const detail = await this.service.getActivePlan(userId);
      res.json({ code: 200, message: '获取当前计划成功', data: detail });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取今日训练任务
   * GET /v1/coaching/today-task
   */
  async getTodayTask(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      const task = await this.service.getTodayTask(userId);
      res.json({ code: 200, message: '获取今日任务成功', data: task });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 更新训练进度
   * PUT /v1/coaching/my-plan/:userPlanId/progress
   */
  async updateProgress(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      const { week, day } = req.body;
      if (!week || !day) throw new AppError('缺少 week 或 day 参数', 400);

      const detail = await this.service.updateProgress(userId, req.params.userPlanId, week, day);
      res.json({ code: 200, message: '训练进度已更新', data: detail });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 退出训练计划
   * DELETE /v1/coaching/my-plan/:userPlanId
   */
  async quitPlan(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) throw new AppError('请先登录', 401);

      await this.service.quitPlan(userId, req.params.userPlanId);
      res.json({ code: 200, message: '已退出训练计划', data: null });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取语音播报内容
   * POST /v1/coaching/voice-guidance
   */
  async getVoiceGuidance(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { distanceM, currentPace, currentHeartRate, config } = req.body;
      if (distanceM === undefined || currentPace === undefined) {
        throw new AppError('缺少 distanceM 或 currentPace 参数', 400);
      }

      const messages = this.service.getVoiceGuidanceConfig(
        distanceM,
        currentPace,
        currentHeartRate,
        config as VoiceGuidanceConfig | undefined,
      );
      res.json({ code: 200, message: '获取语音播报成功', data: { messages } });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取分段教练建议（根据心率）
   * POST /v1/coaching/segment-advice
   */
  async getSegmentAdvice(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { currentHeartRate, age } = req.body;
      if (!currentHeartRate) throw new AppError('缺少 currentHeartRate 参数', 400);

      const advice = this.service.getSegmentAdvice(currentHeartRate, age);
      res.json({ code: 200, message: '获取教练建议成功', data: advice });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取跑后拉伸指导
   * GET /v1/coaching/stretching?runDuration=1800
   */
  async getStretchingGuide(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const runDuration = parseInt(req.query.runDuration as string) || 0;
      const guide = this.service.getStretchingGuide(runDuration);
      res.json({ code: 200, message: '获取拉伸指导成功', data: guide });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取伤病预防建议
   * GET /v1/coaching/injury-prevention?weeklyDistance=30000&recentRunCount=4
   */
  async getInjuryPreventionTips(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const weeklyDistance = parseInt(req.query.weeklyDistance as string) || 0;
      const recentRunCount = parseInt(req.query.recentRunCount as string) || 0;

      const tips = this.service.getInjuryPreventionTips(weeklyDistance, recentRunCount);
      res.json({ code: 200, message: '获取伤病预防建议成功', data: tips });
    } catch (error) {
      next(error);
    }
  }
}
