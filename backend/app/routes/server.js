const express = require('express');
const cors = require('cors');
const { routes } = require('./index.js'); 

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true })); 

// 🎯 增强且安全的日志中间件
app.use((req, res, next) => {
  console.log(`\n========================================`);
  console.log(`➡️ [请求到达] ${req.method} ${req.originalUrl}`);

  // 安全检查：如果有 query 参数，才打印
  if (req.query && Object.keys(req.query).length > 0) {
    console.log(`🔍 [Query 参数]:`, req.query);
  }

  // 安全检查：如果有 body 数据，才打印（完美避开 GET 请求的报错）
  if (req.body && Object.keys(req.body).length > 0) {
    console.log(`📦 [Body 数据]:`, req.body);
  }

  console.log(`========================================`);
  
  // 关键：放行请求，让它继续往下走到具体的接口代码
  next();
});

routes.forEach(route => {
  const method = route.method.toLowerCase();
  app[method](`/v1${route.path}`, route.handler); 
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`🏃 Moveup Mock Backend is running on http://localhost:${PORT}`);
  console.log(`等待请求中...\n`);
});