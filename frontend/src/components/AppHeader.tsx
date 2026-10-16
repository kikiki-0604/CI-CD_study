import { NavLink } from 'react-router-dom'
import { ChefHatIcon, CalendarIcon, SparkleIcon } from './icons'

type Props = {
  title: string
  tagline?: string
}

export default function AppHeader({ title, tagline }: Props) {
  return (
    <header className="app-header">
      <div className="brand-row">
        <span className="brand-badge">
          <ChefHatIcon />
        </span>
        <div>
          <p className="brand-name">献立メーカー</p>
          <nav className="app-nav">
            <NavLink to="/" end className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}>
              <SparkleIcon />
              食材から探す
            </NavLink>
            <NavLink to="/history" className={({ isActive }) => (isActive ? 'nav-link nav-link-active' : 'nav-link')}>
              <CalendarIcon />
              履歴
            </NavLink>
          </nav>
        </div>
      </div>
      <h1 className="app-title">{title}</h1>
      {tagline && <p className="app-tagline">{tagline}</p>}
    </header>
  )
}
