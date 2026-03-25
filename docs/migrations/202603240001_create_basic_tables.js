exports.up = function(knex) {
  return knex.schema
    // 用户表
    .createTable('users', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('phone', 20).notNullable().unique();
      table.string('nickname', 50).notNullable();
      table.string('avatar', 255);
      table.smallint('gender');
      table.date('birthday');
      table.smallint('height');
      table.decimal('weight', 5, 2);
      table.integer('target_distance');
      table.integer('target_time');
      table.string('role', 20).defaultTo('user');
      table.timestamps(true, true); // created_at, updated_at
    })
    // 徽章表
    .createTable('badges', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 50).notNullable();
      table.text('description');
      table.string('icon', 255);
      table.string('condition_type', 30);
      table.integer('condition_value');
      table.string('rarity', 20);
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    // 训练计划表
    .createTable('training_plans', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description');
      table.string('difficulty', 20);
      table.integer('duration_weeks');
      table.integer('target_distance');
      table.jsonb('schedule');
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    // 会员套餐表
    .createTable('membership_plans', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 50).notNullable();
      table.integer('duration_days');
      table.decimal('price', 10, 2);
      table.jsonb('features');
      table.boolean('is_active').defaultTo(true);
      table.timestamp('created_at').defaultTo(knex.fn.now());
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('membership_plans')
    .dropTableIfExists('training_plans')
    .dropTableIfExists('badges')
    .dropTableIfExists('users');
};