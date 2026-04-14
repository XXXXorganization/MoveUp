// knexfile.js
module.exports = {
  development: {
    client: 'pg',
    connection: {
      host: 'postgres',  // 使用Docker服务名
      port: 5432,
      user: 'postgres',
      password: 'postgres',  // 使用环境变量
      database: 'moveup_db'
    },
    migrations: {
      directory: './migrations',
      extension: 'js',
      stub: './migration.stub'
    },
    seeds: {
      directory: './seeds'
    }
  },
  test: {
    client: 'pg',
    connection: {
      host: 'postgres',  // 使用Docker服务名
      port: 5432,
      user: 'postgres',
      password: 'postgres',  // 使用环境变量
      database: 'moveup_db'
    },
    migrations: {
      directory: './migrations',
      extension: 'js',
      stub: './migration.stub'
    },
    seeds: {
      directory: './seeds'
    }
  }
};