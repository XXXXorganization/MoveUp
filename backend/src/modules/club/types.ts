export interface Club {
  id: string;
  name: string;
  description?: string;
  location?: string;
  image_url?: string;
  flag?: string;
  creator_id?: string;
  created_at: string;
  updated_at: string;
}

export interface ClubMember {
  id: string;
  club_id: string;
  user_id: string;
  role: string;
  joined_at: string;
}

export interface ClubPost {
  id: string;
  club_id: string;
  user_id: string;
  content?: string;
  run_id?: string;
  images?: string[];
  created_at: string;
}

export interface ClubComment {
  id: string;
  post_id: string;
  user_id: string;
  content: string;
  reply_to_id?: string;
  created_at: string;
}

export interface ClubPostLike {
  id: string;
  post_id: string;
  user_id: string;
  created_at: string;
}

// Android-expected response shapes
export interface ClubWithMembership {
  id: string;
  name: string;
  location?: string;
  flag: string;
  image_url?: string;
  description?: string;
  is_member: boolean;
  member_count: number;
}

export interface RunSummary {
  distance: number;
  duration: number;
  pace: string;
}

export interface PostWithAuthor {
  id: string;
  club_id: string;
  content?: string;
  run_id?: string;
  images?: string[];
  run_summary?: RunSummary;
  created_at: string;
  author: {
    id: string;
    nickname: string;
    avatar?: string;
  };
  is_liked: boolean;
  like_count: number;
  comments: CommentWithAuthor[];
}

export interface CommentWithAuthor {
  id: string;
  content: string;
  created_at: string;
  reply_to_id?: string;
  author: {
    id: string;
    nickname: string;
    avatar?: string;
  };
}
