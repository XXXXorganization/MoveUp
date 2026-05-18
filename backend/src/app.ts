// src/app.ts
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { createUserRoutes } from './routes/user';
import { createSportRoutes } from './routes/sport';
import { createCoachingRoutes } from './routes/coaching';
import { createSocialRoutes } from './routes/social';
import { createChallengeRoutes } from './routes/challenge';
import { createAIRoutes } from './routes/ai';
import { UserController } from './modules/user/controller';
import { UserService } from './modules/user/service';
import { UserRepository } from './modules/user/repository';
import { SportController } from './modules/sport/controller';
import { SportService } from './modules/sport/service';
import { SportRepository } from './modules/sport/repository';
import { CoachingController } from './modules/coaching/controller';
import { CoachingService } from './modules/coaching/service';
import { CoachingRepository } from './modules/coaching/repository';
import { SocialController } from './modules/social/controller';
import { SocialService } from './modules/social/service';
import { SocialRepository } from './modules/social/repository';
import { ChallengeController } from './modules/challenge/controller';
import { ChallengeService } from './modules/challenge/service';
import { ChallengeRepository } from './modules/challenge/repository';
import { AIController } from './modules/ai/controller';
import { AIService } from './modules/ai/service';
import { db } from './config/database';
import { errorHandler } from './middleware/errorHandler';

const app = express();

// 中间件
app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 请求日志（调试用，看前端有没有调进来）
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl} [${req.ip}]`);
  if (req.method !== 'GET' && req.body && Object.keys(req.body).length > 0) {
    console.log('  Body:', JSON.stringify(req.body));
  }
  if (req.headers.authorization) {
    console.log('  Auth:', req.headers.authorization.slice(0, 30) + '...');
  }
  next();
});

// 依赖注入
const userRepository = new UserRepository(db);
const userService = new UserService(userRepository, process.env.JWT_SECRET || 'your-secret-key', parseInt(process.env.JWT_EXPIRES_IN || '7200'));
const userController = new UserController(userService);

const sportRepository = new SportRepository(db);
const sportService = new SportService(sportRepository);
const sportController = new SportController(sportService);

const coachingRepository = new CoachingRepository(db);
const coachingService = new CoachingService(coachingRepository);
const coachingController = new CoachingController(coachingService);

const socialRepository = new SocialRepository(db);
const socialService = new SocialService(socialRepository);
const socialController = new SocialController(socialService);

const challengeRepository = new ChallengeRepository(db);
const challengeService = new ChallengeService(challengeRepository);
const challengeController = new ChallengeController(challengeService);

const aiService = new AIService();
const aiController = new AIController(aiService);

// 兼容 mock 后端路由（桥接前端旧 API 到真实后端）
import { createCompatRoutes } from './routes/compat';
app.use('/v1', createCompatRoutes(userService, userRepository, sportService));

// 路由
app.use('/v1', createUserRoutes(userController));
app.use('/v1', createSportRoutes(sportController));
app.use('/v1', createCoachingRoutes(coachingController));
app.use('/v1', createSocialRoutes(socialController));
app.use('/v1', createChallengeRoutes(challengeController));
app.use('/v1', createAIRoutes(aiController));

// 健康检查端点
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

// 错误处理中间件
app.use(errorHandler);

export default app;