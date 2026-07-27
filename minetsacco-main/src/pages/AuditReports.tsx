import { getApiBaseUrl } from "../config/api";
import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { AlertCircle, Download, TrendingUp, Activity, BarChart3 } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = getApiBaseUrl();

interface AuditLog {
  id: number;
  username: string;
  action: string;
  entityType: string;
  entityId: number;
  entityDetails: string;
  comments: string;
  status: string;
  timestamp: string;
  ipAddress?: string;
}

interface AuditSummary {
  totalActions: number;
  totalFinancialActions: number;
  totalOtherActions: number;
  successCount: number;
  failureCount: number;
  actionsByType: Record<string, number>;
  actionsByUser: Record<string, number>;
}

const actionColors: Record<string, string> = {
  APPROVE: "bg-green-100 text-green-800",
  REJECT: "bg-red-100 text-red-800",
  DISBURSE: "bg-blue-100 text-blue-800",
  REPAY: "bg-cyan-100 text-cyan-800",
  LOAN_REPAYMENT_APPROVED: "bg-cyan-100 text-cyan-800",
  LOAN_REPAYMENT_REJECTED: "bg-red-100 text-red-800",
  DEPOSIT: "bg-emerald-100 text-emerald-800",
  WITHDRAWAL: "bg-orange-100 text-orange-800",
  ACTIVATE: "bg-emerald-100 text-emerald-800",
  GUARANTOR_PLEDGE_REDUCED: "bg-teal-100 text-teal-800",
  GUARANTOR_DEFAULT_DEBIT: "bg-red-200 text-red-900",
  BULK_UPLOAD: "bg-purple-100 text-purple-800",
  BULK_APPROVE: "bg-green-200 text-green-900",
  BULK_REJECT: "bg-red-200 text-red-900",
  UPDATE_FUND_CONFIG: "bg-yellow-100 text-yellow-800",
};

const AuditReports = () => {
  const [financialLogs, setFinancialLogs] = useState<AuditLog[]>([]);
  const [allActivityLogs, setAllActivityLogs] = useState<AuditLog[]>([]);
  const [summary, setSummary] = useState<AuditSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const { toast } = useToast();
  const { session, role } = useAuth();

  const canViewReports = ["ADMIN", "AUDITOR"].includes(role || "");

  useEffect(() => {
    if (!canViewReports) return;
    fetchReports();
  }, [session]);

  const fetchReports = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (startDate) params.append("startDate", startDate);
      if (endDate) params.append("endDate", endDate);
      const query = params.toString() ? `?${params}` : "";

      const [financialRes, allRes, summaryRes] = await Promise.all([
        fetch(`${API_BASE_URL}/audit-reports/financial-actions${query}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
        fetch(`${API_BASE_URL}/audit-reports/all-activity${query}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
        fetch(`${API_BASE_URL}/audit-reports/summary${query}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
      ]);

      if (!financialRes.ok) {
        console.error("Financial actions error:", financialRes.status, await financialRes.text());
      } else {
        const data = await financialRes.json();
        setFinancialLogs(data.data || []);
      }
      
      if (!allRes.ok) {
        console.error("All activity error:", allRes.status, await allRes.text());
      } else {
        const data = await allRes.json();
        setAllActivityLogs(data.data || []);
      }
      
      if (!summaryRes.ok) {
        console.error("Summary error:", summaryRes.status, await summaryRes.text());
      } else {
        const data = await summaryRes.json();
        setSummary(data.data);
      }
    } catch (error) {
      console.error("Error fetching audit reports:", error);
      toast({ title: "Error", description: "Failed to fetch audit reports", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const handleExportCSV = (data: AuditLog[], filename: string) => {
    const csv = [
      ["Timestamp", "User", "Action", "Entity Type", "Entity ID", "Details", "Comments", "Status"],
      ...data.map(log => [
        new Date(log.timestamp).toLocaleString(),
        log.username,
        log.action,
        log.entityType,
        log.entityId,
        log.entityDetails || "",
        log.comments || "",
        log.status,
      ]),
    ]
      .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(","))
      .join("\n");

    const blob = new Blob([csv], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const LogTable = ({ logs, emptyMessage }: { logs: AuditLog[]; emptyMessage: string }) => (
    loading ? (
      <div className="text-center py-10 text-muted-foreground">Loading...</div>
    ) : logs.length === 0 ? (
      <Alert>
        <AlertCircle className="h-4 w-4" />
        <AlertDescription>{emptyMessage}</AlertDescription>
      </Alert>
    ) : (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Timestamp</TableHead>
              <TableHead>User</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Entity</TableHead>
              <TableHead>Details</TableHead>
              <TableHead>Comments</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map(log => (
              <TableRow key={log.id}>
                <TableCell className="text-xs whitespace-nowrap">
                  {new Date(log.timestamp).toLocaleString()}
                </TableCell>
                <TableCell className="text-sm font-medium">{log.username}</TableCell>
                <TableCell>
                  <Badge className={`text-xs ${actionColors[log.action] || "bg-gray-100 text-gray-800"}`}>
                    {log.action.replace(/_/g, " ")}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm">
                  <div>{log.entityType}</div>
                  {log.entityId && <div className="text-xs text-muted-foreground">ID: {log.entityId}</div>}
                </TableCell>
                <TableCell className="text-xs max-w-xs truncate" title={log.entityDetails}>
                  {log.entityDetails || "—"}
                </TableCell>
                <TableCell className="text-xs max-w-xs truncate" title={log.comments}>
                  {log.comments || "—"}
                </TableCell>
                <TableCell>
                  <Badge className={`text-xs ${log.status === "SUCCESS" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>
                    {log.status}
                  </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    )
  );

  if (!canViewReports) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only Admin and Auditor can view audit reports.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground">Audit Reports</h1>
        <p className="text-muted-foreground">Review system activity, financial actions, and compliance logs</p>
      </div>

      {/* Date Range Filter */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex-1 min-w-[160px]">
              <label className="text-sm font-medium">Start Date</label>
              <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="mt-1" />
            </div>
            <div className="flex-1 min-w-[160px]">
              <label className="text-sm font-medium">End Date</label>
              <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="mt-1" />
            </div>
            <Button onClick={fetchReports} disabled={loading}>
              {loading ? "Loading..." : "Apply Filter"}
            </Button>
            <Button variant="outline" onClick={() => { setStartDate(""); setEndDate(""); }} disabled={loading}>
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Total Actions</p>
              <p className="text-2xl font-bold">{(summary.totalActions || 0).toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Financial Actions</p>
              <p className="text-2xl font-bold text-blue-600">{(summary.totalFinancialActions || 0).toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Other Actions</p>
              <p className="text-2xl font-bold text-gray-600">{(summary.totalOtherActions || 0).toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Successful</p>
              <p className="text-2xl font-bold text-green-600">{(summary.successCount || 0).toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Failed</p>
              <p className="text-2xl font-bold text-red-600">{(summary.failureCount || 0).toLocaleString()}</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tabs */}
      <Tabs defaultValue="financial" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="financial" className="flex items-center gap-2">
            <TrendingUp className="h-4 w-4" />
            Financial Actions
          </TabsTrigger>
          <TabsTrigger value="all-activity" className="flex items-center gap-2">
            <Activity className="h-4 w-4" />
            All Activity
          </TabsTrigger>
          <TabsTrigger value="summary" className="flex items-center gap-2">
            <BarChart3 className="h-4 w-4" />
            Summary
          </TabsTrigger>
        </TabsList>

        {/* Financial Actions Tab */}
        <TabsContent value="financial">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base">Financial Actions ({financialLogs.length})</CardTitle>
                  <p className="text-xs text-muted-foreground mt-1">
                    Loan approvals, disbursements, repayments, deposits, withdrawals, and guarantor actions
                  </p>
                </div>
                <Button variant="outline" size="sm" onClick={() => handleExportCSV(financialLogs, "financial-actions.csv")}>
                  <Download className="mr-2 h-4 w-4" />
                  Export CSV
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <LogTable logs={financialLogs} emptyMessage="No financial action logs found for the selected period" />
            </CardContent>
          </Card>
        </TabsContent>

        {/* All Activity Tab */}
        <TabsContent value="all-activity">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base">All Activity ({allActivityLogs.length})</CardTitle>
                  <p className="text-xs text-muted-foreground mt-1">
                    Every recorded system action including configuration changes, bulk operations, and support tickets
                  </p>
                </div>
                <Button variant="outline" size="sm" onClick={() => handleExportCSV(allActivityLogs, "all-activity.csv")}>
                  <Download className="mr-2 h-4 w-4" />
                  Export CSV
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <LogTable logs={allActivityLogs} emptyMessage="No activity logs found for the selected period" />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Summary Tab */}
        <TabsContent value="summary">
          {!summary ? (
            <div className="text-center py-10 text-muted-foreground">Loading summary...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Actions by Type</CardTitle>
                  <p className="text-xs text-muted-foreground">How many times each action was performed</p>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2 max-h-96 overflow-y-auto">
                    {Object.entries(summary.actionsByType)
                      .sort(([, a], [, b]) => b - a)
                      .map(([action, count]) => (
                        <div key={action} className="flex justify-between items-center py-1 border-b last:border-0">
                          <Badge className={`text-xs ${actionColors[action] || "bg-gray-100 text-gray-800"}`}>
                            {action.replace(/_/g, " ")}
                          </Badge>
                          <span className="text-sm font-semibold">{count}</span>
                        </div>
                      ))}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Actions by User</CardTitle>
                  <p className="text-xs text-muted-foreground">Most active staff members</p>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2 max-h-96 overflow-y-auto">
                    {Object.entries(summary.actionsByUser)
                      .sort(([, a], [, b]) => b - a)
                      .map(([user, count]) => (
                        <div key={user} className="flex justify-between items-center py-1 border-b last:border-0">
                          <span className="text-sm font-medium">{user}</span>
                          <Badge variant="outline">{count} actions</Badge>
                        </div>
                      ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default AuditReports;
