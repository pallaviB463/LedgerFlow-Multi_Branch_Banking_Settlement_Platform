import { useState, useEffect } from 'react';
import { bankingApi, getStoredUser } from '../lib/api';

export default function TransfersPage() {
  const [accounts, setAccounts] = useState([]);
  const [sourceAccountId, setSourceAccountId] = useState('');
  const [destinationAccountId, setDestinationAccountId] = useState('');
  const [amount, setAmount] = useState('250.00');
  const [transferType, setTransferType] = useState('RTGS');
  const [description, setDescription] = useState('Inter-branch supplier settlement');
  const [loading, setLoading] = useState(false);
  const [receipt, setReceipt] = useState(null);
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
      if (accs.length >= 2) {
        // If current user is retail customer, default to their account
        if (currentUser?.accountNumber) {
          const match = accs.find(a => a.accountNumber === currentUser.accountNumber);
          if (match) {
            setSourceAccountId(match.id);
            const other = accs.find(a => a.id !== match.id);
            if (other) setDestinationAccountId(other.id);
            return;
          }
        }
        setSourceAccountId(accs[0].id);
        setDestinationAccountId(accs[1].id);
      }
    } catch (err) {
      console.error('Failed to load accounts:', err);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setReceipt(null);

    if (sourceAccountId === destinationAccountId) {
      setError('Source and Destination accounts cannot be the same.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        sourceAccountId: parseInt(sourceAccountId),
        destinationAccountId: parseInt(destinationAccountId),
        amount: parseFloat(amount),
        currency: 'USD',
        transferType: transferType,
        description: description,
      };

      const res = await bankingApi.executeTransfer(payload);
      setReceipt(res);
      // Reload accounts to update balances
      loadAccounts(user);
    } catch (err) {
      setError(err.message || 'Transfer failed. Check balance or system connectivity.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Interactive Transfer Desk</h1>
        <p className="page-subtitle">
          ACID Atomic Transfer Engine with Repeatable-Read Isolation & Pessimistic Row Locking
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem' }}>
        {/* Transfer Form */}
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Initiate Fund Transfer</h2>
            <span className={`badge ${transferType === 'RTGS' ? 'badge-blue' : 'badge-warning'}`}>
              {transferType}
            </span>
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Payment Rail</label>
              <select
                className="form-select"
                value={transferType}
                onChange={(e) => setTransferType(e.target.value)}
              >
                <option value="RTGS">RTGS — Real-Time Gross Settlement (Immediate Posting)</option>
                <option value="ACH">ACH — Automated Clearing House (Queued for Batch Settle)</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Source Account (Debit)</label>
              <select
                className="form-select font-mono"
                value={sourceAccountId}
                onChange={(e) => setSourceAccountId(e.target.value)}
                required
              >
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountNumber} — {acc.accountHolderName} (${parseFloat(acc.balance).toFixed(2)})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Destination Account (Credit)</label>
              <select
                className="form-select font-mono"
                value={destinationAccountId}
                onChange={(e) => setDestinationAccountId(e.target.value)}
                required
              >
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountNumber} — {acc.accountHolderName} (${parseFloat(acc.balance).toFixed(2)})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Transfer Amount (USD)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="form-input font-mono"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description / Remittance Memo</label>
              <input
                type="text"
                className="form-input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Processing Transaction...' : `Confirm & Authorize ${transferType} Transfer`}
            </button>
          </form>
        </div>

        {/* Transaction Receipt & Ledger Audit */}
        <div>
          {receipt ? (
            <div className="card" style={{ borderColor: '#10b981' }}>
              <div className="card-header">
                <div>
                  <h2 className="card-title" style={{ color: '#34d399' }}>Transaction Executed Successfully</h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem' }}>
                    Reference: <strong className="font-mono" style={{ color: '#f8fafc' }}>{receipt.referenceNumber}</strong>
                  </p>
                </div>
                <span className="badge badge-success">{receipt.status}</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Amount Transferred</span>
                  <span className="font-mono" style={{ fontWeight: 700, fontSize: '1.1rem', color: '#38bdf8' }}>
                    ${parseFloat(receipt.amount).toFixed(2)} USD
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Source Account ({receipt.sourceAccountNumber})</span>
                  <span className="font-mono">
                    Post Balance: <strong>${parseFloat(receipt.sourceBalanceAfter).toFixed(2)}</strong>
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Destination Account ({receipt.destinationAccountNumber})</span>
                  <span className="font-mono">
                    Post Balance: <strong>${parseFloat(receipt.destinationBalanceAfter).toFixed(2)}</strong>
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Audit Log (MongoDB)</span>
                  <span className="badge badge-purple">Stored NoSQL Receipt</span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Timestamp</span>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                    {new Date(receipt.createdAt || Date.now()).toLocaleString()}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">Transaction Architecture</h2>
                <span className="badge badge-blue">ACID Isolation</span>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', lineHeight: '1.6' }}>
                Each transfer executes under <code>REPEATABLE_READ</code> isolation with deterministic account lock ordering:
              </p>
              <ul style={{ paddingLeft: '1.25rem', marginTop: '0.75rem', color: 'var(--text-muted)', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <li><strong>Deadlock Prevention:</strong> Accounts are locked in ascending primary key ID order.</li>
                <li><strong>Balance Non-Negativity:</strong> Enforced by SQL <code>CHECK (balance &gt;= 0)</code> and PL/pgSQL triggers.</li>
                <li><strong>NoSQL Dual-Write:</strong> An immutable audit event is synchronously emitted to the MongoDB collection.</li>
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

