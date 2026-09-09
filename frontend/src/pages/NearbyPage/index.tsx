import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { message } from 'antd'
import PageLoading from '../../components/PageLoading'
import EmptyState from '../../components/EmptyState'
import { listNearbyEvents } from '../../api/events'
import type { NearbyEventVO } from '../../types/event'
import { formatDateTime } from '../../components/EventCard'

const DEFAULT_LNG = 120.21201
const DEFAULT_LAT = 30.2084

export default function NearbyPage() {
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState<NearbyEventVO[] | null>(null)
  const [longitude, setLongitude] = useState(String(DEFAULT_LNG))
  const [latitude, setLatitude] = useState(String(DEFAULT_LAT))
  const [radiusKm, setRadiusKm] = useState('10')

  const load = useCallback(async () => {
    const lng = Number(longitude)
    const lat = Number(latitude)
    if (Number.isNaN(lng) || Number.isNaN(lat)) {
      message.error('请输入有效的经纬度')
      return
    }
    setLoading(true)
    try {
      const resp = await listNearbyEvents({
        longitude: lng,
        latitude: lat,
        radiusKm: Number(radiusKm),
        pageSize: 20
      })
      setRecords(resp.data ?? [])
    } catch {
      message.error('附近演出查询失败')
    } finally {
      setLoading(false)
    }
  }, [longitude, latitude, radiusKm])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="lt-fade" style={{ marginTop: 32 }}>
      <h1 style={{ fontSize: 32, lineHeight: '40px', margin: 0 }}>附近演出</h1>
      <div style={{ color: 'var(--lt-text-2)', marginTop: 8 }}>发现城市里正在发生的现场</div>

      <div className="lt-card lt-nearby-filter">
        <div className="field">
          <label>经度</label>
          <input value={longitude} onChange={e => setLongitude(e.target.value)} />
        </div>
        <div className="field">
          <label>纬度</label>
          <input value={latitude} onChange={e => setLatitude(e.target.value)} />
        </div>
        <div className="field">
          <label>半径</label>
          <select value={radiusKm} onChange={e => setRadiusKm(e.target.value)}>
            <option value="3">3km</option>
            <option value="5">5km</option>
            <option value="10">10km</option>
            <option value="20">20km</option>
          </select>
        </div>
        <button className="lt-btn lt-btn-primary" disabled={loading} onClick={load}>
          {loading ? '查询中…' : '查询'}
        </button>
      </div>

      <div style={{ marginTop: 20 }}>
        {loading ? (
          <PageLoading />
        ) : records === null ? (
          <EmptyState text="输入坐标开始探索" />
        ) : records.length === 0 ? (
          <EmptyState text="该范围内暂无在售演出，试试扩大半径" />
        ) : (
          records.map(e => (
            <Link className="lt-card lt-nearby-item" key={e.id} to={`/events/${e.id}`}>
              <img src={e.coverUrl} alt={e.title} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600 }}>{e.title}</div>
                <div style={{ fontSize: 12, color: 'var(--lt-text-3)', marginTop: 2 }}>
                  {e.venueName} · {formatDateTime(e.startTime)}
                </div>
              </div>
              <div className="distance">{e.distanceKm.toFixed(1)} km</div>
            </Link>
          ))
        )}
      </div>
    </div>
  )
}
