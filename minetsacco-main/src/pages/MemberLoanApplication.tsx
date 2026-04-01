import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/config/api';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ArrowLeft, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { Alert, AlertDescription } from '@/components/ui/alert';

interface LoanProduct {
  id: number;
  name: string;
  maxAmount: number;
  minAmount: number;
  interestRate: number;
  minTermMonths: number;
  maxTermMonths: number;
}

interface GuarantorInfo {
  memberId: number;
  memberNumber: string;
  employeeId: string;
  firstName: string;
  lastName: string;
}

export default function MemberLoanApplication() {
  const [loanProducts, setLoanProducts] = useState<LoanProduct[]>([]);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [amount, setAmount] = useState('');
  const [duration, setDuration] = useState('');
  const [guarantorInput, setGuarantorInput] = useState('');
  const [guarantors, setGuarantors] = useState<GuarantorInfo[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [eligibility, setEligibility] = useState<any>(null);
  const [guarantorLookupLoading, setGuarantorLookupLoading] = useState(false);
  const [guarantorLookupResult, setGuarantorLookupResult] = useState<GuarantorInfo | null>(null);
  const navigate = useNavigate();
  const { toast } = useToast();
  const { session } = useAuth();

  useEffect(() => {
    // Member portal stores token in localStorage directly (not in AuthContext session)
    const token = localStorage.getItem('token');
    if (token) {
      fetchLoanProducts();
      fetchEligibility();
    }
  }, []);

  const fetchEligibility = async () => {
    try {
      const response = await api.get('/member/loan-eligibility');
      setEligibility(response.data);
    } catch (err: any) {
      console.error('Error fetching eligibility:', err);
      setEligibility(null);
    }
  };

  const fetchLoanProducts = async () => {
    try {
      const response = await api.get('/loan-products');
      const products = response.data.data || [];
      setLoanProducts(Array.isArray(products) ? products : []);
    } catch (err: any) {
      console.error('Error fetching loan products:', err);
      setLoanProducts([]);
    }
  };

  const lookupGuarantorByEmployeeId = async () => {
    if (!guarantorInput.trim()) {
      toast({ title: 'Error', description: 'Please enter an employee ID', variant: 'destructive' });
      return;
    }

    setGuarantorLookupLoading(true);
    try {
      const response = await api.get(
        `/member/member-by-employee-id/${guarantorInput.trim()}`
      );
      
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
        description: 'Guarantor not found. Please check the employee ID.', 
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
    if (guarantors.length >= 3) {
      toast({ title: 'Error', description: 'Maximum 3 guarantors allowed', variant: 'destructive' });
      return;
    }
    if (guarantors.some(g => g.memberId === guarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This guarantor is already added', variant: 'destructive' });
      return;
    }
    setGuarantors([...guarantors, guarantorLookupResult]);
    setGuarantorInput('');
    setGuarantorLookupResult(null);
  };

  const handleRemoveGuarantor = (index: number) => {
    setGuarantors(guarantors.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedProduct || !amount || !duration) {
      toast({ title: 'Error', description: 'Please fill all fields', variant: 'destructive' });
      return;
    }

    const loanAmount = parseFloat(amount);
    const loanDuration = parseInt(duration);

    // Validate amount against product limits
    if (selectedProductData) {
      if (loanAmount < selectedProductData.minAmount) {
        toast({ 
          title: 'Error', 
          description: `Loan amount must be at least ${formatCurrency(selectedProductData.minAmount)}`, 
          variant: 'destructive' 
        });
        return;
      }
      if (loanAmount > selectedProductData.maxAmount) {
        toast({ 
          title: 'Error', 
          description: `Loan amount cannot exceed ${formatCurrency(selectedProductData.maxAmount)}`, 
          variant: 'destructive' 
        });
        return;
      }
      if (loanDuration < selectedProductData.minTermMonths) {
        toast({ 
          title: 'Error', 
          description: `Loan term must be at least ${selectedProductData.minTermMonths} months`, 
          variant: 'destructive' 
        });
        return;
      }
      if (loanDuration > selectedProductData.maxTermMonths) {
        toast({ 
          title: 'Error', 
          description: `Loan term cannot exceed ${selectedProductData.maxTermMonths} months`, 
          variant: 'destructive' 
        });
        return;
      }
    }

    if (guarantors.length === 0) {
      toast({ title: 'Error', description: 'Please add at least one guarantor', variant: 'destructive' });
      return;
    }

    // Check if amount exceeds eligibility
    if (eligibility && loanAmount > eligibility.maxEligibleAmount) {
      toast({ 
        title: 'Error', 
        description: `Amount exceeds your maximum eligible amount of ${formatCurrency(eligibility.maxEligibleAmount)}`, 
        variant: 'destructive' 
      });
      return;
    }

    if (!eligibility?.eligible) {
      toast({ 
        title: 'Error', 
        description: 'You are not currently eligible for a loan. Please check the eligibility details above.', 
        variant: 'destructive' 
      });
      return;
    }

    setSubmitting(true);
    try {
      await api.post(
        '/member/apply-loan',
        {
          loanProductId: parseInt(selectedProduct),
          amount: loanAmount,
          termMonths: loanDuration,
          guarantorIds: guarantors.map(g => g.memberId)
        }
      );
      toast({ title: 'Success', description: 'Loan application submitted successfully' });
      navigate('/member/dashboard');
    } catch (err: any) {
      console.error('Loan application error:', err.response?.data || err.message);
      toast({ 
        title: 'Error', 
        description: err.response?.data?.message || err.response?.data?.error || 'Failed to submit application', 
        variant: 'destructive' 
      });
    } finally {
      setSubmitting(false);
    }
  };

  const selectedProductData = loanProducts.find(p => p.id === parseInt(selectedProduct));

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" />
        Back
      </Button>

      {/* Eligibility Card - At the top */}
      {eligibility && (
        <div className={`p-6 rounded-lg space-y-4 ${eligibility.eligible ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-xl">Your Loan Eligibility</h3>
            <span className={`px-4 py-2 rounded-full text-sm font-medium ${eligibility.eligible ? 'bg-green-200 text-green-800' : 'bg-red-200 text-red-800'}`}>
              {eligibility.eligible ? 'Eligible' : 'Not Eligible'}
            </span>
          </div>
          
          <div className="grid grid-cols-2 gap-6">
            <div>
              <p className="text-sm text-gray-600">Max Eligible Amount</p>
              <p className="text-2xl font-bold text-green-600">{formatCurrency(eligibility.maxEligibleAmount || 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Total Balance</p>
              <p className="text-xl font-semibold">{formatCurrency((eligibility.savingsBalance || 0) + (eligibility.sharesBalance || 0))}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Savings</p>
              <p className="text-lg font-semibold">{formatCurrency(eligibility.savingsBalance || 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Shares</p>
              <p className="text-lg font-semibold">{formatCurrency(eligibility.sharesBalance || 0)}</p>
            </div>
          </div>

          {eligibility.errors && eligibility.errors.length > 0 && (
            <div className="bg-red-100 border border-red-300 rounded p-3">
              <p className="text-sm font-semibold text-red-800 mb-1">Issues:</p>
              {eligibility.errors.map((error: string, idx: number) => (
                <p key={idx} className="text-xs text-red-700">• {error}</p>
              ))}
            </div>
          )}

          {eligibility.warnings && eligibility.warnings.length > 0 && (
            <div className="bg-yellow-100 border border-yellow-300 rounded p-3">
              <p className="text-sm font-semibold text-yellow-800 mb-1">Warnings:</p>
              {eligibility.warnings.map((warning: string, idx: number) => (
                <p key={idx} className="text-xs text-yellow-700">• {warning}</p>
              ))}
            </div>
          )}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Apply for a Loan</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <Label htmlFor="product">Loan Product</Label>
              <Select value={selectedProduct} onValueChange={setSelectedProduct}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a loan product" />
                </SelectTrigger>
                <SelectContent>
                  {loanProducts.map((product) => (
                    <SelectItem key={product.id} value={product.id.toString()}>
                      {product.name} - Up to {formatCurrency(product.maxAmount)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedProductData && (
              <div className="bg-blue-50 p-4 rounded-lg space-y-2">
                <p className="text-sm"><span className="font-semibold">Interest Rate:</span> {selectedProductData.interestRate}% per annum</p>
                <p className="text-sm"><span className="font-semibold">Max Amount:</span> {formatCurrency(selectedProductData.maxAmount)}</p>
                <p className="text-sm"><span className="font-semibold">Term Range:</span> {selectedProductData.minTermMonths} - {selectedProductData.maxTermMonths} months</p>
              </div>
            )}

            <div>
              <Label htmlFor="amount">Loan Amount (KES)</Label>
              <Input
                id="amount"
                type="number"
                placeholder="Enter amount"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                min="0"
                step="1000"
              />
            </div>

            <div>
              <Label htmlFor="duration">Loan Duration (months)</Label>
              <Input
                id="duration"
                type="number"
                placeholder="Enter duration"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                min="1"
                max="60"
              />
            </div>

            <div>
              <Label htmlFor="guarantor">Guarantors (Employee ID)</Label>
              <div className="flex gap-2">
                <Input
                  id="guarantor"
                  type="text"
                  placeholder="Enter guarantor employee ID"
                  value={guarantorInput}
                  onChange={(e) => setGuarantorInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), lookupGuarantorByEmployeeId())}
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={lookupGuarantorByEmployeeId}
                  disabled={guarantorLookupLoading || !guarantorInput.trim()}
                >
                  {guarantorLookupLoading ? 'Searching...' : 'Search'}
                </Button>
              </div>

              {guarantorLookupResult && (
                <div className="mt-3 p-3 bg-green-50 border border-green-200 rounded flex items-center justify-between">
                  <div>
                    <p className="text-sm font-semibold">{guarantorLookupResult.firstName} {guarantorLookupResult.lastName}</p>
                    <p className="text-xs text-muted-foreground">Employee ID: {guarantorLookupResult.employeeId}</p>
                  </div>
                  <Button
                    type="button"
                    variant="default"
                    size="sm"
                    onClick={handleAddGuarantor}
                  >
                    Add
                  </Button>
                </div>
              )}

              {guarantors.length > 0 && (
                <div className="mt-3 space-y-2">
                  <p className="text-sm font-semibold">Added Guarantors ({guarantors.length}/3):</p>
                  {guarantors.map((guarantor, index) => (
                    <div key={index} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
                      <div>
                        <p className="text-sm font-medium">{guarantor.firstName} {guarantor.lastName}</p>
                        <p className="text-xs text-muted-foreground">Employee ID: {guarantor.employeeId}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => handleRemoveGuarantor(index)}
                        className="text-red-600 hover:text-red-800"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <p className="text-xs text-muted-foreground mt-2">
                Add up to 3 guarantors. They will receive notifications to approve your loan.
              </p>
            </div>

            {amount && duration && selectedProductData && (
              <div className="bg-green-50 p-4 rounded-lg space-y-2">
                <p className="text-sm font-semibold">Loan Summary</p>
                <p className="text-sm">Amount: {formatCurrency(parseFloat(amount))}</p>
                <p className="text-sm">Duration: {duration} months</p>
                <p className="text-sm">Interest Rate: {selectedProductData.interestRate}%</p>
                <p className="text-sm border-t pt-2 font-semibold">
                  Estimated Monthly Payment: {formatCurrency(
                    (parseFloat(amount) * (1 + (selectedProductData.interestRate / 100) * (parseInt(duration) / 12))) / parseInt(duration)
                  )}
                </p>
              </div>
            )}

            <Alert>
              <AlertDescription>
                Once you submit, your selected guarantors will receive notifications. Your loan will proceed to the Loan Officer only after all guarantors approve.
              </AlertDescription>
            </Alert>

            <Button type="submit" className="w-full" disabled={submitting || guarantors.length === 0}>
              {submitting ? 'Submitting...' : 'Submit Application'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
