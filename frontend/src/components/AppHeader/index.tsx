import { NavLink, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { Dropdown } from 'antd'
import { UserOutlined, LogoutOutlined, SearchOutlined } from '@ant-design/icons'
import { useAuthStore } from '../../store/authStore'

export default function AppHeader() {
  const user = useAuthStore(s => s.user)
  const logout = useAuthStore(s => s.logout)
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')

  const onSearch = () => {
    navigate(`/events${keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''}`)
  }

  return (
    <header className="lt-header">
      <div className="lt-header-inner">
        <a className="lt-logo" href="/">
          <img src="/logo.svg" alt="LiveTicket" style={{ width: 28, height: 28 }} />
          <span>LiveTicket</span>
        </a>
        <nav className="lt-nav">
          <NavLink to="/" end>首页</NavLink>
          <NavLink to="/events">演出</NavLink>
          <NavLink to="/nearby">附近</NavLink>
          <NavLink to="/feed">动态</NavLink>
          <NavLink to="/orders">我的订单</NavLink>
        </nav>
        <div className="lt-header-actions">
          <input
            aria-label="搜索演出"
            placeholder="搜索演出"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && onSearch()}
            style={{
              height: 36,
              width: 180,
              border: '1px solid var(--lt-border)',
              borderRadius: 'var(--lt-radius-input)',
              padding: '0 12px',
              outline: 'none'
            }}
          />
          <button className="lt-btn lt-btn-secondary" onClick={onSearch} aria-label="搜索">
            <SearchOutlined />
          </button>
          <Dropdown
            menu={{
              items: [
                { key: 'profile', icon: <UserOutlined />, label: '个人主页', onClick: () => navigate('/profile') },
                { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: () => { logout(); navigate('/login') } }
              ]
            }}
          >
            <div
              className="avatar"
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                background: 'var(--lt-brand-light)',
                color: 'var(--lt-brand)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              {user?.nickname?.slice(0, 1) ?? 'U'}
            </div>
          </Dropdown>
        </div>
      </div>
    </header>
  )
}
