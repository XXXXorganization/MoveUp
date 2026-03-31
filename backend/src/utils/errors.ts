// src/utils/errors.ts

export class AppError extends Error {
  public code: number;
  public message: string;

  constructor(message: string, code: number = 500) {
    super(message);
    this.code = code;
    this.message = message;
    this.name = 'AppError';
  }
}