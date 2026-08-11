import React, { useState, useEffect } from 'react';
import { useRefresh } from '@/contexts/RefreshContext';
import {
  AlertCircle,
  Plus,
  CheckCircle,
  XCircle,
  Trash2,
  RefreshCw,
} from 'lucide-react';
import glManualEntryService, {
  GLManualEntry,
  GLManualEntryRequest,
  GLAccount,
} from '../services/glManualEntryService';
import { useAuth } from '@/contexts/AuthContext';

export default function GLManualEntries() {
  const { session } = useAuth();
  const { refreshKey } = useRefresh();
  const [entries, setEntries] = useState<GLManualEntry[]>([]);
  const [glAccounts, setGlAccounts] = useState<GLAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'pending'>('pending');
  const role = session?.user?.role?.toUpperCase() ?? '';
  const isAdmin = role === 'ADMIN';
  const isTreasurer = role === 'TREASURER';

  // Form state
  const [formData, setFormData] = useState<GLManualEntryRequest>({
    glAccountId: 0,
    entryDate: new Date().toISOString().split('T')[0],
    description: '',
    amount: 0,
    isDebit: true,
    entryReason: 'ACCRUAL',
  });

  useEffect(() => {
    // Only load data if user is authenticated
    if (session?.user) {
      loadGLAccounts();
      loadEntries();
    }
  }, [activeTab, session]);

  const loadGLAccounts = async () => {
    try {
      const accounts = await glManualEntryService.getGLAccounts();
      setGlAccounts(accounts);
    } catch (err: any) {
      // Silently fail on auth errors (401/403) - these are expected before login
      if (err.response?.status !== 401 && err.response?.status !== 403) {
        console.error('Error loading GL accounts:', err);
        setError('Failed to load GL accounts');
      }
    }
  };

  const loadEntries = async () => {
    setLoading(true);
    setError(null);
    try {
      const data =
        activeTab === 'pending'
          ? await glManualEntryService.getPendingEntries()
          : await glManualEntryService.getAllEntries();
      setEntries(data);
    } catch (err: any) {
      // Silently fail on auth errors (401/403) - these are expected before login
      if (err.response?.status !== 401 && err.response?.status !== 403) {
        console.error('Error loading entries:', err);
        setError('Failed to load entries');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCreateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await glManualEntryService.createManualEntry(formData);
      setSuccess('Entry created successfully and sent for approval');
      setShowForm(false);
      setFormData({
        glAccountId: 0,
        entryDate: new Date().toISOString().split('T')[0],
        description: '',
        amount: 0,
        isDebit: true,
        entryReason: 'ACCRUAL',
      });
      loadEntries();
    } catch (err) {
      console.error('Error creating entry:', err);
      setError('Failed to create entry');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveEntry = async (entry: GLManualEntry) => {
    setLoading(true);
    setError(null);
    try {
      if (entry.entrySource === 'PERIOD_ENTRY') {
        await glManualEntryService.approvePeriodEntry(entry.id);
      } else {
        await glManualEntryService.approveEntry(entry.id);
      }
      setSuccess('Entry approved');
      loadEntries();
    } catch (err) {
      console.error('Error approving entry:', err);
      setError('Failed to approve entry');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectEntry = async (entry: GLManualEntry) => {
    setLoading(true);
    setError(null);
    try {
      if (entry.entrySource === 'PERIOD_ENTRY') {
        const rejectReason = window.prompt('Enter rejection reason (optional):') ?? '';
        await glManualEntryService.rejectPeriodEntry(entry.id, rejectReason);
      } else {
        await glManualEntryService.rejectEntry(entry.id);
      }
      setSuccess('Entry rejected');
      loadEntries();
    } catch (err) {
      console.error('Error rejecting entry:', err);
      setError('Failed to reject entry');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEntry = async (entryId: number) => {
    if (!window.confirm('Are you sure you want to delete this entry?')) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await glManualEntryService.deleteEntry(entryId);
      setSuccess('Entry deleted');
      loadEntries();
    } catch (err) {
      console.error('Error deleting entry:', err);
      setError('Failed to delete entry');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]:
        type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
      ...(name === 'amount' && { [name]: parseFloat(value) || 0 }),
      ...(name === 'glAccountId' && { [name]: parseInt(value) || 0 }),
    }));
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DRAFT':
        return 'bg-gray-100 text-gray-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'POSTED':
        return 'bg-blue-100 text-blue-800';
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'LOCKED':
        return 'bg-purple-100 text-purple-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getDisplayStatus = (entry: GLManualEntry) =>
    entry.workflowStatus || entry.periodStatus || entry.approvalStatus;

  const selectedAccount = glAccounts.find(
    (acc) => acc.id === formData.glAccountId
  );

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">GL Manual Entries</h1>
          <p className="text-gray-600 mt-2">
            Enter accruals, adjustments, and other manual GL entries
          </p>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
            <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-red-900">Error</p>
              <p className="text-red-800">{error}</p>
            </div>
          </div>
        )}

        {success && (
          <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
            <CheckCircle className="text-green-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-green-900">Success</p>
              <p className="text-green-800">{success}</p>
            </div>
          </div>
        )}

        {/* Tabs and Actions */}
        <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex gap-2">
            <button
              onClick={() => setActiveTab('pending')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                activeTab === 'pending'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-800 hover:bg-gray-300'
              }`}
            >
              Pending Approval
            </button>
            <button
              onClick={() => setActiveTab('all')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                activeTab === 'all'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-800 hover:bg-gray-300'
              }`}
            >
              All Entries
            </button>
          </div>

          <div className="flex gap-2">
            <button
              onClick={loadEntries}
              disabled={loading}
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition disabled:opacity-50 flex items-center gap-2"
            >
              <RefreshCw size={18} />
              Refresh
            </button>
            <button
              onClick={() => setShowForm(!showForm)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition flex items-center gap-2"
            >
              <Plus size={18} />
              New Entry
            </button>
          </div>
        </div>

        {/* Form */}
        {showForm && (
          <div className="mb-8 bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Create Manual GL Entry</h2>
            <form onSubmit={handleCreateEntry} className="space-y-4">
              <div className="grid md:grid-cols-2 gap-4">
                {/* GL Account */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    GL Account *
                  </label>
                  <select
                    name="glAccountId"
                    value={formData.glAccountId}
                    onChange={handleInputChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="0">Select an account...</option>
                    {glAccounts.map((acc) => (
                      <option key={acc.id} value={acc.id}>
                        {acc.code} - {acc.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Entry Date */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Entry Date *
                  </label>
                  <input
                    type="date"
                    name="entryDate"
                    value={formData.entryDate}
                    onChange={handleInputChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Amount */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Amount (KES) *
                  </label>
                  <input
                    type="number"
                    name="amount"
                    value={formData.amount}
                    onChange={handleInputChange}
                    required
                    min="0"
                    step="0.01"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Entry Reason */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Entry Reason *
                  </label>
                  <select
                    name="entryReason"
                    value={formData.entryReason}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="ACCRUAL">Accrual</option>
                    <option value="ADJUSTMENT">Adjustment</option>
                    <option value="ALLOCATION">Allocation</option>
                    <option value="RECLASSIFICATION">Reclassification</option>
                  </select>
                </div>

                {/* Debit/Credit */}
                <div className="flex items-end gap-4">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="isDebit"
                      value="true"
                      checked={formData.isDebit === true}
                      onChange={() =>
                        setFormData((prev) => ({ ...prev, isDebit: true }))
                      }
                    />
                    <span>Debit</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="isDebit"
                      value="false"
                      checked={formData.isDebit === false}
                      onChange={() =>
                        setFormData((prev) => ({ ...prev, isDebit: false }))
                      }
                    />
                    <span>Credit</span>
                  </label>
                </div>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                  placeholder="Provide details about this entry..."
                />
              </div>

              {/* Summary */}
              {selectedAccount && (
                <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm">
                    <strong>Account:</strong> {selectedAccount.code} -{' '}
                    {selectedAccount.name}
                  </p>
                  <p className="text-sm">
                    <strong>Amount:</strong> KES{' '}
                    {formData.amount.toLocaleString('en-US', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}{' '}
                    ({formData.isDebit ? 'Debit' : 'Credit'})
                  </p>
                  <p className="text-xs text-gray-600 mt-2">
                    This entry will be created with PENDING status and sent for
                    admin approval before being included in GL calculations.
                  </p>
                </div>
              )}

              {/* Buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                >
                  Submit Entry for Approval
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="px-4 py-2 bg-gray-300 text-gray-800 rounded-lg hover:bg-gray-400 transition"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Entries Table */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          {loading && entries.length === 0 ? (
            <div className="p-8 text-center">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <p className="mt-2 text-gray-600">Loading entries...</p>
            </div>
          ) : entries.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              <p>No {activeTab === 'pending' ? 'pending' : ''} entries found</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-100 border-b border-gray-300">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                      Date
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                      Account Code
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                      Account Name
                    </th>
                    <th className="px-4 py-3 text-center text-sm font-semibold text-gray-900">
                      Source
                    </th>
                    <th className="px-4 py-3 text-center text-sm font-semibold text-gray-900">
                      Period
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                      Reason
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                      Description
                    </th>
                    <th className="px-4 py-3 text-right text-sm font-semibold text-gray-900">
                      Amount
                    </th>
                    <th className="px-4 py-3 text-center text-sm font-semibold text-gray-900">
                      Type
                    </th>
                    <th className="px-4 py-3 text-center text-sm font-semibold text-gray-900">
                      Status
                    </th>
                    <th className="px-4 py-3 text-center text-sm font-semibold text-gray-900">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => (
                    <tr key={entry.id} className="border-b border-gray-200 hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm text-gray-900">
                        {new Date(entry.entryDate).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3 text-sm font-mono text-gray-900">
                        {entry.glAccountCode}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900">
                        {entry.glAccountName}
                      </td>
                      <td className="px-4 py-3 text-sm text-center">
                        <span className="px-2 py-1 rounded text-xs font-medium bg-slate-100 text-slate-800">
                          {entry.entrySource === 'PERIOD_ENTRY' ? 'Period Entry' : 'Manual Entry'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-gray-600">
                        {entry.periodMonth && entry.periodYear
                          ? `${entry.periodMonth}/${entry.periodYear}`
                          : '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        {entry.entryReason}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600 max-w-xs truncate">
                        {entry.description}
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-gray-900 font-medium">
                        KES{' '}
                        {entry.amount.toLocaleString('en-US', {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}
                      </td>
                      <td className="px-4 py-3 text-sm text-center">
                        <span
                          className={`px-2 py-1 rounded text-xs font-medium ${
                            entry.isDebit
                              ? 'bg-red-100 text-red-800'
                              : 'bg-green-100 text-green-800'
                          }`}
                        >
                          {entry.isDebit ? 'Debit' : 'Credit'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center">
                        <span
                          className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                            getDisplayStatus(entry)
                          )}`}
                        >
                          {getDisplayStatus(entry)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-center">
                        <div className="flex justify-center gap-2">
                          {isAdmin &&
                            ((entry.entrySource === 'MANUAL_ENTRY' &&
                              getDisplayStatus(entry) === 'PENDING') ||
                              (entry.entrySource === 'PERIOD_ENTRY' &&
                                getDisplayStatus(entry) === 'POSTED')) && (
                            <>
                              <button
                                onClick={() => handleApproveEntry(entry)}
                                disabled={loading}
                                className="p-1 hover:bg-green-100 rounded transition disabled:opacity-50"
                                title="Approve"
                              >
                                <CheckCircle
                                  size={16}
                                  className="text-green-600"
                                />
                              </button>
                              <button
                                onClick={() => handleRejectEntry(entry)}
                                disabled={loading}
                                className="p-1 hover:bg-red-100 rounded transition disabled:opacity-50"
                                title="Reject"
                              >
                                <XCircle size={16} className="text-red-600" />
                              </button>
                            </>
                          )}
                          {isTreasurer &&
                            entry.entrySource === 'MANUAL_ENTRY' &&
                            getDisplayStatus(entry) === 'PENDING' && (
                              <button
                                onClick={() => handleDeleteEntry(entry.id)}
                                disabled={loading}
                                className="p-1 hover:bg-red-100 rounded transition disabled:opacity-50"
                                title="Delete"
                              >
                                <Trash2 size={16} className="text-red-600" />
                              </button>
                            )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Footer Info */}
        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <p className="text-sm text-gray-700">
            <strong>Workflow:</strong> Manual entries use the direct approval flow,
            while period entries move from `DRAFT` to `POSTED` before admin approval.
            Approved entries are then included in GL calculations and reports.
          </p>
        </div>
      </div>
    </div>
  );
}
