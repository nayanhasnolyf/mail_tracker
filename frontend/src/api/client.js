/**
 * Application Tracker API Client
 * Connects with Spring Boot REST endpoints on /api/** with credentials.
 */

const API_BASE_URL = '/api';

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const config = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...options.headers,
    },
    credentials: 'include',
  };

  try {
    const response = await fetch(url, config);

    if (response.status === 401) {
      const errorData = await response.json().catch(() => ({}));
      throw new ApiError(errorData.message || 'Unauthorized', 401, errorData);
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const message = errorData.message || `Request failed with status ${response.status}`;
      throw new ApiError(message, response.status, errorData);
    }

    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }
    throw new ApiError(err.message || 'Network error', 0, null);
  }
}

export const api = {
  /**
   * Fetches current authenticated user profile
   */
  async getCurrentUser() {
    return request('/me');
  },

  /**
   * Fetches applications with optional filtering and pagination
   */
  async getApplications({ status, company, page = 0, size = 50, sort = 'lastUpdatedAt,desc' } = {}) {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (company) params.append('company', company);
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', sort);

    return request(`/applications?${params.toString()}`);
  },

  /**
   * Fetches single application with full status history
   */
  async getApplicationById(id) {
    return request(`/applications/${id}`);
  },

  /**
   * Updates application status and/or notes
   */
  async updateApplication(id, { status, notes }) {
    const body = {};
    if (status !== undefined) body.status = status;
    if (notes !== undefined) body.notes = notes;

    return request(`/applications/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    });
  },

  /**
   * Fetches aggregate pipeline statistics
   */
  async getStats() {
    return request('/stats');
  },

  /**
   * Fetches applications in APPLIED state with no updates for N days
   */
  async getFollowups(days = 7) {
    return request(`/followups?days=${days}`);
  },

  /**
   * Refreshes OAuth2 access token
   */
  async refreshToken() {
    return request('/auth/refresh', { method: 'POST' });
  },
};

/**
 * Realistic mock data for demo mode or local preview
 */
export const DEMO_USER = {
  id: 'demo-user-123',
  email: 'alex.candidate@gmail.com',
  googleSubjectId: 'sub-google-demo',
  createdAt: new Date().toISOString(),
  reauthRequired: false,
  hasRefreshToken: true,
};

export const INITIAL_DEMO_APPLICATIONS = [
  {
    id: 'app-01',
    company: 'Stripe',
    role: 'Staff Backend Engineer',
    currentStatus: 'INTERVIEW',
    notes: 'Completed technical coding round. System design interview scheduled for next Tuesday.',
    sourceEmailId: 'msg-stripe-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 12).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 1).toISOString(),
    statusHistory: [
      { id: 'h-01-3', status: 'INTERVIEW', changedAt: new Date(Date.now() - 86400000 * 1).toISOString(), sourceEmailId: 'msg-stripe-03' },
      { id: 'h-01-2', status: 'ASSESSMENT', changedAt: new Date(Date.now() - 86400000 * 6).toISOString(), sourceEmailId: 'msg-stripe-02' },
      { id: 'h-01-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 12).toISOString(), sourceEmailId: 'msg-stripe-01' },
    ],
  },
  {
    id: 'app-02',
    company: 'Figma',
    role: 'Senior Frontend Engineer',
    currentStatus: 'ASSESSMENT',
    notes: 'Received take-home challenge. Needs submission by Friday.',
    sourceEmailId: 'msg-figma-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 5).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    statusHistory: [
      { id: 'h-02-2', status: 'ASSESSMENT', changedAt: new Date(Date.now() - 86400000 * 3).toISOString(), sourceEmailId: 'msg-figma-02' },
      { id: 'h-02-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 5).toISOString(), sourceEmailId: 'msg-figma-01' },
    ],
  },
  {
    id: 'app-03',
    company: 'Apple',
    role: 'iOS Systems Architect',
    currentStatus: 'OFFER',
    notes: 'Official offer letter received! Base + RSU equity package under review.',
    sourceEmailId: 'msg-apple-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 25).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    statusHistory: [
      { id: 'h-03-3', status: 'OFFER', changedAt: new Date(Date.now() - 86400000 * 2).toISOString(), sourceEmailId: 'msg-apple-03' },
      { id: 'h-03-2', status: 'INTERVIEW', changedAt: new Date(Date.now() - 86400000 * 10).toISOString(), sourceEmailId: 'msg-apple-02' },
      { id: 'h-03-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 25).toISOString(), sourceEmailId: 'msg-apple-01' },
    ],
  },
  {
    id: 'app-04',
    company: 'Netflix',
    role: 'Distributed Systems Engineer',
    currentStatus: 'APPLIED',
    notes: 'Submitted via Workday portal.',
    sourceEmailId: 'msg-netflix-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 10).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 10).toISOString(),
    statusHistory: [
      { id: 'h-04-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 10).toISOString(), sourceEmailId: 'msg-netflix-01' },
    ],
  },
  {
    id: 'app-05',
    company: 'Datadog',
    role: 'Site Reliability Engineer',
    currentStatus: 'INTERVIEW',
    notes: 'Recruiter phone screen passed. Preparing for technical architecture session.',
    sourceEmailId: 'msg-datadog-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 8).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 4).toISOString(),
    statusHistory: [
      { id: 'h-05-2', status: 'INTERVIEW', changedAt: new Date(Date.now() - 86400000 * 4).toISOString(), sourceEmailId: 'msg-datadog-02' },
      { id: 'h-05-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 8).toISOString(), sourceEmailId: 'msg-datadog-01' },
    ],
  },
  {
    id: 'app-06',
    company: 'Google',
    role: 'Cloud Platform Developer',
    currentStatus: 'REJECTED',
    notes: 'Position filled internally. Suggested applying for team in Q1.',
    sourceEmailId: 'msg-google-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 20).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 7).toISOString(),
    statusHistory: [
      { id: 'h-06-2', status: 'REJECTED', changedAt: new Date(Date.now() - 86400000 * 7).toISOString(), sourceEmailId: 'msg-google-02' },
      { id: 'h-06-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 20).toISOString(), sourceEmailId: 'msg-google-01' },
    ],
  },
  {
    id: 'app-07',
    company: 'Airbnb',
    role: 'Fullstack Infrastructure Engineer',
    currentStatus: 'APPLIED',
    notes: 'Direct application through referral.',
    sourceEmailId: 'msg-airbnb-01',
    firstSeenAt: new Date(Date.now() - 86400000 * 14).toISOString(),
    lastUpdatedAt: new Date(Date.now() - 86400000 * 14).toISOString(),
    statusHistory: [
      { id: 'h-07-1', status: 'APPLIED', changedAt: new Date(Date.now() - 86400000 * 14).toISOString(), sourceEmailId: 'msg-airbnb-01' },
    ],
  },
];
