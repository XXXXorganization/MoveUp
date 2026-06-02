exports.up = function(knex) {
  return knex.schema.alterTable('sport_records', table => {
    table.decimal('distance', 12, 2).notNullable().defaultTo(0).alter();
    table.decimal('calories', 8, 2).defaultTo(0).alter();
  });
};

exports.down = function(knex) {
  return knex.schema.alterTable('sport_records', table => {
    table.integer('distance').notNullable().defaultTo(0).alter();
    table.integer('calories').defaultTo(0).alter();
  });
};
