import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { message } from 'antd'
import { getSeckillResult, seckill } from '../../api/seckill'
import type { TicketSkuVO } from '../../types/event'

type Phase = 'ready' | 'processing' | 'success' | 'failed'

const MAX_POLLING = 30

export default function SeckillPage() {
  const { ticketSkuId } = useParams()
  const location = useLocation()
  const state = (location.state ?? {}) as {
    sku?: TicketSkuVO
    eventId?: number
    eventTitle?: string
  }
  const [sku] = useState<TicketSkuVO | null>(state.sku ?? null)
  const eventId = state.eventId
  const eventTitle = state.eventTitle
  const [phase, setPhase] = useState<Phase>('ready')
  const [orderNo, setOrderNo] = useState<string | null>(null)
  const [reason, setReason] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const pollRef = useRef<{ count: number; timer?: number }>({ count: 0 })

  useEffect(() => {
    if (!sku) {
      message.info('请从演出详情页选择票档后进入抢票')
    }
  }, [sku])

  const startPolling = () => {
    pollRef.current = { count: 0 }
    const poll = async () => {
      pollRef.current.count += 1
      try {
        const resp = await getSeckillResult(ticketSkuId!)
        const result = resp.data
        if (result?.status === 'SUCCESS' && result.orderNo) {
          setOrderNo(result.orderNo)
          setPhase('success')
          return
        }
        if (result?.status === 'FAILED') {
          setReason(result.reason ?? 'ORDER_CREATE_FAILED')
          setPhase('failed')
          return
        }
      } catch {
        // 轮询失败时继续尝试
      }
      if (pollRef.current.count >= MAX_POLLING) {
        setPhase('processing')
        message.info('处理时间较长，请前往订单页查看')
        return
      }
      pollRef.current.timer = window.setTimeout(poll, 1000)
    }
    poll()
  }

  useEffect(() => () => {
    if (pollRef.current.timer) {
      window.clearTimeout(pollRef.current.timer)
    }
  }, [])

  const submitSeckill = async () => {
    if (submitting) return
    setSubmitting(true)
    try {
      const resp = await seckill(ticketSkuId!)
      if (resp.code === 0 && resp.data?.status === 'PROCESSING') {
        setPhase('processing')
        startPolling()
      } else if (resp.data?.status === 'FAILED') {
        setReason(resp.data.reason ?? 'SECKILL_FAILED')
        setPhase('failed')
      } else {
        message.error(resp.message || `抢票失败（${resp.code}）`)
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { code?: number; message?: string } } })?.response?.data
      const code = data?.code
      if (code === 40003) {
        setReason('SECKILL_OUT_OF_STOCK')
        setPhase('failed')
      } else if (code === 40004) {
        message.warning('你已经抢过该票档，请勿重复抢票')
      } else {
        message.error(data?.message || '抢票失败')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!sku) {
    return (
      <div className="lt-seckill lt-fade">
        <div className="state-card">
          <h2>未选择票档</h2>
          <div className="hint">请从演出详情页选择票档后进入抢票。</div>
          <div style={{ marginTop: 20 }}>
            <Link to="/events" className="lt-btn lt-btn-primary lt-btn-lg">浏览演出</Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="lt-seckill lt-fade">
      <div className="state-card">
        {phase === 'ready' && (
          <>
            <div style={{ fontSize: 13, color: 'var(--lt-brand)' }}>限量抢票</div>
            <h2>{eventTitle ?? `票档 #${ticketSkuId}`}</h2>
            <div className="hint">{sku.skuName} · ¥{Number(sku.price).toFixed(0)}</div>
            <div className="hint" style={{ marginTop: 4 }}>
              {sku.availableStock > 0 ? `剩余库存 ${sku.availableStock}` : '已售罄'}
            </div>
            <button
              className="lt-btn lt-btn-primary lt-btn-lg"
              style={{ marginTop: 28, width: '100%' }}
              disabled={submitting}
              onClick={submitSeckill}
            >
              {submitting ? '提交中…' : '立即抢票'}
            </button>
          </>
        )}

        {phase === 'processing' && (
          <>
            <div className="lt-spinner" />
            <h2>正在排队处理中</h2>
            <div className="hint">订单正在异步创建，请保持页面开启。</div>
            <div className="hint" style={{ marginTop: 20 }}>
              <Link to="/orders" style={{ color: 'var(--lt-brand)' }}>前往我的订单</Link>
            </div>
          </>
        )}

        {phase === 'success' && (
          <>
            <div style={{ fontSize: 56, color: 'var(--lt-success)' }}>✓</div>
            <h2>抢票成功</h2>
            <div className="hint">订单号：{orderNo}</div>
            <div style={{ marginTop: 24, display: 'flex', gap: 12, justifyContent: 'center' }}>
              <Link to="/orders" className="lt-btn lt-btn-primary lt-btn-lg">查看订单</Link>
              {eventId && <Link to={`/events/${eventId}`} className="lt-btn lt-btn-secondary lt-btn-lg">返回演出</Link>}
            </div>
          </>
        )}

        {phase === 'failed' && (
          <>
            <div style={{ fontSize: 56, color: 'var(--lt-error)' }}>✕</div>
            <h2>抢票失败</h2>
            <div className="hint">原因：{reason}</div>
            <div style={{ marginTop: 24, display: 'flex', gap: 12, justifyContent: 'center' }}>
              {eventId && <Link to={`/events/${eventId}`} className="lt-btn lt-btn-primary lt-btn-lg">重新查看演出</Link>}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
