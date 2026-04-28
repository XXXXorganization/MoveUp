exports.up = function(knex) {
  return knex.schema
    .createTable('user_tasks', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('task_id').notNullable().references('id').inTable('tasks').onDelete('CASCADE');
      table.integer('progress').defaultTo(0);
      table.boolean('is_completed').defaultTo(false);
      table.timestamp('completed_at');
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.unique(['user_id', 'task_id']);
      table.index('user_id');
    })
    .createTable('user_achievements', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('badge_id').notNullable().references('id').inTable('badges').onDelete('CASCADE');
      table.timestamp('achieved_at').defaultTo(knex.fn.now());
      table.unique(['user_id', 'badge_id']);
      table.index('user_id');
    })
    .createTable('user_challenges', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('challenge_id').notNullable().references('id').inTable('challenges').onDelete('CASCADE');
      table.integer('progress').defaultTo(0);
      table.integer('rank');
      table.boolean('is_completed').defaultTo(false);
      table.timestamp('joined_at').defaultTo(knex.fn.now());
      table.timestamp('completed_at');
      table.unique(['user_id', 'challenge_id']);
      table.index('challenge_id');
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('user_challenges')
    .dropTableIfExists('user_achievements')
    .dropTableIfExists('user_tasks');
};