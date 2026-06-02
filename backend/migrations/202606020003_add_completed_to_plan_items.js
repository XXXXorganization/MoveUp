exports.up = function(knex) {
  return knex.schema.alterTable('user_plan_items', table => {
    table.boolean('is_completed').defaultTo(false);
  });
};

exports.down = function(knex) {
  return knex.schema.alterTable('user_plan_items', table => {
    table.dropColumn('is_completed');
  });
};
