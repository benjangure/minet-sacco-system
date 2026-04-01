import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { FileText, Download, Loader2 } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

const API_BASE_URL = "http://localhost:8080/api";

const Reports = () => {
  const [reportType, setReportType] = useState("cashbook");
  const [loading, setLoading] = useState(false);
  const { session, user } = useAuth();
  const { toast } = useToast();

  // Check if user has permission to view reports
  useEffect(() => {
    if (user && !["ADMIN", "TREASURER", "AUDITOR"].includes(user.role)) {
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
        url += `/reports/sasra/par/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "capital-adequacy") {
        url += `/reports/sasra/capital-adequacy/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "provision-bad-debts") {
        url += `/reports/sasra/provision-bad-debts/export/excel?asAtDate=${sasraReportDate}`;
      } else if (reportType === "sasra-compliance") {
        url += `/reports/sasra/compliance/export/excel?asAtDate=${sasraReportDate}`;
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
        url += `/reports/sasra/par/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "capital-adequacy") {
        url += `/reports/sasra/capital-adequacy/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "provision-bad-debts") {
        url += `/reports/sasra/provision-bad-debts/export/pdf?asAtDate=${sasraReportDate}`;
      } else if (reportType === "sasra-compliance") {
        url += `/reports/sasra/compliance/export/pdf?asAtDate=${sasraReportDate}`;
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
    <div className="space-y-6">
      {user && !["ADMIN", "TREASURER", "AUDITOR"].includes(user.role) ? (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-800">
              <strong>Access Denied:</strong> You don't have permission to view reports. Only Admin, Treasurer, and Auditor roles can access reports.
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
                    <SelectItem value="cashbook">Cashbook Report</SelectItem>
                    <SelectItem value="trial-balance">Trial Balance</SelectItem>
                    <SelectItem value="balance-sheet">Balance Sheet</SelectItem>
                    <SelectItem value="member-statement">Member Statement</SelectItem>
                    <SelectItem value="loan-register">Loan Register</SelectItem>
                    <SelectItem value="profit-loss">Profit & Loss Report</SelectItem>
                    <SelectItem value="par">Portfolio At Risk (PAR)</SelectItem>
                    <SelectItem value="capital-adequacy">Capital Adequacy</SelectItem>
                    <SelectItem value="provision-bad-debts">Provision for Bad Debts</SelectItem>
                    <SelectItem value="sasra-compliance">SASRA Compliance Report</SelectItem>
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

              <div className="flex gap-2 justify-end pt-4">
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
  );
};

export default Reports;
