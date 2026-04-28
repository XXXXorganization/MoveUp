// tests/unit/coaching.service.test.ts
import { CoachingService } from '../../src/modules/coaching/service';
import { CoachingRepository } from '../../src/modules/coaching/repository';
import { TrainingPlan } from '../../src/modules/coaching/types';

const mockRepository: jest.Mocked<CoachingRepository> = {
  getAllTrainingPlans: jest.fn(),
  getTrainingPlanById: jest.fn(),
  getTrainingPlansByDifficulty: jest.fn(),
  createUserPlan: jest.fn(),
  getUserPlanById: jest.fn(),
  getActivePlanByUserId: jest.fn(),
  updateUserPlan: jest.fn(),
  deactivateAllUserPlans: jest.fn(),
} as unknown as jest.Mocked<CoachingRepository>;

function makeService() {
  return new CoachingService(mockRepository);
}

beforeEach(() => {
  jest.clearAllMocks();
});

// ==================== recommendPlan ====================

describe('recommendPlan', () => {
  const plans: TrainingPlan[] = [
    { id: 'p1', name: '入门5公里', difficulty: 'beginner', targetDistance: 5000, durationWeeks: 8, schedule: [] },
    { id: 'p2', name: '进阶10公里', difficulty: 'beginner', targetDistance: 10000, durationWeeks: 12, schedule: [] },
    { id: 'p3', name: '半马', difficulty: 'beginner', targetDistance: 21097, durationWeeks: 16, schedule: [] },
    { id: 'p4', name: '全马', difficulty: 'beginner', targetDistance: 42195, durationWeeks: 20, schedule: [] },
  ];

  it('应返回最接近目标距离的最多 3 个计划', async () => {
    mockRepository.getTrainingPlansByDifficulty.mockResolvedValue(plans);

    const svc = makeService();
    const result = await svc.recommendPlan('u1', { fitnessLevel: 'beginner', targetDistance: 10000, weeklyFrequency: 3, goalType: 'health' });

    expect(result).toHaveLength(3);
    // 最接近 10000 的应排在第一位
    expect(result[0].id).toBe('p2');
  });

  it('可用计划不足 3 个时应返回全部', async () => {
    mockRepository.getTrainingPlansByDifficulty.mockResolvedValue([plans[0]]);

    const svc = makeService();
    const result = await svc.recommendPlan('u1', { fitnessLevel: 'beginner', targetDistance: 5000, weeklyFrequency: 3, goalType: 'health' });

    expect(result).toHaveLength(1);
  });

  it('应按照与目标距离的差值升序排序', async () => {
    mockRepository.getTrainingPlansByDifficulty.mockResolvedValue(plans);

    const svc = makeService();
    const result = await svc.recommendPlan('u1', { fitnessLevel: 'beginner', targetDistance: 21097, weeklyFrequency: 4, goalType: 'distance' });

    // 差值: p3=0, p2=11097, p1=16097, p4=21098 → 前3为 p3, p2, p1
    expect(result[0].id).toBe('p3');
    expect(result[1].id).toBe('p2');
    expect(result[2].id).toBe('p1');
  });

  it('targetDistance 为 0 的计划应被过滤', async () => {
    const plansWithZero: TrainingPlan[] = [
      { id: 'p0', name: '无目标', difficulty: 'beginner', targetDistance: 0, durationWeeks: 4, schedule: [] },
      ...plans,
    ];
    mockRepository.getTrainingPlansByDifficulty.mockResolvedValue(plansWithZero);

    const svc = makeService();
    const result = await svc.recommendPlan('u1', { fitnessLevel: 'beginner', targetDistance: 5000, weeklyFrequency: 3, goalType: 'health' });

    expect(result.every(p => p.targetDistance > 0)).toBe(true);
  });
});

// ==================== getVoiceGuidanceConfig ====================

describe('getVoiceGuidanceConfig', () => {
  it('距离在区间整数倍附近时应播报距离配速消息', () => {
    const svc = makeService();
    // distanceInterval 默认 1000m，distanceM=1005，1005%1000=5 < 10，触发
    const messages = svc.getVoiceGuidanceConfig(1005, 360);
    expect(messages).toHaveLength(1);
    expect(messages[0]).toContain('已跑');
    expect(messages[0]).toContain('公里');
  });

  it('距离不在区间触发位置时应返回空数组', () => {
    const svc = makeService();
    const messages = svc.getVoiceGuidanceConfig(500, 360);
    expect(messages).toHaveLength(0);
  });

  it('配速超过阈值时应播报提速提示', () => {
    const svc = makeService();
    const messages = svc.getVoiceGuidanceConfig(500, 400, undefined, {
      distanceInterval: 1000,
      paceAlertEnabled: true,
      paceThreshold: 360,
      heartRateAlertEnabled: false,
    });
    expect(messages).toContain('配速偏慢，请适当加速');
  });

  it('心率超过阈值时应播报降速提示', () => {
    const svc = makeService();
    const messages = svc.getVoiceGuidanceConfig(500, 300, 180, {
      distanceInterval: 1000,
      paceAlertEnabled: false,
      heartRateAlertEnabled: true,
      heartRateThreshold: 170,
    });
    expect(messages).toContain('心率过高，建议放慢速度');
  });

  it('心率未超阈值时不应播报降速提示', () => {
    const svc = makeService();
    const messages = svc.getVoiceGuidanceConfig(500, 300, 150, {
      distanceInterval: 1000,
      paceAlertEnabled: false,
      heartRateAlertEnabled: true,
      heartRateThreshold: 170,
    });
    expect(messages).not.toContain('心率过高，建议放慢速度');
  });
});

// ==================== getSegmentAdvice ====================

describe('getSegmentAdvice', () => {
  it('心率 >= 90% 最大心率时应返回极限区', () => {
    const svc = makeService();
    // age=25, maxHR=195, 95% = 185.25
    const advice = svc.getSegmentAdvice(186, 25);
    expect(advice.heartRateZone).toBe('max');
    expect(advice.shouldSlowDown).toBe(true);
  });

  it('心率 80-90% 最大心率时应返回无氧区', () => {
    const svc = makeService();
    // 85% of 195 = 165.75
    const advice = svc.getSegmentAdvice(166, 25);
    expect(advice.heartRateZone).toBe('anaerobic');
    expect(advice.shouldSlowDown).toBe(true);
  });

  it('心率 70-80% 最大心率时应返回有氧区', () => {
    const svc = makeService();
    // 75% of 195 = 146.25
    const advice = svc.getSegmentAdvice(147, 25);
    expect(advice.heartRateZone).toBe('aerobic');
    expect(advice.shouldSlowDown).toBeFalsy();
  });

  it('心率 60-70% 最大心率时应返回燃脂区', () => {
    const svc = makeService();
    // 65% of 195 = 126.75
    const advice = svc.getSegmentAdvice(127, 25);
    expect(advice.heartRateZone).toBe('fat_burn');
  });

  it('心率 < 60% 最大心率时应返回热身区', () => {
    const svc = makeService();
    // 50% of 195 = 97.5
    const advice = svc.getSegmentAdvice(98, 25);
    expect(advice.heartRateZone).toBe('warmup');
  });

  it('应正确使用年龄计算最大心率 (220 - age)', () => {
    const svc = makeService();
    // age=30, maxHR=190, 95% = 180.5
    const advice = svc.getSegmentAdvice(181, 30);
    expect(advice.heartRateZone).toBe('max');
  });

  it('应返回 currentHeartRate 字段', () => {
    const svc = makeService();
    const advice = svc.getSegmentAdvice(150, 25);
    expect(advice.currentHeartRate).toBe(150);
  });
});

// ==================== getStretchingGuide ====================

describe('getStretchingGuide', () => {
  it('跑步不足 30 分钟时应返回 4 个基础拉伸动作', () => {
    const svc = makeService();
    const guide = svc.getStretchingGuide(1799);
    expect(guide.exercises).toHaveLength(4);
    expect(guide.runDuration).toBe(1799);
  });

  it('跑步恰好达到 30 分钟时应返回 6 个拉伸动作', () => {
    const svc = makeService();
    const guide = svc.getStretchingGuide(1800);
    expect(guide.exercises).toHaveLength(6);
  });

  it('跑步超过 30 分钟时应包含 IT 髂胫束和臀部拉伸', () => {
    const svc = makeService();
    const guide = svc.getStretchingGuide(3600);
    const names = guide.exercises.map(e => e.name);
    expect(names).toContain('IT 髂胫束拉伸');
    expect(names).toContain('臀部拉伸');
  });
});

// ==================== getInjuryPreventionTips ====================

describe('getInjuryPreventionTips', () => {
  it('始终包含热身和拉伸提示', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(10000, 2);
    const categories = tips.map(t => t.category);
    expect(categories).toContain('warmup');
    expect(categories).toContain('cooldown');
  });

  it('周跑量 > 50km 时应包含过度训练警告', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(51000, 2);
    expect(tips.some(t => t.category === 'overtraining' && t.title === '注意过度训练')).toBe(true);
  });

  it('周跑量 <= 50km 时不应包含过度训练警告', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(50000, 2);
    expect(tips.some(t => t.title === '注意过度训练')).toBe(false);
  });

  it('连续跑步 >= 5 次时应建议休息', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(10000, 5);
    expect(tips.some(t => t.title === '建议休息')).toBe(true);
  });

  it('连续跑步 < 5 次时不应出现建议休息', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(10000, 4);
    expect(tips.some(t => t.title === '建议休息')).toBe(false);
  });

  it('始终包含跑鞋和补水建议', () => {
    const svc = makeService();
    const tips = svc.getInjuryPreventionTips(10000, 0);
    const categories = tips.map(t => t.category);
    expect(categories).toContain('footwear');
    expect(categories).toContain('nutrition');
  });
});
