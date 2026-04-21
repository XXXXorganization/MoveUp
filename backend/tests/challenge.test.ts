// tests/challenge.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

describe('挑战与激励模块 API 测试', () => {
  let authToken: string;
  let userId: string;
  let badgeId: string;
  let taskId: string;
  let challengeId: string;
  let membershipPlanId: string;
  const testPhone = '13800139001';

  const cleanup = async () => {
    const user = await db('users').where({ phone: testPhone }).first();
    if (!user) return;
    await db('points_logs').where({ user_id: user.id }).del();
    await db('user_memberships').where({ user_id: user.id }).del();
    await db('user_challenges').where({ user_id: user.id }).del();
    await db('user_achievements').where({ user_id: user.id }).del();
    await db('user_tasks').where({ user_id: user.id }).del();
  };

  beforeAll(async () => {
    await db.raw('SELECT 1');
    await cleanup();
    authToken = await createTestUserAndGetToken(testPhone);
    const user = await db('users').where({ phone: testPhone }).first();
    userId = user.id;

    // 插入测试徽章
    const [badge] = await db('badges').insert({
      name: '测试徽章', description: '测试用', condition_type: 'distance_total',
      condition_value: 100, rarity: 'common',
    }).returning('id');
    badgeId = badge.id ?? badge;

    // 插入测试任务
    const [task] = await db('tasks').insert({
      name: '测试日常任务', task_type: 'daily',
      requirement: JSON.stringify({ target: 5, unit: 'km' }),
      reward_points: 50, reward_badge_id: badgeId, is_active: true,
    }).returning('id');
    taskId = task.id ?? task;

    // 插入测试挑战（进行中）
    const now = new Date();
    const end = new Date(now); end.setDate(end.getDate() + 7);
    const [challenge] = await db('challenges').insert({
      name: '测试挑战赛', challenge_type: 'distance',
      start_time: now, end_time: end,
      target_value: 50000, reward_points: 200, reward_badge_id: badgeId,
    }).returning('id');
    challengeId = challenge.id ?? challenge;

    // 插入测试会员套餐
    const [plan] = await db('membership_plans').insert({
      name: '测试月度会员', duration_days: 30, price: 29.9,
      features: JSON.stringify(['专属计划', '高级数据']), is_active: true,
    }).returning('id');
    membershipPlanId = plan.id ?? plan;
  });

  afterAll(async () => {
    await cleanup();
    await db('membership_plans').where({ id: membershipPlanId }).del();
    await db('challenges').where({ id: challengeId }).del();
    await db('tasks').where({ id: taskId }).del();
    await db('badges').where({ id: badgeId }).del();
    await db('users').where({ phone: testPhone }).del();
    await db.destroy();
  });

  // ==================== 任务系统 ====================

  describe('GET /v1/challenge/tasks - 获取任务列表', () => {
    it('应该成功获取所有任务', async () => {
      const res = await request(app)
        .get('/v1/challenge/tasks')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('应该按类型过滤任务', async () => {
      const res = await request(app)
        .get('/v1/challenge/tasks?type=daily')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      res.body.data.forEach((t: any) => expect(t.taskType).toBe('daily'));
    });

    it('未认证时应该返回 401', async () => {
      const res = await request(app).get('/v1/challenge/tasks');
      expect(res.status).toBe(401);
    });
  });

  describe('PUT /v1/challenge/tasks/progress - 更新任务进度', () => {
    it('应该成功更新任务进度（未完成）', async () => {
      const res = await request(app)
        .put('/v1/challenge/tasks/progress')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ taskId, progress: 3 });
      expect(res.status).toBe(200);
      expect(res.body.data.progress).toBe(3);
      expect(res.body.data.isCompleted).toBe(false);
    });

    it('进度达标时应该自动完成并发放积分', async () => {
      const res = await request(app)
        .put('/v1/challenge/tasks/progress')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ taskId, progress: 5 });
      expect(res.status).toBe(200);
      expect(res.body.data.isCompleted).toBe(true);

      // 验证积分已发放
      const pointsRes = await request(app)
        .get('/v1/challenge/points')
        .set('Authorization', `Bearer ${authToken}`);
      expect(pointsRes.body.data.totalPoints).toBeGreaterThanOrEqual(50);
    });

    it('任务已完成时再次更新应该返回 409', async () => {
      const res = await request(app)
        .put('/v1/challenge/tasks/progress')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ taskId, progress: 5 });
      expect(res.status).toBe(409);
    });

    it('缺少参数时应该返回 400', async () => {
      const res = await request(app)
        .put('/v1/challenge/tasks/progress')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ taskId });
      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/challenge/tasks/my - 获取我的任务', () => {
    it('应该返回用户任务列表（含任务详情）', async () => {
      const res = await request(app)
        .get('/v1/challenge/tasks/my')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0]).toHaveProperty('task');
    });
  });

  // ==================== 成就徽章 ====================

  describe('GET /v1/challenge/badges - 获取徽章列表', () => {
    it('应该成功获取所有徽章', async () => {
      const res = await request(app)
        .get('/v1/challenge/badges')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });
  });

  describe('GET /v1/challenge/badges/my - 获取我的成就', () => {
    it('完成任务后应该获得徽章', async () => {
      const res = await request(app)
        .get('/v1/challenge/badges/my')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.some((a: any) => a.badgeId === badgeId)).toBe(true);
      expect(res.body.data[0]).toHaveProperty('badge');
    });
  });

  // ==================== 挑战赛 ====================

  describe('GET /v1/challenge/challenges - 获取挑战列表', () => {
    it('应该获取进行中的挑战', async () => {
      const res = await request(app)
        .get('/v1/challenge/challenges?status=ongoing')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('应该获取即将开始的挑战', async () => {
      const res = await request(app)
        .get('/v1/challenge/challenges?status=upcoming')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });
  });

  describe('GET /v1/challenge/challenges/:challengeId - 获取挑战详情', () => {
    it('应该成功获取挑战详情', async () => {
      const res = await request(app)
        .get(`/v1/challenge/challenges/${challengeId}`)
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.id).toBe(challengeId);
    });

    it('挑战不存在时应该返回 404', async () => {
      const res = await request(app)
        .get('/v1/challenge/challenges/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(404);
    });
  });

  describe('POST /v1/challenge/challenges/:challengeId/join - 参与挑战', () => {
    it('应该成功参与挑战', async () => {
      const res = await request(app)
        .post(`/v1/challenge/challenges/${challengeId}/join`)
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(201);
      expect(res.body.data).toHaveProperty('challenge');
    });

    it('重复参与应该返回 409', async () => {
      const res = await request(app)
        .post(`/v1/challenge/challenges/${challengeId}/join`)
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(409);
    });
  });

  describe('PUT /v1/challenge/challenges/:challengeId/progress - 更新挑战进度', () => {
    it('应该成功更新挑战进度', async () => {
      const res = await request(app)
        .put(`/v1/challenge/challenges/${challengeId}/progress`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({ progress: 10000 });
      expect(res.status).toBe(200);
      expect(res.body.data.progress).toBe(10000);
    });

    it('缺少 progress 参数时应该返回 400', async () => {
      const res = await request(app)
        .put(`/v1/challenge/challenges/${challengeId}/progress`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({});
      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/challenge/challenges/:challengeId/ranking - 获取挑战排名', () => {
    it('应该成功获取排名列表', async () => {
      const res = await request(app)
        .get(`/v1/challenge/challenges/${challengeId}/ranking`)
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data[0]).toHaveProperty('rank');
      expect(res.body.data[0]).toHaveProperty('progress');
    });
  });

  describe('GET /v1/challenge/challenges/my - 获取我的挑战', () => {
    it('应该返回用户参与的挑战列表', async () => {
      const res = await request(app)
        .get('/v1/challenge/challenges/my')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.some((uc: any) => uc.challengeId === challengeId)).toBe(true);
    });
  });

  // ==================== 会员服务 ====================

  describe('GET /v1/challenge/membership/plans - 获取会员套餐', () => {
    it('应该成功获取套餐列表', async () => {
      const res = await request(app)
        .get('/v1/challenge/membership/plans')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data[0]).toHaveProperty('features');
    });
  });

  describe('GET /v1/challenge/membership/my - 获取会员状态', () => {
    it('未购买时应该返回 null', async () => {
      const res = await request(app)
        .get('/v1/challenge/membership/my')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeNull();
    });
  });

  describe('POST /v1/challenge/membership/purchase - 购买会员', () => {
    it('应该成功购买会员', async () => {
      const res = await request(app)
        .post('/v1/challenge/membership/purchase')
        .set('Authorization', `Bearer ${authToken}`)
        .send({ planId: membershipPlanId });
      expect(res.status).toBe(201);
      expect(res.body.data).toHaveProperty('daysRemaining');
      expect(res.body.data.daysRemaining).toBeGreaterThan(0);
      expect(res.body.data.plan.name).toBe('测试月度会员');
    });

    it('购买后应该能查到会员状态', async () => {
      const res = await request(app)
        .get('/v1/challenge/membership/my')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).not.toBeNull();
      expect(res.body.data.planId).toBe(membershipPlanId);
    });

    it('缺少 planId 时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/challenge/membership/purchase')
        .set('Authorization', `Bearer ${authToken}`)
        .send({});
      expect(res.status).toBe(400);
    });
  });

  // ==================== 积分 ====================

  describe('GET /v1/challenge/points - 获取积分汇总', () => {
    it('应该返回总积分', async () => {
      const res = await request(app)
        .get('/v1/challenge/points')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('totalPoints');
      expect(res.body.data.totalPoints).toBeGreaterThanOrEqual(0);
    });
  });

  describe('GET /v1/challenge/points/logs - 获取积分记录', () => {
    it('应该返回积分流水列表', async () => {
      const res = await request(app)
        .get('/v1/challenge/points/logs')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('每条记录应包含必要字段', async () => {
      const res = await request(app)
        .get('/v1/challenge/points/logs')
        .set('Authorization', `Bearer ${authToken}`);
      if (res.body.data.length > 0) {
        expect(res.body.data[0]).toHaveProperty('pointsChange');
        expect(res.body.data[0]).toHaveProperty('sourceType');
      }
    });
  });
});
