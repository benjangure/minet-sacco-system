import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Download, Loader2, TrendingUp, TrendingDown } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

const API_BASE_URL = "http://localhost:8080/api";

interface ProfitLossReportDTO {
  period: {
    startDate: string;
    endDate: string;
  };
  revenue: {
    interestIncome: {
      fromLoans: number;
      fromSavings: number;
      total: number;
    };
    feesAndCharges: {
      loanProcessingFees: number;
      accountMaintenanceFees: number;
      otherFees: number;
      total: number;
    };
    otherIncome: number;
    totalRevenue: number;
  };
  expenses: {
    operatingExpenses: {
      salaries: number;
      rent: number;
      utilities: number;
      other: number;
      total: number;
    };
    loanLossProvisions: {
      doubtfulDebts: number;
      writeOffs: number;
      total: number;
    };
    otherExpenses: number;
    totalExpenses: number;
  };
  netProfitLoss: number;
  profitMargin: number;
  generatedAt: string;
}

const ProfitLossReport = () => {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState<ProfitLossReportDTO | null>(null);
  const { session } = useAuth();
  const { toast } = useToast();

  const handleGenerateReport = async () => {
    if (!startDate || !endDate) {
      toast({
        title: "Error",
        description: "Please select both start and end dates",
        variant: "destructive",
      });
      return;
    }

    if (new Date(startDate) > new Date(endDate)) {
      toast({
        title: "Error",
        description: "Start date must be before end date",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(
        `${API_BASE_URL}/reports/profit-loss?startDate=${startDate}&endDate=${endDate}`,
        {
          headers: { Authorization: `Bearer ${session?.token}` },
        }
      );

      if (response.ok) {
        const data = await response.json();
        setReport(data.data);
        toast({
          title: "Success",
          description: "Report generated successfully",
        });
      } else {
        toast({
          title: "Error",
          description: "Failed to generate report",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to generate report",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleExportExcel = async () => {
    if (!startDate || !endDate) {
      toast({
        title: "Error",
        description: "Please generate a report first",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(
        `${API_BASE_URL}/reports/profit-loss/export/excel?startDate=${startDate}&endDate=${endDate}`,
        {
          headers: { Authorization: `Bearer ${session?.token}` },
        }
      );

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `profit_loss_${startDate}_to_${endDate}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({
          title: "Success",
          description: "Report exported to Excel successfully",
        });
      } else {
        toast({
          title: "Error",
          description: "Failed to export report",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Export failed",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleExportPdf = async () => {
    if (!startDate || !endDate) {
      toast({
        title: "Error",
        description: "Please generate a report first",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(
        `${API_BASE_URL}/reports/profit-loss/export/pdf?startDate=${startDate}&endDate=${endDate}`,
        {
          headers: { Authorization: `Bearer ${session?.token}` },
        }
      );

      if (response.ok) {
        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `profit_loss_${startDate}_to_${endDate}.pdf`;
        link.click();
        window.URL.revokeObjectURL(downloadUrl);
        toast({
          title: "Success",
          description: "Report exported to PDF successfully",
        });
      } else {
        toast({
          title: "Error",
          description: "Failed to export report",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Export failed",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "KES",
      minimumFractionDigits: 2,
    }).format(value);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-foreground">Profit & Loss Report</h1>
        <p className="text-muted-foreground">Generate and analyze financial performance</p>
      </div>

      {/* Date Selection Card */}
      <Card>
        <CardHeader>
          <CardTitle>Report Period</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-2">Start Date</label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">End Date</label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
          <Button
            onClick={handleGenerateReport}
            disabled={loading}
            className="w-full"
          >
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Generating...
              </>
            ) : (
              "Generate Report"
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      {report && (
        <>
          <div className="grid grid-cols-3 gap-4">
            <Card>
              <CardContent className="pt-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground mb-2">Total Revenue</p>
                  <p className="text-2xl font-bold text-green-600">
                    {formatCurrency(report.revenue.totalRevenue)}
                  </p>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground mb-2">Total Expenses</p>
                  <p className="text-2xl font-bold text-red-600">
                    {formatCurrency(report.expenses.totalExpenses)}
                  </p>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground mb-2">Net Profit/Loss</p>
                  <div className="flex items-center justify-center gap-2">
                    {report.netProfitLoss >= 0 ? (
                      <TrendingUp className="h-5 w-5 text-green-600" />
                    ) : (
                      <TrendingDown className="h-5 w-5 text-red-600" />
                    )}
                    <p
                      className={`text-2xl font-bold ${
                        report.netProfitLoss >= 0 ? "text-green-600" : "text-red-600"
                      }`}
                    >
                      {formatCurrency(report.netProfitLoss)}
                    </p>
                  </div>
                  <p className="text-xs text-muted-foreground mt-2">
                    Margin: {report.profitMargin.toFixed(2)}%
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Revenue Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle>Revenue Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Interest Income - From Loans</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.interestIncome.fromLoans)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Interest Income - From Savings</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.interestIncome.fromSavings)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Loan Processing Fees</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.feesAndCharges.loanProcessingFees)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Account Maintenance Fees</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.feesAndCharges.accountMaintenanceFees)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Other Fees</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.feesAndCharges.otherFees)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Other Income</span>
                  <span className="font-medium">
                    {formatCurrency(report.revenue.otherIncome)}
                  </span>
                </div>
                <div className="flex justify-between items-center pt-2 font-bold text-green-600">
                  <span>Total Revenue</span>
                  <span>{formatCurrency(report.revenue.totalRevenue)}</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Expenses Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle>Expenses Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="mb-4">
                  <h4 className="font-medium text-sm mb-2">Operating Expenses</h4>
                  <div className="space-y-2 ml-4">
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Salaries</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.operatingExpenses.salaries)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Rent</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.operatingExpenses.rent)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Utilities</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.operatingExpenses.utilities)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Other</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.operatingExpenses.other)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pt-2 font-medium text-sm">
                      <span>Subtotal</span>
                      <span>{formatCurrency(report.expenses.operatingExpenses.total)}</span>
                    </div>
                  </div>
                </div>

                <div className="mb-4">
                  <h4 className="font-medium text-sm mb-2">Loan Loss Provisions</h4>
                  <div className="space-y-2 ml-4">
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Doubtful Debts</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.loanLossProvisions.doubtfulDebts)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pb-2 border-b">
                      <span className="text-sm">Write-offs</span>
                      <span className="font-medium">
                        {formatCurrency(report.expenses.loanLossProvisions.writeOffs)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center pt-2 font-medium text-sm">
                      <span>Subtotal</span>
                      <span>{formatCurrency(report.expenses.loanLossProvisions.total)}</span>
                    </div>
                  </div>
                </div>

                <div className="flex justify-between items-center pb-2 border-b">
                  <span className="text-sm">Other Expenses</span>
                  <span className="font-medium">
                    {formatCurrency(report.expenses.otherExpenses)}
                  </span>
                </div>
                <div className="flex justify-between items-center pt-2 font-bold text-red-600">
                  <span>Total Expenses</span>
                  <span>{formatCurrency(report.expenses.totalExpenses)}</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Export Buttons */}
          <div className="flex gap-2 justify-end">
            <Button onClick={handleExportExcel} disabled={loading} variant="outline">
              <Download className="mr-2 h-4 w-4" />
              Export Excel
            </Button>
            <Button onClick={handleExportPdf} disabled={loading}>
              <Download className="mr-2 h-4 w-4" />
              Export PDF
            </Button>
          </div>
        </>
      )}
    </div>
  );
};

export default ProfitLossReport;