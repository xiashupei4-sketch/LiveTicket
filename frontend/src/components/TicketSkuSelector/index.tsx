import type { TicketSkuVO } from '../../types/event'

export default function TicketSkuSelector({
  skus,
  selectedId,
  onSelect
}: {
  skus: TicketSkuVO[]
  selectedId?: number | null
  onSelect: (sku: TicketSkuVO) => void
}) {
  return (
    <div className="lt-sku-list">
      {skus.map(sku => (
        <div
          key={sku.id}
          className={`lt-sku${selectedId === sku.id ? ' selected' : ''}`}
          onClick={() => onSelect(sku)}
        >
          <div className="name">{sku.skuName}</div>
          <div className="price">¥{Number(sku.price).toFixed(0)}</div>
          <div className="stock">
            {sku.availableStock > 0 ? `剩余 ${sku.availableStock}` : '已售罄'}
          </div>
        </div>
      ))}
    </div>
  )
}
