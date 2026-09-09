import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { register } from '../../api/auth'

export default function RegisterPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const submit = async () => {
    if (username.trim().length < 3) {
      message.error('用户名至少 3 个字符')
      return
    }
    if (password.length < 6) {
      message.error('密码至少 6 位')
      return
    }
    if (!nickname.trim()) {
      message.error('请输入昵称')
      return
    }
    setLoading(true)
    try {
      const resp = await register({ username: username.trim(), password, nickname: nickname.trim() })
      if (resp.code === 0) {
        message.success('注册成功，请登录')
        navigate('/login')
      } else {
        message.error(resp.message || '注册失败')
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lt-auth lt-fade">
      <div className="brand-side">
        <div>
          <img src="/logo.svg" alt="LiveTicket" style={{ width: 40, height: 40 }} />
        </div>
        <div>
          <h1>下一场现场，<br />从这里开始。</h1>
          <div className="sub">注册 LiveTicket，抢到属于你的那一张票。</div>
        </div>
        <div />
      </div>
      <div className="form-side">
        <div className="lt-auth-card">
          <h2>注册</h2>
          <div className="field">
            <label>用户名</label>
            <input value={username} onChange={e => setUsername(e.target.value)} placeholder="3-32 个字符" />
          </div>
          <div className="field">
            <label>密码</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="至少 6 位"
            />
          </div>
          <div className="field">
            <label>昵称</label>
            <input value={nickname} onChange={e => setNickname(e.target.value)} placeholder="你希望被怎样称呼" />
          </div>
          <button className="lt-btn lt-btn-primary submit" disabled={loading} onClick={submit}>
            {loading ? '注册中…' : '注册'}
          </button>
          <div className="alt">
            已有账号？<Link to="/login">去登录</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
