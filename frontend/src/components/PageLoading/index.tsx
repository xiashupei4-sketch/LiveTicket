export default function PageLoading({ text = '加载中…' }: { text?: string }) {
  return (
    <div style={{ padding: '80px 0', textAlign: 'center' }}>
      <div className="lt-spinner" />
      <div style={{ marginTop: 16, color: 'var(--lt-text-2)' }}>{text}</div>
    </div>
  )
}
