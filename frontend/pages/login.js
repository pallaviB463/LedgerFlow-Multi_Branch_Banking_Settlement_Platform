import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import { authApi, getStoredUser } from '../lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [demoUsers, setDemoUsers] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    setCurrentUser(getStoredUser());
    authApi.getDemoUsers()
      .then(res => setDemoUsers(res))
      .catch(() => {
        // Fallback demo users list
        setDemoUsers([
          { username: 'admin', password: 'admin123', role: 'ROLE_BANK_MANAGER', displayName: 'Chief Banking Officer', description: 'Full administrative, ACH batch settlement & compliance analytics' },
          { username: 'teller', password: 'teller123', role: 'ROLE_BANK_MANAGER', displayName: 'Settlement Operator', description: 'Batch processing and account ledger operations' },
          { username: 'alice', password: 'alice123', role: 'ROLE_CUSTOMER', displayName: 'Alice Johnson', description: 'Retail account holder (ACC-10001)' },
          { username: 'bob', password: 'bob123', role: 'ROLE_CUSTOMER', displayName: 'Bob Smith', description: 'Retail account holder (ACC-10002)' }
        ]);
      });
  }, []);

  const handleLogin = async (u = username, p = password) => {
    setLoading(true);
    setError('');
    try {
      await authApi.login(u, p);
      router.push('/');
    } catch (err) {
      setError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const selectDemoUser = (user) => {
    setUsername(user.username);
    setPassword(user.password);
    handleLogin(user.username, user.password);
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Institutional Access & RBAC Authentication</h1>
        <p className="page-subtitle">
          Section 11 Compliant: Role-Based Access Control with dual-role authentication (Bank Manager vs Customer)
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem' }}>
        {/* Credentials Form */}
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Sign In to LedgerFlow</h2>
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          <form onSubmit={(e) => { e.preventDefault(); handleLogin(); }}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                type="text"
                className="form-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Authenticating...' : 'Sign In via JWT'}
            </button>
          </form>
        </div>

        {/* 1-Click Evaluation Credentials */}
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Pre-Seeded Evaluation Roles</h2>
            <span className="badge badge-blue">One-Click Switch</span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
            Click any institutional profile below to instantly authenticate and evaluate role permissions.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {demoUsers.map((u) => {
              const isMgr = u.role === 'ROLE_BANK_MANAGER';
              return (
                <div
                  key={u.username}
                  onClick={() => selectDemoUser(u)}
                  style={{
                    padding: '0.85rem 1rem',
                    background: 'var(--bg-input)',
                    borderRadius: '8px',
                    border: '1px solid var(--border-color)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    transition: 'border-color 0.15s ease',
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.borderColor = '#3b82f6'}
                  onMouseLeave={(e) => e.currentTarget.style.borderColor = 'var(--border-color)'}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
                      <span style={{ fontWeight: 600 }}>{u.displayName}</span>
                      <span className={`badge ${isMgr ? 'badge-blue' : 'badge-purple'}`}>
                        {isMgr ? 'Manager' : 'Customer'}
                      </span>
                    </div>
                    <div style={{ color: 'var(--text-faint)', fontSize: '0.775rem' }}>
                      User: <code style={{ color: '#38bdf8' }}>{u.username}</code> | Pass: <code style={{ color: '#38bdf8' }}>{u.password}</code>
                    </div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.775rem', marginTop: '0.2rem' }}>
                      {u.description}
                    </div>
                  </div>
                  <span style={{ color: '#3b82f6', fontSize: '1.2rem', fontWeight: 700 }}>→</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

