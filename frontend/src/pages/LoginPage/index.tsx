import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { login } from '../../api/auth'
import { useAuthStore } from '../../store/authStore'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const setAuth = useAuthStore(s => s.setAuth)
  const navigate = useNavigate()

  const submit = async () => {
    if (!username.trim() || !password) {
      message.error('请输入用户名和密码')
      return
    }
    setLoading(true)
    try {
      const resp = await login({ username: username.trim(), password })
      if (resp.code === 0 && resp.data) {
        setAuth(resp.data.token, {
          userId: resp.data.userId,
          username: resp.data.username,
          nickname: resp.data.nickname
        })
        message.success(`欢迎回来，${resp.data.nickname}`)
        navigate('/')
      } else {
        message.error(resp.message || '登录失败')
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string } } })?.response?.data
      message.error(data?.message || '登录失败')
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
          <h1>把现场留给热爱。</h1>
          <div className="sub">发现演出、抢票、记录每一次值得记住的现场。</div>
        </div>
        <div style={{ display: 'flex', gap: 14 }}>
          {['/covers/xu-anbo-main.jpg', '/covers/xue-wsz-bj.jpg', '/covers/xu-hxzy-main.jpg'].map((src, i) => (
            <img
              key={src}
              src={src}
              alt=""
              style={{
                width: 150,
                borderRadius: 14,
                boxShadow: '0 18px 40px rgba(17,19,24,0.25)',
                transform: `translateY(${i * 14}px) rotate(${(i - 1) * 4}deg)`
              }}
            />
          ))}
        </div>
      </div>
      <div className="form-side">
        <div className="lt-auth-card">
          <h2>登录</h2>
          <div className="field">
            <label>用户名</label>
            <input value={username} onChange={e => setUsername(e.target.value)} placeholder="demo01" />
          </div>
          <div className="field">
            <label>密码</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && submit()}
              placeholder="123456"
            />
          </div>
          <button className="lt-btn lt-btn-primary submit" disabled={loading} onClick={submit}>
            {loading ? '登录中…' : '登录'}
          </button>
          <div className="alt">
            还没有账号？<Link to="/register">立即注册</Link>
          </div>
          <div className="demo-hint">演示账号：demo01 / 123456</div>
        </div>
      </div>
    </div>
  )
}
