// src/modules/social/repository.ts
import { Knex } from 'knex';
import { FriendshipModel, PostModel, CommentModel, LikeModel } from './model';
import { Post, Comment } from './types';

export class SocialRepository {
  private friendshipModel: FriendshipModel;
  private postModel: PostModel;
  private commentModel: CommentModel;
  private likeModel: LikeModel;

  constructor(private db: Knex) {
    this.friendshipModel = new FriendshipModel(db);
    this.postModel = new PostModel(db);
    this.commentModel = new CommentModel(db);
    this.likeModel = new LikeModel(db);
  }

  // ==================== 好友 ====================

  findFriendship(userId: string, friendId: string) {
    return this.friendshipModel.find(userId, friendId);
  }

  getAcceptedFriendships(userId: string) {
    return this.friendshipModel.findAccepted(userId);
  }

  getPendingRequests(userId: string) {
    return this.friendshipModel.findPendingReceived(userId);
  }

  createFriendship(userId: string, friendId: string) {
    return this.friendshipModel.create(userId, friendId);
  }

  updateFriendshipStatus(userId: string, friendId: string, status: 0 | 1 | 2) {
    return this.friendshipModel.updateStatus(userId, friendId, status);
  }

  deleteFriendship(userId: string, friendId: string) {
    return this.friendshipModel.delete(userId, friendId);
  }

  getFriendIds(userId: string) {
    return this.friendshipModel.getFriendIds(userId);
  }

  searchUsers(keyword: string, currentUserId: string) {
    return this.db('users')
      .where(q => q.where('phone', keyword).orWhere('nickname', 'ilike', `%${keyword}%`))
      .andWhereNot('id', currentUserId)
      .select('id', 'nickname', 'avatar', 'phone')
      .limit(20);
  }

  getUserById(id: string) {
    return this.db('users').where({ id }).select('id', 'nickname', 'avatar').first();
  }

  getUsersByIds(ids: string[]) {
    return this.db('users').whereIn('id', ids).select('id', 'nickname', 'avatar');
  }

  // ==================== 动态 ====================

  getPostById(id: string) {
    return this.postModel.findById(id);
  }

  getPostsByUserIds(userIds: string[], limit: number, offset: number) {
    return this.postModel.findByUserIds(userIds, limit, offset);
  }

  getAllPosts(limit: number, offset: number) {
    return this.postModel.findAll(limit, offset);
  }

  createPost(data: Omit<Post, 'id' | 'likeCount' | 'commentCount' | 'createdAt'>) {
    return this.postModel.create(data);
  }

  deletePost(id: string) {
    return this.postModel.delete(id);
  }

  incrementPostLike(id: string, delta: 1 | -1) {
    return this.postModel.incrementLike(id, delta);
  }

  incrementPostComment(id: string, delta: 1 | -1) {
    return this.postModel.incrementComment(id, delta);
  }

  // ==================== 评论 ====================

  getCommentById(id: string) {
    return this.commentModel.findById(id);
  }

  getCommentsByPostId(postId: string, limit: number, offset: number) {
    return this.commentModel.findByPostId(postId, limit, offset);
  }

  createComment(data: Omit<Comment, 'id' | 'likeCount' | 'createdAt'>) {
    return this.commentModel.create(data);
  }

  deleteComment(id: string) {
    return this.commentModel.delete(id);
  }

  // ==================== 点赞 ====================

  findLike(userId: string, targetType: string, targetId: string) {
    return this.likeModel.find(userId, targetType, targetId);
  }

  createLike(userId: string, targetType: string, targetId: string) {
    return this.likeModel.create(userId, targetType, targetId);
  }

  deleteLike(userId: string, targetType: string, targetId: string) {
    return this.likeModel.delete(userId, targetType, targetId);
  }

  getLikedPostIds(userId: string, postIds: string[]) {
    return this.likeModel.findByUserAndTargets(userId, 'post', postIds);
  }

  // ==================== 排行榜 ====================

  getLeaderboard(userIds: string[], startDate: Date, endDate: Date) {
    return this.db('sport_records')
      .whereIn('user_id', userIds)
      .where('start_time', '>=', startDate)
      .where('start_time', '<', endDate)
      .where('status', 'completed')
      .groupBy('user_id')
      .select(
        'user_id',
        this.db.raw('SUM(distance) as total_distance'),
        this.db.raw('COUNT(*) as run_count'),
      )
      .orderBy('total_distance', 'desc');
  }
}
