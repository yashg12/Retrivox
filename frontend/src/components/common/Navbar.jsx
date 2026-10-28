import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { BookOpen, LogOut, Sun, Moon, User as UserIcon } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="navbar">
      <a href="/" className="brand" aria-label="Retrivox Home">
        <div className="brand-icon">
          <BookOpen size={20} />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.1 }}>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Retrivox</span>
          <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.02em' }}>
            Your Notes. Your Knowledge. Your Next Level.
          </span>
        </div>
      </a>

      <div className="nav-actions">
        <button
          className="btn-icon"
          onClick={toggleTheme}
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          aria-label="Toggle theme"
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>

        {user && (
          <>
            <div className="user-badge" title={user.email}>
              <div className="user-avatar">
                {user.fullName ? user.fullName[0].toUpperCase() : user.email[0].toUpperCase()}
              </div>
              <span style={{ fontWeight: '600', maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user.fullName || user.email.split('@')[0]}
              </span>
            </div>

            <button
              className="btn btn-secondary"
              onClick={logout}
              style={{ padding: '8px 14px', fontSize: '0.85rem' }}
              title="Sign out"
            >
              <LogOut size={16} />
              <span>Logout</span>
            </button>
          </>
        )}
      </div>
    </header>
  );
}
