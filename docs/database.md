dbdiagram.io网址 https://dbdiagram.io/d/69c23dbb78c6c4bc7a5191bf


dbdiagram.io语法设计的ER图，复制以下代码即可，生成ER图
// ===========================================
// 1. 用户管理模块
// ===========================================
Table users {
  id UUID [pk, default: `gen_random_uuid()`]
  phone VARCHAR(20) [unique, not null]
  nickname VARCHAR(50) [not null]
  avatar VARCHAR(255)
  gender SMALLINT
  birthday DATE
  height SMALLINT
  weight DECIMAL(5,2)
  target_distance INT
  target_time INT
  role VARCHAR(20) [default: 'user']
  created_at TIMESTAMPTZ [default: `now()`]
  updated_at TIMESTAMPTZ [default: `now()`]
  indexes {
    phone
  }
}

Table user_devices {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  device_type VARCHAR(20)
  device_token VARCHAR(255)
  login_time TIMESTAMPTZ [default: `now()`]
  logout_time TIMESTAMPTZ
  is_active BOOLEAN [default: true]
  indexes {
    user_id
  }
}

// ===========================================
// 2. 运动数据模块
// ===========================================
Table sport_records {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  start_time TIMESTAMPTZ [not null]
  end_time TIMESTAMPTZ [not null]
  distance INT [not null]
  duration INT [not null]
  calories INT
  avg_pace DECIMAL(5,2)
  max_pace DECIMAL(5,2)
  avg_heart_rate SMALLINT
  max_heart_rate SMALLINT
  gps_track JSONB
  route_id UUID [ref: > routes.id]
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    user_id
    start_time
  }
}

Table routes {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(100) [not null]
  start_point JSONB
  end_point JSONB
  distance INT
  elevation_gain INT
  popularity INT [default: 0]
  is_hot BOOLEAN [default: false]
  created_by UUID [ref: > users.id]
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    popularity
    is_hot
  }
}

// ===========================================
// 3. 社交互动模块
// ===========================================
Table friendships {
  user_id UUID [ref: > users.id]
  friend_id UUID [ref: > users.id]
  status SMALLINT [default: 0]
  created_at TIMESTAMPTZ [default: `now()`]
  updated_at TIMESTAMPTZ [default: `now()`]
  primary key (user_id, friend_id)
}

Table posts {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  content TEXT
  images JSONB
  sport_record_id UUID [ref: > sport_records.id]
  location VARCHAR(255)
  tags JSONB
  like_count INT [default: 0]
  comment_count INT [default: 0]
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    user_id
    created_at
  }
}

Table likes {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  target_type VARCHAR(20)
  target_id UUID
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    (user_id, target_type, target_id) [unique]
    (target_type, target_id)
  }
}

Table comments {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  post_id UUID [ref: > posts.id]
  parent_id UUID [ref: > comments.id]
  content TEXT [not null]
  like_count INT [default: 0]
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    post_id
    created_at
  }
}

// ===========================================
// 4. 挑战与激励模块
// ===========================================
Table badges {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(50) [not null]
  description TEXT
  icon VARCHAR(255)
  condition_type VARCHAR(30)
  condition_value INT
  rarity VARCHAR(20)
  created_at TIMESTAMPTZ [default: `now()`]
}

Table tasks {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(100) [not null]
  description TEXT
  task_type VARCHAR(30)
  requirement JSONB
  reward_points INT [default: 0]
  reward_badge_id UUID [ref: > badges.id]
  is_active BOOLEAN [default: true]
  created_at TIMESTAMPTZ [default: `now()`]
}

Table user_tasks {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  task_id UUID [ref: > tasks.id]
  progress INT [default: 0]
  is_completed BOOLEAN [default: false]
  completed_at TIMESTAMPTZ
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    (user_id, task_id) [unique]
    user_id
  }
}

Table user_achievements {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  badge_id UUID [ref: > badges.id]
  achieved_at TIMESTAMPTZ [default: `now()`]
  indexes {
    (user_id, badge_id) [unique]
    user_id
  }
}

Table challenges {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(100) [not null]
  description TEXT
  challenge_type VARCHAR(30)
  start_time TIMESTAMPTZ [not null]
  end_time TIMESTAMPTZ [not null]
  target_value INT
  reward_points INT
  reward_badge_id UUID [ref: > badges.id]
  created_at TIMESTAMPTZ [default: `now()`]
}

Table user_challenges {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  challenge_id UUID [ref: > challenges.id]
  progress INT [default: 0]
  rank INT
  is_completed BOOLEAN [default: false]
  joined_at TIMESTAMPTZ [default: `now()`]
  completed_at TIMESTAMPTZ
  indexes {
    (user_id, challenge_id) [unique]
    challenge_id
  }
}

// ===========================================
// 5. 智能指导模块
// ===========================================
Table training_plans {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(100) [not null]
  description TEXT
  difficulty VARCHAR(20)
  duration_weeks INT
  target_distance INT
  schedule JSONB
  created_at TIMESTAMPTZ [default: `now()`]
}

Table user_plans {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  plan_id UUID [ref: > training_plans.id]
  current_week INT [default: 1]
  current_day INT [default: 1]
  is_active BOOLEAN [default: true]
  started_at TIMESTAMPTZ [default: `now()`]
  completed_at TIMESTAMPTZ
  indexes {
    user_id
  }
}

// ===========================================
// 6. 会员服务模块
// ===========================================
Table membership_plans {
  id UUID [pk, default: `gen_random_uuid()`]
  name VARCHAR(50) [not null]
  duration_days INT
  price DECIMAL(10,2)
  features JSONB
  is_active BOOLEAN [default: true]
  created_at TIMESTAMPTZ [default: `now()`]
}

Table user_memberships {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  plan_id UUID [ref: > membership_plans.id]
  start_date TIMESTAMPTZ [not null]
  end_date TIMESTAMPTZ [not null]
  is_active BOOLEAN [default: true]
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    user_id
    end_date
  }
}

Table points_logs {
  id UUID [pk, default: `gen_random_uuid()`]
  user_id UUID [ref: > users.id]
  points_change INT [not null]
  source_type VARCHAR(30)
  source_id UUID
  description VARCHAR(255)
  created_at TIMESTAMPTZ [default: `now()`]
  indexes {
    user_id
    created_at
  }
}