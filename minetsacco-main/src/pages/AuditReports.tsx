import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { AlertCircle, Download } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface AuditLog {
  id: number;
  user: { username: string };
  action: string;
  entityType: string;
  entityId: number;
  details: string;
  status: string;
  timestamp: string;
}

interface AuditSummary {
  totalActions: number;
  totalAccess: number;
  totalModifications: number;
  actionsByType: Record<string, number>;
  actionsByUser: Record<string, number>;
}

const AuditReports = () => {
  const [dataAccessLogs, setDataAccessLogs] = useState<AuditLog[]>([]);
  const [userActivityLogs, setUserActivityLogs] = useState<AuditLog[]>([]);
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

      const [accessRes, activityRes, summaryRes] = await Promise.all([
        fetch(`${API_BASE_URL}/audit-reports/data-access?${params}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
        fetch(`${API_BASE_URL}/audit-reports/user-activity?${params}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
        fetch(`${API_BASE_URL}/audit-reports/summary?${params}`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
      ]);

      if (accessRes.ok) {
        const data = await accessRes.json();
        setDataAccessLogs(data.data || []);
      }

      if (activityRes.ok) {
        const data = await activityRes.json();
        setUserActivityLogs(data.data || []);
      }

      if (summaryRes.ok) {
        const data = await summaryRes.json();
        setSummary(data.data);
      }
    } catch (error) {
      console.error("Error fetching reports:", error);
      toast({ title: "Error", description: "Failed to fetch reports", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const handleExportCSV = (data: AuditLog[], filename: string) => {
    const csv = [
      ["User", "Action", "Entity Type", "Entity ID", "Details", "Status", "Timestamp"],
      ...data.map(log => [
        log.user.username,
        log.action,
        log.entityType,
        log.entityId,
        log.details,
        log.status,
        new Date(log.timestamp).toLocaleString(),
      ]),
    ]
      .map(row => row.map(cell => `"${cell}"`).join(","))
      .join("\n");

    const blob = new Blob([csv], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
  };

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
        <p className="text-muted-foreground">View system audit logs and compliance reports</p>
      </div>

      {/* Date Range Filter */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex gap-4 items-end">
            <div className="flex-1">
              <label className="text-sm font-medium">Start Date</label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="mt-1"
              />
            </div>
            <div className="flex-1">
              <label className="text-sm font-medium">End Date</label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="mt-1"
              />
            </div>
            <Button onClick={fetchReports} disabled={loading}>
              {loading ? "Loading..." : "Filter"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Total Actions</p>
              <p className="text-2xl font-bold">{summary.totalActions}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Data Access</p>
              <p className="text-2xl font-bold text-blue-600">{summary.totalAccess}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Modifications</p>
              <p className="text-2xl font-bold text-orange-600">{summary.totalModifications}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-xs text-muted-foreground">Active Users</p>
              <p className="text-2xl font-bold">{Object.keys(summary.actionsByUser).length}</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tabs */}
      <Tabs defaultValue="data-access" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="data-access">Data Access Logs</TabsTrigger>
          <TabsTrigger value="user-activity">User Activity</TabsTrigger>
          <TabsTrigger value="summary">Summary</TabsTrigger>
        </TabsList>

        {/* Data Access Logs Tab */}
        <TabsContent value="data-access">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">Data Access Logs ({dataAccessLogs.length})</CardTitle>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleExportCSV(dataAccessLogs, "data-access-logs.csv")}
                >
                  <Download className="mr-2 h-4 w-4" />
                  Export CSV
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading...</div>
              ) : dataAccessLogs.length === 0 ? (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>No data access logs found</AlertDescription>
                </Alert>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>User</TableHead>
                        <TableHead>Entity Type</TableHead>
                        <TableHead>Details</TableHead>
                        <TableHead>Timestamp</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {dataAccessLogs.map(log => (
                        <TableRow key={log.id}>
                          <TableCell className="text-sm font-medium">{log.user.username}</TableCell>
                          <TableCell>
                            <Badge variant="outline">{log.entityType}</Badge>
                          </TableCell>
                          <TableCell className="text-sm">{log.details}</TableCell>
                          <TableCell className="text-sm">
                            {new Date(log.timestamp).toLocaleString()}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* User Activity Tab */}
        <TabsContent value="user-activity">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">User Activity Logs ({userActivityLogs.length})</CardTitle>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleExportCSV(userActivityLogs, "user-activity-logs.csv")}
                >
                  <Download className="mr-2 h-4 w-4" />
                  Export CSV
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading...</div>
              ) : userActivityLogs.length === 0 ? (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>No user activity logs found</AlertDescription>
                </Alert>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>User</TableHead>
                        <TableHead>Action</TableHead>
                        <TableHead>Entity</TableHead>
                        <TableHead>Details</TableHead>
                        <TableHead>Timestamp</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {userActivityLogs.map(log => (
                        <TableRow key={log.id}>
                          <TableCell className="text-sm font-medium">{log.user.username}</TableCell>
                          <TableCell>
                            <Badge>{log.action}</Badge>
                          </TableCell>
                          <TableCell className="text-sm">{log.entityType}</TableCell>
                          <TableCell className="text-sm">{log.details}</TableCell>
                          <TableCell className="text-sm">
                            {new Date(log.timestamp).toLocaleString()}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Summary Tab */}
        <TabsContent value="summary">
          {summary && (
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Actions by Type</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {Object.entries(summary.actionsByType).map(([action, count]) => (
                      <div key={action} className="flex justify-between items-center">
                        <span className="text-sm">{action}</span>
                        <Badge variant="outline">{count}</Badge>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Actions by User</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {Object.entries(summary.actionsByUser).map(([user, count]) => (
                      <div key={user} className="flex justify-between items-center">
                        <span className="text-sm">{user}</span>
                        <Badge variant="outline">{count}</Badge>
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
