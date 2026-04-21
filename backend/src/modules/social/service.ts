// src/modules/social/service.ts
import { SocialRepository } from './repository';
import {
  FriendInfo,
  PostDetail,
  CommentDetail,
  LeaderboardEntry,
  CreatePostRequest,
  LeaderboardType,
  LeaderboardScope,
} from './types';
import { AppError } from '../../utils/errors';

export class SocialService {
  constructor(private repository: SocialRepository) {}

  // ==================== 好友系统 ====================

  async searchUsers(keyword: string, currentUserId: string) {
    if (!keyword || keyword.trim().length < 2) throw new AppError('搜索关键词至少2个字符', 400);
    return this.repository.searchUsers(keyword.trim(), currentUserId);
  }

  async sendFriendRequest(userId: string, friendId: string): Promise<void> {
    if (userId === friendId) throw new AppError('不能添加自己为好友', 400);

    const target = await this.repository.getUserById(friendId);
    if (!target) throw new AppError('用户不存在', 404);

    const existing = await this.repository.findFriendship(userId, friendId)
      ?? await this.repository.findFriendship(friendId, userId);

    if (existing) {
      if (existing.status === 1) throw new AppError('已经是好友了', 409);
      if (existing.status === 0) throw new AppError('好友请求已发送，等待对方确认', 409);
    }

    await this.repository.createFriendship(userId, friendId);
  }

  async respondFriendRequest(userId: string, requesterId: string, action: 'accept' | 'reject'): Promise<void> {
    // 请求方是 requesterId，接收方是 userId
    const friendship = await this.repository.findFriendship(requesterId, userId);
    if (!friendship || friendship.status !== 0) throw new AppError('好友请求不存在', 404);

    await this.repository.updateFriendshipStatus(requesterId, userId, action === 'accept' ? 1 : 2);
  }

  async getFriends(userId: string): Promise<FriendInfo[]> {
    const friendships = await this.repository.getAcceptedFriendships(userId);
    const friendIds = friendships.map(f => (f.userId === userId ? f.friendId : f.userId));
    if (friendIds.length === 0) return [];

    const users = await this.repository.getUsersByIds(friendIds);
    const userMap = new Map(users.map((u: any) => [u.id, u]));

    return friendships.map(f => {
      const friendId = f.userId === userId ? f.friendId : f.userId;
      const user = userMap.get(friendId) as any;
      return {
        userId: friendId,
        nickname: user?.nickname ?? '',
        avatar: user?.avatar,
        status: f.status,
        createdAt: f.createdAt,
      };
    });
  }

  async getPendingRequests(userId: string): Promise<FriendInfo[]> {
    const requests = await this.repository.getPendingRequests(userId);
    if (requests.length === 0) return [];

    const requesterIds = requests.map(r => r.userId);
    const users = await this.repository.getUsersByIds(requesterIds);
    const userMap = new Map(users.map((u: any) => [u.id, u]));

    return requests.map(r => {
      const user = userMap.get(r.userId) as any;
      return {
        userId: r.userId,
        nickname: user?.nickname ?? '',
        avatar: user?.avatar,
        status: r.status,
        createdAt: r.createdAt,
      };
    });
  }

  async deleteFriend(userId: string, friendId: string): Promise<void> {
    const friendship = await this.repository.findFriendship(userId, friendId)
      ?? await this.repository.findFriendship(friendId, userId);
    if (!friendship || friendship.status !== 1) throw new AppError('好友关系不存在', 404);
    await this.repository.deleteFriendship(userId, friendId);
  }

  async getFriendActivities(userId: string, friendId: string, limit = 20, offset = 0): Promise<PostDetail[]> {
    const friendship = await this.repository.findFriendship(userId, friendId)
      ?? await this.repository.findFriendship(friendId, userId);
    if (!friendship || friendship.status !== 1) throw new AppError('对方不是你的好友', 403);

    const posts = await this.repository.getPostsByUserIds([friendId], limit, offset);
    return this.enrichPosts(posts, userId);
  }

  // ==================== 社区内容 ====================

  async createPost(userId: string, data: CreatePostRequest): Promise<PostDetail> {
    if (!data.content && (!data.images || data.images.length === 0)) {
      throw new AppError('动态内容不能为空', 400);
    }
    const post = await this.repository.createPost({ userId, ...data });
    return this.enrichPosts([post], userId).then(posts => posts[0]);
  }

  async getPosts(userId: string, type: 'following' | 'recommend', limit = 20, offset = 0): Promise<PostDetail[]> {
    let posts;
    if (type === 'following') {
      const friendIds = await this.repository.getFriendIds(userId);
      posts = await this.repository.getPostsByUserIds([userId, ...friendIds], limit, offset);
    } else {
      posts = await this.repository.getAllPosts(limit, offset);
    }
    return this.enrichPosts(posts, userId);
  }

  async getPostById(postId: string, userId: string): Promise<PostDetail> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('动态不存在', 404);
    return this.enrichPosts([post], userId).then(posts => posts[0]);
  }

  async deletePost(userId: string, postId: string): Promise<void> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('动态不存在', 404);
    if (post.userId !== userId) throw new AppError('无权删除该动态', 403);
    await this.repository.deletePost(postId);
  }

  async toggleLike(userId: string, postId: string): Promise<{ liked: boolean; likeCount: number }> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('动态不存在', 404);

    const existing = await this.repository.findLike(userId, 'post', postId);
    if (existing) {
      await this.repository.deleteLike(userId, 'post', postId);
      await this.repository.incrementPostLike(postId, -1);
      return { liked: false, likeCount: post.likeCount - 1 };
    } else {
      await this.repository.createLike(userId, 'post', postId);
      await this.repository.incrementPostLike(postId, 1);
      return { liked: true, likeCount: post.likeCount + 1 };
    }
  }

  async addComment(userId: string, postId: string, content: string, parentId?: string): Promise<CommentDetail> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('动态不存在', 404);

    const comment = await this.repository.createComment({ userId, postId, content, parentId });
    await this.repository.incrementPostComment(postId, 1);

    const author = await this.repository.getUserById(userId) as any;
    return { ...comment, author: { id: author.id, nickname: author.nickname, avatar: author.avatar } };
  }

  async getComments(postId: string, limit = 20, offset = 0): Promise<CommentDetail[]> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('动态不存在', 404);

    const comments = await this.repository.getCommentsByPostId(postId, limit, offset);
    if (comments.length === 0) return [];

    const authorIds = [...new Set(comments.map(c => c.userId))];
    const authors = await this.repository.getUsersByIds(authorIds);
    const authorMap = new Map(authors.map((a: any) => [a.id, a]));

    return comments.map(c => {
      const author = authorMap.get(c.userId) as any;
      return { ...c, author: { id: author?.id, nickname: author?.nickname, avatar: author?.avatar } };
    });
  }

  async deleteComment(userId: string, postId: string, commentId: string): Promise<void> {
    const comment = await this.repository.getCommentById(commentId);
    if (!comment || comment.postId !== postId) throw new AppError('评论不存在', 404);
    if (comment.userId !== userId) throw new AppError('无权删除该评论', 403);
    await this.repository.deleteComment(commentId);
    await this.repository.incrementPostComment(postId, -1);
  }

  // ==================== 排行榜 ====================

  async getLeaderboard(userId: string, type: LeaderboardType, scope: LeaderboardScope): Promise<LeaderboardEntry[]> {
    const now = new Date();
    let startDate: Date;

    if (type === 'weekly') {
      startDate = new Date(now);
      startDate.setDate(now.getDate() - now.getDay());
      startDate.setHours(0, 0, 0, 0);
    } else {
      startDate = new Date(now.getFullYear(), now.getMonth(), 1);
    }

    const endDate = new Date(now);
    endDate.setHours(23, 59, 59, 999);

    let userIds: string[];
    if (scope === 'friends') {
      const friendIds = await this.repository.getFriendIds(userId);
      userIds = [userId, ...friendIds];
    } else {
      // global: 取所有用户（实际生产应分页/缓存）
      const allUsers = await this.repository.getUsersByIds([]);
      userIds = allUsers.map((u: any) => u.id);
      if (userIds.length === 0) userIds = [userId];
    }

    const stats = await this.repository.getLeaderboard(userIds, startDate, endDate);
    if (stats.length === 0) return [];

    const statUserIds = stats.map((s: any) => s.user_id);
    const users = await this.repository.getUsersByIds(statUserIds);
    const userMap = new Map(users.map((u: any) => [u.id, u]));

    return stats.map((s: any, index: number) => {
      const user = userMap.get(s.user_id) as any;
      return {
        rank: index + 1,
        userId: s.user_id,
        nickname: user?.nickname ?? '',
        avatar: user?.avatar,
        totalDistance: parseInt(s.total_distance),
        runCount: parseInt(s.run_count),
      };
    });
  }

  // ==================== 私有方法 ====================

  private async enrichPosts(posts: any[], userId: string): Promise<PostDetail[]> {
    if (posts.length === 0) return [];

    const authorIds = [...new Set(posts.map(p => p.userId))];
    const authors = await this.repository.getUsersByIds(authorIds);
    const authorMap = new Map(authors.map((a: any) => [a.id, a]));

    const postIds = posts.map(p => p.id);
    const likedIds = await this.repository.getLikedPostIds(userId, postIds);
    const likedSet = new Set(likedIds);

    return posts.map(p => {
      const author = authorMap.get(p.userId) as any;
      return {
        ...p,
        author: { id: author?.id, nickname: author?.nickname, avatar: author?.avatar },
        isLiked: likedSet.has(p.id),
      };
    });
  }
}
