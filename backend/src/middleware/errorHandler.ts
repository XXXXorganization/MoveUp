// src/middleware/errorHandler.ts
import { Request, Response, NextFunction } from 'express';
import { AppError } from '../utils/errors';

export const errorHandler = (error: Error, req: Request, res: Response, next: NextFunction): void => {
  if (error instanceof AppError) {
    res.status(error.code).json({
      code: error.code,
      message: error.message,
      data: null,
    });
  } else {
    console.error(error);
    res.status(500).json({
      code: 500,
      message: '服务器内部错误',
      data: null,
    });
  }
};