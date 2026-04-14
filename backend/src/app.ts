// src/app.ts
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { createUserRoutes } from './routes/user';
import { createSportRoutes } from './routes/sport';
import { UserController } from './modules/user/controller';
import { UserService } from './modules/user/service';
import { UserRepository } from './modules/user/repository';
import { SportController } from './modules/sport/controller';
import { SportService } from './modules/sport/service';
import { SportRepository } from './modules/sport/repository';
import { db } from './config/database';
import { errorHandler } from './middleware/errorHandler';

const app = express();

// 中间件
app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 依赖注入
const userRepository = new UserRepository(db);
const userService = new UserService(userRepository, process.env.JWT_SECRET || 'your-secret-key', parseInt(process.env.JWT_EXPIRES_IN || '7200'));
const userController = new UserController(userService);

const sportRepository = new SportRepository(db);
const sportService = new SportService(sportRepository);
const sportController = new SportController(sportService);

// 路由
app.use('/v1', createUserRoutes(userController));
app.use('/v1', createSportRoutes(sportController));

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