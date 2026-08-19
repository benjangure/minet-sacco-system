import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/config/api';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ArrowLeft, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { Alert, AlertDescription } from '@/components/ui/alert';
import MemberLayout from '@/components/MemberLayout';

interface LoanProduct {
  id: number;
  name: string;
  maxAmount: number;
  minAmount: number;
  interestRate: number;
  minTermMonths: number;
  maxTermMonths: number;
  maxTotalBorrowingLimit?: number;
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
  isNextOfKin?: boolean; // Flag for next of kin guarantors (standby status)
}

interface Loan {
  id: number;
  loanNumber: string;
  amount: number;
  outstandingBalance: number;
  status: string;
  productName?: string; // Optional since API might not return it
  hasPendingTopup?: boolean; // Whether this loan has a pending top-up request
  pendingTopupStatus?: string | null; // Status of the pending top-up if any
}

export default function MemberLoanApplication() {
  const [activeTab, setActiveTab] = useState<'apply' | 'topup'>('apply');
  const [loanProducts, setLoanProducts] = useState<LoanProduct[]>([]);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [amount, setAmount] = useState('');
  const [duration, setDuration] = useState('');
  const [guarantorInput, setGuarantorInput] = useState('');
  const [guarantorAmount, setGuarantorAmount] = useState('');
  const [guarantors, setGuarantors] = useState<GuarantorWithAmount[]>([]);
  const [addAsNextOfKin, setAddAsNextOfKin] = useState(false); // NEW: Checkbox for next of kin
  const [nokGuarantorInput, setNokGuarantorInput] = useState(''); // NEW: NOK search input
  const [nokGuarantorLookupResult, setNokGuarantorLookupResult] = useState<GuarantorInfo | null>(null); // NEW: NOK lookup result
  const [nokGuarantorLookupLoading, setNokGuarantorLookupLoading] = useState(false); // NEW: NOK loading state
  const [submitting, setSubmitting] = useState(false);
  const [eligibility, setEligibility] = useState<any>(null);
  const [guarantorLookupLoading, setGuarantorLookupLoading] = useState(false);
  const [guarantorLookupResult, setGuarantorLookupResult] = useState<GuarantorInfo | null>(null);
  const [selfGuaranteeAmount, setSelfGuaranteeAmount] = useState('');
  const [useSelfGuarantee, setUseSelfGuarantee] = useState(false);
  
  const [hypotheticalEligibility, setHypotheticalEligibility] = useState<any>(null);
  const [calculatingEligibility, setCalculatingEligibility] = useState(false);
  const [availableBorrowingCapacity, setAvailableBorrowingCapacity] = useState<number | null>(null);
  const [loadingCapacity, setLoadingCapacity] = useState(false);
  const [memberFirstName, setMemberFirstName] = useState<string>('Member');
  
  // Top-up related states
  const [eligibleLoans, setEligibleLoans] = useState<Loan[]>([]);
  const [selectedLoan, setSelectedLoan] = useState('');
  const [topupAmount, setTopupAmount] = useState('');
  const [topupPurpose, setTopupPurpose] = useState('');
  const [topupGuarantors, setTopupGuarantors] = useState<GuarantorWithAmount[]>([]);
  const [topupGuarantorInput, setTopupGuarantorInput] = useState('');
  const [topupGuarantorAmount, setTopupGuarantorAmount] = useState('');
  const [topupGuarantorLookupResult, setTopupGuarantorLookupResult] = useState<GuarantorInfo | null>(null);
  const [topupGuarantorLookupLoading, setTopupGuarantorLookupLoading] = useState(false);
  const [addAsNextOfKinTopup, setAddAsNextOfKinTopup] = useState(false); // NEW: Checkbox for next of kin in topup
  const [nokTopupGuarantorInput, setNokTopupGuarantorInput] = useState(''); // NEW: NOK topup search input
  const [nokTopupGuarantorLookupResult, setNokTopupGuarantorLookupResult] = useState<GuarantorInfo | null>(null); // NEW: NOK topup lookup result
  const [nokTopupGuarantorLookupLoading, setNokTopupGuarantorLookupLoading] = useState(false); // NEW: NOK topup loading state
  const [loansLoading, setLoansLoading] = useState(false);
  
  // Next of kin guarantor states for top-up
  const [useNextOfKinGuarantor, setUseNextOfKinGuarantor] = useState(false);
  const [nextOfKinName, setNextOfKinName] = useState('');
  const [nextOfKinPhone, setNextOfKinPhone] = useState('');
  const [nextOfKinRelationship, setNextOfKinRelationship] = useState('');
  
  const navigate = useNavigate();
  const { toast } = useToast();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('session');
    navigate('/member/login');
  };

  useEffect(() => {
    // Member portal stores token in localStorage directly (not in AuthContext session)
    const token = localStorage.getItem('token');
    if (token) {
      fetchMemberInfo();
      fetchLoanProducts();
      fetchEligibility();
      fetchEligibleLoansForTopup();
    }
  }, []);

  // Refresh eligible loans when tab changes to topup
  useEffect(() => {
    if (activeTab === 'topup') {
      fetchEligibleLoansForTopup();
    }
  }, [activeTab]);

  const fetchMemberInfo = async () => {
    try {
      const response = await api.get('/member/dashboard');
      if (response.data && (response.data.fullName || response.data.firstName)) {
        setMemberFirstName(response.data.fullName || response.data.firstName);
      }
    } catch (err) {
      console.error('Error fetching member info:', err);
    }
  };

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

  // Calculate available borrowing capacity when product is selected
  const calculateAvailableCapacity = async (productId: string) => {
    const product = loanProducts.find(p => p.id === parseInt(productId));
    
    // Only show capacity if product has a max_total_borrowing_limit set
    if (!product || !product.maxTotalBorrowingLimit || product.maxTotalBorrowingLimit <= 0) {
      setAvailableBorrowingCapacity(null);
      return;
    }

    try {
      setLoadingCapacity(true);
      // Call a new endpoint that returns current outstanding + available capacity
      const response = await api.get(`/loans/product/${productId}/available-capacity`);
      
      if (response.data && response.data.data) {
        const { availableCapacity } = response.data.data;
        setAvailableBorrowingCapacity(availableCapacity);
      }
    } catch (error) {
      console.error('Error calculating available capacity:', error);
      setAvailableBorrowingCapacity(null);
    } finally {
      setLoadingCapacity(false);
    }
  };

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

  const fetchEligibleLoansForTopup = async () => {
    try {
      setLoansLoading(true);
      const response = await api.get('/member/loans');
      const loans = response.data || [];
      
      console.log('All member loans:', loans);
      console.log('Loan statuses:', loans.map((l: Loan) => ({ id: l.id, loanNumber: l.loanNumber, status: l.status, outstanding: l.outstandingBalance })));
      
      // Filter loans that are ACTIVE, DISBURSED, or APPROVED (all active statuses)
      const eligible = loans.filter((loan: Loan) => {
        const validStatuses = ['ACTIVE', 'DISBURSED', 'APPROVED'];
        const hasValidStatus = validStatuses.includes(loan.status);
        
        // Handle null outstandingBalance - treat as eligible if status is valid
        // The actual outstanding will be fetched when they select the loan
        const hasOutstanding = loan.outstandingBalance === null || loan.outstandingBalance === undefined || loan.outstandingBalance > 0;
        
        console.log(`Loan ${loan.loanNumber}: status=${loan.status}, hasValidStatus=${hasValidStatus}, outstanding=${loan.outstandingBalance}, hasOutstanding=${hasOutstanding}`);
        
        return hasValidStatus && hasOutstanding;
      });
      
      // Check each eligible loan for pending top-up requests
      const loansWithPendingStatus = await Promise.all(
        eligible.map(async (loan: Loan) => {
          try {
            const topupResponse = await api.get(`/loans/${loan.id}/topup-requests`);
            const topupRequests = topupResponse.data?.data || topupResponse.data || [];
            const pendingTopups = topupRequests.filter((req: any) => 
              req.status === 'PENDING_GUARANTOR_APPROVAL' || req.status === 'PENDING_REVIEW'
            );
            return {
              ...loan,
              hasPendingTopup: pendingTopups.length > 0,
              pendingTopupStatus: pendingTopups.length > 0 ? pendingTopups[0].status : null
            };
          } catch (err) {
            console.error(`Error fetching top-up requests for loan ${loan.id}:`, err);
            return { ...loan, hasPendingTopup: false, pendingTopupStatus: null };
          }
        })
      );
      
      console.log('Eligible loans for top-up with pending status:', loansWithPendingStatus);
      setEligibleLoans(loansWithPendingStatus);
    } catch (err: any) {
      console.error('Error fetching loans:', err);
      setEligibleLoans([]);
    } finally {
      setLoansLoading(false);
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
      isSelfGuarantee: false,
      isNextOfKin: addAsNextOfKin // Include the next of kin flag
    };
    
    setGuarantors([...guarantors, newGuarantor]);
    setGuarantorInput('');
    setGuarantorAmount('');
    setGuarantorLookupResult(null);
    setAddAsNextOfKin(false); // Reset checkbox
    
    toast({ 
      title: 'Success', 
      description: addAsNextOfKin 
        ? 'Next of kin guarantor added (standby status)' 
        : 'Guarantor added successfully'
    });
  };

  const handleRemoveGuarantor = (index: number) => {
    setGuarantors(guarantors.filter((_, i) => i !== index));
  };

  // Next of Kin guarantor lookup and add handlers
  const lookupNokGuarantorByEmployeeId = async () => {
    if (!nokGuarantorInput.trim()) {
      toast({ title: 'Error', description: 'Please enter an employee ID for next of kin', variant: 'destructive' });
      return;
    }

    setNokGuarantorLookupLoading(true);
    try {
      const response = await api.get(
        `/member/member-by-employee-id/${nokGuarantorInput.trim()}`
      );
      
      const guarantorInfo: GuarantorInfo = {
        memberId: response.data.memberId,
        memberNumber: response.data.memberNumber,
        employeeId: response.data.employeeId,
        firstName: response.data.firstName,
        lastName: response.data.lastName
      };
      
      setNokGuarantorLookupResult(guarantorInfo);
      toast({ title: 'Success', description: `Found NOK: ${guarantorInfo.firstName} ${guarantorInfo.lastName}` });
    } catch (err: any) {
      setNokGuarantorLookupResult(null);
      toast({ 
        title: 'Error', 
        description: 'Next of kin not found. Please check the employee ID.', 
        variant: 'destructive' 
      });
    } finally {
      setNokGuarantorLookupLoading(false);
    }
  };

  const handleAddNokGuarantor = () => {
    if (!nokGuarantorLookupResult) {
      toast({ title: 'Error', description: 'Please search for a next of kin guarantor first', variant: 'destructive' });
      return;
    }
    if (!guarantorAmount || parseFloat(guarantorAmount) <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid guarantee amount first', variant: 'destructive' });
      return;
    }
    if (guarantors.some(g => g.memberId === nokGuarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This person is already added as a guarantor', variant: 'destructive' });
      return;
    }
    
    const nokGuarantor: GuarantorWithAmount = {
      ...nokGuarantorLookupResult,
      guaranteeAmount: parseFloat(guarantorAmount), // Same amount as primary guarantor
      isSelfGuarantee: false,
      isNextOfKin: true // Mark as next of kin
    };
    
    setGuarantors([...guarantors, nokGuarantor]);
    setNokGuarantorInput('');
    setNokGuarantorLookupResult(null);
    setAddAsNextOfKin(false); // Uncheck the checkbox
    
    toast({ 
      title: 'Success', 
      description: 'Next of kin guarantor added (standby status)'
    });
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

      // Prepare request body
      const requestBody: any = {
        loanProductId: parseInt(selectedProduct),
        amount: loanAmount,
        termMonths: loanDuration,
        guarantors: guarantorRequests
      };

      await api.post('/member/apply-loan', requestBody);
      
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

  // Top-up handlers
  const lookupTopupGuarantorByEmployeeId = async () => {
    if (!topupGuarantorInput.trim()) {
      toast({ title: 'Error', description: 'Please enter an employee ID', variant: 'destructive' });
      return;
    }

    setTopupGuarantorLookupLoading(true);
    try {
      const response = await api.get(
        `/member/member-by-employee-id/${topupGuarantorInput.trim()}`
      );
      
      const guarantorInfo: GuarantorInfo = {
        memberId: response.data.memberId,
        memberNumber: response.data.memberNumber,
        employeeId: response.data.employeeId,
        firstName: response.data.firstName,
        lastName: response.data.lastName
      };
      
      setTopupGuarantorLookupResult(guarantorInfo);
      toast({ title: 'Success', description: `Found: ${guarantorInfo.firstName} ${guarantorInfo.lastName}` });
    } catch (err: any) {
      setTopupGuarantorLookupResult(null);
      toast({ 
        title: 'Error', 
        description: 'Guarantor not found. Please check the employee ID.', 
        variant: 'destructive' 
      });
    } finally {
      setTopupGuarantorLookupLoading(false);
    }
  };

  const handleAddTopupGuarantor = () => {
    if (!topupGuarantorLookupResult) {
      toast({ title: 'Error', description: 'Please search for a guarantor first', variant: 'destructive' });
      return;
    }
    if (!topupGuarantorAmount || parseFloat(topupGuarantorAmount) <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid guarantee amount', variant: 'destructive' });
      return;
    }
    if (topupGuarantors.some(g => g.memberId === topupGuarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This guarantor is already added', variant: 'destructive' });
      return;
    }
    
    const newGuarantor: GuarantorWithAmount = {
      ...topupGuarantorLookupResult,
      guaranteeAmount: parseFloat(topupGuarantorAmount),
      isSelfGuarantee: false,
      isNextOfKin: addAsNextOfKinTopup // Include the next of kin flag
    };
    
    setTopupGuarantors([...topupGuarantors, newGuarantor]);
    setTopupGuarantorInput('');
    setTopupGuarantorAmount('');
    setTopupGuarantorLookupResult(null);
    setAddAsNextOfKinTopup(false); // Reset checkbox
    
    toast({ 
      title: 'Success', 
      description: addAsNextOfKinTopup 
        ? 'Next of kin guarantor added (standby status)' 
        : 'Guarantor added successfully'
    });
  };

  const handleRemoveTopupGuarantor = (index: number) => {
    setTopupGuarantors(topupGuarantors.filter((_, i) => i !== index));
  };

  // Next of Kin guarantor lookup and add handlers for top-up
  const lookupNokTopupGuarantorByEmployeeId = async () => {
    if (!nokTopupGuarantorInput.trim()) {
      toast({ title: 'Error', description: 'Please enter an employee ID for next of kin', variant: 'destructive' });
      return;
    }

    setNokTopupGuarantorLookupLoading(true);
    try {
      const response = await api.get(
        `/member/member-by-employee-id/${nokTopupGuarantorInput.trim()}`
      );
      
      const guarantorInfo: GuarantorInfo = {
        memberId: response.data.memberId,
        memberNumber: response.data.memberNumber,
        employeeId: response.data.employeeId,
        firstName: response.data.firstName,
        lastName: response.data.lastName
      };
      
      setNokTopupGuarantorLookupResult(guarantorInfo);
      toast({ title: 'Success', description: `Found NOK: ${guarantorInfo.firstName} ${guarantorInfo.lastName}` });
    } catch (err: any) {
      setNokTopupGuarantorLookupResult(null);
      toast({ 
        title: 'Error', 
        description: 'Next of kin not found. Please check the employee ID.', 
        variant: 'destructive' 
      });
    } finally {
      setNokTopupGuarantorLookupLoading(false);
    }
  };

  const handleAddNokTopupGuarantor = () => {
    if (!nokTopupGuarantorLookupResult) {
      toast({ title: 'Error', description: 'Please search for a next of kin guarantor first', variant: 'destructive' });
      return;
    }
    if (!topupGuarantorAmount || parseFloat(topupGuarantorAmount) <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid guarantee amount first', variant: 'destructive' });
      return;
    }
    if (topupGuarantors.some(g => g.memberId === nokTopupGuarantorLookupResult.memberId)) {
      toast({ title: 'Error', description: 'This person is already added as a guarantor', variant: 'destructive' });
      return;
    }
    
    const nokGuarantor: GuarantorWithAmount = {
      ...nokTopupGuarantorLookupResult,
      guaranteeAmount: parseFloat(topupGuarantorAmount), // Same amount as primary guarantor
      isSelfGuarantee: false,
      isNextOfKin: true // Mark as next of kin
    };
    
    setTopupGuarantors([...topupGuarantors, nokGuarantor]);
    setNokTopupGuarantorInput('');
    setNokTopupGuarantorLookupResult(null);
    setAddAsNextOfKinTopup(false); // Uncheck the checkbox
    
    toast({ 
      title: 'Success', 
      description: 'Next of kin guarantor added (standby status)'
    });
  };

  const handleTopupSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedLoan || !topupAmount || !topupPurpose.trim()) {
      toast({ title: 'Error', description: 'Please fill all required fields', variant: 'destructive' });
      return;
    }

    const requestedAmount = parseFloat(topupAmount);
    if (requestedAmount <= 0) {
      toast({ title: 'Error', description: 'Please enter a valid amount', variant: 'destructive' });
      return;
    }

    // Guarantors are now optional - allow submission without guarantors
    // Only validate total if guarantors are provided
    if (topupGuarantors.length > 0) {
      // Calculate total guarantee amount
      const totalGuaranteeAmount = topupGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0);

      if (Math.abs(totalGuaranteeAmount - requestedAmount) > 0.01) {
        toast({ 
          title: 'Error', 
          description: `Total guarantee amount (${formatCurrency(totalGuaranteeAmount)}) must equal top-up amount (${formatCurrency(requestedAmount)})`, 
          variant: 'destructive' 
        });
        return;
      }
    }

    setSubmitting(true);
    try {
      const guarantorRequests = topupGuarantors.map(g => ({
        memberNumber: g.memberNumber,  // Backend expects memberNumber (String), not memberId
        guaranteeAmount: g.guaranteeAmount
      }));

      // Prepare request body
      const requestBody: any = {
        requestedAmount,
        purpose: topupPurpose,
        guarantors: guarantorRequests
      };

      await api.post(`/loans/${selectedLoan}/request-topup`, requestBody);

      toast({ 
        title: 'Success', 
        description: 'Top-up request submitted successfully. Your guarantors will be notified.' 
      });
      
      // Reset form
      setSelectedLoan('');
      setTopupAmount('');
      setTopupPurpose('');
      setTopupGuarantors([]);
      setTopupGuarantorInput('');
      setTopupGuarantorAmount('');
      setTopupGuarantorLookupResult(null);
      
      navigate('/member/dashboard');
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

  const selectedProductData = loanProducts.find(p => p.id === parseInt(selectedProduct));

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  return (
    <MemberLayout memberName={memberFirstName} onLogout={handleLogout}>
      <div className="space-y-6 max-w-7xl mx-auto px-4 lg:px-0">
        <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>

        {/* Page Heading */}
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            {activeTab === 'apply' ? 'Apply for a Loan' : 'Request Loan Top-Up'}
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            {activeTab === 'apply' 
              ? 'Complete the form below to apply for a new loan' 
              : 'Top up your existing loan with additional funds'}
          </p>
        </div>

      {/* Eligibility Card - Always visible, ABOVE tab navigation */}
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

        {/* Tab Navigation - Using system Tabs component */}
        <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as 'apply' | 'topup')} className="w-full">
          <TabsList className="grid w-full grid-cols-2 bg-primary/10">
            <TabsTrigger value="apply">Apply for Loan</TabsTrigger>
            <TabsTrigger value="topup">Request Top-Up</TabsTrigger>
          </TabsList>

      {/* Apply for Loan Tab Content */}
      <TabsContent value="apply" className="mt-6">
      <Card>
        <CardHeader>
          <CardTitle>Loan Application Form</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <Label htmlFor="product">Loan Product</Label>
              <Select value={selectedProduct} onValueChange={(value) => {
                setSelectedProduct(value);
                calculateAvailableCapacity(value);
              }}>
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
                
                {selectedProductData.maxTotalBorrowingLimit && (
                  <div className="border-t pt-2 mt-2">
                    {loadingCapacity ? (
                      <p className="text-sm text-gray-600">Loading available capacity...</p>
                    ) : availableBorrowingCapacity !== null ? (
                      <div className="space-y-1">
                        <p className="text-sm"><span className="font-semibold">Maximum Borrowing Limit:</span> {formatCurrency(selectedProductData.maxTotalBorrowingLimit)}</p>
                        <p className={`text-sm ${availableBorrowingCapacity > 0 ? 'text-green-700' : 'text-red-700'}`}>
                          <span className="font-semibold">Available to Borrow:</span> {formatCurrency(availableBorrowingCapacity)}
                        </p>
                      </div>
                    ) : null}
                  </div>
                )}
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
                      <div className="space-y-2 mt-1">
                        <Input
                          id="guarantorAmount"
                          type="number"
                          placeholder="Enter amount to guarantee"
                          value={guarantorAmount}
                          onChange={(e) => setGuarantorAmount(e.target.value)}
                          min="0"
                          step="1000"
                        />
                        <div className="flex items-center space-x-2">
                          <input
                            type="checkbox"
                            id="addAsNextOfKin"
                            checked={addAsNextOfKin}
                            onChange={(e) => setAddAsNextOfKin(e.target.checked)}
                            className="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                          />
                          <Label htmlFor="addAsNextOfKin" className="text-xs cursor-pointer">
                            Add Next of Kin guarantor (standby - activates when this guarantor exits)
                          </Label>
                        </div>
                        <Button
                          type="button"
                          variant="default"
                          size="sm"
                          onClick={handleAddGuarantor}
                          className="w-full"
                        >
                          Add Guarantor
                        </Button>
                      </div>
                    </div>

                    {/* Next of Kin Search - appears when checkbox is ticked */}
                    {addAsNextOfKin && (
                      <div className="mt-3 p-3 bg-purple-50 border border-purple-200 rounded">
                        <Label className="text-xs font-semibold text-purple-800">Search for Next of Kin (Standby Guarantor)</Label>
                        <div className="flex gap-2 mt-2">
                          <Input
                            type="text"
                            placeholder="Enter NOK employee ID"
                            value={nokGuarantorInput}
                            onChange={(e) => setNokGuarantorInput(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), lookupNokGuarantorByEmployeeId())}
                            className="text-sm"
                          />
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={lookupNokGuarantorByEmployeeId}
                            disabled={nokGuarantorLookupLoading || !nokGuarantorInput.trim()}
                          >
                            {nokGuarantorLookupLoading ? 'Searching...' : 'Search'}
                          </Button>
                        </div>

                        {nokGuarantorLookupResult && (
                          <div className="mt-2 p-2 bg-white border border-purple-300 rounded">
                            <p className="text-xs font-semibold">{nokGuarantorLookupResult.firstName} {nokGuarantorLookupResult.lastName}</p>
                            <p className="text-xs text-muted-foreground">Employee ID: {nokGuarantorLookupResult.employeeId}</p>
                            <p className="text-xs text-purple-700 mt-1">
                              Will cover: {formatCurrency(parseFloat(guarantorAmount) || 0)} (same as primary guarantor)
                            </p>
                            <Button
                              type="button"
                              variant="default"
                              size="sm"
                              onClick={handleAddNokGuarantor}
                              className="w-full mt-2 bg-purple-600 hover:bg-purple-700"
                            >
                              Add Next of Kin
                            </Button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {guarantors.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-sm font-semibold">Added Guarantors ({guarantors.length}):</p>
                    {guarantors.map((guarantor, index) => (
                      <div key={index} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
                        <div>
                          <p className="text-sm font-medium">
                            {guarantor.firstName} {guarantor.lastName}
                            {guarantor.isNextOfKin && (
                              <span className="ml-2 px-2 py-0.5 text-xs font-semibold rounded bg-purple-100 text-purple-800">
                                Next of Kin (Standby)
                              </span>
                            )}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            Employee ID: {guarantor.employeeId} | Guarantee: {formatCurrency(guarantor.guaranteeAmount)}
                          </p>
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

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Submit Application'}
            </Button>
          </form>
        </CardContent>
      </Card>
      </TabsContent>

      {/* Request Top-Up Tab Content */}
      <TabsContent value="topup" className="mt-6">
          {loansLoading ? (
            <Card>
              <CardContent className="py-8">
                <p className="text-center text-muted-foreground">Loading eligible loans...</p>
              </CardContent>
            </Card>
          ) : eligibleLoans.length === 0 ? (
            <Card>
              <CardContent className="py-8">
                <div className="text-center space-y-3">
                  <p className="text-lg font-semibold text-gray-700">No Eligible Loans</p>
                  <p className="text-sm text-muted-foreground">
                    You don't have any active or disbursed loans that can be topped up.
                  </p>
                  <Button onClick={() => setActiveTab('apply')} className="mt-4">
                    Apply for a New Loan
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>Top-Up Request Form</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleTopupSubmit} className="space-y-6">
                  <div>
                    <Label htmlFor="loanSelect">Select Loan</Label>
                    <Select value={selectedLoan} onValueChange={setSelectedLoan}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a loan to top up" />
                      </SelectTrigger>
                      <SelectContent>
                        {eligibleLoans.map((loan) => (
                          <SelectItem 
                            key={loan.id} 
                            value={loan.id.toString()}
                            disabled={loan.hasPendingTopup}
                          >
                            Loan #{loan.loanNumber} {loan.productName ? `- ${loan.productName}` : ''} - Outstanding: {loan.outstandingBalance !== null && loan.outstandingBalance !== undefined ? formatCurrency(loan.outstandingBalance) : 'Pending calculation'}
                            {loan.hasPendingTopup && (
                              <span className="text-yellow-600 font-medium ml-2">
                                (Pending {loan.pendingTopupStatus === 'PENDING_GUARANTOR_APPROVAL' ? 'Guarantor Approval' : 'Review'})
                              </span>
                            )}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {eligibleLoans.some(l => l.hasPendingTopup) && (
                      <p className="text-xs text-yellow-600 mt-1">
                        ⚠️ Loans with pending top-up requests are disabled. Wait for approval or cancellation before requesting another top-up.
                      </p>
                    )}
                  </div>

                  {selectedLoan && (
                    <div className="bg-blue-50 p-4 rounded-lg">
                      <p className="text-sm">
                        <span className="font-semibold">Current Outstanding:</span>{' '}
                        {eligibleLoans.find(l => l.id.toString() === selectedLoan)?.outstandingBalance !== null && 
                         eligibleLoans.find(l => l.id.toString() === selectedLoan)?.outstandingBalance !== undefined
                          ? formatCurrency(eligibleLoans.find(l => l.id.toString() === selectedLoan)?.outstandingBalance || 0)
                          : 'Will be calculated upon submission'}
                      </p>
                    </div>
                  )}

                  <div>
                    <Label htmlFor="topupAmount">Top-Up Amount (KES)</Label>
                    <Input
                      id="topupAmount"
                      type="number"
                      placeholder="Enter amount"
                      value={topupAmount}
                      onChange={(e) => setTopupAmount(e.target.value)}
                      min="1000"
                      step="1000"
                      required
                    />
                    {selectedLoan && topupAmount && (
                      <p className="text-xs text-muted-foreground mt-1">
                        New outstanding will be: {formatCurrency(
                          (eligibleLoans.find(l => l.id.toString() === selectedLoan)?.outstandingBalance || 0) + 
                          (parseFloat(topupAmount) || 0)
                        )}
                      </p>
                    )}
                  </div>

                  <div>
                    <Label htmlFor="topupPurpose">Purpose</Label>
                    <Textarea
                      id="topupPurpose"
                      placeholder="Explain why you need this top-up"
                      value={topupPurpose}
                      onChange={(e) => setTopupPurpose(e.target.value)}
                      required
                      rows={3}
                    />
                  </div>

                  <div>
                    <Label htmlFor="topupGuarantor">Add Guarantors (Employee ID)</Label>
                    <div className="space-y-3">
                      <div className="flex gap-2">
                        <Input
                          id="topupGuarantor"
                          type="text"
                          placeholder="Enter guarantor employee ID"
                          value={topupGuarantorInput}
                          onChange={(e) => setTopupGuarantorInput(e.target.value)}
                          onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), lookupTopupGuarantorByEmployeeId())}
                        />
                        <Button
                          type="button"
                          variant="outline"
                          onClick={lookupTopupGuarantorByEmployeeId}
                          disabled={topupGuarantorLookupLoading || !topupGuarantorInput.trim()}
                        >
                          {topupGuarantorLookupLoading ? 'Searching...' : 'Search'}
                        </Button>
                      </div>

                      {topupGuarantorLookupResult && (
                        <div className="p-3 bg-green-50 border border-green-200 rounded space-y-3">
                          <div>
                            <p className="text-sm font-semibold">
                              {topupGuarantorLookupResult.firstName} {topupGuarantorLookupResult.lastName}
                            </p>
                            <p className="text-xs text-muted-foreground">Employee ID: {topupGuarantorLookupResult.employeeId}</p>
                          </div>
                          <div>
                            <Label htmlFor="topupGuarantorAmount" className="text-xs">Guarantee Amount (KES)</Label>
                            <div className="space-y-2 mt-1">
                              <Input
                                id="topupGuarantorAmount"
                                type="number"
                                placeholder="Enter amount to guarantee"
                                value={topupGuarantorAmount}
                                onChange={(e) => setTopupGuarantorAmount(e.target.value)}
                                min="0"
                                step="1000"
                              />
                              <div className="flex items-center space-x-2">
                                <input
                                  type="checkbox"
                                  id="addAsNextOfKinTopup"
                                  checked={addAsNextOfKinTopup}
                                  onChange={(e) => setAddAsNextOfKinTopup(e.target.checked)}
                                  className="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                                />
                                <Label htmlFor="addAsNextOfKinTopup" className="text-xs cursor-pointer">
                                  Add Next of Kin guarantor (standby - activates when this guarantor exits)
                                </Label>
                              </div>
                              <Button
                                type="button"
                                variant="default"
                                size="sm"
                                onClick={handleAddTopupGuarantor}
                                className="w-full"
                              >
                                Add Guarantor
                              </Button>
                            </div>

                            {/* Next of Kin Search for Top-Up - appears when checkbox is ticked */}
                            {addAsNextOfKinTopup && (
                              <div className="mt-3 p-3 bg-purple-50 border border-purple-200 rounded">
                                <Label className="text-xs font-semibold text-purple-800">Search for Next of Kin (Standby Guarantor)</Label>
                                <div className="flex gap-2 mt-2">
                                  <Input
                                    type="text"
                                    placeholder="Enter NOK employee ID"
                                    value={nokTopupGuarantorInput}
                                    onChange={(e) => setNokTopupGuarantorInput(e.target.value)}
                                    onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), lookupNokTopupGuarantorByEmployeeId())}
                                    className="text-sm"
                                  />
                                  <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    onClick={lookupNokTopupGuarantorByEmployeeId}
                                    disabled={nokTopupGuarantorLookupLoading || !nokTopupGuarantorInput.trim()}
                                  >
                                    {nokTopupGuarantorLookupLoading ? 'Searching...' : 'Search'}
                                  </Button>
                                </div>

                                {nokTopupGuarantorLookupResult && (
                                  <div className="mt-2 p-2 bg-white border border-purple-300 rounded">
                                    <p className="text-xs font-semibold">{nokTopupGuarantorLookupResult.firstName} {nokTopupGuarantorLookupResult.lastName}</p>
                                    <p className="text-xs text-muted-foreground">Employee ID: {nokTopupGuarantorLookupResult.employeeId}</p>
                                    <p className="text-xs text-purple-700 mt-1">
                                      Will cover: {formatCurrency(parseFloat(topupGuarantorAmount) || 0)} (same as primary guarantor)
                                    </p>
                                    <Button
                                      type="button"
                                      variant="default"
                                      size="sm"
                                      onClick={handleAddNokTopupGuarantor}
                                      className="w-full mt-2 bg-purple-600 hover:bg-purple-700"
                                    >
                                      Add Next of Kin
                                    </Button>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        </div>
                      )}

                      {topupGuarantors.length > 0 && (
                        <div className="space-y-2">
                          <p className="text-sm font-semibold">Added Guarantors ({topupGuarantors.length}):</p>
                          {topupGuarantors.map((guarantor, index) => (
                            <div key={index} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
                              <div>
                                <p className="text-sm font-medium">
                                  {guarantor.firstName} {guarantor.lastName}
                                  {guarantor.isNextOfKin && (
                                    <span className="ml-2 px-2 py-0.5 text-xs font-semibold rounded bg-purple-100 text-purple-800">
                                      Next of Kin (Standby)
                                    </span>
                                  )}
                                </p>
                                <p className="text-xs text-muted-foreground">
                                  Employee ID: {guarantor.employeeId} | Guarantee: {formatCurrency(guarantor.guaranteeAmount)}
                                </p>
                              </div>
                              <button
                                type="button"
                                onClick={() => handleRemoveTopupGuarantor(index)}
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

                  {/* Guarantee Summary */}
                  {topupGuarantors.length > 0 && topupAmount && (
                    <div className="bg-purple-50 p-4 rounded-lg space-y-2 border border-purple-200">
                      <p className="text-sm font-semibold">Guarantee Summary</p>
                      {topupGuarantors.map((g, idx) => (
                        <div key={idx} className="flex justify-between text-sm">
                          <span>{g.firstName} {g.lastName}:</span>
                          <span className="font-medium">{formatCurrency(g.guaranteeAmount)}</span>
                        </div>
                      ))}
                      <div className="border-t pt-2 flex justify-between text-sm font-semibold">
                        <span>Total Guaranteed:</span>
                        <span className={
                          topupAmount && Math.abs(topupGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) - parseFloat(topupAmount)) < 0.01
                            ? 'text-green-600'
                            : 'text-red-600'
                        }>
                          {formatCurrency(topupGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0))}
                        </span>
                      </div>
                      {topupAmount && Math.abs(topupGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0) - parseFloat(topupAmount)) > 0.01 && (
                        <p className="text-xs text-red-600 mt-2">
                          ⚠️ Total guarantee must equal top-up amount of {formatCurrency(parseFloat(topupAmount))}
                        </p>
                      )}
                    </div>
                  )}

                  <Alert>
                    <AlertDescription>
                      Once you submit, your selected guarantors will receive notifications. Your top-up will proceed to the Loan Officer only after all guarantors approve.
                    </AlertDescription>
                  </Alert>

                  <Button 
                    type="submit" 
                    className="w-full" 
                    disabled={submitting || topupGuarantors.length === 0 || !selectedLoan}
                  >
                    {submitting ? 'Submitting...' : 'Submit Top-Up Request'}
                  </Button>
                </form>
              </CardContent>
            </Card>
          )}
      </TabsContent>
      </Tabs>
      </div>
    </MemberLayout>
  );
}
