"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
// src/app.ts
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const helmet_1 = __importDefault(require("helmet"));
const user_1 = require("./routes/user");
const sport_1 = require("./routes/sport");
const controller_1 = require("./modules/user/controller");
const service_1 = require("./modules/user/service");
const repository_1 = require("./modules/user/repository");
const controller_2 = require("./modules/sport/controller");
const service_2 = require("./modules/sport/service");
const repository_2 = require("./modules/sport/repository");
const database_1 = require("./config/database");
const errorHandler_1 = require("./middleware/errorHandler");
const app = (0, express_1.default)();
// 中间件
app.use((0, helmet_1.default)());
app.use((0, cors_1.default)());
app.use(express_1.default.json());
app.use(express_1.default.urlencoded({ extended: true }));
// 依赖注入
const userRepository = new repository_1.UserRepository(database_1.db);
const userService = new service_1.UserService(userRepository, process.env.JWT_SECRET || 'your-secret-key', parseInt(process.env.JWT_EXPIRES_IN || '7200'));
const userController = new controller_1.UserController(userService);
const sportRepository = new repository_2.SportRepository(database_1.db);
const sportService = new service_2.SportService(sportRepository);
const sportController = new controller_2.SportController(sportService);
// 路由
app.use('/v1', (0, user_1.createUserRoutes)(userController));
app.use('/v1', (0, sport_1.createSportRoutes)(sportController));
// 错误处理中间件
app.use(errorHandler_1.errorHandler);
exports.default = app;
