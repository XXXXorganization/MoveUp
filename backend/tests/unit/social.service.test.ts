// tests/unit/social.service.test.ts
import { SocialService } from '../../src/modules/social/service';
import { SocialRepository } from '../../src/modules/social/repository';

const mockRepo: jest.Mocked<SocialRepository> = {
  searchUsers: jest.fn(),
  getUserById: jest.fn(),
  findFriendship: jest.fn(),
  createFriendship: jest.fn(),
  updateFriendshipStatus: jest.fn(),
  getAcceptedFriendships: jest.fn(),
  getUsersByIds: jest.fn(),
  getPendingRequests: jest.fn(),
  deleteFriendship: jest.fn(),
  getFriendIds: jest.fn(),
  getPostsByUserIds: jest.fn(),
  createPost: jest.fn(),
  getAllPosts: jest.fn(),
  getPostById: jest.fn(),
  deletePost: jest.fn(),
  findLike: jest.fn(),
  createLike: jest.fn(),
  deleteLike: jest.fn(),
  incrementPostLike: jest.fn(),
  createComment: jest.fn(),
  incrementPostComment: jest.fn(),
  getCommentsByPostId: jest.fn(),
  getCommentById: jest.fn(),
  deleteComment: jest.fn(),
  getLikedPostIds: jest.fn(),
  getLeaderboard: jest.fn(),
} as any as jest.Mocked<SocialRepository>;

const svc = () => new SocialService(mockRepo);

beforeEach(() => jest.clearAllMocks());

// ==================== searchUsers ====================

describe('searchUsers', () => {
  it('关键词少于2字符时应抛出 400', async () => {
    await expect(svc().searchUsers('a', 'u1')).rejects.toMatchObject({ code: 400 });
    await expect(svc().searchUsers('', 'u1')).rejects.toMatchObject({ code: 400 });
  });

  it('有效关键词时应调用 repository', async () => {
    mockRepo.searchUsers.mockResolvedValue([]);
    await svc().searchUsers('测试用户', 'u1');
    expect(mockRepo.searchUsers).toHaveBeenCalledWith('测试用户', 'u1');
  });
});

// ==================== sendFriendRequest ====================

describe('sendFriendRequest', () => {
  it('不能添加自己为好友', async () => {
    await expect(svc().sendFriendRequest('u1', 'u1')).rejects.toMatchObject({ code: 400 });
  });

  it('目标用户不存在时应抛出 404', async () => {
    mockRepo.getUserById.mockResolvedValue(null);
    await expect(svc().sendFriendRequest('u1', 'u2')).rejects.toMatchObject({ code: 404 });
  });

  it('已是好友时应抛出 409', async () => {
    mockRepo.getUserById.mockResolvedValue({ id: 'u2' } as any);
    mockRepo.findFriendship.mockResolvedValueOnce({ status: 1 } as any);
    await expect(svc().sendFriendRequest('u1', 'u2')).rejects.toMatchObject({ code: 409 });
  });

  it('好友请求已发送时应抛出 409', async () => {
    mockRepo.getUserById.mockResolvedValue({ id: 'u2' } as any);
    mockRepo.findFriendship.mockResolvedValueOnce({ status: 0 } as any);
    await expect(svc().sendFriendRequest('u1', 'u2')).rejects.toMatchObject({ code: 409 });
  });

  it('无好友关系时应创建好友请求', async () => {
    mockRepo.getUserById.mockResolvedValue({ id: 'u2' } as any);
    mockRepo.findFriendship.mockResolvedValue(null);
    mockRepo.createFriendship.mockResolvedValue(null as any);
    await svc().sendFriendRequest('u1', 'u2');
    expect(mockRepo.createFriendship).toHaveBeenCalledWith('u1', 'u2');
  });
});

// ==================== respondFriendRequest ====================

describe('respondFriendRequest', () => {
  it('好友请求不存在时应抛出 404', async () => {
    mockRepo.findFriendship.mockResolvedValue(null);
    await expect(svc().respondFriendRequest('u1', 'u2', 'accept')).rejects.toMatchObject({ code: 404 });
  });

  it('请求状态不是 pending 时应抛出 404', async () => {
    mockRepo.findFriendship.mockResolvedValue({ status: 1 } as any);
    await expect(svc().respondFriendRequest('u1', 'u2', 'accept')).rejects.toMatchObject({ code: 404 });
  });

  it('接受请求时应更新状态为 1', async () => {
    mockRepo.findFriendship.mockResolvedValue({ status: 0 } as any);
    mockRepo.updateFriendshipStatus.mockResolvedValue(null as any);
    await svc().respondFriendRequest('u1', 'u2', 'accept');
    expect(mockRepo.updateFriendshipStatus).toHaveBeenCalledWith('u2', 'u1', 1);
  });

  it('拒绝请求时应更新状态为 2', async () => {
    mockRepo.findFriendship.mockResolvedValue({ status: 0 } as any);
    mockRepo.updateFriendshipStatus.mockResolvedValue(null as any);
    await svc().respondFriendRequest('u1', 'u2', 'reject');
    expect(mockRepo.updateFriendshipStatus).toHaveBeenCalledWith('u2', 'u1', 2);
  });
});

// ==================== getFriends ====================

describe('getFriends', () => {
  it('无好友时应返回空数组', async () => {
    mockRepo.getAcceptedFriendships.mockResolvedValue([]);
    const result = await svc().getFriends('u1');
    expect(result).toEqual([]);
  });

  it('应正确映射好友信息', async () => {
    mockRepo.getAcceptedFriendships.mockResolvedValue([
      { userId: 'u1', friendId: 'u2', status: 1, createdAt: new Date() } as any,
    ]);
    mockRepo.getUsersByIds.mockResolvedValue([
      { id: 'u2', nickname: '好友A', avatar: null } as any,
    ]);
    const friends = await svc().getFriends('u1');
    expect(friends[0].userId).toBe('u2');
    expect(friends[0].nickname).toBe('好友A');
  });
});

// ==================== deleteFriend ====================

describe('deleteFriend', () => {
  it('好友关系不存在时应抛出 404', async () => {
    mockRepo.findFriendship.mockResolvedValue(null);
    await expect(svc().deleteFriend('u1', 'u2')).rejects.toMatchObject({ code: 404 });
  });

  it('好友关系状态不是已接受时应抛出 404', async () => {
    mockRepo.findFriendship.mockResolvedValueOnce({ status: 0 } as any);
    mockRepo.findFriendship.mockResolvedValueOnce(null);
    await expect(svc().deleteFriend('u1', 'u2')).rejects.toMatchObject({ code: 404 });
  });

  it('好友关系存在时应删除', async () => {
    mockRepo.findFriendship.mockResolvedValueOnce({ status: 1 } as any);
    mockRepo.deleteFriendship.mockResolvedValue(null as any);
    await svc().deleteFriend('u1', 'u2');
    expect(mockRepo.deleteFriendship).toHaveBeenCalledWith('u1', 'u2');
  });
});

// ==================== createPost ====================

describe('createPost', () => {
  it('内容和图片均为空时应抛出 400', async () => {
    await expect(svc().createPost('u1', { content: '', images: [] })).rejects.toMatchObject({ code: 400 });
  });

  it('有内容时应创建动态', async () => {
    const post = { id: 'p1', userId: 'u1', content: '今天跑了5公里', likeCount: 0, commentCount: 0 };
    mockRepo.createPost.mockResolvedValue(post as any);
    mockRepo.getUsersByIds.mockResolvedValue([{ id: 'u1', nickname: '测试', avatar: null } as any]);
    mockRepo.getLikedPostIds.mockResolvedValue([]);

    const result = await svc().createPost('u1', { content: '今天跑了5公里' });
    expect(result.content).toBe('今天跑了5公里');
  });
});

// ==================== deletePost ====================

describe('deletePost', () => {
  it('动态不存在时应抛出 404', async () => {
    mockRepo.getPostById.mockResolvedValue(null);
    await expect(svc().deletePost('u1', 'p1')).rejects.toMatchObject({ code: 404 });
  });

  it('非作者删除时应抛出 403', async () => {
    mockRepo.getPostById.mockResolvedValue({ id: 'p1', userId: 'u2' } as any);
    await expect(svc().deletePost('u1', 'p1')).rejects.toMatchObject({ code: 403 });
  });

  it('作者可以删除自己的动态', async () => {
    mockRepo.getPostById.mockResolvedValue({ id: 'p1', userId: 'u1' } as any);
    mockRepo.deletePost.mockResolvedValue(null as any);
    await svc().deletePost('u1', 'p1');
    expect(mockRepo.deletePost).toHaveBeenCalledWith('p1');
  });
});

// ==================== toggleLike ====================

describe('toggleLike', () => {
  it('动态不存在时应抛出 404', async () => {
    mockRepo.getPostById.mockResolvedValue(null);
    await expect(svc().toggleLike('u1', 'p1')).rejects.toMatchObject({ code: 404 });
  });

  it('未点赞时应点赞并返回 liked=true', async () => {
    mockRepo.getPostById.mockResolvedValue({ id: 'p1', likeCount: 5 } as any);
    mockRepo.findLike.mockResolvedValue(null);
    mockRepo.createLike.mockResolvedValue(null as any);
    mockRepo.incrementPostLike.mockResolvedValue(null as any);

    const result = await svc().toggleLike('u1', 'p1');
    expect(result.liked).toBe(true);
    expect(result.likeCount).toBe(6);
  });

  it('已点赞时应取消点赞并返回 liked=false', async () => {
    mockRepo.getPostById.mockResolvedValue({ id: 'p1', likeCount: 5 } as any);
    mockRepo.findLike.mockResolvedValue({ id: 'l1' } as any);
    mockRepo.deleteLike.mockResolvedValue(null as any);
    mockRepo.incrementPostLike.mockResolvedValue(null as any);

    const result = await svc().toggleLike('u1', 'p1');
    expect(result.liked).toBe(false);
    expect(result.likeCount).toBe(4);
  });
});

// ==================== addComment ====================

describe('addComment', () => {
  it('动态不存在时应抛出 404', async () => {
    mockRepo.getPostById.mockResolvedValue(null);
    await expect(svc().addComment('u1', 'p1', '好棒！')).rejects.toMatchObject({ code: 404 });
  });

  it('应创建评论并更新评论计数', async () => {
    const comment = { id: 'c1', userId: 'u1', postId: 'p1', content: '好棒！' };
    mockRepo.getPostById.mockResolvedValue({ id: 'p1' } as any);
    mockRepo.createComment.mockResolvedValue(comment as any);
    mockRepo.incrementPostComment.mockResolvedValue(null as any);
    mockRepo.getUserById.mockResolvedValue({ id: 'u1', nickname: '测试', avatar: null } as any);

    const result = await svc().addComment('u1', 'p1', '好棒！');
    expect(result.content).toBe('好棒！');
    expect(mockRepo.incrementPostComment).toHaveBeenCalledWith('p1', 1);
  });
});

// ==================== deleteComment ====================

describe('deleteComment', () => {
  it('评论不存在时应抛出 404', async () => {
    mockRepo.getCommentById.mockResolvedValue(null);
    await expect(svc().deleteComment('u1', 'p1', 'c1')).rejects.toMatchObject({ code: 404 });
  });

  it('评论属于其他帖子时应抛出 404', async () => {
    mockRepo.getCommentById.mockResolvedValue({ id: 'c1', postId: 'p2', userId: 'u1' } as any);
    await expect(svc().deleteComment('u1', 'p1', 'c1')).rejects.toMatchObject({ code: 404 });
  });

  it('非评论作者时应抛出 403', async () => {
    mockRepo.getCommentById.mockResolvedValue({ id: 'c1', postId: 'p1', userId: 'u2' } as any);
    await expect(svc().deleteComment('u1', 'p1', 'c1')).rejects.toMatchObject({ code: 403 });
  });

  it('作者可以删除评论并更新计数', async () => {
    mockRepo.getCommentById.mockResolvedValue({ id: 'c1', postId: 'p1', userId: 'u1' } as any);
    mockRepo.deleteComment.mockResolvedValue(null as any);
    mockRepo.incrementPostComment.mockResolvedValue(null as any);
    await svc().deleteComment('u1', 'p1', 'c1');
    expect(mockRepo.incrementPostComment).toHaveBeenCalledWith('p1', -1);
  });
});

// ==================== getLeaderboard ====================

describe('getLeaderboard', () => {
  it('friends 范围时应包含当前用户和好友', async () => {
    mockRepo.getFriendIds.mockResolvedValue(['u2', 'u3']);
    mockRepo.getLeaderboard.mockResolvedValue([]);
    await svc().getLeaderboard('u1', 'weekly', 'friends');
    expect(mockRepo.getLeaderboard).toHaveBeenCalledWith(
      ['u1', 'u2', 'u3'],
      expect.any(Date),
      expect.any(Date),
    );
  });

  it('应正确计算排名顺序', async () => {
    mockRepo.getFriendIds.mockResolvedValue([]);
    mockRepo.getLeaderboard.mockResolvedValue([
      { user_id: 'u1', total_distance: '10000', run_count: '5' },
      { user_id: 'u2', total_distance: '8000', run_count: '4' },
    ] as any);
    mockRepo.getUsersByIds.mockResolvedValue([
      { id: 'u1', nickname: '选手A' } as any,
      { id: 'u2', nickname: '选手B' } as any,
    ]);

    const board = await svc().getLeaderboard('u1', 'weekly', 'friends');
    expect(board[0].rank).toBe(1);
    expect(board[1].rank).toBe(2);
    expect(board[0].totalDistance).toBe(10000);
  });
});
