import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, Upload } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';

interface Loan {
  id: number;
  loanProduct: { name: string };
  amount: number;
  outstandingBalance: number;
  status: string;
}

interface LoanRepaymentFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRepaymentSuccess?: () => void;
}

export default function LoanRepaymentForm({
  open,
  onOpenChange,
  onRepaymentSuccess
}: LoanRepaymentFormProps) {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoanId, setSelectedLoanId] = useState('');
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('SAVINGS_DEDUCTION');
  const [description, setDescription] = useState('');
  const [proofFile, setProofFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (open) {
      fetchLoans();
    }
  }, [open]);

  const fetchLoans = async () => {
    setLoading(true);
    try {
      const response = await api.get('/member/loans');
      // Filter only active loans
      const activeLoans = (response.data || []).filter(
        (loan: Loan) => loan.status === 'DISBURSED' && loan.outstandingBalance > 0
      );
      setLoans(activeLoans);
    } catch (err: any) {
      console.error('Error fetching loans:', err);
      toast({
        title: 'Error',
        description: 'Failed to load loans',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedLoanId || !amount) {
      toast({
        title: 'Error',
        description: 'Please fill in all required fields',
        variant: 'destructive'
      });
      return;
    }

    // Validate file for bank transfer
    if (paymentMethod === 'BANK_TRANSFER' && !proofFile) {
      toast({
        title: 'Error',
        description: 'Please upload proof of payment for bank transfer',
        variant: 'destructive'
      });
      return;
    }

    const selectedLoan = loans.find(l => l.id === parseInt(selectedLoanId));
    if (!selectedLoan) {
      toast({
        title: 'Error',
        description: 'Invalid loan selected',
        variant: 'destructive'
      });
      return;
    }

    const repaymentAmount = parseFloat(amount);
    if (repaymentAmount <= 0) {
      toast({
        title: 'Error',
        description: 'Amount must be greater than zero',
        variant: 'destructive'
      });
      return;
    }

    if (repaymentAmount > selectedLoan.outstandingBalance) {
      toast({
        title: 'Error',
        description: `Amount cannot exceed outstanding balance of KES ${selectedLoan.outstandingBalance.toLocaleString()}`,
        variant: 'destructive'
      });
      return;
    }

    setProcessing(true);
    try {
      if (paymentMethod === 'BANK_TRANSFER') {
        // For bank transfer, create a repayment request with file
        const formData = new FormData();
        formData.append('loanId', selectedLoanId);
        formData.append('amount', repaymentAmount.toString());
        formData.append('paymentMethod', paymentMethod);
        formData.append('description', description || 'Loan repayment');
        formData.append('proofFile', proofFile!);

        await api.post('/member/request-loan-repayment', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });

        toast({
          title: 'Success',
          description: 'Repayment request submitted. Teller will verify and approve.'
        });
      } else {
        // For savings deduction, process immediately
        await api.post(
          '/member/repay-loan',
          {
            loanId: parseInt(selectedLoanId),
            amount: repaymentAmount,
            paymentMethod,
            description: description || 'Loan repayment'
          }
        );

        toast({
          title: 'Success',
          description: 'Loan repayment processed successfully'
        });
      }

      // Reset form
      setSelectedLoanId('');
      setAmount('');
      setPaymentMethod('SAVINGS_DEDUCTION');
      setDescription('');
      setProofFile(null);
      onOpenChange(false);
      onRepaymentSuccess?.();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to process repayment',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  const selectedLoan = loans.find(l => l.id === parseInt(selectedLoanId));
  const maxAmount = selectedLoan?.outstandingBalance || 0;

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
          <DialogTitle>Make Loan Repayment</DialogTitle>
          <DialogDescription>
            Select a loan and make a repayment
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
          </div>
        ) : loans.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-muted-foreground">No active loans to repay</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Loan Selection */}
            <div className="space-y-2">
              <Label htmlFor="loan">Select Loan</Label>
              <Select value={selectedLoanId} onValueChange={setSelectedLoanId}>
                <SelectTrigger>
                  <SelectValue placeholder="Choose a loan" />
                </SelectTrigger>
                <SelectContent>
                  {loans.map((loan) => (
                    <SelectItem key={loan.id} value={loan.id.toString()}>
                      {loan.loanProduct.name} - Outstanding: {formatCurrency(loan.outstandingBalance)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Loan Details */}
            {selectedLoan && (
              <Card className="bg-muted/50">
                <CardContent className="pt-6">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <p className="text-sm text-muted-foreground">Original Amount</p>
                      <p className="font-semibold">{formatCurrency(selectedLoan.amount)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">Outstanding Balance</p>
                      <p className="font-semibold text-lg text-primary">
                        {formatCurrency(selectedLoan.outstandingBalance)}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Amount */}
            <div className="space-y-2">
              <Label htmlFor="amount">Repayment Amount (KES)</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0"
                max={maxAmount}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Enter amount"
                disabled={!selectedLoan}
              />
              {selectedLoan && (
                <p className="text-xs text-muted-foreground">
                  Maximum: {formatCurrency(maxAmount)}
                </p>
              )}
            </div>

            {/* Payment Method */}
            <div className="space-y-2">
              <Label htmlFor="method">Payment Method</Label>
              <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SAVINGS_DEDUCTION">Savings Deduction</SelectItem>
                  <SelectItem value="BANK_TRANSFER">Bank Transfer</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                {paymentMethod === 'SAVINGS_DEDUCTION' 
                  ? 'Amount will be deducted from your savings account immediately' 
                  : 'Upload proof of payment - teller will verify and approve'}
              </p>
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">Description (Optional)</Label>
              <Input
                id="description"
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g., Monthly repayment"
              />
            </div>

            {/* File Upload for Bank Transfer */}
            {paymentMethod === 'BANK_TRANSFER' && (
              <div className="space-y-2">
                <Label htmlFor="proof">Upload Proof of Payment *</Label>
                <div className="border-2 border-dashed rounded-lg p-4 text-center cursor-pointer hover:bg-muted/50 transition"
                  onClick={() => document.getElementById('file-input')?.click()}>
                  <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
                  <p className="text-sm font-medium">
                    {proofFile ? proofFile.name : 'Click to upload or drag and drop'}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    PDF, JPG, PNG (Max 5MB)
                  </p>
                  <input
                    id="file-input"
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        if (file.size > 5 * 1024 * 1024) {
                          toast({
                            title: 'Error',
                            description: 'File size must be less than 5MB',
                            variant: 'destructive'
                          });
                        } else {
                          setProofFile(file);
                        }
                      }
                    }}
                    className="hidden"
                  />
                </div>
              </div>
            )}

            {/* Info Alert */}
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                {paymentMethod === 'SAVINGS_DEDUCTION' 
                  ? 'Your repayment will be processed immediately from your savings account.'
                  : 'Your repayment request will be submitted for teller verification. Once approved, the outstanding balance will be updated.'}
              </AlertDescription>
            </Alert>

            {/* Buttons */}
            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={processing}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={processing || !selectedLoan}
                className="flex-1"
              >
                {processing ? 'Processing...' : 'Make Repayment'}
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
