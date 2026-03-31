/* eslint-disable @typescript-eslint/no-explicit-any */

// Moveup API mock client (for development/testing without backend).
// Returns data shapes consistent with docs/api.yaml, but uses static placeholder values.

import type {
  BadgeItem,
  ChallengeListResponse,
  CommentCreate,
  FinishRunRequest,
  FriendRespond,
  FriendRequest,
  FileLike,
  RouteCreateRequest,
  RunListResponse,
  RunPointsRequest,
  RunPointItem,
  RunStartRequest,
  RunStartResponse,
  SearchUsersResponse,
  StatsResponse,
  TokenData,
  UserProfile,
} from "./apiClient";

const MOCK_TOKEN = "MOCK_JWT_TOKEN";

function getMockUser(): UserProfile {
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

function getMockTokenData(): TokenData {
  return {
    token: MOCK_TOKEN,
    expires_in: 7200,
    user: getMockUser(),
  };
}

function getMockRunPointItems(): RunPointItem[] {
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

export function createMockApiClient() {
  return {
    // Auth
    authCode: async (_params: { phone: string; type: "login" | "register" | "reset" }) => {
      return;
    },
    authLogin: async (_params: { phone: string; code: string }) => {
      return getMockTokenData();
    },

    // User
    getProfile: async () => {
      return getMockUser();
    },
    updateProfile: async (_patch: { nickname?: string; height?: number; weight?: number; birthday?: string }) => {
      return;
    },
    uploadAvatar: async (_file: FileLike) => {
      return;
    },

    // Runs
    runsStart: async (body: RunStartRequest): Promise<RunStartResponse> => {
      return {
        run_id: `RUN_MOCK_1_${body.run_type}`,
        start_time: "2024-11-01T08:00:00Z",
      };
    },
    runsUploadPoints: async (_run_id: string, _body: RunPointsRequest) => {
      return;
    },
    runsFinish: async (_run_id: string, _body: FinishRunRequest) => {
      return;
    },
    runsList: async (_params: { page?: number; size?: number; start_date?: string; end_date?: string }): Promise<RunListResponse> => {
      return {
        total: 1,
        list: [
          {
            run_id: "RUN_MOCK_1",
            date: "2024-11-01",
            distance: 5.23,
            duration: 1845,
            pace_avg: "5'30\"",
            calories: 380,
            route_image: "https://example.com/route.jpg",
          },
        ],
      };
    },
    runsDetail: async (_run_id: string) => {
      // openapi.yaml does not define data schema for GET /runs/{run_id}
      return {
        run_id: "RUN_MOCK_1",
        points: getMockRunPointItems(),
      };
    },

    // Stats
    stats: async (_params: { type: "week" | "month" | "year"; date: string }): Promise<StatsResponse> => {
      return {
        total_distance: 35.6,
        total_duration: 10800,
        total_calories: 2450,
        avg_pace: "5'15\"",
        runs_count: 5,
        daily_data: [
          { date: "11-01", distance: 5.2 },
          { date: "11-02", distance: 8.5 },
        ],
      };
    },

    // Routes
    saveRoute: async (_body: RouteCreateRequest) => {
      return;
    },
    routesRecommended: async (_params: { lat: number; lng: number; radius?: number }) => {
      return {
        routes: [
          {
            name: "朝阳公园晨跑路线",
            distance: 3.2,
            city: "北京",
          },
        ],
      };
    },

    // Users
    searchUsers: async (_keyword: string): Promise<SearchUsersResponse> => {
      return {
        users: [
          {
            ...getMockUser(),
            id: 10002,
            nickname: "跑步进阶",
          },
        ],
      };
    },

    // Friends
    friendsRequest: async (_body: FriendRequest) => {
      return;
    },
    friendsRespond: async (_body: FriendRespond) => {
      return;
    },
    friendsList: async () => {
      return {
        friends: [getMockUser()],
      };
    },

    // Feeds
    feedsPost: async (_params: { content: string; run_id?: string; images?: FileLike[]; topics?: string[] }) => {
      return;
    },
    feedsList: async (_params: { type: "following" | "recommend" | "friends"; page?: number; size?: number }) => {
      return {
        list: [
          {
            feed_id: "FEED_MOCK_1",
            content: "今日完成10公里！",
          },
        ],
      };
    },
    feedsLike: async (_feed_id: string) => {
      return;
    },
    feedsComment: async (_feed_id: string, _body: CommentCreate) => {
      return;
    },

    // Leaderboard
    leaderboard: async (_params: { type: "weekly" | "monthly"; scope: "friends" | "city" | "global" }) => {
      return {
        list: [
          { user_id: 10001, nickname: "跑步小白", distance: 42.0 },
        ],
      };
    },

    // Challenges
    challengesList: async (_params: { status: "ongoing" | "upcoming" | "completed" }): Promise<ChallengeListResponse> => {
      return {
        challenges: [
          {
            id: 20001,
            name: "城市打卡挑战",
            status: _params.status,
            description: "一周内完成指定距离",
          },
        ],
      };
    },
    challengesJoin: async (_challenge_id: number) => {
      return;
    },

    // Badges
    badgesList: async (): Promise<BadgeItem[]> => {
      return [
        {
          id: 30001,
          name: "首跑徽章",
          icon: "https://example.com/badge.png",
          achieved: true,
        },
      ];
    },

    // Upload generic image
    uploadImage: async (_params: { file: FileLike; type: "avatar" | "feed" | "route" }) => {
      return;
    },

    // Config
    config: async () => {
      return {
        voice_packs: [],
        app_version: "1.0",
        privacy_policy_url: "https://example.com/privacy",
      };
    },

    // WebSocket
    getWebSocketUrl: (token: string) => {
      const wsBaseUrl = "wss://api.moveup.com/ws";
      return `${wsBaseUrl}?token=${encodeURIComponent(token || MOCK_TOKEN)}`;
    },
  };
}

export default createMockApiClient;

