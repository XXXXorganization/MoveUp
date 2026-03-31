"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.errorHandler = void 0;
const errors_1 = require("../utils/errors");
const errorHandler = (error, req, res, next) => {
    if (error instanceof errors_1.AppError) {
        res.status(error.code).json({
            code: error.code,
            message: error.message,
            data: null,
        });
    }
    else {
        console.error(error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            data: null,
        });
    }
};
exports.errorHandler = errorHandler;
