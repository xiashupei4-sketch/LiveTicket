import { Outlet, useNavigate } from 'react-router-dom'
import AppHeader from '../components/AppHeader'
import { useEffect } from 'react'
import { useAuthStore } from '../store/authStore'

export default function MainLayout() {
  const hydrate = useAuthStore(s => s.hydrate)
  const isAuthenticated = useAuthStore(s => s.isAuthenticated)
  const navigate = useNavigate()

  useEffect(() => {
    hydrate()
  }, [hydrate])

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
    }
  }, [isAuthenticated, navigate])

  return (
    <div className="lt-fade">
      <AppHeader />
      <main className="lt-container">
        <Outlet />
      </main>
      <footer className="lt-footer">
        LiveTicket · 把现场留给热爱 · 演出票务与社交平台演示项目
      </footer>
    </div>
  )
}
