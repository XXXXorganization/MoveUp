// tests/social.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

describe('社交互动模块 API 测试', () => {
  let tokenA: string;
  let tokenB: string;
  let userAId: string;
  let userBId: string;
  let postId: string;
  let commentId: string;

  const phoneA = '13900000001';
  const phoneB = '13900000002';

  const cleanup = async () => {
    const users = await db('users').whereIn('phone', [phoneA, phoneB]).select('id');
    const ids = users.map((u: any) => u.id);
    if (ids.length === 0) return;
    await db('likes').whereIn('user_id', ids).del();
    await db('comments').whereIn('user_id', ids).del();
    await db('posts').whereIn('user_id', ids).del();
    await db('friendships').where(q => q.whereIn('user_id', ids).orWhereIn('friend_id', ids)).del();
  };

  beforeAll(async () => {
    await db.raw('SELECT 1');
    await cleanup();

    tokenA = await createTestUserAndGetToken(phoneA);
    tokenB = await createTestUserAndGetToken(phoneB);

    const userA = await db('users').where({ phone: phoneA }).first();
    const userB = await db('users').where({ phone: phoneB }).first();
    userAId = userA.id;
    userBId = userB.id;
  });

  afterAll(async () => {
    await cleanup();
    await db('users').whereIn('phone', [phoneA, phoneB]).del();
    await db.destroy();
  });

  // ==================== 用户搜索 ====================

  describe('GET /v1/social/users/search - 搜索用户', () => {
    it('应该按昵称搜索到用户', async () => {
      const res = await request(app)
        .get('/v1/social/users/search?keyword=测试用户')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('关键词少于2个字符时应该返回 400', async () => {
      const res = await request(app)
        .get('/v1/social/users/search?keyword=a')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(400);
    });

    it('未认证时应该返回 401', async () => {
      const res = await request(app).get('/v1/social/users/search?keyword=test');
      expect(res.status).toBe(401);
    });
  });

  // ==================== 好友系统 ====================

  describe('POST /v1/social/friends/request - 发送好友请求', () => {
    it('应该成功发送好友请求', async () => {
      const res = await request(app)
        .post('/v1/social/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ friendId: userBId });

      expect(res.status).toBe(200);
      expect(res.body.code).toBe(200);
    });

    it('重复发送应该返回 409', async () => {
      const res = await request(app)
        .post('/v1/social/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ friendId: userBId });

      expect(res.status).toBe(409);
    });

    it('添加自己应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/social/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ friendId: userAId });

      expect(res.status).toBe(400);
    });

    it('缺少 friendId 应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/social/friends/request')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({});

      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/social/friends/requests - 获取好友请求列表', () => {
    it('B 应该能看到 A 的好友请求', async () => {
      const res = await request(app)
        .get('/v1/social/friends/requests')
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.some((r: any) => r.userId === userAId)).toBe(true);
    });
  });

  describe('PUT /v1/social/friends/respond - 处理好友请求', () => {
    it('B 接受 A 的好友请求', async () => {
      const res = await request(app)
        .put('/v1/social/friends/respond')
        .set('Authorization', `Bearer ${tokenB}`)
        .send({ requesterId: userAId, action: 'accept' });

      expect(res.status).toBe(200);
    });

    it('action 参数非法时应该返回 400', async () => {
      const res = await request(app)
        .put('/v1/social/friends/respond')
        .set('Authorization', `Bearer ${tokenB}`)
        .send({ requesterId: userAId, action: 'invalid' });

      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/social/friends - 获取好友列表', () => {
    it('A 的好友列表应该包含 B', async () => {
      const res = await request(app)
        .get('/v1/social/friends')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.some((f: any) => f.userId === userBId)).toBe(true);
    });
  });

  // ==================== 社区内容 ====================

  describe('POST /v1/social/posts - 发布动态', () => {
    it('应该成功发布动态', async () => {
      const res = await request(app)
        .post('/v1/social/posts')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ content: '今天跑了5公里！', tags: ['晨跑打卡', '健康生活'] });

      expect(res.status).toBe(201);
      expect(res.body.data).toHaveProperty('id');
      expect(res.body.data.author).toHaveProperty('nickname');
      postId = res.body.data.id;
    });

    it('内容和图片都为空时应该返回 400', async () => {
      const res = await request(app)
        .post('/v1/social/posts')
        .set('Authorization', `Bearer ${tokenA}`)
        .send({});

      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/social/posts - 获取动态列表', () => {
    it('获取推荐动态', async () => {
      const res = await request(app)
        .get('/v1/social/posts?type=recommend')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('获取关注动态', async () => {
      const res = await request(app)
        .get('/v1/social/posts?type=following')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('每条动态应包含 author 和 isLiked 字段', async () => {
      const res = await request(app)
        .get('/v1/social/posts?type=recommend')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      if (res.body.data.length > 0) {
        expect(res.body.data[0]).toHaveProperty('author');
        expect(res.body.data[0]).toHaveProperty('isLiked');
      }
    });
  });

  describe('GET /v1/social/posts/:postId - 获取动态详情', () => {
    it('应该成功获取动态详情', async () => {
      const res = await request(app)
        .get(`/v1/social/posts/${postId}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data.id).toBe(postId);
    });

    it('动态不存在时应该返回 404', async () => {
      const res = await request(app)
        .get('/v1/social/posts/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(404);
    });
  });

  describe('POST /v1/social/posts/:postId/like - 点赞/取消点赞', () => {
    it('第一次点赞应该返回 liked: true', async () => {
      const res = await request(app)
        .post(`/v1/social/posts/${postId}/like`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data.liked).toBe(true);
      expect(res.body.data.likeCount).toBe(1);
    });

    it('再次点赞应该取消，返回 liked: false', async () => {
      const res = await request(app)
        .post(`/v1/social/posts/${postId}/like`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data.liked).toBe(false);
      expect(res.body.data.likeCount).toBe(0);
    });
  });

  describe('POST /v1/social/posts/:postId/comments - 发表评论', () => {
    it('应该成功发表评论', async () => {
      const res = await request(app)
        .post(`/v1/social/posts/${postId}/comments`)
        .set('Authorization', `Bearer ${tokenB}`)
        .send({ content: '厉害了！' });

      expect(res.status).toBe(201);
      expect(res.body.data).toHaveProperty('id');
      expect(res.body.data.content).toBe('厉害了！');
      expect(res.body.data.author).toHaveProperty('nickname');
      commentId = res.body.data.id;
    });

    it('评论内容为空时应该返回 400', async () => {
      const res = await request(app)
        .post(`/v1/social/posts/${postId}/comments`)
        .set('Authorization', `Bearer ${tokenB}`)
        .send({});

      expect(res.status).toBe(400);
    });

    it('应该支持回复评论（parentId）', async () => {
      const res = await request(app)
        .post(`/v1/social/posts/${postId}/comments`)
        .set('Authorization', `Bearer ${tokenA}`)
        .send({ content: '谢谢！', parentId: commentId });

      expect(res.status).toBe(201);
      expect(res.body.data.parentId).toBe(commentId);
    });
  });

  describe('GET /v1/social/posts/:postId/comments - 获取评论列表', () => {
    it('应该成功获取评论列表', async () => {
      const res = await request(app)
        .get(`/v1/social/posts/${postId}/comments`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0]).toHaveProperty('author');
    });
  });

  describe('DELETE /v1/social/posts/:postId/comments/:commentId - 删除评论', () => {
    it('非评论作者删除应该返回 403', async () => {
      const res = await request(app)
        .delete(`/v1/social/posts/${postId}/comments/${commentId}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(403);
    });

    it('评论作者应该能成功删除', async () => {
      const res = await request(app)
        .delete(`/v1/social/posts/${postId}/comments/${commentId}`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
    });
  });

  describe('GET /v1/social/friends/:friendId/activities - 查看好友动态', () => {
    it('好友应该能查看对方动态', async () => {
      const res = await request(app)
        .get(`/v1/social/friends/${userAId}/activities`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('非好友查看应该返回 403', async () => {
      const tokenC = await createTestUserAndGetToken('13900000003');
      const res = await request(app)
        .get(`/v1/social/friends/${userAId}/activities`)
        .set('Authorization', `Bearer ${tokenC}`);

      expect(res.status).toBe(403);
      await db('users').where({ phone: '13900000003' }).del();
    });
  });

  describe('DELETE /v1/social/posts/:postId - 删除动态', () => {
    it('非作者删除应该返回 403', async () => {
      const res = await request(app)
        .delete(`/v1/social/posts/${postId}`)
        .set('Authorization', `Bearer ${tokenB}`);

      expect(res.status).toBe(403);
    });

    it('作者应该能成功删除动态', async () => {
      const res = await request(app)
        .delete(`/v1/social/posts/${postId}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
    });
  });

  describe('DELETE /v1/social/friends/:friendId - 删除好友', () => {
    it('应该成功删除好友', async () => {
      const res = await request(app)
        .delete(`/v1/social/friends/${userBId}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
    });

    it('删除不存在的好友关系应该返回 404', async () => {
      const res = await request(app)
        .delete(`/v1/social/friends/${userBId}`)
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(404);
    });
  });

  // ==================== 排行榜 ====================

  describe('GET /v1/social/leaderboard - 获取排行榜', () => {
    it('获取好友周榜', async () => {
      const res = await request(app)
        .get('/v1/social/leaderboard?type=weekly&scope=friends')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('获取全球月榜', async () => {
      const res = await request(app)
        .get('/v1/social/leaderboard?type=monthly&scope=global')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      expect(res.body.data).toBeInstanceOf(Array);
    });

    it('排行榜条目应包含必要字段', async () => {
      // 先插入一条运动记录让排行榜有数据
      await db('sport_records').insert({
        user_id: userAId,
        start_time: new Date(),
        end_time: new Date(),
        distance: 5000,
        duration: 1800,
        status: 'completed',
      });

      const res = await request(app)
        .get('/v1/social/leaderboard?type=weekly&scope=global')
        .set('Authorization', `Bearer ${tokenA}`);

      expect(res.status).toBe(200);
      if (res.body.data.length > 0) {
        expect(res.body.data[0]).toHaveProperty('rank');
        expect(res.body.data[0]).toHaveProperty('totalDistance');
        expect(res.body.data[0]).toHaveProperty('runCount');
      }

      await db('sport_records').where({ user_id: userAId }).del();
    });
  });
});
