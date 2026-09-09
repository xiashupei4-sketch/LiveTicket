export default function EmptyState({ text = '这里还什么都没有' }: { text?: string }) {
  return (
    <div
      style={{
        padding: '64px 0',
        textAlign: 'center',
        color: 'var(--lt-text-3)'
      }}
    >
      <div style={{ fontSize: 40, marginBottom: 12 }}>🎟️</div>
      <div>{text}</div>
    </div>
  )
}
