// tests/club.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

describe('Club Module API Tests', () => {
  let authToken: string;
  let clubId: string;
  let postId: string;
  const testPhone = '13800138002';

  beforeAll(async () => {
    await db.raw('SELECT 1');
  });

  beforeEach(async () => {
    authToken = await createTestUserAndGetToken(testPhone);
    // Clean up test data in order
    await db('club_comments').del();
    await db('club_post_likes').del();
    await db('club_posts').del();
    await db('club_members').del();
    await db('clubs').del();
    // Seed test club
    const [club] = await db('clubs').insert({
      name: 'Test Club', description: 'A test club', location: 'Test City', flag: 'CN',
    }).returning('*');
    clubId = club.id;
  });

  afterAll(async () => {
    await db.destroy();
  });

  describe('GET /v1/clubs', () => {
    it('should return club list', async () => {
      const res = await request(app).get('/v1/clubs').set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.list).toBeDefined();
    });
  });

  describe('GET /v1/clubs/:id', () => {
    it('should return club detail', async () => {
      const res = await request(app).get(`/v1/clubs/${clubId}`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.name).toBe('Test Club');
    });
  });

  describe('POST /v1/clubs/:id/toggle', () => {
    it('should join club', async () => {
      const res = await request(app).post(`/v1/clubs/${clubId}/toggle`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.joined).toBe(true);
    });

    it('should leave club when already joined', async () => {
      await request(app).post(`/v1/clubs/${clubId}/toggle`).set('Authorization', `Bearer ${authToken}`);
      const res = await request(app).post(`/v1/clubs/${clubId}/toggle`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.joined).toBe(false);
    });
  });

  describe('GET /v1/user/clubs', () => {
    it('should return user clubs', async () => {
      const res = await request(app).get('/v1/user/clubs').set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
    });
  });

  describe('POST /v1/clubs/:id/posts', () => {
    it('should create a post', async () => {
      const res = await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Hello club!' });
      expect(res.status).toBe(200);
      expect(res.body.data.content).toBe('Hello club!');
      postId = res.body.data.id;
    });
  });

  describe('GET /v1/clubs/:id/posts', () => {
    it('should return posts with comments', async () => {
      await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Test post' });
      const res = await request(app).get(`/v1/clubs/${clubId}/posts`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.list).toBeDefined();
    });
  });

  describe('POST /v1/posts/:id/like', () => {
    it('should toggle like on a post', async () => {
      const createRes = await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Like me' });
      const pid = createRes.body.data.id;

      const res = await request(app).post(`/v1/posts/${pid}/like`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.is_liked).toBe(true);
    });
  });

  describe('POST /v1/posts/:id/comment', () => {
    it('should add comment to post', async () => {
      const createRes = await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Comment test' });
      const pid = createRes.body.data.id;

      const res = await request(app).post(`/v1/posts/${pid}/comment`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Great post!' });
      expect(res.status).toBe(200);
      expect(res.body.data.content).toBe('Great post!');
    });

    it('should reject empty comment', async () => {
      const createRes = await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'test' });
      const pid = createRes.body.data.id;

      const res = await request(app).post(`/v1/posts/${pid}/comment`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: '' });
      expect(res.status).toBe(400);
    });
  });

  describe('GET /v1/posts/:id/comments', () => {
    it('should return post comments', async () => {
      const createRes = await request(app).post(`/v1/clubs/${clubId}/posts`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Post' });
      const pid = createRes.body.data.id;

      await request(app).post(`/v1/posts/${pid}/comment`)
        .set('Authorization', `Bearer ${authToken}`).send({ content: 'Comment 1' });

      const res = await request(app).get(`/v1/posts/${pid}/comments`).set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.list).toBeDefined();
    });
  });
});
