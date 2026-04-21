exports.up = function(knex) {
  return knex.schema
    .createTable('training_plans', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description');
      table.string('difficulty', 20); // beginner / intermediate / advanced
      table.integer('duration_weeks');
      table.integer('target_distance'); // 米
      table.jsonb('schedule'); // [{week, days:[{day, type, distance, duration, pace, description}]}]
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    .createTable('user_plans', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('plan_id').notNullable().references('id').inTable('training_plans').onDelete('CASCADE');
      table.integer('current_week').defaultTo(1);
      table.integer('current_day').defaultTo(1);
      table.boolean('is_active').defaultTo(true);
      table.timestamp('started_at').defaultTo(knex.fn.now());
      table.timestamp('completed_at');
      table.index('user_id');
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('user_plans')
    .dropTableIfExists('training_plans');
};
