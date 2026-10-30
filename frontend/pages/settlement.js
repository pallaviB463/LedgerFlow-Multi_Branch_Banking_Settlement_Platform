import { useState, useEffect } from 'react';
import { bankingApi, getStoredUser } from '../lib/api';

export default function SettlementPage() {
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [settling, setSettling] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [user, setUser] = useState(null);

  useEffect(() => {
    setUser(getStoredUser());
    loadBatches();
  }, []);

  const loadBatches = async () => {
    setLoading(true);
    try {
      const data = await bankingApi.getBatches();
      setBatches(data || []);
    } catch (err) {
      console.error('Failed to load batches:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSettle = async () => {
    setSettling(true);
    setError('');
    setResult(null);
    try {
      const res = await bankingApi.settleCurrentBatch();
      setResult(res);
      loadBatches();
    } catch (err) {
      setError(err.message || 'Settlement failed. Ensure you have the ROLE_BANK_MANAGER role.');
    } finally {
      setSettling(false);
    }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">ACH Batch Settlement & Netting Engine</h1>
          <p className="page-subtitle">
            Manager Portal: Executes inter-branch multilateral netting and archives clearing receipts to MongoDB
          </p>
        </div>
        <button
          onClick={handleSettle}
          disabled={settling}
          className="btn btn-success"
          style={{ padding: '0.65rem 1.5rem' }}
        >
          {settling ? 'Executing Settlement Saga...' : '⚡ Settle Current ACH Batch'}
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {result && (
        <div className="alert alert-success">
          <strong>Batch Settlement Completed!</strong> Settled batch <code>#{result.batchId}</code> containing {result.settledCount || 0} transfers.
          Netting balances updated and archived to MongoDB collection <code>ach_clearing_receipts</code>.
        </div>
      )}

      {/* Batches Table */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">ACH Batches Ledger</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem' }}>
              Batches accumulate queued transfers and clear through the multi-stage netting algorithm.
            </p>
          </div>
          <button onClick={loadBatches} className="btn btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.775rem' }}>
            ↻ Refresh
          </button>
        </div>

        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Batch ID</th>
                <th>Status</th>
                <th>Total Volume</th>
                <th>Transfer Count</th>
                <th>Created At</th>
                <th>Settled At</th>
              </tr>
            </thead>
            <tbody>
              {batches.length > 0 ? (
                batches.map((b) => (
                  <tr key={b.id}>
                    <td><strong className="font-mono">BATCH-{b.id}</strong></td>
                    <td>
                      <span className={`badge ${b.status === 'SETTLED' ? 'badge-success' : b.status === 'PROCESSING' ? 'badge-purple' : 'badge-warning'}`}>
                        {b.status}
                      </span>
                    </td>
                    <td className="font-mono" style={{ fontWeight: 600 }}>
                      ${(b.totalAmount || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="font-mono">{b.transactionCount || 0} transfers</td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      {b.createdAt ? new Date(b.createdAt).toLocaleString() : 'N/A'}
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      {b.updatedAt ? new Date(b.updatedAt).toLocaleString() : 'Open'}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', color: 'var(--text-faint)', padding: '2rem' }}>
                    {loading ? 'Loading ACH batches...' : 'No ACH batches found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

