// src/types/express.d.ts
declare namespace Express {
  interface Request {
    user?: {
      userId: string;
      // 如果你在 auth 中间件中还挂了其他字段，也一并声明，比如：
      // role?: string;
      // email?: string;
    };
  }
}