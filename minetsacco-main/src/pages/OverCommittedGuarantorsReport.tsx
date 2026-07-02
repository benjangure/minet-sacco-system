import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { AlertCircle, Download, RefreshCw, TrendingUp } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface RiskyGuarantee {
  loanId: number;
  loanNumber: string;
  borrowerName: string;
  loanAmount: number;
  outstandingBalance: number;
  guarantorPledgeAmount: number;
  currentFrozenPledge: number;
  guarantorStatus: string;
}

interface OverCommittedGuarantor {
  memberId: number;
  memberNumber: string;
  memberName: string;
  memberStatus: string;
  totalSavings: number;
  frozenSelfGuarantee: number;
  frozenPledges: number;
  totalFrozen: number;
  availableSavings: number;
  amountOverCommitted: number;
  numberOfLoansGuaranteeing: number;
  riskyGuarantees: RiskyGuarantee[];
}

interface OverCommittedGuarantorReport {
  overCommittedGuarantors: OverCommittedGuarantor[];
  totalAtRisk: number;
  countOverCommitted: number;
  systemRiskExposure: number;
}

const OverCommittedGuarantorsReport: React.FC = () => {
  const [reportData, setReportData] = useState<OverCommittedGuarantorReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedGuarantor, setExpandedGuarantor] = useState<number | null>(null);
  const { authToken, userRole } = useAuth();

  useEffect(() => {
    fetchReport();
  }, []);

  const fetchReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axios.get(
        `${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/api/reports/over-committed-guarantors`,
        {
          headers: {
            Authorization: `Bearer ${authToken}`,
            'Content-Type': 'application/json',
          },
        }
      );
      setReportData(response.data.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load report');
      console.error('Error fetching report:', err);
    } finally {
      setLoading(false);
    }
  };

  const exportToExcel = async () => {
    try {
      const response = await axios.get(
        `${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/api/reports/over-committed-guarantors/export/excel`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
          responseType: 'blob',
        }
      );
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `over_committed_guarantors_${new Date().toISOString().split('T')[0]}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (err) {
      console.error('Error exporting to Excel:', err);
      alert('Failed to export report');
    }
  };

  const exportToPdf = async () => {
    try {
      const response = await axios.get(
        `${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/api/reports/over-committed-guarantors/export/pdf`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
          responseType: 'blob',
        }
      );
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `over_committed_guarantors_${new Date().toISOString().split('T')[0]}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (err) {
      console.error('Error exporting to PDF:', err);
      alert('Failed to export report');
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      minimumFractionDigits: 2,
    }).format(value);
  };

  const getRiskLevel = (overCommittedAmount: number) => {
    if (overCommittedAmount > 500000) return { level: 'CRITICAL', color: 'text-red-600', bgColor: 'bg-red-50' };
    if (overCommittedAmount > 100000) return { level: 'HIGH', color: 'text-orange-600', bgColor: 'bg-orange-50' };
    return { level: 'MEDIUM', color: 'text-yellow-600', bgColor: 'bg-yellow-50' };
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <RefreshCw className="mx-auto h-8 w-8 animate-spin text-blue-600" />
          <p className="mt-2 text-gray-600">Loading over-committed guarantor report...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <div className="flex">
          <AlertCircle className="h-5 w-5 text-red-600" />
          <p className="ml-3 text-red-800">{error}</p>
        </div>
      </div>
    );
  }

  if (!reportData || reportData.countOverCommitted === 0) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
        <p className="text-green-800">✓ No over-committed guarantors detected. System is healthy.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Over-Committed Guarantors Report</h1>
          <p className="mt-2 text-gray-600">
            Guarantors with frozen pledges exceeding available savings capacity
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={fetchReport}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <RefreshCw className="h-4 w-4" />
            Refresh
          </button>
          <button
            onClick={exportToExcel}
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            <Download className="h-4 w-4" />
            Excel
          </button>
          <button
            onClick={exportToPdf}
            className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            <Download className="h-4 w-4" />
            PDF
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-600 text-sm font-semibold">GUARANTORS AT RISK</p>
          <p className="mt-2 text-2xl font-bold text-red-900">{reportData.countOverCommitted}</p>
        </div>
        <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
          <p className="text-orange-600 text-sm font-semibold">TOTAL SYSTEM EXPOSURE</p>
          <p className="mt-2 text-2xl font-bold text-orange-900">{formatCurrency(reportData.systemRiskExposure)}</p>
        </div>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <p className="text-yellow-600 text-sm font-semibold">AVERAGE OVER-COMMITMENT</p>
          <p className="mt-2 text-2xl font-bold text-yellow-900">
            {formatCurrency(reportData.systemRiskExposure / reportData.countOverCommitted)}
          </p>
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Member</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Total Savings</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Available Savings</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Frozen Pledges</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Over-Committed</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Risk</th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {reportData.overCommittedGuarantors.map((guarantor) => {
              const riskInfo = getRiskLevel(guarantor.amountOverCommitted);
              const isExpanded = expandedGuarantor === guarantor.memberId;

              return (
                <React.Fragment key={guarantor.memberId}>
                  <tr className={riskInfo.bgColor}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{guarantor.memberName}</p>
                        <p className="text-xs text-gray-500">{guarantor.memberNumber}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {formatCurrency(guarantor.totalSavings)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {formatCurrency(guarantor.availableSavings)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">
                      {formatCurrency(guarantor.frozenPledges)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-700">
                      {formatCurrency(guarantor.amountOverCommitted)}
                    </td>
                    <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold ${riskInfo.color}`}>
                      {riskInfo.level}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <button
                        onClick={() => setExpandedGuarantor(isExpanded ? null : guarantor.memberId)}
                        className="text-blue-600 hover:text-blue-900 text-sm font-medium"
                      >
                        {isExpanded ? 'Hide' : 'Show'} ({guarantor.numberOfLoansGuaranteeing})
                      </button>
                    </td>
                  </tr>

                  {/* Expanded Details - Risky Loans */}
                  {isExpanded && (
                    <tr>
                      <td colSpan={7} className="px-6 py-4 bg-gray-50">
                        <div className="space-y-3">
                          <p className="font-semibold text-gray-900 mb-3">Loans Guaranteed by {guarantor.memberName}:</p>
                          <div className="space-y-2">
                            {guarantor.riskyGuarantees.map((loan) => (
                              <div key={loan.loanId} className="bg-white border border-gray-200 rounded p-3 text-sm">
                                <div className="grid grid-cols-3 gap-4">
                                  <div>
                                    <p className="text-gray-500 text-xs uppercase">Loan</p>
                                    <p className="font-medium text-gray-900">{loan.loanNumber}</p>
                                    <p className="text-xs text-gray-600">Borrower: {loan.borrowerName}</p>
                                  </div>
                                  <div>
                                    <p className="text-gray-500 text-xs uppercase">Pledge Amount</p>
                                    <p className="font-medium text-gray-900">{formatCurrency(loan.guarantorPledgeAmount)}</p>
                                    <p className="text-xs text-gray-600">Outstanding: {formatCurrency(loan.outstandingBalance)}</p>
                                  </div>
                                  <div>
                                    <p className="text-gray-500 text-xs uppercase">Current Frozen</p>
                                    <p className="font-medium text-red-600">{formatCurrency(loan.currentFrozenPledge)}</p>
                                    <p className="text-xs text-gray-600">Status: {loan.guarantorStatus}</p>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Key Metrics */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
          <div className="text-sm text-blue-800">
            <p className="font-semibold mb-2">Risk Summary:</p>
            <ul className="list-disc list-inside space-y-1">
              <li>
                <strong>{reportData.countOverCommitted}</strong> guarantors have pledged more than their available savings capacity
              </li>
              <li>
                Total system risk exposure: <strong>{formatCurrency(reportData.systemRiskExposure)}</strong>
              </li>
              <li>
                If all loans default simultaneously, these guarantors cannot cover their pledged amounts
              </li>
              <li>Action Required: Contact over-committed guarantors to reduce pledges or increase savings</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OverCommittedGuarantorsReport;
