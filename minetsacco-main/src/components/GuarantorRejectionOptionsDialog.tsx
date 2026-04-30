import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import api from '@/config/api';
import { AlertCircle, CheckCircle, XCircle } from 'lucide-react';

interface Guarantor {
  id: number;
  firstName: string;
  lastName: string;
  guaranteeAmount: number;
}

interface GuarantorRejectionOptionsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  loanId: number;
  loanAmount: number;
  rejectedGuarantor: Guarantor | null;
  remainingGuarantors: Guarantor[];
  onSuccess: () => void;
}

type OptionType = 'replace' | 'reduce' | 'withdraw' | null;

export default function GuarantorRejectionOptionsDialog({
  open,
  onOpenChange,
  loanId,
  loanAmount,
  rejectedGuarantor,
  remainingGuarantors,
  onSuccess,
}: GuarantorRejectionOptionsDialogProps) {
  const [selectedOption, setSelectedOption] = useState<OptionType>(null);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  // Replace Guarantor state
  const [newGuarantorId, setNewGuarantorId] = useState('');
  const [newGuaranteeAmount, setNewGuaranteeAmount] = useState(rejectedGuarantor?.guaranteeAmount.toString() || '');

  // Reduce Amount state
  const [newAmount, setNewAmount] = useState('');
  const [reduceReason, setReduceReason] = useState('');

  // Withdraw state
  const [withdrawReason, setWithdrawReason] = useState('');

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
    }).format(amount);
  };

  const handleReplaceGuarantor = async () => {
    if (!newGuarantorId || !newGuaranteeAmount) {
      toast({
        title: 'Error',
        description: 'Please enter guarantor ID and guarantee amount',
        variant: 'destructive',
      });
      return;
    }

    setLoading(true);
    try {
      await api.post(`/loans/${loanId}/replace-guarantor`, null, {
        params: {
          oldGuarantorId: rejectedGuarantor?.id,
          newGuarantorMemberId: parseInt(newGuarantorId),
          newGuaranteeAmount: parseFloat(newGuaranteeAmount),
        },
      });

      toast({
        title: 'Success',
        description: 'Guarantor replaced successfully. New guarantor will receive a notification.',
      });

      onOpenChange(false);
      onSuccess();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to replace guarantor',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleReduceAmount = async () => {
    if (!newAmount || parseFloat(newAmount) <= 0) {
      toast({
        title: 'Error',
        description: 'Please enter a valid new amount',
        variant: 'destructive',
      });
      return;
    }

    if (parseFloat(newAmount) >= loanAmount) {
      toast({
        title: 'Error',
        description: 'New amount must be less than current loan amount',
        variant: 'destructive',
      });
      return;
    }

    setLoading(true);
    try {
      await api.post(`/loans/${loanId}/reduce-amount`, null, {
        params: {
          newAmount: parseFloat(newAmount),
          reason: reduceReason || 'Guarantor rejected',
        },
      });

      toast({
        title: 'Success',
        description: 'Loan amount reduced. Your application will be re-reviewed by the Credit Committee.',
      });

      onOpenChange(false);
      onSuccess();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to reduce loan amount',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleWithdraw = async () => {
    setLoading(true);
    try {
      await api.post(`/loans/${loanId}/withdraw`, null, {
        params: {
          reason: withdrawReason || 'Member requested withdrawal',
        },
      });

      toast({
        title: 'Success',
        description: 'Loan application withdrawn. You can reapply anytime.',
      });

      onOpenChange(false);
      onSuccess();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to withdraw application',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const totalRemainingGuarantee = remainingGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertCircle className="h-5 w-5 text-red-600" />
            Guarantor Rejected - Choose Your Option
          </DialogTitle>
          <DialogDescription>
            {rejectedGuarantor?.firstName} {rejectedGuarantor?.lastName} has rejected your loan application.
            You have 3 options to proceed.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Loan Summary */}
          <Card className="p-4 bg-blue-50 border-blue-200">
            <p className="text-sm font-semibold mb-2">Loan Details</p>
            <div className="space-y-1 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Loan Amount:</span>
                <span className="font-medium">{formatCurrency(loanAmount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Rejected Guarantor:</span>
                <span className="font-medium text-red-600">
                  {rejectedGuarantor?.firstName} {rejectedGuarantor?.lastName}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Rejected Amount:</span>
                <span className="font-medium text-red-600">
                  {formatCurrency(rejectedGuarantor?.guaranteeAmount || 0)}
                </span>
              </div>
              <div className="flex justify-between border-t pt-1">
                <span className="text-gray-600">Remaining Guarantees:</span>
                <span className="font-medium text-green-600">
                  {formatCurrency(totalRemainingGuarantee)}
                </span>
              </div>
            </div>
          </Card>

          {/* Option Selection */}
          {!selectedOption ? (
            <div className="space-y-3">
              {/* Option 1: Replace Guarantor */}
              <Card
                className="p-4 cursor-pointer hover:bg-blue-50 border-2 hover:border-blue-300 transition-all"
                onClick={() => setSelectedOption('replace')}
              >
                <div className="flex items-start gap-3">
                  <CheckCircle className="h-5 w-5 text-blue-600 mt-1 flex-shrink-0" />
                  <div className="flex-1">
                    <h3 className="font-semibold text-blue-900">1. Replace Guarantor</h3>
                    <p className="text-sm text-gray-600 mt-1">
                      Find a new guarantor to replace {rejectedGuarantor?.firstName}. The new guarantor will need to approve.
                    </p>
                    <p className="text-xs text-gray-500 mt-2">
                      ✓ Loan amount stays the same
                      <br />✓ New guarantor must pledge {formatCurrency(rejectedGuarantor?.guaranteeAmount || 0)}
                    </p>
                  </div>
                </div>
              </Card>

              {/* Option 2: Reduce Amount */}
              <Card
                className="p-4 cursor-pointer hover:bg-amber-50 border-2 hover:border-amber-300 transition-all"
                onClick={() => setSelectedOption('reduce')}
              >
                <div className="flex items-start gap-3">
                  <AlertCircle className="h-5 w-5 text-amber-600 mt-1 flex-shrink-0" />
                  <div className="flex-1">
                    <h3 className="font-semibold text-amber-900">2. Reduce Loan Amount</h3>
                    <p className="text-sm text-gray-600 mt-1">
                      Reduce the loan amount to match your remaining guarantees. Your application will be re-reviewed.
                    </p>
                    <p className="text-xs text-gray-500 mt-2">
                      ✓ Can reduce to {formatCurrency(totalRemainingGuarantee)} or less
                      <br />✓ Remaining guarantors stay the same
                      <br />⚠️ Requires Credit Committee re-approval
                    </p>
                  </div>
                </div>
              </Card>

              {/* Option 3: Withdraw */}
              <Card
                className="p-4 cursor-pointer hover:bg-red-50 border-2 hover:border-red-300 transition-all"
                onClick={() => setSelectedOption('withdraw')}
              >
                <div className="flex items-start gap-3">
                  <XCircle className="h-5 w-5 text-red-600 mt-1 flex-shrink-0" />
                  <div className="flex-1">
                    <h3 className="font-semibold text-red-900">3. Withdraw Application</h3>
                    <p className="text-sm text-gray-600 mt-1">
                      Cancel this application. You can reapply anytime with different guarantors.
                    </p>
                    <p className="text-xs text-gray-500 mt-2">
                      ✓ No further action needed
                      <br />✓ Can reapply later
                      <br />⚠️ This action cannot be undone
                    </p>
                  </div>
                </div>
              </Card>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Replace Guarantor Form */}
              {selectedOption === 'replace' && (
                <div className="space-y-4">
                  <Alert>
                    <AlertDescription>
                      Enter the member ID of your new guarantor. They must be able to pledge{' '}
                      {formatCurrency(rejectedGuarantor?.guaranteeAmount || 0)}.
                    </AlertDescription>
                  </Alert>

                  <div>
                    <Label htmlFor="newGuarantorId">New Guarantor Member ID</Label>
                    <Input
                      id="newGuarantorId"
                      type="number"
                      placeholder="Enter member ID"
                      value={newGuarantorId}
                      onChange={(e) => setNewGuarantorId(e.target.value)}
                    />
                  </div>

                  <div>
                    <Label htmlFor="newGuaranteeAmount">Guarantee Amount (KES)</Label>
                    <Input
                      id="newGuaranteeAmount"
                      type="number"
                      placeholder="Enter guarantee amount"
                      value={newGuaranteeAmount}
                      onChange={(e) => setNewGuaranteeAmount(e.target.value)}
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Must be {formatCurrency(rejectedGuarantor?.guaranteeAmount || 0)} to match the rejected amount
                    </p>
                  </div>
                </div>
              )}

              {/* Reduce Amount Form */}
              {selectedOption === 'reduce' && (
                <div className="space-y-4">
                  <Alert>
                    <AlertDescription>
                      Your remaining guarantees total {formatCurrency(totalRemainingGuarantee)}. You can reduce the loan
                      amount to this or less.
                    </AlertDescription>
                  </Alert>

                  <div>
                    <Label htmlFor="newAmount">New Loan Amount (KES)</Label>
                    <Input
                      id="newAmount"
                      type="number"
                      placeholder="Enter new amount"
                      value={newAmount}
                      onChange={(e) => setNewAmount(e.target.value)}
                      max={totalRemainingGuarantee}
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Current: {formatCurrency(loanAmount)} → Maximum: {formatCurrency(totalRemainingGuarantee)}
                    </p>
                  </div>

                  <div>
                    <Label htmlFor="reduceReason">Reason for Reduction (Optional)</Label>
                    <Input
                      id="reduceReason"
                      type="text"
                      placeholder="e.g., Guarantor rejected, reducing to match remaining guarantees"
                      value={reduceReason}
                      onChange={(e) => setReduceReason(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Withdraw Form */}
              {selectedOption === 'withdraw' && (
                <div className="space-y-4">
                  <Alert variant="destructive">
                    <AlertDescription>
                      ⚠️ This will cancel your loan application. You can reapply anytime, but this action cannot be
                      undone.
                    </AlertDescription>
                  </Alert>

                  <div>
                    <Label htmlFor="withdrawReason">Reason for Withdrawal (Optional)</Label>
                    <Input
                      id="withdrawReason"
                      type="text"
                      placeholder="e.g., Guarantor rejected, will reapply later"
                      value={withdrawReason}
                      onChange={(e) => setWithdrawReason(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex gap-2 pt-4 border-t">
                <Button
                  variant="outline"
                  onClick={() => setSelectedOption(null)}
                  disabled={loading}
                  className="flex-1"
                >
                  Back
                </Button>

                <Button
                  onClick={() => {
                    if (selectedOption === 'replace') handleReplaceGuarantor();
                    else if (selectedOption === 'reduce') handleReduceAmount();
                    else if (selectedOption === 'withdraw') handleWithdraw();
                  }}
                  disabled={loading}
                  className="flex-1"
                  variant={selectedOption === 'withdraw' ? 'destructive' : 'default'}
                >
                  {loading ? 'Processing...' : 'Confirm'}
                </Button>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
