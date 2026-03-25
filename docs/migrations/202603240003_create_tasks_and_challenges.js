exports.up = function(knex) {
  return knex.schema
    .createTable('tasks', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description');
      table.string('task_type', 30);
      table.jsonb('requirement');
      table.integer('reward_points').defaultTo(0);
      table.uuid('reward_badge_id').references('id').inTable('badges');
      table.boolean('is_active').defaultTo(true);
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    .createTable('challenges', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description');
      table.string('challenge_type', 30);
      table.timestamp('start_time').notNullable();
      table.timestamp('end_time').notNullable();
      table.integer('target_value');
      table.integer('reward_points');
      table.uuid('reward_badge_id').references('id').inTable('badges');
      table.timestamp('created_at').defaultTo(knex.fn.now());
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('challenges')
    .dropTableIfExists('tasks');
};