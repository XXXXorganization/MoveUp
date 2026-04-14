// src/modules/sport/types.ts

/**
 * 运动记录
 */
export interface SportRecord {
  id: string;
  userId: string;
  startTime: Date;
  endTime?: Date;
  distance: number; // 米
  duration: number; // 秒
  calories: number; // 卡路里
  status: 'active' | 'completed';
  averagePace?: number; // 秒/公里
  maxHeartRate?: number; // 最大心率
  averageHeartRate?: number; // 平均心率
  created_at?: string;
  updated_at?: string;
}

/**
 * GPS 轨迹点
 */
export interface GpsPoint {
  id?: string;
  recordId: string;
  latitude: number;
  longitude: number;
  timestamp: Date;
  speed?: number; // m/s
  altitude?: number; // 米
  accuracy?: number; // 米
}

/**
 * 心率数据
 */
export interface HeartRateData {
  id?: string;
  recordId: string;
  timestamp: Date;
  heartRate: number; // bpm
}

/**
 * 运动数据更新请求
 */
export interface SportUpdateData {
  recordId: string;
  gpsPoints?: Omit<GpsPoint, 'id'>[];
  heartRates?: Omit<HeartRateData, 'id'>[];
  distance?: number;
}

/**
 * 卡路里计算参数
 */
export interface CalorieCalculationParams {
  weight: number; // kg - 体重
  duration: number; // 秒 - 运动时长
  averagePace: number; // 秒/公里 - 配速
  averageHeartRate?: number; // bpm - 平均心率
  age?: number; // 年龄
  gender?: number; // 1-男，2-女
}

/**
 * 实时运动数据
 */
export interface RealTimeSportData {
  distance: number; // 米
  duration: number; // 秒
  pace: number; // 秒/公里
  speed: number; // m/s
  calories: number; // 卡路里
  heartRate?: number; // bpm
  lastGpsPoint?: GpsPoint;
}

/**
 * GPS 纠偏结果
 */
export interface GpsCorrectionResult {
  originalPoints: GpsPoint[];
  correctedPoints: GpsPoint[];
  removedPointsCount: number;
}

/**
 * 蓝牙心率设备数据
 */
export interface BluetoothHeartRateData {
  deviceId: string;
  heartRate: number;
  batteryLevel?: number;
  timestamp: Date;
}

/**
 * 运动记录统计
 */
export interface SportRecordStats {
  totalDistance: number; // 总距离（米）
  totalTime: number; // 总时长（秒）
  totalCalories: number; // 总卡路里
  averagePace: number; // 平均配速
  bestPace: number; // 最佳配速
  maxHeartRate: number; // 最大心率
  averageHeartRate: number; // 平均心率
}

/**
 * 配速分段统计
 */
export interface PaceSegment {
  segment: string; // 如 "0-5:00", "5:00-6:00"
  distance: number; // 米
  duration: number; // 秒
  averagePace: number; // 秒/公里
}
