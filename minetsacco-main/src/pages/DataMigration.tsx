import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Upload, Loader, Download, Eye, Check, X } from 'lucide-react';
import api from '../config/api';
import { useAuth } from '../contexts/AuthContext';

interface MigrationBatch {
  id: number;
  uploadedBy: { firstName: string; lastName: string };
  uploadedAt: string;
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  verificationStatus: string;
  approvalStatus: string;
  migrationExecuted: boolean;
  executedAt?: string;
  errorMessage?: string;
  verifiedBy?: { firstName: string; lastName: string };
  verifiedAt?: string;
  verificationNotes?: string;
}

export default function DataMigration() {
  const { role } = useAuth();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [batch, setBatch] = useState<MigrationBatch | null>(null);
  const [pendingBatches, setPendingBatches] = useState<MigrationBatch[]>([]);
  const [selectedBatch, setSelectedBatch] = useState<MigrationBatch | null>(null);
  const [approvalNotes, setApprovalNotes] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [showApprovalForm, setShowApprovalForm] = useState(false);
  const [showRejectionForm, setShowRejectionForm] = useState(false);

  const isTreasurer = role && role.toLowerCase() === 'treasurer';
  const isAdmin = role && role.toLowerCase() === 'admin';

  useEffect(() => {
    if (isAdmin) {
      fetchPendingBatches();
    }
  }, [isAdmin]);

  const fetchPendingBatches = async () => {
    try {
      const response = await api.get('/admin/migration/pending');
      if (response.data.success) {
        setPendingBatches(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load pending batches');
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      if (!selectedFile.name.endsWith('.xlsx')) {
        setError('Please select an Excel file (.xlsx)');
        return;
      }
      setFile(selectedFile);
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.post('/admin/migration/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (response.data.success) {
        setBatch(response.data.data);
        setSuccess('File uploaded and validated successfully');
        setFile(null);
      } else {
        setError(response.data.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Upload failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await api.get('/admin/migration/template/download', {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'migration_template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.parentElement?.removeChild(link);
    } catch (err: any) {
      setError('Failed to download template');
    }
  };

  const handleDownloadBatch = async (batchId: number) => {
    try {
      const response = await api.get(`/admin/migration/${batchId}/download`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `migration_batch_${batchId}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentElement?.removeChild(link);
    } catch (err: any) {
      setError('Failed to download batch');
    }
  };

  const handlePreviewBatch = async (batchId: number) => {
    try {
      const response = await api.get(`/admin/migration/${batchId}/preview`);
      if (response.data.success) {
        setSelectedBatch(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to preview batch');
    }
  };

  const handleApproveBatch = async (batchId: number) => {
    setLoading(true);
    try {
      const response = await api.post(`/admin/migration/${batchId}/approve`, {
        notes: approvalNotes,
      });

      if (response.data.success) {
        setSuccess('Batch approved successfully');
        setPendingBatches(pendingBatches.filter(b => b.id !== batchId));
        setShowApprovalForm(false);
        setApprovalNotes('');
        setSelectedBatch(null);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve batch');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectBatch = async (batchId: number) => {
    if (!rejectionReason.trim()) {
      setError('Please provide a rejection reason');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post(`/admin/migration/${batchId}/reject`, {
        reason: rejectionReason,
      });

      if (response.data.success) {
        setSuccess('Batch rejected successfully');
        setPendingBatches(pendingBatches.filter(b => b.id !== batchId));
        setShowRejectionForm(false);
        setRejectionReason('');
        setSelectedBatch(null);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject batch');
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteMigration = async (batchId: number) => {
    setLoading(true);
    try {
      const response = await api.post(`/admin/migration/${batchId}/execute`);

      if (response.data.success) {
        setSuccess('Migration executed successfully');
        setPendingBatches(pendingBatches.filter(b => b.id !== batchId));
        setSelectedBatch(null);
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to execute migration');
    } finally {
      setLoading(false);
    }
  };

  if (!isTreasurer && !isAdmin) {
    return (
      <div className="p-6 max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Data Migration</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
          <AlertCircle className="w-6 h-6 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-red-800 font-semibold">Access Denied</p>
            <p className="text-red-700 text-sm mt-1">Only Treasurer and Admin can access data migration.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Data Migration - Maker & Checker Workflow</h1>

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

      {/* TREASURER SECTION - MAKER */}
      {isTreasurer && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <h2 className="text-xl font-semibold">Treasurer - Upload Migration File</h2>
            <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded">MAKER</span>
          </div>

          <div className="mb-4">
            <button
              onClick={handleDownloadTemplate}
              className="bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 flex items-center gap-2 mb-4"
            >
              <Download className="w-4 h-4" />
              Download Template
            </button>
            <p className="text-sm text-gray-600">Download the template to see the expected format</p>
          </div>

          <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center mb-4">
            <Upload className="w-12 h-12 text-gray-400 mx-auto mb-3" />
            <input
              type="file"
              accept=".xlsx"
              onChange={handleFileSelect}
              className="hidden"
              id="file-input"
            />
            <label htmlFor="file-input" className="cursor-pointer">
              <p className="text-gray-600">
                {file ? file.name : 'Click to select Excel file or drag and drop'}
              </p>
            </label>
          </div>

          <button
            onClick={handleUpload}
            disabled={!file || loading}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-400"
          >
            {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
            Upload & Validate File
          </button>

          {batch && (
            <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <h3 className="font-semibold mb-3">Upload Summary</h3>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-gray-600">Total Records</p>
                  <p className="font-semibold">{batch.totalRecords}</p>
                </div>
                <div>
                  <p className="text-gray-600">Valid Records</p>
                  <p className="font-semibold text-green-600">{batch.successfulRecords}</p>
                </div>
                <div>
                  <p className="text-gray-600">Invalid Records</p>
                  <p className="font-semibold text-red-600">{batch.failedRecords}</p>
                </div>
                <div>
                  <p className="text-gray-600">Status</p>
                  <p className="font-semibold">{batch.verificationStatus}</p>
                </div>
              </div>
              {batch.errorMessage && (
                <div className="mt-3 p-2 bg-red-50 text-red-800 text-sm rounded">
                  {batch.errorMessage}
                </div>
              )}
              <p className="text-xs text-gray-600 mt-3">
                Batch ID: {batch.id} - Waiting for Admin approval
              </p>
            </div>
          )}
        </div>
      )}

      {/* ADMIN SECTION - CHECKER */}
      {isAdmin && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center gap-2 mb-4">
            <h2 className="text-xl font-semibold">Admin - Review & Approve Migrations</h2>
            <span className="px-2 py-1 bg-purple-100 text-purple-800 text-xs rounded">CHECKER</span>
          </div>

          {pendingBatches.length === 0 ? (
            <p className="text-gray-600 text-center py-8">No pending migrations for approval</p>
          ) : (
            <div className="space-y-4">
              {pendingBatches.map((pendingBatch) => (
                <div key={pendingBatch.id} className="border border-gray-200 rounded-lg p-4">
                  <div className="flex justify-between items-start mb-3">
                    <div>
                      <p className="font-semibold">Batch #{pendingBatch.id}</p>
                      <p className="text-sm text-gray-600">
                        Uploaded by: {pendingBatch.uploadedBy.firstName} {pendingBatch.uploadedBy.lastName}
                      </p>
                      <p className="text-sm text-gray-600">
                        {new Date(pendingBatch.uploadedAt).toLocaleString()}
                      </p>
                    </div>
                    <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded">
                      PENDING APPROVAL
                    </span>
                  </div>

                  <div className="grid grid-cols-4 gap-2 mb-3 text-sm">
                    <div className="bg-gray-50 p-2 rounded">
                      <p className="text-gray-600">Total</p>
                      <p className="font-semibold">{pendingBatch.totalRecords}</p>
                    </div>
                    <div className="bg-green-50 p-2 rounded">
                      <p className="text-gray-600">Valid</p>
                      <p className="font-semibold text-green-600">{pendingBatch.successfulRecords}</p>
                    </div>
                    <div className="bg-red-50 p-2 rounded">
                      <p className="text-gray-600">Invalid</p>
                      <p className="font-semibold text-red-600">{pendingBatch.failedRecords}</p>
                    </div>
                    <div className="bg-blue-50 p-2 rounded">
                      <p className="text-gray-600">Status</p>
                      <p className="font-semibold">{pendingBatch.verificationStatus}</p>
                    </div>
                  </div>

                  <div className="flex gap-2 mb-3">
                    <button
                      onClick={() => handlePreviewBatch(pendingBatch.id)}
                      className="flex-1 bg-gray-600 text-white px-3 py-2 rounded text-sm hover:bg-gray-700 flex items-center justify-center gap-1"
                    >
                      <Eye className="w-4 h-4" />
                      Preview
                    </button>
                    <button
                      onClick={() => handleDownloadBatch(pendingBatch.id)}
                      className="flex-1 bg-gray-600 text-white px-3 py-2 rounded text-sm hover:bg-gray-700 flex items-center justify-center gap-1"
                    >
                      <Download className="w-4 h-4" />
                      Download
                    </button>
                    <button
                      onClick={() => {
                        setSelectedBatch(pendingBatch);
                        setShowApprovalForm(true);
                      }}
                      className="flex-1 bg-green-600 text-white px-3 py-2 rounded text-sm hover:bg-green-700 flex items-center justify-center gap-1"
                    >
                      <Check className="w-4 h-4" />
                      Approve
                    </button>
                    <button
                      onClick={() => {
                        setSelectedBatch(pendingBatch);
                        setShowRejectionForm(true);
                      }}
                      className="flex-1 bg-red-600 text-white px-3 py-2 rounded text-sm hover:bg-red-700 flex items-center justify-center gap-1"
                    >
                      <X className="w-4 h-4" />
                      Reject
                    </button>
                  </div>

                  {selectedBatch?.id === pendingBatch.id && showApprovalForm && (
                    <div className="bg-green-50 border border-green-200 rounded p-3 mb-3">
                      <label className="block text-sm font-medium text-gray-700 mb-2">Approval Notes</label>
                      <textarea
                        value={approvalNotes}
                        onChange={(e) => setApprovalNotes(e.target.value)}
                        placeholder="Add any notes for approval"
                        className="w-full px-3 py-2 border border-gray-300 rounded text-sm mb-2"
                        rows={2}
                      />
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleApproveBatch(pendingBatch.id)}
                          disabled={loading}
                          className="flex-1 bg-green-600 text-white px-3 py-2 rounded text-sm hover:bg-green-700 disabled:bg-gray-400"
                        >
                          {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
                          Confirm Approval
                        </button>
                        <button
                          onClick={() => {
                            setShowApprovalForm(false);
                            setApprovalNotes('');
                          }}
                          className="flex-1 bg-gray-300 text-gray-700 px-3 py-2 rounded text-sm hover:bg-gray-400"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}

                  {selectedBatch?.id === pendingBatch.id && showRejectionForm && (
                    <div className="bg-red-50 border border-red-200 rounded p-3 mb-3">
                      <label className="block text-sm font-medium text-gray-700 mb-2">Rejection Reason</label>
                      <textarea
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="Explain why this batch is being rejected"
                        className="w-full px-3 py-2 border border-gray-300 rounded text-sm mb-2"
                        rows={2}
                      />
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleRejectBatch(pendingBatch.id)}
                          disabled={loading}
                          className="flex-1 bg-red-600 text-white px-3 py-2 rounded text-sm hover:bg-red-700 disabled:bg-gray-400"
                        >
                          {loading ? <Loader className="w-4 h-4 animate-spin inline mr-2" /> : null}
                          Confirm Rejection
                        </button>
                        <button
                          onClick={() => {
                            setShowRejectionForm(false);
                            setRejectionReason('');
                          }}
                          className="flex-1 bg-gray-300 text-gray-700 px-3 py-2 rounded text-sm hover:bg-gray-400"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
