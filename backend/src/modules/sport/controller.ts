// src/modules/sport/controller.ts
import { Request, Response, NextFunction } from 'express';
import { SportService } from './service';
import { SportUpdateData, CalorieCalculationParams, BluetoothHeartRateData } from './types';
import { AppError } from '../../utils/errors';

/**
 * 运动控制器
 * 处理运动数据相关的 HTTP 请求
 */
export class SportController {
  constructor(private service: SportService) {}

  /**
   * 开始运动记录
   * POST /v1/sport/start
   */
  async startSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) {
        throw new AppError('请先登录', 401);
      }

      const record = await this.service.startSportRecord(userId);
      res.status(201).json({
        code: 200,
        message: '运动记录已创建',
        data: record,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 更新运动记录（实时上传）
   * PUT /v1/sport/:recordId/update
   */
  async updateSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;
      const updateData: SportUpdateData = req.body;

      if (updateData.recordId !== recordId) {
        throw new AppError('记录 ID 不匹配', 400);
      }

      const record = await this.service.updateSportRecord(recordId, updateData);
      res.json({
        code: 200,
        message: '运动记录已更新',
        data: record,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 批量上传 GPS 轨迹点
   * POST /v1/sport/:recordId/gps/batch
   */
  async batchUploadGpsPoints(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;
      const { gpsPoints } = req.body;

      if (!gpsPoints || !Array.isArray(gpsPoints)) {
        throw new AppError('GPS 点数据格式错误', 400);
      }

      const points = await this.service.batchUploadGpsPoints(recordId, gpsPoints);
      res.json({
        code: 200,
        message: 'GPS 轨迹点上传成功',
        data: {
          count: points.length,
          points,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 上传心率数据
   * POST /v1/sport/:recordId/heart-rate/batch
   */
  async uploadHeartRateData(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;
      const { heartRates } = req.body;

      if (!heartRates || !Array.isArray(heartRates)) {
        throw new AppError('心率数据格式错误', 400);
      }

      const rates = await this.service.uploadHeartRateData(recordId, heartRates);
      res.json({
        code: 200,
        message: '心率数据上传成功',
        data: {
          count: rates.length,
          rates,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 停止运动记录
   * PUT /v1/sport/:recordId/stop
   */
  async stopSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const record = await this.service.stopSportRecord(recordId);
      res.json({
        code: 200,
        message: '运动记录已结束',
        data: record,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取实时运动数据
   * GET /v1/sport/:recordId/realtime
   */
  async getRealTimeData(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const data = await this.service.getRealTimeData(recordId);
      res.json({
        code: 200,
        message: '获取实时数据成功',
        data,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取运动记录统计
   * GET /v1/sport/:recordId/stats
   */
  async getSportRecordStats(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const stats = await this.service.getSportRecordStats(recordId);
      res.json({
        code: 200,
        message: '获取统计数据成功',
        data: stats,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取用户运动统计
   * GET /v1/sport/stats
   */
  async getUserSportStats(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) {
        throw new AppError('请先登录', 401);
      }

      const stats = await this.service.getUserSportStats(userId);
      res.json({
        code: 200,
        message: '获取用户统计成功',
        data: stats,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取配速分段统计
   * GET /v1/sport/:recordId/pace-segments
   */
  async getPaceSegments(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const record = await this.service.getSportRecordById(recordId);
      const gpsPoints = await this.service.getGpsPointsByRecordId(recordId);

      // 使用 service 内部方法计算分段（需要暴露或重新实现）
      // 这里简化处理，返回基础信息
      res.json({
        code: 200,
        message: '获取配速分段成功',
        data: {
          recordId,
          totalDistance: record.distance,
          averagePace: record.averagePace,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 接入蓝牙心率设备数据
   * POST /v1/sport/:recordId/bluetooth/heart-rate
   */
  async processBluetoothHeartRate(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;
      const bluetoothData: BluetoothHeartRateData = req.body;

      if (!bluetoothData.deviceId || !bluetoothData.heartRate) {
        throw new AppError('蓝牙心率数据格式错误', 400);
      }

      const heartRate = await this.service.processBluetoothHeartRate(recordId, bluetoothData);
      res.json({
        code: 200,
        message: '蓝牙心率数据已记录',
        data: heartRate,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取单个运动记录
   * GET /v1/sport/:recordId
   */
  async getSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const record = await this.service.getSportRecordById(recordId);
      res.json({
        code: 200,
        message: '获取运动记录成功',
        data: record,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取用户的运动记录列表
   * GET /v1/sport
   */
  async getSportRecords(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = (req as any).user?.userId;
      if (!userId) {
        throw new AppError('请先登录', 401);
      }

      const records = await this.service.getSportRecordsByUserId(userId);
      res.json({
        code: 200,
        message: '获取运动记录列表成功',
        data: records,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取 GPS 轨迹点
   * GET /v1/sport/:recordId/gps
   */
  async getGpsPoints(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const points = await this.service.getGpsPointsByRecordId(recordId);
      res.json({
        code: 200,
        message: '获取 GPS 轨迹点成功',
        data: {
          count: points.length,
          points,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 获取心率数据
   * GET /v1/sport/:recordId/heart-rate
   */
  async getHeartRates(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { recordId } = req.params;

      const rates = await this.service.getHeartRatesByRecordId(recordId);
      res.json({
        code: 200,
        message: '获取心率数据成功',
        data: {
          count: rates.length,
          rates,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * 计算卡路里（工具接口，可用于测试）
   * POST /v1/sport/calculate-calories
   */
  async calculateCalories(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const params: CalorieCalculationParams = req.body;

      if (!params.weight || !params.duration || !params.averagePace) {
        throw new AppError('缺少必要参数', 400);
      }

      // 注意：这里需要创建一个公开方法来暴露 calculateCalories
      // 暂时返回一个示例值
      res.json({
        code: 200,
        message: '卡路里计算成功',
        data: {
          calories: 0, // TODO: 实现公开方法
          params,
        },
      });
    } catch (error) {
      next(error);
    }
  }
}
