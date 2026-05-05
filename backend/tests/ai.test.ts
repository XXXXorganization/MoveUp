// tests/ai.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

/**
 * AI 数据总结模块 API 测试
 *
 * 注意：这些测试会调用真实的 DeepSeek API（如果环境变量已配置）。
 * 在 CI/开发环境中建议设置 DEEPSEEK_API_KEY=mock 并 mock getLLMClient。
 */

// Mock LLM 客户端，避免测试时消耗真实 API 余额
jest.mock('../src/utils/llm', () => {
  const mockChat = jest.fn().mockResolvedValue({
    content: '{"summary": "本次跑步表现良好，配速稳定，继续保持！", "suggestions": ["保持每周3次跑步频率", "注意跑后拉伸放松", "下次尝试提速0.5公里"]}',
    usage: { promptTokens: 120, completionTokens: 80 },
  });

  return {
    getLLMClient: () => ({ chat: mockChat }),
    LLMClient: jest.fn().mockImplementation(() => ({ chat: mockChat })),
  };
});

describe('AI 数据总结模块 API 测试', () => {
  let authToken: string;
  const testPhone = '13800138099';

  beforeAll(async () => {
    await db.raw('SELECT 1');
  });

  beforeEach(async () => {
    authToken = await createTestUserAndGetToken(testPhone);
  });

  afterAll(async () => {
    await db.destroy();
  });

  // ==================== POST /v1/ai/sport-summary ====================
  describe('POST /v1/ai/sport-summary - 单次运动 AI 总结', () => {
    const validPayload = {
      distance: 5000,      // 5公里（米）
      duration: 1800,      // 30分钟（秒）
      calories: 350,       // 千卡
      avgPace: 360,        // 6分钟/公里（秒/公里）
      avgHeartRate: 145,   // bpm
      maxHeartRate: 165,   // bpm
      date: '2026-04-20',
    };

    it('应该成功生成单次运动 AI 总结（完整参数）', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send(validPayload);

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
      expect(res.body.message).toBe('AI 总结生成成功');
      expect(res.body.data).toHaveProperty('summary');
      expect(res.body.data).toHaveProperty('suggestions');
      expect(res.body.data).toHaveProperty('generatedAt');
      expect(typeof res.body.data.summary).toBe('string');
      expect(Array.isArray(res.body.data.suggestions)).toBe(true);
      expect(res.body.data.summary.length).toBeGreaterThan(0);
    });

    it('应该成功生成单次运动 AI 总结（仅必要参数）', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          distance: 3000,
          duration: 900,
          calories: 180,
        });

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
      expect(res.body.data).toHaveProperty('summary');
      expect(res.body.data).toHaveProperty('suggestions');
      expect(res.body.data).toHaveProperty('generatedAt');
    });

    it('缺少 distance 参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ duration: 1800, calories: 350 });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('缺少 duration 参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ distance: 5000, calories: 350 });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('缺少 calories 参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ distance: 5000, duration: 1800 });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('未登录时应该返回 401', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .send(validPayload);

      expect(res.status).toBe(401);
    });

    it('token 无效时应该返回 401', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', 'Bearer invalid-token')
        .send(validPayload);

      expect(res.status).toBe(401);
    });

    it('返回的 suggestions 应该是字符串数组', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send(validPayload);

      expect(res.status).toBe(200);
      const { suggestions } = res.body.data;
      expect(Array.isArray(suggestions)).toBe(true);
      suggestions.forEach((s: any) => {
        expect(typeof s).toBe('string');
      });
    });

    it('返回的 generatedAt 应该是合法 ISO 时间字符串', async () => {
      const res = await request(app)
        .post('/v1/ai/sport-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send(validPayload);

      expect(res.status).toBe(200);
      const { generatedAt } = res.body.data;
      expect(new Date(generatedAt).toISOString()).toBe(generatedAt);
    });
  });

  // ==================== POST /v1/ai/history-summary ====================
  describe('POST /v1/ai/history-summary - 历史运动 AI 总结', () => {
    const validRecords = [
      { distance: 5000, duration: 1800, calories: 350, avgPace: 360 },
      { distance: 8000, duration: 2700, calories: 560, avgPace: 337 },
      { distance: 3000, duration: 1200, calories: 210, avgPace: 400 },
    ];

    it('应该成功生成历史运动 AI 总结（带 periodLabel）', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: validRecords, periodLabel: '本周' });

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
      expect(res.body.message).toBe('AI 历史总结生成成功');
      expect(res.body.data).toHaveProperty('summary');
      expect(res.body.data).toHaveProperty('suggestions');
      expect(res.body.data).toHaveProperty('generatedAt');
      expect(typeof res.body.data.summary).toBe('string');
      expect(Array.isArray(res.body.data.suggestions)).toBe(true);
    });

    it('应该成功生成历史运动 AI 总结（不带 periodLabel，默认"近期"）', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: validRecords });

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
      expect(res.body.data).toHaveProperty('summary');
    });

    it('应该成功生成单条记录的历史总结', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          records: [{ distance: 5000, duration: 1800, calories: 350 }],
          periodLabel: '今日',
        });

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('summary');
    });

    it('records 为空数组时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: [], periodLabel: '本周' });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('records 不是数组时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: 'invalid', periodLabel: '本周' });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('缺少 records 参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ periodLabel: '本周' });

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty('message');
    });

    it('未登录时应该返回 401', async () => {
      const res = await request(app)
        .post('/v1/ai/history-summary')
        .send({ records: validRecords, periodLabel: '本周' });

      expect(res.status).toBe(401);
    });

    it('包含心率数据的记录应该正常处理', async () => {
      const recordsWithHR = [
        { distance: 5000, duration: 1800, calories: 350, avgPace: 360, avgHeartRate: 145, maxHeartRate: 165 },
        { distance: 8000, duration: 2700, calories: 560, avgPace: 337, avgHeartRate: 150, maxHeartRate: 170 },
      ];

      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: recordsWithHR, periodLabel: '本月' });

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('summary');
      expect(res.body.data).toHaveProperty('suggestions');
    });

    it('大量记录（20条）应该正常处理', async () => {
      const manyRecords = Array.from({ length: 20 }, (_, i) => ({
        distance: 5000 + i * 100,
        duration: 1800 + i * 30,
        calories: 300 + i * 10,
        avgPace: 360 - i * 2,
      }));

      const res = await request(app)
        .post('/v1/ai/history-summary')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ records: manyRecords, periodLabel: '近三个月' });

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('summary');
    });
  });
});
