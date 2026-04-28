// src/server.ts
import 'dotenv/config';
import app from './app';
import { db } from './config/database';

const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    await db.raw('SELECT 1');
    console.log('Database connected successfully');

    // 自动运行迁移
    await db.migrate.latest();
    console.log('Database migrations completed');

    app.listen(PORT, () => {
      console.log(`Server is running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();