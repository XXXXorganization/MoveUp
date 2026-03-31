"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.db = void 0;
// src/config/database.ts
const knex_1 = __importDefault(require("knex"));
const knexfile_1 = __importDefault(require("../../knexfile"));
const environment = process.env.NODE_ENV || 'development';
const config = knexfile_1.default[environment];
exports.db = (0, knex_1.default)(config);
