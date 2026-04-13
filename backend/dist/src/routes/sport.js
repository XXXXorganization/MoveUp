"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createSportRoutes = void 0;
// src/routes/sport.ts
const express_1 = require("express");
/**
 * 创建运动模块路由
 */
const createSportRoutes = (sportController) => {
    const router = (0, express_1.Router)();
    // ============ 运动记录管理 ============
    // 开始运动记录
    router.post('/sport/start', sportController.startSportRecord.bind(sportController));
    // 获取用户的运动记录列表
    router.get('/sport', sportController.getSportRecords.bind(sportController));
    // 获取用户运动统计
    router.get('/sport/stats', sportController.getUserSportStats.bind(sportController));
    // 获取单个运动记录
    router.get('/sport/:recordId', sportController.getSportRecord.bind(sportController));
    // 更新运动记录（实时上传）
    router.put('/sport/:recordId/update', sportController.updateSportRecord.bind(sportController));
    // 停止运动记录
    router.put('/sport/:recordId/stop', sportController.stopSportRecord.bind(sportController));
    // ============ 实时数据 ============
    // 获取实时运动数据
    router.get('/sport/:recordId/realtime', sportController.getRealTimeData.bind(sportController));
    // 获取运动记录统计
    router.get('/sport/:recordId/stats', sportController.getSportRecordStats.bind(sportController));
    // 获取配速分段统计
    router.get('/sport/:recordId/pace-segments', sportController.getPaceSegments.bind(sportController));
    // ============ GPS 轨迹 ============
    // 批量上传 GPS 轨迹点
    router.post('/sport/:recordId/gps/batch', sportController.batchUploadGpsPoints.bind(sportController));
    // 获取 GPS 轨迹点
    router.get('/sport/:recordId/gps', sportController.getGpsPoints.bind(sportController));
    // ============ 心率数据 ============
    // 上传心率数据（批量）
    router.post('/sport/:recordId/heart-rate/batch', sportController.uploadHeartRateData.bind(sportController));
    // 获取心率数据
    router.get('/sport/:recordId/heart-rate', sportController.getHeartRates.bind(sportController));
    // 接入蓝牙心率设备数据
    router.post('/sport/:recordId/bluetooth/heart-rate', sportController.processBluetoothHeartRate.bind(sportController));
    // ============ 工具接口 ============
    // 计算卡路里
    router.post('/calculate-calories', sportController.calculateCalories.bind(sportController));
    return router;
};
exports.createSportRoutes = createSportRoutes;
