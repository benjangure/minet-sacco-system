import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Upload, Loader, Download, Eye, Check, X, FileText } from 'lucide-react';
import api from '../config/api';
import { useAuth } from '../contexts/AuthContext';

interface MigrationBatch {
  id: number;
  uploadedBy: string;
  uploadedAt: string;
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  verificationStatus: string;
  approvalStatus: string;
  errorMessage?: string;
  verifiedBy?: string;
  verifiedAt?: string;
}

export default function DataMigrationMakerChecker() {
  const { role } = useAuth();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [batch, setBatch] = useState<MigrationBatch | null>(null);
  const [pendingBatches, setPendingBatches] = useState<MigrationBatch[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState<'upload' | 'pending' | 'template'>('upload');
  const [approvalNotes, setApprovalNotes] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [selectedBatch, setSelectedBatch] = useState<MigrationBatch | null>(null);

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
        setSuccess('File uploaded and validated successfully. Waiting for admin approval.');
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

  const handleDownload = async (batchId: number) => {
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
      setError('Failed to download file');
    }
  };

  const handlePreview = async (batchId: number) => {
    try {
      const response = await api.get(`/admin/migration/${batchId}/preview`);
      if (response.data.success) {
        setSelectedBatch(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load preview');
    }
  };

  const handleApprove = async (batchId: number) => {
    setLoading(true);
    try {
      const response = await api.post(`/admin/migration/${batchId}/approve`, {
        notes: approvalNotes,
      });

      if (response.data.success) {
        setSuccess('Batch approved successfully');
        setApprovalNotes('');
        fetchPendingBatches();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve batch');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (batchId: number) => {
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
        setRejectionReason('');
        fetchPendingBatches();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reject batch');
    } finally {
      setLoading(false);
    }
  };

  const handleExecute = async (batchId: number) => {
    setLoading(true);
    try {
      const response = await api.post(`/admin/migration/${batchId}/execute`);

      if (response.data.success) {
        setSuccess('Migration executed successfully');
        fetchPendingBatches();
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
      <h1 className="text-3xl font-bold mb-6">Data Migration - Maker/Checker Workflow</h1>

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

      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200">
        <div className="flex gap-4">
          {isTreasurer && (
            <>
              <button
                onClick={() => setActiveTab('upload')}
                className={`px-4 py-2 font-medium border-b-2 ${
                  activeTab === 'upload'
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-600 hover:text-gray-900'
                }`}
              >
                Upload File
              </button>
              <button
                onClick={() => setActiveTab('template')}
                className={`px-4 py-2 font-medium border-b-2 ${
                  activeTab === 'template'
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-600 hover:text-gray-900'
                }`}
              >
                Download Template
              </button>
            </>
          )}
          {isAdmin && (
            <button
              onClick={() => setActiveTab('pending')}
              className={`px-4 py-2 font-medium border-b-2 ${
                activeTab === 'pending'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-600 hover:text-gray-900'
              }`}
            >
              Pending Approvals ({pendingBatches.length})
            </button>
          )}
        </div>
      </div>

      {/* Upload Tab (Treasurer) */}
      {activeTab === 'upload' && isTreasurer && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Upload Migration File</h2>

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
              Upload File
            </button>
          </div>

          {batch && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Upload Summary</h2>

              <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-gray-50 p-4 rounded">
                  <p className="text-gray-600 text-sm">Total Records</p>
                  <p className="text-2xl font-bold">{batch.totalRecords}</p>
                </div>
                <div className="bg-green-50 p-4 rounded">
                  <p className="text-gray-600 text-sm">Valid Records</p>
                  <p className="text-2xl font-bold text-green-600">{batch.successfulRecords}</p>
                </div>
                <div className="bg-red-50 p-4 rounded">
                  <p className="text-gray-600 text-sm">Invalid Records</p>
                  <p className="text-2xl font-bold text-red-600">{batch.failedRecords}</p>
                </div>
                <div className="bg-yellow-50 p-4 rounded">
                  <p className="text-gray-600 text-sm">Status</p>
                  <p className="text-lg font-bold text-yellow-600">Awaiting Admin Approval</p>
                </div>
              </div>

              {batch.errorMessage && (
                <div className="bg-red-50 p-4 rounded text-sm text-red-800">
                  <p className="font-semibold mb-2">Validation Errors:</p>
                  <p>{batch.errorMessage}</p>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Template Tab (Treasurer) */}
      {activeTab === 'template' && isTreasurer && (
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">Migration File Template</h2>

          <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-blue-800 text-sm">
              Download the template below to see the required format for the migration file.
            </p>
          </div>

          <button
            onClick={() => {
              // In production, this would download an actual template file
              alert('Template download would be implemented here');
            }}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            Download Template
          </button>

          <div className="mt-8">
            <h3 className="font-semibold mb-4">Required Columns:</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left">Column</th>
                    <th className="px-4 py-2 text-left">Description</th>
                    <th className="px-4 py-2 text-left">Example</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  <tr>
                    <td className="px-4 py-2">Employee ID</td>
                    <td className="px-4 py-2">Unique member identifier</td>
                    <td className="px-4 py-2">EMP001</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Months Contributed</td>
                    <td className="px-4 py-2">Number of months contributed</td>
                    <td className="px-4 py-2">12</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Loan Number</td>
                    <td className="px-4 py-2">Loan identifier (optional)</td>
                    <td className="px-4 py-2">LOAN001</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Outstanding Balance</td>
                    <td className="px-4 py-2">Loan outstanding amount</td>
                    <td className="px-4 py-2">50000.00</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Loan Start Date</td>
                    <td className="px-4 py-2">Date loan was disbursed</td>
                    <td className="px-4 py-2">2024-01-15</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Guarantor IDs</td>
                    <td className="px-4 py-2">Employee IDs of guarantors</td>
                    <td className="px-4 py-2">EMP002, EMP003</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2">Guarantee Amounts</td>
                    <td className="px-4 py-2">Amounts guaranteed by each</td>
                    <td className="px-4 py-2">25000, 25000</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Pending Approvals Tab (Admin) */}
      {activeTab === 'pending' && isAdmin && (
        <div className="space-y-6">
          {pendingBatches.length === 0 ? (
            <div className="bg-white rounded-lg shadow p-6 text-center text-gray-500">
              No pending batches for approval
            </div>
          ) : (
            pendingBatches.map((batch) => (
              <div key={batch.id} className="bg-white rounded-lg shadow p-6">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="font-semibold text-lg">Batch #{batch.id}</h3>
                    <p className="text-sm text-gray-600">
                      Uploaded by {batch.uploadedBy} on {new Date(batch.uploadedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <span className="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">
                    Pending Review
                  </span>
                </div>

                <div className="grid grid-cols-4 gap-4 mb-6">
                  <div className="bg-gray-50 p-3 rounded">
                    <p className="text-gray-600 text-xs">Total Records</p>
                    <p className="text-xl font-bold">{batch.totalRecords}</p>
                  </div>
                  <div className="bg-green-50 p-3 rounded">
                    <p className="text-gray-600 text-xs">Valid</p>
                    <p className="text-xl font-bold text-green-600">{batch.successfulRecords}</p>
                  </div>
                  <div className="bg-red-50 p-3 rounded">
                    <p className="text-gray-600 text-xs">Invalid</p>
                    <p className="text-xl font-bold text-red-600">{batch.failedRecords}</p>
                  </div>
                  <div className="bg-blue-50 p-3 rounded">
                    <p className="text-gray-600 text-xs">Status</p>
                    <p className="text-xl font-bold text-blue-600">{batch.verificationStatus}</p>
                  </div>
                </div>

                <div className="flex gap-2 mb-4">
                  <button
                    onClick={() => handleDownload(batch.id)}
                    className="flex-1 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center justify-center gap-2"
                  >
                    <Download className="w-4 h-4" />
                    Download File
                  </button>
                  <button
                    onClick={() => handlePreview(batch.id)}
                    className="flex-1 bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 flex items-center justify-center gap-2"
                  >
                    <Eye className="w-4 h-4" />
                    Preview Data
                  </button>
                </div>

                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">Approval Notes</label>
                  <textarea
                    value={approvalNotes}
                    onChange={(e) => setApprovalNotes(e.target.value)}
                    placeholder="Add any notes or comments about this batch"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    rows={3}
                  />
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => handleApprove(batch.id)}
                    disabled={loading}
                    className="flex-1 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 disabled:bg-gray-400 flex items-center justify-center gap-2"
                  >
                    <Check className="w-4 h-4" />
                    Approve & Execute
                  </button>
                  <button
                    onClick={() => handleReject(batch.id)}
                    disabled={loading}
                    className="flex-1 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:bg-gray-400 flex items-center justify-center gap-2"
                  >
                    <X className="w-4 h-4" />
                    Reject
                  </button>
                </div>

                {selectedBatch?.id === batch.id && (
                  <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                    <h4 className="font-semibold mb-2">Preview Data</h4>
                    <pre className="text-xs overflow-auto max-h-48">
                      {JSON.stringify(selectedBatch, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
