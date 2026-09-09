import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Tabs, message } from 'antd'
import OrderCard from '../../components/OrderCard'
import EmptyState from '../../components/EmptyState'
import PageLoading from '../../components/PageLoading'
import { cancelOrder, listOrders, payOrder } from '../../api/orders'
import type { OrderVO } from '../../types/order'

const TAB_ITEMS = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待支付' },
  { key: '1', label: '已支付' },
  { key: '2', label: '已取消' }
]

export default function OrdersPage() {
  const [loading, setLoading] = useState(true)
  const [status, setStatus] = useState<string>('all')
  const [records, setRecords] = useState<OrderVO[]>([])
  const navigate = useNavigate()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const resp = await listOrders({
        status: status === 'all' ? undefined : Number(status),
        page: 1,
        pageSize: 20
      })
      setRecords(resp.data?.records ?? [])
    } catch {
      message.error('订单列表加载失败')
    } finally {
      setLoading(false)
    }
  }, [status])

  useEffect(() => {
    load()
  }, [load])

  const doPay = async (order: OrderVO) => {
    try {
      const resp = await payOrder(order.orderNo)
      if (resp.code === 0) {
        message.success('支付成功')
        load()
      } else {
        message.error(resp.message || `支付失败（${resp.code}）`)
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '支付失败')
    }
  }

  const doCancel = async (order: OrderVO) => {
    try {
      const resp = await cancelOrder(order.orderNo)
      if (resp.code === 0) {
        message.success('订单已取消，库存已恢复')
        load()
      } else {
        message.error(resp.message || `取消失败（${resp.code}）`)
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '取消失败')
    }
  }

  return (
    <div className="lt-fade" style={{ marginTop: 32 }}>
      <h1 style={{ fontSize: 32, lineHeight: '40px', margin: 0 }}>我的订单</h1>
      <Tabs
        activeKey={status}
        items={TAB_ITEMS}
        onChange={key => setStatus(key)}
        style={{ marginTop: 8 }}
      />
      {loading ? (
        <PageLoading />
      ) : records.length === 0 ? (
        <EmptyState text="暂无相关订单" />
      ) : (
        <div style={{ marginTop: 12 }}>
          {records.map(o => (
            <OrderCard
              key={o.orderNo}
              order={o}
              onPay={doPay}
              onCancel={doCancel}
            />
          ))}
        </div>
      )}
      <div style={{ textAlign: 'center', marginTop: 8 }}>
        <button className="lt-btn lt-btn-secondary" onClick={() => navigate('/events')}>去挑选演出</button>
      </div>
    </div>
  )
}
