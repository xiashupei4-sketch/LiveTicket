import { useEffect, useState } from 'react'
import { message } from 'antd'
import PageLoading from '../../components/PageLoading'
import { useAuthStore } from '../../store/authStore'
import { getCalendar, getStreak, checkinEvent } from '../../api/checkin'
import { listFollowing } from '../../api/social'
import { listOrders } from '../../api/orders'
import type { CheckinCalendar } from '../../api/checkin'
import type { FollowVO } from '../../types/social'
import type { OrderVO } from '../../types/order'
import { formatDateTime } from '../../components/EventCard'

const WEEK_HEADS = ['一', '二', '三', '四', '五', '六', '日']

export default function ProfilePage() {
  const user = useAuthStore(s => s.user)
  const [loading, setLoading] = useState(true)
  const [calendar, setCalendar] = useState<CheckinCalendar | null>(null)
  const [streak, setStreak] = useState(0)
  const [following, setFollowing] = useState<FollowVO[]>([])
  const [recentOrders, setRecentOrders] = useState<OrderVO[]>([])
  const [checkingIn, setCheckingIn] = useState<string | null>(null)

  const reloadCheckin = async () => {
    const month = new Date()
    const yyyyMM = `${month.getFullYear()}${String(month.getMonth() + 1).padStart(2, '0')}`
    const [calResp, streakResp] = await Promise.all([getCalendar(yyyyMM), getStreak()])
    setCalendar(calResp.data ?? null)
    setStreak(streakResp.data ?? 0)
  }

  useEffect(() => {
    const load = async () => {
      try {
        const month = new Date()
        const yyyyMM = `${month.getFullYear()}${String(month.getMonth() + 1).padStart(2, '0')}`
        const [calResp, streakResp, followingResp, ordersResp] = await Promise.all([
          getCalendar(yyyyMM),
          getStreak(),
          listFollowing(user?.userId ?? 0),
          listOrders({ page: 1, pageSize: 3 })
        ])
        setCalendar(calResp.data ?? null)
        setStreak(streakResp.data ?? 0)
        setFollowing(followingResp.data?.records ?? [])
        setRecentOrders(ordersResp.data?.records ?? [])
      } catch {
        message.error('个人主页数据加载失败')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [user?.userId])

  if (loading) return <PageLoading />

  const doCheckin = async (order: OrderVO) => {
    setCheckingIn(order.orderNo)
    try {
      await checkinEvent(order.eventId)
      message.success(`已为「${order.eventTitle}」打卡`)
      await reloadCheckin()
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { code?: number; message?: string } } })?.response?.data
      if (data?.code === 70001) {
        message.info('今天已经打过卡啦')
      } else {
        message.error(data?.message || '打卡失败')
      }
    } finally {
      setCheckingIn(null)
    }
  }

  const ym = calendar?.month ?? ''
  const year = Number(ym.slice(0, 4))
  const month = Number(ym.slice(4, 6))
  const daysInMonth = new Date(year, month, 0).getDate()
  const firstDay = (new Date(year, month - 1, 1).getDay() + 6) % 7
  const checked = new Set(calendar?.checkedDays ?? [])

  return (
    <div className="lt-fade">
      <div className="lt-card lt-profile-user">
        <div className="avatar">{user?.nickname?.slice(0, 1) ?? 'U'}</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 20, fontWeight: 700 }}>{user?.nickname}</div>
          <div style={{ color: 'var(--lt-text-3)', marginTop: 2 }}>@{user?.username}</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 24, fontWeight: 700 }}>{following.length}</div>
          <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>关注</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 24, fontWeight: 700 }}>{streak}</div>
          <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>连续打卡</div>
        </div>
      </div>

      <div className="lt-profile-grid">
        <div className="lt-card panel">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>
            观演打卡日历 · {ym}（已打卡 {calendar?.checkedCount ?? 0} 天）
          </div>
          <div className="lt-calendar">
            {WEEK_HEADS.map(h => <div key={h} className="cell head">{h}</div>)}
            {Array.from({ length: firstDay }).map((_, i) => <div key={`pad-${i}`} className="cell" />)}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1
              const on = checked.has(day)
              return (
                <div key={day} className={`cell${on ? ' on' : ''}`}>
                  {day}
                </div>
              )
            })}
          </div>
        </div>

        <div className="lt-card panel">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>连续打卡 & 最近观演</div>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
            <div style={{ flex: 1, background: 'var(--lt-bg)', borderRadius: 10, padding: 14, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--lt-brand)' }}>{streak}</div>
              <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>天连续打卡</div>
            </div>
            <div style={{ flex: 1, background: 'var(--lt-bg)', borderRadius: 10, padding: 14, textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--lt-success)' }}>
                {calendar?.checkedCount ?? 0}
              </div>
              <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>本月打卡</div>
            </div>
          </div>
          <div>
            {recentOrders.length === 0 ? (
              <div style={{ color: 'var(--lt-text-3)' }}>还没有观演订单，去抢一张票吧</div>
            ) : (
              recentOrders.map(o => (
                <div key={o.orderNo} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--lt-border)' }}>
                  <span>{o.eventTitle}</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ color: 'var(--lt-text-3)', fontSize: 12 }}>
                      {formatDateTime(o.eventStartTime)}
                    </span>
                    {o.status === 1 && (
                      <button
                        className="lt-btn lt-btn-secondary"
                        style={{ fontSize: 12, padding: '4px 12px' }}
                        disabled={checkingIn === o.orderNo}
                        onClick={() => doCheckin(o)}
                      >
                        {checkingIn === o.orderNo ? '打卡中…' : '观演打卡'}
                      </button>
                    )}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
