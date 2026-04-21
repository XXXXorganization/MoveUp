// src/modules/social/types.ts

export interface Friendship {
  userId: string;
  friendId: string;
  status: 0 | 1 | 2; // 0-待确认 1-已确认 2-已拒绝
  createdAt: Date;
  updatedAt: Date;
}

export interface FriendInfo {
  userId: string;
  nickname: string;
  avatar?: string;
  status: 0 | 1 | 2;
  createdAt: Date;
}

export interface Post {
  id: string;
  userId: string;
  content?: string;
  images?: string[];
  sportRecordId?: string;
  location?: string;
  tags?: string[];
  likeCount: number;
  commentCount: number;
  createdAt: Date;
}

export interface PostDetail extends Post {
  author: {
    id: string;
    nickname: string;
    avatar?: string;
  };
  isLiked?: boolean;
}

export interface CreatePostRequest {
  content?: string;
  images?: string[];
  sportRecordId?: string;
  location?: string;
  tags?: string[];
}

export interface Comment {
  id: string;
  userId: string;
  postId: string;
  parentId?: string;
  content: string;
  likeCount: number;
  createdAt: Date;
}

export interface CommentDetail extends Comment {
  author: {
    id: string;
    nickname: string;
    avatar?: string;
  };
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  nickname: string;
  avatar?: string;
  totalDistance: number; // 米
  runCount: number;
}

export type LeaderboardType = 'weekly' | 'monthly';
export type LeaderboardScope = 'friends' | 'global';
