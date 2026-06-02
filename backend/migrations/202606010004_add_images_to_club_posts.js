exports.up = function(knex) {
  return knex.schema.alterTable('club_posts', table => {
    table.jsonb('images').nullable();
  });
};

exports.down = function(knex) {
  return knex.schema.alterTable('club_posts', table => {
    table.dropColumn('images');
  });
};
