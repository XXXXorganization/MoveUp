// Moveup backend mock routes (OpenAPI docs/api.yaml based).

function standardResponse(code = 200, message = "success", data) {
  const res = { code, message };
  if (data !== undefined) res.data = data;
  return res;
}

function sendJson(res, payload, statusCode = 200) {
  if (typeof res?.json === "function") {
    if (typeof res.status === "function") res.status(statusCode);
    return res.json(payload);
  }
  if (typeof res?.writeHead === "function" && typeof res?.end === "function") {
    res.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
    res.end(JSON.stringify(payload));
    return;
  }
  res.body = payload;
  return;
}

function mockUser() {
  return {
    id: 10001, nickname: "跑步小白", avatar: "https://example.com/avatar.jpg",
    gender: 0, birthday: "1995-01-01", height: 175, weight: 70,
    total_distance: 1250.5, total_time: 36000, total_runs: 156, level: 5,
    preferences: { target_distance: 10, remind_time: "07:00", voice_frequency: "every_km" },
  };
}

function mockTokenData() {
  return { token: "MOCK_JWT_TOKEN", expires_in: 7200, user: mockUser() };
}

function mockRunStartResponse() { return { run_id: "RUN_MOCK_1", start_time: "2024-11-01T08:00:00Z" }; }
function mockRunPoints() { return [{ lat: 39.9087, lng: 116.3974, altitude: 50, timestamp: "2024-11-01T08:01:00Z", speed: 3.5, heart_rate: 145 }]; }
function mockRunListResponse() { return { total: 1, list: [{ run_id: "RUN_MOCK_1", date: "2024-11-01", distance: 5.23, duration: 1845, pace_avg: '5\'30"', calories: 380, route_image: "https://example.com/route.jpg" }] }; }
function mockStatsResponse() { return { total_distance: 35.6, total_duration: 10800, total_calories: 2450, avg_pace: '5\'15"', runs_count: 5, daily_data: [{ date: "11-01", distance: 5.2 }, { date: "11-02", distance: 8.5 }] }; }
function mockChallengeListResponse(status = "ongoing") { return { challenges: [{ id: 20001, name: "城市打卡挑战", status, description: "一周内完成指定距离" }] }; }
function mockBadges() { return [{ id: 30001, name: "首跑徽章", icon: "https://example.com/badge.png", achieved: true }]; }

const planDataDB = {
  "13800138000": { "MONDAY": [ { time: "6 A.M.", distance: "20 Km" } ], "TUESDAY": [ { time: "4 P.M.", distance: "10 Km" } ] }
};

const postsDB = [
  {
    id: "p1", club_id: "c1", author: "Anonim", timeText: "Today at 23:43 AM", lateTitle: "Late Post",
    postImageResId: "term1", postBadgeText: "3", subLine: "Morning Walk", subDetail: "6.00 Km • 30m00s", avatarResId: "ic_avatar_placeholder",
    liked_by: [], 
    comments: [
      { id: "cm1", user_id: "admin", author: "管理员", content: "太棒了！", time: "10:00 AM", reply_to_id: null, reply_to_name: null },
      { id: "cm2", user_id: "user2", author: "跑友A", content: "同感，昨天我也去跑了", time: "11:00 AM", reply_to_id: "cm1", reply_to_name: "管理员" },
      { id: "cm3", user_id: "user3", author: "跑友B", content: "这个配速无敌了", time: "12:00 PM", reply_to_id: null, reply_to_name: null },
      { id: "cm4", user_id: "user4", author: "跑友C", content: "下次一起跑！", time: "13:00 PM", reply_to_id: "cm3", reply_to_name: "跑友B" }
    ]
  }
];

const usersDB = {
  "13800138000": { password: "123456", username: "跑步小白" },
  "admin": { password: "admin123", username: "管理员" }
};

const clubsDB = [
  { id: "c1", name: "Hangzhou Pacers", location: "中国浙江杭州团", flag: "🇨🇳" },
  { id: "c2", name: "Quzhou Runners", location: "中国浙江衢州团", flag: "🇨🇳" },
  { id: "c3", name: "Taizhou Trackers", location: "中国浙江台州团", flag: "🇨🇳" },
  { id: "c4", name: "Ningbo Sprint", location: "中国浙江宁波团", flag: "🇨🇳" },
  { id: "c5", name: "Wenzhou Marathon", location: "中国浙江温州团", flag: "🇨🇳" },
  { id: "c6", name: "Tangerang Runners", location: "Tangerang, Indonesia", flag: "🇮🇩" },
  { id: "c7", name: "JakBar Pacer", location: "Jakarta Barat, Indonesia", flag: "🇮🇩" }
];

const userClubsDB = { "13800138000": ["c1", "c2"] };

const userRunsDB = {
  "13800138000": { 
    stats: { total_distance: "35.60", total_duration_str: "10h 30m", total_runs: "5", avg_pace: "5'15\"" }, 
    list: [ 
      { id: "run_101", date: "2024-04-08", title: "Morning Run", duration_str: "25.30", pace: "5'05\"", distance: "5.00 Km" },
      { id: "run_102", date: "2024-04-06", title: "Evening Jog", duration_str: "15.00", pace: "6'00\"", distance: "2.50 Km" }
    ] 
  }
};

const routes = [
  { method: "POST", path: "/auth/code", handler: (req, res) => sendJson(res, standardResponse(200, "success")) },
  { method: "POST", path: "/auth/register", handler: (req, res) => {
      const { phone, username, password } = req.body || {};
      if (!phone || !username || !password) return sendJson(res, standardResponse(400, "注册信息不完整"));
      if (usersDB.hasOwnProperty(phone)) return sendJson(res, standardResponse(409, "该手机号已注册，请直接登录"));
      usersDB[phone] = { password, username };
      userClubsDB[phone] = [];
      return sendJson(res, standardResponse(200, "注册成功"));
    }
  },
  { method: "POST", path: "/auth/login", handler: (req, res) => {
      const { phone, code } = req.body || {};
      if (!phone || !usersDB.hasOwnProperty(phone)) return sendJson(res, standardResponse(404, "账号不存在，请先注册"));
      if (usersDB[phone].password !== code) return sendJson(res, standardResponse(401, "密码错误，请重新输入"));
      return sendJson(res, standardResponse(200, `欢迎回来, ${usersDB[phone].username}`, mockTokenData()));
    }
  },
  { method: "GET", path: "/clubs/:id/posts", handler: (req, res) => {
      const clubId = req.params.id;
      const userId = req.query.user_id || "13800138000";
      let clubPosts = postsDB.filter(p => p.club_id === clubId);
      const formattedPosts = clubPosts.map(post => {
        return {
          ...post,
          is_liked: post.liked_by.includes(userId),
          like_count: post.liked_by.length,
          total_comments: post.comments.length,
          comments: post.comments.slice(-3) // 只返回最新的3条
        };
      });
      return sendJson(res, standardResponse(200, "success", { list: formattedPosts }));
    }
  },

  { method: "POST", path: "/clubs/:id/posts", handler: (req, res) => {
      const clubId = req.params.id;
      const { user_id, run_id, content, timestamp } = req.body || {};
      const userId = user_id || "13800138000";
      const userName = usersDB[userId] ? usersDB[userId].username : "Unknown";

      const userRuns = userRunsDB[userId]?.list || [];
      const runInfo = userRuns.find(r => r.id === run_id) || { title: "跑步打卡", distance: "0.00 Km", duration_str: "0m00s" };

      const newPost = {
        id: "p_" + Date.now(),
        club_id: clubId,
        author: userName,
        timeText: new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
        lateTitle: content || "完成了一次超棒的跑步！",
        postImageResId: "term1", 
        postBadgeText: "Shared",
        subLine: runInfo.title,
        subDetail: `${runInfo.distance} • ${runInfo.duration_str}`, 
        avatarResId: "ic_avatar_placeholder",
        liked_by: [],
        comments: []
      };

      postsDB.unshift(newPost);
      return sendJson(res, standardResponse(200, "分享成功", newPost));
    }
  },

  { method: "POST", path: "/posts/:id/like", handler: (req, res) => {
      const postId = req.params.id;
      const userId = (req.body || {}).user_id || "13800138000";
      const post = postsDB.find(p => p.id === postId);
      if (!post) return sendJson(res, standardResponse(404, "帖子不存在"));
      const idx = post.liked_by.indexOf(userId);
      let isLikedNow = false;
      if (idx > -1) { post.liked_by.splice(idx, 1); } 
      else { post.liked_by.push(userId); isLikedNow = true; }
      return sendJson(res, standardResponse(200, "操作成功", { is_liked: isLikedNow, like_count: post.liked_by.length }));
    }
  },
  { method: "POST", path: "/posts/:id/comment", handler: (req, res) => {
      const postId = req.params.id;
      const { user_id, content, timestamp, reply_to_id } = req.body || {};
      const userId = user_id || "13800138000";
      const userName = usersDB[userId] ? usersDB[userId].username : "Unknown";

      const post = postsDB.find(p => p.id === postId);
      if (!post) return sendJson(res, standardResponse(404, "帖子不存在"));

      let reply_to_name = null;
      if (reply_to_id) {
          const parentComment = post.comments.find(c => c.id === reply_to_id);
          if (parentComment) {
              reply_to_name = parentComment.author;
          }
      }

      const newComment = {
        id: "cm_" + Date.now(),
        user_id: userId,
        author: userName,
        content: content,
        time: new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
        reply_to_id: reply_to_id || null,
        reply_to_name: reply_to_name 
      };

      post.comments.push(newComment);
      return sendJson(res, standardResponse(200, "评论成功", {
        total_comments: post.comments.length,
        comments: post.comments.slice(-3)
      }));
    }
  },
  { method: "GET", path: "/posts/:id/comments", handler: (req, res) => {
      const postId = req.params.id;
      const post = postsDB.find(p => p.id === postId);
      if (!post) return sendJson(res, standardResponse(404, "帖子不存在"));
      const allComments = [...post.comments].reverse();
      return sendJson(res, standardResponse(200, "success", { list: allComments }));
    }
  },

  { method: "GET", path: "/user/clubs", handler: (req, res) => {
      const userId = req.query.user_id || "13800138000";
      const joinedClubIds = userClubsDB[userId] || [];
      const myClubs = clubsDB.filter(c => joinedClubIds.includes(c.id));
      return sendJson(res, standardResponse(200, "success", { list: myClubs }));
    }
  },
  
  { method: "GET", path: "/plan/details", handler: (req, res) => {
      const userId = req.query.user_id || "13800138000";
      const day = (req.query.day || "MONDAY").toUpperCase();
      const userPlans = planDataDB[userId] || planDataDB["13800138000"];
      return sendJson(res, standardResponse(200, "success", { day: day, list: userPlans[day] || [] }));
    }
  },

  // 🌟 新增：计算该用户一周七天累加的总距离
  { method: "GET", path: "/plan/total_distance", handler: (req, res) => {
      const userId = req.query.user_id || "13800138000";
      const userPlans = planDataDB[userId] || {};
      
      let total = 0;
      for (const day in userPlans) {
          userPlans[day].forEach(plan => {
              if (plan.distance) {
                  // 提取出形如 "20.5 Km" 里面的数字 20.5
                  const val = parseFloat(plan.distance);
                  if (!isNaN(val)) total += val;
              }
          });
      }
      
      // 去掉多余的小数点 0（比如 42.00 变 42，42.5 变 42.5）
      let totalStr = Number(total.toFixed(2)).toString();
      
      return sendJson(res, standardResponse(200, "success", { total_distance: totalStr }));
    }
  },
  
  { method: "POST", path: "/plan/details", handler: (req, res) => {
      const { user_id, day, start_time, end_time, distance } = req.body || {};
      
      if (!planDataDB[user_id]) planDataDB[user_id] = {};
      if (!planDataDB[user_id][day]) planDataDB[user_id][day] = [];
      
      let timeStr = start_time || "";
      if (end_time && end_time.trim() !== "") {
          timeStr += " - " + end_time;
      }
      
      planDataDB[user_id][day].push({ 
          time: timeStr, 
          start_time: start_time, 
          end_time: end_time, 
          distance: distance 
      });
      
      console.log(`✅ [计划保存] 用户 ${user_id} 添加计划到 ${day}：${timeStr} (${distance})`);
      return sendJson(res, standardResponse(200, "添加成功"));
    }
  },

  { method: "POST", path: "/plan/details/delete", handler: (req, res) => {
      const { user_id, day, index } = req.body || {};
      if (planDataDB[user_id] && planDataDB[user_id][day]) {
        planDataDB[user_id][day].splice(index, 1);
      }
      return sendJson(res, standardResponse(200, "删除成功"));
    }
  },
  { method: "GET", path: "/clubs", handler: (req, res) => sendJson(res, standardResponse(200, "success", { list: clubsDB })) },
  { method: "GET", path: "/clubs/:id", handler: (req, res) => {
      const club = clubsDB.find(c => c.id === req.params.id);
      const isJoined = (userClubsDB[req.query.user_id || "13800138000"] || []).includes(req.params.id);
      return sendJson(res, standardResponse(200, "success", { ...club, is_joined: isJoined }));
    }
  },
  { method: "POST", path: "/clubs/:id/toggle", handler: (req, res) => {
      const userId = (req.body || {}).user_id || "13800138000";
      if (!userClubsDB[userId]) userClubsDB[userId] = [];
      const idx = userClubsDB[userId].indexOf(req.params.id);
      let isJoined = false;
      if (idx > -1) userClubsDB[userId].splice(idx, 1);
      else { userClubsDB[userId].push(req.params.id); isJoined = true; }
      return sendJson(res, standardResponse(200, "操作成功", { is_joined: isJoined }));
    }
  },
  { method: "GET", path: "/user/profile", handler: (req, res) => sendJson(res, standardResponse(200, "success", { phone: "138", username: "测试" })) },
  { method: "GET", path: "/runs", handler: (req, res) => sendJson(res, standardResponse(200, "success", userRunsDB["13800138000"])) },
  { method: "POST", path: "/runs/start", handler: (req, res) => sendJson(res, standardResponse(200, "success", mockRunStartResponse())) },
  { method: "POST", path: "/runs/:run_id/points", handler: (req, res) => sendJson(res, standardResponse(200, "success")) }
];

module.exports = { routes, standardResponse };