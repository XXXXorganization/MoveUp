exports.up = function(knex) {
  return knex.schema.createTable('sport_records', table => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    table.timestamp('start_time').notNullable();
    table.timestamp('end_time').notNullable();
    table.integer('distance').notNullable();
    table.integer('duration').notNullable();
    table.integer('calories');
    table.decimal('avg_pace', 5, 2);
    table.decimal('max_pace', 5, 2);
    table.smallint('avg_heart_rate');
    table.smallint('max_heart_rate');
    table.jsonb('gps_track');
    table.uuid('route_id').references('id').inTable('routes');
    table.timestamp('created_at').defaultTo(knex.fn.now());
    table.index(['user_id', 'start_time']);
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('sport_records');
};