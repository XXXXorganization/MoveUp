// src/modules/coaching/service.ts
import { CoachingRepository } from './repository';
import {
  TrainingPlan,
  UserPlan,
  UserPlanDetail,
  PlanRecommendRequest,
  VoiceGuidanceConfig,
  SegmentAdvice,
  StretchingGuide,
  StretchingExercise,
  InjuryPreventionTip,
} from './types';
import { AppError } from '../../utils/errors';

export class CoachingService {
  constructor(private repository: CoachingRepository) {}

  // ==================== 个性化计划 ====================

  async getAllPlans(difficulty?: string): Promise<TrainingPlan[]> {
    if (difficulty) {
      return this.repository.getTrainingPlansByDifficulty(difficulty);
    }
    return this.repository.getAllTrainingPlans();
  }

  async getPlanById(id: string): Promise<TrainingPlan> {
    const plan = await this.repository.getTrainingPlanById(id);
    if (!plan) throw new AppError('训练计划不存在', 404);
    return plan;
  }

  async recommendPlan(userId: string, req: PlanRecommendRequest): Promise<TrainingPlan[]> {
    const plans = await this.repository.getTrainingPlansByDifficulty(req.fitnessLevel);
    // 按目标距离过滤并排序（最接近目标的优先）
    return plans
      .filter(p => p.targetDistance > 0)
      .sort((a, b) => Math.abs(a.targetDistance - req.targetDistance) - Math.abs(b.targetDistance - req.targetDistance))
      .slice(0, 3);
  }

  async adoptPlan(userId: string, planId: string): Promise<UserPlanDetail> {
    const plan = await this.repository.getTrainingPlanById(planId);
    if (!plan) throw new AppError('训练计划不存在', 404);

    // 停用旧计划
    await this.repository.deactivateAllUserPlans(userId);

    const userPlan = await this.repository.createUserPlan({
      userId,
      planId,
      currentWeek: 1,
      currentDay: 1,
      isActive: true,
      startedAt: new Date(),
    });

    return this.buildUserPlanDetail(userPlan, plan);
  }

  async getActivePlan(userId: string): Promise<UserPlanDetail> {
    const userPlan = await this.repository.getActivePlanByUserId(userId);
    if (!userPlan) throw new AppError('暂无进行中的训练计划', 404);

    const plan = await this.repository.getTrainingPlanById(userPlan.planId);
    if (!plan) throw new AppError('训练计划不存在', 404);

    return this.buildUserPlanDetail(userPlan, plan);
  }

  async getTodayTask(userId: string) {
    const detail = await this.getActivePlan(userId);
    return {
      userPlanId: detail.id,
      planName: detail.plan.name,
      week: detail.currentWeek,
      day: detail.currentDay,
      task: detail.todayTask,
      completionRate: detail.completionRate,
    };
  }

  async updateProgress(userId: string, userPlanId: string, week: number, day: number): Promise<UserPlanDetail> {
    const userPlan = await this.repository.getUserPlanById(userPlanId);
    if (!userPlan || userPlan.userId !== userId) throw new AppError('计划记录不存在', 404);

    const plan = await this.repository.getTrainingPlanById(userPlan.planId);
    if (!plan) throw new AppError('训练计划不存在', 404);

    const isCompleted = week > plan.durationWeeks;
    const updated = await this.repository.updateUserPlan(userPlanId, {
      currentWeek: week,
      currentDay: day,
      isActive: !isCompleted,
      completedAt: isCompleted ? new Date() : undefined,
    });

    if (!updated) throw new AppError('更新进度失败', 500);
    return this.buildUserPlanDetail(updated, plan);
  }

  async quitPlan(userId: string, userPlanId: string): Promise<void> {
    const userPlan = await this.repository.getUserPlanById(userPlanId);
    if (!userPlan || userPlan.userId !== userId) throw new AppError('计划记录不存在', 404);

    await this.repository.updateUserPlan(userPlanId, { isActive: false });
  }

  // ==================== 语音指导 ====================

  getVoiceGuidanceConfig(distanceM: number, currentPace: number, currentHeartRate?: number, config?: VoiceGuidanceConfig): string[] {
    const messages: string[] = [];
    const cfg = config ?? { distanceInterval: 1000, paceAlertEnabled: true, heartRateAlertEnabled: true };

    if (distanceM > 0 && distanceM % cfg.distanceInterval < 10) {
      const km = (distanceM / 1000).toFixed(1);
      const paceMin = Math.floor(currentPace / 60);
      const paceSec = currentPace % 60;
      messages.push(`已跑 ${km} 公里，当前配速 ${paceMin} 分 ${paceSec} 秒`);
    }

    if (cfg.paceAlertEnabled && cfg.paceThreshold && currentPace > cfg.paceThreshold) {
      messages.push('配速偏慢，请适当加速');
    }

    if (cfg.heartRateAlertEnabled && cfg.heartRateThreshold && currentHeartRate && currentHeartRate > cfg.heartRateThreshold) {
      messages.push('心率过高，建议放慢速度');
    }

    return messages;
  }

  getSegmentAdvice(currentHeartRate: number, age: number = 25): SegmentAdvice {
    const maxHR = 220 - age;
    const ratio = currentHeartRate / maxHR;

    let zone: SegmentAdvice['heartRateZone'];
    let zoneLabel: string;
    let advice: string;
    let suggestedPace: string | undefined;
    let shouldSlowDown = false;

    if (ratio >= 0.9) {
      zone = 'max';
      zoneLabel = '极限区（90-100%）';
      advice = '心率已达极限，请立即降速或停止运动';
      suggestedPace = '慢跑或步行';
      shouldSlowDown = true;
    } else if (ratio >= 0.8) {
      zone = 'anaerobic';
      zoneLabel = '无氧区（80-90%）';
      advice = '心率较高，适合短距离冲刺，长跑建议降速';
      suggestedPace = '5:00-5:30/km';
      shouldSlowDown = true;
    } else if (ratio >= 0.7) {
      zone = 'aerobic';
      zoneLabel = '有氧区（70-80%）';
      advice = '心率理想，保持当前配速';
      suggestedPace = '5:30-6:30/km';
    } else if (ratio >= 0.6) {
      zone = 'fat_burn';
      zoneLabel = '燃脂区（60-70%）';
      advice = '心率偏低，可适当提速以提升训练效果';
      suggestedPace = '6:00-7:00/km';
    } else {
      zone = 'warmup';
      zoneLabel = '热身区（<60%）';
      advice = '心率过低，建议加速进入有效训练区间';
      suggestedPace = '6:30-7:30/km';
    }

    return { currentHeartRate, heartRateZone: zone, zoneLabel, advice, suggestedPace, shouldSlowDown };
  }

  // ==================== 健康建议 ====================

  getStretchingGuide(runDurationSeconds: number): StretchingGuide {
    const base: StretchingExercise[] = [
      { name: '股四头肌拉伸', duration: 30, description: '单腿站立，弯曲膝盖将脚跟拉向臀部', targetMuscle: '大腿前侧' },
      { name: '腘绳肌拉伸', duration: 30, description: '坐地伸直双腿，身体前倾触碰脚尖', targetMuscle: '大腿后侧' },
      { name: '小腿拉伸', duration: 30, description: '面墙站立，一脚向后蹬地拉伸小腿', targetMuscle: '小腿' },
      { name: '髋屈肌拉伸', duration: 30, description: '弓步姿势，前腿弯曲，后腿膝盖着地', targetMuscle: '髋部' },
    ];

    // 跑步超过 30 分钟增加额外拉伸
    if (runDurationSeconds >= 1800) {
      base.push(
        { name: 'IT 髂胫束拉伸', duration: 30, description: '交叉双腿站立，身体侧弯', targetMuscle: '大腿外侧' },
        { name: '臀部拉伸', duration: 30, description: '仰卧，将一侧膝盖拉向对侧肩膀', targetMuscle: '臀部' },
      );
    }

    return { runDuration: runDurationSeconds, exercises: base };
  }

  getInjuryPreventionTips(weeklyDistanceM: number, recentRunCount: number): InjuryPreventionTip[] {
    const tips: InjuryPreventionTip[] = [
      {
        category: 'warmup',
        title: '跑前热身',
        content: '每次跑步前进行 5-10 分钟动态热身，包括高抬腿、踢臀跑等',
        priority: 'high',
      },
      {
        category: 'cooldown',
        title: '跑后拉伸',
        content: '跑步结束后进行静态拉伸，每个动作保持 30 秒',
        priority: 'high',
      },
    ];

    // 周跑量超过 50km 提示过度训练风险
    if (weeklyDistanceM > 50000) {
      tips.push({
        category: 'overtraining',
        title: '注意过度训练',
        content: '本周跑量较大，建议安排 1-2 天休息日，避免疲劳性骨折',
        priority: 'high',
      });
    }

    // 连续跑步超过 5 次提示休息
    if (recentRunCount >= 5) {
      tips.push({
        category: 'overtraining',
        title: '建议休息',
        content: '已连续跑步多天，肌肉需要恢复时间，建议今日休息或进行低强度交叉训练',
        priority: 'medium',
      });
    }

    tips.push(
      {
        category: 'footwear',
        title: '跑鞋选择',
        content: '跑鞋使用超过 500-800 公里后应更换，磨损的跑鞋会增加受伤风险',
        priority: 'medium',
      },
      {
        category: 'nutrition',
        title: '补水与营养',
        content: '跑步前后及时补水，长跑后 30 分钟内补充碳水化合物和蛋白质',
        priority: 'low',
      },
    );

    return tips;
  }

  // ==================== 私有方法 ====================

  private buildUserPlanDetail(userPlan: UserPlan, plan: TrainingPlan): UserPlanDetail {
    const weekSchedule = plan.schedule.find(w => w.week === userPlan.currentWeek);
    const todayTask = weekSchedule?.days.find(d => d.day === userPlan.currentDay);

    const totalDays = plan.durationWeeks * 7;
    const completedDays = (userPlan.currentWeek - 1) * 7 + userPlan.currentDay - 1;
    const completionRate = Math.min(100, Math.round((completedDays / totalDays) * 100));

    return { ...userPlan, plan, todayTask, completionRate };
  }
}
