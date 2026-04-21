// src/routes/social.ts
import { Router } from 'express';
import { SocialController } from '../modules/social/controller';
import { authenticateToken } from '../middleware/auth';

export function createSocialRoutes(controller: SocialController): Router {
  const router = Router();

  // 用户搜索
  router.get('/social/users/search', authenticateToken, controller.searchUsers.bind(controller));

  // 好友系统
  router.get('/social/friends', authenticateToken, controller.getFriends.bind(controller));
  router.get('/social/friends/requests', authenticateToken, controller.getPendingRequests.bind(controller));
  router.post('/social/friends/request', authenticateToken, controller.sendFriendRequest.bind(controller));
  router.put('/social/friends/respond', authenticateToken, controller.respondFriendRequest.bind(controller));
  router.delete('/social/friends/:friendId', authenticateToken, controller.deleteFriend.bind(controller));
  router.get('/social/friends/:friendId/activities', authenticateToken, controller.getFriendActivities.bind(controller));

  // 社区动态
  router.get('/social/posts', authenticateToken, controller.getPosts.bind(controller));
  router.post('/social/posts', authenticateToken, controller.createPost.bind(controller));
  router.get('/social/posts/:postId', authenticateToken, controller.getPostById.bind(controller));
  router.delete('/social/posts/:postId', authenticateToken, controller.deletePost.bind(controller));
  router.post('/social/posts/:postId/like', authenticateToken, controller.toggleLike.bind(controller));
  router.post('/social/posts/:postId/comments', authenticateToken, controller.addComment.bind(controller));
  router.get('/social/posts/:postId/comments', authenticateToken, controller.getComments.bind(controller));
  router.delete('/social/posts/:postId/comments/:commentId', authenticateToken, controller.deleteComment.bind(controller));

  // 排行榜
  router.get('/social/leaderboard', authenticateToken, controller.getLeaderboard.bind(controller));

  return router;
}
