import { useState, useEffect } from 'react';
import axios from 'axios';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { CheckCircle, XCircle, AlertCircle, Eye } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';
import { downloadAndOpenFile } from '@/utils/downloadHelper';

interface DepositRequest {
  id: number;
  memberNumber: string;
  memberName: string;
  accountType: string;
  claimedAmount: number;
  confirmedAmount?: number;
  description: string;
  receiptFileName: string;
  status: string;
  createdAt: string;
}

interface DepositApprovalPanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onApprovalChange?: () => void;
}

export default function DepositApprovalPanel({ open, onOpenChange, onApprovalChange }: DepositApprovalPanelProps) {
  const { session } = useAuth();
  const [requests, setRequests] = useState<DepositRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<DepositRequest | null>(null);
  const [approvalDialogOpen, setApprovalDialogOpen] = useState(false);
  const [confirmedAmount, setConfirmedAmount] = useState('');
  const [approvalNotes, setApprovalNotes] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [isRejecting, setIsRejecting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (open) {
      fetchPendingRequests();
    }
  }, [open]);

  const fetchPendingRequests = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/teller/deposit-requests/pending`, {
        headers: { Authorization: `Bearer ${session?.token}` }
      });
      setRequests(response.data.data || []);
    } catch (err: any) {
      console.error('Error fetching requests:', err);
      toast({
        title: 'Error',
        description: 'Failed to load deposit requests',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSelectRequest = (request: DepositRequest) => {
    setSelectedRequest(request);
    setConfirmedAmount(request.claimedAmount.toString());
    setApprovalNotes('');
    setRejectionReason('');
    setIsRejecting(false);
    setApprovalDialogOpen(true);
  };

  const handleApprove = async () => {
    if (!selectedRequest || !confirmedAmount) {
      toast({ title: 'Error', description: 'Please enter confirmed amount', variant: 'destructive' });
      return;
    }

    const amount = parseFloat(confirmedAmount);
    if (isNaN(amount) || amount <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid amount', variant: 'destructive' });
      return;
    }

    setSubmitting(true);
    try {
      const params = new URLSearchParams();
      params.append('confirmedAmount', amount.toString());
      if (approvalNotes.trim()) {
        params.append('approvalNotes', approvalNotes);
      }

      await axios.post(
        `${API_BASE_URL}/teller/deposit-requests/${selectedRequest.id}/approve?${params.toString()}`,
        {},
        {
          headers: { Authorization: `Bearer ${session?.token}` }
        }
      );

      toast({ title: 'Success', description: 'Deposit request approved successfully' });
      setApprovalDialogOpen(false);
      setSelectedRequest(null);
      fetchPendingRequests();
      onApprovalChange?.();
    } catch (err: any) {
      console.error('Approval error:', err);
      toast({
        title: 'Error',
        description: err.response?.data?.message || err.response?.data?.error || 'Failed to approve request',
        variant: 'destructive'
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest || !rejectionReason.trim()) {
      toast({ title: 'Error', description: 'Please provide rejection reason', variant: 'destructive' });
      return;
    }

    setSubmitting(true);
    try {
      const params = new URLSearchParams();
      params.append('rejectionReason', rejectionReason);

      await axios.post(
        `${API_BASE_URL}/teller/deposit-requests/${selectedRequest.id}/reject?${params.toString()}`,
        {},
        {
          headers: { Authorization: `Bearer ${session?.token}` }
        }
      );

      toast({ title: 'Success', description: 'Deposit request rejected' });
      setApprovalDialogOpen(false);
      setSelectedRequest(null);
      fetchPendingRequests();
      onApprovalChange?.();
    } catch (err: any) {
      console.error('Rejection error:', err);
      toast({
        title: 'Error',
        description: err.response?.data?.message || err.response?.data?.error || 'Failed to reject request',
        variant: 'destructive'
      });
    } finally {
      setSubmitting(false);
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

  const handleDownloadReceipt = async () => {
    if (!selectedRequest) return;
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/teller/deposit-requests/${selectedRequest.id}/receipt/download`,
        {
          headers: { Authorization: `Bearer ${session?.token}` }
        }
      );
      
      if (!response.ok) {
        const errorText = await response.text();
        try {
          const errorObj = JSON.parse(errorText);
          throw new Error(errorObj.message || `HTTP ${response.status}`);
        } catch {
          throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
      }

      const blob = await response.blob();
      await downloadAndOpenFile(
        blob,
        selectedRequest.receiptFileName || 'receipt.pdf',
        (message) => toast({ title: 'Success', description: message }),
        (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
      );
    } catch (err: any) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to view receipt';
      console.error('Error downloading receipt:', err);
      toast({
        title: 'Error',
        description: errorMsg,
        variant: 'destructive'
      });
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Pending Deposit Requests</DialogTitle>
          </DialogHeader>

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
            </div>
          ) : requests.length === 0 ? (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>No pending deposit requests at this time.</AlertDescription>
            </Alert>
          ) : (
            <div className="space-y-4">
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  {requests.length} pending request{requests.length !== 1 ? 's' : ''} awaiting approval
                </AlertDescription>
              </Alert>

              <div className="space-y-3">
                {requests.map((request) => (
                  <Card
                    key={request.id}
                    className="cursor-pointer hover:shadow-md transition-shadow"
                    onClick={() => handleSelectRequest(request)}
                  >
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <p className="font-semibold">{request.memberName}</p>
                          <p className="text-sm text-muted-foreground">Member: {request.memberNumber}</p>
                          <p className="text-sm text-muted-foreground">Account: {request.accountType}</p>
                          <p className="text-sm font-medium mt-2">
                            Claimed: {formatCurrency(request.claimedAmount)}
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">
                            {formatDate(request.createdAt)}
                          </p>
                        </div>
                        <div className="text-right">
                          <div className="inline-flex items-center gap-2 px-3 py-1 bg-yellow-50 text-yellow-700 rounded-full text-sm font-medium">
                            <AlertCircle className="h-4 w-4" />
                            Pending
                          </div>
                          <p className="text-xs text-muted-foreground mt-2">Receipt: {request.receiptFileName}</p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              <Button
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="w-full"
              >
                Close
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {selectedRequest && (
        <Dialog open={approvalDialogOpen} onOpenChange={setApprovalDialogOpen}>
          <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>
                {isRejecting ? 'Reject Deposit Request' : 'Approve Deposit Request'}
              </DialogTitle>
            </DialogHeader>

            <div className="space-y-6">
              <Card className="bg-blue-50">
                <CardContent className="pt-6 space-y-3">
                  <div>
                    <p className="text-sm text-muted-foreground">Member</p>
                    <p className="font-semibold">{selectedRequest.memberName}</p>
                    <p className="text-sm text-muted-foreground">{selectedRequest.memberNumber}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Account</p>
                    <p className="font-semibold">{selectedRequest.accountType}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Description</p>
                    <p className="text-sm">{selectedRequest.description}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Receipt</p>
                    <Button
                      onClick={handleDownloadReceipt}
                      variant="outline"
                      size="sm"
                      className="mt-1"
                    >
                      <Eye className="h-4 w-4 mr-1" />
                      View
                    </Button>
                    <p className="text-xs text-muted-foreground mt-1">{selectedRequest.receiptFileName}</p>
                  </div>
                  <div className="border-t pt-3">
                    <p className="text-sm text-muted-foreground">Claimed Amount</p>
                    <p className="text-2xl font-bold text-primary">
                      {formatCurrency(selectedRequest.claimedAmount)}
                    </p>
                  </div>
                </CardContent>
              </Card>

              {!isRejecting ? (
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="confirmed">Confirmed Amount (KES)</Label>
                    <Input
                      id="confirmed"
                      type="number"
                      value={confirmedAmount}
                      onChange={(e) => setConfirmedAmount(e.target.value)}
                      min="0"
                      step="100"
                      className="text-lg font-semibold"
                    />
                    <p className="text-xs text-muted-foreground mt-1">
                      Enter the amount you verified from the receipt. Can be different from claimed amount.
                    </p>
                  </div>

                  <div>
                    <Label htmlFor="notes">Approval Notes (Optional)</Label>
                    <Textarea
                      id="notes"
                      value={approvalNotes}
                      onChange={(e) => setApprovalNotes(e.target.value)}
                      placeholder="e.g., Receipt verified, amount matches bank statement"
                      className="min-h-20"
                    />
                  </div>

                  <Alert className="bg-green-50 border-green-200">
                    <CheckCircle className="h-4 w-4 text-green-600" />
                    <AlertDescription className="text-green-800">
                      Approving will credit {formatCurrency(parseFloat(confirmedAmount) || 0)} to the member's account.
                    </AlertDescription>
                  </Alert>

                  <div className="flex gap-3">
                    <Button
                      variant="outline"
                      onClick={() => setIsRejecting(true)}
                      className="flex-1"
                    >
                      Reject Instead
                    </Button>
                    <Button
                      onClick={handleApprove}
                      disabled={submitting || !confirmedAmount}
                      className="flex-1"
                    >
                      {submitting ? 'Approving...' : 'Approve'}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="reason">Rejection Reason</Label>
                    <Textarea
                      id="reason"
                      value={rejectionReason}
                      onChange={(e) => setRejectionReason(e.target.value)}
                      placeholder="e.g., Receipt is unclear, amount doesn't match, duplicate submission"
                      className="min-h-24"
                    />
                  </div>

                  <Alert className="bg-red-50 border-red-200">
                    <XCircle className="h-4 w-4 text-red-600" />
                    <AlertDescription className="text-red-800">
                      Rejecting will notify the member to resubmit with correct information.
                    </AlertDescription>
                  </Alert>

                  <div className="flex gap-3">
                    <Button
                      variant="outline"
                      onClick={() => setIsRejecting(false)}
                      className="flex-1"
                    >
                      Back
                    </Button>
                    <Button
                      onClick={handleReject}
                      disabled={submitting || !rejectionReason.trim()}
                      variant="destructive"
                      className="flex-1"
                    >
                      {submitting ? 'Rejecting...' : 'Confirm Rejection'}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}
