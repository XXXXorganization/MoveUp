import { Router } from 'express';
import { ClubController } from '../modules/club/controller';
import { authenticateToken } from '../middleware/auth';

export function createClubRoutes(controller: ClubController): Router {
  const router = Router();

  router.get('/clubs', authenticateToken, controller.getClubs.bind(controller));
  router.get('/user/clubs', authenticateToken, controller.getMyClubs.bind(controller));
  router.get('/clubs/:id', authenticateToken, controller.getClubById.bind(controller));
  router.post('/clubs/:id/toggle', authenticateToken, controller.toggleMembership.bind(controller));
  router.get('/clubs/:id/posts', authenticateToken, controller.getPosts.bind(controller));
  router.post('/clubs/:id/posts', authenticateToken, controller.createPost.bind(controller));
  router.post('/posts/:id/like', authenticateToken, controller.toggleLike.bind(controller));
  router.post('/posts/:id/comment', authenticateToken, controller.addComment.bind(controller));
  router.get('/posts/:id/comments', authenticateToken, controller.getComments.bind(controller));

  return router;
}
