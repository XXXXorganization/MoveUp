"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
// tests/sport.test.ts
const supertest_1 = __importDefault(require("supertest"));
const app_1 = __importDefault(require("../src/app"));
const database_1 = require("../src/config/database");
describe('运动数据模块 API 测试', () => {
    let authToken;
    let recordId;
    const testPhone = '13800138001';
    const testCode = '123456';
    beforeAll(async () => {
        // 确保数据库连接
        await database_1.db.raw('SELECT 1');
    });
    beforeEach(async () => {
        // 清理测试数据
        await (0, database_1.db)('heart_rates').del();
        await (0, database_1.db)('gps_points').del();
        await (0, database_1.db)('sport_records').del();
    });
    afterAll(async () => {
        await database_1.db.destroy();
    });
    /**
     * 辅助函数：获取认证 token
     */
    async function getAuthToken() {
        // 先发送验证码
        await (0, supertest_1.default)(app_1.default)
            .post('/v1/auth/code')
            .send({ phone: testPhone, type: 'login' });
        // 登录获取 token
        const loginRes = await (0, supertest_1.default)(app_1.default)
            .post('/v1/auth/login')
            .send({ phone: testPhone, code: testCode });
        return loginRes.body.data.token;
    }
    describe('POST /v1/sport/start - 开始运动记录', () => {
        it('应该成功创建运动记录', async () => {
            authToken = await getAuthToken();
            const res = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(201);
            expect(res.body.code).toBe(200);
            expect(res.body.data).toHaveProperty('id');
            expect(res.body.data.status).toBe('active');
            expect(res.body.data.distance).toBe(0);
            expect(res.body.data.calories).toBe(0);
            recordId = res.body.data.id;
        });
        it('用户已有进行中的记录时应该失败', async () => {
            authToken = await getAuthToken();
            // 创建第一条记录
            await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            // 尝试创建第二条记录
            const res = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(400);
            expect(res.body).toHaveProperty('message');
        });
    });
    describe('PUT /v1/sport/:recordId/update - 更新运动记录', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
        });
        it('应该成功更新 GPS 轨迹点', async () => {
            const gpsPoints = [
                {
                    recordId,
                    latitude: 39.9042,
                    longitude: 116.4074,
                    timestamp: new Date(),
                    speed: 3.5,
                    accuracy: 5,
                },
                {
                    recordId,
                    latitude: 39.9052,
                    longitude: 116.4084,
                    timestamp: new Date(Date.now() + 1000),
                    speed: 3.6,
                    accuracy: 4,
                },
            ];
            const res = await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/update`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ recordId, gpsPoints });
            expect(res.status).toBe(200);
            expect(res.body.data).toHaveProperty('distance');
        });
        it('应该成功更新心率数据', async () => {
            const heartRates = [
                {
                    recordId,
                    timestamp: new Date(),
                    heartRate: 145,
                },
                {
                    recordId,
                    timestamp: new Date(Date.now() + 1000),
                    heartRate: 150,
                },
            ];
            const res = await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/update`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ recordId, heartRates });
            expect(res.status).toBe(200);
            expect(res.body.data.averageHeartRate).toBeDefined();
        });
    });
    describe('POST /v1/sport/:recordId/gps/batch - 批量上传 GPS 轨迹点', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
        });
        it('应该成功批量上传 GPS 轨迹点', async () => {
            const gpsPoints = [
                { recordId, latitude: 39.9042, longitude: 116.4074, timestamp: new Date() },
                { recordId, latitude: 39.9052, longitude: 116.4084, timestamp: new Date(Date.now() + 1000) },
                { recordId, latitude: 39.9062, longitude: 116.4094, timestamp: new Date(Date.now() + 2000) },
            ];
            const res = await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/gps/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ gpsPoints });
            expect(res.status).toBe(200);
            expect(res.body.data.count).toBe(gpsPoints.length);
        });
        it('GPS 点格式错误时应该失败', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/gps/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ gpsPoints: 'invalid' });
            expect(res.status).toBe(400);
        });
    });
    describe('POST /v1/sport/:recordId/heart-rate/batch - 批量上传心率数据', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
        });
        it('应该成功批量上传心率数据', async () => {
            const heartRates = [
                { recordId, timestamp: new Date(), heartRate: 140 },
                { recordId, timestamp: new Date(Date.now() + 1000), heartRate: 145 },
                { recordId, timestamp: new Date(Date.now() + 2000), heartRate: 150 },
            ];
            const res = await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/heart-rate/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ heartRates });
            expect(res.status).toBe(200);
            expect(res.body.data.count).toBe(heartRates.length);
        });
    });
    describe('PUT /v1/sport/:recordId/stop - 停止运动记录', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
            // 添加一些 GPS 点
            await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/gps/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({
                gpsPoints: [
                    { recordId, latitude: 39.9042, longitude: 116.4074, timestamp: new Date() },
                    { recordId, latitude: 39.9052, longitude: 116.4084, timestamp: new Date(Date.now() + 1000) },
                ],
            });
        });
        it('应该成功停止运动记录', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/stop`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data.status).toBe('completed');
            expect(res.body.data.endTime).toBeDefined();
            expect(res.body.data.calories).toBeGreaterThanOrEqual(0);
        });
        it('记录不存在时应该失败', async () => {
            const fakeId = '00000000-0000-0000-0000-000000000000';
            const res = await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${fakeId}/stop`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(404);
        });
        it('记录已结束应该失败', async () => {
            // 先停止记录
            await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/stop`)
                .set('Authorization', `Bearer ${authToken}`);
            // 再次停止
            const res = await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/stop`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(400);
        });
    });
    describe('GET /v1/sport/:recordId/realtime - 获取实时运动数据', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
        });
        it('应该成功获取实时数据', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .get(`/v1/sport/${recordId}/realtime`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data).toHaveProperty('distance');
            expect(res.body.data).toHaveProperty('duration');
            expect(res.body.data).toHaveProperty('pace');
            expect(res.body.data).toHaveProperty('calories');
        });
    });
    describe('GET /v1/sport - 获取用户运动记录列表', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            // 创建一些测试记录
            await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
        });
        it('应该成功获取运动记录列表', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .get('/v1/sport')
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data).toBeInstanceOf(Array);
        });
    });
    describe('GET /v1/sport/:recordId/gps - 获取 GPS 轨迹点', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
            // 添加 GPS 点
            await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/gps/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({
                gpsPoints: [
                    { recordId, latitude: 39.9042, longitude: 116.4074, timestamp: new Date() },
                    { recordId, latitude: 39.9052, longitude: 116.4084, timestamp: new Date(Date.now() + 1000) },
                ],
            });
        });
        it('应该成功获取 GPS 轨迹点', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .get(`/v1/sport/${recordId}/gps`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data.count).toBeGreaterThan(0);
            expect(res.body.data.points).toBeInstanceOf(Array);
        });
    });
    describe('GET /v1/sport/:recordId/heart-rate - 获取心率数据', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
            // 添加心率数据
            await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/heart-rate/batch`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({
                heartRates: [
                    { recordId, timestamp: new Date(), heartRate: 140 },
                    { recordId, timestamp: new Date(Date.now() + 1000), heartRate: 145 },
                ],
            });
        });
        it('应该成功获取心率数据', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .get(`/v1/sport/${recordId}/heart-rate`)
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data.count).toBeGreaterThan(0);
            expect(res.body.data.rates).toBeInstanceOf(Array);
        });
    });
    describe('POST /v1/sport/:recordId/bluetooth/heart-rate - 蓝牙心率设备数据', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
        });
        it('应该成功记录蓝牙心率数据', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/bluetooth/heart-rate`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({
                deviceId: 'HRM-001',
                heartRate: 145,
                batteryLevel: 80,
                timestamp: new Date(),
            });
            expect(res.status).toBe(200);
            expect(res.body.data.heartRate).toBe(145);
        });
        it('数据格式错误时应该失败', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .post(`/v1/sport/${recordId}/bluetooth/heart-rate`)
                .set('Authorization', `Bearer ${authToken}`)
                .send({ deviceId: 'HRM-001' }); // 缺少 heartRate
            expect(res.status).toBe(400);
        });
    });
    describe('GET /v1/sport/stats - 获取用户运动统计', () => {
        beforeEach(async () => {
            authToken = await getAuthToken();
        });
        it('应该成功获取用户统计（无记录）', async () => {
            const res = await (0, supertest_1.default)(app_1.default)
                .get('/v1/sport/stats')
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data.totalDistance).toBe(0);
            expect(res.body.data.totalTime).toBe(0);
        });
        it('应该成功获取用户统计（有记录）', async () => {
            // 创建并结束一条记录
            const startRes = await (0, supertest_1.default)(app_1.default)
                .post('/v1/sport/start')
                .set('Authorization', `Bearer ${authToken}`);
            recordId = startRes.body.data.id;
            await (0, supertest_1.default)(app_1.default)
                .put(`/v1/sport/${recordId}/stop`)
                .set('Authorization', `Bearer ${authToken}`);
            const res = await (0, supertest_1.default)(app_1.default)
                .get('/v1/sport/stats')
                .set('Authorization', `Bearer ${authToken}`);
            expect(res.status).toBe(200);
            expect(res.body.data.totalDistance).toBeGreaterThanOrEqual(0);
        });
    });
});
