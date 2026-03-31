// src/app.ts
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { createUserRoutes } from './routes/user';
import { UserController } from './modules/user/controller';
import { UserService } from './modules/user/service';
import { UserRepository } from './modules/user/repository';
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

// 路由
app.use('/v1', createUserRoutes(userController));

// 错误处理中间件
app.use(errorHandler);

export default app;