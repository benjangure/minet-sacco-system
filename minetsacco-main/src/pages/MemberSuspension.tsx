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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [suspensionReason, setSuspensionReason] = useState('');
  const [showSuspendForm, setShowSuspendForm] = useState(false);

  // Check if user has permission to suspend members
  const canSuspend = role && ['admin', 'credit_committee'].includes(role.toLowerCase());

  useEffect(() => {
    fetchActiveSuspensions();
  }, []);

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

  const handleSuspendMember = async () => {
    if (!canSuspend) {
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

  const handleLiftSuspension = async (memberId: number) => {
    if (!canSuspend) {
      setError('You do not have permission to lift suspensions');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post(`/members/${memberId}/lift-suspension`);

      if (response.data.success) {
        setSuccess('Suspension lifted successfully');
        setSuspensions(suspensions.filter(s => s.member.id !== memberId));
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to lift suspension');
    } finally {
      setLoading(false);
    }
  };

  if (!canSuspend) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Member Suspension Management</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
          <AlertCircle className="w-6 h-6 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-red-800 font-semibold">Access Denied</p>
            <p className="text-red-700 text-sm mt-1">Only Admin and Credit Committee members can manage member suspensions.</p>
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
        <button
          onClick={() => setShowSuspendForm(!showSuspendForm)}
          className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 flex items-center gap-2"
        >
          <Ban className="w-4 h-4" />
          Suspend Member
        </button>

        {showSuspendForm && (
          <div className="mt-4 p-4 border border-gray-200 rounded-lg">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Member ID</label>
              <input
                type="number"
                value={selectedMemberId || ''}
                onChange={(e) => setSelectedMemberId(e.target.value ? parseInt(e.target.value) : null)}
                placeholder="Enter member ID"
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
      </div>

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
    </div>
  );
}
