import { Link } from 'react-router-dom'
import { Modal } from 'antd'
import type { OrderVO } from '../../types/order'
import { formatDateTime } from '../EventCard'

const STATUS = {
  0: { label: '待支付', cls: 'lt-status-pending' },
  1: { label: '已支付', cls: 'lt-status-paid' },
  2: { label: '已取消', cls: 'lt-status-cancelled' },
  3: { label: '已关闭', cls: 'lt-status-closed' }
} as const

export default function OrderCard({
  order,
  onPay,
  onCancel
}: {
  order: OrderVO
  onPay?: (order: OrderVO) => void
  onCancel?: (order: OrderVO) => void
}) {
  const status = STATUS[order.status as 0 | 1 | 2 | 3] ?? STATUS[0]

  const doCancel = () => {
    Modal.confirm({
      title: '取消订单',
      content: `确定取消订单 ${order.orderNo} 吗？取消后库存将立即恢复。`,
      okText: '取消订单',
      okButtonProps: { danger: true },
      cancelText: '再想想',
      onOk: () => onCancel?.(order)
    })
  }

  return (
    <div className="lt-card lt-order-card">
      <img className="poster" src={order.eventCoverUrl} alt={order.eventTitle} />
      <div className="mid">
        <div className="title">{order.eventTitle}</div>
        <div className="meta">{formatDateTime(order.eventStartTime)}</div>
        <div className="meta">{order.venueName}</div>
        <div className="meta">{order.skuName} × {order.quantity}</div>
        <div className="meta" style={{ marginTop: 6 }}>
          <Link to={`/orders/${order.orderNo}`} style={{ color: 'var(--lt-brand)' }}>
            订单号 {order.orderNo}
          </Link>
        </div>
      </div>
      <div className="right">
        <div className="amount">¥{Number(order.totalAmount).toFixed(2)}</div>
        <div className={`lt-status ${status.cls}`}>{status.label}</div>
        {order.status === 0 && (
          <div className="ops">
            <button className="lt-btn lt-btn-primary" onClick={() => onPay?.(order)}>模拟支付</button>
            <button className="lt-btn lt-btn-secondary" onClick={doCancel}>取消订单</button>
          </div>
        )}
        {order.status === 1 && (
          <div className="ops">
            <span style={{ fontSize: 12, color: 'var(--lt-text-3)' }}>已支付</span>
          </div>
        )}
      </div>
    </div>
  )
}
