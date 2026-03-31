// src/server.ts
import app from './app';
import { db } from './config/database';

const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    // 测试数据库连接
    await db.raw('SELECT 1');
    console.log('Database connected successfully');

    app.listen(PORT, () => {
      console.log(`Server is running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();