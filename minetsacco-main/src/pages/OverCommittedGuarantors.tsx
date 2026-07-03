import React, { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AlertCircle, Download, Loader2, ChevronDown, ChevronUp } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { getApiBaseUrl } from "../config/api";

const API_BASE_URL = getApiBaseUrl();

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

interface ReportData {
  overCommittedGuarantors: OverCommittedGuarantor[];
  totalAtRisk: number;
  countOverCommitted: number;
  systemRiskExposure: number;
}

const OverCommittedGuarantors = () => {
  const [reportData, setReportData] = useState<ReportData | null>(null);
  const [exitedLoanData, setExitedLoanData] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());
  const { session } = useAuth();
  const { toast } = useToast();

  useEffect(() => {
    fetchReport();
    fetchExitedLoansReport();
  }, []);

  const fetchReport = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/reports/over-committed-guarantors`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setReportData(data.data);
      } else {
        toast({
          title: "Error",
          description: "Failed to fetch over-committed guarantor report",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to load report",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchExitedLoansReport = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/reports/exited-members-outstanding-loans`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setExitedLoanData(data.data);
      } else {
        toast({
          title: "Error",
          description: "Failed to fetch exited members loans report",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to load exited loans report",
        variant: "destructive",
      });
    }
  };

  const handleExportExcel = async () => {
    try {
      setExporting(true);
      const response = await fetch(`${API_BASE_URL}/reports/over-committed-guarantors/export/excel`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `over-committed-guarantors_${new Date().toISOString().split("T")[0]}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported to Excel" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setExporting(false);
    }
  };

  const handleExportPdf = async () => {
    try {
      setExporting(true);
      const response = await fetch(`${API_BASE_URL}/reports/over-committed-guarantors/export/pdf`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `over-committed-guarantors_${new Date().toISOString().split("T")[0]}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported to PDF" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setExporting(false);
    }
  };

  const handleExitedLoansExportExcel = async () => {
    try {
      setExporting(true);
      const response = await fetch(`${API_BASE_URL}/reports/exited-members-outstanding-loans/export/excel`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `exited_members_outstanding_loans_${new Date().toISOString().split("T")[0]}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported to Excel" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setExporting(false);
    }
  };

  const handleExitedLoansExportPdf = async () => {
    try {
      setExporting(true);
      const response = await fetch(`${API_BASE_URL}/reports/exited-members-outstanding-loans/export/pdf`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `exited_members_outstanding_loans_${new Date().toISOString().split("T")[0]}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({ title: "Success", description: "Report exported to PDF" });
      } else {
        toast({ title: "Error", description: "Failed to export report", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Export failed", variant: "destructive" });
    } finally {
      setExporting(false);
    }
  };

  const toggleRowExpansion = (memberId: number) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(memberId)) {
      newExpanded.delete(memberId);
    } else {
      newExpanded.add(memberId);
    }
    setExpandedRows(newExpanded);
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "KES" }).format(value);
  };

  const getRiskColor = (overCommitted: number, available: number) => {
    const ratio = overCommitted / available;
    if (ratio > 0.5) return "text-red-600";
    if (ratio > 0.25) return "text-orange-600";
    return "text-yellow-600";
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!reportData) {
    return (
      <Card className="border-red-200 bg-red-50">
        <CardContent className="pt-6">
          <p className="text-red-800">Failed to load report data</p>
        </CardContent>
      </Card>
    );
  }

  // Split guarantors into exited and active
  const exitedGuarantors = reportData.overCommittedGuarantors.filter(
    (g) => g.memberStatus === "EXITED"
  );
  const activeOverCommitted = reportData.overCommittedGuarantors.filter(
    (g) => g.memberStatus !== "EXITED"
  );

  // Render function for guarantor table - eliminates duplication
  const renderGuarantorTable = (list: OverCommittedGuarantor[]) => (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b bg-gray-50">
          <th className="text-left py-3 px-4 font-semibold">Member</th>
          <th className="text-right py-3 px-4 font-semibold">Total Savings</th>
          <th className="text-right py-3 px-4 font-semibold">Available Savings</th>
          <th className="text-right py-3 px-4 font-semibold">Frozen Pledges</th>
          <th className="text-right py-3 px-4 font-semibold">Over-Committed</th>
          <th className="text-center py-3 px-4 font-semibold"># Loans</th>
          <th className="text-center py-3 px-4 font-semibold"></th>
        </tr>
      </thead>
      <tbody>
        {list.map((guarantor) => (
          <React.Fragment key={guarantor.memberId}>
            <tr className="border-b hover:bg-muted/50">
              <td className="py-4 px-4">
                <div>
                  <p className="font-semibold">{guarantor.memberName}</p>
                  <p className="text-xs text-muted-foreground">{guarantor.memberNumber}</p>
                </div>
              </td>
              <td className="text-right py-4 px-4">{formatCurrency(guarantor.totalSavings)}</td>
              <td className="text-right py-4 px-4 font-semibold">
                {formatCurrency(guarantor.availableSavings)}
              </td>
              <td className="text-right py-4 px-4 font-semibold">
                {formatCurrency(guarantor.frozenPledges)}
              </td>
              <td
                className={`text-right py-4 px-4 font-bold ${getRiskColor(
                  guarantor.amountOverCommitted,
                  guarantor.availableSavings
                )}`}
              >
                {formatCurrency(guarantor.amountOverCommitted)}
              </td>
              <td className="text-center py-4 px-4">{guarantor.numberOfLoansGuaranteeing}</td>
              <td className="text-center py-4 px-4">
                <button
                  onClick={() => toggleRowExpansion(guarantor.memberId)}
                  className="p-1 hover:bg-muted rounded"
                >
                  {expandedRows.has(guarantor.memberId) ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </button>
              </td>
            </tr>

            {/* Expanded row - Risky Guarantees */}
            {expandedRows.has(guarantor.memberId) && (
              <tr className="border-b bg-muted/30">
                <td colSpan={7} className="py-4 px-4">
                  <div className="space-y-2">
                    <h4 className="font-semibold text-sm">Risky Guarantees:</h4>
                    <div className="overflow-x-auto">
                      <table className="w-full text-xs">
                        <thead>
                          <tr className="border-b">
                            <th className="text-left py-2 px-2">Loan #</th>
                            <th className="text-left py-2 px-2">Borrower</th>
                            <th className="text-right py-2 px-2">Outstanding</th>
                            <th className="text-right py-2 px-2">Pledge Amt</th>
                            <th className="text-right py-2 px-2">Frozen Pledge</th>
                            <th className="text-left py-2 px-2">Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {guarantor.riskyGuarantees.map((loan, idx) => (
                            <tr key={idx} className="border-b">
                              <td className="py-2 px-2 font-mono">{loan.loanNumber}</td>
                              <td className="py-2 px-2">{loan.borrowerName}</td>
                              <td className="text-right py-2 px-2">{formatCurrency(loan.outstandingBalance)}</td>
                              <td className="text-right py-2 px-2">
                                {formatCurrency(loan.guarantorPledgeAmount)}
                              </td>
                              <td className="text-right py-2 px-2 font-semibold">
                                {formatCurrency(loan.currentFrozenPledge)}
                              </td>
                              <td className="py-2 px-2">
                                <span
                                  className={`inline-block px-2 py-1 rounded text-xs font-medium ${
                                    loan.guarantorStatus === "ACTIVE"
                                      ? "bg-green-100 text-green-800"
                                      : "bg-gray-100 text-gray-800"
                                  }`}
                                >
                                  {loan.guarantorStatus}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </td>
              </tr>
            )}
          </React.Fragment>
        ))}
      </tbody>
    </table>
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-foreground">Over-Committed Guarantors</h1>
        <p className="text-muted-foreground">Guarantors with frozen pledges exceeding available savings</p>
      </div>

      {/* Alert if there are over-committed guarantors */}
      {reportData.countOverCommitted > 0 && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6 flex items-start gap-4">
            <AlertCircle className="h-5 w-5 text-red-600 mt-1 flex-shrink-0" />
            <div>
              <h3 className="font-semibold text-red-900">Risk Alert</h3>
              <p className="text-red-800">
                {reportData.countOverCommitted} guarantor{reportData.countOverCommitted > 1 ? "s" : ""} have over-committed
                beyond their available savings, with a total system risk exposure of{" "}
                <span className="font-bold">{formatCurrency(reportData.systemRiskExposure)}</span>
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">Total At Risk</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{formatCurrency(reportData.totalAtRisk)}</div>
            <p className="text-xs text-muted-foreground">System exposure</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">Over-Committed Count</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{reportData.countOverCommitted}</div>
            <p className="text-xs text-muted-foreground">Guarantors at risk</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium">Average Over-Commitment</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {reportData.countOverCommitted > 0
                ? formatCurrency(reportData.totalAtRisk / reportData.countOverCommitted)
                : "N/A"}
            </div>
            <p className="text-xs text-muted-foreground">Per guarantor</p>
          </CardContent>
        </Card>
      </div>

      {/* Export Buttons */}
      <div className="flex gap-3">
        <Button onClick={handleExportExcel} disabled={exporting} className="gap-2">
          {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
          Export to Excel
        </Button>
        <Button onClick={handleExportPdf} disabled={exporting} variant="outline" className="gap-2">
          {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
          Export to PDF
        </Button>
        <Button onClick={fetchReport} variant="outline">
          Refresh
        </Button>
      </div>

      {/* Exited Members - Reallocation Required */}
      <Card className="border-orange-200">
        <CardHeader>
          <CardTitle className="text-orange-900">Exited Members — Reallocation Required</CardTitle>
          <p className="text-sm text-muted-foreground">
            These members have left the SACCO but still have active guarantee pledges. Reallocate their pledges on the Loans page.
          </p>
        </CardHeader>
        <CardContent>
          {exitedGuarantors.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <p>No exited members currently guaranteeing loans.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">{renderGuarantorTable(exitedGuarantors)}</div>
          )}
        </CardContent>
      </Card>

      {/* Active Members - Over-Committed */}
      <Card>
        <CardHeader>
          <CardTitle>Active Members — Over-Committed</CardTitle>
        </CardHeader>
        <CardContent>
          {activeOverCommitted.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <p>No active over-committed guarantors found.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">{renderGuarantorTable(activeOverCommitted)}</div>
          )}
        </CardContent>
      </Card>

      {/* Exited Members - Outstanding Loans */}
      <Card>
        <CardHeader>
          <CardTitle>Exited Members — Outstanding Loans</CardTitle>
          <p className="text-sm text-muted-foreground">
            Members who have exited but still have active disbursed loans with outstanding balances.
          </p>
        </CardHeader>
        <CardContent>
          {exitedLoanData?.exitedMembersWithLoans && exitedLoanData.exitedMembersWithLoans.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <p>No exited members with outstanding loans found.</p>
            </div>
          ) : exitedLoanData?.exitedMembersWithLoans ? (
            <div className="space-y-4">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b bg-gray-50">
                      <th className="text-left py-3 px-4 font-semibold">Member</th>
                      <th className="text-left py-3 px-4 font-semibold">Exit Date</th>
                      <th className="text-left py-3 px-4 font-semibold">Exit Reason</th>
                      <th className="text-left py-3 px-4 font-semibold">Loan Number</th>
                      <th className="text-right py-3 px-4 font-semibold">Original Amount</th>
                      <th className="text-right py-3 px-4 font-semibold">Outstanding Balance</th>
                      <th className="text-left py-3 px-4 font-semibold">Disbursement Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {exitedLoanData.exitedMembersWithLoans.map((loan: any, idx: number) => (
                      <tr key={idx} className="border-b hover:bg-muted/50">
                        <td className="py-4 px-4">
                          <div>
                            <p className="font-semibold">{loan.memberName}</p>
                            <p className="text-xs text-muted-foreground">{loan.memberNumber}</p>
                          </div>
                        </td>
                        <td className="py-4 px-4 text-sm">
                          {loan.exitDate ? new Date(loan.exitDate).toLocaleDateString() : "N/A"}
                        </td>
                        <td className="py-4 px-4 text-sm">{loan.exitReason || "N/A"}</td>
                        <td className="py-4 px-4 text-sm font-mono">{loan.loanNumber}</td>
                        <td className="text-right py-4 px-4 text-sm">{formatCurrency(loan.originalAmount)}</td>
                        <td className="text-right py-4 px-4 text-sm font-semibold text-red-600">
                          {formatCurrency(loan.outstandingBalance)}
                        </td>
                        <td className="py-4 px-4 text-sm">
                          {loan.disbursementDate ? new Date(loan.disbursementDate).toLocaleDateString() : "N/A"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex gap-2 pt-4">
                <Button onClick={handleExitedLoansExportExcel} disabled={exporting} className="gap-2">
                  {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
                  Export to Excel
                </Button>
                <Button onClick={handleExitedLoansExportPdf} disabled={exporting} variant="outline" className="gap-2">
                  {exporting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
                  Export to PDF
                </Button>
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              <p>Loading exited members report...</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Risk Analysis Footer */}
      {reportData.overCommittedGuarantors.length > 0 && (
        <Card className="bg-blue-50 border-blue-200">
          <CardHeader>
            <CardTitle className="text-base">Risk Analysis</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium">Highest Risk Guarantor</p>
                <p className="text-lg font-bold">
                  {reportData.overCommittedGuarantors[0]?.memberName} -{" "}
                  {formatCurrency(reportData.overCommittedGuarantors[0]?.amountOverCommitted || 0)} over-committed
                </p>
              </div>
              <div>
                <p className="text-sm font-medium">Recommendation</p>
                <p className="text-sm">
                  Review and consider reducing guarantee amounts for guarantors with high over-commitment ratios or request
                  additional savings contributions to cover the shortfall.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default OverCommittedGuarantors;
