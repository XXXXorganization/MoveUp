// tests/unit/club.service.test.ts
import { ClubService } from '../../src/modules/club/service';
import { ClubRepository } from '../../src/modules/club/repository';

const mockRepository: jest.Mocked<ClubRepository> = {
  findAllClubs: jest.fn(),
  findClubById: jest.fn(),
  createClub: jest.fn(),
  findMember: jest.fn(),
  addMember: jest.fn(),
  removeMember: jest.fn(),
  getMemberCount: jest.fn(),
  getUserClubIds: jest.fn(),
  getUserClubs: jest.fn(),
  getPostsByClubId: jest.fn(),
  getPostById: jest.fn(),
  createPost: jest.fn(),
  getCommentsByPostId: jest.fn(),
  createComment: jest.fn(),
  deleteComment: jest.fn(),
  findLike: jest.fn(),
  addLike: jest.fn(),
  removeLike: jest.fn(),
  getLikeCount: jest.fn(),
  getUserById: jest.fn(),
} as any as jest.Mocked<ClubRepository>;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('ClubService', () => {
  const svc = new ClubService(mockRepository);
  const userId = 'test-user-id';

  describe('getClubs', () => {
    it('should return clubs with membership info', async () => {
      mockRepository.findAllClubs.mockResolvedValue([
        { id: 'c1', name: 'Club1', location: 'HZ', flag: 'CN', image_url: null, description: null, creator_id: null, created_at: '', updated_at: '' },
      ]);
      mockRepository.getUserClubIds.mockResolvedValue(['c1']);
      mockRepository.getMemberCount.mockResolvedValue(5);

      const result = await svc.getClubs(userId);
      expect(result).toHaveLength(1);
      expect(result[0].is_member).toBe(true);
      expect(result[0].member_count).toBe(5);
    });
  });

  describe('toggleMembership', () => {
    it('should join if not a member', async () => {
      mockRepository.findClubById.mockResolvedValue({ id: 'c1' } as any);
      mockRepository.findMember.mockResolvedValue(undefined);

      const result = await svc.toggleMembership('c1', userId);
      expect(result.joined).toBe(true);
      expect(mockRepository.addMember).toHaveBeenCalledWith('c1', userId);
    });

    it('should leave if already a member', async () => {
      mockRepository.findClubById.mockResolvedValue({ id: 'c1' } as any);
      mockRepository.findMember.mockResolvedValue({ id: 'm1' } as any);

      const result = await svc.toggleMembership('c1', userId);
      expect(result.joined).toBe(false);
      expect(mockRepository.removeMember).toHaveBeenCalledWith('c1', userId);
    });

    it('should throw if club not found', async () => {
      mockRepository.findClubById.mockResolvedValue(undefined);
      await expect(svc.toggleMembership('bad', userId)).rejects.toMatchObject({ code: 404 });
    });
  });

  describe('createPost', () => {
    it('should create post and return with author', async () => {
      mockRepository.createPost.mockResolvedValue({ id: 'p1', club_id: 'c1', user_id: userId, content: 'hello', run_id: null, created_at: '' });
      mockRepository.getUserById.mockResolvedValue({ id: userId, nickname: 'TestUser', avatar: null });

      const result = await svc.createPost('c1', userId, 'hello');
      expect(result.content).toBe('hello');
      expect(result.author.nickname).toBe('TestUser');
      expect(result.is_liked).toBe(false);
    });
  });

  describe('toggleLike', () => {
    it('should add like if not liked', async () => {
      mockRepository.getPostById.mockResolvedValue({ id: 'p1' } as any);
      mockRepository.findLike.mockResolvedValue(undefined);
      mockRepository.getLikeCount.mockResolvedValue(5);

      const result = await svc.toggleLike('p1', userId);
      expect(result.is_liked).toBe(true);
      expect(result.like_count).toBe(5);
    });

    it('should remove like if already liked', async () => {
      mockRepository.getPostById.mockResolvedValue({ id: 'p1' } as any);
      mockRepository.findLike.mockResolvedValue({ id: 'l1' } as any);
      mockRepository.getLikeCount.mockResolvedValue(4);

      const result = await svc.toggleLike('p1', userId);
      expect(result.is_liked).toBe(false);
      expect(result.like_count).toBe(4);
    });
  });

  describe('addComment', () => {
    it('should create comment and return with author', async () => {
      mockRepository.getPostById.mockResolvedValue({ id: 'p1' } as any);
      mockRepository.createComment.mockResolvedValue({ id: 'c1', post_id: 'p1', user_id: userId, content: 'nice', reply_to_id: null, created_at: '' });
      mockRepository.getUserById.mockResolvedValue({ id: userId, nickname: 'Commenter', avatar: null });

      const result = await svc.addComment('p1', userId, 'nice');
      expect(result.content).toBe('nice');
      expect(result.author.nickname).toBe('Commenter');
    });
  });

  describe('getMyClubs', () => {
    it('should return user clubs', async () => {
      mockRepository.getUserClubs.mockResolvedValue([{ id: 'c1', name: 'Club1' }]);
      const result = await svc.getMyClubs(userId);
      expect(result).toHaveLength(1);
    });
  });
});
