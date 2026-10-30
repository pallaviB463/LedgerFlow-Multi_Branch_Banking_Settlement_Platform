import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { getStoredUser, clearAuthSession } from '../lib/api';

export default function Navbar() {
  const router = useRouter();
  const [user, setUser] = useState(null);

  useEffect(() => {
    setUser(getStoredUser());
  }, [router.asPath]);

  const handleLogout = () => {
    clearAuthSession();
    setUser(null);
    router.push('/login');
  };

  const isManager = user?.role === 'ROLE_BANK_MANAGER';

  return (
    <nav className="navbar">
      <div className="container nav-container">
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <Link href="/" className="brand">
            <span style={{ color: '#3b82f6', fontSize: '1.4rem' }}>◆</span>
            <span>LedgerFlow</span>
            <span className="brand-badge">Core Banking</span>
          </Link>

          <div className="nav-links">
            <Link href="/" className={`nav-link ${router.pathname === '/' ? 'active' : ''}`}>
              Overview
            </Link>
            <Link href="/transfers" className={`nav-link ${router.pathname === '/transfers' ? 'active' : ''}`}>
              Transfer Desk
            </Link>
            <Link href="/accounts" className={`nav-link ${router.pathname === '/accounts' ? 'active' : ''}`}>
              Ledger Query
            </Link>

            {isManager && (
              <>
                <Link href="/settlement" className={`nav-link ${router.pathname === '/settlement' ? 'active' : ''}`}>
                  ACH Settlement
                </Link>
                <Link href="/compliance" className={`nav-link ${router.pathname === '/compliance' ? 'active' : ''}`}>
                  NoSQL Compliance
                </Link>
              </>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div className="user-badge">
                <span style={{ fontWeight: 600 }}>{user.displayName || user.username}</span>
                <span className={`badge ${isManager ? 'badge-blue' : 'badge-purple'}`}>
                  {isManager ? 'Manager' : 'Customer'}
                </span>
                {user.accountNumber && (
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                    {user.accountNumber}
                  </span>
                )}
              </div>
              <button onClick={handleLogout} className="btn btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.775rem' }}>
                Logout
              </button>
            </div>
          ) : (
            <Link href="/login" className="btn btn-primary" style={{ padding: '0.45rem 1rem', fontSize: '0.85rem' }}>
              Sign In (RBAC)
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}

