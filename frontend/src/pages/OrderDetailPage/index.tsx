import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { message } from 'antd'
import PageLoading from '../../components/PageLoading'
import EmptyState from '../../components/EmptyState'
import OrderCard from '../../components/OrderCard'
import { cancelOrder, getOrder, payOrder } from '../../api/orders'
import type { OrderVO } from '../../types/order'

export default function OrderDetailPage() {
  const { orderNo } = useParams()
  const [loading, setLoading] = useState(true)
  const [order, setOrder] = useState<OrderVO | null>(null)

  const load = async () => {
    if (!orderNo) return
    setLoading(true)
    try {
      const resp = await getOrder(orderNo)
      setOrder(resp.data)
    } catch {
      message.error('订单加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [orderNo])

  const doPay = async () => {
    if (!order) return
    try {
      const resp = await payOrder(order.orderNo)
      if (resp.code === 0) {
        message.success('支付成功')
        load()
      } else {
        message.error(resp.message || '支付失败')
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '支付失败')
    }
  }

  const doCancel = async () => {
    if (!order) return
    try {
      const resp = await cancelOrder(order.orderNo)
      if (resp.code === 0) {
        message.success('订单已取消')
        load()
      } else {
        message.error(resp.message || '取消失败')
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '取消失败')
    }
  }

  if (loading) return <PageLoading />
  if (!order) return <EmptyState text="订单不存在" />

  return (
    <div className="lt-fade" style={{ marginTop: 32 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h1 style={{ fontSize: 32, lineHeight: '40px', margin: 0 }}>订单详情</h1>
        <Link to="/orders" style={{ color: 'var(--lt-brand)' }}>返回列表</Link>
      </div>
      <div style={{ marginTop: 20 }}>
        <OrderCard
          order={order}
          onPay={doPay}
          onCancel={doCancel}
        />
      </div>
      <div className="lt-card" style={{ padding: 20, marginTop: 8 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <Info label="订单号" value={order.orderNo} />
          <Info label="订单来源" value={order.source === 1 ? '限量抢票' : '普通购买'} />
          <Info label="单价" value={`¥${Number(order.unitPrice).toFixed(2)}`} />
          <Info label="数量" value={String(order.quantity)} />
          <Info label="总金额" value={`¥${Number(order.totalAmount).toFixed(2)}`} />
          <Info label="创建时间" value={order.createdAt?.replace('T', ' ').slice(0, 19)} />
          <Info label="支付时间" value={order.paidAt ? order.paidAt.replace('T', ' ').slice(0, 19) : '-'} />
          <Info label="取消时间" value={order.cancelledAt ? order.cancelledAt.replace('T', ' ').slice(0, 19) : '-'} />
        </div>
      </div>
    </div>
  )
}

function Info({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <div style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>{label}</div>
      <div style={{ marginTop: 2 }}>{value}</div>
    </div>
  )
}
