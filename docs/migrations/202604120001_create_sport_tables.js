exports.up = function(knex) {
  return knex.schema
    .createTable('sport_records', (table) => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.timestamp('start_time').notNullable().defaultTo(knex.fn.now());
      table.timestamp('end_time');
      table.decimal('distance', 10, 2).notNullable().defaultTo(0); // meters
      table.integer('duration').notNullable().defaultTo(0); // seconds
      table.decimal('calories', 8, 2).notNullable().defaultTo(0);
      table.enu('status', ['active', 'completed']).notNullable().defaultTo('active');
      table.decimal('average_pace', 6, 2); // seconds per km
      table.integer('max_heart_rate');
      table.decimal('average_heart_rate', 5, 2);
      table.timestamps(true, true);
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
    .dropTableIfExists('sport_records');
};