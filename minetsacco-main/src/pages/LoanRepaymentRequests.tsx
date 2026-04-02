import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, Download, CheckCircle, XCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface RepaymentRequest {
  id: number;
  loan: {
    id: number;
    loanNumber: string;
    amount: number;
    outstandingBalance: number;
  };
  member: {
    id: number;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  };
  amount: number;
  confirmedAmount?: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  description: string;
  proofFileName: string;
  createdAt: string;
  approvedAt?: string;
  approvedBy?: string;
  rejectionReason?: string;
}

export default function LoanRepaymentRequests() {
  const [requests, setRequests] = useState<RepaymentRequest[]>([]);
  const [filteredRequests, setFilteredRequests] = useState<RepaymentRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [selectedRequest, setSelectedRequest] = useState<RepaymentRequest | null>(null);
  const [showApprovalDialog, setShowApprovalDialog] = useState(false);
  const [showRejectionDialog, setShowRejectionDialog] = useState(false);
  const [confirmedAmount, setConfirmedAmount] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [processing, setProcessing] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    fetchRequests();
  }, []);

  useEffect(() => {
    if (statusFilter === 'ALL') {
      setFilteredRequests(requests);
    } else {
      setFilteredRequests(requests.filter(r => r.status === statusFilter));
    }
  }, [statusFilter, requests]);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const response = await api.get('/teller/loan-repayments');
      setRequests(response.data || []);
    } catch (err: any) {
      console.error('Error fetching requests:', err);
      toast({
        title: 'Error',
        description: 'Failed to load repayment requests',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadProof = async (requestId: number, fileName: string) => {
    try {
      const response = await api.get(
        `/teller/loan-repayments/${requestId}/proof/download`,
        { responseType: 'blob' }
      );
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.parentElement?.removeChild(link);
    } catch (err: any) {
      toast({
        title: 'Error',
        description: 'Failed to download proof file',
        variant: 'destructive'
      });
    }
  };

  const handleApprove = async () => {
    if (!selectedRequest || !confirmedAmount) {
      toast({
        title: 'Error',
        description: 'Please enter confirmed amount',
        variant: 'destructive'
      });
      return;
    }

    const amount = parseFloat(confirmedAmount);
    if (amount <= 0 || amount > selectedRequest.amount) {
      toast({
        title: 'Error',
        description: 'Confirmed amount must be between 0 and requested amount',
        variant: 'destructive'
      });
      return;
    }

    setProcessing(true);
    try {
      await api.post(
        `/teller/loan-repayments/${selectedRequest.id}/approve`,
        null,
        { params: { confirmedAmount: amount } }
      );

      toast({
        title: 'Success',
        description: 'Repayment request approved successfully'
      });

      setShowApprovalDialog(false);
      setConfirmedAmount('');
      setSelectedRequest(null);
      fetchRequests();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to approve request',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest || !rejectionReason) {
      toast({
        title: 'Error',
        description: 'Please enter rejection reason',
        variant: 'destructive'
      });
      return;
    }

    setProcessing(true);
    try {
      await api.post(
        `/teller/loan-repayments/${selectedRequest.id}/reject`,
        null,
        { params: { rejectionReason } }
      );

      toast({
        title: 'Success',
        description: 'Repayment request rejected successfully'
      });

      setShowRejectionDialog(false);
      setRejectionReason('');
      setSelectedRequest(null);
      fetchRequests();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to reject request',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">Pending</span>;
      case 'APPROVED':
        return <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">Approved</span>;
      case 'REJECTED':
        return <span className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm font-medium">Rejected</span>;
      default:
        return <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm font-medium">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">Loan Repayment Requests</h1>
        <p className="text-muted-foreground mt-2">
          Manage bank transfer loan repayment requests from members
        </p>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex gap-4 items-end">
            <div className="flex-1">
              <Label htmlFor="status">Filter by Status</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="APPROVED">Approved</SelectItem>
                  <SelectItem value="REJECTED">Rejected</SelectItem>
                  <SelectItem value="ALL">All</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button onClick={fetchRequests} variant="outline">
              Refresh
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-muted-foreground text-sm">Pending Requests</p>
              <p className="text-3xl font-bold text-yellow-600">
                {requests.filter(r => r.status === 'PENDING').length}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-muted-foreground text-sm">Approved</p>
              <p className="text-3xl font-bold text-green-600">
                {requests.filter(r => r.status === 'APPROVED').length}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-muted-foreground text-sm">Rejected</p>
              <p className="text-3xl font-bold text-red-600">
                {requests.filter(r => r.status === 'REJECTED').length}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Requests Table */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
        </div>
      ) : filteredRequests.length === 0 ? (
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground">No repayment requests found</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredRequests.map((request) => (
            <Card key={request.id} className="hover:shadow-md transition">
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Left Column */}
                  <div className="space-y-4">
                    <div>
                      <p className="text-sm text-muted-foreground">Member</p>
                      <p className="font-semibold">
                        {request.member.firstName} {request.member.lastName}
                      </p>
                      <p className="text-sm text-muted-foreground">{request.member.phoneNumber}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Loan</p>
                      <p className="font-semibold">{request.loan.loanNumber}</p>
                      <p className="text-sm">Outstanding: {formatCurrency(request.loan.outstandingBalance)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Requested Amount</p>
                      <p className="font-semibold text-lg">{formatCurrency(request.amount)}</p>
                    </div>
                  </div>

                  {/* Right Column */}
                  <div className="space-y-4">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="text-sm text-muted-foreground">Status</p>
                        {getStatusBadge(request.status)}
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-muted-foreground">Submitted</p>
                        <p className="text-sm">{formatDate(request.createdAt)}</p>
                      </div>
                    </div>

                    {request.status === 'APPROVED' && (
                      <div>
                        <p className="text-sm text-muted-foreground">Confirmed Amount</p>
                        <p className="font-semibold text-green-600">
                          {formatCurrency(request.confirmedAmount || 0)}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          Approved by {request.approvedBy} on {formatDate(request.approvedAt || '')}
                        </p>
                      </div>
                    )}

                    {request.status === 'REJECTED' && (
                      <div>
                        <p className="text-sm text-muted-foreground">Rejection Reason</p>
                        <p className="text-sm text-red-600 font-medium">{request.rejectionReason}</p>
                        <p className="text-xs text-muted-foreground">
                          Rejected by {request.approvedBy} on {formatDate(request.approvedAt || '')}
                        </p>
                        <p className="text-xs text-blue-600 mt-2">
                          Member can resubmit with corrected information
                        </p>
                      </div>
                    )}

                    {/* Actions */}
                    <div className="flex gap-2 pt-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDownloadProof(request.id, request.proofFileName)}
                        className="flex-1"
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Proof
                      </Button>
                      {request.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            variant="default"
                            onClick={() => {
                              setSelectedRequest(request);
                              setConfirmedAmount(request.amount.toString());
                              setShowApprovalDialog(true);
                            }}
                            className="flex-1 bg-green-600 hover:bg-green-700"
                          >
                            <CheckCircle className="h-4 w-4 mr-2" />
                            Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => {
                              setSelectedRequest(request);
                              setShowRejectionDialog(true);
                            }}
                            className="flex-1"
                          >
                            <XCircle className="h-4 w-4 mr-2" />
                            Reject
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Approval Dialog */}
      <Dialog open={showApprovalDialog} onOpenChange={setShowApprovalDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Approve Repayment Request</DialogTitle>
            <DialogDescription>
              Review and confirm the repayment amount
            </DialogDescription>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4">
              <div className="bg-muted p-4 rounded-lg space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Member:</span>
                  <span className="font-semibold">
                    {selectedRequest.member.firstName} {selectedRequest.member.lastName}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Loan:</span>
                  <span className="font-semibold">{selectedRequest.loan.loanNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Outstanding:</span>
                  <span className="font-semibold">
                    {formatCurrency(selectedRequest.loan.outstandingBalance)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Requested:</span>
                  <span className="font-semibold">
                    {formatCurrency(selectedRequest.amount)}
                  </span>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmed">Confirmed Amount (KES)</Label>
                <Input
                  id="confirmed"
                  type="number"
                  step="0.01"
                  value={confirmedAmount}
                  onChange={(e) => setConfirmedAmount(e.target.value)}
                  max={selectedRequest.amount}
                  max={selectedRequest.loan.outstandingBalance}
                />
                <p className="text-xs text-muted-foreground">
                  Max: {formatCurrency(Math.min(selectedRequest.amount, selectedRequest.loan.outstandingBalance))}
                </p>
              </div>

              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  The confirmed amount will be deducted from the loan balance and recorded as a transaction.
                </AlertDescription>
              </Alert>

              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowApprovalDialog(false)}
                  disabled={processing}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleApprove}
                  disabled={processing}
                  className="flex-1 bg-green-600 hover:bg-green-700"
                >
                  {processing ? 'Processing...' : 'Approve'}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Rejection Dialog */}
      <Dialog open={showRejectionDialog} onOpenChange={setShowRejectionDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject Repayment Request</DialogTitle>
            <DialogDescription>
              Provide a reason for rejecting this request
            </DialogDescription>
          </DialogHeader>

          {selectedRequest && (
            <div className="space-y-4">
              <div className="bg-muted p-4 rounded-lg space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Member:</span>
                  <span className="font-semibold">
                    {selectedRequest.member.firstName} {selectedRequest.member.lastName}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Amount:</span>
                  <span className="font-semibold">
                    {formatCurrency(selectedRequest.amount)}
                  </span>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="reason">Rejection Reason</Label>
                <textarea
                  id="reason"
                  className="w-full px-3 py-2 border rounded-md text-sm"
                  rows={4}
                  placeholder="e.g., Proof document is unclear, Amount mismatch, etc."
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                />
              </div>

              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  The member will be notified of the rejection and can resubmit with corrected information.
                </AlertDescription>
              </Alert>

              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowRejectionDialog(false)}
                  disabled={processing}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleReject}
                  disabled={processing}
                  variant="destructive"
                  className="flex-1"
                >
                  {processing ? 'Processing...' : 'Reject'}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
