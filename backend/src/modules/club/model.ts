import { Club, ClubMember, ClubPost, ClubComment, ClubPostLike } from './types';

export class ClubModel {
  constructor(private db: any) {}

  // === Clubs ===
  async findAll(): Promise<Club[]> {
    return this.db('clubs').select('*').orderBy('created_at', 'desc');
  }

  async findById(id: string): Promise<Club | undefined> {
    return this.db('clubs').where({ id }).first();
  }

  async deleteById(id: string): Promise<void> {
    await this.db('clubs').where({ id }).del();
  }

  async create(data: Omit<Club, 'id' | 'created_at' | 'updated_at'>): Promise<Club> {
    const [row] = await this.db('clubs').insert(data).returning('*');
    return row;
  }

  // === Members ===
  async findMember(clubId: string, userId: string): Promise<ClubMember | undefined> {
    return this.db('club_members').where({ club_id: clubId, user_id: userId }).first();
  }

  async addMember(clubId: string, userId: string): Promise<ClubMember> {
    const [row] = await this.db('club_members').insert({ club_id: clubId, user_id: userId }).returning('*');
    return row;
  }

  async removeMember(clubId: string, userId: string): Promise<void> {
    await this.db('club_members').where({ club_id: clubId, user_id: userId }).del();
  }

  async getMemberCount(clubId: string): Promise<number> {
    const result = await this.db('club_members').where({ club_id: clubId }).count('id as count').first();
    return parseInt(result.count, 10);
  }

  async getUserClubIds(userId: string): Promise<string[]> {
    const rows = await this.db('club_members').where({ user_id: userId }).select('club_id');
    return rows.map((r: any) => r.club_id);
  }

  async getUserClubs(userId: string): Promise<any[]> {
    return this.db('club_members')
      .join('clubs', 'club_members.club_id', 'clubs.id')
      .where('club_members.user_id', userId)
      .select('clubs.id', 'clubs.name', 'clubs.location', 'clubs.image_url', 'clubs.flag');
  }

  // === Posts ===
  async getPostsByClubId(clubId: string): Promise<ClubPost[]> {
    const rows = await this.db('club_posts').where({ club_id: clubId }).select('*').orderBy('created_at', 'desc');
    return rows.map((row: any) => ({
      ...row,
      images: typeof row.images === 'string' ? JSON.parse(row.images) : (row.images || undefined),
    }));
  }

  async getPostById(id: string): Promise<ClubPost | undefined> {
    const row = await this.db('club_posts').where({ id }).first();
    if (row && typeof row.images === 'string') {
      row.images = JSON.parse(row.images);
    }
    return row;
  }

  async createPost(data: Omit<ClubPost, 'id' | 'created_at'>): Promise<ClubPost> {
    const insertData: any = { ...data };
    if (insertData.images) {
      insertData.images = JSON.stringify(insertData.images);
    }
    const [row] = await this.db('club_posts').insert(insertData).returning('*');
    return row;
  }

  // === Comments ===
  async getCommentsByPostId(postId: string): Promise<ClubComment[]> {
    return this.db('club_comments').where({ post_id: postId }).select('*').orderBy('created_at', 'asc');
  }

  async createComment(data: Omit<ClubComment, 'id' | 'created_at'>): Promise<ClubComment> {
    const [row] = await this.db('club_comments').insert(data).returning('*');
    return row;
  }

  async deleteComment(id: string): Promise<void> {
    await this.db('club_comments').where({ id }).del();
  }

  // === Likes ===
  async findLike(postId: string, userId: string): Promise<ClubPostLike | undefined> {
    return this.db('club_post_likes').where({ post_id: postId, user_id: userId }).first();
  }

  async addLike(postId: string, userId: string): Promise<void> {
    await this.db('club_post_likes').insert({ post_id: postId, user_id: userId });
  }

  async removeLike(postId: string, userId: string): Promise<void> {
    await this.db('club_post_likes').where({ post_id: postId, user_id: userId }).del();
  }

  async getLikeCount(postId: string): Promise<number> {
    const result = await this.db('club_post_likes').where({ post_id: postId }).count('id as count').first();
    return parseInt(result.count, 10);
  }

  // === Users ===
  async getUserById(id: string): Promise<any> {
    return this.db('users').where({ id }).select('id', 'nickname', 'avatar').first();
  }

  // === Sport Records ===
  async getRunSummary(runId: string): Promise<any> {
    return this.db('sport_records')
      .where({ id: runId })
      .select('distance', 'duration', 'average_pace')
      .first();
  }
}
