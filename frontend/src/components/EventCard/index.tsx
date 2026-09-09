import { Link } from 'react-router-dom'
import type { EventVO } from '../../types/event'

const CATEGORY_LABELS: Record<string, string> = {
  concert: '演唱会',
  livehouse: 'Livehouse',
  theatre: '话剧',
  exhibition: '展览'
}

const CITY_LABELS: Record<string, string> = {
  hangzhou: '杭州',
  nanjing: '南京',
  shanghai: '上海',
  beijing: '北京'
}

export function formatDateTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

export default function EventCard({ event }: { event: EventVO }) {
  return (
    <Link className="lt-event-card" to={`/events/${event.id}`}>
      <img className="poster" src={event.coverUrl} alt={event.title} loading="lazy" />
      <div className="info">
        <span className="category-tag">{CATEGORY_LABELS[event.category] ?? event.category}</span>
        <div className="title">{event.title}</div>
        <div className="meta">{event.artist}</div>
        <div className="meta">{formatDateTime(event.startTime)}</div>
        <div className="meta">{CITY_LABELS[event.cityCode] ?? event.cityCode} · {event.venueName}</div>
        <div className="price">
          {event.minPrice != null ? `¥${Number(event.minPrice).toFixed(0)} 起` : '即将开售'}
        </div>
      </div>
    </Link>
  )
}
