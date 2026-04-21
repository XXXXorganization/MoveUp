// src/modules/challenge/repository.ts
import { Knex } from 'knex';
import {
  BadgeModel, TaskModel, UserTaskModel, UserAchievementModel,
  ChallengeModel, UserChallengeModel, MembershipPlanModel,
  UserMembershipModel, PointsLogModel,
} from './model';

export class ChallengeRepository {
  private badgeModel: BadgeModel;
  private taskModel: TaskModel;
  private userTaskModel: UserTaskModel;
  private userAchievementModel: UserAchievementModel;
  private challengeModel: ChallengeModel;
  private userChallengeModel: UserChallengeModel;
  private membershipPlanModel: MembershipPlanModel;
  private userMembershipModel: UserMembershipModel;
  private pointsLogModel: PointsLogModel;

  constructor(private db: Knex) {
    this.badgeModel = new BadgeModel(db);
    this.taskModel = new TaskModel(db);
    this.userTaskModel = new UserTaskModel(db);
    this.userAchievementModel = new UserAchievementModel(db);
    this.challengeModel = new ChallengeModel(db);
    this.userChallengeModel = new UserChallengeModel(db);
    this.membershipPlanModel = new MembershipPlanModel(db);
    this.userMembershipModel = new UserMembershipModel(db);
    this.pointsLogModel = new PointsLogModel(db);
  }

  // Badges
  getAllBadges() { return this.badgeModel.findAll(); }
  getBadgeById(id: string) { return this.badgeModel.findById(id); }
  getBadgesByIds(ids: string[]) { return this.badgeModel.findByIds(ids); }

  // Tasks
  getActiveTasks(taskType?: string) { return this.taskModel.findActive(taskType); }
  getTaskById(id: string) { return this.taskModel.findById(id); }

  // User Tasks
  getUserTasks(userId: string) { return this.userTaskModel.findByUserId(userId); }
  getUserTask(userId: string, taskId: string) { return this.userTaskModel.findOne(userId, taskId); }
  upsertUserTask(userId: string, taskId: string, progress: number, isCompleted: boolean) {
    return this.userTaskModel.upsert(userId, taskId, progress, isCompleted);
  }

  // Achievements
  getUserAchievements(userId: string) { return this.userAchievementModel.findByUserId(userId); }
  getUserAchievement(userId: string, badgeId: string) { return this.userAchievementModel.findOne(userId, badgeId); }
  createUserAchievement(userId: string, badgeId: string) { return this.userAchievementModel.create(userId, badgeId); }

  // Challenges
  getChallengesByStatus(status: 'ongoing' | 'upcoming' | 'completed') { return this.challengeModel.findByStatus(status); }
  getChallengeById(id: string) { return this.challengeModel.findById(id); }

  // User Challenges
  getUserChallenges(userId: string) { return this.userChallengeModel.findByUserId(userId); }
  getUserChallenge(userId: string, challengeId: string) { return this.userChallengeModel.findOne(userId, challengeId); }
  joinChallenge(userId: string, challengeId: string) { return this.userChallengeModel.create(userId, challengeId); }
  updateChallengeProgress(userId: string, challengeId: string, progress: number, isCompleted: boolean) {
    return this.userChallengeModel.updateProgress(userId, challengeId, progress, isCompleted);
  }
  getChallengeRanking(challengeId: string) { return this.userChallengeModel.getRanking(challengeId); }

  // Membership Plans
  getMembershipPlans() { return this.membershipPlanModel.findActive(); }
  getMembershipPlanById(id: string) { return this.membershipPlanModel.findById(id); }

  // User Memberships
  getActiveMembership(userId: string) { return this.userMembershipModel.findActiveByUserId(userId); }
  getMembershipHistory(userId: string) { return this.userMembershipModel.findByUserId(userId); }
  createMembership(userId: string, planId: string, startDate: Date, endDate: Date) {
    return this.userMembershipModel.create(userId, planId, startDate, endDate);
  }

  // Points
  getPointsLogs(userId: string, limit: number, offset: number) { return this.pointsLogModel.findByUserId(userId, limit, offset); }
  getTotalPoints(userId: string) { return this.pointsLogModel.getTotalPoints(userId); }
  addPoints(userId: string, points: number, sourceType: string, sourceId?: string, description?: string) {
    return this.pointsLogModel.create(userId, points, sourceType, sourceId, description);
  }
}
