// API client for LedgerFlow Spring Boot Backend

const API_BASE = process.env.NEXT_PUBLIC_API_URL || '';

export const getAuthToken = () => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('lf_token');
  }
  return null;
};

export const getStoredUser = () => {
  if (typeof window !== 'undefined') {
    const raw = localStorage.getItem('lf_user');
    return raw ? JSON.parse(raw) : null;
  }
  return null;
};

export const setAuthSession = (token, user) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem('lf_token', token);
    localStorage.setItem('lf_user', JSON.stringify(user));
  }
};

export const clearAuthSession = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('lf_token');
    localStorage.removeItem('lf_user');
  }
};

export async function apiRequest(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const url = `${API_BASE}${endpoint}`;
  try {
    const res = await fetch(url, { ...options, headers });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(data.message || `Request failed with status ${res.status}`);
    }
    return data.data !== undefined ? data.data : data;
  } catch (err) {
    console.error(`API Error on ${endpoint}:`, err);
    throw err;
  }
}

// Authentication API
export const authApi = {
  login: async (username, password) => {
    const data = await apiRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    setAuthSession(data.token, data);
    return data;
  },
  getMe: () => apiRequest('/api/auth/me'),
  getDemoUsers: () => apiRequest('/api/auth/demo-users'),
  logout: () => clearAuthSession(),
};

// Banking Operations API
export const bankingApi = {
  getHealth: () => apiRequest('/api/health'),
  getBranches: () => apiRequest('/api/branches'),
  getAccounts: () => apiRequest('/api/accounts'),
  createAccount: (payload) =>
    apiRequest('/api/accounts', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getAccountLedger: (accountId) => apiRequest(`/api/ledger/accounts/${accountId}`),
  executeTransfer: (payload) =>
    apiRequest('/api/transfers', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getTransactions: () => apiRequest('/api/transactions'),
  getTrialBalance: () => apiRequest('/api/reports/views/trial-balance'),
  getBranchSummary: () => apiRequest('/api/reports/views/branch-summary'),
  getBatches: () => apiRequest('/api/ach/batches'),
  settleCurrentBatch: () =>
    apiRequest('/api/ach/batches/current/settle', {
      method: 'POST',
    }),
};

// Compliance & MongoDB NoSQL Analytics API
export const complianceApi = {
  getBranchRiskAnalytics: () => apiRequest('/api/compliance/analytics/branch-risk'),
  getEventDistribution: () => apiRequest('/api/compliance/analytics/event-distribution'),
  getAuditLogs: () => apiRequest('/api/compliance/logs'),
};

