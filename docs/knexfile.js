// knexfile.js
module.exports = {
  development: {
    client: 'pg',
    connection: {
      host: '127.0.0.1',
      port: 5432,
      user: 'postgres',     // 替换为你的用户名
      password: 'asdQWE05--', // 替换为你的密码
      database: 'Moveup_db'
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