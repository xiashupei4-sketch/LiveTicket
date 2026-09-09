import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Modal, message } from 'antd'
import TicketSkuSelector from '../../components/TicketSkuSelector'
import PageLoading from '../../components/PageLoading'
import EmptyState from '../../components/EmptyState'
import { getEventDetail } from '../../api/events'
import { listEventTickets } from '../../api/tickets'
import { getEventUv } from '../../api/statistics'
import { createOrder } from '../../api/orders'
import type { EventDetailVO, TicketSkuVO } from '../../types/event'

const CATEGORY_LABELS: Record<string, string> = {
  concert: '演唱会',
  livehouse: 'Livehouse',
  theatre: '话剧',
  exhibition: '展览'
}

export default function EventDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [event, setEvent] = useState<EventDetailVO | null>(null)
  const [skus, setSkus] = useState<TicketSkuVO[]>([])
  const [selectedSku, setSelectedSku] = useState<TicketSkuVO | null>(null)
  const [uv, setUv] = useState<number | null>(null)
  const [buying, setBuying] = useState(false)

  useEffect(() => {
    const load = async () => {
      if (!id) return
      setLoading(true)
      try {
        const [detailResp, skuResp, uvResp] = await Promise.all([
          getEventDetail(id),
          listEventTickets(id),
          getEventUv(id)
        ])
        if (detailResp.code === 0 && detailResp.data) {
          setEvent(detailResp.data)
        } else {
          message.error(detailResp.message || '演出不存在')
        }
        setSkus(skuResp.data ?? [])
        if (skuResp.data && skuResp.data.length > 0) {
          setSelectedSku(skuResp.data[0])
        }
        setUv(uvResp.data?.uv ?? null)
      } catch {
        message.error('演出详情加载失败')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  const buyNormal = async () => {
    if (!selectedSku || !event) {
      message.error('请先选择票档')
      return
    }
    if (selectedSku.availableStock <= 0) {
      message.error('该票档已售罄')
      return
    }
    setBuying(true)
    try {
      const resp = await createOrder({ ticketSkuId: selectedSku.id, quantity: 1 })
      if (resp.code === 0 && resp.data) {
        message.success(`下单成功，订单号 ${resp.data.orderNo}`)
        Modal.confirm({
          title: '下单成功',
          content: `订单号 ${resp.data.orderNo}，已创建待支付订单。`,
          okText: '去支付',
          cancelText: '稍后再说',
          onOk: () => navigate('/orders')
        })
      } else {
        message.error(resp.message || `下单失败（${resp.code}）`)
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '下单失败')
    } finally {
      setBuying(false)
    }
  }

  const goSeckill = () => {
    if (!selectedSku || !event) {
      message.error('请先选择票档')
      return
    }
    navigate(`/seckill/${selectedSku.id}`, {
      state: { sku: selectedSku, eventId: event.id, eventTitle: event.title }
    })
  }

  if (loading) return <PageLoading />
  if (!event) return <EmptyState text="演出不存在或已下线" />

  return (
    <div className="lt-fade">
      <div className="lt-detail-top">
        <img className="poster" src={event.coverUrl} alt={event.title} />
        <div className="info">
          <span className="category-tag" style={{
            color: 'var(--lt-brand)', background: 'var(--lt-brand-light)',
            borderRadius: 4, padding: '0 8px', fontSize: 12
          }}>
            {CATEGORY_LABELS[event.category] ?? event.category}
          </span>
          <h1>{event.title}</h1>
          <div className="artist">{event.artist}</div>
          <div className="row">
            <span className="label">时间</span>
            <span>{event.startTime?.replace('T', ' ').slice(0, 16)}</span>
          </div>
          <div className="row">
            <span className="label">场馆</span>
            <span>{event.venueName}</span>
          </div>
          <div className="row">
            <span className="label">地址</span>
            <span>{event.address}</span>
          </div>
          <div className="row">
            <span className="label">热度</span>
            <span>{event.heatScore}{uv != null ? ` · 今日 UV ${uv}` : ''}</span>
          </div>
          <p style={{ color: 'var(--lt-text-2)', marginTop: 20, lineHeight: '24px' }}>
            {event.description}
          </p>
        </div>
      </div>

      <div className="lt-section">
        <div className="lt-section-title">选择票档</div>
        <TicketSkuSelector skus={skus} selectedId={selectedSku?.id} onSelect={setSelectedSku} />
        <div className="lt-detail-actions">
          <button
            className="lt-btn lt-btn-secondary lt-btn-lg"
            disabled={buying}
            onClick={buyNormal}
          >
            普通购买
          </button>
          <button className="lt-btn lt-btn-primary lt-btn-lg" onClick={goSeckill}>
            立即抢票
          </button>
        </div>
      </div>
    </div>
  )
}
