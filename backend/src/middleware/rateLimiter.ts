// src/middleware/rateLimiter.ts
import rateLimit from 'express-rate-limit';
import { Request } from 'express';

/**
 * 全局 API 限频
 * 每 IP 每 15 分钟最多 300 次请求
 */
export const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 分钟
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request): string => {
    return req.ip ?? req.socket.remoteAddress ?? 'unknown';
  },
  message: {
    code: 429,
    message: '请求过于频繁，请稍后再试',
    data: null,
  },
});

/**
 * 登录接口严格限频
 * 每 IP 每 15 分钟最多 10 次尝试
 */
export const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request): string => {
    return req.ip ?? req.socket.remoteAddress ?? 'unknown';
  },
  message: {
    code: 429,
    message: '登录尝试次数过多，请15分钟后再试',
    data: null,
  },
});

/**
 * 验证码发送限频
 * 每 IP 每分钟最多 1 次，防止短信轰炸
 */
export const smsLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 分钟
  max: 1,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request): string => {
    const phone = (req.body as { phone?: string })?.phone ?? 'unknown';
    return `${req.ip}_sms_${phone}`;
  },
  message: {
    code: 429,
    message: '验证码发送过于频繁，请1分钟后再试',
    data: null,
  },
});
