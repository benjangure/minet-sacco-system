import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Download, Filter, Eye } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { API_BASE_URL } from "@/config/api";

interface AuditLog {
  id: number;
  user: {
    id: number;
    username: string;
    firstName: string;
    lastName: string;
  };
  action: string;
  entityType: string;
  entityId: number;
  entityDetails: string;
  comments: string;
  timestamp: string;
  status: string;
  errorMessage?: string;
  ipAddress: string;
}

const actionColors: Record<string, string> = {
  APPROVE: "bg-green-100 text-green-800",
  REJECT: "bg-red-100 text-red-800",
  DISBURSE: "bg-blue-100 text-blue-800",
  CREATE: "bg-purple-100 text-purple-800",
  UPDATE: "bg-yellow-100 text-yellow-800",
  DELETE: "bg-red-200 text-red-900",
};

const statusColors: Record<string, string> = {
  SUCCESS: "bg-green-50 text-green-700",
  FAILURE: "bg-red-50 text-red-700",
};

export const AuditTrail = () => {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  // Filters
  const [actionFilter, setActionFilter] = useState("");
  const [entityTypeFilter, setEntityTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const { toast } = useToast();
  const { session } = useAuth();

  useEffect(() => {
    if (session) {
      fetchAuditLogs();
    }
  }, [session, page, pageSize, actionFilter, entityTypeFilter, statusFilter, startDate, endDate]);

  const fetchAuditLogs = async () => {
    setLoading(true);
    try {
      // Validate date range
      if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
        toast({ title: "Error", description: "Start date cannot be later than end date", variant: "destructive" });
        setLoading(false);
        return;
      }

      // Use /api/audit endpoint with optional filters
      let url = `${API_BASE_URL}/audit?page=${page}&size=${pageSize}`;

      if (actionFilter) url += `&action=${actionFilter}`;
      if (entityTypeFilter) url += `&entityType=${entityTypeFilter}`;
      if (statusFilter) url += `&status=${statusFilter}`;
      if (startDate && endDate) {
        url += `&startDate=${startDate}T00:00:00&endDate=${endDate}T23:59:59`;
      }

      const response = await fetch(url, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setAuditLogs(data.data.content || []);
        setTotalElements(data.data.totalElements || 0);
        setTotalPages(data.data.totalPages || 0);
      } else {
        console.error("Error response:", response.status);
        toast({ title: "Error", description: "Failed to fetch audit logs", variant: "destructive" });
      }
    } catch (error) {
      console.error("Error fetching audit logs:", error);
      toast({ title: "Error", description: "Failed to fetch audit logs", variant: "destructive" });
    }
    setLoading(false);
  };

  const handleExport = async () => {
    try {
      let url = `${API_BASE_URL}/audit?page=0&size=10000`;
      if (actionFilter) url += `&action=${actionFilter}`;
      if (entityTypeFilter) url += `&entityType=${entityTypeFilter}`;
      if (statusFilter) url += `&status=${statusFilter}`;
      if (startDate && endDate) {
        url += `&startDate=${startDate}T00:00:00&endDate=${endDate}T23:59:59`;
      }

      const response = await fetch(url, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        const logs = data.data.content || [];

        // Create CSV
        const headers = ["Timestamp", "User", "Action", "Entity Type", "Entity ID", "Details", "Comments", "Status", "IP Address"];
        const rows = logs.map((log: AuditLog) => [
          new Date(log.timestamp).toLocaleString(),
          `${log.user.firstName} ${log.user.lastName}`,
          log.action,
          log.entityType,
          log.entityId,
          log.entityDetails,
          log.comments || "",
          log.status,
          log.ipAddress,
        ]);

        const csv = [headers, ...rows].map((row: (string | number)[]) => row.map((cell: string | number) => `"${cell}"`).join(",")).join("\n");
        const blob = new Blob([csv], { type: "text/csv" });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `audit-trail-${new Date().toISOString().split("T")[0]}.csv`;
        a.click();

        toast({ title: "Success", description: "Audit trail exported successfully" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to export audit trail", variant: "destructive" });
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const handleViewDetails = (log: AuditLog) => {
    setSelectedLog(log);
    setShowDetailsModal(true);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-foreground">Audit Trail</h1>
        <p className="text-muted-foreground">Track all system actions and changes</p>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Filters</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <div>
              <label className="text-sm font-medium">Action</label>
              <Select value={actionFilter || "all"} onValueChange={(val) => setActionFilter(val === "all" ? "" : val)}>
                <SelectTrigger>
                  <SelectValue placeholder="All actions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All actions</SelectItem>
                  <SelectItem value="APPROVE">Approve</SelectItem>
                  <SelectItem value="REJECT">Reject</SelectItem>
                  <SelectItem value="DISBURSE">Disburse</SelectItem>
                  <SelectItem value="CREATE">Create</SelectItem>
                  <SelectItem value="UPDATE">Update</SelectItem>
                  <SelectItem value="DELETE">Delete</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="text-sm font-medium">Entity Type</label>
              <Select value={entityTypeFilter || "all"} onValueChange={(val) => setEntityTypeFilter(val === "all" ? "" : val)}>
                <SelectTrigger>
                  <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All types</SelectItem>
                  <SelectItem value="LOAN">Loan</SelectItem>
                  <SelectItem value="DEPOSIT_REQUEST">Deposit Request</SelectItem>
                  <SelectItem value="MEMBER">Member</SelectItem>
                  <SelectItem value="GUARANTOR">Guarantor</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="text-sm font-medium">Status</label>
              <Select value={statusFilter || "all"} onValueChange={(val) => setStatusFilter(val === "all" ? "" : val)}>
                <SelectTrigger>
                  <SelectValue placeholder="All statuses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All statuses</SelectItem>
                  <SelectItem value="SUCCESS">Success</SelectItem>
                  <SelectItem value="FAILURE">Failure</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="text-sm font-medium">Start Date</label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>

            <div>
              <label className="text-sm font-medium">End Date</label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>

          <div className="flex gap-2">
            <Button 
              onClick={fetchAuditLogs} 
              variant="outline" 
              size="sm"
              disabled={startDate && endDate && new Date(startDate) > new Date(endDate)}
            >
              <Filter className="mr-2 h-4 w-4" />
              Apply Filters
            </Button>
            <Button onClick={handleExport} variant="outline" size="sm">
              <Download className="mr-2 h-4 w-4" />
              Export CSV
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Audit Logs Table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">
            Audit Logs ({totalElements} total)
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center py-8">
              <p className="text-muted-foreground">Loading audit logs...</p>
            </div>
          ) : auditLogs.length === 0 ? (
            <div className="flex justify-center py-8">
              <p className="text-muted-foreground">No audit logs found</p>
            </div>
          ) : (
            <>
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
                      <TableHead>IP Address</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {auditLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell className="text-xs whitespace-nowrap">
                          {formatDate(log.timestamp)}
                        </TableCell>
                        <TableCell className="text-sm">
                          <div className="font-medium">{log.user.firstName} {log.user.lastName}</div>
                          <div className="text-xs text-muted-foreground">{log.user.username}</div>
                        </TableCell>
                        <TableCell>
                          <Badge className={`${actionColors[log.action] || "bg-gray-100 text-gray-800"} text-xs`}>
                            {log.action}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm">
                          <div>{log.entityType}</div>
                          <div className="text-xs text-muted-foreground">ID: {log.entityId}</div>
                        </TableCell>
                        <TableCell className="text-xs max-w-xs truncate">
                          {log.entityDetails}
                        </TableCell>
                        <TableCell className="text-xs max-w-xs truncate">
                          {log.comments || "-"}
                        </TableCell>
                        <TableCell>
                          <Badge className={`${statusColors[log.status] || "bg-gray-100 text-gray-800"} text-xs`}>
                            {log.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-xs">{log.ipAddress}</TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewDetails(log)}
                            title="View full details"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <div className="text-sm text-muted-foreground">
                  Page {page + 1} of {totalPages}
                </div>
                <div className="flex gap-2">
                  <Button
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    variant="outline"
                    size="sm"
                  >
                    Previous
                  </Button>
                  <Button
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                    variant="outline"
                    size="sm"
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Details Modal */}
      <Dialog open={showDetailsModal} onOpenChange={setShowDetailsModal}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Audit Log Details</DialogTitle>
          </DialogHeader>
          {selectedLog && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Timestamp</label>
                  <p className="text-sm">{formatDate(selectedLog.timestamp)}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Log ID</label>
                  <p className="text-sm">{selectedLog.id}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">User</label>
                  <p className="text-sm font-medium">{selectedLog.user.firstName} {selectedLog.user.lastName}</p>
                  <p className="text-xs text-muted-foreground">{selectedLog.user.username}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Action</label>
                  <Badge className={`${actionColors[selectedLog.action] || "bg-gray-100 text-gray-800"} text-xs`}>
                    {selectedLog.action}
                  </Badge>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Entity Type</label>
                  <p className="text-sm">{selectedLog.entityType}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Entity ID</label>
                  <p className="text-sm">{selectedLog.entityId}</p>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-muted-foreground">Entity Details</label>
                <p className="text-sm bg-muted p-2 rounded break-words">{selectedLog.entityDetails}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-muted-foreground">Comments</label>
                <p className="text-sm bg-muted p-2 rounded break-words">{selectedLog.comments || "-"}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Status</label>
                  <Badge className={`${statusColors[selectedLog.status] || "bg-gray-100 text-gray-800"} text-xs`}>
                    {selectedLog.status}
                  </Badge>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">IP Address</label>
                  <p className="text-sm">{selectedLog.ipAddress}</p>
                </div>
              </div>

              {selectedLog.errorMessage && (
                <div>
                  <label className="text-sm font-medium text-muted-foreground">Error Message</label>
                  <p className="text-sm bg-red-50 p-2 rounded text-red-700 break-words">{selectedLog.errorMessage}</p>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};
