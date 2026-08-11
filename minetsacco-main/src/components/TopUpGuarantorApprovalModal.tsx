import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import api from '@/config/api';

interface TopUpGuarantorApprovalModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The top-up request object (contains member, loan, requestedAmount, purpose, etc.) */
  topUpRequest: any;
  /** The amount this guarantor is committing */
  guaranteeAmount: number;
  /** The ID of the top-up request — used in the approve/reject API call */
  topUpRequestId: number;
  onSuccess: () => void;
}

export default function TopUpGuarantorApprovalModal({
  open,
  onOpenChange,
  topUpRequest,
  guaranteeAmount,
  topUpRequestId,
  onSuccess
}: TopUpGuarantorApprovalModalProps) {
  const [processing, setProcessing] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [showRejectForm, setShowRejectForm] = useState(false);
  const { toast } = useToast();

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount || 0);
  };

  const handleApprove = async () => {
    if (!topUpRequestId) {
      toast({
        title: 'Error',
        description: 'Invalid top-up request',
        variant: 'destructive'
      });
      return;
    }

    try {
      setProcessing(true);
      await api.post(`/topup-requests/${topUpRequestId}/guarantor/approve`);

      toast({
        title: 'Success',
        description: 'Top-up guarantee approved successfully'
      });

      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      console.error('Error approving top-up guarantee:', err);
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to approve top-up guarantee',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!rejectionReason.trim()) {
      toast({
        title: 'Error',
        description: 'Please provide a reason for rejection',
        variant: 'destructive'
      });
      return;
    }

    if (!topUpRequestId) {
      toast({
        title: 'Error',
        description: 'Invalid top-up request',
        variant: 'destructive'
      });
      return;
    }

    try {
      setProcessing(true);
      await api.post(`/topup-requests/${topUpRequestId}/guarantor/reject`, {
        reason: rejectionReason
      });

      toast({
        title: 'Success',
        description: 'Top-up guarantee rejected'
      });

      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      console.error('Error rejecting top-up guarantee:', err);
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to reject top-up guarantee',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  // Extract display data from the topUpRequest object.
  // The backend returns borrower info under topUpRequest.requestingMember.
  // Fall back to topUpRequest.member for backwards compatibility.
  const borrower = topUpRequest?.requestingMember || topUpRequest?.member;
  const borrowerName = borrower
    ? `${borrower.firstName || ''} ${borrower.lastName || ''}`.trim()
    : 'N/A';
  const memberNumber = borrower?.memberNumber || 'N/A';
  const loanNumber = topUpRequest?.loanNumber || topUpRequest?.loan?.loanNumber || 'N/A';
  const currentOutstanding = topUpRequest?.currentOutstanding ?? topUpRequest?.loan?.outstandingBalance ?? 0;
  const topUpAmount = topUpRequest?.requestedAmount ?? 0;
  const newOutstanding = currentOutstanding + topUpAmount;
  const purpose = topUpRequest?.purpose || 'No purpose provided';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Top-Up Guarantee Request</DialogTitle>
          <DialogDescription>
            Review the top-up request details and decide whether to approve or reject
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Borrower Information */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h3 className="font-semibold mb-3">Borrower Information</h3>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-muted-foreground">Name</p>
                <p className="font-medium">{borrowerName}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Member Number</p>
                <p className="font-medium">{memberNumber}</p>
              </div>
            </div>
          </div>

          {/* Loan & Top-Up Details */}
          <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
            <h3 className="font-semibold mb-3">Loan & Top-Up Details</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Loan Number</span>
                <span className="font-medium">#{loanNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Current Outstanding</span>
                <span className="font-medium">{formatCurrency(currentOutstanding)}</span>
              </div>
              <div className="flex justify-between border-t pt-2">
                <span className="text-muted-foreground">Top-Up Amount Requested</span>
                <span className="font-medium text-purple-700">{formatCurrency(topUpAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">New Outstanding (After Top-Up)</span>
                <span className="font-semibold text-lg">
                  {formatCurrency(newOutstanding)}
                </span>
              </div>
            </div>
          </div>

          {/* Purpose */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
            <h3 className="font-semibold mb-2">Purpose of Top-Up</h3>
            <p className="text-sm text-muted-foreground">{purpose}</p>
          </div>

          {/* Your Guarantee */}
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <h3 className="font-semibold mb-3">Your Guarantee Commitment</h3>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Amount You're Guaranteeing</span>
                <span className="font-semibold text-lg text-green-700">{formatCurrency(guaranteeAmount)}</span>
              </div>
              <Alert className="bg-yellow-50 border-yellow-200">
                <AlertCircle className="h-4 w-4 text-yellow-700" />
                <AlertDescription className="text-yellow-800 text-xs">
                  By approving, this amount will be frozen from your savings until the top-up is fully repaid
                </AlertDescription>
              </Alert>
            </div>
          </div>

          {/* Rejection Form */}
          {showRejectForm && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <Label htmlFor="rejectionReason">Reason for Rejection</Label>
              <Textarea
                id="rejectionReason"
                placeholder="Explain why you're rejecting this guarantee request..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={4}
                className="mt-2"
              />
            </div>
          )}
        </div>

        <DialogFooter className="flex gap-2 sm:gap-2">
          {!showRejectForm ? (
            <>
              <Button
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={processing}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={() => setShowRejectForm(true)}
                disabled={processing}
                className="gap-2"
              >
                <XCircle className="h-4 w-4" />
                Reject
              </Button>
              <Button
                onClick={handleApprove}
                disabled={processing}
                className="gap-2"
              >
                <CheckCircle className="h-4 w-4" />
                {processing ? 'Approving...' : 'Approve Guarantee'}
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline"
                onClick={() => {
                  setShowRejectForm(false);
                  setRejectionReason('');
                }}
                disabled={processing}
              >
                Back
              </Button>
              <Button
                variant="destructive"
                onClick={handleReject}
                disabled={processing || !rejectionReason.trim()}
              >
                {processing ? 'Rejecting...' : 'Confirm Rejection'}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
