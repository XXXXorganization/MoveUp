exports.up = function(knex) {
  return knex.schema.alterTable('users', table => {
    table.string('password_hash', 255).nullable();
  });
};

exports.down = function(knex) {
  return knex.schema.alterTable('users', table => {
    table.dropColumn('password_hash');
  });
};
