import { ClubModel } from './model';
import { Club, ClubMember, ClubPost, ClubComment } from './types';

export class ClubRepository {
  constructor(private model: ClubModel) {}

  // Clubs
  findAllClubs() { return this.model.findAll(); }
  findClubById(id: string) { return this.model.findById(id); }
  createClub(data: Omit<Club, 'id' | 'created_at' | 'updated_at'>) { return this.model.create(data); }
  deleteClub(id: string) { return this.model.deleteById(id); }

  // Members
  findMember(clubId: string, userId: string) { return this.model.findMember(clubId, userId); }
  addMember(clubId: string, userId: string) { return this.model.addMember(clubId, userId); }
  removeMember(clubId: string, userId: string) { return this.model.removeMember(clubId, userId); }
  getMemberCount(clubId: string) { return this.model.getMemberCount(clubId); }
  getUserClubIds(userId: string) { return this.model.getUserClubIds(userId); }
  getUserClubs(userId: string) { return this.model.getUserClubs(userId); }

  // Posts
  getPostsByClubId(clubId: string) { return this.model.getPostsByClubId(clubId); }
  getPostById(id: string) { return this.model.getPostById(id); }
  createPost(data: Omit<ClubPost, 'id' | 'created_at'>) { return this.model.createPost(data); }

  // Comments
  getCommentsByPostId(postId: string) { return this.model.getCommentsByPostId(postId); }
  createComment(data: Omit<ClubComment, 'id' | 'created_at'>) { return this.model.createComment(data); }
  deleteComment(id: string) { return this.model.deleteComment(id); }

  // Likes
  findLike(postId: string, userId: string) { return this.model.findLike(postId, userId); }
  addLike(postId: string, userId: string) { return this.model.addLike(postId, userId); }
  removeLike(postId: string, userId: string) { return this.model.removeLike(postId, userId); }
  getLikeCount(postId: string) { return this.model.getLikeCount(postId); }

  // Users
  getUserById(id: string) { return this.model.getUserById(id); }

  // Sport Records
  getRunSummary(runId: string) { return this.model.getRunSummary(runId); }
}
