exports.up = function(knex) {
  return knex.schema
    // 用户设备表
    .createTable('user_devices', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.string('device_type', 20);
      table.string('device_token', 255);
      table.timestamp('login_time').defaultTo(knex.fn.now());
      table.timestamp('logout_time');
      table.boolean('is_active').defaultTo(true);
      table.index('user_id');
    })
    // 路线表
    .createTable('routes', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.jsonb('start_point');
      table.jsonb('end_point');
      table.integer('distance');
      table.integer('elevation_gain');
      table.integer('popularity').defaultTo(0);
      table.boolean('is_hot').defaultTo(false);
      table.uuid('created_by').references('id').inTable('users');
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.index(['popularity', 'is_hot']);
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('routes')
    .dropTableIfExists('user_devices');
};