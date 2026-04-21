// src/modules/challenge/service.ts
import { ChallengeRepository } from './repository';
import {
  UserTaskDetail, UserAchievementDetail, UserChallengeDetail,
  UserMembershipDetail, UpdateTaskProgressRequest,
} from './types';
import { AppError } from '../../utils/errors';

export class ChallengeService {
  constructor(private repository: ChallengeRepository) {}

  // ==================== 任务系统 ====================

  async getTasks(taskType?: string) {
    return this.repository.getActiveTasks(taskType);
  }

  async getUserTasks(userId: string): Promise<UserTaskDetail[]> {
    const userTasks = await this.repository.getUserTasks(userId);
    if (userTasks.length === 0) return [];

    const taskIds = userTasks.map(ut => ut.taskId);
    const tasks = await Promise.all(taskIds.map(id => this.repository.getTaskById(id)));
    const taskMap = new Map(tasks.filter(Boolean).map(t => [t!.id, t!]));

    const badgeIds = tasks.filter(t => t?.rewardBadgeId).map(t => t!.rewardBadgeId!);
    const badges = badgeIds.length > 0 ? await this.repository.getBadgesByIds(badgeIds) : [];
    const badgeMap = new Map(badges.map(b => [b.id, b]));

    return userTasks.map(ut => {
      const task = taskMap.get(ut.taskId)!;
      return { ...ut, task, badge: task?.rewardBadgeId ? badgeMap.get(task.rewardBadgeId) : undefined };
    });
  }

  async updateTaskProgress(userId: string, req: UpdateTaskProgressRequest): Promise<UserTaskDetail> {
    const task = await this.repository.getTaskById(req.taskId);
    if (!task) throw new AppError('任务不存在', 404);

    const isCompleted = req.progress >= task.requirement.target;
    const existing = await this.repository.getUserTask(userId, req.taskId);

    if (existing?.isCompleted) throw new AppError('任务已完成', 409);

    const userTask = await this.repository.upsertUserTask(userId, req.taskId, req.progress, isCompleted);

    // 完成任务时发放积分和徽章
    if (isCompleted && !existing?.isCompleted) {
      if (task.rewardPoints > 0) {
        await this.repository.addPoints(userId, task.rewardPoints, 'task', task.id, `完成任务：${task.name}`);
      }
      if (task.rewardBadgeId) {
        const alreadyHas = await this.repository.getUserAchievement(userId, task.rewardBadgeId);
        if (!alreadyHas) {
          await this.repository.createUserAchievement(userId, task.rewardBadgeId);
        }
      }
    }

    const badge = task.rewardBadgeId ? await this.repository.getBadgeById(task.rewardBadgeId) ?? undefined : undefined;
    return { ...userTask, task, badge };
  }

  // ==================== 成就徽章 ====================

  async getAllBadges() {
    return this.repository.getAllBadges();
  }

  async getUserAchievements(userId: string): Promise<UserAchievementDetail[]> {
    const achievements = await this.repository.getUserAchievements(userId);
    if (achievements.length === 0) return [];

    const badgeIds = achievements.map(a => a.badgeId);
    const badges = await this.repository.getBadgesByIds(badgeIds);
    const badgeMap = new Map(badges.map(b => [b.id, b]));

    return achievements.map(a => ({ ...a, badge: badgeMap.get(a.badgeId)! }));
  }

  // ==================== 挑战赛 ====================

  async getChallenges(status: 'ongoing' | 'upcoming' | 'completed' = 'ongoing') {
    return this.repository.getChallengesByStatus(status);
  }

  async getChallengeById(id: string) {
    const challenge = await this.repository.getChallengeById(id);
    if (!challenge) throw new AppError('挑战不存在', 404);
    return challenge;
  }

  async joinChallenge(userId: string, challengeId: string): Promise<UserChallengeDetail> {
    const challenge = await this.repository.getChallengeById(challengeId);
    if (!challenge) throw new AppError('挑战不存在', 404);

    const now = new Date();
    if (now > challenge.endTime) throw new AppError('挑战已结束', 400);
    if (now < challenge.startTime) throw new AppError('挑战尚未开始', 400);

    const existing = await this.repository.getUserChallenge(userId, challengeId);
    if (existing) throw new AppError('已参与该挑战', 409);

    const userChallenge = await this.repository.joinChallenge(userId, challengeId);
    return { ...userChallenge, challenge };
  }

  async getUserChallenges(userId: string): Promise<UserChallengeDetail[]> {
    const userChallenges = await this.repository.getUserChallenges(userId);
    if (userChallenges.length === 0) return [];

    const challengeIds = userChallenges.map(uc => uc.challengeId);
    const challenges = await Promise.all(challengeIds.map(id => this.repository.getChallengeById(id)));
    const challengeMap = new Map(challenges.filter(Boolean).map(c => [c!.id, c!]));

    return userChallenges.map(uc => ({ ...uc, challenge: challengeMap.get(uc.challengeId)! }));
  }

  async updateChallengeProgress(userId: string, challengeId: string, progress: number): Promise<UserChallengeDetail> {
    const challenge = await this.repository.getChallengeById(challengeId);
    if (!challenge) throw new AppError('挑战不存在', 404);

    const existing = await this.repository.getUserChallenge(userId, challengeId);
    if (!existing) throw new AppError('尚未参与该挑战', 404);

    const isCompleted = progress >= challenge.targetValue;
    const updated = await this.repository.updateChallengeProgress(userId, challengeId, progress, isCompleted);
    if (!updated) throw new AppError('更新失败', 500);

    if (isCompleted && !existing.isCompleted) {
      if (challenge.rewardPoints > 0) {
        await this.repository.addPoints(userId, challenge.rewardPoints, 'challenge', challenge.id, `完成挑战：${challenge.name}`);
      }
      if (challenge.rewardBadgeId) {
        const alreadyHas = await this.repository.getUserAchievement(userId, challenge.rewardBadgeId);
        if (!alreadyHas) await this.repository.createUserAchievement(userId, challenge.rewardBadgeId);
      }
    }

    return { ...updated, challenge };
  }

  async getChallengeRanking(challengeId: string) {
    const challenge = await this.repository.getChallengeById(challengeId);
    if (!challenge) throw new AppError('挑战不存在', 404);
    return this.repository.getChallengeRanking(challengeId);
  }

  // ==================== 会员服务 ====================

  async getMembershipPlans() {
    return this.repository.getMembershipPlans();
  }

  async getActiveMembership(userId: string): Promise<UserMembershipDetail | null> {
    const membership = await this.repository.getActiveMembership(userId);
    if (!membership) return null;
    const plan = await this.repository.getMembershipPlanById(membership.planId);
    const daysRemaining = Math.max(0, Math.ceil((membership.endDate.getTime() - Date.now()) / 86400000));
    return { ...membership, plan: plan!, daysRemaining };
  }

  async purchaseMembership(userId: string, planId: string): Promise<UserMembershipDetail> {
    const plan = await this.repository.getMembershipPlanById(planId);
    if (!plan || !plan.isActive) throw new AppError('会员套餐不存在', 404);

    const existing = await this.repository.getActiveMembership(userId);
    const startDate = existing ? existing.endDate : new Date();
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + plan.durationDays);

    const membership = await this.repository.createMembership(userId, planId, startDate, endDate);
    const daysRemaining = Math.ceil((endDate.getTime() - Date.now()) / 86400000);
    return { ...membership, plan, daysRemaining };
  }

  // ==================== 积分 ====================

  async getPointsSummary(userId: string) {
    const total = await this.repository.getTotalPoints(userId);
    return { totalPoints: total };
  }

  async getPointsLogs(userId: string, limit = 20, offset = 0) {
    return this.repository.getPointsLogs(userId, limit, offset);
  }
}
