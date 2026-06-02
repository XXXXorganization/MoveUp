// tests/compatibility.integration.test.ts
import request from 'supertest';
import app from '../src/app';
import { db } from '../src/config/database';
import { createTestUserAndGetToken } from './helpers/auth-helper';

describe('Compatibility Routes Integration Tests', () => {
  let authToken: string;
  const testPhone = '13800138003';

  beforeAll(async () => {
    await db.raw('SELECT 1');
  });

  beforeEach(async () => {
    authToken = await createTestUserAndGetToken(testPhone);
  });

  afterAll(async () => {
    await db.destroy();
  });

  describe('Plan routes', () => {
    it('GET /v1/plan/total_distance returns weekly distance', async () => {
      const res = await request(app).get('/v1/plan/total_distance').set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('total_distance');
    });

    it('POST + GET /v1/plan/details - add and fetch plan items', async () => {
      const addRes = await request(app).post('/v1/plan/details').set('Authorization', `Bearer ${authToken}`)
        .send({ day: 'MONDAY', start_time: '07:00', end_time: '08:00', distance: '5' });
      expect(addRes.status).toBe(200);

      const getRes = await request(app).get('/v1/plan/details?day=MONDAY').set('Authorization', `Bearer ${authToken}`);
      expect(getRes.status).toBe(200);
      expect(getRes.body.data.list.length).toBeGreaterThanOrEqual(1);
    });

    it('PUT /v1/plan/toggle_complete - toggle plan completion', async () => {
      await request(app).post('/v1/plan/details').set('Authorization', `Bearer ${authToken}`)
        .send({ day: 'TUESDAY', start_time: '08:00', distance: '3' });

      const toggleRes = await request(app).put('/v1/plan/toggle_complete').set('Authorization', `Bearer ${authToken}`)
        .send({ day: 'TUESDAY', index: 0 });
      expect(toggleRes.status).toBe(200);
      expect(toggleRes.body.data.is_completed).toBe(true);

      const detailsRes = await request(app).get('/v1/plan/details?day=TUESDAY').set('Authorization', `Bearer ${authToken}`);
      expect(detailsRes.body.data.list[0].is_completed).toBe(true);
    });

    it('DELETE /v1/plan/details/delete - delete plan item', async () => {
      await request(app).post('/v1/plan/details').set('Authorization', `Bearer ${authToken}`)
        .send({ day: 'WEDNESDAY', start_time: '09:00', distance: '2' });

      const delRes = await request(app).post('/v1/plan/details/delete').set('Authorization', `Bearer ${authToken}`)
        .send({ day: 'WEDNESDAY', index: 0 });
      expect(delRes.status).toBe(200);

      const getRes = await request(app).get('/v1/plan/details?day=WEDNESDAY').set('Authorization', `Bearer ${authToken}`);
      expect(getRes.body.data.list.length).toBe(0);
    });
  });

  describe('AI chat route', () => {
    it('POST /v1/ai/chat returns reply', async () => {
      const res = await request(app).post('/v1/ai/chat').set('Authorization', `Bearer ${authToken}`)
        .send({ chat_history: [{ role: 'user', content: 'Hi!' }] });
      expect(res.status).toBe(200);
      expect(res.body.data.reply).toBeDefined();
    });
  });

  describe('Run routes', () => {
    it('POST /v1/runs/start + finish full flow', async () => {
      // Start
      const startRes = await request(app).post('/v1/runs/start').set('Authorization', `Bearer ${authToken}`);
      expect(startRes.status).toBe(200);
      const runId = startRes.body.data.run_id;
      expect(runId).toBeDefined();

      // Upload GPS points
      const pointsRes = await request(app).post(`/v1/runs/${runId}/points`).set('Authorization', `Bearer ${authToken}`)
        .send({ points: [{ lat: 30.3, lng: 120.4, timestamp: new Date().toISOString(), speed: 3.5 }] });
      expect(pointsRes.status).toBe(200);

      // Finish
      const finishRes = await request(app).post('/v1/runs/finish').set('Authorization', `Bearer ${authToken}`)
        .send({ run_id: runId });
      expect(finishRes.status).toBe(200);
      expect(finishRes.body.data).toHaveProperty('distance');
    });

    it('GET /v1/runs returns run history', async () => {
      const res = await request(app).get('/v1/runs').set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.list).toBeDefined();
    });
  });
});
