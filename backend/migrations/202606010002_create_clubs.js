exports.up = function(knex) {
  return knex.schema
    // 社团表
    .createTable('clubs', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.string('name', 100).notNullable();
      table.text('description');
      table.string('location', 100);
      table.string('image_url', 255);
      table.string('flag', 10).defaultTo('CN');
      table.uuid('creator_id').references('id').inTable('users');
      table.timestamps(true, true);
    })
    // 社团成员表
    .createTable('club_members', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('club_id').notNullable().references('id').inTable('clubs').onDelete('CASCADE');
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.string('role', 20).defaultTo('member');
      table.timestamp('joined_at').defaultTo(knex.fn.now());
      table.unique(['club_id', 'user_id']);
    })
    // 社团帖子表
    .createTable('club_posts', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('club_id').notNullable().references('id').inTable('clubs').onDelete('CASCADE');
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.text('content');
      table.string('run_id');
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    // 帖子评论表
    .createTable('club_comments', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('post_id').notNullable().references('id').inTable('club_posts').onDelete('CASCADE');
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.text('content').notNullable();
      table.uuid('reply_to_id');
      table.timestamp('created_at').defaultTo(knex.fn.now());
    })
    // 帖子点赞表
    .createTable('club_post_likes', table => {
      table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
      table.uuid('post_id').notNullable().references('id').inTable('club_posts').onDelete('CASCADE');
      table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
      table.timestamp('created_at').defaultTo(knex.fn.now());
      table.unique(['post_id', 'user_id']);
    });
};

exports.down = function(knex) {
  return knex.schema
    .dropTableIfExists('club_post_likes')
    .dropTableIfExists('club_comments')
    .dropTableIfExists('club_posts')
    .dropTableIfExists('club_members')
    .dropTableIfExists('clubs');
};
