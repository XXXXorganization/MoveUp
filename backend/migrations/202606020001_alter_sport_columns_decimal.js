exports.up = function(knex) {
  return knex.schema.alterTable('sport_records', table => {
    table.decimal('average_pace', 10, 2).alter();
    table.decimal('duration', 10, 2).alter();
    table.decimal('max_heart_rate', 6, 2).alter();
    table.decimal('average_heart_rate', 6, 2).alter();
  });
};

exports.down = function(knex) {
  return knex.schema.alterTable('sport_records', table => {
    table.integer('average_pace').alter();
    table.integer('duration').alter();
    table.integer('max_heart_rate').alter();
    table.integer('average_heart_rate').alter();
  });
};
