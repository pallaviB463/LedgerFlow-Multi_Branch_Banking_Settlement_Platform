import { useState, useEffect } from 'react';
import { bankingApi, getStoredUser } from '../lib/api';

export default function AccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [statement, setStatement] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [user, setUser] = useState(null);

  useEffect(() => {
    const u = getStoredUser();
    setUser(u);
    loadAccounts(u);
  }, []);

  const loadAccounts = async (currentUser) => {
    try {
      const accs = await bankingApi.getAccounts();
      setAccounts(accs);
      if (accs.length > 0) {
        let defaultId = accs[0].id;
        if (currentUser?.accountNumber) {
          const match = accs.find(a => a.accountNumber === currentUser.accountNumber);
          if (match) defaultId = match.id;
        }
        setSelectedAccountId(defaultId);
        fetchStatement(defaultId);
      }
    } catch (err) {
      console.error('Failed to load accounts:', err);
    }
  };

  const fetchStatement = async (id) => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const data = await bankingApi.getAccountLedger(id);
      setStatement(data);
    } catch (err) {
      setError(err.message || 'Failed to retrieve account ledger statement.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectAccount = (e) => {
    const id = e.target.value;
    setSelectedAccountId(id);
    fetchStatement(id);
  };

  const isBalanced = statement && Math.abs(parseFloat(statement.currentBalance) - parseFloat(statement.ledgerBalance)) < 0.001;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">Account Ledger & Audit Statement</h1>
          <p className="page-subtitle">
            Section 7: Verifies stored balance against full chronological double-entry debit/credit ledger
          </p>
        </div>
        <div style={{ minWidth: '260px' }}>
          <label className="form-label">Select Account to Inquire</label>
          <select
            className="form-select font-mono"
            value={selectedAccountId}
            onChange={handleSelectAccount}
          >
            {accounts.map((acc) => (
              <option key={acc.id} value={acc.id}>
                {acc.accountNumber} — {acc.accountHolderName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {statement && (
        <>
          {/* Dual Balance Integrity Cards */}
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-label">Stored Account Balance</div>
              <div className="stat-value" style={{ color: '#38bdf8' }}>
                ${(parseFloat(statement.currentBalance) || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </div>
              <div className="stat-help">Direct query from PostgreSQL <code>accounts.balance</code></div>
            </div>

            <div className="stat-card">
              <div className="stat-label">Calculated Ledger Balance</div>
              <div className="stat-value" style={{ color: '#a78bfa' }}>
                ${(parseFloat(statement.ledgerBalance) || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </div>
              <div className="stat-help">Summed over all chronological <code>ledger_entries</code></div>
            </div>

            <div className="stat-card">
              <div className="stat-label">Mathematical Invariant Check</div>
              <div className="stat-value" style={{ color: isBalanced ? '#34d399' : '#f87171' }}>
                {isBalanced ? '100% MATCH' : 'DISCREPANCY'}
              </div>
              <div className="stat-help">Delta = ${(Math.abs(parseFloat(statement.currentBalance) - parseFloat(statement.ledgerBalance))).toFixed(4)}</div>
            </div>
          </div>

          {/* Statement Entries Table */}
          <div className="card">
            <div className="card-header">
              <div>
                <h2 className="card-title">Chronological Statement for {statement.accountHolderName} ({statement.accountNumber})</h2>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem' }}>
                  Double-entry ledger history with running balance after each posted transaction.
                </p>
              </div>
              <span className="badge badge-blue">{statement.entries?.length || 0} Entries</span>
            </div>

            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Entry ID</th>
                    <th>Reference</th>
                    <th>Type</th>
                    <th>Description</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th style={{ textAlign: 'right' }}>Running Balance</th>
                    <th>Posted At</th>
                  </tr>
                </thead>
                <tbody>
                  {statement.entries && statement.entries.length > 0 ? (
                    statement.entries.map((entry) => {
                      const isCredit = entry.entryType === 'CREDIT';
                      return (
                        <tr key={entry.ledgerEntryId}>
                          <td className="font-mono">#{entry.ledgerEntryId}</td>
                          <td className="font-mono" style={{ color: '#38bdf8' }}>{entry.referenceNumber || 'N/A'}</td>
                          <td>
                            <span className={`badge ${isCredit ? 'badge-success' : 'badge-danger'}`}>
                              {entry.entryType}
                            </span>
                          </td>
                          <td>{entry.description || 'Fund settlement'}</td>
                          <td className="font-mono" style={{ textAlign: 'right', fontWeight: 600, color: isCredit ? '#34d399' : '#f87171' }}>
                            {isCredit ? '+' : '-'}${parseFloat(entry.amount).toFixed(2)}
                          </td>
                          <td className="font-mono" style={{ textAlign: 'right' }}>
                            ${parseFloat(entry.runningBalance).toFixed(2)}
                          </td>
                          <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                            {entry.postedAt ? new Date(entry.postedAt).toLocaleString() : 'N/A'}
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan="7" style={{ textAlign: 'center', color: 'var(--text-faint)', padding: '2rem' }}>
                        No ledger entries found for this account.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

