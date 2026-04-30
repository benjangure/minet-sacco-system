import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Loader, LogOut, CheckCheck } from 'lucide-react';
import api from '../config/api';
import { useAuth } from '../contexts/AuthContext';

interface ExitRecord {
  id: number;
  member: { id: number; employeeId: string; firstName: string; lastName: string };
  exitReason: string;
  savingsBalance: number;
  outstandingLoan: number;
  loanDeduction: number;
  remainingPayout: number;
  sharesRefund: number;
  totalPayout: number;
  approvedBy?: { firstName: string; lastName: string };
  approvedAt?: string;
  createdAt: string;
}

interface ExitSummary {
  memberId: number;
  memberName: string;
  savingsBalance: number;
  outstandingLoan: number;
  loanDeduction: number;
  remainingPayout: number;
  sharesRefund: number;
  totalPayout: number;
  isActiveGuarantor: boolean;
  activeGuarantorCount: number;
}

export default function MemberExit() {
  const { role } = useAuth();
  const [pendingExits, setPendingExits] = useState<ExitRecord[]>([]);
  const [approvedExits, setApprovedExits] = useState<ExitRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showInitiateForm, setShowInitiateForm] = useState(false);
  const [memberId, setMemberId] = useState('');
  const [exitReason, setExitReason] = useState('');
  const [exitSummary, setExitSummary] = useState<ExitSummary | null>(null);

  // Check permissions
  const canInitiate = role && ['admin', 'credit_committee'].includes(role.toLowerCase());
  const canApprove = role && role.toLowerCase() === 'treasurer';

  useEffect(() => {
    fetchExits();
  }, []);

  const fetchExits = async () => {
    try {
      const [pendingRes, approvedRes] = await Promise.all([
        api.get('/members/exits/pending'),
        api.get('/members/exits/approved'),
      ]);

      if (pendingRes.data.success) setPendingExits(pendingRes.data.data);
      if (approvedRes.data.success) setApprovedExits(approvedRes.data.data);
    } catch (err: any) {
      setError('Failed to load exits');
    } finally {
      setLoading(false);
    }
  };

  const handleCalculateSummary = async () => {
    if (!memberId) {
      setError('Please enter member ID');
      return;
    }

    try {
      const response = await api.get(`/members/${memberId}/exit/summary`);
      if (response.data.success) {
        setExitSummary(response.data.data);
        setError('');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to calculate summary');
      setExitSummary(null);
    }
  };

  const handleInitiateExit = async () => {
    if (!canInitiate) {
      setError('You do not have permission to initiate member exits');
      return;
    }

    if (!memberId || !exitReason.trim()) {
      setError('Please enter member ID and exit reason');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post(`/members/${memberId}/exit`, {
        exitReason,
      });

      if (response.data.success) {
        setSuccess('Member exit initiated successfully');
        setPendingExits([...pendingExits, response.data.data]);
        setMemberId('');
        setExitReason('');
        setExitSummary(null);
        setShowInitiateForm(false);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to initiate exit');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveExit = async (exitId: number) => {
    if (!canApprove) {
      setError('You do not have permission to approve exits');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post(`/members/exit/${exitId}/approve`);

      if (response.data.success) {
        setSuccess('Member exit approved successfully');
        setPendingExits(pendingExits.filter(e => e.id !== exitId));
        setApprovedExits([...approvedExits, response.data.data]);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve exit');
    } finally {
      setLoading(false);
    }
  };

  if (loading && pendingExits.length === 0 && approvedExits.length === 0) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!canInitiate && !canApprove) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Member Exit Management</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
          <AlertCircle className="w-6 h-6 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-red-800 font-semibold">Access Denied</p>
            <p className="text-red-700 text-sm mt-1">Only Admin, Credit Committee, and Treasurer can manage member exits.</p>
          </div>
        </div>
      </div>
    );
  }

  if (loading && pendingExits.length === 0 && approvedExits.length === 0) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Member Exit Management</h1>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {success && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
          <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
          <p className="text-green-800">{success}</p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600 text-sm">Pending Approvals</p>
          <p className="text-3xl font-bold text-yellow-600">{pendingExits.length}</p>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600 text-sm">Approved Exits</p>
          <p className="text-3xl font-bold text-green-600">{approvedExits.length}</p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        {canInitiate ? (
          <>
            <button
              onClick={() => setShowInitiateForm(!showInitiateForm)}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center gap-2"
            >
              <LogOut className="w-4 h-4" />
              Initiate Member Exit
            </button>

            {showInitiateForm && (
              <div className="mt-4 p-4 border border-gray-200 rounded-lg">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Member ID</label>
              <input
                type="number"
                value={memberId}
                onChange={(e) => setMemberId(e.target.value)}
                placeholder="Enter member ID"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            <button
              onClick={handleCalculateSummary}
              disabled={!memberId}
              className="w-full bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 disabled:bg-gray-400 mb-4"
            >
              Calculate Exit Summary
            </button>

            {exitSummary && (
              <div className="mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h3 className="font-semibold mb-3">Exit Summary for {exitSummary.memberName}</h3>
                <div className="grid grid-cols-2 gap-3 text-sm mb-3">
                  <div>
                    <p className="text-gray-600">Savings Balance</p>
                    <p className="font-semibold">KES {exitSummary.savingsBalance.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Outstanding Loan</p>
                    <p className="font-semibold">KES {exitSummary.outstandingLoan.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Loan Deduction</p>
                    <p className="font-semibold">KES {exitSummary.loanDeduction.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Remaining Payout</p>
                    <p className="font-semibold">KES {exitSummary.remainingPayout.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Shares Refund</p>
                    <p className="font-semibold">KES {exitSummary.sharesRefund.toLocaleString()}</p>
                  </div>
                  <div className="bg-green-50 p-2 rounded">
                    <p className="text-gray-600">Total Payout</p>
                    <p className="font-bold text-green-600">KES {exitSummary.totalPayout.toLocaleString()}</p>
                  </div>
                </div>

                {exitSummary.isActiveGuarantor && (
                  <div className="p-3 bg-red-50 border border-red-200 rounded text-red-800 text-sm mb-3">
                    ⚠️ Member is an active guarantor for {exitSummary.activeGuarantorCount} loan(s). Cannot exit until guarantor roles are replaced.
                  </div>
                )}
              </div>
            )}

            {exitSummary && !exitSummary.isActiveGuarantor && (
              <>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">Exit Reason</label>
                  <select
                    value={exitReason}
                    onChange={(e) => setExitReason(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  >
                    <option value="">Select reason</option>
                    <option value="RETIREMENT">Retirement</option>
                    <option value="RESIGNATION">Resignation</option>
                    <option value="TERMINATION">Termination</option>
                    <option value="DECEASED">Deceased</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={handleInitiateExit}
                    disabled={loading || !exitReason}
                    className="flex-1 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-400"
                  >
                    {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
                    Confirm Exit
                  </button>
                  <button
                    onClick={() => {
                      setShowInitiateForm(false);
                      setMemberId('');
                      setExitReason('');
                      setExitSummary(null);
                    }}
                    className="flex-1 bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400"
                  >
                    Cancel
                  </button>
                </div>
              </>
            )}
              </div>
            )}
          </>
        ) : (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-yellow-600 mt-0.5 flex-shrink-0" />
            <p className="text-yellow-800 text-sm">Only Admin and Credit Committee can initiate member exits.</p>
          </div>
        )}
      </div>

      {pendingExits.length > 0 && (
        <div className="bg-white rounded-lg shadow mb-6 overflow-hidden">
          <div className="p-6 border-b bg-yellow-50">
            <h2 className="text-xl font-semibold">Pending Approvals</h2>
          </div>
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Total Payout</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {pendingExits.map((exit) => (
                <tr key={exit.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {exit.member.firstName} {exit.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{exit.exitReason}</td>
                  <td className="px-6 py-4 text-sm font-semibold text-green-600">
                    KES {exit.totalPayout.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {new Date(exit.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <button
                      onClick={() => handleApproveExit(exit.id)}
                      disabled={loading}
                      className="text-green-600 hover:text-green-700 disabled:text-gray-400 flex items-center gap-1"
                    >
                      <CheckCheck className="w-4 h-4" />
                      Approve
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {approvedExits.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="p-6 border-b bg-green-50">
            <h2 className="text-xl font-semibold">Approved Exits</h2>
          </div>
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Total Payout</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Approved By</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {approvedExits.map((exit) => (
                <tr key={exit.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {exit.member.firstName} {exit.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{exit.exitReason}</td>
                  <td className="px-6 py-4 text-sm font-semibold text-green-600">
                    KES {exit.totalPayout.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {exit.approvedBy ? `${exit.approvedBy.firstName} ${exit.approvedBy.lastName}` : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {exit.approvedAt ? new Date(exit.approvedAt).toLocaleDateString() : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
