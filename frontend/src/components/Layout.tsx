import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'

export default function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [user, setUser] = useState<any>(null)

  useEffect(() => {
    const raw = localStorage.getItem('user')
    if (raw) {
      setUser(JSON.parse(raw))
    }
  }, [location])

  function handleLogout() {
    localStorage.removeItem('access_token')
    localStorage.removeItem('user')
    navigate('/login')
  }

  return (
    <div className="app-layout">
      <header className="navbar">
        <div className="navbar-brand" onClick={() => navigate('/dashboard')}>
          Order System
        </div>
        {user && (
          <div className="navbar-end">
            <span className="navbar-user">{user.full_name || user.email}</span>
            <button className="btn btn-sm" onClick={handleLogout}>
              Logout
            </button>
          </div>
        )}
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
