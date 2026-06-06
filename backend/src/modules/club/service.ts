import { ClubRepository } from './repository';
import { ClubWithMembership, PostWithAuthor, CommentWithAuthor } from './types';
import { AppError } from '../../utils/errors';

export class ClubService {
  constructor(private repository: ClubRepository) {}

  async createClub(data: { name: string; description?: string; location?: string; image_url?: string; flag?: string }) {
    return this.repository.createClub({
      name: data.name,
      description: data.description,
      location: data.location,
      image_url: data.image_url,
      flag: data.flag || 'CN',
    });
  }

  async deleteClub(clubId: string) {
    const club = await this.repository.findClubById(clubId);
    if (!club) throw new AppError('社团不存在', 404);
    await this.repository.deleteClub(clubId);
  }

  async getClubs(userId: string): Promise<ClubWithMembership[]> {
    const clubs = await this.repository.findAllClubs();
    const userClubIds = await this.repository.getUserClubIds(userId);

    const result: ClubWithMembership[] = [];
    for (const club of clubs) {
      result.push({
        id: club.id,
        name: club.name,
        location: club.location,
        flag: club.flag || 'CN',
        image_url: club.image_url,
        description: club.description,
        is_member: userClubIds.includes(club.id),
        member_count: await this.repository.getMemberCount(club.id),
      });
    }
    return result;
  }

  async getClubDetail(clubId: string, userId: string): Promise<ClubWithMembership> {
    const club = await this.repository.findClubById(clubId);
    if (!club) throw new AppError('社团不存在', 404);
    const isMember = !!(await this.repository.findMember(clubId, userId));
    return {
      id: club.id,
      name: club.name,
      location: club.location,
      flag: club.flag || 'CN',
      image_url: club.image_url,
      description: club.description,
      is_member: isMember,
      member_count: await this.repository.getMemberCount(clubId),
    };
  }

  async toggleMembership(clubId: string, userId: string): Promise<{ joined: boolean }> {
    const club = await this.repository.findClubById(clubId);
    if (!club) throw new AppError('社团不存在', 404);

    const existing = await this.repository.findMember(clubId, userId);
    if (existing) {
      await this.repository.removeMember(clubId, userId);
      return { joined: false };
    } else {
      await this.repository.addMember(clubId, userId);
      return { joined: true };
    }
  }

  async getMyClubs(userId: string) {
    return this.repository.getUserClubs(userId);
  }

  async getPosts(clubId: string, userId: string): Promise<PostWithAuthor[]> {
    const posts = await this.repository.getPostsByClubId(clubId);
    const result: PostWithAuthor[] = [];
    for (const post of posts) {
      const author = await this.repository.getUserById(post.user_id);
      const comments = await this.repository.getCommentsByPostId(post.id);
      const like = await this.repository.findLike(post.id, userId);
      const likeCount = await this.repository.getLikeCount(post.id);

      const commentsWithAuthor: CommentWithAuthor[] = [];
      for (const c of comments) {
        const commentAuthor = await this.repository.getUserById(c.user_id);
        commentsWithAuthor.push({
          id: c.id,
          content: c.content,
          created_at: c.created_at,
          reply_to_id: c.reply_to_id,
          author: { id: commentAuthor?.id, nickname: commentAuthor?.nickname || '未知', avatar: commentAuthor?.avatar },
        });
      }

      // 如果有跑步记录关联，获取摘要
      let runSummary = undefined;
      if (post.run_id) {
        const run = await this.repository.getRunSummary(post.run_id);
        if (run) {
          const pace = run.average_pace
            ? Math.floor(run.average_pace / 60) + "'" + String(Math.floor(run.average_pace % 60)).padStart(2, '0') + '"'
            : "0'00\"";
          runSummary = {
            distance: Math.round(run.distance) / 1000, // m → km
            duration: run.duration,
            pace,
          };
        }
      }

      result.push({
        id: post.id,
        club_id: post.club_id,
        content: post.content,
        run_id: post.run_id,
        images: post.images,
        run_summary: runSummary,
        created_at: post.created_at,
        author: { id: author?.id, nickname: author?.nickname || '未知', avatar: author?.avatar },
        is_liked: !!like,
        like_count: likeCount,
        comments: commentsWithAuthor,
      });
    }
    return result;
  }

  async createPost(clubId: string, userId: string, content: string, runId?: string, images?: string[]): Promise<PostWithAuthor> {
    const post = await this.repository.createPost({ club_id: clubId, user_id: userId, content, run_id: runId, images });
    const author = await this.repository.getUserById(userId);
    return {
      id: post.id,
      club_id: post.club_id,
      content: post.content,
      run_id: post.run_id,
      images: post.images,
      created_at: post.created_at,
      author: { id: author?.id, nickname: author?.nickname || '未知', avatar: author?.avatar },
      is_liked: false,
      like_count: 0,
      comments: [],
    };
  }

  async toggleLike(postId: string, userId: string): Promise<{ is_liked: boolean; like_count: number }> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('帖子不存在', 404);

    const existing = await this.repository.findLike(postId, userId);
    if (existing) {
      await this.repository.removeLike(postId, userId);
    } else {
      await this.repository.addLike(postId, userId);
    }
    return {
      is_liked: !existing,
      like_count: await this.repository.getLikeCount(postId),
    };
  }

  async addComment(postId: string, userId: string, content: string, replyToId?: string): Promise<CommentWithAuthor> {
    const post = await this.repository.getPostById(postId);
    if (!post) throw new AppError('帖子不存在', 404);

    const comment = await this.repository.createComment({ post_id: postId, user_id: userId, content, reply_to_id: replyToId });
    const author = await this.repository.getUserById(userId);
    return {
      id: comment.id,
      content: comment.content,
      created_at: comment.created_at,
      reply_to_id: comment.reply_to_id,
      author: { id: author?.id, nickname: author?.nickname || '未知', avatar: author?.avatar },
    };
  }

  async getComments(postId: string, userId: string): Promise<CommentWithAuthor[]> {
    const comments = await this.repository.getCommentsByPostId(postId);
    const result: CommentWithAuthor[] = [];
    for (const c of comments) {
      const author = await this.repository.getUserById(c.user_id);
      result.push({
        id: c.id,
        content: c.content,
        created_at: c.created_at,
        reply_to_id: c.reply_to_id,
        author: { id: author?.id, nickname: author?.nickname || '未知', avatar: author?.avatar },
      });
    }
    return result;
  }
}
