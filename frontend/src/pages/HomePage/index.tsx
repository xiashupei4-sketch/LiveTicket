import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { message } from 'antd'
import EventCard from '../../components/EventCard'
import EmptyState from '../../components/EmptyState'
import PageLoading from '../../components/PageLoading'
import PostCard from '../../components/PostCard'
import { listEvents, listHotEvents, listNearbyEvents } from '../../api/events'
import { getFeed } from '../../api/social'
import type { EventVO, NearbyEventVO } from '../../types/event'
import type { PostVO } from '../../types/social'

export default function HomePage() {
  const [loading, setLoading] = useState(true)
  const [hot, setHot] = useState<EventVO[]>([])
  const [week, setWeek] = useState<EventVO[]>([])
  const [nearby, setNearby] = useState<NearbyEventVO[]>([])
  const [posts, setPosts] = useState<PostVO[]>([])

  useEffect(() => {
    const load = async () => {
      try {
        const [hotResp, weekResp] = await Promise.all([
          listHotEvents(4),
          listEvents({ page: 1, pageSize: 4 })
        ])
        setHot(hotResp.data ?? [])
        setWeek(weekResp.data?.records ?? [])

        const nearbyResp = await listNearbyEvents({
          longitude: 120.21201,
          latitude: 30.2084,
          radiusKm: 20,
          pageSize: 4
        })
        setNearby(nearbyResp.data ?? [])

        const feedResp = await getFeed({ pageSize: 3 })
        setPosts(feedResp.data?.records ?? [])
      } catch {
        message.error('首页数据加载失败')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div className="lt-fade">
      <section className="lt-hero">
        <div className="left">
          <span className="tag">LIVE NOW</span>
          <h1>
            下一场现场，
            <br />
            从这里开始。
          </h1>
          <div className="desc">发现演唱会、话剧、展览与城市现场，抢到属于你的那一张票。</div>
          <div className="actions">
            <Link to="/events" className="lt-btn lt-btn-lg lt-btn-white">浏览全部演出</Link>
            <Link to="/nearby" className="lt-btn lt-btn-lg lt-btn-outline">查看附近</Link>
          </div>
        </div>
        <div className="right">
          <img src="/covers/xu-anbo-main.jpg" alt="" style={{ top: 80, right: 210, transform: 'rotate(-6deg)' }} />
          <img src="/covers/xue-wsz-main.jpg" alt="" style={{ top: 60, right: 40, transform: 'rotate(4deg)' }} />
          <img src="/covers/xue-wsz-hz.jpg" alt="" style={{ top: 230, right: 120, transform: 'rotate(-2deg)' }} />
        </div>
      </section>

      {loading ? (
        <PageLoading />
      ) : (
        <>
          <section className="lt-section">
            <div className="lt-section-title">热门演出</div>
            {hot.length === 0 ? <EmptyState text="暂无热门演出" /> : (
              <div className="lt-event-grid">
                {hot.map(e => <EventCard key={e.id} event={e} />)}
              </div>
            )}
          </section>

          <section className="lt-section">
            <div className="lt-section-title">本周精选</div>
            {week.length === 0 ? <EmptyState text="暂无演出" /> : (
              <div className="lt-event-grid">
                {week.map(e => <EventCard key={e.id} event={e} />)}
              </div>
            )}
          </section>

          <section className="lt-section">
            <div className="lt-section-title">附近演出</div>
            {nearby.length === 0 ? <EmptyState text="附近暂无演出" /> : (
              <div className="lt-event-grid">
                {nearby.map(e => <EventCard key={e.id} event={e} />)}
              </div>
            )}
          </section>

          <section className="lt-section">
            <div className="lt-section-title">社区动态预览</div>
            {posts.length === 0 ? <EmptyState text="暂无动态，去 Feed 发布第一条吧" /> : (
              posts.map(p => <PostCard key={p.postId} post={p} likedIds={new Set()} />)
            )}
          </section>
        </>
      )}
    </div>
  )
}
