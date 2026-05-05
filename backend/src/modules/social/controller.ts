// src/modules/social/controller.ts
import { Request, Response, NextFunction } from 'express';
import { SocialService } from './service';
import { AppError } from '../../utils/errors';

export class SocialController {
  constructor(private service: SocialService) {}

  private getUserId(req: Request): string {
    const userId = req.user?.userId;
    if (!userId) throw new AppError('请先登录', 401);
    return userId;
  }

  // ==================== 好友系统 ====================

  /** GET /v1/social/users/search?keyword=xxx */
  async searchUsers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const users = await this.service.searchUsers(req.query.keyword as string, userId);
      res.json({ code: 200, message: '搜索成功', data: users });
    } catch (e) { next(e); }
  }

  /** POST /v1/social/friends/request */
  async sendFriendRequest(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { friendId } = req.body;
      if (!friendId) throw new AppError('缺少 friendId 参数', 400);
      await this.service.sendFriendRequest(userId, friendId);
      res.json({ code: 200, message: '好友请求已发送', data: null });
    } catch (e) { next(e); }
  }

  /** PUT /v1/social/friends/respond */
  async respondFriendRequest(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { requesterId, action } = req.body;
      if (!requesterId || !action) throw new AppError('缺少 requesterId 或 action 参数', 400);
      if (!['accept', 'reject'].includes(action)) throw new AppError('action 只能是 accept 或 reject', 400);
      await this.service.respondFriendRequest(userId, requesterId, action);
      res.json({ code: 200, message: action === 'accept' ? '已接受好友请求' : '已拒绝好友请求', data: null });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/friends */
  async getFriends(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const friends = await this.service.getFriends(userId);
      res.json({ code: 200, message: '获取好友列表成功', data: friends });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/friends/requests */
  async getPendingRequests(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const requests = await this.service.getPendingRequests(userId);
      res.json({ code: 200, message: '获取好友请求成功', data: requests });
    } catch (e) { next(e); }
  }

  /** DELETE /v1/social/friends/:friendId */
  async deleteFriend(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      await this.service.deleteFriend(userId, req.params.friendId);
      res.json({ code: 200, message: '已删除好友', data: null });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/friends/:friendId/activities */
  async getFriendActivities(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const limit = parseInt(req.query.limit as string) || 20;
      const offset = parseInt(req.query.offset as string) || 0;
      const posts = await this.service.getFriendActivities(userId, req.params.friendId, limit, offset);
      res.json({ code: 200, message: '获取好友动态成功', data: posts });
    } catch (e) { next(e); }
  }

  // ==================== 社区内容 ====================

  /** POST /v1/social/posts */
  async createPost(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const post = await this.service.createPost(userId, req.body);
      res.status(201).json({ code: 200, message: '动态发布成功', data: post });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/posts?type=following&limit=20&offset=0 */
  async getPosts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const type = (req.query.type as string) === 'following' ? 'following' : 'recommend';
      const limit = parseInt(req.query.limit as string) || 20;
      const offset = parseInt(req.query.offset as string) || 0;
      const posts = await this.service.getPosts(userId, type, limit, offset);
      res.json({ code: 200, message: '获取动态列表成功', data: posts });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/posts/:postId */
  async getPostById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const post = await this.service.getPostById(req.params.postId, userId);
      res.json({ code: 200, message: '获取动态成功', data: post });
    } catch (e) { next(e); }
  }

  /** DELETE /v1/social/posts/:postId */
  async deletePost(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      await this.service.deletePost(userId, req.params.postId);
      res.json({ code: 200, message: '动态已删除', data: null });
    } catch (e) { next(e); }
  }

  /** POST /v1/social/posts/:postId/like */
  async toggleLike(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const result = await this.service.toggleLike(userId, req.params.postId);
      res.json({ code: 200, message: result.liked ? '点赞成功' : '已取消点赞', data: result });
    } catch (e) { next(e); }
  }

  /** POST /v1/social/posts/:postId/comments */
  async addComment(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const { content, parentId } = req.body;
      if (!content) throw new AppError('评论内容不能为空', 400);
      const comment = await this.service.addComment(userId, req.params.postId, content, parentId);
      res.status(201).json({ code: 200, message: '评论成功', data: comment });
    } catch (e) { next(e); }
  }

  /** GET /v1/social/posts/:postId/comments */
  async getComments(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      this.getUserId(req);
      const limit = parseInt(req.query.limit as string) || 20;
      const offset = parseInt(req.query.offset as string) || 0;
      const comments = await this.service.getComments(req.params.postId, limit, offset);
      res.json({ code: 200, message: '获取评论成功', data: comments });
    } catch (e) { next(e); }
  }

  /** DELETE /v1/social/posts/:postId/comments/:commentId */
  async deleteComment(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      await this.service.deleteComment(userId, req.params.postId, req.params.commentId);
      res.json({ code: 200, message: '评论已删除', data: null });
    } catch (e) { next(e); }
  }

  // ==================== 排行榜 ====================

  /** GET /v1/social/leaderboard?type=weekly&scope=friends */
  async getLeaderboard(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.getUserId(req);
      const type = (req.query.type as string) === 'monthly' ? 'monthly' : 'weekly';
      const scope = (req.query.scope as string) === 'global' ? 'global' : 'friends';
      const data = await this.service.getLeaderboard(userId, type, scope);
      res.json({ code: 200, message: '获取排行榜成功', data });
    } catch (e) { next(e); }
  }
}
