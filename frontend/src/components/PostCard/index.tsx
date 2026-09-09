import { Link } from 'react-router-dom'
import { LikeFilled, LikeOutlined } from '@ant-design/icons'
import { message } from 'antd'
import { useState } from 'react'
import type { PostVO } from '../../types/social'
import { likePost, unlikePost } from '../../api/social'
import { formatDateTime } from '../EventCard'

export default function PostCard({
  post,
  likedIds,
  onToggleLike
}: {
  post: PostVO
  likedIds: Set<number>
  onToggleLike?: (postId: number, liked: boolean) => void
}) {
  const [loading, setLoading] = useState(false)
  const liked = likedIds.has(post.postId)

  const toggleLike = async () => {
    if (loading) return
    setLoading(true)
    try {
      if (liked) {
        await unlikePost(post.postId)
        message.success('已取消点赞')
      } else {
        await likePost(post.postId)
      }
      onToggleLike?.(post.postId, !liked)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lt-card lt-post-card">
      <div className="head">
        <div className="avatar">{post.authorNickname?.slice(0, 1) ?? 'U'}</div>
        <div>
          <div className="nickname">{post.authorNickname ?? `用户${post.authorId}`}</div>
          <div className="time">{formatDateTime(post.createdAt)}</div>
        </div>
      </div>
      <div className="content">{post.content}</div>
      {post.eventId && (
        <Link className="event-mini" to={`/events/${post.eventId}`}>
          {post.eventCoverUrl && <img src={post.eventCoverUrl} alt={post.eventTitle ?? ''} />}
          <div>
            <div style={{ fontWeight: 600, fontSize: 13 }}>相关演出</div>
            <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>{post.eventTitle}</div>
          </div>
        </Link>
      )}
      <div className="ops">
        <span className={`like${liked ? ' liked' : ''}`} onClick={toggleLike}>
          {liked ? <LikeFilled /> : <LikeOutlined />}
          <span>{post.likeCount}</span>
        </span>
      </div>
    </div>
  )
}
