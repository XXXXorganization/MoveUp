// tests/unit/sport.service.test.ts
import { SportService } from '../../src/modules/sport/service';
import { SportRepository } from '../../src/modules/sport/repository';
import { GpsPoint, CalorieCalculationParams } from '../../src/modules/sport/types';
import { AppError } from '../../src/utils/errors';

const mockRepository: jest.Mocked<SportRepository> = {
  getActiveSportRecordByUserId: jest.fn(),
  createSportRecord: jest.fn(),
  getSportRecordById: jest.fn(),
  updateSportRecord: jest.fn(),
  insertGpsPoints: jest.fn(),
  insertHeartRates: jest.fn(),
  getGpsPointsByRecordId: jest.fn(),
  getHeartRatesByRecordId: jest.fn(),
  getSportRecordsByUserId: jest.fn(),
  getPaceSegments: jest.fn(),
} as unknown as jest.Mocked<SportRepository>;

function makeService() {
  return new SportService(mockRepository);
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ==================== correctGpsPoints ====================

describe('correctGpsPoints', () => {
  const baseTime = new Date('2024-01-01T08:00:00Z');
  const secondLater = new Date('2024-01-01T08:00:01Z');

  function makePoint(lat: number, lon: number, time: Date, accuracy?: number): Omit<GpsPoint, 'id'> {
    return { recordId: 'r1', latitude: lat, longitude: lon, timestamp: time, accuracy, altitude: 0, speed: 0 };
  }

  it('输入为空时应返回空数组', () => {
    const svc = makeService();
    expect(svc.correctGpsPoints([])).toEqual([]);
  });

  it('始终保留第一个点', () => {
    const svc = makeService();
    const pts = [makePoint(39.9, 116.3, baseTime)];
    const result = svc.correctGpsPoints(pts);
    expect(result).toHaveLength(1);
  });

  it('应过滤精度超过 50m 的漂移点', () => {
    const svc = makeService();
    const pts = [
      makePoint(39.9000, 116.3000, baseTime, 10),
      makePoint(39.9001, 116.3001, secondLater, 100), // 精度差，应被过滤
    ];
    const result = svc.correctGpsPoints(pts);
    expect(result).toHaveLength(1);
  });

  it('应过滤速度超过 8m/s 的异常点', () => {
    const svc = makeService();
    // 两点之间约 1000m，间隔 1 秒 → 速度 ≈ 1000 m/s，超出阈值
    const pts = [
      makePoint(39.9000, 116.3000, baseTime),
      makePoint(39.9100, 116.3100, secondLater), // 距离约 1.3km，1秒内 → 异常
    ];
    const result = svc.correctGpsPoints(pts);
    expect(result).toHaveLength(1);
  });

  it('应保留速度正常的连续点', () => {
    const svc = makeService();
    const t0 = new Date('2024-01-01T08:00:00Z');
    const t1 = new Date('2024-01-01T08:00:10Z'); // 10秒后
    const t2 = new Date('2024-01-01T08:00:20Z'); // 再10秒

    const pts = [
      makePoint(39.90000, 116.30000, t0),
      makePoint(39.90010, 116.30010, t1), // ~14m，10s，速度 1.4m/s
      makePoint(39.90020, 116.30020, t2), // ~14m，10s，速度 1.4m/s
    ];
    const result = svc.correctGpsPoints(pts);
    expect(result).toHaveLength(3);
  });

  it('应同时过滤精度差且速度异常的点', () => {
    const svc = makeService();
    const t0 = new Date('2024-01-01T08:00:00Z');
    const t1 = new Date('2024-01-01T08:00:10Z');
    const t2 = new Date('2024-01-01T08:00:20Z');

    const pts = [
      makePoint(39.90000, 116.30000, t0),
      makePoint(39.90010, 116.30010, t1, 200), // 精度差
      makePoint(39.90020, 116.30020, t2),       // 有效
    ];
    const result = svc.correctGpsPoints(pts);
    // 第二点被过滤，第三点以第一点为前点，速度正常
    expect(result).toHaveLength(2);
  });
});

// ==================== calculateCalories ====================

describe('calculateCalories', () => {
  it('配速 < 4:00/km（<240s/km）时应使用 MET=12', () => {
    const svc = makeService();
    // paceMinPerKm = 230/60 ≈ 3.83 < 4 → MET = 12
    const cal = svc.calculateCalories({ weight: 60, duration: 3600, averagePace: 230 });
    expect(cal).toBeCloseTo(12 * 60 * 1 * 1.0, 0);
  });

  it('配速 4:00-5:00/km（240-300s/km）时应使用 MET=10', () => {
    const svc = makeService();
    // paceMinPerKm = 270/60 = 4.5 → MET = 10
    const cal = svc.calculateCalories({ weight: 60, duration: 3600, averagePace: 270 });
    expect(cal).toBeCloseTo(10 * 60 * 1 * 1.0, 0);
  });

  it('配速 5:00-6:00/km（300-360s/km）时应使用 MET=8.3', () => {
    const svc = makeService();
    // paceMinPerKm = 330/60 = 5.5 → MET = 8.3
    const cal = svc.calculateCalories({ weight: 70, duration: 3600, averagePace: 330 });
    expect(cal).toBeCloseTo(8.3 * 70 * 1 * 1.0, 0);
  });

  it('配速 6:00-7:00/km 时应使用 MET=7', () => {
    const svc = makeService();
    const cal = svc.calculateCalories({ weight: 70, duration: 3600, averagePace: 390 });
    expect(cal).toBeCloseTo(7 * 70 * 1 * 1.0, 0);
  });

  it('配速 > 7:00/km 时应使用 MET=5.5', () => {
    const svc = makeService();
    const cal = svc.calculateCalories({ weight: 70, duration: 3600, averagePace: 450 });
    expect(cal).toBeCloseTo(5.5 * 70 * 1 * 1.0, 0);
  });

  it('有心率数据时应应用心率系数（无氧区 > 1.3x）', () => {
    const svc = makeService();
    // 年龄25，maxHR=195，心率185 → ratio=0.949 > 0.9 → multiplier=1.3
    const cal = svc.calculateCalories({ weight: 70, duration: 3600, averagePace: 330, averageHeartRate: 185, age: 25 });
    const baseMet = 8.3 * 1.3;
    expect(cal).toBeCloseTo(baseMet * 70 * 1, 0);
  });

  it('女性应乘以 0.9 性别系数', () => {
    const svc = makeService();
    const maleCal = svc.calculateCalories({ weight: 60, duration: 3600, averagePace: 330, gender: 1 });
    const femaleCal = svc.calculateCalories({ weight: 60, duration: 3600, averagePace: 330, gender: 0 });
    expect(femaleCal).toBeCloseTo(maleCal * 0.9, 0);
  });

  it('结果应保留一位小数', () => {
    const svc = makeService();
    const cal = svc.calculateCalories({ weight: 70, duration: 1800, averagePace: 330 });
    const str = cal.toString();
    const decimals = str.includes('.') ? str.split('.')[1].length : 0;
    expect(decimals).toBeLessThanOrEqual(1);
  });
});

// ==================== startSportRecord ====================

describe('startSportRecord', () => {
  it('已有进行中记录时应抛出 400 错误', async () => {
    mockRepository.getActiveSportRecordByUserId.mockResolvedValue({ id: 'r1' } as any);
    const svc = makeService();
    await expect(svc.startSportRecord('u1')).rejects.toMatchObject({ code: 400 });
  });

  it('无进行中记录时应创建新记录', async () => {
    mockRepository.getActiveSportRecordByUserId.mockResolvedValue(null);
    mockRepository.createSportRecord.mockResolvedValue({ id: 'r2', status: 'active' } as any);

    const svc = makeService();
    const record = await svc.startSportRecord('u1');

    expect(mockRepository.createSportRecord).toHaveBeenCalledWith(
      expect.objectContaining({ userId: 'u1', status: 'active', distance: 0, calories: 0 }),
    );
    expect(record.id).toBe('r2');
  });
});

// ==================== stopSportRecord ====================

describe('stopSportRecord', () => {
  it('记录不存在时应抛出 404 错误', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    const svc = makeService();
    await expect(svc.stopSportRecord('bad-id')).rejects.toMatchObject({ code: 404 });
  });

  it('记录已结束时应抛出 400 错误', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'completed', startTime: new Date() } as any);
    const svc = makeService();
    await expect(svc.stopSportRecord('r1')).rejects.toMatchObject({ code: 400 });
  });

  it('正常停止时应更新状态为 completed', async () => {
    const startTime = new Date(Date.now() - 30000); // 30秒前开始
    const activeRecord = { id: 'r1', status: 'active', startTime };
    const finalRecord = { ...activeRecord, status: 'completed', distance: 0, duration: 30, calories: 0 };

    mockRepository.getSportRecordById.mockResolvedValue(activeRecord as any);
    mockRepository.getGpsPointsByRecordId.mockResolvedValue([]);
    mockRepository.getHeartRatesByRecordId.mockResolvedValue([]);
    mockRepository.updateSportRecord.mockResolvedValue({ ...finalRecord, status: 'completed' } as any);

    const svc = makeService();
    const result = await svc.stopSportRecord('r1');

    expect(mockRepository.updateSportRecord).toHaveBeenCalledWith(
      'r1',
      expect.objectContaining({ status: 'completed' }),
    );
    expect(result.status).toBe('completed');
  });
});

// ==================== updateSportRecord ====================

describe('updateSportRecord', () => {
  it('记录不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().updateSportRecord('r1', { recordId: 'r1' } as any)).rejects.toMatchObject({ code: 404 });
  });

  it('记录已结束时应抛出 400', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'completed', startTime: new Date() } as any);
    await expect(makeService().updateSportRecord('r1', { recordId: 'r1' } as any)).rejects.toMatchObject({ code: 400 });
  });

  it('有 GPS 点时应执行纠偏并插入', async () => {
    const startTime = new Date(Date.now() - 10000);
    const activeRecord = { id: 'r1', status: 'active', startTime };
    mockRepository.getSportRecordById.mockResolvedValue(activeRecord as any);
    mockRepository.insertGpsPoints.mockResolvedValue([]);
    mockRepository.getGpsPointsByRecordId.mockResolvedValue([]);
    mockRepository.getHeartRatesByRecordId.mockResolvedValue([]);
    mockRepository.updateSportRecord.mockResolvedValue({ id: 'r1', status: 'active', startTime } as any);

    const gpsPoints = [{ recordId: 'r1', latitude: 39.9, longitude: 116.3, timestamp: new Date(), accuracy: 10, altitude: 0, speed: 0 }];
    await makeService().updateSportRecord('r1', { recordId: 'r1', gpsPoints } as any);
    expect(mockRepository.insertGpsPoints).toHaveBeenCalled();
  });
});

// ==================== batchUploadGpsPoints ====================

describe('batchUploadGpsPoints', () => {
  it('记录不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().batchUploadGpsPoints('r1', [])).rejects.toMatchObject({ code: 404 });
  });

  it('记录已结束时应抛出 400', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'completed', startTime: new Date() } as any);
    await expect(makeService().batchUploadGpsPoints('r1', [])).rejects.toMatchObject({ code: 400 });
  });

  it('正常时应纠偏并返回 GPS 点', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'active', startTime: new Date() } as any);
    mockRepository.insertGpsPoints.mockResolvedValue([{ id: 'g1' }] as any);
    const result = await makeService().batchUploadGpsPoints('r1', []);
    expect(mockRepository.insertGpsPoints).toHaveBeenCalled();
  });
});

// ==================== uploadHeartRateData ====================

describe('uploadHeartRateData', () => {
  it('记录不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().uploadHeartRateData('r1', [])).rejects.toMatchObject({ code: 404 });
  });

  it('记录已结束时应抛出 400', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'completed', startTime: new Date() } as any);
    await expect(makeService().uploadHeartRateData('r1', [])).rejects.toMatchObject({ code: 400 });
  });
});

// ==================== getRealTimeData ====================

describe('getRealTimeData', () => {
  it('记录不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().getRealTimeData('r1')).rejects.toMatchObject({ code: 404 });
  });

  it('应返回实时运动指标', async () => {
    const startTime = new Date(Date.now() - 60000);
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'active', startTime } as any);
    mockRepository.getGpsPointsByRecordId.mockResolvedValue([]);
    mockRepository.getHeartRatesByRecordId.mockResolvedValue([{ heartRate: 150 }] as any);

    const result = await makeService().getRealTimeData('r1');
    expect(result.heartRate).toBe(150);
    expect(result.duration).toBeGreaterThan(0);
  });
});

// ==================== getSportRecordStats ====================

describe('getSportRecordStats', () => {
  it('记录不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().getSportRecordStats('r1')).rejects.toMatchObject({ code: 404 });
  });

  it('应返回统计数据', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({
      id: 'r1', distance: 5000, duration: 1800, calories: 300,
      averagePace: 360, maxHeartRate: 175, averageHeartRate: 155,
    } as any);
    mockRepository.getGpsPointsByRecordId.mockResolvedValue([]);
    mockRepository.getHeartRatesByRecordId.mockResolvedValue([]);

    const stats = await makeService().getSportRecordStats('r1');
    expect(stats.totalDistance).toBe(5000);
    expect(stats.averagePace).toBe(360);
  });
});

// ==================== getUserSportStats ====================

describe('getUserSportStats', () => {
  it('无完成记录时应返回全零统计', async () => {
    mockRepository.getSportRecordsByUserId.mockResolvedValue([
      { status: 'active', distance: 1000, duration: 300, calories: 50 } as any,
    ]);
    const stats = await makeService().getUserSportStats('u1');
    expect(stats.totalDistance).toBe(0);
    expect(stats.totalCalories).toBe(0);
  });

  it('有完成记录时应正确聚合', async () => {
    mockRepository.getSportRecordsByUserId.mockResolvedValue([
      { status: 'completed', distance: 5000, duration: 1800, calories: 300, averagePace: 360, maxHeartRate: 175, averageHeartRate: 155 } as any,
      { status: 'completed', distance: 8000, duration: 2700, calories: 480, averagePace: 337, maxHeartRate: 180, averageHeartRate: 160 } as any,
    ]);

    const stats = await makeService().getUserSportStats('u1');
    expect(stats.totalDistance).toBe(13000);
    expect(stats.totalCalories).toBe(780);
    expect(stats.bestPace).toBe(337);
  });
});

// ==================== getSportRecordById ====================

describe('getSportRecordById', () => {
  it('不存在时应抛出 404', async () => {
    mockRepository.getSportRecordById.mockResolvedValue(null);
    await expect(makeService().getSportRecordById('bad')).rejects.toMatchObject({ code: 404 });
  });

  it('存在时应返回记录', async () => {
    mockRepository.getSportRecordById.mockResolvedValue({ id: 'r1', status: 'completed' } as any);
    const record = await makeService().getSportRecordById('r1');
    expect(record.id).toBe('r1');
  });
});
