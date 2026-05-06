// src/modules/challenge/controller.ts
import { Request, Response, NextFunction } from 'express';
import { ChallengeService } from './service';
import { AppError } from '../../utils/errors';

export class ChallengeController {
  constructor(private service: ChallengeService) {}

  private getUserId(req: Request): string {
    const userId = req.user?.userId;
    if (!userId) throw new AppError('请先登录', 401);
    return userId;
  }

  // ==================== 任务系统 ====================

  /** GET /v1/challenge/tasks?type=daily */
  async getTasks(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const tasks = await this.service.getTasks(req.query.type as string | undefined);
      res.json({ code: 200, message: '获取任务列表成功', data: tasks });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/tasks/my */
  async getUserTasks(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const tasks = await this.service.getUserTasks(userId);
      res.json({ code: 200, message: '获取我的任务成功', data: tasks });
    } catch (e) { next(e); }
  }

  /** PUT /v1/challenge/tasks/progress */
  async updateTaskProgress(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { taskId, progress } = req.body;
      if (!taskId || progress === undefined) throw new AppError('缺少 taskId 或 progress 参数', 400);
      const result = await this.service.updateTaskProgress(userId, { taskId, progress });
      res.json({ code: 200, message: '任务进度已更新', data: result });
    } catch (e) { next(e); }
  }

  // ==================== 成就徽章 ====================

  /** GET /v1/challenge/badges */
  async getAllBadges(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const badges = await this.service.getAllBadges();
      res.json({ code: 200, message: '获取徽章列表成功', data: badges });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/badges/my */
  async getUserAchievements(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const achievements = await this.service.getUserAchievements(userId);
      res.json({ code: 200, message: '获取我的成就成功', data: achievements });
    } catch (e) { next(e); }
  }

  // ==================== 挑战赛 ====================

  /** GET /v1/challenge/challenges?status=ongoing */
  async getChallenges(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const status = (['ongoing', 'upcoming', 'completed'].includes(req.query.status as string)
        ? req.query.status : 'ongoing') as 'ongoing' | 'upcoming' | 'completed';
      const challenges = await this.service.getChallenges(status);
      res.json({ code: 200, message: '获取挑战列表成功', data: challenges });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/challenges/:challengeId */
  async getChallengeById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const challenge = await this.service.getChallengeById(req.params.challengeId);
      res.json({ code: 200, message: '获取挑战详情成功', data: challenge });
    } catch (e) { next(e); }
  }

  /** POST /v1/challenge/challenges/:challengeId/join */
  async joinChallenge(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const result = await this.service.joinChallenge(userId, req.params.challengeId);
      res.status(201).json({ code: 200, message: '成功参与挑战', data: result });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/challenges/my */
  async getUserChallenges(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const challenges = await this.service.getUserChallenges(userId);
      res.json({ code: 200, message: '获取我的挑战成功', data: challenges });
    } catch (e) { next(e); }
  }

  /** PUT /v1/challenge/challenges/:challengeId/progress */
  async updateChallengeProgress(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { progress } = req.body;
      if (progress === undefined) throw new AppError('缺少 progress 参数', 400);
      const result = await this.service.updateChallengeProgress(userId, req.params.challengeId, progress);
      res.json({ code: 200, message: '挑战进度已更新', data: result });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/challenges/:challengeId/ranking */
  async getChallengeRanking(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      this.getUserId(req);
      const ranking = await this.service.getChallengeRanking(req.params.challengeId);
      res.json({ code: 200, message: '获取排名成功', data: ranking });
    } catch (e) { next(e); }
  }

  // ==================== 会员服务 ====================

  /** GET /v1/challenge/membership/plans */
  async getMembershipPlans(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const plans = await this.service.getMembershipPlans();
      res.json({ code: 200, message: '获取会员套餐成功', data: plans });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/membership/my */
  async getActiveMembership(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const membership = await this.service.getActiveMembership(userId);
      res.json({ code: 200, message: '获取会员状态成功', data: membership });
    } catch (e) { next(e); }
  }

  /** POST /v1/challenge/membership/purchase */
  async purchaseMembership(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { planId } = req.body;
      if (!planId) throw new AppError('缺少 planId 参数', 400);
      const result = await this.service.purchaseMembership(userId, planId);
      res.status(201).json({ code: 200, message: '会员购买成功', data: result });
    } catch (e) { next(e); }
  }

  // ==================== 积分 ====================

  /** GET /v1/challenge/points */
  async getPointsSummary(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const summary = await this.service.getPointsSummary(userId);
      res.json({ code: 200, message: '获取积分成功', data: summary });
    } catch (e) { next(e); }
  }

  /** GET /v1/challenge/points/logs */
  async getPointsLogs(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const limit = parseInt(req.query.limit as string) || 20;
      const offset = parseInt(req.query.offset as string) || 0;
      const logs = await this.service.getPointsLogs(userId, limit, offset);
      res.json({ code: 200, message: '获取积分记录成功', data: logs });
    } catch (e) { next(e); }
  }
}
