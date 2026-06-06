// tests/compatibility.test.ts
import 'jest';
import request from 'supertest';
import express from 'express';
import { createCompatibilityRoutes } from '../src/routes/compatibility';

// Mock auth middleware
jest.mock('../src/middleware/auth', () => ({
  authenticateToken: (req: any, _res: any, next: any) => {
    req.user = { userId: 'test-user-id' };
    next();
  },
}));

const mockSportService = {
  startSportRecord: jest.fn(),
  batchUploadGpsPoints: jest.fn(),
  stopSportRecord: jest.fn(),
  getSportRecordsByUserId: jest.fn(),
} as any;

describe('Compatibility Routes', () => {
  let app: express.Application;

  beforeEach(() => {
    jest.clearAllMocks();

    app = express();
    app.use(express.json());
    // Pass undefined as db to skip plan routes (they handle db===undefined gracefully)
    app.use('/v1', createCompatibilityRoutes(mockSportService, undefined));
  });

  describe('AI Chat', () => {
    it('POST /v1/ai/chat should return reply', async () => {
      const res = await request(app).post('/v1/ai/chat')
        .send({ chat_history: [{ role: 'user', content: 'hello' }] });
      expect(res.status).toBe(200);
      expect(res.body.data).toHaveProperty('reply');
    });
  });

  describe('Run Routes', () => {
    it('POST /v1/runs/start should start a sport record', async () => {
      mockSportService.startSportRecord.mockResolvedValue({ id: 'run-1', startTime: new Date().toISOString() });
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

    it('POST /v1/runs/:id/points should reject invalid points', async () => {
      const res = await request(app).post('/v1/runs/run-1/points').send({});
      expect(res.status).toBe(400);
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

    it('POST /v1/runs/finish should handle finish without run_id', async () => {
      mockSportService.getSportRecordsByUserId.mockResolvedValue([
        { id: 'run-auto', status: 'active' },
      ]);
      mockSportService.stopSportRecord.mockResolvedValue({
        id: 'run-auto', distance: 1000, duration: 300, averagePace: 300, calories: 50,
      });
      const res = await request(app).post('/v1/runs/finish').send({});
      expect(res.status).toBe(200);
      expect(res.body.data.id).toBe('run-auto');
    });

    it('GET /v1/runs should return run list', async () => {
      mockSportService.getSportRecordsByUserId.mockResolvedValue([
        { id: 'r1', startTime: '2026-01-01T00:00:00Z', distance: 5000, duration: 1800, averagePace: 360, calories: 300 },
      ]);
      const res = await request(app).get('/v1/runs');
      expect(res.status).toBe(200);
      expect(res.body.data.list).toHaveLength(1);
    });
  });
});
