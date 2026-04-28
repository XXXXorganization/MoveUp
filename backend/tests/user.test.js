"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
require("jest");
const supertest_1 = __importDefault(require("supertest"));
const express_1 = __importDefault(require("express"));
const user_1 = require("../src/routes/user");
const controller_1 = require("../src/modules/user/controller");
const service_1 = require("../src/modules/user/service");
const repository_1 = require("../src/modules/user/repository");
// Mock database
const mockDb = {
    select: jest.fn().mockReturnThis(),
    from: jest.fn().mockReturnThis(),
    where: jest.fn().mockReturnThis(),
    first: jest.fn().mockResolvedValue(null),
    insert: jest.fn().mockResolvedValue([1]),
    update: jest.fn().mockResolvedValue(1),
};
// Mock UserRepository
jest.mock('../src/modules/user/repository');
const MockUserRepository = repository_1.UserRepository;
// Mock UserService
jest.mock('../src/modules/user/service');
const MockUserService = service_1.UserService;
describe('User Module Tests', () => {
    let app;
    let userController;
    let userService;
    let userRepository;
    beforeEach(() => {
        // Reset mocks
        MockUserRepository.mockClear();
        MockUserService.mockClear();
        // Create instances
        userRepository = new MockUserRepository(mockDb);
        userService = new MockUserService(userRepository, 'test-secret', 3600);
        userController = new controller_1.UserController(userService);
        // Setup app
        app = (0, express_1.default)();
        app.use(express_1.default.json());
        app.use('/v1', (0, user_1.createUserRoutes)(userController));
    });
    describe('POST /v1/auth/code - Send Verification Code', () => {
        it('should send verification code successfully', async () => {
            userService.sendVerificationCode = jest.fn().mockResolvedValue(undefined);
            const response = await (0, supertest_1.default)(app)
                .post('/v1/auth/code')
                .send({ phone: '13800138000', type: 'login' });
            expect(response.status).toBe(200);
            expect(response.body).toEqual({
                code: 200,
                message: '验证码发送成功'
            });
            expect(userService.sendVerificationCode).toHaveBeenCalledWith({
                phone: '13800138000',
                type: 'login'
            });
        });
        it('should handle service errors', async () => {
            userService.sendVerificationCode = jest.fn().mockRejectedValue(new Error('Service error'));
            const response = await (0, supertest_1.default)(app)
                .post('/v1/auth/code')
                .send({ phone: '13800138000', type: 'login' });
            expect(response.status).toBe(500);
        });
    });
    describe('POST /v1/auth/login - User Login', () => {
        it('should login successfully', async () => {
            const mockLoginResponse = { token: 'jwt-token', user: { id: 1, phone: '13800138000' } };
            userService.login = jest.fn().mockResolvedValue(mockLoginResponse);
            const response = await (0, supertest_1.default)(app)
                .post('/v1/auth/login')
                .send({ phone: '13800138000', code: '123456' });
            expect(response.status).toBe(200);
            expect(response.body).toEqual({
                code: 200,
                message: '登录成功',
                data: mockLoginResponse
            });
            expect(userService.login).toHaveBeenCalledWith({
                phone: '13800138000',
                code: '123456'
            });
        });
        it('should handle invalid code', async () => {
            userService.login = jest.fn().mockRejectedValue(new Error('验证码无效'));
            const response = await (0, supertest_1.default)(app)
                .post('/v1/auth/login')
                .send({ phone: '13800138000', code: 'wrong' });
            expect(response.status).toBe(500);
        });
    });
    // Add more tests for getUserProfile and updateUserProfile
    // Note: These require authentication middleware mocking
});
