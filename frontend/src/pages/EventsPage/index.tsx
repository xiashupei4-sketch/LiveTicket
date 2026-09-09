import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Pagination, message } from 'antd'
import EventCard from '../../components/EventCard'
import EmptyState from '../../components/EmptyState'
import PageLoading from '../../components/PageLoading'
import { listEvents } from '../../api/events'
import type { EventVO } from '../../types/event'

const CITIES = [
  { code: '', label: '全部' },
  { code: 'hangzhou', label: '杭州' },
  { code: 'shanghai', label: '上海' },
  { code: 'nanjing', label: '南京' },
  { code: 'beijing', label: '北京' },
  { code: 'chengdu', label: '成都' }
]

const CATEGORIES = [
  { code: '', label: '全部' },
  { code: 'concert', label: '演唱会' }
]

export default function EventsPage() {
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [records, setRecords] = useState<EventVO[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')
  const [cityCode, setCityCode] = useState('')
  const [category, setCategory] = useState('')
  const pageSize = 12

  const load = useCallback(
    async (targetPage: number) => {
      setLoading(true)
      try {
        const resp = await listEvents({
          page: targetPage,
          pageSize,
          keyword: keyword.trim() || undefined,
          cityCode: cityCode || undefined,
          category: category || undefined
        })
        setRecords(resp.data?.records ?? [])
        setTotal(resp.data?.total ?? 0)
        setPage(targetPage)
      } catch {
        message.error('演出列表加载失败')
      } finally {
        setLoading(false)
      }
    },
    [keyword, cityCode, category]
  )

  useEffect(() => {
    load(1)
  }, [load])

  return (
    <div className="lt-fade" style={{ marginTop: 32 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 20, flexWrap: 'wrap' }}>
        <h1 style={{ fontSize: 32, lineHeight: '40px', margin: 0 }}>发现演出</h1>
        <input
          placeholder="搜索演出 / 艺人"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && load(1)}
          style={{
            height: 40,
            width: 420,
            maxWidth: '100%',
            border: '1px solid var(--lt-border)',
            borderRadius: 'var(--lt-radius-input)',
            padding: '0 14px',
            outline: 'none',
            background: 'var(--lt-card)'
          }}
        />
      </div>

      <div style={{ display: 'flex', gap: 24, marginTop: 18, flexWrap: 'wrap' }}>
        <div>
          {CITIES.map(c => (
            <button
              key={c.code}
              onClick={() => { setCityCode(c.code) }}
              style={{
                border: 'none',
                background: cityCode === c.code ? 'var(--lt-brand-light)' : 'transparent',
                color: cityCode === c.code ? 'var(--lt-brand)' : 'var(--lt-text-2)',
                borderRadius: 8,
                padding: '4px 12px',
                cursor: 'pointer'
              }}
            >
              {c.label}
            </button>
          ))}
        </div>
        <div>
          {CATEGORIES.map(c => (
            <button
              key={c.code}
              onClick={() => { setCategory(c.code) }}
              style={{
                border: 'none',
                background: category === c.code ? 'var(--lt-brand-light)' : 'transparent',
                color: category === c.code ? 'var(--lt-brand)' : 'var(--lt-text-2)',
                borderRadius: 8,
                padding: '4px 12px',
                cursor: 'pointer'
              }}
            >
              {c.label}
            </button>
          ))}
        </div>
      </div>

      <div style={{ marginTop: 24 }}>
        {loading ? (
          <PageLoading />
        ) : records.length === 0 ? (
          <EmptyState text="没有找到匹配的演出" />
        ) : (
          <div className="lt-event-grid">
            {records.map(e => <EventCard key={e.id} event={e} />)}
          </div>
        )}
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', marginTop: 32 }}>
        <Pagination
          current={page}
          pageSize={pageSize}
          total={total}
          onChange={p => load(p)}
          hideOnSinglePage
        />
      </div>
    </div>
  )
}
