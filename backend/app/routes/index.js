// Moveup backend mock routes (OpenAPI docs/api.yaml based).
//
// This project currently ships only a DB connection test in /backend.
// To unblock front-end development, this file provides a lightweight mock
// route configuration with placeholder StandardResponse payloads.

function standardResponse(code = 200, message = "success", data) {
  const res = { code, message };
  if (data !== undefined) res.data = data;
  return res;
}

function sendJson(res, payload, statusCode = 200) {
  // Express-like response
  if (typeof res?.json === "function") {
    if (typeof res.status === "function") res.status(statusCode);
    return res.json(payload);
  }

  // Node http-like response
  if (typeof res?.writeHead === "function" && typeof res?.end === "function") {
    res.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
    res.end(JSON.stringify(payload));
    return;
  }

  // Fallback
  res.body = payload;
  return;
}

function mockUser() {
  return {
    id: 10001,
    nickname: "跑步小白",
    avatar: "https://example.com/avatar.jpg",
    gender: 0,
    birthday: "1995-01-01",
    height: 175,
    weight: 70,
    total_distance: 1250.5,
    total_time: 36000,
    total_runs: 156,
    level: 5,
    preferences: {
      target_distance: 10,
      remind_time: "07:00",
      voice_frequency: "every_km",
    },
  };
}

function mockTokenData() {
  return {
    token: "MOCK_JWT_TOKEN",
    expires_in: 7200,
    user: mockUser(),
  };
}

function mockRunStartResponse() {
  return {
    run_id: "RUN_MOCK_1",
    start_time: "2024-11-01T08:00:00Z",
  };
}

function mockRunPoints() {
  return [
    {
      lat: 39.9087,
      lng: 116.3974,
      altitude: 50,
      timestamp: "2024-11-01T08:01:00Z",
      speed: 3.5,
      heart_rate: 145,
    },
  ];
}

function mockRunListResponse() {
  return {
    total: 1,
    list: [
      {
        run_id: "RUN_MOCK_1",
        date: "2024-11-01",
        distance: 5.23,
        duration: 1845,
        pace_avg: '5\'30"',
        calories: 380,
        route_image: "https://example.com/route.jpg",
      },
    ],
  };
}

function mockStatsResponse() {
  return {
    total_distance: 35.6,
    total_duration: 10800,
    total_calories: 2450,
    avg_pace: '5\'15"',
    runs_count: 5,
    daily_data: [
      { date: "11-01", distance: 5.2 },
      { date: "11-02", distance: 8.5 },
    ],
  };
}

function mockChallengeListResponse(status = "ongoing") {
  return {
    challenges: [
      {
        id: 20001,
        name: "城市打卡挑战",
        status,
        description: "一周内完成指定距离",
      },
    ],
  };
}

function mockBadges() {
  return [
    {
      id: 30001,
      name: "首跑徽章",
      icon: "https://example.com/badge.png",
      achieved: true,
    },
  ];
}

// Route paths use ":" params style (e.g. /runs/:run_id/points).
// You can wire this with your router of choice.
const routes = [
  {
    method: "POST",
    path: "/auth/code",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "POST",
    path: "/auth/login",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockTokenData())),
  },

  {
    method: "GET",
    path: "/user/profile",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockUser())),
  },
  {
    method: "PUT",
    path: "/user/profile",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "POST",
    path: "/user/avatar",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },

  {
    method: "POST",
    path: "/runs/start",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockRunStartResponse())),
  },
  {
    method: "POST",
    path: "/runs/:run_id/points",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "POST",
    path: "/runs/:run_id/finish",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "GET",
    path: "/runs",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockRunListResponse())),
  },
  {
    method: "GET",
    path: "/runs/:run_id",
    handler: (req, res) =>
      sendJson(res, standardResponse(200, "success", { run_id: req?.params?.run_id || "RUN_MOCK_1", points: mockRunPoints() })),
  },
  {
    method: "GET",
    path: "/stats",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockStatsResponse())),
  },

  {
    method: "POST",
    path: "/routes",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "GET",
    path: "/routes/recommended",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", { routes: [{ name: "朝阳公园晨跑路线", distance: 3.2, city: "北京" }] })),
  },

  {
    method: "GET",
    path: "/users/search",
    handler: (req, res) =>
      sendJson(res, standardResponse(200, "success", { users: [{ ...mockUser(), id: 10002, nickname: "跑步进阶" }] })),
  },

  {
    method: "POST",
    path: "/friends/request",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "POST",
    path: "/friends/respond",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "GET",
    path: "/friends",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", { friends: [mockUser()] })),
  },

  {
    method: "POST",
    path: "/feeds",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "GET",
    path: "/feeds",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", { list: [{ feed_id: "FEED_MOCK_1", content: "今日完成10公里！" }] })),
  },
  {
    method: "POST",
    path: "/feeds/:feed_id/like",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },
  {
    method: "POST",
    path: "/feeds/:feed_id/comment",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },

  {
    method: "GET",
    path: "/leaderboard",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", { list: [{ user_id: 10001, nickname: "跑步小白", distance: 42.0 }] })),
  },

  {
    method: "GET",
    path: "/challenges",
    handler: (req, res) => {
      const status = req?.query?.status || "ongoing";
      sendJson(res, standardResponse(200, "success", mockChallengeListResponse(status)));
    },
  },
  {
    method: "POST",
    path: "/challenges/:challenge_id/join",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },

  {
    method: "GET",
    path: "/badges",
    handler: (req, res) => sendJson(res, standardResponse(200, "success", mockBadges())),
  },

  {
    method: "POST",
    path: "/upload/image",
    handler: (req, res) => sendJson(res, standardResponse(200, "success")),
  },

  {
    method: "GET",
    path: "/config",
    handler: (req, res) =>
      sendJson(res, standardResponse(200, "success", { voice_packs: [], app_version: "1.0", privacy_policy_url: "https://example.com/privacy" })),
  },
];

module.exports = {
  routes,
  standardResponse,
};

