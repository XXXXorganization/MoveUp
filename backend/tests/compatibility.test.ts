// tests/compatibility.test.ts
import 'jest';
import request from 'supertest';
import express from 'express';
import { createCompatibilityRoutes } from '../src/routes/compatibility';
import { authenticateToken } from '../src/middleware/auth';

// Mock auth middleware to pass through with a test user
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req: any, _res: any, next: any) => {
    req.user = { userId: 'test-user-id' };
    next();
  },
}));

// Mock sport service
const mockSportService = {
  startSportRecord: jest.fn(),
  batchUploadGpsPoints: jest.fn(),
  stopSportRecord: jest.fn(),
  getSportRecordsByUserId: jest.fn(),
} as any;

// Mock DB that supports db('table').where().orderBy() chain
const createChainable = (resolveVal: any = []) => {
  const chain: any = jest.fn();
  chain.mockReturnValue(chain);
  chain.where = jest.fn().mockReturnValue(chain);
  chain.orderBy = jest.fn().mockReturnValue(chain);
  chain.sum = jest.fn().mockReturnValue(chain);
  chain.select = jest.fn().mockReturnValue(chain);
  chain.first = jest.fn().mockResolvedValue(resolveVal);
  chain.insert = jest.fn().mockResolvedValue([]);
  chain.update = jest.fn().mockResolvedValue(1);
  chain.del = jest.fn().mockResolvedValue(1);
  return chain;
};

const mockDb = createChainable({ total: 0 });

describe('Compatibility Routes', () => {
  let app: express.Application;
  let mockDb: any;

  beforeEach(() => {
    jest.clearAllMocks();
    mockDb = createChainable({ total: 0 });

    app = express();
    app.use(express.json());
    app.use('/v1', createCompatibilityRoutes(mockSportService, mockDb));
  });

  // ========== Plan routes ==========

  describe('Plan Routes', () => {
    it('GET /v1/plan/total_distance should return 0 when no runs', async () => {
      mockDb.first.mockResolvedValue({ total: 0 });
      const res = await request(app).get('/v1/plan/total_distance');
      expect(res.status).toBe(200);
      expect(res.body.data.total_distance).toBe(0);
    });

    it('GET /v1/plan/details should return list', async () => {
      const chain = createChainable([
        { id: '1', start_time: '07:00', end_time: '08:00', distance_km: 5, is_completed: false },
      ]);
      mockDb.where = jest.fn().mockReturnValue(chain);
      const res = await request(app).get('/v1/plan/details?day=MONDAY');
      expect(res.status).toBe(200);
      expect(res.body.data.list).toHaveLength(1);
      expect(res.body.data.list[0].is_completed).toBe(false);
    });

    it('POST /v1/plan/details should add plan item', async () => {
      const res = await request(app).post('/v1/plan/details')
        .send({ day: 'MONDAY', start_time: '07:00', distance: '5' });
      expect(res.status).toBe(200);
      expect(mockDb.insert).toHaveBeenCalled();
    });

    it('POST /v1/plan/details/delete should delete item by index', async () => {
      const delChain = createChainable([{ id: 'p1' }]);
      mockDb.where = jest.fn().mockReturnValue(delChain);
      const res = await request(app).post('/v1/plan/details/delete')
        .send({ day: 'MONDAY', index: 0 });
      expect(res.status).toBe(200);
    });

    it('PUT /v1/plan/toggle_complete should toggle completion', async () => {
      const toggleChain = createChainable([{ id: 'p1', is_completed: false }]);
      mockDb.where = jest.fn().mockReturnValue(toggleChain);
      const res = await request(app).put('/v1/plan/toggle_complete')
        .send({ day: 'MONDAY', index: 0 });
      expect(res.status).toBe(200);
      expect(res.body.data.is_completed).toBe(true);
    });
  });

  // ========== AI chat ==========

  describe('AI Chat', () => {
    it('POST /v1/ai/chat should return reply', async () => {
      const res = await request(app).post('/v1/ai/chat')
        .send({ chat_history: [{ role: 'user', content: 'hello' }] });
      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('reply');
    });
  });

  // ========== Run routes ==========

  describe('Run Routes', () => {
    it('POST /v1/runs/start should start a sport record', async () => {
      mockSportService.startSportRecord.mockResolvedValue({ id: 'run-1', startTime: '2026-01-01T00:00:00Z' });
      const res = await request(app).post('/v1/runs/start').send({ run_type: 'outdoor' });
      expect(res.status).toBe(200);
      expect(res.body.data.run_id).toBe('run-1');
    });

    it('POST /v1/runs/:id/points should upload GPS points', async () => {
      const points = [{ lat: 30.0, lng: 120.0, timestamp: '2026-01-01', speed: 3.5 }];
      const res = await request(app).post('/v1/runs/run-1/points').send({ points });
      expect(res.status).toBe(200);
      expect(mockSportService.batchUploadGpsPoints).toHaveBeenCalled();
    });

    it('POST /v1/runs/finish should stop active record', async () => {
      mockSportService.getSportRecordsByUserId.mockResolvedValue([
        { id: 'run-1', status: 'active' },
      ]);
      mockSportService.stopSportRecord.mockResolvedValue({
        id: 'run-1', distance: 5000, duration: 1800, averagePace: 360, calories: 300,
      });
      const res = await request(app).post('/v1/runs/finish').send({ run_id: 'run-1' });
      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('distance');
    });

    it('GET /v1/runs should return run list', async () => {
      mockSportService.getSportRecordsByUserId.mockResolvedValue([
        { id: 'run-1', startTime: '2026-01-01T00:00:00Z', distance: 5000, duration: 1800, averagePace: 360, calories: 300 },
      ]);
      const res = await request(app).get('/v1/runs');
      expect(res.status).toBe(200);
      expect(res.body.data.list).toHaveLength(1);
    });

    it('POST /v1/runs/:id/points should reject invalid points', async () => {
      const res = await request(app).post('/v1/runs/run-1/points').send({});
      expect(res.status).toBe(400);
    });
  });
});
