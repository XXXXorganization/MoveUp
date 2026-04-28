// tests/unit/user.service.test.ts
import { UserService } from '../../src/modules/user/service';
import { UserRepository } from '../../src/modules/user/repository';
import { AppError } from '../../src/utils/errors';
import jwt from 'jsonwebtoken';

jest.mock('jsonwebtoken');

const mockRepository: jest.Mocked<UserRepository> = {
  findByPhone: jest.fn(),
  findById: jest.fn(),
  create: jest.fn(),
  update: jest.fn(),
  getUserStats: jest.fn(),
} as unknown as jest.Mocked<UserRepository>;

const JWT_SECRET = 'test-secret';
const JWT_EXPIRES_IN = 86400;

function makeService() {
  return new UserService(mockRepository, JWT_SECRET, JWT_EXPIRES_IN);
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ==================== sendVerificationCode ====================

describe('sendVerificationCode', () => {
  it('应为手机号生成并存储验证码（不抛出错误）', async () => {
    const svc = makeService();
    await expect(svc.sendVerificationCode({ phone: '13800138000', type: 'login' })).resolves.toBeUndefined();
  });
});

// ==================== login ====================

describe('login', () => {
  it('验证码错误时应抛出 400 错误', async () => {
    const svc = makeService();
    await expect(svc.login({ phone: '13800138000', code: '000000' })).rejects.toMatchObject({
      code: 400,
      message: '验证码无效或已过期',
    });
  });

  it('验证码正确时应返回 token 和用户信息', async () => {
    const phone = '13800138001';
    const fakeUser = { id: 'u1', phone, nickname: '用户8001', avatar: null };

    mockRepository.findByPhone.mockResolvedValue(fakeUser as any);
    (jwt.sign as jest.Mock).mockReturnValue('mock-token');

    const svc = makeService();
    await svc.sendVerificationCode({ phone, type: 'login' });

    // 从内部 Map 取出真实验证码（通过 console.log 注入） — 改为直接访问私有字段
    const codes = (svc as any).verificationCodes as Map<string, { code: string; expires: number }>;
    const entry = codes.get(`${phone}_login`)!;

    const result = await svc.login({ phone, code: entry.code });

    expect(result.token).toBe('mock-token');
    expect(result.user.id).toBe('u1');
    expect(result.expires_in).toBe(JWT_EXPIRES_IN);
  });

  it('用户不存在时应自动注册并返回新用户', async () => {
    const phone = '13900139000';
    const newUser = { id: 'u2', phone, nickname: '用户9000', avatar: null };

    mockRepository.findByPhone.mockResolvedValue(undefined);
    mockRepository.create.mockResolvedValue(newUser as any);
    (jwt.sign as jest.Mock).mockReturnValue('new-token');

    const svc = makeService();
    await svc.sendVerificationCode({ phone, type: 'login' });
    const codes = (svc as any).verificationCodes as Map<string, { code: string; expires: number }>;
    const entry = codes.get(`${phone}_login`)!;

    const result = await svc.login({ phone, code: entry.code });

    expect(mockRepository.create).toHaveBeenCalledWith(
      expect.objectContaining({ phone, nickname: '用户9000' }),
    );
    expect(result.user.id).toBe('u2');
  });

  it('验证码过期时应抛出 400 错误', async () => {
    const phone = '13800138002';
    const svc = makeService();
    const codes = (svc as any).verificationCodes as Map<string, { code: string; expires: number }>;

    // 手动注入已过期的验证码
    codes.set(`${phone}_login`, { code: '123456', expires: Date.now() - 1000 });

    await expect(svc.login({ phone, code: '123456' })).rejects.toMatchObject({ code: 400 });
  });

  it('登录成功后验证码应被清除', async () => {
    const phone = '13800138003';
    const fakeUser = { id: 'u3', phone, nickname: '用户8003', avatar: null };

    mockRepository.findByPhone.mockResolvedValue(fakeUser as any);
    (jwt.sign as jest.Mock).mockReturnValue('t');

    const svc = makeService();
    await svc.sendVerificationCode({ phone, type: 'login' });
    const codes = (svc as any).verificationCodes as Map<string, { code: string; expires: number }>;
    const entry = codes.get(`${phone}_login`)!;

    await svc.login({ phone, code: entry.code });

    expect(codes.has(`${phone}_login`)).toBe(false);
  });
});

// ==================== getUserProfile ====================

describe('getUserProfile', () => {
  it('用户不存在时应抛出 404 错误', async () => {
    mockRepository.findById.mockResolvedValue(undefined);
    const svc = makeService();
    await expect(svc.getUserProfile('unknown-id')).rejects.toMatchObject({ code: 404 });
  });

  it('应根据 total_distance 正确计算用户等级', async () => {
    const fakeUser = { id: 'u1', nickname: '测试', avatar: null, gender: null, birthday: null, height: null, weight: null, target_distance: null };
    const fakeStats = { total_distance: 350, total_time: 3600, total_runs: 5 };

    mockRepository.findById.mockResolvedValue(fakeUser as any);
    mockRepository.getUserStats.mockResolvedValue(fakeStats as any);

    const svc = makeService();
    const profile = await svc.getUserProfile('u1');

    // level = floor(350 / 100) + 1 = 4
    expect(profile.level).toBe(4);
  });

  it('应将 target_distance 从米转换为公里', async () => {
    const fakeUser = { id: 'u1', nickname: '测试', avatar: null, gender: null, birthday: null, height: null, weight: null, target_distance: 5000 };
    const fakeStats = { total_distance: 0, total_time: 0, total_runs: 0 };

    mockRepository.findById.mockResolvedValue(fakeUser as any);
    mockRepository.getUserStats.mockResolvedValue(fakeStats as any);

    const svc = makeService();
    const profile = await svc.getUserProfile('u1');

    expect(profile.preferences.target_distance).toBe(5);
  });

  it('target_distance 为空时 preferences 中应为 undefined', async () => {
    const fakeUser = { id: 'u1', nickname: '测试', avatar: null, gender: null, birthday: null, height: null, weight: null, target_distance: null };
    const fakeStats = { total_distance: 0, total_time: 0, total_runs: 0 };

    mockRepository.findById.mockResolvedValue(fakeUser as any);
    mockRepository.getUserStats.mockResolvedValue(fakeStats as any);

    const svc = makeService();
    const profile = await svc.getUserProfile('u1');

    expect(profile.preferences.target_distance).toBeUndefined();
  });
});

// ==================== updateUserProfile ====================

describe('updateUserProfile', () => {
  it('更新成功时应返回更新后的用户', async () => {
    const updated = { id: 'u1', nickname: '新名字' };
    mockRepository.update.mockResolvedValue(updated as any);

    const svc = makeService();
    const result = await svc.updateUserProfile('u1', { nickname: '新名字' });

    expect(result.nickname).toBe('新名字');
  });

  it('用户不存在时应抛出 404 错误', async () => {
    mockRepository.update.mockResolvedValue(undefined);
    const svc = makeService();
    await expect(svc.updateUserProfile('bad-id', { nickname: '测试' })).rejects.toMatchObject({ code: 404 });
  });
});
