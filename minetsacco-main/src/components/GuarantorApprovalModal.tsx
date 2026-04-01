import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { Textarea } from '@/components/ui/textarea';
import React from 'react';

interface GuarantorApprovalModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  guarantor: any;
  loan: any;
  onSuccess?: () => void;
}

interface GuarantorEligibility {
  eligible: boolean;
  savingsBalance: number;
  sharesBalance: number;
  currentPledges: number;
  availableCapacity: number;
  activeGuarantorships: number;
  loanAmount: number;
  canGuarantee: boolean;
  errors: string[];
  warnings: string[];
}

export default function GuarantorApprovalModal({
  open,
  onOpenChange,
  guarantor,
  loan,
  onSuccess
}: GuarantorApprovalModalProps) {
  const [step, setStep] = useState<'view' | 'reject'>('view');
  const [rejectionReason, setRejectionReason] = useState('');
  const [processing, setProcessing] = useState(false);
  const [eligibility, setEligibility] = useState<GuarantorEligibility | null>(null);
  const [loadingEligibility, setLoadingEligibility] = useState(true);
  const { toast } = useToast();

  // Load eligibility when modal opens
  useEffect(() => {
    if (open) {
      loadEligibility();
    }
  }, [open]);

  const loadEligibility = async () => {
    const token = localStorage.getItem('token');
    const memberId = localStorage.getItem('memberId');
    
    if (!token) {
      toast({
        title: 'Error',
        description: 'Authentication token not found',
        variant: 'destructive'
      });
      return;
    }

    if (!memberId) {
      toast({
        title: 'Error',
        description: 'Member ID not found',
        variant: 'destructive'
      });
      return;
    }

    setLoadingEligibility(true);
    try {
      const response = await api.get(
        `/member/guarantor-eligibility/${memberId}/${loan.amount}`
      );
      setEligibility(response.data);
    } catch (err: any) {
      console.error('Error checking eligibility:', err);
      toast({
        title: 'Error',
        description: 'Failed to check eligibility',
        variant: 'destructive'
      });
    } finally {
      setLoadingEligibility(false);
    }
  };

  const handleApprove = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      toast({
        title: 'Error',
        description: 'Authentication token not found',
        variant: 'destructive'
      });
      return;
    }

    setProcessing(true);
    try {
      await api.post(
        `/member/guarantor-requests/${guarantor.id}/approve`,
        {}
      );

      toast({
        title: 'Success',
        description: 'You have approved this guarantee',
        variant: 'default'
      });

      onSuccess?.();
      onOpenChange(false);
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to approve guarantee',
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

    const token = localStorage.getItem('token');
    if (!token) {
      toast({
        title: 'Error',
        description: 'Authentication token not found',
        variant: 'destructive'
      });
      return;
    }

    setProcessing(true);
    try {
      await api.post(
        `/member/guarantor-requests/${guarantor.id}/reject?reason=${encodeURIComponent(rejectionReason)}`,
        {}
      );

      toast({
        title: 'Success',
        description: 'You have rejected this guarantee',
        variant: 'default'
      });

      onSuccess?.();
      onOpenChange(false);
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to reject guarantee',
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Loan Guarantee Request</DialogTitle>
          <DialogDescription>
            Review the loan details and decide whether to approve or reject the guarantee
          </DialogDescription>
        </DialogHeader>

        {step === 'view' && (
          <div className="space-y-6">
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Member {loan.member?.memberNumber} ({loan.member?.firstName} {loan.member?.lastName}) is requesting you to be a guarantor for their loan application.
              </AlertDescription>
            </Alert>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Loan Details */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Loan Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <p className="text-sm text-muted-foreground">Member Name</p>
                    <p className="font-semibold">{loan.member?.firstName} {loan.member?.lastName}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Member ID</p>
                    <p className="font-semibold">{loan.member?.memberNumber}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Loan Amount</p>
                    <p className="font-semibold text-lg text-blue-600">{formatCurrency(loan.amount)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Loan Product</p>
                    <p className="font-semibold">{loan.loanProduct?.name}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Term</p>
                    <p className="font-semibold">{loan.termMonths} months</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Monthly Repayment</p>
                    <p className="font-semibold">{formatCurrency(loan.monthlyRepayment)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Purpose</p>
                    <p className="font-semibold text-sm">{loan.purpose}</p>
                  </div>
                </CardContent>
              </Card>

              {/* Guarantor Eligibility */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Your Eligibility as Guarantor</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {loadingEligibility ? (
                    <p className="text-center text-muted-foreground py-8">Loading eligibility...</p>
                  ) : eligibility ? (
                    <>
                      <div className={`p-3 rounded-lg mb-4 ${eligibility.canGuarantee ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                        <p className={`font-semibold text-sm ${eligibility.canGuarantee ? 'text-green-800' : 'text-red-800'}`}>
                          {eligibility.canGuarantee ? '✓ You can guarantee this loan' : '✗ You cannot guarantee this loan'}
                        </p>
                      </div>

                      <div>
                        <p className="text-sm text-muted-foreground">Savings Balance</p>
                        <p className="font-semibold">{formatCurrency(eligibility.savingsBalance)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Shares Balance</p>
                        <p className="font-semibold">{formatCurrency(eligibility.sharesBalance)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Current Pledges (Frozen)</p>
                        <p className="font-semibold text-orange-600">{formatCurrency(eligibility.currentPledges)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Available Capacity</p>
                        <p className="font-semibold text-green-600">{formatCurrency(eligibility.availableCapacity)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Active Guarantorships</p>
                        <p className="font-semibold">{eligibility.activeGuarantorships}</p>
                      </div>

                      {eligibility.errors.length > 0 && (
                        <div className="mt-3 bg-red-100 border border-red-300 rounded p-2">
                          <p className="text-xs font-semibold text-red-800 mb-1">Issues:</p>
                          {eligibility.errors.map((error, idx) => (
                            <p key={idx} className="text-xs text-red-700">• {error}</p>
                          ))}
                        </div>
                      )}

                      {eligibility.warnings.length > 0 && (
                        <div className="mt-3 bg-yellow-100 border border-yellow-300 rounded p-2">
                          <p className="text-xs font-semibold text-yellow-800 mb-1">Warnings:</p>
                          {eligibility.warnings.map((warning, idx) => (
                            <p key={idx} className="text-xs text-yellow-700">• {warning}</p>
                          ))}
                        </div>
                      )}
                    </>
                  ) : (
                    <p className="text-center text-red-600 py-8">Failed to load eligibility</p>
                  )}
                </CardContent>
              </Card>
            </div>

            <Alert className="bg-blue-50 border-blue-200">
              <AlertCircle className="h-4 w-4 text-blue-600" />
              <AlertDescription className="text-blue-800">
                As a guarantor, you are jointly and severally liable for this loan. If the member defaults, you may be required to repay the outstanding balance.
              </AlertDescription>
            </Alert>

            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={() => setStep('reject')}
                className="flex-1"
              >
                Reject
              </Button>
              <Button
                onClick={handleApprove}
                disabled={processing || !eligibility?.canGuarantee}
                className="flex-1"
              >
                {processing ? 'Approving...' : 'Approve'}
              </Button>
            </div>
          </div>
        )}

        {step === 'reject' && (
          <div className="space-y-6">
            <Alert className="bg-red-50 border-red-200">
              <XCircle className="h-4 w-4 text-red-600" />
              <AlertDescription className="text-red-800">
                Please provide a reason for rejecting this guarantee request.
              </AlertDescription>
            </Alert>

            <div className="space-y-2">
              <label className="text-sm font-medium">Reason for Rejection</label>
              <Textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Enter your reason for rejecting this guarantee..."
                className="min-h-24"
              />
            </div>

            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  setStep('view');
                  setRejectionReason('');
                }}
                className="flex-1"
              >
                Back
              </Button>
              <Button
                onClick={handleReject}
                disabled={processing || !rejectionReason.trim()}
                variant="destructive"
                className="flex-1"
              >
                {processing ? 'Rejecting...' : 'Confirm Rejection'}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
