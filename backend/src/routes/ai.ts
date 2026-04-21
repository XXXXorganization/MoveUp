// src/routes/ai.ts
import { Router } from 'express';
import { AIController } from '../modules/ai/controller';
import { authenticateToken } from '../middleware/auth';

export function createAIRoutes(controller: AIController): Router {
  const router = Router();

  router.post('/ai/sport-summary', authenticateToken, controller.summarizeSportRecord.bind(controller));
  router.post('/ai/history-summary', authenticateToken, controller.summarizeHistory.bind(controller));

  return router;
}
