import { useCallback, useEffect, useRef, useState } from 'react'
import { message } from 'antd'
import PostCard from '../../components/PostCard'
import PageLoading from '../../components/PageLoading'
import EmptyState from '../../components/EmptyState'
import { createPost, getFeed } from '../../api/social'
import type { PostVO } from '../../types/social'

export default function FeedPage() {
  const [loading, setLoading] = useState(true)
  const [posts, setPosts] = useState<PostVO[]>([])
  const [likedIds, setLikedIds] = useState<Set<number>>(new Set())
  const [hasNext, setHasNext] = useState(false)
  const [cursor, setCursor] = useState<{ maxTime: number | null; offset: number }>({ maxTime: null, offset: 0 })
  const [composing, setComposing] = useState('')
  const [posting, setPosting] = useState(false)
  const loadingMoreRef = useRef(false)

  const loadFirstPage = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await getFeed({ pageSize: 10 })
      const page = resp.data
      setPosts(page?.records ?? [])
      setHasNext(page?.hasNext ?? false)
      setCursor({ maxTime: page?.nextMaxTime ?? null, offset: page?.nextOffset ?? 0 })
    } catch {
      message.error('Feed 加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadFirstPage()
  }, [loadFirstPage])

  const loadMore = useCallback(async () => {
    if (loadingMoreRef.current || !hasNext) return
    loadingMoreRef.current = true
    try {
      const resp = await getFeed({ maxTime: cursor.maxTime, offset: cursor.offset, pageSize: 10 })
      const page = resp.data
      const existing = new Set(posts.map(p => p.postId))
      const incoming = (page?.records ?? []).filter(p => !existing.has(p.postId))
      setPosts(prev => [...prev, ...incoming])
      setHasNext(page?.hasNext ?? false)
      setCursor({ maxTime: page?.nextMaxTime ?? cursor.maxTime, offset: page?.nextOffset ?? 0 })
    } catch {
      message.error('加载更多失败')
    } finally {
      loadingMoreRef.current = false
    }
  }, [cursor, hasNext, posts])

  useEffect(() => {
    const onScroll = () => {
      const bottom = document.documentElement.scrollHeight - window.scrollY - window.innerHeight
      if (bottom < 300) {
        loadMore()
      }
    }
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [loadMore])

  const toggleLike = (postId: number, liked: boolean) => {
    setLikedIds(prev => {
      const next = new Set(prev)
      if (liked) {
        next.add(postId)
      } else {
        next.delete(postId)
      }
      return next
    })
    setPosts(prev => prev.map(p => (
      p.postId === postId
        ? { ...p, likeCount: p.likeCount + (liked ? 1 : -1) }
        : p
    )))
  }

  const submitPost = async () => {
    if (!composing.trim()) {
      message.error('说点什么吧')
      return
    }
    if (posting) return
    setPosting(true)
    try {
      const resp = await createPost({ content: composing.trim() })
      if (resp.code === 0 && resp.data) {
        message.success('发布成功，已推送给粉丝')
        setComposing('')
        loadFirstPage()
      } else {
        message.error(resp.message || '发布失败')
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '发布失败')
    } finally {
      setPosting(false)
    }
  }

  return (
    <div className="lt-feed lt-fade" style={{ paddingTop: 32 }}>
      <div className="lt-card lt-composer">
        <textarea
          placeholder="分享你的现场瞬间…"
          value={composing}
          onChange={e => setComposing(e.target.value)}
        />
        <div className="row">
          <span style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>发布后将实时推送给你的粉丝</span>
          <button className="lt-btn lt-btn-primary" disabled={posting} onClick={submitPost}>
            {posting ? '发布中…' : '发布动态'}
          </button>
        </div>
      </div>

      {loading ? (
        <PageLoading />
      ) : posts.length === 0 ? (
        <EmptyState text="你的 Feed 还是空的，关注更多人或发布一条动态" />
      ) : (
        posts.map(p => (
          <PostCard key={p.postId} post={p} likedIds={likedIds} onToggleLike={toggleLike} />
        ))
      )}
      {!loading && hasNext && (
        <div style={{ textAlign: 'center', padding: '12px 0 24px', color: 'var(--lt-text-3)' }}>
          下拉加载更多（maxTime={cursor.maxTime} offset={cursor.offset}）
        </div>
      )}
    </div>
  )
}
