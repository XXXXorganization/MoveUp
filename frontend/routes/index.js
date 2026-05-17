// Moveup backend mock routes (OpenAPI docs/api.yaml based).
const axios = require('axios');

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

// ============== Mock 数据库 =================
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

const planDataDB = {
  "13800138000": { "MONDAY": [ { time: "06:00 AM - 07:00 AM", start_time: "06:00 AM", end_time: "07:00 AM", distance: "20 Km" } ], "TUESDAY": [ { time: "04:00 PM - 05:00 PM", start_time: "04:00 PM", end_time: "05:00 PM", distance: "10 Km" } ] }
};

const postsDB = [
  {
    id: "p1", club_id: "c1", author: "Anonim", timeText: "Today at 23:43 AM", lateTitle: "Late Post",
    postImageResId: "term1", postBadgeText: "3", subLine: "Morning Walk", subDetail: "6.00 Km • 30m00s", avatarResId: "ic_avatar_placeholder",
    liked_by: [], 
    comments: []
  }
];

const usersDB = {
  "13800138000": { password: "123456", username: "跑步小白", email: "user@example.com" }
};

// 🌟 修改点：给所有的社团加上了真实的运动场景网络图片 image_url
const clubsDB = [
  { id: "c1", name: "Hangzhou Pacers", location: "中国浙江杭州团", flag: "🇨🇳", image_url: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=600&q=80" },
  { id: "c2", name: "Quzhou Runners", location: "中国浙江衢州团", flag: "🇨🇳", image_url: "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=600&q=80" },
  { id: "c3", name: "Taizhou Trackers", location: "中国浙江台州团", flag: "🇨🇳", image_url: "https://images.unsplash.com/photo-1530549387789-4c1017266635?auto=format&fit=crop&w=600&q=80" }
];

const userClubsDB = { "13800138000": ["c1", "c2"] };

const userRunsDB = {
  "13800138000": { 
    stats: { total_distance: "35.60", total_duration_str: "10h 30m", total_runs: "5", avg_pace: "5'15\"" }, 
    list: [ 
      { id: "run_101", date: "2024-04-08", title: "Morning Run", duration_str: "25.30", pace: "5'05\"", distance: "5.00 Km" }
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
          comments: post.comments.slice(-3)
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

  { method: "GET", path: "/plan/total_distance", handler: (req, res) => {
      const userId = req.query.user_id || "13800138000";
      const userPlans = planDataDB[userId] || {};
      
      let total = 0;
      for (const day in userPlans) {
          userPlans[day].forEach(plan => {
              if (plan.distance) {
                  const val = parseFloat(plan.distance);
                  if (!isNaN(val)) total += val;
              }
          });
      }
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

  // 1. 获取个人资料
  { method: "GET", path: "/user/profile", handler: (req, res) => {
      const userIdStr = req.query.user_id || "13800138000";
      const user = usersDB[userIdStr] || { username: "测试账号", email: "", password: "" };
      
      return sendJson(res, standardResponse(200, "success", { 
          phone: userIdStr, 
          username: user.username || "",
          email: user.email || "",
          password: user.password || ""
      }));
    }
  },

  // 2. 修改个人资料
  { method: "PUT", path: "/user/profile", handler: (req, res) => {
      const { user_id, username, email, password } = req.body || {};
      const userIdStr = user_id || "13800138000";

      if (!usersDB[userIdStr]) {
          usersDB[userIdStr] = {};
      }

      if (username) usersDB[userIdStr].username = username;
      if (email) usersDB[userIdStr].email = email;
      if (password) usersDB[userIdStr].password = password;

      console.log(`✅ [用户信息更新] 账号: ${userIdStr} | 新昵称: ${username} | 新邮箱: ${email} | 新密码: ${password}`);

      return sendJson(res, standardResponse(200, "Profile updated successfully"));
    }
  },

  { method: "GET", path: "/runs", handler: (req, res) => sendJson(res, standardResponse(200, "success", userRunsDB["13800138000"])) },
  { method: "POST", path: "/runs/start", handler: (req, res) => sendJson(res, standardResponse(200, "success", mockRunStartResponse())) },
  { method: "POST", path: "/runs/:run_id/points", handler: (req, res) => sendJson(res, standardResponse(200, "success")) },

  { method: "POST", path: "/runs/finish", handler: (req, res) => {
      const { user_id, distance, duration_str, pace, calories } = req.body || {};
      const userIdStr = user_id || "13800138000";
      
      if (!userRunsDB[userIdStr]) {
          userRunsDB[userIdStr] = { stats: { total_distance: "0.00", total_duration_str: "0h 0m", total_runs: "0", avg_pace: "0'00\"" }, list: [] };
      }

      const today = new Date();
      const dateStr = today.getFullYear() + "-" + String(today.getMonth() + 1).padStart(2, '0') + "-" + String(today.getDate()).padStart(2, '0');

      const newRun = {
          id: "run_" + Date.now(),
          date: dateStr,
          title: "Outdoor Run",
          duration_str: duration_str || "00.00",
          pace: pace || "0'00\"",
          distance: (distance || "0.00") + " Km",
          calories: calories || "0"
      };

      userRunsDB[userIdStr].list.unshift(newRun);

      let currentTotalDist = parseFloat(userRunsDB[userIdStr].stats.total_distance) || 0;
      let addedDist = parseFloat(distance) || 0;
      userRunsDB[userIdStr].stats.total_distance = (currentTotalDist + addedDist).toFixed(2);

      let runsCount = parseInt(userRunsDB[userIdStr].stats.total_runs) || 0;
      userRunsDB[userIdStr].stats.total_runs = (runsCount + 1).toString();

      console.log(`✅ [记录保存] 用户 ${userIdStr} 结束了运动！上传数据：距离 ${distance}Km, 时长 ${duration_str}, 消耗 ${calories}kcal`);

      return sendJson(res, standardResponse(200, "Run finished and saved to history", newRun));
    }
  },

  // ==========================================
  // 🌟 Agent AI 接口：恢复完整的智能提取和对话逻辑
  // ==========================================
  { method: "POST", path: "/ai/chat", async handler(req, res) {
      const { user_id, chat_history } = req.body || {};
      
      if (!chat_history || !Array.isArray(chat_history) || chat_history.length === 0) {
          return sendJson(res, standardResponse(400, "聊天记录不能为空"));
      }

      const userIdStr = user_id || "13800138000";
      
      let runningDataStr = "该用户目前没有跑步记录。";
      if (userRunsDB[userIdStr]) {
          const stats = userRunsDB[userIdStr].stats;
          runningDataStr = `该用户历史总跑步里程为 ${stats.total_distance} 公里，总时长 ${stats.total_duration_str}，平均配速 ${stats.avg_pace}。`;
      }

      const currentPlans = planDataDB[userIdStr] || {};
      let planStr = "当前没有任何计划。\n";
      const daysOfWeek = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
      
      planStr = daysOfWeek.map(day => {
          const plans = currentPlans[day];
          if (plans && plans.length > 0) {
              return `[${day}]: ` + plans.map(p => `${p.time} (${p.distance})`).join("，");
          } else {
              return `[${day}]: 休息无安排`;
          }
      }).join("\n");

      const aiPromptMessages = [
          { 
              "role": "system", 
              "content": `你是一个名叫 MoveUp 的专业跑步助理教练。
【已知信息】：${runningDataStr}
【用户当前的每周详细计划表】：
${planStr}

🔴 【核心任务 1：查询计划】
如果用户问“查看我现在的计划表”等，你必须完整、清晰地列出周一到周日所有7天的安排，绝对不能因为字数原因截断或省略！

🔴 【核心任务 2：生成或增加计划】
如果用户要求你生成或安排新的运动计划：
1. 请先给出文字建议。如果用户要求你每天增加计划，你必须将计划分配到一周的多个日子中。
2. 在你的回答的最末尾，严格另起一行，输出以下特殊标记的 JSON 数组。

格式要求极度严格，必须长这样（必须包含 end_time）：
###PLAN:[{"day":"MONDAY","start_time":"07:00 AM","end_time":"08:00 AM","distance":"5 Km"},{"day":"FRIDAY","start_time":"06:00 PM","end_time":"07:30 PM","distance":"8 Km"}]

说明：
1. day 必须是英文星期全大写（如 MONDAY、TUESDAY、WEDNESDAY 等）。
2. start_time 和 end_time 都必须填写，格式如 07:00 AM。
3. distance 必须带单位 Km（如 5 Km）。

⚠️ 警告：在输出完 ']' 之后，绝对不允许再输出任何标点符号、祝福语或其他文字！必须以 ']' 结尾！如果你只是正常聊天，绝不要输出 ###PLAN: 标记！`
          },
          ...chat_history 
      ];

      const SILICONFLOW_API_KEY = "sk-qumwdesmjahfmwrnjvdrtqqtkphdtzdjezygnqzbudrcoyud";

      try {
          const response = await axios.post('https://api.siliconflow.cn/v1/chat/completions', {
              model: "Qwen/Qwen2.5-7B-Instruct",
              messages: aiPromptMessages,
              stream: false
          }, {
              headers: {
                  'Authorization': `Bearer ${SILICONFLOW_API_KEY}`,
                  'Content-Type': 'application/json'
              },
              timeout: 15000 
          });

          let aiReplyText = response.data.choices[0].message.content;

          if (aiReplyText.includes("###PLAN:")) {
              try {
                  const parts = aiReplyText.split("###PLAN:");
                  aiReplyText = parts[0].trim(); 
                  
                  let rawJsonArea = parts[1].trim(); 
                  
                  const jsonMatch = rawJsonArea.match(/\[[\s\S]*\]/);
                  
                  if (jsonMatch) {
                      let cleanJsonStr = jsonMatch[0];
                      const newPlans = JSON.parse(cleanJsonStr);

                      if (Array.isArray(newPlans)) {
                          if (!planDataDB[userIdStr]) planDataDB[userIdStr] = {};
                          
                          newPlans.forEach(p => {
                              const d = (p.day || "MONDAY").toUpperCase();
                              if (!planDataDB[userIdStr][d]) planDataDB[userIdStr][d] = [];
                              
                              const sTime = p.start_time || "07:00 AM";
                              const eTime = p.end_time || "08:00 AM"; 
                              const dist = p.distance || "5 Km";
                              const tStr = sTime + " - " + eTime;

                              planDataDB[userIdStr][d].push({ 
                                  time: tStr, 
                                  start_time: sTime, 
                                  end_time: eTime, 
                                  distance: dist 
                              });
                          });
                          console.log(`✅ [AI 自动执行] 成功为用户提取并写入了 ${newPlans.length} 条计划！`);
                          
                          aiReplyText += "\n\n(🔔 MoveUp系统提示：我已经帮您把这份计划自动添加到了系统 Plan 日历中，您可以随时退出聊天前往查看啦！)";
                      }
                  } else {
                      console.error("❌ 正则提取失败，未能找到合法的 JSON 数组结构");
                  }
              } catch (e) {
                  console.error("❌ 解析 AI 的计划暗号失败:", e.message);
              }
          }

          return sendJson(res, standardResponse(200, "success", { reply: aiReplyText }));

      } catch (error) {
          console.error("❌ 调用 API 失败:", error.response ? error.response.data : error.message);
          return sendJson(res, standardResponse(500, "AI 服务暂时不可用，请稍后再试"));
      }
    }
  }
];

module.exports = { routes, standardResponse };