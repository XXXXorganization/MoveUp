// src/routes/challenge.ts
import { Router } from 'express';
import { ChallengeController } from '../modules/challenge/controller';
import { authenticateToken } from '../middleware/auth';

export function createChallengeRoutes(controller: ChallengeController): Router {
  const router = Router();

  // 任务系统
  router.get('/challenge/tasks', authenticateToken, controller.getTasks.bind(controller));
  router.get('/challenge/tasks/my', authenticateToken, controller.getUserTasks.bind(controller));
  router.put('/challenge/tasks/progress', authenticateToken, controller.updateTaskProgress.bind(controller));

  // 成就徽章
  router.get('/challenge/badges', authenticateToken, controller.getAllBadges.bind(controller));
  router.get('/challenge/badges/my', authenticateToken, controller.getUserAchievements.bind(controller));

  // 挑战赛
  router.get('/challenge/challenges', authenticateToken, controller.getChallenges.bind(controller));
  router.get('/challenge/challenges/my', authenticateToken, controller.getUserChallenges.bind(controller));
  router.get('/challenge/challenges/:challengeId', authenticateToken, controller.getChallengeById.bind(controller));
  router.post('/challenge/challenges/:challengeId/join', authenticateToken, controller.joinChallenge.bind(controller));
  router.put('/challenge/challenges/:challengeId/progress', authenticateToken, controller.updateChallengeProgress.bind(controller));
  router.get('/challenge/challenges/:challengeId/ranking', authenticateToken, controller.getChallengeRanking.bind(controller));

  // 会员服务
  router.get('/challenge/membership/plans', authenticateToken, controller.getMembershipPlans.bind(controller));
  router.get('/challenge/membership/my', authenticateToken, controller.getActiveMembership.bind(controller));
  router.post('/challenge/membership/purchase', authenticateToken, controller.purchaseMembership.bind(controller));

  // 积分
  router.get('/challenge/points', authenticateToken, controller.getPointsSummary.bind(controller));
  router.get('/challenge/points/logs', authenticateToken, controller.getPointsLogs.bind(controller));

  return router;
}
