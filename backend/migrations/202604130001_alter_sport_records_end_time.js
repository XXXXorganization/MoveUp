exports.up = function(knex) {
  return knex.schema
    .alterTable('sport_records', (table) => {
      table.timestamp('end_time').alter().nullable().defaultTo(null);
    });
};

exports.down = function(knex) {
  return knex.schema
    .alterTable('sport_records', (table) => {
      table.timestamp('end_time').alter().notNullable();
    });
};
