exports.up = function(knex) {
  return knex.schema
    .createTable('friendships', table => {
      table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
      table.uuid('friend_id').references('id').inTable('users').onDelete('CASCADE');
      table.smallint('status').defaultTo(0);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.timestamp('updated_at').defaultTo(knex.fn.now());
      table.primary(['user_id', 'friend_id']);
    })
    .createTable('posts', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.text('content');
      table.jsonb('images');
      table.uuid('sport_record_id').references('id').inTable('sport_records');
      table.string('location', 255);
      table.jsonb('tags');
      table.integer('like_count').defaultTo(0);
      table.integer('comment_count').defaultTo(0);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.index(['user_id', 'created_at']);
    })
    .createTable('comments', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.uuid('post_id').notNullable().references('id').inTable('posts').onDelete('CASCADE');
      table.uuid('parent_id').references('id').inTable('comments').onDelete('CASCADE');
      table.text('content').notNullable();
      table.integer('like_count').defaultTo(0);
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.index(['post_id', 'created_at']);
    })
    .createTable('likes', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.string('target_type', 20).notNullable();
      table.uuid('target_id').notNullable();
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.unique(['user_id', 'target_type', 'target_id']);
      table.index(['target_type', 'target_id']);
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('likes')
    .dropTableIfExists('comments')
    .dropTableIfExists('posts')
    .dropTableIfExists('friendships');
};