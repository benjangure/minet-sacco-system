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

interface GuarantorWithAmount extends GuarantorInfo {
  guaranteeAmount: number;
  isSelfGuarantee: boolean;
}

export default function MemberLoanApplication() {
  const [loanProducts, setLoanProducts] = useState<LoanProduct[]>([]);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [amount, setAmount] = useState('');
  const [duration, setDuration] = useState('');
  const [guarantorInput, setGuarantorInput] = useState('');
  const [guarantorAmount, setGuarantorAmount] = useState('');
  const [guarantors, setGuarantors] = useState<GuarantorWithAmount[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [eligibility, setEligibility] = useState<any>(null);
  const [guarantorLookupLoading, setGuarantorLookupLoading] = useState(false);
  const [guarantorLookupResult, setGuarantorLookupResult] = useState<GuarantorInfo | null>(null);
  const [selfGuaranteeAmount, setSelfGuaranteeAmount] = useState('');
  const [useSelfGuarantee, setUseSelfGuarantee] = useState(false);
  const [hypotheticalEligibility, setHypotheticalEligibility] = useState<any>(null);
  const [calculatingEligibility, setCalculatingEligibility] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    // Member portal stores token in localStorage directly (not in AuthContext session)
    const token = localStorage.getItem('token');
    if (token) {
      fetchLoanProducts();
      fetchEligibility();
    }
  }, []);

  // Calculate hypothetical eligibility when loan amount changes
  // FIXED: Now triggers for ALL loan types, not just self-guarantee loans
  // Members need to see eligibility impact regardless of guarantee structure
  useEffect(() => {
    if (amount && parseFloat(amount) > 0) {
      calculateHypotheticalEligibility();
    } else {
      setHypotheticalEligibility(null);
    }
  }, [amount, selfGuaranteeAmount, useSelfGuarantee]);

  const fetchEligibility = async () => {
    try {
      const response = await api.get('/member/eligibility');
      console.log('Eligibility response:', response.data);
      
      if (response.data && response.data.data) {
        // Transform the response to match the expected format
        const eligibilityData = response.data.data;
        console.log('Eligibility data:', eligibilityData);
        console.log('Member ID from eligibility:', eligibilityData.memberId);
        
        // If memberId is not in eligibility response, fetch from profile
        let memberId = eligibilityData.memberId;
        if (!memberId) {
          try {
            const profileResponse = await api.get('/member/profile');
            memberId = profileResponse.data?.id;
            console.log('Member ID from profile:', memberId);
          } catch (err) {
            console.error('Error fetching profile:', err);
          }
        }
        
        // Use the actual account balance from backend (never changes due to loans)
        const baseSavings = eligibilityData.accountBalance || 0;
        
        // Calculate total frozen (from self-guarantees and guarantor pledges)
        const totalFrozen = eligibilityData.totalFrozen || 0;
        
        setEligibility({
          eligible: eligibilityData.remainingEligibility >= 0,
          displayAmount: eligibilityData.remainingEligibility,
          displayLabel: eligibilityData.remainingEligibility >= 0 ? 'Remaining Eligible' : 'Not Eligible',
          baseSavings: baseSavings,  // Actual account balance from backend
          totalDisbursed: 0,
          trueSavings: eligibilityData.trueSavings,  // Available for calculation (after deducting frozen)
          grossEligibility: eligibilityData.grossEligibility,
          totalOutstanding: eligibilityData.unguaranteedOutstanding,
          netEligibleAmount: eligibilityData.remainingEligibility,
          currentSavings: eligibilityData.trueSavings,
          sharesBalance: 0,
          activeLoans: 0,
          errors: eligibilityData.errors || [],
          warnings: eligibilityData.warnings || [],
          selfGuaranteedAmount: eligibilityData.selfGuaranteedAmount || 0,
          selfGuaranteedInterest: eligibilityData.selfGuaranteedInterest || 0,
          totalFrozen: totalFrozen,
          memberId: memberId
        });
      }
    } catch (err: any) {
      console.error('Error fetching eligibility:', err);
      setEligibility(null);
    }
  };

  const calculateHypotheticalEligibility = async () => {
    if (!amount || parseFloat(amount) <= 0) {
      setHypotheticalEligibility(null);
      return;
    }

    const loanAmount = parseFloat(amount);
    const selfGuarantee = useSelfGuarantee && selfGuaranteeAmount ? parseFloat(selfGuaranteeAmount) : 0;

    // Validate self-guarantee doesn't exceed loan amount
    if (selfGuarantee > loanAmount) {
      setHypotheticalEligibility(null);
      return;
    }

    setCalculatingEligibility(true);
    try {
      const response = await api.post('/member/eligibility/calculate', {
        loanAmount: loanAmount,
        selfGuaranteeAmount: selfGuarantee
      });
      
      if (response.data && response.data.data) {
        setHypotheticalEligibility(response.data.data);
      }
    } catch (err: any) {
      console.error('Error calculating hypothetical eligibility:', err);
      setHypotheticalEligibility(null);
    } finally {
      setCalculatingEligibility(false);
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
    if (!guarantorAmount || parseFloat(guarantorAmount) <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid guarantee amount', variant: 'destructive' });
      return;
    }
    if (guarantors.some(g => g.memberId === guarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This guarantor is already added', variant: 'destructive' });
      return;
    }
    
    const newGuarantor: GuarantorWithAmount = {
      ...guarantorLookupResult,
      guaranteeAmount: parseFloat(guarantorAmount),
      isSelfGuarantee: false
    };
    
    setGuarantors([...guarantors, newGuarantor]);
    setGuarantorInput('');
    setGuarantorAmount('');
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

    // Calculate total guarantee amount
    const totalGuaranteeAmount = guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) + 
                                  (useSelfGuarantee && selfGuaranteeAmount ? parseFloat(selfGuaranteeAmount) : 0);

    // Validate guarantees
    if (totalGuaranteeAmount === 0) {
      toast({ title: 'Error', description: 'Please add guarantors or enable self-guarantee', variant: 'destructive' });
      return;
    }

    if (Math.abs(totalGuaranteeAmount - loanAmount) > 0.01) {
      toast({ 
        title: 'Error', 
        description: `Total guarantee amount (${formatCurrency(totalGuaranteeAmount)}) must equal loan amount (${formatCurrency(loanAmount)})`, 
        variant: 'destructive' 
      });
      return;
    }

    // Check if amount exceeds eligibility
    if (eligibility && loanAmount > eligibility.displayAmount) {
      toast({ 
        title: 'Error', 
        description: `Amount exceeds your ${eligibility.displayLabel.toLowerCase()} of ${formatCurrency(eligibility.displayAmount)}`, 
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
      // Build guarantor requests
      const guarantorRequests = guarantors.map(g => ({
        guarantorId: g.memberId,
        guaranteeAmount: g.guaranteeAmount,
        selfGuarantee: false
      }));

      // Add self-guarantee if applicable
      if (useSelfGuarantee && selfGuaranteeAmount && parseFloat(selfGuaranteeAmount) > 0) {
        // Get member ID from eligibility or from profile
        const memberId = eligibility?.memberId;
        
        if (!memberId) {
          toast({ 
            title: 'Error', 
            description: 'Unable to determine your member ID. Please refresh the page and try again.', 
            variant: 'destructive' 
          });
          setSubmitting(false);
          return;
        }
        
        guarantorRequests.push({
          guarantorId: memberId,
          guaranteeAmount: parseFloat(selfGuaranteeAmount),
          selfGuarantee: true
        });
      }

      await api.post(
        '/member/apply-loan',
        {
          loanProductId: parseInt(selectedProduct),
          amount: loanAmount,
          termMonths: loanDuration,
          guarantors: guarantorRequests
        }
      );
      toast({ title: 'Success', description: 'Loan application submitted successfully' });
      navigate('/member/dashboard');
    } catch (err: any) {
      console.error('Loan application error:', err.response?.data || err.message);
      console.error('Full error:', err);
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
          
          {/* Base Savings - Clear starting point */}
          <div className="bg-white rounded p-4 border border-gray-200">
            <p className="text-sm text-gray-600 mb-1">Your Savings Balance</p>
            <p className="text-3xl font-bold text-blue-600">{formatCurrency(eligibility.baseSavings || 0)}</p>
            <p className="text-xs text-gray-500 mt-2">This is your total savings in the SACCO</p>
          </div>

          {/* Main eligibility display */}
          <div className="bg-white rounded p-4 border border-gray-200">
            <p className="text-sm text-gray-600 mb-1">{eligibility.displayLabel}</p>
            <p className="text-3xl font-bold text-green-600">{formatCurrency(eligibility.displayAmount || 0)}</p>
            <p className="text-xs text-gray-500 mt-2">Maximum you can borrow right now</p>
          </div>

          {/* Calculation breakdown - always show detailed explanation */}
          <div className="bg-white rounded p-4 border border-gray-200 space-y-4">
            <p className="text-sm font-semibold text-gray-700">How we calculated your eligibility:</p>
            
            {/* Show self-guaranteed loans if any */}
            {eligibility.selfGuaranteedAmount > 0 && (
              <div className="bg-orange-50 p-3 rounded border border-orange-200 space-y-2">
                <p className="text-sm font-semibold text-orange-800">Your Self-Guaranteed Loans</p>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Self Guaranteed:</span>
                  <span className="font-medium text-orange-600">{formatCurrency(eligibility.selfGuaranteedAmount)}</span>
                </div>
                {eligibility.selfGuaranteedInterest > 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Interest on Self-Guarantee:</span>
                    <span className="font-medium text-orange-600">{formatCurrency(eligibility.selfGuaranteedInterest)}</span>
                  </div>
                )}
              </div>
            )}
            
            {/* Step 1: Starting point */}
            <div className="space-y-2 text-sm">
              <div className="flex justify-between pb-2 border-b">
                <span className="text-gray-600">Step 1: Your Total Savings</span>
                <span className="font-medium">{formatCurrency(eligibility.baseSavings || 0)}</span>
              </div>
              
              {/* Step 2: Deduct frozen amounts - SMART DISPLAY based on type */}
              <div className="space-y-2">
                <p className="text-xs text-gray-500 font-semibold mt-2">Step 2: Deduct Frozen Amounts</p>
                
                {eligibility.totalFrozen === 0 ? (
                  // No frozen amounts - simple display
                  <div className="bg-gray-50 p-3 rounded border border-gray-200 space-y-1">
                    <p className="text-xs text-gray-600 font-semibold">Total Frozen: {formatCurrency(0)}</p>
                    <p className="text-xs text-gray-500">✓ No frozen amounts - all your savings are available</p>
                  </div>
                ) : (
                  // Has frozen amounts - show smart breakdown
                  <div className="space-y-2">
                    {/* Self-Guarantee Frozen - if any */}
                    {(eligibility.selfGuaranteedAmount || 0) > 0 && (
                      <div className="bg-orange-50 p-3 rounded border border-orange-200 space-y-2">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <p className="text-xs font-semibold text-orange-800">🔒 Self-Guarantee Frozen</p>
                            <p className="text-xs text-orange-700 mt-1">You locked this by guaranteeing your own loans</p>
                          </div>
                          <span className="text-sm font-bold text-orange-600 ml-2">{formatCurrency(eligibility.selfGuaranteedAmount || 0)}</span>
                        </div>
                        
                        {/* Show which loans are self-guaranteed */}
                        {eligibility.selfGuaranteedAmount > 0 && (
                          <div className="bg-white p-2 rounded border border-orange-100 text-xs text-gray-600">
                            <p className="font-semibold mb-1">Your self-guaranteed loans:</p>
                            <p>• Amount guaranteed: {formatCurrency(eligibility.selfGuaranteedAmount)}</p>
                            {eligibility.selfGuaranteedInterest > 0 && (
                              <p>• Interest accrued: {formatCurrency(eligibility.selfGuaranteedInterest)}</p>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                    
                    {/* Guarantor Pledges Frozen - if any */}
                    {((eligibility.totalFrozen || 0) - (eligibility.selfGuaranteedAmount || 0)) > 0 && (
                      <div className="bg-purple-50 p-3 rounded border border-purple-200 space-y-2">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <p className="text-xs font-semibold text-purple-800">🤝 Guarantor Pledges Frozen</p>
                            <p className="text-xs text-purple-700 mt-1">You locked this by guaranteeing other members' loans</p>
                          </div>
                          <span className="text-sm font-bold text-purple-600 ml-2">
                            {formatCurrency((eligibility.totalFrozen || 0) - (eligibility.selfGuaranteedAmount || 0))}
                          </span>
                        </div>
                        
                        <div className="bg-white p-2 rounded border border-purple-100 text-xs text-gray-600">
                          <p className="font-semibold mb-1">Your guarantor commitments:</p>
                          <p>• Total pledged: {formatCurrency((eligibility.totalFrozen || 0) - (eligibility.selfGuaranteedAmount || 0))}</p>
                          <p className="text-purple-600 font-semibold mt-2">💡 Tip: As other members repay their loans, your pledged amounts will be released and your eligibility will increase.</p>
                        </div>
                      </div>
                    )}
                    
                    {/* Total Frozen Summary */}
                    <div className="bg-red-50 p-2 rounded border border-red-200">
                      <div className="flex justify-between items-center">
                        <span className="text-xs font-semibold text-red-800">Total Frozen:</span>
                        <span className="text-sm font-bold text-red-600">{formatCurrency(eligibility.totalFrozen)}</span>
                      </div>
                      <p className="text-xs text-red-600 mt-1">⚠️ These amounts cannot be used for new loans until released</p>
                    </div>
                  </div>
                )}
              </div>
              
              {/* Step 3: Available savings */}
              <div className="flex justify-between pt-2 pb-2 border-b font-semibold bg-blue-50 p-2 rounded">
                <span className="text-gray-700">Step 3: Available Savings (for calculation)</span>
                <span className="text-blue-600">{formatCurrency(eligibility.trueSavings || 0)}</span>
              </div>
              
              {/* Step 4: Multiply by 3 */}
              <div className="space-y-2">
                <p className="text-xs text-gray-500 font-semibold mt-2">Step 4: Calculate Borrowing Power</p>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">{formatCurrency(eligibility.trueSavings || 0)} × 3 (multiplier):</span>
                  <span className="font-medium">{formatCurrency(eligibility.grossEligibility || 0)}</span>
                </div>
              </div>
              
              {/* Step 5: Deduct outstanding loans if any */}
              {eligibility.totalOutstanding > 0 && (
                <div className="space-y-2">
                  <p className="text-xs text-gray-500 font-semibold mt-2">Step 5: Deduct Outstanding Loans</p>
                  <div className="bg-red-50 p-3 rounded border border-red-100 space-y-1">
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Outstanding Balance:</span>
                      <span className="font-medium text-red-600">−{formatCurrency(eligibility.totalOutstanding || 0)}</span>
                    </div>
                    <p className="text-xs text-gray-500">Active loans reduce your available borrowing capacity</p>
                  </div>
                </div>
              )}
              
              {/* Final result */}
              <div className="flex justify-between pt-3 pb-2 border-t-2 border-b-2 font-bold bg-green-50 p-3 rounded">
                <span className="text-green-700">Your Borrowing Capacity:</span>
                <span className="text-green-700">{formatCurrency(eligibility.displayAmount || 0)}</span>
              </div>
            </div>
            
            {/* Summary explanation - SMART based on frozen type */}
            <div className="bg-blue-50 p-3 rounded border border-blue-100 text-xs text-gray-600 space-y-2">
              <p>💡 <strong>How Your Eligibility Works:</strong></p>
              
              {eligibility.totalFrozen === 0 ? (
                <p>Your actual savings are {formatCurrency(eligibility.baseSavings || 0)}. Since you have no frozen amounts, all your savings are available for calculating your borrowing capacity ({formatCurrency(eligibility.baseSavings || 0)} × 3 = {formatCurrency(eligibility.grossEligibility || 0)}).</p>
              ) : (
                <>
                  <p>Your actual savings are {formatCurrency(eligibility.baseSavings || 0)}. However, {formatCurrency(eligibility.totalFrozen || 0)} is frozen (locked):</p>
                  
                  <ul className="list-disc ml-5 space-y-1">
                    {(eligibility.selfGuaranteedAmount || 0) > 0 && (
                      <li>
                        <strong>🔒 Self-Guarantee:</strong> {formatCurrency(eligibility.selfGuaranteedAmount)} locked because you guaranteed your own loans. This reduces your available savings.
                      </li>
                    )}
                    {((eligibility.totalFrozen || 0) - (eligibility.selfGuaranteedAmount || 0)) > 0 && (
                      <li>
                        <strong>🤝 Guarantor Pledges:</strong> {formatCurrency((eligibility.totalFrozen || 0) - (eligibility.selfGuaranteedAmount || 0))} locked because you're guaranteeing other members' loans. As they repay, this amount will be released.
                      </li>
                    )}
                  </ul>
                  
                  <p className="pt-2 border-t">This leaves {formatCurrency(eligibility.trueSavings || 0)} available for calculating your borrowing capacity ({formatCurrency(eligibility.trueSavings || 0)} × 3 = {formatCurrency(eligibility.grossEligibility || 0)}).</p>
                </>
              )}
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
              <Label htmlFor="guarantor">Add Guarantors (Employee ID)</Label>
              <div className="space-y-3">
                <div className="flex gap-2">
                  <Input
                    id="guarantor"
                    type="text"
                    placeholder="Enter guarantor employee ID"
                    value={guarantorInput}
                    onChange={(e) => setGuarantorInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), lookupGuarantorByEmployeeId())}
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
                  <div className="p-3 bg-green-50 border border-green-200 rounded space-y-3">
                    <div>
                      <p className="text-sm font-semibold">{guarantorLookupResult.firstName} {guarantorLookupResult.lastName}</p>
                      <p className="text-xs text-muted-foreground">Employee ID: {guarantorLookupResult.employeeId}</p>
                    </div>
                    <div>
                      <Label htmlFor="guarantorAmount" className="text-xs">Guarantee Amount (KES)</Label>
                      <div className="flex gap-2 mt-1">
                        <Input
                          id="guarantorAmount"
                          type="number"
                          placeholder="Enter amount to guarantee"
                          value={guarantorAmount}
                          onChange={(e) => setGuarantorAmount(e.target.value)}
                          min="0"
                          step="1000"
                        />
                        <Button
                          type="button"
                          variant="default"
                          size="sm"
                          onClick={handleAddGuarantor}
                        >
                          Add
                        </Button>
                      </div>
                    </div>
                  </div>
                )}

                {guarantors.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-sm font-semibold">Added Guarantors ({guarantors.length}):</p>
                    {guarantors.map((guarantor, index) => (
                      <div key={index} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
                        <div>
                          <p className="text-sm font-medium">{guarantor.firstName} {guarantor.lastName}</p>
                          <p className="text-xs text-muted-foreground">Employee ID: {guarantor.employeeId} | Guarantee: {formatCurrency(guarantor.guaranteeAmount)}</p>
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
              </div>
            </div>

            <div className="border-t pt-4">
              <div className="flex items-center gap-3 mb-3">
                <input
                  type="checkbox"
                  id="selfGuarantee"
                  checked={useSelfGuarantee}
                  onChange={(e) => setUseSelfGuarantee(e.target.checked)}
                  className="w-4 h-4"
                />
                <Label htmlFor="selfGuarantee" className="cursor-pointer">
                  I want to self-guarantee part or all of this loan
                </Label>
              </div>

              {useSelfGuarantee && (
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="selfAmount">Self-Guarantee Amount (KES)</Label>
                    <Input
                      id="selfAmount"
                      type="number"
                      placeholder="Enter amount you want to self-guarantee"
                      value={selfGuaranteeAmount}
                      onChange={(e) => setSelfGuaranteeAmount(e.target.value)}
                      min="0"
                      step="1000"
                    />
                    <p className="text-xs text-muted-foreground mt-1">
                      You can self-guarantee the full loan amount or combine with external guarantors.
                    </p>
                  </div>

                  {/* Real-time Eligibility Calculation Display */}
                  {amount && (
                    <div className={`p-4 rounded-lg border ${hypotheticalEligibility ? 'bg-blue-50 border-blue-200' : 'bg-gray-50 border-gray-200'}`}>
                      <p className="text-sm font-semibold mb-3">Your Eligibility After This Loan</p>
                      
                      {calculatingEligibility ? (
                        <p className="text-sm text-gray-600">Calculating...</p>
                      ) : hypotheticalEligibility ? (
                        <div className="space-y-2">
                          <div className="flex justify-between text-sm">
                            <span className="text-gray-600">True Savings:</span>
                            <span className="font-medium">{formatCurrency(hypotheticalEligibility.trueSavings || 0)}</span>
                          </div>
                          
                          <div className="flex justify-between text-sm">
                            <span className="text-gray-600">Frozen (Self-Guarantee):</span>
                            <span className="font-medium text-orange-600">−{formatCurrency(hypotheticalEligibility.totalFrozen || 0)}</span>
                          </div>
                          
                          <div className="flex justify-between text-sm border-t pt-2">
                            <span className="text-gray-600">Available Savings:</span>
                            <span className="font-medium">{formatCurrency(hypotheticalEligibility.availableSavings || 0)}</span>
                          </div>
                          
                          <div className="flex justify-between text-sm">
                            <span className="text-gray-600">Gross Eligibility (3×):</span>
                            <span className="font-medium">{formatCurrency(hypotheticalEligibility.grossEligibility || 0)}</span>
                          </div>
                          
                          {hypotheticalEligibility.unguaranteedOutstanding > 0 && (
                            <div className="flex justify-between text-sm">
                              <span className="text-gray-600">External Outstanding:</span>
                              <span className="font-medium text-red-600">−{formatCurrency(hypotheticalEligibility.unguaranteedOutstanding || 0)}</span>
                            </div>
                          )}
                          
                          <div className="flex justify-between text-sm border-t pt-2 font-semibold text-green-700">
                            <span>Remaining Eligibility:</span>
                            <span>{formatCurrency(hypotheticalEligibility.remainingEligibility || 0)}</span>
                          </div>

                          {hypotheticalEligibility.remainingEligibility < 0 && (
                            <div className="bg-red-100 border border-red-300 rounded p-2 mt-2">
                              <p className="text-xs text-red-700">
                                ⚠️ Self-guarantee amount exceeds your available savings. You must have at least {formatCurrency(parseFloat(selfGuaranteeAmount) || 0)} in savings.
                              </p>
                            </div>
                          )}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-600">Enter a self-guarantee amount to see your eligibility</p>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Guarantee Summary */}
            {(guarantors.length > 0 || useSelfGuarantee) && (
              <div className="bg-purple-50 p-4 rounded-lg space-y-2 border border-purple-200">
                <p className="text-sm font-semibold">Guarantee Summary</p>
                {guarantors.map((g, idx) => (
                  <div key={idx} className="flex justify-between text-sm">
                    <span>{g.firstName} {g.lastName}:</span>
                    <span className="font-medium">{formatCurrency(g.guaranteeAmount)}</span>
                  </div>
                ))}
                {useSelfGuarantee && selfGuaranteeAmount && (
                  <div className="flex justify-between text-sm">
                    <span>Your self-guarantee:</span>
                    <span className="font-medium">{formatCurrency(parseFloat(selfGuaranteeAmount))}</span>
                  </div>
                )}
                <div className="border-t pt-2 flex justify-between text-sm font-semibold">
                  <span>Total Guaranteed:</span>
                  <span className={
                    amount && Math.abs((guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) + (useSelfGuarantee && selfGuaranteeAmount ? parseFloat(selfGuaranteeAmount) : 0)) - parseFloat(amount)) < 0.01
                      ? 'text-green-600'
                      : 'text-red-600'
                  }>
                    {formatCurrency(guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) + (useSelfGuarantee && selfGuaranteeAmount ? parseFloat(selfGuaranteeAmount) : 0))}
                  </span>
                </div>
                {amount && Math.abs((guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) + (useSelfGuarantee && selfGuaranteeAmount ? parseFloat(selfGuaranteeAmount) : 0)) - parseFloat(amount)) > 0.01 && (
                  <p className="text-xs text-red-600 mt-2">
                    ⚠️ Total guarantee must equal loan amount of {formatCurrency(parseFloat(amount))}
                  </p>
                )}
              </div>
            )}

            {amount && duration && selectedProductData && (
              <div className="bg-green-50 p-4 rounded-lg space-y-2">
                <p className="text-sm font-semibold">Loan Summary</p>
                <p className="text-sm">Amount: {formatCurrency(parseFloat(amount))}</p>
                <p className="text-sm">Duration: {duration} months</p>
                <p className="text-sm">Interest Rate: {selectedProductData.interestRate}%</p>
                <p className="text-sm border-t pt-2 font-semibold">
                  Estimated Monthly Payment: {formatCurrency(
                    (() => {
                      // FIXED: Use reducing balance formula (standard for Kenyan SACCOs)
                      // Instead of flat rate: (principal * (1 + rate * term)) / term
                      const principal = parseFloat(amount);
                      const monthlyRate = selectedProductData.interestRate / 100 / 12;
                      const months = parseInt(duration);
                      
                      if (monthlyRate === 0) {
                        // No interest - simple division
                        return principal / months;
                      } else {
                        // Reducing balance formula: P * r * (1+r)^n / ((1+r)^n - 1)
                        const numerator = principal * monthlyRate * Math.pow(1 + monthlyRate, months);
                        const denominator = Math.pow(1 + monthlyRate, months) - 1;
                        return numerator / denominator;
                      }
                    })()
                  )}
                </p>
              </div>
            )}

            <Alert>
              <AlertDescription>
                Once you submit, your selected guarantors will receive notifications. Your loan will proceed to the Loan Officer only after all guarantors approve.
              </AlertDescription>
            </Alert>

            <Button type="submit" className="w-full" disabled={submitting || (guarantors.length === 0 && !useSelfGuarantee)}>
              {submitting ? 'Submitting...' : 'Submit Application'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
