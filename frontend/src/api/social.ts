import http from './http'
import type { ApiResponse } from '../types/api'
import type { FeedPageVO, FollowVO, PostVO } from '../types/social'

export function followUser(userId: number): Promise<ApiResponse<null>> {
  return http.post(`/users/${userId}/follow`) as Promise<ApiResponse<null>>
}

export function unfollowUser(userId: number): Promise<ApiResponse<null>> {
  return http.delete(`/users/${userId}/follow`) as Promise<ApiResponse<null>>
}

export function listFollowers(userId: number): Promise<ApiResponse<{ records: FollowVO[] }>> {
  return http.get(`/users/${userId}/followers`) as Promise<ApiResponse<{ records: FollowVO[] }>>
}

export function listFollowing(userId: number): Promise<ApiResponse<{ records: FollowVO[] }>> {
  return http.get(`/users/${userId}/following`) as Promise<ApiResponse<{ records: FollowVO[] }>>
}

export function createPost(params: { content: string; eventId?: number | null; imageUrl?: string | null }): Promise<ApiResponse<PostVO>> {
  return http.post('/posts', params) as Promise<ApiResponse<PostVO>>
}

export function likePost(postId: number): Promise<ApiResponse<null>> {
  return http.post(`/posts/${postId}/like`) as Promise<ApiResponse<null>>
}

export function unlikePost(postId: number): Promise<ApiResponse<null>> {
  return http.delete(`/posts/${postId}/like`) as Promise<ApiResponse<null>>
}

export function getFeed(params: {
  maxTime?: number | null
  offset?: number | null
  pageSize?: number
}): Promise<ApiResponse<FeedPageVO>> {
  return http.get('/feed', { params }) as Promise<ApiResponse<FeedPageVO>>
}
