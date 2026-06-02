import 'jest';
import request from 'supertest';
import express from 'express';
import { createUserRoutes } from '../src/routes/user';
import { UserController } from '../src/modules/user/controller';
import { UserService } from '../src/modules/user/service';
import { UserRepository } from '../src/modules/user/repository';

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
const MockUserRepository = UserRepository as jest.MockedClass<typeof UserRepository>;

// Mock UserService
jest.mock('../src/modules/user/service');
const MockUserService = UserService as jest.MockedClass<typeof UserService>;

describe('User Module Tests', () => {
  let app: express.Application;
  let userController: UserController;
  let userService: jest.Mocked<UserService>;
  let userRepository: jest.Mocked<UserRepository>;

  beforeEach(() => {
    // Reset mocks
    MockUserRepository.mockClear();
    MockUserService.mockClear();

    // Create instances
    userRepository = new MockUserRepository(mockDb as any) as jest.Mocked<UserRepository>;
    userService = new MockUserService(userRepository, 'test-secret', 3600) as jest.Mocked<UserService>;
    userController = new UserController(userService);

    // Setup app
    app = express();
    app.use(express.json());
    app.use('/v1', createUserRoutes(userController));
  });

  describe('POST /v1/auth/code - Send Verification Code', () => {
    it('should send verification code successfully', async () => {
      userService.sendVerificationCode = jest.fn().mockResolvedValue(undefined);

      const response = await request(app)
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

      const response = await request(app)
        .post('/v1/auth/code')
        .send({ phone: '13800138000', type: 'login' });

      expect(response.status).toBe(500);
    });
  });

  describe('POST /v1/auth/login - User Login', () => {
    it('should login successfully', async () => {
      const mockLoginResponse = { token: 'jwt-token', user: { id: 1, phone: '13800138000' } };
      userService.login = jest.fn().mockResolvedValue(mockLoginResponse);

      const response = await request(app)
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

    it('should handle invalid code as password attempt', async () => {
      userService.loginWithPassword = jest.fn().mockRejectedValue(new Error('密码错误'));

      const response = await request(app)
        .post('/v1/auth/login')
        .send({ phone: '13800138000', code: 'wrongpassword' });

      expect(response.status).toBe(500);
    });
  });

  describe('POST /v1/auth/register - Password Register', () => {
    it('should register new user with password', async () => {
      userService.registerWithPassword = jest.fn().mockResolvedValue({
        token: 'jwt-token', expires_in: 7200,
        user: { id: 'new-id', nickname: 'newuser', avatar: null },
      });

      const res = await request(app).post('/v1/auth/register')
        .send({ phone: '13600000001', username: 'newuser', password: '123456' });
      expect(res.status).toBe(200);
      expect(res.body.data.token).toBe('jwt-token');
    });

    it('should reject missing fields', async () => {
      const res = await request(app).post('/v1/auth/register')
        .send({ phone: '13600000001' });
      expect(res.status).toBe(400);
    });
  });
});