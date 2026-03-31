// src/config/database.ts
import knex from 'knex';
const knexConfig = require('../../knexfile');

const environment = process.env.NODE_ENV || 'development';
const config = knexConfig[environment];

export const db = knex(config);