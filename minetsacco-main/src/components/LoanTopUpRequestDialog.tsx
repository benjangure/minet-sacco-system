import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { X, Plus, Search } from 'lucide-react';
import api from '@/config/api';

interface LoanTopUpRequestDialogProps {
  isOpen: boolean;
  onClose: () => void;
  loanId: number;
  loanNumber: string;
  currentOutstanding: number;
  onSuccess: () => void;
}

interface GuarantorInfo {
  memberId: number;
  memberNumber: string;
  employeeId: string;
  firstName: string;
  lastName: string;
}

interface GuarantorWithAmount extends GuarantorInfo {
  guaranteeAmount: number;
}

export default function LoanTopUpRequestDialog({
  isOpen,
  onClose,
  loanId,
  loanNumber,
  currentOutstanding,
  onSuccess
}: LoanTopUpRequestDialogProps) {
  const [amount, setAmount] = useState('');
  const [purpose, setPurpose] = useState('');
  const [guarantorInput, setGuarantorInput] = useState('');
  const [guarantorAmount, setGuarantorAmount] = useState('');
  const [guarantors, setGuarantors] = useState<GuarantorWithAmount[]>([]);
  const [guarantorLookupLoading, setGuarantorLookupLoading] = useState(false);
  const [guarantorLookupResult, setGuarantorLookupResult] = useState<GuarantorInfo | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(value);
  };

  const handleSearchGuarantor = async () => {
    if (!guarantorInput.trim()) {
      toast({ title: 'Error', description: 'Please enter an employee ID', variant: 'destructive' });
      return;
    }

    try {
      setGuarantorLookupLoading(true);
      const response = await api.get(`/member/member-by-employee-id/${guarantorInput.trim()}`);
      
      const guarantorInfo: GuarantorInfo = {
        memberId: response.data.memberId,
        memberNumber: response.data.memberNumber,
        employeeId: response.data.employeeId,
        firstName: response.data.firstName,
        lastName: response.data.lastName
      };
      
      setGuarantorLookupResult(guarantorInfo);
      toast({ title: 'Success', description: `Found: ${guarantorInfo.firstName} ${guarantorInfo.lastName}` });
    } catch (err: any) {
      setGuarantorLookupResult(null);
      toast({ 
        title: 'Error', 
        description: err.response?.data?.message || 'Guarantor not found. Please check the employee ID.', 
        variant: 'destructive' 
      });
    } finally {
      setGuarantorLookupLoading(false);
    }
  };

  const handleAddGuarantor = () => {
    if (!guarantorLookupResult) {
      toast({ title: 'Error', description: 'Please search for a guarantor first', variant: 'destructive' });
      return;
    }

    const guaranteeAmt = parseFloat(guarantorAmount);
    if (!guaranteeAmt || guaranteeAmt <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid guarantee amount', variant: 'destructive' });
      return;
    }

    // Check if guarantor already exists
    if (guarantors.some(g => g.memberId === guarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This guarantor is already added', variant: 'destructive' });
      return;
    }

    const newGuarantor: GuarantorWithAmount = {
      ...guarantorLookupResult,
      guaranteeAmount: guaranteeAmt
    };

    setGuarantors([...guarantors, newGuarantor]);
    setGuarantorLookupResult(null);
    setGuarantorInput('');
    setGuarantorAmount('');
    toast({ title: 'Success', description: 'Guarantor added successfully' });
  };

  const handleRemoveGuarantor = (memberId: number) => {
    setGuarantors(guarantors.filter(g => g.memberId !== memberId));
  };

  const getTotalGuaranteeAmount = () => {
    return guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const requestedAmount = parseFloat(amount);
    if (!requestedAmount || requestedAmount <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid amount', variant: 'destructive' });
      return;
    }

    if (!purpose.trim()) {
      toast({ title: 'Error', description: 'Please enter the purpose', variant: 'destructive' });
      return;
    }

    // Guarantors are now optional - allow submission without guarantors
    // Only validate total if guarantors are provided
    if (guarantors.length > 0) {
      const totalGuarantee = getTotalGuaranteeAmount();
      if (totalGuarantee !== requestedAmount) {
        toast({ 
          title: 'Error', 
          description: `Total guarantee (${formatCurrency(totalGuarantee)}) must equal the requested amount (${formatCurrency(requestedAmount)})`, 
          variant: 'destructive' 
        });
        return;
      }
    }

    try {
      setSubmitting(true);
      const guarantorRequests = guarantors.map(g => ({
        memberId: g.memberId,
        guaranteeAmount: g.guaranteeAmount
      }));

      await api.post(`/loans/${loanId}/request-topup`, {
        requestedAmount,
        purpose,
        guarantors: guarantorRequests
      });

      toast({ 
        title: 'Success', 
        description: 'Top-up request submitted successfully. Your guarantors will be notified.' 
      });
      
      onSuccess();
      handleClose();
    } catch (err: any) {
      console.error('Top-up request error:', err);
      toast({ 
        title: 'Error', 
        description: err.response?.data?.message || 'Failed to submit top-up request', 
        variant: 'destructive' 
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    setAmount('');
    setPurpose('');
    setGuarantorInput('');
    setGuarantorAmount('');
    setGuarantors([]);
    setGuarantorLookupResult(null);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Request Loan Top-Up</DialogTitle>
          <DialogDescription>
            Request additional funds for Loan #{loanNumber}. Current outstanding: {formatCurrency(currentOutstanding)}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <Label htmlFor="amount">Top-Up Amount (KES)</Label>
            <Input
              id="amount"
              type="number"
              placeholder="Enter amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min="1000"
              step="1000"
              required
            />
            <p className="text-xs text-muted-foreground mt-1">
              New outstanding will be: {formatCurrency(currentOutstanding + (parseFloat(amount) || 0))}
            </p>
          </div>

          <div>
            <Label htmlFor="purpose">Purpose</Label>
            <Textarea
              id="purpose"
              placeholder="Explain why you need this top-up"
              value={purpose}
              onChange={(e) => setPurpose(e.target.value)}
              required
              rows={3}
            />
          </div>

          <div className="space-y-4">
            <Label>Add Guarantors (Employee ID)</Label>
            
            <div className="space-y-3">
              <div className="flex gap-2">
                <Input
                  placeholder="Enter employee ID"
                  value={guarantorInput}
                  onChange={(e) => setGuarantorInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleSearchGuarantor();
                    }
                  }}
                />
                <Button
                  type="button"
                  onClick={handleSearchGuarantor}
                  disabled={guarantorLookupLoading}
                >
                  <Search className="h-4 w-4" />
                </Button>
              </div>

              {guarantorLookupResult && (
                <div className="p-3 bg-green-50 border border-green-200 rounded space-y-3">
                  <div>
                    <p className="text-sm font-semibold">{guarantorLookupResult.firstName} {guarantorLookupResult.lastName}</p>
                    <p className="text-xs text-muted-foreground">Employee ID: {guarantorLookupResult.employeeId}</p>
                  </div>
                  <div className="flex gap-2">
                    <Input
                      type="number"
                      placeholder="Guarantee amount"
                      value={guarantorAmount}
                      onChange={(e) => setGuarantorAmount(e.target.value)}
                      min="0"
                      step="1000"
                    />
                    <Button type="button" onClick={handleAddGuarantor} size="sm">
                      <Plus className="h-4 w-4 mr-1" />
                      Add
                    </Button>
                  </div>
                </div>
              )}

              {guarantors.length > 0 && (
                <div className="space-y-2">
                  <p className="text-sm font-semibold">Selected Guarantors:</p>
                  {guarantors.map((guarantor) => (
                    <div key={guarantor.memberId} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
                      <div>
                        <p className="text-sm font-medium">{guarantor.firstName} {guarantor.lastName}</p>
                        <p className="text-xs text-muted-foreground">
                          Employee ID: {guarantor.employeeId} | Guarantee: {formatCurrency(guarantor.guaranteeAmount)}
                        </p>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => handleRemoveGuarantor(guarantor.memberId)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h4 className="font-semibold mb-2">Guarantee Summary</h4>
            <div className="space-y-1 text-sm">
              <div className="flex justify-between">
                <span>Top-Up Amount:</span>
                <span className="font-medium">{formatCurrency(parseFloat(amount) || 0)}</span>
              </div>
              <div className="flex justify-between">
                <span>Total Guaranteed:</span>
                <span className={`font-medium ${getTotalGuaranteeAmount() === parseFloat(amount) ? 'text-green-600' : 'text-red-600'}`}>
                  {formatCurrency(getTotalGuaranteeAmount())}
                </span>
              </div>
              {getTotalGuaranteeAmount() !== parseFloat(amount) && parseFloat(amount) > 0 && (
                <p className="text-xs text-red-600 mt-2">
                  ⚠️ Total guarantee must equal the top-up amount
                </p>
              )}
            </div>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <p className="text-sm text-yellow-800">
              <strong>Note:</strong> Once submitted, your selected guarantors will receive notifications. 
              Your top-up will proceed to the Loan Officer only after all guarantors approve.
            </p>
          </div>

          <div className="flex gap-3 justify-end">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button 
              type="submit" 
              disabled={submitting || (guarantors.length > 0 && getTotalGuaranteeAmount() !== parseFloat(amount))}
            >
              {submitting ? 'Submitting...' : 'Submit Top-Up Request'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
