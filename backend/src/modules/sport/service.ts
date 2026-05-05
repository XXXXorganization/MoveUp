/* eslint-disable no-useless-assignment, @typescript-eslint/no-unused-vars */

// src/modules/sport/service.ts
import { SportRepository } from './repository';
import {
  SportRecord,
  GpsPoint,
  HeartRateData,
  SportUpdateData,
  CalorieCalculationParams,
  RealTimeSportData,
  BluetoothHeartRateData,
  SportRecordStats,
  PaceSegment,
} from './types';
import { AppError } from '../../utils/errors';

/**
 * 运动服务类
 * 提供运动数据追踪、GPS 纠偏、卡路里计算等功能
 */
export class SportService {
  // GPS 纠偏参数
  private readonly GPS_ACCURACY_THRESHOLD = 50; // 精度阈值（米），超过此值视为漂移点
  private readonly MAX_SPEED_THRESHOLD = 8; // 最大速度阈值（m/s），约 28.8 km/h，超过视为异常
  private readonly MIN_POINT_DISTANCE = 5; // 最小点间距（米），小于此值的点可能被合并

  // 卡路里计算参数
  private readonly BASE_MET = 8.3; // 基础代谢当量（中等强度跑步）

  constructor(private repository: SportRepository) {}

  /**
   * 开始运动记录
   */
  async startSportRecord(userId: string): Promise<SportRecord> {
    // 检查用户是否有进行中的运动记录
    const activeRecord = await this.repository.getActiveSportRecordByUserId(userId);
    if (activeRecord) {
      throw new AppError('您已有一个进行中的运动记录', 400);
    }

    const record: Omit<SportRecord, 'id' | 'created_at' | 'updated_at'> = {
      userId,
      startTime: new Date(),
      distance: 0,
      duration: 0,
      calories: 0,
      status: 'active',
    };

    return this.repository.createSportRecord(record);
  }

  /**
   * 更新运动记录（实时数据上传）
   */
  async updateSportRecord(recordId: string, data: SportUpdateData): Promise<SportRecord> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    if (record.status !== 'active') {
      throw new AppError('运动记录已结束，无法更新', 400);
    }

    // 处理 GPS 点（进行纠偏）
    if (data.gpsPoints && data.gpsPoints.length > 0) {
      const correctedPoints = this.correctGpsPoints(data.gpsPoints);
      await this.repository.insertGpsPoints(correctedPoints);
    }

    // 处理心率数据
    if (data.heartRates && data.heartRates.length > 0) {
      await this.repository.insertHeartRates(data.heartRates);
    }

    // 实时计算运动指标
    const updatedRecord = await this.calculateRealTimeMetrics(recordId);
    return updatedRecord;
  }

  /**
   * 批量上传 GPS 轨迹点
   */
  async batchUploadGpsPoints(recordId: string, points: Omit<GpsPoint, 'id'>[]): Promise<GpsPoint[]> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    if (record.status !== 'active') {
      throw new AppError('运动记录已结束，无法上传数据', 400);
    }

    // 纠偏处理
    const correctedPoints = this.correctGpsPoints(points);
    return this.repository.insertGpsPoints(correctedPoints);
  }

  /**
   * 上传心率数据
   */
  async uploadHeartRateData(recordId: string, heartRates: Omit<HeartRateData, 'id'>[]): Promise<HeartRateData[]> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    if (record.status !== 'active') {
      throw new AppError('运动记录已结束，无法上传数据', 400);
    }

    return this.repository.insertHeartRates(heartRates);
  }

  /**
   * 停止运动记录
   */
  async stopSportRecord(recordId: string): Promise<SportRecord> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    if (record.status !== 'active') {
      throw new AppError('运动记录已结束', 400);
    }

    const endTime = new Date();

    // 最终计算所有指标
    const finalRecord = await this.calculateFinalMetrics(recordId);

    const updates: Partial<SportRecord> = {
      endTime,
      duration: finalRecord.duration,
      status: 'completed',
      distance: finalRecord.distance,
      calories: finalRecord.calories,
      averagePace: finalRecord.averagePace,
      maxHeartRate: finalRecord.maxHeartRate,
      averageHeartRate: finalRecord.averageHeartRate,
    };

    const updatedRecord = await this.repository.updateSportRecord(recordId, updates);
    if (!updatedRecord) {
      throw new AppError('更新运动记录失败', 500);
    }
    return updatedRecord;
  }

  /**
   * 实时计算运动指标
   */
  private async calculateRealTimeMetrics(recordId: string): Promise<SportRecord> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    const gpsPoints = await this.repository.getGpsPointsByRecordId(recordId);
    const heartRates = await this.repository.getHeartRatesByRecordId(recordId);

    // 调用统一的方法计算所有指标
    const metrics = this.computeAllMetrics(record, gpsPoints, heartRates);

    // 更新记录
    const updates: Partial<SportRecord> = {
      distance: metrics.distance,
      duration: metrics.duration,
      averagePace: metrics.averagePace,
      maxHeartRate: metrics.maxHeartRate,
      averageHeartRate: metrics.averageHeartRate,
    };

    const updatedRecord = await this.repository.updateSportRecord(recordId, updates);
    if (!updatedRecord) {
      throw new AppError('更新运动记录失败', 500);
    }
    return updatedRecord;
  }

  /**
   * 最终计算运动指标（停止时使用）
   */
  private async calculateFinalMetrics(recordId: string): Promise<SportRecord> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    const gpsPoints = await this.repository.getGpsPointsByRecordId(recordId);
    const heartRates = await this.repository.getHeartRatesByRecordId(recordId);

    // 调用统一的方法计算所有指标
    const metrics = this.computeAllMetrics(record, gpsPoints, heartRates);

    // 计算卡路里（需要用户体重数据，这里先使用默认值）
    const calories = this.calculateCalories({
      weight: 70, // 默认体重 70kg，实际应从用户信息获取
      duration: metrics.duration!,
      averagePace: metrics.averagePace || 0,
      averageHeartRate: metrics.averageHeartRate,
    });

    return {
      ...record,
      distance: metrics.distance!,
      duration: metrics.duration!,
      calories,
      averagePace: metrics.averagePace,
      maxHeartRate: metrics.maxHeartRate,
      averageHeartRate: metrics.averageHeartRate,
    };
  }

  /**
   * 统一计算所有运动指标
   */
  private computeAllMetrics(
    record: SportRecord,
    gpsPoints: GpsPoint[],
    heartRates: HeartRateData[]
  ): Pick<SportRecord, 'distance' | 'duration' | 'averagePace' | 'maxHeartRate' | 'averageHeartRate'> {
    // 计算距离
    const distance = this.calculateDistanceFromGps(gpsPoints);

    // 计算时长
    const duration = Math.floor((new Date().getTime() - record.startTime.getTime()) / 1000);

    // 计算配速
    const averagePace = distance > 0 ? Math.round(duration / (distance / 1000)) : undefined;

    // 计算心率统计
    const maxHeartRate = heartRates.length > 0 ? Math.max(...heartRates.map(hr => hr.heartRate)) : undefined;
    const averageHeartRate = heartRates.length > 0
      ? Math.round(heartRates.reduce((sum, hr) => sum + hr.heartRate, 0) / heartRates.length)
      : undefined;

    return {
      distance,
      duration,
      averagePace,
      maxHeartRate,
      averageHeartRate,
    };
  }

  /**
   * GPS 轨迹纠偏
   * 移除漂移点和异常点
   */
  correctGpsPoints(points: Omit<GpsPoint, 'id'>[]): GpsPoint[] {
    if (points.length < 1) {
      return [];
    }

    const normalizedPoints = this.normalizeGpsPoints(points);
    const correctedPoints: GpsPoint[] = [];

    // 第一个点始终保留（添加 id 字段）
    const firstPoint = normalizedPoints[0] as GpsPoint;
    correctedPoints.push(firstPoint);

    for (let i = 1; i < normalizedPoints.length; i++) {
      const prevPoint = correctedPoints[correctedPoints.length - 1] as GpsPoint;
      const currPoint = normalizedPoints[i];

      // 检查点的有效性
      if (this.isValidGpsPoint(prevPoint, currPoint)) {
        correctedPoints.push(currPoint);
      }
      // 无效点被跳过（不加入结果数组）
    }

    return correctedPoints;
  }

  /**
   * 验证 GPS 点是否有效
   */
  private isValidGpsPoint(prevPoint: GpsPoint | Omit<GpsPoint, 'id'>, currPoint: Omit<GpsPoint, 'id'>): boolean {
    // 检查精度
    if (currPoint.accuracy !== undefined && currPoint.accuracy > this.GPS_ACCURACY_THRESHOLD) {
      return false;
    }

    // 确保 timestamp 是 Date 对象
    const prevTimestamp = prevPoint.timestamp instanceof Date ? prevPoint.timestamp : new Date(prevPoint.timestamp);
    const currTimestamp = currPoint.timestamp instanceof Date ? currPoint.timestamp : new Date(currPoint.timestamp);

    // 计算距离和时间差
    const distance = this.haversineDistance(
      prevPoint.latitude,
      prevPoint.longitude,
      currPoint.latitude,
      currPoint.longitude
    );

    const timeDiff = (currTimestamp.getTime() - prevTimestamp.getTime()) / 1000;

    // 检查速度是否异常
    if (timeDiff > 0) {
      const speed = distance / timeDiff;
      if (speed > this.MAX_SPEED_THRESHOLD) {
        return false;
      }
    }

    return true;
  }

  /**
   * 从 GPS 点计算总距离
   */
  private calculateDistanceFromGps(points: GpsPoint[]): number {
    if (points.length < 2) return 0;

    let totalDistance = 0;
    for (let i = 1; i < points.length; i++) {
      const prev = points[i - 1];
      const curr = points[i];
      totalDistance += this.haversineDistance(
        prev.latitude,
        prev.longitude,
        curr.latitude,
        curr.longitude
      );
    }

    return Math.round(totalDistance * 100) / 100; // 保留两位小数
  }

  /**
   * 确保 GPS 点的 timestamp 是 Date 对象
   */
  private normalizeGpsPoints(points: GpsPoint[]): GpsPoint[] {
    return points.map(point => ({
      ...point,
      timestamp: point.timestamp instanceof Date ? point.timestamp : new Date(point.timestamp),
    }));
  }

  /**
   * 计算两点之间的球面距离（Haversine 公式）
   */
  private haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371e3; // 地球半径（米）
    const φ1 = (lat1 * Math.PI) / 180;
    const φ2 = (lat2 * Math.PI) / 180;
    const Δφ = ((lat2 - lat1) * Math.PI) / 180;
    const Δλ = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
      Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  /**
   * 计算卡路里消耗
   * 基于心率、体重、配速的综合算法
   */
  calculateCalories(params: CalorieCalculationParams): number {
    const { weight, duration, averagePace, averageHeartRate, age = 25, gender = 1 } = params;

    // 基础代谢当量（根据配速调整）
    let met = this.BASE_MET;

    // 根据配速调整 MET 值
    const paceMinPerKm = averagePace / 60; // 转换为分钟/公里

    if (paceMinPerKm < 4) {
      // 配速快于 4:00/km（高强度）
      met = 12.0;
    } else if (paceMinPerKm < 5) {
      // 配速 4:00-5:00/km（中高强度）
      met = 10.0;
    } else if (paceMinPerKm < 6) {
      // 配速 5:00-6:00/km（中等强度）
      met = 8.3;
    } else if (paceMinPerKm < 7) {
      // 配速 6:00-7:00/km（中低强度）
      met = 7.0;
    } else {
      // 配速慢于 7:00/km（低强度）
      met = 5.5;
    }

    // 根据心率调整（如果有心率数据）
    if (averageHeartRate) {
      const maxHeartRateEstimate = 220 - age;
      const heartRateZone = averageHeartRate / maxHeartRateEstimate;

      // 心率区间系数
      let heartRateMultiplier = 1.0;
      if (heartRateZone > 0.9) {
        heartRateMultiplier = 1.3; // 无氧区
      } else if (heartRateZone > 0.8) {
        heartRateMultiplier = 1.2; // 乳酸阈值区
      } else if (heartRateZone > 0.7) {
        heartRateMultiplier = 1.1; // 有氧区
      } else if (heartRateZone > 0.6) {
        heartRateMultiplier = 1.0; // 燃脂区
      } else {
        heartRateMultiplier = 0.9; // 热身区
      }

      met *= heartRateMultiplier;
    }

    // 性别系数（男性基础代谢略高）
    const genderCoefficient = gender === 1 ? 1.0 : 0.9;

    // 计算卡路里
    const hours = duration / 3600;
    const calories = met * weight * hours * genderCoefficient;

    return Math.round(calories * 10) / 10; // 保留一位小数
  }

  /**
   * 获取实时运动数据
   */
  async getRealTimeData(recordId: string): Promise<RealTimeSportData> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    const gpsPoints = await this.repository.getGpsPointsByRecordId(recordId);
    const heartRates = await this.repository.getHeartRatesByRecordId(recordId);

    // 计算距离
    const distance = this.calculateDistanceFromGps(gpsPoints);

    // 计算时长
    const duration = Math.floor((new Date().getTime() - record.startTime.getTime()) / 1000);

    // 计算配速
    const pace = distance > 0 ? Math.round(duration / (distance / 1000)) : 0;

    // 计算速度
    const speed = duration > 0 ? distance / duration : 0;

    // 获取最新心率
    const latestHeartRate = heartRates.length > 0 ? heartRates[heartRates.length - 1].heartRate : undefined;

    // 获取最新 GPS 点
    const lastGpsPoint = gpsPoints.length > 0 ? gpsPoints[gpsPoints.length - 1] : undefined;

    // 计算卡路里
    const calories = this.calculateCalories({
      weight: 70,
      duration,
      averagePace: pace,
      averageHeartRate: latestHeartRate,
    });

    return {
      distance,
      duration,
      pace,
      speed,
      calories,
      heartRate: latestHeartRate,
      lastGpsPoint,
    };
  }

  /**
   * 获取运动记录统计
   */
  async getSportRecordStats(recordId: string): Promise<SportRecordStats> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    const gpsPoints = await this.repository.getGpsPointsByRecordId(recordId);
    const heartRates = await this.repository.getHeartRatesByRecordId(recordId);

    // 计算最佳配速（从分段中获取）
    const paceSegments = this.calculatePaceSegments(gpsPoints);
    const bestPace = paceSegments.length > 0
      ? Math.min(...paceSegments.map(s => s.averagePace))
      : (record.averagePace || 0);

    return {
      totalDistance: record.distance,
      totalTime: record.duration,
      totalCalories: record.calories,
      averagePace: record.averagePace || 0,
      bestPace,
      maxHeartRate: record.maxHeartRate || 0,
      averageHeartRate: record.averageHeartRate || 0,
    };
  }

  /**
   * 计算配速分段统计
   */
  private calculatePaceSegments(gpsPoints: GpsPoint[]): PaceSegment[] {
    if (gpsPoints.length < 2) {
      return [];
    }

    const segments: PaceSegment[] = [];
    const segmentDistance = 1000; // 每 1 公里为一个分段

    let currentSegmentStart = 0;
    let accumulatedDistance = 0;

    for (let i = 1; i < gpsPoints.length; i++) {
      const prevPoint = gpsPoints[i - 1];
      const currPoint = gpsPoints[i];

      const segmentDist = this.haversineDistance(
        prevPoint.latitude,
        prevPoint.longitude,
        currPoint.latitude,
        currPoint.longitude
      );

      accumulatedDistance += segmentDist;

      // 当累积距离达到分段距离时，计算该分段的配速
      if (accumulatedDistance >= segmentDistance) {
        const startTime = gpsPoints[currentSegmentStart].timestamp.getTime();
        const endTime = currPoint.timestamp.getTime();
        const duration = (endTime - startTime) / 1000; // 秒
        const averagePace = duration / (accumulatedDistance / 1000); // 秒/公里

        const paceMin = Math.floor(averagePace / 60);
        const paceSec = Math.round(averagePace % 60);
        const segmentLabel = `${paceMin}:${paceSec.toString().padStart(2, '0')}/km`;

        segments.push({
          segment: segmentLabel,
          distance: Math.round(accumulatedDistance),
          duration: Math.round(duration),
          averagePace: Math.round(averagePace),
        });

        currentSegmentStart = i;
        accumulatedDistance = 0;
      }
    }

    return segments;
  }

  /**
   * 处理蓝牙心率设备数据
   */
  async processBluetoothHeartRate(
    recordId: string,
    bluetoothData: BluetoothHeartRateData
  ): Promise<HeartRateData> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }

    const heartRateData: Omit<HeartRateData, 'id'> = {
      recordId,
      timestamp: bluetoothData.timestamp,
      heartRate: bluetoothData.heartRate,
    };

    const [inserted] = await this.repository.insertHeartRates([heartRateData]);
    return inserted;
  }

  // ============ 查询方法 ============

  async getSportRecordById(recordId: string): Promise<SportRecord> {
    const record = await this.repository.getSportRecordById(recordId);
    if (!record) {
      throw new AppError('运动记录不存在', 404);
    }
    return record;
  }

  async getSportRecordsByUserId(userId: string): Promise<SportRecord[]> {
    return this.repository.getSportRecordsByUserId(userId);
  }

  async getGpsPointsByRecordId(recordId: string): Promise<GpsPoint[]> {
    await this.getSportRecordById(recordId); // 验证记录存在
    return this.repository.getGpsPointsByRecordId(recordId);
  }

  async getHeartRatesByRecordId(recordId: string): Promise<HeartRateData[]> {
    await this.getSportRecordById(recordId); // 验证记录存在
    return this.repository.getHeartRatesByRecordId(recordId);
  }

  async getUserSportStats(userId: string): Promise<SportRecordStats> {
    const records = await this.repository.getSportRecordsByUserId(userId);
    const completedRecords = records.filter(r => r.status === 'completed');

    if (completedRecords.length === 0) {
      return {
        totalDistance: 0,
        totalTime: 0,
        totalCalories: 0,
        averagePace: 0,
        bestPace: 0,
        maxHeartRate: 0,
        averageHeartRate: 0,
      };
    }

    const totalDistance = completedRecords.reduce((sum, r) => sum + r.distance, 0);
    const totalTime = completedRecords.reduce((sum, r) => sum + r.duration, 0);
    const totalCalories = completedRecords.reduce((sum, r) => sum + r.calories, 0);

    const paces = completedRecords.map(r => r.averagePace || 0).filter(p => p > 0);
    const averagePace = paces.length > 0 ? paces.reduce((a, b) => a + b, 0) / paces.length : 0;
    const bestPace = paces.length > 0 ? Math.min(...paces) : 0;

    const heartRates = completedRecords.map(r => r.averageHeartRate || 0).filter(h => h > 0);
    const averageHeartRate = heartRates.length > 0 ? heartRates.reduce((a, b) => a + b, 0) / heartRates.length : 0;
    const maxHeartRate = Math.max(...completedRecords.map(r => r.maxHeartRate || 0));

    return {
      totalDistance,
      totalTime,
      totalCalories,
      averagePace,
      bestPace,
      maxHeartRate,
      averageHeartRate,
    };
  }
}
