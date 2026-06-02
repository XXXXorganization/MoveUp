exports.up = function(knex) {
  return knex.schema.createTable('user_plan_items', table => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    table.string('day_of_week', 10).notNullable();
    table.string('start_time', 10);
    table.string('end_time', 10);
    table.decimal('distance_km', 6, 2);
    table.integer('sort_order').defaultTo(0);
    table.timestamp('created_at').defaultTo(knex.fn.now());
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('user_plan_items');
};
