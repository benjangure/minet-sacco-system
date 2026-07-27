import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Loader, Search, Ban, RotateCcw } from 'lucide-react';
import api from '../config/api';
import { useAuth } from '../contexts/AuthContext';

interface Member {
  id: number;
  employeeId: string;
  firstName: string;
  lastName: string;
}

interface Suspension {
  id: number;
  member: Member;
  reason: string;
  suspendedBy: { firstName: string; lastName: string };
  suspendedAt: string;
  isActive: boolean;
  liftedAt?: string;
}

export default function MemberSuspension() {
  const { role } = useAuth();
  const [members, setMembers] = useState<Member[]>([]);
  const [suspensions, setSuspensions] = useState<Suspension[]>([]);
  const [pendingSuspensions, setPendingSuspensions] = useState<Suspension[]>([]);
  const [allSuspensions, setAllSuspensions] = useState<Suspension[]>([]);
  const [pendingReactivations, setPendingReactivations] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [suspensionReason, setSuspensionReason] = useState('');
  const [reactivationReason, setReactivationReason] = useState('');
  const [showSuspendForm, setShowSuspendForm] = useState(false);
  const [showReactivateForm, setShowReactivateForm] = useState(false);
  const [reactivateMemberId, setReactivateMemberId] = useState<string | null>(null);

  // Check if user has permission to manage member suspensions (Credit Committee can initiate, Treasurer can validate)
  const canManageSuspensions = role && ['credit_committee', 'treasurer'].includes(role.toLowerCase());
  const canInitiateSuspension = role && role.toLowerCase() === 'credit_committee';

  useEffect(() => {
    fetchActiveSuspensions();
    fetchAllSuspensions();
    if (role && role.toLowerCase() === 'treasurer') {
      fetchPendingSuspensions();
      fetchPendingReactivations();
    }
  }, [role]);

  const fetchActiveSuspensions = async () => {
    try {
      const response = await api.get('/members/suspensions/active');
      if (response.data.success) {
        setSuspensions(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load suspensions');
    }
  };

  const fetchPendingSuspensions = async () => {
    try {
      const response = await api.get('/members/suspensions/pending');
      if (response.data.success) {
        setPendingSuspensions(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load pending suspensions');
    }
  };

  const fetchAllSuspensions = async () => {
    try {
      const response = await api.get('/members/suspensions/all');
      if (response.data.success) {
        setAllSuspensions(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load all suspensions');
    }
  };

  const fetchPendingReactivations = async () => {
    try {
      const response = await api.get('/members/reactivations/pending');
      if (response.data.success) {
        setPendingReactivations(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load pending reactivations');
    }
  };

  const handleSuspendMember = async () => {
    if (!canManageSuspensions) {
      setError('You do not have permission to suspend members');
      return;
    }

    if (!selectedMemberId || !suspensionReason.trim()) {
      setError('Please select a member and provide a reason');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post(`/members/${selectedMemberId}/suspend`, {
        reason: suspensionReason,
      });

      if (response.data.success) {
        setSuccess('Member suspended successfully');
        setSuspensions([...suspensions, response.data.data]);
        setSelectedMemberId(null);
        setSuspensionReason('');
        setShowSuspendForm(false);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to suspend member');
    } finally {
      setLoading(false);
    }
  };

  const handleLiftSuspension = async (suspensionId: number) => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/suspensions/${suspensionId}/lift`);
      if (response.data.success) {
        setSuccess('Suspension lifted successfully');
        fetchActiveSuspensions();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to lift suspension');
    } finally {
      setLoading(false);
    }
  };

  const handleReactivateMember = async () => {
    if (!reactivateMemberId || !reactivationReason.trim()) {
      setError('Please provide a reactivation reason');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/${reactivateMemberId}/reactivate`, {
        reason: reactivationReason
      });
      if (response.data.success) {
        setSuccess('Reactivation initiated successfully. Pending Treasurer approval.');
        setShowReactivateForm(false);
        setReactivateMemberId(null);
        setReactivationReason('');
        if (role && role.toLowerCase() === 'treasurer') {
          fetchPendingReactivations();
        }
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to initiate reactivation');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveSuspension = async (suspensionId: number) => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/suspension/${suspensionId}/validate`, {
        validationNotes: 'Approved by Treasurer'
      });
      if (response.data.success) {
        setSuccess('Suspension approved successfully');
        fetchPendingSuspensions();
        fetchActiveSuspensions();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve suspension');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectSuspension = async (suspensionId: number) => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/suspension/${suspensionId}/validate`, {
        validationNotes: 'Rejected by Treasurer'
      });
      if (response.data.success) {
        setSuccess('Suspension rejected successfully');
        fetchPendingSuspensions();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject suspension');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveReactivation = async (reactivationId: number) => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/reactivation/${reactivationId}/validate`, {
        validationNotes: 'Approved by Treasurer'
      });
      if (response.data.success) {
        setSuccess('Reactivation approved successfully');
        fetchPendingReactivations();
        fetchActiveSuspensions();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve reactivation');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectReactivation = async (reactivationId: number) => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await api.post(`/members/reactivation/${reactivationId}/validate`, {
        validationNotes: 'Rejected by Treasurer'
      });
      if (response.data.success) {
        setSuccess('Reactivation rejected successfully');
        fetchPendingReactivations();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject reactivation');
    } finally {
      setLoading(false);
    }
  };

  if (!canManageSuspensions) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Member Suspension Management</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
          <AlertCircle className="w-6 h-6 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-red-800 font-semibold">Access Denied</p>
            <p className="text-red-700 text-sm mt-1">Only Credit Committee and Treasurer members can manage member suspensions.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Member Suspension Management</h1>

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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600 text-sm">Active Suspensions</p>
          <p className="text-3xl font-bold text-red-600">{suspensions.length}</p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex gap-2">
          {canInitiateSuspension && (
            <button
              onClick={() => setShowSuspendForm(!showSuspendForm)}
              className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 flex items-center gap-2"
            >
              <Ban className="w-4 h-4" />
              Suspend Member
            </button>
          )}
          {canInitiateSuspension && (
            <button
              onClick={() => setShowReactivateForm(!showReactivateForm)}
              className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center gap-2"
            >
              <RotateCcw className="w-4 h-4" />
              Reactivate Member
            </button>
          )}
        </div>

        {showSuspendForm && (
          <div className="mt-4 p-4 border border-gray-200 rounded-lg">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Member ID</label>
              <input
                type="text"
                value={selectedMemberId || ''}
                onChange={(e) => setSelectedMemberId(e.target.value || null)}
                placeholder="Enter member ID (e.g., EMP001)"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Reason for Suspension</label>
              <textarea
                value={suspensionReason}
                onChange={(e) => setSuspensionReason(e.target.value)}
                placeholder="Enter suspension reason"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleSuspendMember}
                disabled={loading}
                className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:bg-gray-400"
              >
                {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
                Confirm Suspension
              </button>
              <button
                onClick={() => setShowSuspendForm(false)}
                className="bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {showReactivateForm && (
          <div className="mt-4 p-4 border border-gray-200 rounded-lg">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Member ID</label>
              <input
                type="text"
                value={reactivateMemberId || ''}
                onChange={(e) => setReactivateMemberId(e.target.value)}
                placeholder="Enter member ID (e.g., EMP001)"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Reason for Reactivation</label>
              <textarea
                value={reactivationReason}
                onChange={(e) => setReactivationReason(e.target.value)}
                placeholder="Enter reactivation reason"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleReactivateMember}
                disabled={loading || !reactivateMemberId}
                className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 disabled:bg-gray-400"
              >
                {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
                Initiate Reactivation
              </button>
              <button
                onClick={() => setShowReactivateForm(false)}
                className="bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Pending Suspensions for Treasurer Approval */}
      {role && role.toLowerCase() === 'treasurer' && pendingSuspensions.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden mb-6">
          <div className="p-6 border-b bg-yellow-50">
            <h2 className="text-xl font-semibold text-yellow-800">Pending Suspensions (Awaiting Your Approval)</h2>
          </div>

          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Employee ID</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Initiated By</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {pendingSuspensions.map((suspension) => (
                <tr key={suspension.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {suspension.member.firstName} {suspension.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.member.employeeId}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.reason}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {suspension.suspendedBy.firstName} {suspension.suspendedBy.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {new Date(suspension.suspendedAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-sm flex gap-2">
                    <button
                      onClick={() => handleApproveSuspension(suspension.id)}
                      disabled={loading}
                      className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700 disabled:bg-gray-400"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => handleRejectSuspension(suspension.id)}
                      disabled={loading}
                      className="bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700 disabled:bg-gray-400"
                    >
                      Reject
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pending Reactivations for Treasurer Approval */}
      {role && role.toLowerCase() === 'treasurer' && pendingReactivations.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden mt-6">
          <div className="p-6 border-b bg-green-50">
            <h2 className="text-xl font-semibold text-green-800">Pending Reactivations (Awaiting Your Approval)</h2>
          </div>

          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Employee ID</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Initiated By</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {pendingReactivations.map((reactivation) => (
                <tr key={reactivation.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {reactivation.member.firstName} {reactivation.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{reactivation.member.employeeId}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">{reactivation.reason}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {reactivation.initiatedBy.firstName} {reactivation.initiatedBy.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {new Date(reactivation.initiatedAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-sm flex gap-2">
                    <button
                      onClick={() => handleApproveReactivation(reactivation.id)}
                      disabled={loading}
                      className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700 disabled:bg-gray-400"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => handleRejectReactivation(reactivation.id)}
                      disabled={loading}
                      className="bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700 disabled:bg-gray-400"
                    >
                      Reject
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="p-6 border-b">
          <h2 className="text-xl font-semibold">Active Suspensions</h2>
        </div>

        {suspensions.length === 0 ? (
          <div className="p-6 text-center text-gray-500">
            No active suspensions
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Employee ID</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Suspended By</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {suspensions.map((suspension) => (
                <tr key={suspension.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {suspension.member.firstName} {suspension.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.member.employeeId}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.reason}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {suspension.suspendedBy.firstName} {suspension.suspendedBy.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {new Date(suspension.suspendedAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <button
                      onClick={() => handleLiftSuspension(suspension.member.id)}
                      disabled={loading}
                      className="text-green-600 hover:text-green-700 disabled:text-gray-400 flex items-center gap-1"
                    >
                      <RotateCcw className="w-4 h-4" />
                      Lift
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* All Suspensions Report */}
      <div className="bg-white rounded-lg shadow overflow-hidden mt-6">
        <div className="p-6 border-b bg-blue-50">
          <h2 className="text-xl font-semibold text-blue-800">All Suspensions Report</h2>
        </div>

        {allSuspensions.length === 0 ? (
          <div className="p-6 text-center text-gray-500">
            No suspensions found
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Member</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Employee ID</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Reason</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Suspended By</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {allSuspensions.map((suspension) => (
                <tr key={suspension.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    {suspension.member.firstName} {suspension.member.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.member.employeeId}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">{suspension.reason}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {suspension.suspendedBy.firstName} {suspension.suspendedBy.lastName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {new Date(suspension.suspendedAt).toLocaleDateString()}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <span className={`px-2 py-1 rounded ${
                      suspension.isActive ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {suspension.isActive ? 'Active' : 'Lifted'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
