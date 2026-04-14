exports.up = function(knex) {
  return knex.schema
    .alterTable('sport_records', (table) => {
      table.enu('status', ['active', 'completed']).notNullable().defaultTo('active');
      table.renameColumn('avg_pace', 'average_pace');
      table.renameColumn('avg_heart_rate', 'average_heart_rate');
      table.timestamp('updated_at').defaultTo(knex.fn.now());
    })
    .createTable('gps_points', (table) => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
      table.decimal('latitude', 10, 8).notNullable();
      table.decimal('longitude', 11, 8).notNullable();
      table.timestamp('timestamp').notNullable();
      table.decimal('speed', 5, 2); // m/s
      table.decimal('altitude', 7, 1); // meters
      table.decimal('accuracy', 6, 2); // meters
      table.timestamps(true, true);
    })
    .createTable('heart_rates', (table) => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
      table.timestamp('timestamp').notNullable();
      table.integer('heart_rate').notNullable(); // bpm
      table.timestamps(true, true);
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('heart_rates')
    .dropTableIfExists('gps_points')
    .alterTable('sport_records', (table) => {
      table.dropColumn('status');
      table.renameColumn('average_pace', 'avg_pace');
      table.renameColumn('average_heart_rate', 'avg_heart_rate');
      table.dropColumn('updated_at');
    });
};