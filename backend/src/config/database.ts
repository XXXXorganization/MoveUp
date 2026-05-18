import knex from 'knex';

const config = {
  client: 'pg',
  connection: process.env.DATABASE_URL || {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432'),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
    database: process.env.DB_NAME || 'moveup_db',
  },
  migrations: {
    directory: './migrations',
    extension: 'js',
  },
};

export const db = knex(config);