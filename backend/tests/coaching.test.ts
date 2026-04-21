// tests/coaching.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

describe('智能指导模块 API 测试', () => {
  let authToken: string;
  let testPlanId: string;
  let userPlanId: string;
  const testPhone = '13800138099';

  beforeAll(async () => {
    await db.raw('SELECT 1');

    // 插入测试用训练计划
    const [plan] = await db('training_plans')
      .insert({
        name: '测试5公里入门计划',
        description: '测试用计划',
        difficulty: 'beginner',
        duration_weeks: 4,
        target_distance: 5000,
        schedule: JSON.stringify([
          {
            week: 1,
            days: [
              { day: 1, type: 'run', distance: 3000, duration: 30, pace: '6:30-7:00', description: '轻松跑' },
              { day: 2, type: 'rest' },
              { day: 3, type: 'run', distance: 3000, duration: 30, pace: '6:30-7:00', description: '轻松跑' },
            ],
          },
          {
            week: 2,
            days: [
              { day: 1, type: 'run', distance: 4000, duration: 35, pace: '6:00-6:30', description: '配速跑' },
            ],
          },
        ]),
      })
      .returning('id');
    testPlanId = plan.id ?? plan;
  });

  beforeEach(async () => {
    authToken = await createTestUserAndGetToken(testPhone);
    await db('user_plans').whereIn(
      'user_id',
      db('users').where({ phone: testPhone }).select('id'),
    ).del();
  });

  afterAll(async () => {
    await db('user_plans').whereIn(
      'user_id',
      db('users').where({ phone: testPhone }).select('id'),
    ).del();
    await db('training_plans').where({ name: '测试5公里入门计划' }).del();
    await db('users').where({ phone: testPhone }).del();
    await db.destroy();
  });

  // ==================== 训练计划 ====================

  describe('GET /v1/coaching/plans - 获取训练计划列表', () => {
    it('应该成功获取所有计划', async () => {
      const res = await request(app)
        .get('/v1/coaching/plans')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('应该按难度过滤计划', async () => {
      const res = await request(app)
        .get('/v1/coaching/plans?difficulty=beginner')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      res.body.data.forEach((plan: any) => {
        expect(plan.difficulty).toBe('beginner');
      });
    });

    it('未认证时应该返回 401', async () => {
      const res = await request(app).get('/v1/coaching/plans');
      expect(res.status).toBe(401);
    });
  });

  describe('GET /v1/coaching/plans/:planId - 获取计划详情', () => {
    it('应该成功获取计划详情', async () => {
      const res = await request(app)
        .get(`/v1/coaching/plans/${testPlanId}`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.id).toBe(testPlanId);
      expect(res.body.data).toHaveProperty('schedule');
      expect(res.body.data.schedule).toBeInstanceOf(Array);
    });

    it('计划不存在时应该返回 404', async () => {
      const res = await request(app)
        .get('/v1/coaching/plans/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(404);
    });
  });

  describe('POST /v1/coaching/plans/recommend - AI 推荐计划', () => {
    it('应该成功返回推荐计划', async () => {
      const res = await request(app)
        .post('/v1/coaching/plans/recommend')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          fitnessLevel: 'beginner',
          targetDistance: 5000,
          weeklyFrequency: 3,
          goalType: 'health',
        });

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.length).toBeLessThanOrEqual(3);
    });

    it('缺少必要参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/coaching/plans/recommend')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ fitnessLevel: 'beginner' });

      expect(res.status).toBe(400);
    });
  });

  describe('POST /v1/coaching/plans/:planId/adopt - 采用训练计划', () => {
    it('应该成功采用计划', async () => {
      const res = await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(201);
      expect(res.body.data).toHaveProperty('id');
      expect(res.body.data.currentWeek).toBe(1);
      expect(res.body.data.currentDay).toBe(1);
      expect(res.body.data.isActive).toBe(true);
      expect(res.body.data.completionRate).toBe(0);
      userPlanId = res.body.data.id;
    });

    it('采用新计划时应自动停用旧计划', async () => {
      // 先采用一次
      await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);

      // 再采用一次（应停用旧的）
      const res = await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(201);

      // 验证只有一个活跃计划
      const activePlans = await db('user_plans').whereIn(
        'user_id',
        db('users').where({ phone: testPhone }).select('id'),
      ).where({ is_active: true });
      expect(activePlans.length).toBe(1);
    });
  });

  // ==================== 用户计划进度 ====================

  describe('GET /v1/coaching/my-plan - 获取当前计划', () => {
    it('有活跃计划时应该成功返回', async () => {
      await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);

      const res = await request(app)
        .get('/v1/coaching/my-plan')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('plan');
      expect(res.body.data).toHaveProperty('completionRate');
      expect(res.body.data.isActive).toBe(true);
    });

    it('无活跃计划时应该返回 404', async () => {
      const res = await request(app)
        .get('/v1/coaching/my-plan')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(404);
    });
  });

  describe('GET /v1/coaching/today-task - 获取今日任务', () => {
    it('应该成功返回今日训练任务', async () => {
      await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);

      const res = await request(app)
        .get('/v1/coaching/today-task')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('week');
      expect(res.body.data).toHaveProperty('day');
      expect(res.body.data).toHaveProperty('task');
      expect(res.body.data).toHaveProperty('completionRate');
    });
  });

  describe('PUT /v1/coaching/my-plan/:userPlanId/progress - 更新训练进度', () => {
    beforeEach(async () => {
      const res = await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);
      userPlanId = res.body.data.id;
    });

    it('应该成功更新进度', async () => {
      const res = await request(app)
        .put(`/v1/coaching/my-plan/${userPlanId}/progress`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({ week: 1, day: 3 });

      expect(res.status).toBe(200);
      expect(res.body.data.currentWeek).toBe(1);
      expect(res.body.data.currentDay).toBe(3);
    });

    it('缺少参数时应该返回 400', async () => {
      const res = await request(app)
        .put(`/v1/coaching/my-plan/${userPlanId}/progress`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({ week: 1 });

      expect(res.status).toBe(400);
    });

    it('计划不存在时应该返回 404', async () => {
      const res = await request(app)
        .put('/v1/coaching/my-plan/00000000-0000-0000-0000-000000000000/progress')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ week: 1, day: 2 });

      expect(res.status).toBe(404);
    });
  });

  describe('DELETE /v1/coaching/my-plan/:userPlanId - 退出计划', () => {
    beforeEach(async () => {
      const res = await request(app)
        .post(`/v1/coaching/plans/${testPlanId}/adopt`)
        .set('Authorization', `Bearer ${authToken}`);
      userPlanId = res.body.data.id;
    });

    it('应该成功退出计划', async () => {
      const res = await request(app)
        .delete(`/v1/coaching/my-plan/${userPlanId}`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);

      // 验证计划已停用
      const plan = await db('user_plans').where({ id: userPlanId }).first();
      expect(plan.is_active).toBe(false);
    });
  });

  // ==================== 语音指导 ====================

  describe('POST /v1/coaching/voice-guidance - 获取语音播报', () => {
    it('到达公里节点时应该返回播报内容', async () => {
      const res = await request(app)
        .post('/v1/coaching/voice-guidance')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ distanceM: 1000, currentPace: 330 });

      expect(res.status).toBe(200);
      expect(res.body.data.messages).toBeInstanceOf(Array);
      expect(res.body.data.messages.length).toBeGreaterThan(0);
    });

    it('未到节点时应该返回空播报', async () => {
      const res = await request(app)
        .post('/v1/coaching/voice-guidance')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ distanceM: 500, currentPace: 330 });

      expect(res.status).toBe(200);
      expect(res.body.data.messages).toBeInstanceOf(Array);
      expect(res.body.data.messages.length).toBe(0);
    });

    it('心率超阈值时应该触发提醒', async () => {
      const res = await request(app)
        .post('/v1/coaching/voice-guidance')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          distanceM: 500,
          currentPace: 330,
          currentHeartRate: 180,
          config: { distanceInterval: 1000, paceAlertEnabled: false, heartRateAlertEnabled: true, heartRateThreshold: 170 },
        });

      expect(res.status).toBe(200);
      expect(res.body.data.messages.some((m: string) => m.includes('心率'))).toBe(true);
    });

    it('缺少必要参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/coaching/voice-guidance')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ distanceM: 1000 });

      expect(res.status).toBe(400);
    });
  });

  describe('POST /v1/coaching/segment-advice - 获取分段教练建议', () => {
    const cases = [
      { heartRate: 100, expectedZone: 'warmup' },
      { heartRate: 130, expectedZone: 'fat_burn' },
      { heartRate: 148, expectedZone: 'aerobic' },
      { heartRate: 165, expectedZone: 'anaerobic' },
      { heartRate: 185, expectedZone: 'max' },
    ];

    cases.forEach(({ heartRate, expectedZone }) => {
      it(`心率 ${heartRate} 应该返回 ${expectedZone} 区间`, async () => {
        const res = await request(app)
          .post('/v1/coaching/segment-advice')
          .set('Authorization', `Bearer ${authToken}`)
          .send({ currentHeartRate: heartRate, age: 25 });

        expect(res.status).toBe(200);
        expect(res.body.data.heartRateZone).toBe(expectedZone);
        expect(res.body.data).toHaveProperty('advice');
        expect(res.body.data).toHaveProperty('shouldSlowDown');
      });
    });

    it('缺少心率参数时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/coaching/segment-advice')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ age: 25 });

      expect(res.status).toBe(400);
    });
  });

  // ==================== 健康建议 ====================

  describe('GET /v1/coaching/stretching - 获取拉伸指导', () => {
    it('跑步时长不足30分钟时应该返回基础拉伸', async () => {
      const res = await request(app)
        .get('/v1/coaching/stretching?runDuration=1200')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.exercises).toBeInstanceOf(Array);
      expect(res.body.data.exercises.length).toBe(4);
    });

    it('跑步时长超过30分钟时应该返回更多拉伸动作', async () => {
      const res = await request(app)
        .get('/v1/coaching/stretching?runDuration=2400')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.exercises.length).toBeGreaterThan(4);
    });

    it('每个拉伸动作应包含必要字段', async () => {
      const res = await request(app)
        .get('/v1/coaching/stretching')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      res.body.data.exercises.forEach((ex: any) => {
        expect(ex).toHaveProperty('name');
        expect(ex).toHaveProperty('duration');
        expect(ex).toHaveProperty('description');
        expect(ex).toHaveProperty('targetMuscle');
      });
    });
  });

  describe('GET /v1/coaching/injury-prevention - 获取伤病预防建议', () => {
    it('应该返回基础预防建议', async () => {
      const res = await request(app)
        .get('/v1/coaching/injury-prevention')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('周跑量超过50km时应该包含过度训练提醒', async () => {
      const res = await request(app)
        .get('/v1/coaching/injury-prevention?weeklyDistance=55000&recentRunCount=3')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      const hasOvertrainingTip = res.body.data.some((t: any) => t.category === 'overtraining');
      expect(hasOvertrainingTip).toBe(true);
    });

    it('连续跑步5天以上时应该包含休息建议', async () => {
      const res = await request(app)
        .get('/v1/coaching/injury-prevention?weeklyDistance=20000&recentRunCount=5')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      const hasRestTip = res.body.data.some((t: any) => t.category === 'overtraining');
      expect(hasRestTip).toBe(true);
    });

    it('每条建议应包含必要字段', async () => {
      const res = await request(app)
        .get('/v1/coaching/injury-prevention')
        .set('Authorization', `Bearer ${authToken}`);

      res.body.data.forEach((tip: any) => {
        expect(tip).toHaveProperty('category');
        expect(tip).toHaveProperty('title');
        expect(tip).toHaveProperty('content');
        expect(tip).toHaveProperty('priority');
      });
    });
  });
});
