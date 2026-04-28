"use strict";
// src/utils/errors.ts
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppError = void 0;
class AppError extends Error {
    constructor(message, code = 500) {
        super(message);
        this.code = code;
        this.message = message;
        this.name = 'AppError';
    }
}
exports.AppError = AppError;
