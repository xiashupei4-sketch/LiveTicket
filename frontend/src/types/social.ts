export interface PostVO {
  postId: number
  content: string
  imageUrl?: string | null
  likeCount: number
  createdAt: string
  authorId: number
  authorNickname?: string
  authorAvatarUrl?: string | null
  eventId?: number | null
  eventTitle?: string | null
  eventCoverUrl?: string | null
}

export interface FeedPageVO {
  records: PostVO[]
  nextMaxTime?: number | null
  nextOffset?: number | null
  hasNext: boolean
  pageSize: number
}

export interface FollowVO {
  userId: number
  username: string
  nickname: string
  avatarUrl?: string | null
  followedAt: string
}
