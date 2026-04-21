// src/modules/social/model.ts
import { Knex } from 'knex';
import { Friendship, Post, Comment } from './types';

export class FriendshipModel {
  private tableName = 'friendships';
  constructor(private db: Knex) {}

  async find(userId: string, friendId: string): Promise<Friendship | null> {
    const row = await this.db(this.tableName).where({ user_id: userId, friend_id: friendId }).first();
    return row ? this.mapToModel(row) : null;
  }

  async findAccepted(userId: string): Promise<Friendship[]> {
    const rows = await this.db(this.tableName)
      .where(q => q.where({ user_id: userId, status: 1 }).orWhere({ friend_id: userId, status: 1 }));
    return rows.map(r => this.mapToModel(r));
  }

  async findPendingReceived(userId: string): Promise<Friendship[]> {
    const rows = await this.db(this.tableName).where({ friend_id: userId, status: 0 });
    return rows.map(r => this.mapToModel(r));
  }

  async create(userId: string, friendId: string): Promise<Friendship> {
    await this.db(this.tableName).insert({ user_id: userId, friend_id: friendId, status: 0 });
    return (await this.find(userId, friendId))!;
  }

  async updateStatus(userId: string, friendId: string, status: 0 | 1 | 2): Promise<void> {
    await this.db(this.tableName)
      .where({ user_id: userId, friend_id: friendId })
      .update({ status, updated_at: new Date() });
  }

  async delete(userId: string, friendId: string): Promise<void> {
    await this.db(this.tableName)
      .where(q => q.where({ user_id: userId, friend_id: friendId }).orWhere({ user_id: friendId, friend_id: userId }))
      .del();
  }

  async getFriendIds(userId: string): Promise<string[]> {
    const rows = await this.db(this.tableName)
      .where(q => q.where({ user_id: userId, status: 1 }).orWhere({ friend_id: userId, status: 1 }));
    return rows.map(r => (r.user_id === userId ? r.friend_id : r.user_id));
  }

  private mapToModel(row: any): Friendship {
    return {
      userId: row.user_id,
      friendId: row.friend_id,
      status: row.status,
      createdAt: new Date(row.created_at),
      updatedAt: new Date(row.updated_at),
    };
  }
}

export class PostModel {
  private tableName = 'posts';
  constructor(private db: Knex) {}

  async findById(id: string): Promise<Post | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.mapToModel(row) : null;
  }

  async findByUserIds(userIds: string[], limit: number, offset: number): Promise<Post[]> {
    const rows = await this.db(this.tableName)
      .whereIn('user_id', userIds)
      .orderBy('created_at', 'desc')
      .limit(limit)
      .offset(offset);
    return rows.map(r => this.mapToModel(r));
  }

  async findAll(limit: number, offset: number): Promise<Post[]> {
    const rows = await this.db(this.tableName)
      .orderBy('created_at', 'desc')
      .limit(limit)
      .offset(offset);
    return rows.map(r => this.mapToModel(r));
  }

  async create(data: Omit<Post, 'id' | 'likeCount' | 'commentCount' | 'createdAt'>): Promise<Post> {
    const [created] = await this.db(this.tableName)
      .insert({
        user_id: data.userId,
        content: data.content,
        images: data.images ? JSON.stringify(data.images) : null,
        sport_record_id: data.sportRecordId,
        location: data.location,
        tags: data.tags ? JSON.stringify(data.tags) : null,
      })
      .returning('*');
    return this.mapToModel(created);
  }

  async delete(id: string): Promise<void> {
    await this.db(this.tableName).where({ id }).del();
  }

  async incrementLike(id: string, delta: 1 | -1): Promise<void> {
    await this.db(this.tableName).where({ id }).increment('like_count', delta);
  }

  async incrementComment(id: string, delta: 1 | -1): Promise<void> {
    await this.db(this.tableName).where({ id }).increment('comment_count', delta);
  }

  private mapToModel(row: any): Post {
    return {
      id: row.id,
      userId: row.user_id,
      content: row.content,
      images: typeof row.images === 'string' ? JSON.parse(row.images) : (row.images ?? undefined),
      sportRecordId: row.sport_record_id,
      location: row.location,
      tags: typeof row.tags === 'string' ? JSON.parse(row.tags) : (row.tags ?? undefined),
      likeCount: row.like_count,
      commentCount: row.comment_count,
      createdAt: new Date(row.created_at),
    };
  }
}

export class CommentModel {
  private tableName = 'comments';
  constructor(private db: Knex) {}

  async findById(id: string): Promise<Comment | null> {
    const row = await this.db(this.tableName).where({ id }).first();
    return row ? this.mapToModel(row) : null;
  }

  async findByPostId(postId: string, limit: number, offset: number): Promise<Comment[]> {
    const rows = await this.db(this.tableName)
      .where({ post_id: postId })
      .orderBy('created_at', 'asc')
      .limit(limit)
      .offset(offset);
    return rows.map(r => this.mapToModel(r));
  }

  async create(data: Omit<Comment, 'id' | 'likeCount' | 'createdAt'>): Promise<Comment> {
    const [created] = await this.db(this.tableName)
      .insert({ user_id: data.userId, post_id: data.postId, parent_id: data.parentId, content: data.content })
      .returning('*');
    return this.mapToModel(created);
  }

  async delete(id: string): Promise<void> {
    await this.db(this.tableName).where({ id }).del();
  }

  private mapToModel(row: any): Comment {
    return {
      id: row.id,
      userId: row.user_id,
      postId: row.post_id,
      parentId: row.parent_id,
      content: row.content,
      likeCount: row.like_count,
      createdAt: new Date(row.created_at),
    };
  }
}

export class LikeModel {
  private tableName = 'likes';
  constructor(private db: Knex) {}

  async find(userId: string, targetType: string, targetId: string) {
    return this.db(this.tableName).where({ user_id: userId, target_type: targetType, target_id: targetId }).first();
  }

  async create(userId: string, targetType: string, targetId: string): Promise<void> {
    await this.db(this.tableName).insert({ user_id: userId, target_type: targetType, target_id: targetId });
  }

  async delete(userId: string, targetType: string, targetId: string): Promise<void> {
    await this.db(this.tableName).where({ user_id: userId, target_type: targetType, target_id: targetId }).del();
  }

  async findByUserAndTargets(userId: string, targetType: string, targetIds: string[]): Promise<string[]> {
    const rows = await this.db(this.tableName)
      .where({ user_id: userId, target_type: targetType })
      .whereIn('target_id', targetIds)
      .select('target_id');
    return rows.map((r: any) => r.target_id);
  }
}
