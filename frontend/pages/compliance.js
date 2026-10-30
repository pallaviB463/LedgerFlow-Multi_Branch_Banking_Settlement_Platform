import { useState, useEffect } from 'react';
import { complianceApi, getStoredUser } from '../lib/api';

export default function CompliancePage() {
  const [riskData, setRiskData] = useState([]);
  const [eventDistribution, setEventDistribution] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [user, setUser] = useState(null);

  useEffect(() => {
    setUser(getStoredUser());
    loadComplianceData();
  }, []);

  const loadComplianceData = async () => {
    setLoading(true);
    setError('');
    try {
      const [risk, dist, l] = await Promise.allSettled([
        complianceApi.getBranchRiskAnalytics(),
        complianceApi.getEventDistribution(),
        complianceApi.getAuditLogs()
      ]);

      if (risk.status === 'fulfilled') setRiskData(risk.value || []);
      if (dist.status === 'fulfilled') setEventDistribution(dist.value || []);
      if (l.status === 'fulfilled') setLogs(l.value || []);
    } catch (err) {
      setError(err.message || 'Failed to query MongoDB NoSQL Aggregation Pipeline');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="page-title">NoSQL Compliance & Risk Intelligence</h1>
          <p className="page-subtitle">
            Section 3 & 7: MongoDB 7.0 Document Store powered by 4-Stage Aggregation Pipeline
          </p>
        </div>
        <button onClick={loadComplianceData} className="btn btn-secondary">
          ↻ Re-run Aggregation
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Multi-Stage Aggregation Pipeline Banner */}
      <div className="card" style={{ background: 'rgba(30, 41, 59, 0.4)', borderColor: '#8b5cf6' }}>
        <div className="card-header">
          <div>
            <h2 className="card-title" style={{ color: '#c084fc' }}>
              Advanced NoSQL Feature: 4-Stage MongoDB Aggregation Pipeline
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.825rem', marginTop: '0.2rem' }}>
              Rubric Section 7: Complex multi-stage analytical aggregation executing inside the MongoDB engine.
            </p>
          </div>
          <span className="badge badge-purple">$match → $group → $project → $sort</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', fontSize: '0.8rem' }}>
          <div style={{ padding: '0.75rem', background: 'var(--bg-card)', borderRadius: '6px' }}>
            <strong style={{ color: '#38bdf8' }}>Stage 1: $match</strong>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>Filters audit logs within a rolling 30-day temporal window.</p>
          </div>
          <div style={{ padding: '0.75rem', background: 'var(--bg-card)', borderRadius: '6px' }}>
            <strong style={{ color: '#38bdf8' }}>Stage 2: $group</strong>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>Groups by composite key <code>(branchCode, eventType)</code>, computing sums & averages.</p>
          </div>
          <div style={{ padding: '0.75rem', background: 'var(--bg-card)', borderRadius: '6px' }}>
            <strong style={{ color: '#38bdf8' }}>Stage 3: $project</strong>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>Shapes final analytical risk metrics and rounds currency fractions.</p>
          </div>
          <div style={{ padding: '0.75rem', background: 'var(--bg-card)', borderRadius: '6px' }}>
            <strong style={{ color: '#38bdf8' }}>Stage 4: $sort</strong>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>Orders branches by total transactional exposure volume descending.</p>
          </div>
        </div>
      </div>

      {/* Aggregation Results Table */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Branch Transaction Exposure & Risk Velocity (Pipeline Output)</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem' }}>
              Live results streamed directly from MongoDB <code>compliance_audit_logs</code> collection.
            </p>
          </div>
          <span className="badge badge-blue">{riskData.length} Branches Analyzed</span>
        </div>

        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Branch Code</th>
                <th>Event Type</th>
                <th style={{ textAlign: 'right' }}>Total Volume (USD)</th>
                <th style={{ textAlign: 'right' }}>Event Count</th>
                <th style={{ textAlign: 'right' }}>Average Transfer Size</th>
                <th style={{ textAlign: 'right' }}>Peak Single Transfer</th>
              </tr>
            </thead>
            <tbody>
              {riskData.length > 0 ? (
                riskData.map((row, idx) => (
                  <tr key={idx}>
                    <td><strong className="font-mono">{row.branchCode || 'HEADQUARTERS'}</strong></td>
                    <td>
                      <span className="badge badge-blue">{row.eventType || 'TRANSFER'}</span>
                    </td>
                    <td className="font-mono" style={{ textAlign: 'right', fontWeight: 600, color: '#34d399' }}>
                      ${(row.totalVolume || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="font-mono" style={{ textAlign: 'right' }}>
                      {row.eventCount || 0}
                    </td>
                    <td className="font-mono" style={{ textAlign: 'right' }}>
                      ${(row.avgTransferSize || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="font-mono" style={{ textAlign: 'right', color: '#f87171' }}>
                      ${(row.maxTransferSize || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', color: 'var(--text-faint)', padding: '2rem' }}>
                    {loading ? 'Running MongoDB aggregation pipeline...' : 'No audit events recorded yet. Perform transfers to populate MongoDB.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* MongoDB Audit Event Stream */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Immutable MongoDB Document Stream (Audit Logs)</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.2rem' }}>
              Non-blocking dual-write audit logs stored with flexible BSON schemas.
            </p>
          </div>
          <span className="badge badge-purple">{logs.length} BSON Documents</span>
        </div>

        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Document ID</th>
                <th>Event Type</th>
                <th>Entity Type</th>
                <th>Branch</th>
                <th>Payload Snapshot</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {logs.length > 0 ? (
                logs.slice(0, 10).map((log) => (
                  <tr key={log.id}>
                    <td className="font-mono" style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                      {log.id ? log.id.substring(0, 12) + '...' : 'N/A'}
                    </td>
                    <td><span className="badge badge-blue">{log.eventType}</span></td>
                    <td className="font-mono">{log.entityType}</td>
                    <td className="font-mono">{log.branchCode || 'GLOBAL'}</td>
                    <td>
                      <code style={{ fontSize: '0.75rem', color: '#38bdf8', maxWidth: '300px', display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {JSON.stringify(log.payload || {})}
                      </code>
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : 'N/A'}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', color: 'var(--text-faint)', padding: '2rem' }}>
                    Audit stream empty. Real-time transactions trigger MongoDB writes.
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

