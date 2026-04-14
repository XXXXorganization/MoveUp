// tests/helpers/auth-helper.ts
import jwt from 'jsonwebtoken';
import { db } from '../../src/config/database';

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';
const JWT_EXPIRES_IN = parseInt(process.env.JWT_EXPIRES_IN || '7200');

/**
 * 生成 UUID v4
 */
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * 测试辅助函数：创建测试用户并返回 token
 */
export async function createTestUserAndGetToken(phone: string = '13800138001'): Promise<string> {
  // 查找或创建用户
  let user = await db('users').where({ phone }).first();

  if (!user) {
    // 创建测试用户
    const userId = generateUUID();
    await db('users').insert({
      id: userId,
      phone,
      nickname: `测试用户${phone.slice(-4)}`,
      created_at: new Date(),
      updated_at: new Date(),
    });
    user = await db('users').where({ phone }).first();
  }

  // 生成 JWT token
  const token = jwt.sign(
    { userId: user.id, phone: user.phone },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );

  return token;
}

/**
 * 测试辅助函数：清理测试数据
 */
export async function cleanupTestUser(phone: string): Promise<void> {
  await db('users').where({ phone }).del();
}
