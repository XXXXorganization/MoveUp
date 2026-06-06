import { Request, Response, NextFunction } from 'express';
import { ClubService } from './service';

export class ClubController {
  constructor(private service: ClubService) {}

  async createClub(req: Request, res: Response, next: NextFunction) {
    try {
      const { name, description, location, image_url, flag } = req.body;
      if (!name) { res.status(400).json({ code: 400, message: '社团名称必填', data: null }); return; }
      const club = await this.service.createClub({ name, description, location, image_url, flag });
      res.json({ code: 200, message: '创建成功', data: club });
    } catch (error) { next(error); }
  }

  async getClubs(req: Request, res: Response, next: NextFunction) {
    try {
      const clubs = await this.service.getClubs(req.user!.userId);
      res.json({ code: 200, message: 'success', data: { list: clubs } });
    } catch (error) { next(error); }
  }

  async getClubById(req: Request, res: Response, next: NextFunction) {
    try {
      const club = await this.service.getClubDetail(req.params.id, req.user!.userId);
      res.json({ code: 200, message: 'success', data: club });
    } catch (error) { next(error); }
  }

  async toggleMembership(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await this.service.toggleMembership(req.params.id, req.user!.userId);
      res.json({ code: 200, message: result.joined ? '已加入社团' : '已退出社团', data: result });
    } catch (error) { next(error); }
  }

  async getMyClubs(req: Request, res: Response, next: NextFunction) {
    try {
      const clubs = await this.service.getMyClubs(req.user!.userId);
      res.json({ code: 200, message: 'success', data: { list: clubs } });
    } catch (error) { next(error); }
  }

  async getPosts(req: Request, res: Response, next: NextFunction) {
    try {
      const posts = await this.service.getPosts(req.params.id, req.user!.userId);
      res.json({ code: 200, message: 'success', data: { list: posts } });
    } catch (error) { next(error); }
  }

  async createPost(req: Request, res: Response, next: NextFunction) {
    try {
      const { content, run_id, images } = req.body;
      const post = await this.service.createPost(req.params.id, req.user!.userId, content, run_id, images);
      res.json({ code: 200, message: '发布成功', data: post });
    } catch (error) { next(error); }
  }

  async toggleLike(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await this.service.toggleLike(req.params.id, req.user!.userId);
      res.json({ code: 200, message: 'success', data: result });
    } catch (error) { next(error); }
  }

  async addComment(req: Request, res: Response, next: NextFunction) {
    try {
      const { content, reply_to_id } = req.body;
      if (!content) {
        res.status(400).json({ code: 400, message: '评论内容不能为空', data: null });
        return;
      }
      const comment = await this.service.addComment(req.params.id, req.user!.userId, content, reply_to_id);
      res.json({ code: 200, message: '评论成功', data: comment });
    } catch (error) { next(error); }
  }

  async getComments(req: Request, res: Response, next: NextFunction) {
    try {
      const comments = await this.service.getComments(req.params.id, req.user!.userId);
      res.json({ code: 200, message: 'success', data: { list: comments } });
    } catch (error) { next(error); }
  }
}
