// tests/unit/user.password.test.ts
import { UserService } from '../../src/modules/user/service';
import { UserRepository } from '../../src/modules/user/repository';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

jest.mock('jsonwebtoken');
jest.mock('bcryptjs');

const mockRepository: jest.Mocked<UserRepository> = {
  findByPhone: jest.fn(),
  findById: jest.fn(),
  create: jest.fn(),
  update: jest.fn(),
  getUserStats: jest.fn(),
} as any as jest.Mocked<UserRepository>;

function makeService() {
  return new UserService(mockRepository, 'test-secret', 7200);
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe('registerWithPassword', () => {
  it('should create user with hashed password and return token', async () => {
    const newUser = { id: 'u1', phone: '13900000000', nickname: 'test', avatar: null };
    mockRepository.findByPhone.mockResolvedValue(undefined);
    mockRepository.create.mockResolvedValue(newUser as any);
    (bcrypt.hash as jest.Mock).mockResolvedValue('hashed-pw');
    (jwt.sign as jest.Mock).mockReturnValue('new-token');

    const svc = makeService();
    const result = await svc.registerWithPassword('13900000000', 'testuser', '123456');

    expect(bcrypt.hash).toHaveBeenCalledWith('123456', 10);
    expect(mockRepository.create).toHaveBeenCalledWith(expect.objectContaining({
      phone: '13900000000', nickname: 'testuser', password_hash: 'hashed-pw',
    }));
    expect(result.token).toBe('new-token');
    expect(result.user.id).toBe('u1');
  });

  it('should throw if phone already exists', async () => {
    mockRepository.findByPhone.mockResolvedValue({ id: 'existing' } as any);
    const svc = makeService();
    await expect(svc.registerWithPassword('13900000000', 'test', '123456')).rejects.toMatchObject({ code: 400 });
  });
});

describe('loginWithPassword', () => {
  it('should return token if password matches', async () => {
    const user = { id: 'u1', phone: '13900000000', nickname: 'test', avatar: null, password_hash: 'hash' };
    mockRepository.findByPhone.mockResolvedValue(user as any);
    (bcrypt.compare as jest.Mock).mockResolvedValue(true);
    (jwt.sign as jest.Mock).mockReturnValue('login-token');

    const svc = makeService();
    const result = await svc.loginWithPassword('13900000000', '123456');

    expect(bcrypt.compare).toHaveBeenCalledWith('123456', 'hash');
    expect(result.token).toBe('login-token');
  });

  it('should throw if user not found', async () => {
    mockRepository.findByPhone.mockResolvedValue(undefined);
    const svc = makeService();
    await expect(svc.loginWithPassword('13900000000', '123456')).rejects.toMatchObject({ code: 400 });
  });

  it('should throw if user has no password', async () => {
    mockRepository.findByPhone.mockResolvedValue({ id: 'u1', password_hash: null } as any);
    const svc = makeService();
    await expect(svc.loginWithPassword('13900000000', '123456')).rejects.toMatchObject({ code: 400 });
  });

  it('should throw if password is wrong', async () => {
    mockRepository.findByPhone.mockResolvedValue({ id: 'u1', password_hash: 'hash' } as any);
    (bcrypt.compare as jest.Mock).mockResolvedValue(false);
    const svc = makeService();
    await expect(svc.loginWithPassword('13900000000', 'wrong')).rejects.toMatchObject({ code: 400 });
  });
});
