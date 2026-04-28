/* eslint-disable @typescript-eslint/no-explicit-any */

// Moveup OpenAPI (docs/api.yaml) based API client.
// This file is framework-agnostic: uses global `fetch` and FormData for multipart.

// Android 模拟器访问本机服务时，需要把 localhost 换成 10.0.2.2
// 后端 mock 服务默认跑在 http://localhost:3000/v1
export const DEFAULT_BASE_URL = "http://10.0.2.2:3000/v1";
export const DEFAULT_WS_BASE_URL = "ws://10.0.2.2:3000/ws";

export type StandardResponse<T = unknown> = {
  code: number;
  message: string;
  data?: T;
};

export class ApiError extends Error {
  public httpStatus?: number;
  public apiCode?: number;
  public apiMessage?: string;
  public apiData?: unknown;

  constructor(
    message: string,
    opts?: {
      httpStatus?: number;
      apiCode?: number;
      apiMessage?: string;
      apiData?: unknown;
    }
  ) {
    super(message);
    this.name = "ApiError";
    this.httpStatus = opts?.httpStatus;
    this.apiCode = opts?.apiCode;
    this.apiMessage = opts?.apiMessage;
    this.apiData = opts?.apiData;
  }
}

export type FileLike = any; // React Native uses { uri, type, name } and web uses Blob/File.

export type UserProfile = {
  id: number;
  nickname: string;
  avatar?: string;
  gender?: number; // 1=male,2=female,0=unknown
  birthday?: string; // date (YYYY-MM-DD)
  height?: number;
  weight?: number;
  total_distance?: number;
  total_time?: number;
  total_runs?: number;
  level?: number;
  preferences?: {
    target_distance?: number;
    remind_time?: string;
    voice_frequency?: string;
  };
};

export type TokenData = {
  token: string;
  expires_in: number;
  user: UserProfile;
};

export type RunStartRequest = {
  run_type: "outdoor" | "indoor";
  target_distance?: number;
  target_time?: number;
};

export type RunStartResponse = {
  run_id: string;
  start_time: string; // date-time
};

export type RunPointItem = {
  lat: number;
  lng: number;
  altitude?: number;
  timestamp: string; // date-time
  speed?: number;
  heart_rate?: number;
};

export type RunPointsRequest = {
  points: RunPointItem[];
};

export type FinishRunRequest = {
  feelings: number; // 1..5
  notes?: string;
};

export type RunListItem = {
  run_id: string;
  date?: string;
  distance?: number;
  duration?: number;
  pace_avg?: string;
  calories?: number;
  route_image?: string;
};

export type RunListResponse = {
  total?: number;
  list?: RunListItem[];
};

export type StatsResponse = {
  total_distance?: number;
  total_duration?: number;
  total_calories?: number;
  avg_pace?: string;
  runs_count?: number;
  daily_data?: Array<{
    date?: string;
    distance?: number;
  }>;
};

export type RouteCreateRequest = {
  name: string;
  points: RunPointItem[];
  distance: number;
  city: string;
};

export type FriendRequest = {
  user_id: number;
  message?: string;
};

export type FriendRespond = {
  request_id: number;
  action: "accept" | "reject";
};

export type CommentCreate = {
  content: string;
};

export type SearchUsersResponse = {
  users: UserProfile[];
};

export type ChallengeListResponse = {
  challenges: Array<{
    id: number;
    name?: string;
    status?: string;
    description?: string;
  }>;
};

export type BadgeItem = {
  id: number;
  name?: string;
  icon?: string;
  achieved?: boolean;
};

export type ApiTokenSetter = (token: string | null) => void;

export type ApiClient = ReturnType<typeof createApiClient>;

function toQueryString(query?: Record<string, unknown>): string {
  if (!query) return "";
  const usp = new URLSearchParams();
  for (const [k, v] of Object.entries(query)) {
    if (v === undefined || v === null) continue;
    if (Array.isArray(v)) {
      for (const item of v) {
        if (item === undefined || item === null) continue;
        usp.append(k, String(item));
      }
    } else {
      usp.set(k, String(v));
    }
  }
  const s = usp.toString();
  return s ? `?${s}` : "";
}

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

function buildAuthHeaders() {
  if (!authToken) return {};
  return { Authorization: `Bearer ${authToken}` };
}

function getWebSocketUrl(token: string, wsBaseUrl: string = DEFAULT_WS_BASE_URL) {
  const encoded = encodeURIComponent(token);
  return `${wsBaseUrl}?token=${encoded}`;
}

async function parseJsonSafely(res: Response) {
  try {
    return (await res.json()) as any;
  } catch {
    return null;
  }
}

async function requestData<T>(args: {
  baseUrl: string;
  path: string;
  method: string;
  query?: Record<string, unknown>;
  headers?: Record<string, string>;
  body?: any;
  formData?: FormData;
  fetchImpl?: typeof fetch;
}): Promise<T> {
  const { baseUrl, path, method, query, headers, body, formData, fetchImpl } = args;
  const url = `${baseUrl}${path}${toQueryString(query)}`;
  const f = fetchImpl ?? fetch;

  const mergedHeaders: Record<string, string> = {
    ...buildAuthHeaders(),
    ...(headers ?? {}),
  };

  let reqBody: any = undefined;
  if (formData) {
    reqBody = formData;
    // Important: do NOT set Content-Type for multipart; browser/fetch will add boundary.
  } else if (body !== undefined) {
    reqBody = JSON.stringify(body);
    mergedHeaders["Content-Type"] = mergedHeaders["Content-Type"] ?? "application/json";
  }

  const res = await f(url, {
    method,
    headers: mergedHeaders,
    body: reqBody,
  });

  const json = await parseJsonSafely(res);
  if (!res.ok) {
    throw new ApiError(`HTTP ${res.status}`, {
      httpStatus: res.status,
      apiCode: json?.code,
      apiMessage: json?.message,
      apiData: json?.data,
    });
  }

  const apiCode = json?.code;
  const apiMessage = json?.message;
  const apiData = json?.data as T | undefined;

  if (apiCode !== 200) {
    throw new ApiError(apiMessage ?? "API Error", {
      apiCode,
      apiMessage,
      apiData,
    });
  }

  return apiData as T;
}

export function createApiClient(options?: {
  baseUrl?: string;
  fetchImpl?: typeof fetch;
}) {
  const baseUrl = options?.baseUrl ?? DEFAULT_BASE_URL;
  const fetchImpl = options?.fetchImpl;

  return {
    // Auth
    authCode: async (params: { phone: string; type: "login" | "register" | "reset" }) => {
      await requestData<void>({
        baseUrl,
        path: "/auth/code",
        method: "POST",
        body: params,
        fetchImpl,
      });
    },
    authLogin: async (params: { phone: string; code: string }) => {
      return requestData<TokenData>({
        baseUrl,
        path: "/auth/login",
        method: "POST",
        body: params,
        fetchImpl,
      });
    },

    // User
    getProfile: async () => {
      return requestData<UserProfile>({
        baseUrl,
        path: "/user/profile",
        method: "GET",
        fetchImpl,
      });
    },
    updateProfile: async (patch: {
      nickname?: string;
      height?: number;
      weight?: number;
      birthday?: string; // date
    }) => {
      await requestData<void>({
        baseUrl,
        path: "/user/profile",
        method: "PUT",
        body: patch,
        fetchImpl,
      });
    },
    uploadAvatar: async (file: FileLike) => {
      const formData = new FormData();
      formData.append("file", file);
      await requestData<void>({
        baseUrl,
        path: "/user/avatar",
        method: "POST",
        formData,
        fetchImpl,
      });
    },

    // Runs
    runsStart: async (body: RunStartRequest) => {
      return requestData<RunStartResponse>({
        baseUrl,
        path: "/runs/start",
        method: "POST",
        body,
        fetchImpl,
      });
    },
    runsUploadPoints: async (run_id: string, body: RunPointsRequest) => {
      await requestData<void>({
        baseUrl,
        path: `/runs/${encodeURIComponent(run_id)}/points`,
        method: "POST",
        body,
        fetchImpl,
      });
    },
    runsFinish: async (run_id: string, body: FinishRunRequest) => {
      await requestData<void>({
        baseUrl,
        path: `/runs/${encodeURIComponent(run_id)}/finish`,
        method: "POST",
        body,
        fetchImpl,
      });
    },
    runsList: async (params: {
      page?: number;
      size?: number;
      start_date?: string; // date
      end_date?: string; // date
    }) => {
      return requestData<RunListResponse>({
        baseUrl,
        path: "/runs",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },
    runsDetail: async (run_id: string) => {
      return requestData<unknown>({
        baseUrl,
        path: `/runs/${encodeURIComponent(run_id)}`,
        method: "GET",
        fetchImpl,
      });
    },

    // Stats
    stats: async (params: { type: "week" | "month" | "year"; date: string }) => {
      return requestData<StatsResponse>({
        baseUrl,
        path: "/stats",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },

    // Routes
    saveRoute: async (body: RouteCreateRequest) => {
      await requestData<void>({
        baseUrl,
        path: "/routes",
        method: "POST",
        body,
        fetchImpl,
      });
    },
    routesRecommended: async (params: { lat: number; lng: number; radius?: number }) => {
      return requestData<unknown>({
        baseUrl,
        path: "/routes/recommended",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },

    // Users
    searchUsers: async (keyword: string) => {
      return requestData<SearchUsersResponse>({
        baseUrl,
        path: "/users/search",
        method: "GET",
        query: { keyword },
        fetchImpl,
      });
    },

    // Friends
    friendsRequest: async (body: FriendRequest) => {
      await requestData<void>({
        baseUrl,
        path: "/friends/request",
        method: "POST",
        body,
        fetchImpl,
      });
    },
    friendsRespond: async (body: FriendRespond) => {
      await requestData<void>({
        baseUrl,
        path: "/friends/respond",
        method: "POST",
        body,
        fetchImpl,
      });
    },
    friendsList: async () => {
      return requestData<unknown>({
        baseUrl,
        path: "/friends",
        method: "GET",
        fetchImpl,
      });
    },

    // Feeds
    feedsPost: async (params: {
      content: string;
      run_id?: string;
      images?: FileLike[];
      topics?: string[];
    }) => {
      const formData = new FormData();
      formData.append("content", params.content);
      if (params.run_id) formData.append("run_id", params.run_id);
      (params.topics ?? []).forEach((t) => formData.append("topics", t));
      (params.images ?? []).forEach((img) => formData.append("images", img));

      await requestData<void>({
        baseUrl,
        path: "/feeds",
        method: "POST",
        formData,
        fetchImpl,
      });
    },
    feedsList: async (params: { type: "following" | "recommend" | "friends"; page?: number; size?: number }) => {
      return requestData<unknown>({
        baseUrl,
        path: "/feeds",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },
    feedsLike: async (feed_id: string) => {
      await requestData<void>({
        baseUrl,
        path: `/feeds/${encodeURIComponent(feed_id)}/like`,
        method: "POST",
        fetchImpl,
      });
    },
    feedsComment: async (feed_id: string, body: CommentCreate) => {
      await requestData<void>({
        baseUrl,
        path: `/feeds/${encodeURIComponent(feed_id)}/comment`,
        method: "POST",
        body,
        fetchImpl,
      });
    },

    // Leaderboard
    leaderboard: async (params: { type: "weekly" | "monthly"; scope: "friends" | "city" | "global" }) => {
      return requestData<unknown>({
        baseUrl,
        path: "/leaderboard",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },

    // Challenges
    challengesList: async (params: { status: "ongoing" | "upcoming" | "completed" }) => {
      return requestData<ChallengeListResponse>({
        baseUrl,
        path: "/challenges",
        method: "GET",
        query: params as any,
        fetchImpl,
      });
    },
    challengesJoin: async (challenge_id: number) => {
      await requestData<void>({
        baseUrl,
        path: `/challenges/${encodeURIComponent(String(challenge_id))}/join`,
        method: "POST",
        fetchImpl,
      });
    },

    // Badges
    badgesList: async () => {
      return requestData<BadgeItem[]>({
        baseUrl,
        path: "/badges",
        method: "GET",
        fetchImpl,
      });
    },

    // Upload generic image
    uploadImage: async (params: { file: FileLike; type: "avatar" | "feed" | "route" }) => {
      const formData = new FormData();
      formData.append("file", params.file);
      formData.append("type", params.type);
      await requestData<void>({
        baseUrl,
        path: "/upload/image",
        method: "POST",
        formData,
        fetchImpl,
      });
    },

    // Config
    config: async () => {
      return requestData<unknown>({
        baseUrl,
        path: "/config",
        method: "GET",
        fetchImpl,
      });
    },

    // WebSocket
    getWebSocketUrl: (token: string) => getWebSocketUrl(token),
  };
}

export default createApiClient;

