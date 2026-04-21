// src/routes/coaching.ts
import { Router } from 'express';
import { CoachingController } from '../modules/coaching/controller';
import { authenticateToken } from '../middleware/auth';

export function createCoachingRoutes(controller: CoachingController): Router {
  const router = Router();

  // 训练计划
  router.get('/coaching/plans', authenticateToken, controller.getPlans.bind(controller));
  router.get('/coaching/plans/:planId', authenticateToken, controller.getPlanById.bind(controller));
  router.post('/coaching/plans/recommend', authenticateToken, controller.recommendPlan.bind(controller));
  router.post('/coaching/plans/:planId/adopt', authenticateToken, controller.adoptPlan.bind(controller));

  // 用户计划进度
  router.get('/coaching/my-plan', authenticateToken, controller.getActivePlan.bind(controller));
  router.get('/coaching/today-task', authenticateToken, controller.getTodayTask.bind(controller));
  router.put('/coaching/my-plan/:userPlanId/progress', authenticateToken, controller.updateProgress.bind(controller));
  router.delete('/coaching/my-plan/:userPlanId', authenticateToken, controller.quitPlan.bind(controller));

  // 语音指导
  router.post('/coaching/voice-guidance', authenticateToken, controller.getVoiceGuidance.bind(controller));
  router.post('/coaching/segment-advice', authenticateToken, controller.getSegmentAdvice.bind(controller));

  // 健康建议
  router.get('/coaching/stretching', authenticateToken, controller.getStretchingGuide.bind(controller));
  router.get('/coaching/injury-prevention', authenticateToken, controller.getInjuryPreventionTips.bind(controller));

  return router;
}
