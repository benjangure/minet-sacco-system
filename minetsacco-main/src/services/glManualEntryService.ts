import { getApiBaseUrl } from '../config/api';
import { nativeFetch } from '../utils/nativeHttp';

const getAuthHeaders = () => {
  let token = null;
  
  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      token = parsedSession.token;
    }
  } catch (e) {
    console.warn('Failed to parse session from localStorage');
  }
  
  if (!token) {
    token = localStorage.getItem('token');
  }
  
  if (!token) {
    console.warn('No token found in localStorage');
  }
  
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

const getApiBaseUrlDynamic = (): string => {
  return getApiBaseUrl();
};

export interface GLManualEntry {
  id: number;
  glAccountId: number;
  glAccountCode: string;
  glAccountName: string;
  entryDate: string;
  description: string;
  amount: number;
  isDebit: boolean;
  entryReason: string;
  approvalStatus: string;
  createdByUserName: string;
  approvedByUserName?: string;
  createdAt: string;
  approvedAt?: string;
  periodStatus?: string;
  workflowStatus: string;
  entrySource: 'MANUAL_ENTRY' | 'PERIOD_ENTRY';
  periodMonth?: number;
  periodYear?: number;
}

export interface GLManualEntryRequest {
  glAccountId: number;
  entryDate: string;
  description: string;
  amount: number;
  isDebit: boolean;
  entryReason: string;
}

export interface GLAccount {
  id: number;
  code: string;
  name: string;
  accountType: string;
  balanceCalculationType: string;
  isActive: boolean;
}

const glManualEntryService = {
  // Get all GL accounts for dropdown
  getGLAccounts: async (): Promise<GLAccount[]> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/accounts`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to fetch GL accounts');
      const data = await response.json();
      return data.data || [];
    } catch (error) {
      console.error('Error fetching GL accounts:', error);
      throw error;
    }
  },

  // Create a new manual entry
  createManualEntry: async (entry: GLManualEntryRequest): Promise<GLManualEntry> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(entry),
      });
      if (!response.ok) throw new Error('Failed to create manual entry');
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error creating manual entry:', error);
      throw error;
    }
  },

  // Get all pending entries
  getPendingEntries: async (): Promise<GLManualEntry[]> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries/pending`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to fetch pending entries');
      const data = await response.json();
      return data.data || [];
    } catch (error) {
      console.error('Error fetching pending entries:', error);
      throw error;
    }
  },

  // Get all manual entries
  getAllEntries: async (): Promise<GLManualEntry[]> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to fetch manual entries');
      const data = await response.json();
      return data.data || [];
    } catch (error) {
      console.error('Error fetching manual entries:', error);
      throw error;
    }
  },

  // Get entries by account
  getEntriesByAccount: async (accountId: number): Promise<GLManualEntry[]> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries/account/${accountId}`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to fetch entries by account');
      const data = await response.json();
      return data.data || [];
    } catch (error) {
      console.error('Error fetching entries by account:', error);
      throw error;
    }
  },

  // Approve an entry
  approveEntry: async (entryId: number): Promise<GLManualEntry> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries/${entryId}/approve`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({}),
      });
      if (!response.ok) throw new Error('Failed to approve entry');
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error approving entry:', error);
      throw error;
    }
  },

  // Reject an entry
  rejectEntry: async (entryId: number): Promise<GLManualEntry> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries/${entryId}/reject`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({}),
      });
      if (!response.ok) throw new Error('Failed to reject entry');
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error rejecting entry:', error);
      throw error;
    }
  },

  approvePeriodEntry: async (entryId: number): Promise<GLManualEntry> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/period-entry/${entryId}/approve`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to approve period entry');
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error approving period entry:', error);
      throw error;
    }
  },

  rejectPeriodEntry: async (entryId: number, rejectReason = ''): Promise<GLManualEntry> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/period-entry/${entryId}/reject`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ rejectReason }),
      });
      if (!response.ok) throw new Error('Failed to reject period entry');
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error rejecting period entry:', error);
      throw error;
    }
  },

  // Delete an entry
  deleteEntry: async (entryId: number): Promise<void> => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}/gl/manual-entries/${entryId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (!response.ok) throw new Error('Failed to delete entry');
    } catch (error) {
      console.error('Error deleting entry:', error);
      throw error;
    }
  },
};

export default glManualEntryService;
