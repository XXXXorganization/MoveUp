// seeds/initial_data.js
exports.seed = async function(knex) {
  // 清空可能已经存在的数据（按依赖顺序删除）
  await knex('user_tasks').del();
  await knex('user_achievements').del();
  await knex('user_challenges').del();
  await knex('tasks').del();
  await knex('challenges').del();
  await knex('badges').del();
  await knex('membership_plans').del();
  await knex('training_plans').del();

  // 插入徽章
  const badgeIds = await knex('badges').insert([
    {
      name: '首跑完成',
      description: '完成你的第一次跑步',
      icon: '/badges/first_run.png',
      condition_type: 'first_run',
      condition_value: 1,
      rarity: 'common'
    },
    {
      name: '1000公里俱乐部',
      description: '累计跑步距离达到1000公里',
      icon: '/badges/1000km.png',
      condition_type: 'distance_total',
      condition_value: 1000,
      rarity: 'rare'
    },
    {
      name: '马拉松勇士',
      description: '完成一次全程马拉松',
      icon: '/badges/marathon.png',
      condition_type: 'marathon_completed',
      condition_value: 1,
      rarity: 'epic'
    },
    {
      name: '晨跑之星',
      description: '连续30天晨跑',
      icon: '/badges/morning_star.png',
      condition_type: 'streak_days',
      condition_value: 30,
      rarity: 'epic'
    }
  ], 'id');

  // 插入任务
  await knex('tasks').insert([
    {
      name: '每日5公里',
      description: '单次跑步距离达到5公里',
      task_type: 'daily',
      requirement: JSON.stringify({ target: 5, unit: 'km' }),
      reward_points: 50,
      reward_badge_id: null,
      is_active: true
    },
    {
      name: '连续打卡7天',
      description: '连续7天完成运动',
      task_type: 'weekly',
      requirement: JSON.stringify({ target: 7, unit: 'days' }),
      reward_points: 100,
      reward_badge_id: null,
      is_active: true
    }
  ]);

  // 插入挑战（修正 reward_badge_id 为 badgeIds[0].id）
  await knex('challenges').insert([
    {
      name: '国庆线上跑',
      description: '10月1日至7日，累计跑量21公里',
      challenge_type: 'distance',
      start_time: new Date('2025-10-01 00:00:00+08'),
      end_time: new Date('2025-10-07 23:59:59+08'),
      target_value: 21,
      reward_points: 200,
      reward_badge_id: badgeIds[0].id  // 修正：提取 id 字符串
    }
  ]);

  // 插入会员套餐
  await knex('membership_plans').insert([
    {
      name: '基础会员',
      duration_days: 30,
      price: 9.9,
      features: JSON.stringify(['专属计划', '高级数据']),
      is_active: true
    },
    {
      name: '尊享会员',
      duration_days: 365,
      price: 99.0,
      features: JSON.stringify(['所有权益', '线下活动优先']),
      is_active: true
    }
  ]);

  // 插入默认训练计划
  await knex('training_plans').insert([
    {
      name: '新手5公里养成计划',
      description: '4周内完成5公里跑',
      difficulty: 'beginner',
      duration_weeks: 4,
      target_distance: 5,
      schedule: JSON.stringify([
        { week: 1, days: [{ day: 1, type: 'run', distance: 2 }] }
        // 可继续添加更多训练日
      ])
    }
  ]);
};