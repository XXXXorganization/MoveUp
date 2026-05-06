// tests/unit/challenge.service.test.ts
import { ChallengeService } from '../../src/modules/challenge/service';
import { ChallengeRepository } from '../../src/modules/challenge/repository';

const mockRepo: jest.Mocked<ChallengeRepository> = {
  getActiveTasks: jest.fn(),
  getUserTasks: jest.fn(),
  getTaskById: jest.fn(),
  getBadgesByIds: jest.fn(),
  getUserTask: jest.fn(),
  upsertUserTask: jest.fn(),
  addPoints: jest.fn(),
  getUserAchievement: jest.fn(),
  createUserAchievement: jest.fn(),
  getBadgeById: jest.fn(),
  getAllBadges: jest.fn(),
  getUserAchievements: jest.fn(),
  getChallengesByStatus: jest.fn(),
  getChallengeById: jest.fn(),
  getUserChallenge: jest.fn(),
  joinChallenge: jest.fn(),
  getUserChallenges: jest.fn(),
  updateChallengeProgress: jest.fn(),
  getChallengeRanking: jest.fn(),
  getMembershipPlans: jest.fn(),
  getMembershipPlanById: jest.fn(),
  getActiveMembership: jest.fn(),
  createMembership: jest.fn(),
  getTotalPoints: jest.fn(),
  getPointsLogs: jest.fn(),
} as any as jest.Mocked<ChallengeRepository>;

const svc = () => new ChallengeService(mockRepo);

beforeEach(() => jest.clearAllMocks());

// ==================== updateTaskProgress ====================

describe('updateTaskProgress', () => {
  const baseTask = {
    id: 't1', name: '跑步5公里', requirement: { target: 5000 },
    rewardPoints: 100, rewardBadgeId: null,
  };

  it('任务不存在时应抛出 404', async () => {
    mockRepo.getTaskById.mockResolvedValue(null);
    await expect(svc().updateTaskProgress('u1', { taskId: 't1', progress: 1000 }))
      .rejects.toMatchObject({ code: 404 });
  });

  it('任务已完成时应抛出 409', async () => {
    mockRepo.getTaskById.mockResolvedValue(baseTask as any);
    mockRepo.getUserTask.mockResolvedValue({ isCompleted: true } as any);
    await expect(svc().updateTaskProgress('u1', { taskId: 't1', progress: 5000 }))
      .rejects.toMatchObject({ code: 409 });
  });

  it('进度未达目标时不应发放奖励', async () => {
    mockRepo.getTaskById.mockResolvedValue(baseTask as any);
    mockRepo.getUserTask.mockResolvedValue(null);
    mockRepo.upsertUserTask.mockResolvedValue({ taskId: 't1', isCompleted: false } as any);
    mockRepo.getBadgeById.mockResolvedValue(null);

    await svc().updateTaskProgress('u1', { taskId: 't1', progress: 3000 });
    expect(mockRepo.addPoints).not.toHaveBeenCalled();
  });

  it('进度达标时应发放积分', async () => {
    mockRepo.getTaskById.mockResolvedValue(baseTask as any);
    mockRepo.getUserTask.mockResolvedValue(null);
    mockRepo.upsertUserTask.mockResolvedValue({ taskId: 't1', isCompleted: true } as any);
    mockRepo.addPoints.mockResolvedValue(null as any);
    mockRepo.getBadgeById.mockResolvedValue(null);

    await svc().updateTaskProgress('u1', { taskId: 't1', progress: 5000 });
    expect(mockRepo.addPoints).toHaveBeenCalledWith('u1', 100, 'task', 't1', expect.any(String));
  });

  it('达标且有徽章奖励时应发放徽章', async () => {
    const taskWithBadge = { ...baseTask, rewardBadgeId: 'b1' };
    mockRepo.getTaskById.mockResolvedValue(taskWithBadge as any);
    mockRepo.getUserTask.mockResolvedValue(null);
    mockRepo.upsertUserTask.mockResolvedValue({ taskId: 't1', isCompleted: true } as any);
    mockRepo.addPoints.mockResolvedValue(null as any);
    mockRepo.getUserAchievement.mockResolvedValue(null);
    mockRepo.createUserAchievement.mockResolvedValue(null as any);
    mockRepo.getBadgeById.mockResolvedValue({ id: 'b1', name: '首跑达人' } as any);

    await svc().updateTaskProgress('u1', { taskId: 't1', progress: 5000 });
    expect(mockRepo.createUserAchievement).toHaveBeenCalledWith('u1', 'b1');
  });

  it('已有徽章时不应重复发放', async () => {
    const taskWithBadge = { ...baseTask, rewardBadgeId: 'b1' };
    mockRepo.getTaskById.mockResolvedValue(taskWithBadge as any);
    mockRepo.getUserTask.mockResolvedValue(null);
    mockRepo.upsertUserTask.mockResolvedValue({ taskId: 't1', isCompleted: true } as any);
    mockRepo.addPoints.mockResolvedValue(null as any);
    mockRepo.getUserAchievement.mockResolvedValue({ id: 'a1' } as any);
    mockRepo.getBadgeById.mockResolvedValue({ id: 'b1' } as any);

    await svc().updateTaskProgress('u1', { taskId: 't1', progress: 5000 });
    expect(mockRepo.createUserAchievement).not.toHaveBeenCalled();
  });
});

// ==================== joinChallenge ====================

describe('joinChallenge', () => {
  const now = new Date();
  const ongoing = {
    id: 'c1', name: '月度马拉松',
    startTime: new Date(now.getTime() - 86400000),
    endTime: new Date(now.getTime() + 86400000),
    targetValue: 42195, rewardPoints: 500, rewardBadgeId: null,
  };

  it('挑战不存在时应抛出 404', async () => {
    mockRepo.getChallengeById.mockResolvedValue(null);
    await expect(svc().joinChallenge('u1', 'c1')).rejects.toMatchObject({ code: 404 });
  });

  it('挑战已结束时应抛出 400', async () => {
    mockRepo.getChallengeById.mockResolvedValue({
      ...ongoing,
      endTime: new Date(now.getTime() - 1000),
    } as any);
    await expect(svc().joinChallenge('u1', 'c1')).rejects.toMatchObject({ code: 400 });
  });

  it('挑战尚未开始时应抛出 400', async () => {
    mockRepo.getChallengeById.mockResolvedValue({
      ...ongoing,
      startTime: new Date(now.getTime() + 86400000),
    } as any);
    await expect(svc().joinChallenge('u1', 'c1')).rejects.toMatchObject({ code: 400 });
  });

  it('已参与时应抛出 409', async () => {
    mockRepo.getChallengeById.mockResolvedValue(ongoing as any);
    mockRepo.getUserChallenge.mockResolvedValue({ id: 'uc1' } as any);
    await expect(svc().joinChallenge('u1', 'c1')).rejects.toMatchObject({ code: 409 });
  });

  it('正常参与时应返回用户挑战详情', async () => {
    mockRepo.getChallengeById.mockResolvedValue(ongoing as any);
    mockRepo.getUserChallenge.mockResolvedValue(null);
    mockRepo.joinChallenge.mockResolvedValue({ challengeId: 'c1', userId: 'u1' } as any);

    const result = await svc().joinChallenge('u1', 'c1');
    expect(result.challenge.id).toBe('c1');
  });
});

// ==================== updateChallengeProgress ====================

describe('updateChallengeProgress', () => {
  const challenge = {
    id: 'c1', name: '月度马拉松', targetValue: 42195,
    rewardPoints: 500, rewardBadgeId: null,
  };

  it('挑战不存在时应抛出 404', async () => {
    mockRepo.getChallengeById.mockResolvedValue(null);
    await expect(svc().updateChallengeProgress('u1', 'c1', 1000)).rejects.toMatchObject({ code: 404 });
  });

  it('未参与挑战时应抛出 404', async () => {
    mockRepo.getChallengeById.mockResolvedValue(challenge as any);
    mockRepo.getUserChallenge.mockResolvedValue(null);
    await expect(svc().updateChallengeProgress('u1', 'c1', 1000)).rejects.toMatchObject({ code: 404 });
  });

  it('进度未达目标时不发放奖励', async () => {
    mockRepo.getChallengeById.mockResolvedValue(challenge as any);
    mockRepo.getUserChallenge.mockResolvedValue({ isCompleted: false } as any);
    mockRepo.updateChallengeProgress.mockResolvedValue({ challengeId: 'c1', isCompleted: false } as any);

    await svc().updateChallengeProgress('u1', 'c1', 10000);
    expect(mockRepo.addPoints).not.toHaveBeenCalled();
  });

  it('进度达标时应发放奖励积分', async () => {
    mockRepo.getChallengeById.mockResolvedValue(challenge as any);
    mockRepo.getUserChallenge.mockResolvedValue({ isCompleted: false } as any);
    mockRepo.updateChallengeProgress.mockResolvedValue({ challengeId: 'c1', isCompleted: true } as any);
    mockRepo.addPoints.mockResolvedValue(null as any);

    await svc().updateChallengeProgress('u1', 'c1', 42195);
    expect(mockRepo.addPoints).toHaveBeenCalledWith('u1', 500, 'challenge', 'c1', expect.any(String));
  });
});

// ==================== getActiveMembership ====================

describe('getActiveMembership', () => {
  it('无有效会员时应返回 null', async () => {
    mockRepo.getActiveMembership.mockResolvedValue(null);
    const result = await svc().getActiveMembership('u1');
    expect(result).toBeNull();
  });

  it('有会员时应返回包含 daysRemaining 的详情', async () => {
    const endDate = new Date(Date.now() + 10 * 86400000); // 10天后
    mockRepo.getActiveMembership.mockResolvedValue({ planId: 'p1', endDate } as any);
    mockRepo.getMembershipPlanById.mockResolvedValue({ id: 'p1', name: '月度会员' } as any);

    const result = await svc().getActiveMembership('u1');
    expect(result).not.toBeNull();
    expect(result!.daysRemaining).toBeGreaterThan(0);
    expect(result!.daysRemaining).toBeLessThanOrEqual(10);
  });
});

// ==================== purchaseMembership ====================

describe('purchaseMembership', () => {
  it('套餐不存在时应抛出 404', async () => {
    mockRepo.getMembershipPlanById.mockResolvedValue(null);
    await expect(svc().purchaseMembership('u1', 'p1')).rejects.toMatchObject({ code: 404 });
  });

  it('套餐未激活时应抛出 404', async () => {
    mockRepo.getMembershipPlanById.mockResolvedValue({ id: 'p1', isActive: false } as any);
    await expect(svc().purchaseMembership('u1', 'p1')).rejects.toMatchObject({ code: 404 });
  });

  it('无现有会员时应从今天开始', async () => {
    const plan = { id: 'p1', isActive: true, durationDays: 30 };
    mockRepo.getMembershipPlanById.mockResolvedValue(plan as any);
    mockRepo.getActiveMembership.mockResolvedValue(null);
    const endDate = new Date(Date.now() + 30 * 86400000);
    mockRepo.createMembership.mockResolvedValue({ planId: 'p1', endDate } as any);

    const result = await svc().purchaseMembership('u1', 'p1');
    expect(result.daysRemaining).toBeGreaterThan(0);
  });

  it('有现有会员时应续费（从现有到期日顺延）', async () => {
    const plan = { id: 'p1', isActive: true, durationDays: 30 };
    const existingEndDate = new Date(Date.now() + 5 * 86400000);
    mockRepo.getMembershipPlanById.mockResolvedValue(plan as any);
    mockRepo.getActiveMembership.mockResolvedValue({ planId: 'p1', endDate: existingEndDate } as any);

    const newEndDate = new Date(existingEndDate.getTime() + 30 * 86400000);
    mockRepo.createMembership.mockResolvedValue({ planId: 'p1', endDate: newEndDate } as any);

    await svc().purchaseMembership('u1', 'p1');
    // startDate 应传入现有的 endDate
    expect(mockRepo.createMembership).toHaveBeenCalledWith(
      'u1', 'p1', existingEndDate, expect.any(Date),
    );
  });
});

// ==================== getPointsSummary ====================

describe('getPointsSummary', () => {
  it('应返回总积分', async () => {
    mockRepo.getTotalPoints.mockResolvedValue(350);
    const result = await svc().getPointsSummary('u1');
    expect(result.totalPoints).toBe(350);
  });
});
