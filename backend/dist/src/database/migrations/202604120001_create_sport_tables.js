"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.up = up;
exports.down = down;
/**
 * 创建运动数据相关表的迁移脚本
 *
 * 运行方式：
 * - npm run migrate 运行所有迁移
 * - npm run migrate:rollback 回滚上一次迁移
 */
async function up(knex) {
    // 创建运动记录表
    await knex.schema.createTable('sport_records', (table) => {
        table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
        table.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
        table.timestamp('start_time').notNullable().defaultTo(knex.fn.now());
        table.timestamp('end_time');
        table.decimal('distance', 10, 2).notNullable().defaultTo(0); // 米
        table.integer('duration').notNullable().defaultTo(0); // 秒
        table.decimal('calories', 8, 2).notNullable().defaultTo(0); // 卡路里
        table.enu('status', ['active', 'completed']).notNullable().defaultTo('active');
        table.decimal('average_pace', 6, 2); // 秒/公里
        table.integer('max_heart_rate'); // 最大心率
        table.decimal('average_heart_rate', 5, 2); // 平均心率
        table.timestamps(true, true);
    });
    // 创建 GPS 轨迹点表
    await knex.schema.createTable('gps_points', (table) => {
        table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
        table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
        table.decimal('latitude', 10, 8).notNullable();
        table.decimal('longitude', 11, 8).notNullable();
        table.timestamp('timestamp').notNullable();
        table.decimal('speed', 5, 2); // m/s
        table.decimal('altitude', 7, 1); // 米
        table.decimal('accuracy', 6, 2); // 米
        table.timestamps(true, true);
    });
    // 创建心率数据表
    await knex.schema.createTable('heart_rates', (table) => {
        table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
        table.uuid('record_id').notNullable().references('id').inTable('sport_records').onDelete('CASCADE');
        table.timestamp('timestamp').notNullable();
        table.integer('heart_rate').notNullable(); // bpm
        table.timestamps(true, true);
    });
    // 创建索引以提高查询性能
    await knex.schema.raw('CREATE INDEX idx_sport_records_user_id ON sport_records(user_id)');
    await knex.schema.raw('CREATE INDEX idx_sport_records_status ON sport_records(status)');
    await knex.schema.raw('CREATE INDEX idx_sport_records_start_time ON sport_records(start_time)');
    await knex.schema.raw('CREATE INDEX idx_gps_points_record_id ON gps_points(record_id)');
    await knex.schema.raw('CREATE INDEX idx_gps_points_timestamp ON gps_points(timestamp)');
    await knex.schema.raw('CREATE INDEX idx_heart_rates_record_id ON heart_rates(record_id)');
    await knex.schema.raw('CREATE INDEX idx_heart_rates_timestamp ON heart_rates(timestamp)');
}
async function down(knex) {
    await knex.schema.dropTableIfExists('heart_rates');
    await knex.schema.dropTableIfExists('gps_points');
    await knex.schema.dropTableIfExists('sport_records');
}
