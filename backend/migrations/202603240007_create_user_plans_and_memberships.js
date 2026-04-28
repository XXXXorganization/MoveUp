exports.up = function(knex) {
  return knex.schema
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
    })
    .createTable('user_memberships', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('plan_id').notNullable().references('id').inTable('membership_plans').onDelete('CASCADE');
      table.timestamp('start_date').notNullable();
      table.timestamp('end_date').notNullable();
      table.boolean('is_active').defaultTo(true);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.index(['user_id', 'end_date']);
    })
    .createTable('points_logs', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.integer('points_change').notNullable();
      table.string('source_type', 30);
      table.uuid('source_id');
      table.string('description', 255);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.index(['user_id', 'created_at']);
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('points_logs')
    .dropTableIfExists('user_memberships')
    .dropTableIfExists('user_plans');
};