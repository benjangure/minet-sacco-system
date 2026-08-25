import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useRefresh } from "@/contexts/RefreshContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { FileText, Download, Loader2, Search, Eye } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

import { getApiBaseUrl } from "../config/api";
const API_BASE_URL = getApiBaseUrl();

const Reports = () => {
  const [reportType, setReportType] = useState("cashbook");
  const [loading, setLoading] = useState(false);
  const { session, user } = useAuth();
  const { toast } = useToast();
  const { refreshKey } = useRefresh();

  // Check if user has permission to view reports
  const canViewFinancialReports = user && ["ADMIN", "TREASURER", "AUDITOR"].includes(user.role);
  const canViewGuarantorReport = user && ["ADMIN", "TREASURER", "AUDITOR", "LOAN_OFFICER"].includes(user.role);
  const canViewLoanEligibilityReport = user && ["ADMIN", "TREASURER", "AUDITOR", "LOAN_OFFICER", "CUSTOMER_SUPPORT"].includes(user.role);
  const canViewWithdrawalMonitoringReport = user && ["ADMIN", "TREASURER", "AUDITOR"].includes(user.role);

  useEffect(() => {
    if (user && !canViewFinancialReports && !canViewGuarantorReport && !canViewLoanEligibilityReport && !canViewWithdrawalMonitoringReport) {
      toast({
        title: "Access Denied",
        description: "You don't have permission to view reports",
        variant: "destructive",
      });
    }
  }, [user, toast]);

  // Cashbook filters
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [memberNumber, setMemberNumber] = useState("");
  const [transactionType, setTransactionType] = useState("");
  const [accountType, setAccountType] = useState("");

  // Trial Balance filters
  const [tbMemberNumber, setTbMemberNumber] = useState("");
  const [tbAccountType, setTbAccountType] = useState("");

  // Member Statement filters
  const [statementMemberId, setStatementMemberId] = useState("");
  const [statementStartDate, setStatementStartDate] = useState("");
  const [statementEndDate, setStatementEndDate] = useState("");

  // Loan Register filters
  const [lrMemberNumber, setLrMemberNumber] = useState("");
  const [loanStatus, setLoanStatus] = useState("");
  const [loanProduct, setLoanProduct] = useState("");

  // SASRA Report filters
  const [sasraReportDate, setSasraReportDate] = useState(new Date().toISOString().split("T")[0]);

  // Withdrawal Monitoring filters
  const [wmStartDate, setWmStartDate] = useState("");
  const [wmEndDate, setWmEndDate] = useState("");
  const [wmMemberNumber, setWmMemberNumber] = useState("");
  const [wmWithdrawalMethod, setWmWithdrawalMethod] = useState("");
  const [wmTransactionStatus, setWmTransactionStatus] = useState("");

  // Guarantor Report filters
  const [guarantorMemberId, setGuarantorMemberId] = useState("");
  const [guarantorStatus, setGuarantorStatus] = useState("");
  const [guarantorViewMode, setGuarantorViewMode] = useState("single");  // single or all

  // Loan Eligibility filters
  const [eligibilityMemberId, setEligibilityMemberId] = useState("");

  // Monthly Contribution filters
  const [mctStartDate, setMctStartDate] = useState("");
  const [mctEndDate, setMctEndDate] = useState("");
  const [mctBatchStatus, setMctBatchStatus] = useState("");

  // Member Contributions filters
  const [mcMemberId, setMcMemberId] = useState("");
  const [mcStartDate, setMcStartDate] = useState("");
  const [mcEndDate, setMcEndDate] = useState("");
  const [mcAccountType, setMcAccountType] = useState("");
  const [mcMembers, setMcMembers] = useState<{ id: number; memberNumber: string; fullName: string }[]>([]);
  const [mcMembersLoading, setMcMembersLoading] = useState(false);
  const [mcMemberSearch, setMcMemberSearch] = useState("");

  // Member Contributions — view modal state
  const [mcModalOpen, setMcModalOpen] = useState(false);
  const [mcModalLoading, setMcModalLoading] = useState(false);
  const [mcReportData, setMcReportData] = useState<any>(null);

  // Fetch members when "member-contributions" is selected
  useEffect(() => {
    if (reportType === "member-contributions" && mcMembers.length === 0 && !mcMembersLoading) {
      setMcMembersLoading(true);
      fetch(`${API_BASE_URL}/members/status/ACTIVE`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      })
        .then(r => r.ok ? r.json() : Promise.reject(r))
        .then(data => setMcMembers(data.data || data || []))
        .catch(() => setMcMembers([]))
        .finally(() => setMcMembersLoading(false));
    }
  }, [reportType]);

  const handleViewContributions = async () => {
    if (!mcMemberId) {
      toast({ title: "Error", description: "Please select a member first", variant: "destructive" });
      return;
    }
    setMcModalLoading(true);
    try {
      let url = `${API_BASE_URL}/reports/member-contributions/${mcMemberId}`;
      const params: string[] = [];
      if (mcStartDate) params.push(`startDate=${mcStartDate}`);
      if (mcEndDate) params.push(`endDate=${mcEndDate}`);
      if (mcAccountType && mcAccountType !== "ALL") params.push(`accountType=${mcAccountType}`);
      if (params.length) url += `?${params.join("&")}`;

      const res = await fetch(url, { headers: { Authorization: `Bearer ${session?.token}` } });
      if (res.ok) {
        const json = await res.json();
        setMcReportData(json.data || json);
        setMcModalOpen(true);
      } else {
        const err = await res.json().catch(() => ({}));
        toast({ title: "Error", description: err.message || "Failed to load report", variant: "destructive" });
      }
    } catch {
      toast({ title: "Error", description: "Failed to load report", variant: "destructive" });
    } finally {
      setMcModalLoading(false);
    }
  };

  const handleExportExcel = async () => {
    try {
      setLoading(true);
      let url = `${API_BASE_URL}/reports`;

      if (reportType === "cashbook") {
        if (!startDate || !endDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/cashbook/export/excel?startDate=${startDate}&endDate=${endDate}`;
        if (memberNumber) url += `&memberNumber=${memberNumber}`;
        if (transactionType) url += `&transactionType=${transactionType}`;
        if (accountType) url += `&accountType=${accountType}`;
      } else if (reportType === "trial-balance") {
        url += `/trial-balance/export/excel`;
        if (tbMemberNumber) url += `?memberNumber=${tbMemberNumber}`;
        if (tbAccountType) url += `${tbMemberNumber ? "&" : "?"}accountType=${tbAccountType}`;
      } else if (reportType === "balance-sheet") {
        url += `/balance-sheet/export/excel`;
      } else if (reportType === "member-statement") {
        if (!statementMemberId || !statementStartDate || !statementEndDate) {
          toast({ title: "Error", description: "Member ID and dates are required", variant: "destructive" });
          return;
        }
        url += `/${statementMemberId}/export/excel?startDate=${statementStartDate}&endDate=${statementEndDate}`;
      } else if (reportType === "loan-register") {
        url += `/loan-register/export/excel`;
        if (lrMemberNumber) url += `?memberNumber=${lrMemberNumber}`;
        if (loanStatus) url += `${lrMemberNumber ? "&" : "?"}loanStatus=${loanStatus}`;
        if (loanProduct) url += `${lrMemberNumber || loanStatus ? "&" : "?"}loanProduct=${loanProduct}`;
      } else if (reportType === "profit-loss") {
        if (!startDate || !endDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/profit-loss/export/excel?startDate=${startDate}&endDate=${endDate}`;
      } else if (reportType === "par") {
        url += `/sasra/par/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "capital-adequacy") {
        url += `/sasra/capital-adequacy/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "provision-bad-debts") {
        url += `/sasra/provision-bad-debts/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "sasra-compliance") {
        url += `/sasra/compliance/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "withdrawal-monitoring") {
        if (!wmStartDate || !wmEndDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/withdrawal-monitoring/export/excel?startDate=${wmStartDate}&endDate=${wmEndDate}`;
        if (wmMemberNumber) url += `&memberNumber=${wmMemberNumber}`;
        if (wmWithdrawalMethod) url += `&withdrawalMethod=${wmWithdrawalMethod}`;
        if (wmTransactionStatus) url += `&transactionStatus=${wmTransactionStatus}`;
      } else if (reportType === "guarantor-report") {
        if (!guarantorMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/guarantor/${guarantorMemberId}/export/excel`;
        if (guarantorStatus) url += `?guarantorStatus=${guarantorStatus}`;
      } else if (reportType === "guarantor-all-report") {
        url += `/guarantor/all/export/excel`;  // Note: May need endpoint adjustment
      } else if (reportType === "loan-eligibility-report") {
        if (!eligibilityMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/loan-eligibility/${eligibilityMemberId}/export/excel`;
      } else if (reportType === "monthly-contribution-tracking") {
        if (!mctStartDate || !mctEndDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/monthly-contribution-tracking/export/excel?startDate=${mctStartDate}&endDate=${mctEndDate}`;
        if (mctBatchStatus) url += `&batchStatus=${mctBatchStatus}`;
      } else if (reportType === "member-contributions") {
        if (!mcMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/member-contributions/${mcMemberId}/export/excel`;
        const params: string[] = [];
        if (mcStartDate) params.push(`startDate=${mcStartDate}`);
        if (mcEndDate) params.push(`endDate=${mcEndDate}`);
        if (mcAccountType && mcAccountType !== "ALL") params.push(`accountType=${mcAccountType}`);
        if (params.length) url += `?${params.join("&")}`;
      }

      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `${reportType}_${new Date().toISOString().split("T")[0]}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported successfully" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const handleExportPdf = async () => {
    try {
      setLoading(true);
      let url = `${API_BASE_URL}/reports`;

      if (reportType === "cashbook") {
        if (!startDate || !endDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/cashbook/export/pdf?startDate=${startDate}&endDate=${endDate}`;
        if (memberNumber) url += `&memberNumber=${memberNumber}`;
        if (transactionType) url += `&transactionType=${transactionType}`;
        if (accountType) url += `&accountType=${accountType}`;
      } else if (reportType === "trial-balance") {
        url += `/trial-balance/export/pdf`;
        if (tbMemberNumber) url += `?memberNumber=${tbMemberNumber}`;
        if (tbAccountType) url += `${tbMemberNumber ? "&" : "?"}accountType=${tbAccountType}`;
      } else if (reportType === "balance-sheet") {
        url += `/balance-sheet/export/pdf`;
      } else if (reportType === "member-statement") {
        if (!statementMemberId || !statementStartDate || !statementEndDate) {
          toast({ title: "Error", description: "Member ID and dates are required", variant: "destructive" });
          return;
        }
        url += `/${statementMemberId}/export/pdf?startDate=${statementStartDate}&endDate=${statementEndDate}`;
      } else if (reportType === "loan-register") {
        url += `/loan-register/export/pdf`;
        if (lrMemberNumber) url += `?memberNumber=${lrMemberNumber}`;
        if (loanStatus) url += `${lrMemberNumber ? "&" : "?"}loanStatus=${loanStatus}`;
        if (loanProduct) url += `${lrMemberNumber || loanStatus ? "&" : "?"}loanProduct=${loanProduct}`;
      } else if (reportType === "profit-loss") {
        if (!startDate || !endDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/profit-loss/export/pdf?startDate=${startDate}&endDate=${endDate}`;
      } else if (reportType === "par") {
        url += `/sasra/par/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "capital-adequacy") {
        url += `/sasra/capital-adequacy/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "provision-bad-debts") {
        url += `/sasra/provision-bad-debts/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "sasra-compliance") {
        url += `/sasra/compliance/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "withdrawal-monitoring") {
        if (!wmStartDate || !wmEndDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/withdrawal-monitoring/export/pdf?startDate=${wmStartDate}&endDate=${wmEndDate}`;
        if (wmMemberNumber) url += `&memberNumber=${wmMemberNumber}`;
        if (wmWithdrawalMethod) url += `&withdrawalMethod=${wmWithdrawalMethod}`;
        if (wmTransactionStatus) url += `&transactionStatus=${wmTransactionStatus}`;
      } else if (reportType === "guarantor-report") {
        if (!guarantorMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/guarantor/${guarantorMemberId}/export/pdf`;
        if (guarantorStatus) url += `?guarantorStatus=${guarantorStatus}`;
      } else if (reportType === "guarantor-all-report") {
        url += `/guarantor/all/export/pdf`;  // Note: May need endpoint adjustment
      } else if (reportType === "loan-eligibility-report") {
        if (!eligibilityMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/loan-eligibility/${eligibilityMemberId}/export/pdf`;
      } else if (reportType === "monthly-contribution-tracking") {
        if (!mctStartDate || !mctEndDate) {
          toast({ title: "Error", description: "Start and end dates are required", variant: "destructive" });
          return;
        }
        url += `/monthly-contribution-tracking/export/pdf?startDate=${mctStartDate}&endDate=${mctEndDate}`;
        if (mctBatchStatus) url += `&batchStatus=${mctBatchStatus}`;
      } else if (reportType === "member-contributions") {
        if (!mcMemberId) {
          toast({ title: "Error", description: "Member ID is required", variant: "destructive" });
          return;
        }
        url += `/member-contributions/${mcMemberId}/export/pdf`;
        const params: string[] = [];
        if (mcStartDate) params.push(`startDate=${mcStartDate}`);
        if (mcEndDate) params.push(`endDate=${mcEndDate}`);
        if (mcAccountType && mcAccountType !== "ALL") params.push(`accountType=${mcAccountType}`);
        if (params.length) url += `?${params.join("&")}`;
      }

      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `${reportType}_${new Date().toISOString().split("T")[0]}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported successfully" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
    <div className="space-y-6">
      {user && !canViewFinancialReports && !canViewGuarantorReport && !canViewLoanEligibilityReport && !canViewWithdrawalMonitoringReport ? (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-800">
              <strong>Access Denied:</strong> You don't have permission to view reports. Contact your administrator for access.
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <div>
            <h1 className="text-3xl font-bold text-foreground">Financial Reports</h1>
            <p className="text-muted-foreground">Generate and export financial reports with filters</p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Report Selection</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <label className="block text-sm font-medium mb-2">Report Type</label>
                <Select value={reportType} onValueChange={setReportType}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {canViewFinancialReports && (
                      <>
                        <SelectItem value="cashbook">Cashbook Report</SelectItem>
                        <SelectItem value="trial-balance">Trial Balance</SelectItem>
                        <SelectItem value="balance-sheet">Balance Sheet</SelectItem>
                        <SelectItem value="profit-loss">Profit & Loss Report</SelectItem>
                        <SelectItem value="par">Portfolio At Risk (PAR)</SelectItem>
                        <SelectItem value="capital-adequacy">Capital Adequacy</SelectItem>
                        <SelectItem value="provision-bad-debts">Provision for Bad Debts</SelectItem>
                        <SelectItem value="sasra-compliance">SASRA Compliance Report</SelectItem>
                      </>
                    )}
                    {(canViewFinancialReports || canViewGuarantorReport || canViewLoanEligibilityReport) && (
                      <>
                        <SelectItem value="member-statement">Member Statement</SelectItem>
                        <SelectItem value="loan-register">Loan Register</SelectItem>
                      </>
                    )}
                    {canViewLoanEligibilityReport && (
                      <SelectItem value="loan-eligibility-report">Loan Eligibility Report</SelectItem>
                    )}
                    {canViewWithdrawalMonitoringReport && (
                      <SelectItem value="withdrawal-monitoring">Withdrawal Monitoring Report</SelectItem>
                    )}
                    {canViewGuarantorReport && (
                      <SelectItem value="guarantor-report">Guarantor Report</SelectItem>
                    )}
                    {canViewGuarantorReport && (
                      <SelectItem value="guarantor-all-report">Guarantor Report (All Members)</SelectItem>
                    )}
                    {canViewFinancialReports && (
                      <SelectItem value="monthly-contribution-tracking">Monthly Contribution Tracking</SelectItem>
                    )}
                    {(canViewFinancialReports || canViewGuarantorReport || canViewLoanEligibilityReport) && (
                      <SelectItem value="member-contributions">Member Contributions Export</SelectItem>
                    )}
                  </SelectContent>
                </Select>
              </div>

              {/* Cashbook Filters */}
              {reportType === "cashbook" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Cashbook Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Start Date *</label>
                      <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">End Date *</label>
                      <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Member Number</label>
                      <Input placeholder="e.g., M001" value={memberNumber} onChange={(e) => setMemberNumber(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Transaction Type</label>
                      <Select value={transactionType} onValueChange={setTransactionType}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="DEPOSIT">Deposit</SelectItem>
                          <SelectItem value="WITHDRAWAL">Withdrawal</SelectItem>
                          <SelectItem value="LOAN_DISBURSEMENT">Loan Disbursement</SelectItem>
                          <SelectItem value="LOAN_REPAYMENT">Loan Repayment</SelectItem>
                          <SelectItem value="INTEREST">Interest</SelectItem>
                          <SelectItem value="LOAN_DEFAULT_DEBIT">Loan Default Debit</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Account Type</label>
                      <Select value={accountType} onValueChange={setAccountType}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="SAVINGS">Savings</SelectItem>
                          <SelectItem value="SHARES">Shares</SelectItem>
                          <SelectItem value="BENEVOLENT_FUND">Benevolent Fund</SelectItem>
                          <SelectItem value="DEVELOPMENT_FUND">Development Fund</SelectItem>
                          <SelectItem value="SCHOOL_FEES">School Fees</SelectItem>
                          <SelectItem value="HOLIDAY_FUND">Holiday Fund</SelectItem>
                          <SelectItem value="EMERGENCY_FUND">Emergency Fund</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              )}

              {/* Trial Balance Filters */}
              {reportType === "trial-balance" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Trial Balance Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Member Number</label>
                      <Input placeholder="e.g., M001" value={tbMemberNumber} onChange={(e) => setTbMemberNumber(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Account Type</label>
                      <Select value={tbAccountType} onValueChange={setTbAccountType}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="SAVINGS">Savings</SelectItem>
                          <SelectItem value="SHARES">Shares</SelectItem>
                          <SelectItem value="LOAN">Loan</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              )}

              {/* Member Statement Filters */}
              {reportType === "member-statement" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Member Statement Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Member ID *</label>
                      <Input type="text" placeholder="e.g., EMP001" value={statementMemberId} onChange={(e) => setStatementMemberId(e.target.value)} />
                    </div>
                    <div />
                    <div>
                      <label className="block text-sm font-medium mb-1">Start Date *</label>
                      <Input type="date" value={statementStartDate} onChange={(e) => setStatementStartDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">End Date *</label>
                      <Input type="date" value={statementEndDate} onChange={(e) => setStatementEndDate(e.target.value)} />
                    </div>
                  </div>
                </div>
              )}

              {/* Loan Register Filters */}
              {reportType === "loan-register" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Loan Register Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Member Number</label>
                      <Input placeholder="e.g., M001" value={lrMemberNumber} onChange={(e) => setLrMemberNumber(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Loan Status</label>
                      <Select value={loanStatus} onValueChange={setLoanStatus}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select status" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="PENDING">Pending</SelectItem>
                          <SelectItem value="APPROVED">Approved</SelectItem>
                          <SelectItem value="REJECTED">Rejected</SelectItem>
                          <SelectItem value="DISBURSED">Disbursed</SelectItem>
                          <SelectItem value="REPAID">Repaid</SelectItem>
                          <SelectItem value="DEFAULTED">Defaulted</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Loan Product</label>
                      <Input placeholder="e.g., Personal Loan" value={loanProduct} onChange={(e) => setLoanProduct(e.target.value)} />
                    </div>
                  </div>
                </div>
              )}

              {/* Profit & Loss Filters */}
              {reportType === "profit-loss" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Profit & Loss Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Start Date *</label>
                      <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">End Date *</label>
                      <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                    </div>
                  </div>
                </div>
              )}

              {/* SASRA Reports Filters */}
              {(reportType === "par" || reportType === "capital-adequacy" || reportType === "provision-bad-debts" || reportType === "sasra-compliance") && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Report Date</h3>
                  <div>
                    <label className="block text-sm font-medium mb-1">As at Date</label>
                    <Input 
                      type="date" 
                      value={sasraReportDate} 
                      onChange={(e) => setSasraReportDate(e.target.value)}
                    />
                    <p className="text-xs text-muted-foreground mt-1">
                      {reportType === "par" && "Portfolio At Risk report as at selected date"}
                      {reportType === "capital-adequacy" && "Capital Adequacy report as at selected date"}
                      {reportType === "provision-bad-debts" && "Provision for Bad Debts report as at selected date"}
                      {reportType === "sasra-compliance" && "SASRA Regulatory Compliance report as at selected date"}
                    </p>
                  </div>
                </div>
              )}

              {/* Withdrawal Monitoring Filters */}
              {reportType === "withdrawal-monitoring" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Withdrawal Monitoring Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Start Date *</label>
                      <Input type="date" value={wmStartDate} onChange={(e) => setWmStartDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">End Date *</label>
                      <Input type="date" value={wmEndDate} onChange={(e) => setWmEndDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Member Number</label>
                      <Input placeholder="e.g., M001" value={wmMemberNumber} onChange={(e) => setWmMemberNumber(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Withdrawal Method</label>
                      <Select value={wmWithdrawalMethod} onValueChange={setWmWithdrawalMethod}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select method" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="M_PESA">M-Pesa</SelectItem>
                          <SelectItem value="MANUAL_CASH">Manual Cash</SelectItem>
                          <SelectItem value="BANK_TRANSFER">Bank Transfer</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Transaction Status</label>
                      <Select value={wmTransactionStatus} onValueChange={setWmTransactionStatus}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select status" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="COMPLETED">Completed</SelectItem>
                          <SelectItem value="PENDING">Pending</SelectItem>
                          <SelectItem value="FAILED">Failed</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              )}

              {/* Guarantor Report Filters */}
              {reportType === "guarantor-report" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Guarantor Report Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Member ID *</label>
                      <Input type="text" placeholder="e.g., 1" value={guarantorMemberId} onChange={(e) => setGuarantorMemberId(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Guarantor Status</label>
                      <Select value={guarantorStatus} onValueChange={setGuarantorStatus}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select status" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ACTIVE">Active</SelectItem>
                          <SelectItem value="RELEASED">Released</SelectItem>
                          <SelectItem value="DEFAULTED">Defaulted</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              )}

              {/* Guarantor Report All Members Filters */}
              {reportType === "guarantor-all-report" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Guarantor Report (All Members)</h3>
                  <p className="text-sm text-muted-foreground">Displays guarantorship capacity for all members at a glance</p>
                </div>
              )}

              {/* Loan Eligibility Report Filters */}
              {reportType === "loan-eligibility-report" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Loan Eligibility Report Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Member ID *</label>
                      <Input type="text" placeholder="e.g., 1" value={eligibilityMemberId} onChange={(e) => setEligibilityMemberId(e.target.value)} />
                    </div>
                  </div>
                </div>
              )}

              {/* Monthly Contribution Tracking Filters */}
              {reportType === "monthly-contribution-tracking" && (
                <div className="space-y-4 p-4 bg-accent rounded-lg">
                  <h3 className="font-medium">Monthly Contribution Tracking Filters</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">Start Date *</label>
                      <Input type="date" value={mctStartDate} onChange={(e) => setMctStartDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">End Date *</label>
                      <Input type="date" value={mctEndDate} onChange={(e) => setMctEndDate(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Batch Status</label>
                      <Select value={mctBatchStatus} onValueChange={setMctBatchStatus}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select status" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="PENDING">Pending</SelectItem>
                          <SelectItem value="PROCESSING">Processing</SelectItem>
                          <SelectItem value="COMPLETED">Completed</SelectItem>
                          <SelectItem value="FAILED">Failed</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
              )}

              {/* Member Contributions Export Filters */}
              {reportType === "member-contributions" && (
                <div className="space-y-5 p-5 bg-accent rounded-xl border border-border/50">
                  <div>
                    <h3 className="font-semibold text-base">Member Contributions Export</h3>
                    <p className="text-sm text-muted-foreground mt-0.5">
                      Export the full transaction history for any member. Filters are optional — leave blank to export everything.
                    </p>
                  </div>

                  {/* Member picker — custom combobox, always opens downward */}
                  <div className="space-y-1.5">
                    <label className="block text-sm font-medium">
                      Member <span className="text-destructive">*</span>
                    </label>
                    <div className="relative">
                      {/* Search input that doubles as the trigger */}
                      <div className={`flex items-center gap-2 w-full rounded-md border bg-background px-3 py-2 text-sm shadow-sm transition-colors
                        ${mcMemberId ? "border-primary ring-1 ring-primary/30" : "border-input hover:border-muted-foreground/50"}`}>
                        <Search className="h-4 w-4 text-muted-foreground shrink-0" />
                        <input
                          className="flex-1 bg-transparent outline-none placeholder:text-muted-foreground"
                          placeholder={mcMembersLoading ? "Loading members…" : "Search by name or member number…"}
                          value={mcMemberSearch}
                          onChange={e => {
                            setMcMemberSearch(e.target.value);
                            if (mcMemberId) setMcMemberId(""); // clear selection when user types
                          }}
                          onFocus={e => e.currentTarget.select()}
                          disabled={mcMembersLoading}
                        />
                        {mcMemberId && (
                          <button
                            type="button"
                            className="text-muted-foreground hover:text-foreground transition-colors"
                            onClick={() => { setMcMemberId(""); setMcMemberSearch(""); }}
                            title="Clear selection"
                          >
                            ✕
                          </button>
                        )}
                      </div>

                      {/* Dropdown list — always below, never above */}
                      {mcMemberSearch && !mcMemberId && (
                        <div className="absolute top-full left-0 right-0 mt-1 z-50 rounded-md border border-border bg-white shadow-lg overflow-hidden">
                          <div className="max-h-56 overflow-y-auto">
                            {mcMembersLoading ? (
                              <div className="flex items-center gap-2 px-3 py-3 text-sm text-muted-foreground">
                                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                                </svg>
                                Loading members…
                              </div>
                            ) : (() => {
                              const filtered = mcMembers.filter(m =>
                                m.fullName?.toLowerCase().includes(mcMemberSearch.toLowerCase()) ||
                                m.memberNumber?.toLowerCase().includes(mcMemberSearch.toLowerCase())
                              );
                              return filtered.length === 0 ? (
                                <div className="px-3 py-3 text-sm text-muted-foreground text-center">No members match "{mcMemberSearch}"</div>
                              ) : (
                                filtered.map(m => (
                                  <button
                                    key={m.id}
                                    type="button"
                                    className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-accent transition-colors group"
                                    onClick={() => {
                                      setMcMemberId(m.id.toString());
                                      setMcMemberSearch(`${m.memberNumber} — ${m.fullName}`);
                                    }}
                                  >
                                    <span className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">
                                      {(m.fullName || "?").charAt(0).toUpperCase()}
                                    </span>
                                    <div className="min-w-0">
                                      <p className="text-sm font-medium text-foreground truncate">{m.fullName}</p>
                                      <p className="text-xs text-muted-foreground">{m.memberNumber}</p>
                                    </div>
                                  </button>
                                ))
                              );
                            })()}
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Selected member badge */}
                    {mcMemberId && (() => {
                      const sel = mcMembers.find(m => m.id.toString() === mcMemberId);
                      return sel ? (
                        <div className="flex items-center gap-2 mt-2 px-3 py-2 rounded-md bg-primary/5 border border-primary/20 text-sm">
                          <span className="w-6 h-6 rounded-full bg-primary/15 text-primary flex items-center justify-center text-xs font-bold shrink-0">
                            {(sel.fullName || "?").charAt(0).toUpperCase()}
                          </span>
                          <span className="font-medium text-foreground">{sel.fullName}</span>
                          <span className="text-muted-foreground">·</span>
                          <span className="text-muted-foreground text-xs font-mono">{sel.memberNumber}</span>
                        </div>
                      ) : null;
                    })()}
                  </div>

                  {/* Second row: account type + dates */}
                  <div className="grid grid-cols-3 gap-4">
                    <div className="space-y-1.5">
                      <label className="block text-sm font-medium">Account Type</label>
                      <Select value={mcAccountType} onValueChange={setMcAccountType}>
                        <SelectTrigger className="bg-background">
                          <SelectValue placeholder="All account types" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ALL">All Account Types</SelectItem>
                          <SelectItem value="SAVINGS">Savings</SelectItem>
                          <SelectItem value="SHARES">Shares</SelectItem>
                          <SelectItem value="CONTRIBUTIONS">Contributions</SelectItem>
                          <SelectItem value="BENEVOLENT_FUND">Benevolent Fund</SelectItem>
                          <SelectItem value="DEVELOPMENT_FUND">Development Fund</SelectItem>
                          <SelectItem value="SCHOOL_FEES">School Fees</SelectItem>
                          <SelectItem value="HOLIDAY_FUND">Holiday Fund</SelectItem>
                          <SelectItem value="EMERGENCY_FUND">Emergency Fund</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-1.5">
                      <label className="block text-sm font-medium">Start Date</label>
                      <Input type="date" value={mcStartDate} onChange={e => setMcStartDate(e.target.value)} className="bg-background" />
                    </div>
                    <div className="space-y-1.5">
                      <label className="block text-sm font-medium">End Date</label>
                      <Input type="date" value={mcEndDate} onChange={e => setMcEndDate(e.target.value)} className="bg-background" />
                    </div>
                  </div>
                </div>
              )}

              <div className="flex gap-2 justify-end pt-4">
                {reportType === "member-contributions" && (
                  <Button
                    onClick={handleViewContributions}
                    disabled={mcModalLoading || !mcMemberId}
                    variant="outline"
                    className="border-primary/40 text-primary hover:bg-primary/5"
                  >
                    {mcModalLoading ? (
                      <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Loading…</>
                    ) : (
                      <><Eye className="mr-2 h-4 w-4" />View Report</>
                    )}
                  </Button>
                )}
                <Button onClick={handleExportExcel} disabled={loading} variant="outline">
                  <Download className="mr-2 h-4 w-4" />
                  Export Excel
                </Button>
                <Button onClick={handleExportPdf} disabled={loading}>
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Exporting...
                    </>
                  ) : (
                    <>
                      <FileText className="mr-2 h-4 w-4" />
                      Export PDF
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>

    {/* ── Member Contributions View Modal ──────────────────────────────────── */}
    {mcReportData && (
      <Dialog open={mcModalOpen} onOpenChange={setMcModalOpen}>
        <DialogContent className="max-w-5xl w-full max-h-[90vh] flex flex-col p-0 gap-0">
          {/* Header */}
          <DialogHeader className="px-6 pt-6 pb-4 border-b shrink-0">
            <div className="flex items-start justify-between">
              <div>
                <DialogTitle className="text-xl font-bold">Member Contributions Report</DialogTitle>
                <p className="text-sm text-muted-foreground mt-1">
                  {mcReportData.memberName} &nbsp;·&nbsp;
                  <span className="font-mono">{mcReportData.memberNumber}</span>
                  {(mcReportData.startDate || mcReportData.endDate) && (
                    <> &nbsp;·&nbsp; {mcReportData.startDate ?? "—"} to {mcReportData.endDate ?? "—"}</>
                  )}
                </p>
              </div>
              {/* Download buttons inside modal */}
              <div className="flex gap-2 ml-4 shrink-0">
                <Button size="sm" variant="outline" onClick={handleExportExcel} disabled={loading}>
                  <Download className="mr-1.5 h-3.5 w-3.5" />Excel
                </Button>
                <Button size="sm" variant="outline" onClick={handleExportPdf} disabled={loading}>
                  <FileText className="mr-1.5 h-3.5 w-3.5" />PDF
                </Button>
              </div>
            </div>
          </DialogHeader>

          {/* Scrollable body */}
          <div className="overflow-y-auto flex-1 px-6 py-5 space-y-6">

            {/* Summary cards */}
            <div className="grid grid-cols-3 gap-4">
              <Card className="border-none bg-green-50">
                <CardContent className="pt-4 pb-3">
                  <p className="text-xs text-muted-foreground mb-1">Total Deposited</p>
                  <p className="text-xl font-bold text-green-700">
                    KES {(mcReportData.totalDeposited ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </p>
                </CardContent>
              </Card>
              <Card className="border-none bg-red-50">
                <CardContent className="pt-4 pb-3">
                  <p className="text-xs text-muted-foreground mb-1">Total Withdrawn</p>
                  <p className="text-xl font-bold text-red-700">
                    KES {(mcReportData.totalWithdrawn ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </p>
                </CardContent>
              </Card>
              <Card className="border-none bg-blue-50">
                <CardContent className="pt-4 pb-3">
                  <p className="text-xs text-muted-foreground mb-1">Net Contribution</p>
                  <p className="text-xl font-bold text-blue-700">
                    KES {(mcReportData.netContribution ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Per-account breakdown */}
            {mcReportData.accountSummaries?.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold mb-3 text-muted-foreground uppercase tracking-wide">Account Breakdown</h3>
                <div className="grid grid-cols-2 gap-3">
                  {mcReportData.accountSummaries.map((acc: any) => (
                    <div key={acc.accountType} className="flex items-center justify-between rounded-lg border px-4 py-3 bg-background">
                      <div>
                        <p className="text-sm font-medium">{acc.accountType.replace(/_/g, " ")}</p>
                        <p className="text-xs text-muted-foreground">{acc.transactionCount} transaction{acc.transactionCount !== 1 ? "s" : ""}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-bold">KES {(acc.currentBalance ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
                        <p className="text-xs text-muted-foreground">current balance</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Full transaction table */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  Transaction History
                </h3>
                <Badge variant="secondary">{mcReportData.entries?.length ?? 0} transactions</Badge>
              </div>

              {(!mcReportData.entries || mcReportData.entries.length === 0) ? (
                <div className="text-center py-10 text-muted-foreground text-sm border rounded-lg">
                  No transactions found for the selected criteria.
                </div>
              ) : (
                <div className="rounded-lg border overflow-hidden">
                  <Table>
                    <TableHeader>
                      <TableRow className="bg-muted/50">
                        <TableHead className="text-xs">Date & Time</TableHead>
                        <TableHead className="text-xs">Account</TableHead>
                        <TableHead className="text-xs">Type</TableHead>
                        <TableHead className="text-xs text-right">Amount (KES)</TableHead>
                        <TableHead className="text-xs">Description</TableHead>
                        <TableHead className="text-xs">Processed By</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {mcReportData.entries.map((entry: any, idx: number) => {
                        const isCredit = entry.transactionType === "DEPOSIT" || entry.transactionType === "LOAN_DISBURSEMENT";
                        return (
                          <TableRow key={entry.transactionId ?? idx} className="hover:bg-muted/30">
                            <TableCell className="text-xs font-mono whitespace-nowrap">
                              {entry.transactionDate
                                ? new Date(entry.transactionDate).toLocaleString("en-KE", {
                                    day: "2-digit", month: "short", year: "numeric",
                                    hour: "2-digit", minute: "2-digit"
                                  })
                                : "—"}
                            </TableCell>
                            <TableCell className="text-xs">
                              <Badge variant="outline" className="text-[10px] font-normal">
                                {(entry.accountType ?? "").replace(/_/g, " ")}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-xs">
                              <span className={`inline-flex items-center gap-1 font-medium ${
                                isCredit ? "text-green-700" : "text-red-700"
                              }`}>
                                {isCredit ? "▲" : "▼"} {(entry.transactionType ?? "").replace(/_/g, " ")}
                              </span>
                            </TableCell>
                            <TableCell className={`text-xs text-right font-mono font-semibold ${
                              isCredit ? "text-green-700" : "text-red-700"
                            }`}>
                              {isCredit ? "+" : "-"}
                              {(entry.amount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                            </TableCell>
                            <TableCell className="text-xs max-w-[200px] truncate" title={entry.description}>
                              {entry.description || "—"}
                            </TableCell>
                            <TableCell className="text-xs text-muted-foreground">
                              {entry.processedBy || "system"}
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>
    )}
    </>
  );
};

export default Reports;
