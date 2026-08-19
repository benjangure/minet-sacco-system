import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useRefresh } from "@/contexts/RefreshContext";
import { useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Plus, Search, Eye, CheckCircle, XCircle, DollarSign, AlertCircle, Users } from "lucide-react";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import GuarantorDetailsModal from "@/components/GuarantorDetailsModal";
import LoanTopUpReviewSection from "@/components/LoanTopUpReviewSection";
import { useLoansSubscription, useTopUpsSubscription } from "@/hooks/useWebSocket";

import { API_BASE_URL } from "@/config/api";

const loanStatusColors: Record<string, string> = {
  PENDING: "bg-blue-100 text-blue-800",
  PENDING_GUARANTOR_APPROVAL: "bg-yellow-100 text-yellow-800",
  PENDING_GUARANTOR_REPLACEMENT: "bg-yellow-100 text-yellow-800",
  PENDING_GUARANTOR_REASSIGNMENT: "bg-yellow-100 text-yellow-800",
  PENDING_LOAN_OFFICER_REVIEW: "bg-yellow-100 text-yellow-800",
  PENDING_CREDIT_COMMITTEE: "bg-yellow-100 text-yellow-800",
  PENDING_TREASURER: "bg-yellow-100 text-yellow-800",
  APPROVED: "bg-green-100 text-green-800",
  REJECTED: "bg-red-100 text-red-800",
  DISBURSED: "bg-purple-100 text-purple-800",
  REPAID: "bg-green-200 text-green-900",
  DEFAULTED: "bg-red-200 text-red-900",
};

interface Loan {
  id: number;
  loanNumber: string;
  member: {
    id: number;
    memberNumber: string;
    firstName: string;
    fullName?: string;
    lastName: string;
  };
  loanProduct: {
    id: number;
    name: string;
    interestRate: number;
  };
  amount: number;
  interestRate: number;
  termMonths: number;
  monthlyRepayment: number;
  totalInterest: number;
  interestCollected?: number;
  principalRepaid?: number;
  totalRepayable: number;
  outstandingBalance: number;
  repaymentPercentage?: number;
  status: string;
  purpose?: string;
  applicationDate: string;
  approvalDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
  memberEligibilityStatus?: string;
  memberEligibilityErrors?: string;
  memberEligibilityWarnings?: string;
  migrationStatus?: string;
  guarantors?: Array<{
    id: number;
    member: {
      id: number;
      memberNumber: string;
      firstName: string;
      fullName?: string;
      lastName: string;
    };
    status: string;
  }>;
  guarantor1EligibilityStatus?: string;
  guarantor1EligibilityErrors?: string;
  guarantor2EligibilityStatus?: string;
  guarantor2EligibilityErrors?: string;
  guarantor3EligibilityStatus?: string;
  guarantor3EligibilityErrors?: string;
}

interface LoanProduct {
  id: number;
  name: string;
  description: string;
  interestRate: number;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  isActive: boolean;
}

interface Member {
  id: number;
  memberNumber: string;
  employeeId?: string;
  firstName: string;
  fullName?: string;
  lastName: string;
  status: string;
}

const Loans = () => {
  const [searchParams] = useSearchParams();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [products, setProducts] = useState<LoanProduct[]>([]);
  const [search, setSearch] = useState("");
  
  // Valid loan statuses from backend
  const validStatuses = [
    "all", "PENDING", "PENDING_GUARANTOR_APPROVAL", "PENDING_GUARANTOR_REPLACEMENT",
    "PENDING_GUARANTOR_REASSIGNMENT", "PENDING_LOAN_OFFICER_REVIEW", 
    "PENDING_CREDIT_COMMITTEE", "PENDING_TREASURER", "APPROVED", "REJECTED", 
    "DISBURSED", "REPAID", "DEFAULTED"
  ];
  
  const [statusFilter, setStatusFilter] = useState(() => {
    // Initialize from query parameter if present, otherwise default to "all"
    const paramStatus = searchParams.get("status") || "all";
    // Validate that the status is in the valid list
    return validStatuses.includes(paramStatus) ? paramStatus : "all";
  });
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewLoan, setViewLoan] = useState<Loan | null>(null);
  const [actionDialog, setActionDialog] = useState<{ loan: Loan; action: string } | null>(null);
  const [actionNotes, setActionNotes] = useState("");
  const [eligibilityValidationOpen, setEligibilityValidationOpen] = useState(false);
  const [eligibilityValidation, setEligibilityValidation] = useState<any>(null);
  const [validatingLoan, setValidatingLoan] = useState<Loan | null>(null);
  const [approvalReason, setApprovalReason] = useState("");
  const [approvalAction, setApprovalAction] = useState<"approve" | "reject" | null>(null);
  const [loanDetailsOpen, setLoanDetailsOpen] = useState(false);
  const [selectedLoanForDetails, setSelectedLoanForDetails] = useState<Loan | null>(null);
  const [preCheck, setPreCheck] = useState<any>(null);
  const [preCheckLoading, setPreCheckLoading] = useState(false);
  const [approvalSubmitting, setApprovalSubmitting] = useState(false);
  const [guarantorModalOpen, setGuarantorModalOpen] = useState(false);
  const [selectedLoanGuarantors, setSelectedLoanGuarantors] = useState<any[]>([]);
  const [selectedLoanForGuarantors, setSelectedLoanForGuarantors] = useState<Loan | null>(null);
  
  // Delete loan state
  const [deleteLoanDialog, setDeleteLoanDialog] = useState(false);
  const [loanToDelete, setLoanToDelete] = useState<Loan | null>(null);
  const [deleteReason, setDeleteReason] = useState("");
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  
  // Full edit state
  const [fullEditDialog, setFullEditDialog] = useState(false);
  const [fullEditForm, setFullEditForm] = useState({
    principal: "",
    outstandingBalance: "",
    interestRate: "",
    termMonths: "",
    totalInterest: "",
    totalRepayable: "",
    monthlyRepayment: "",
    interestCollected: "",
    principalRepaid: "",
    reason: ""
  });
  const [fullEditSubmitting, setFullEditSubmitting] = useState(false);
  
  // Top-up states
  const [topUpDialogOpen, setTopUpDialogOpen] = useState(false);
  const [topUpAmount, setTopUpAmount] = useState("");
  const [topUpPurpose, setTopUpPurpose] = useState("");
  const [topUpGuarantors, setTopUpGuarantors] = useState<Array<{ id: string; employeeId: string; pledgeAmount: number }>>([]);
  const [topUpPreview, setTopUpPreview] = useState<any>(null);
  const [topUpHistory, setTopUpHistory] = useState<any[]>([]);
  const [loadingTopUpHistory, setLoadingTopUpHistory] = useState(false);
  const [topUpSubmitting, setTopUpSubmitting] = useState(false);
  
  // Top-up edit/delete states
  const [editTopUpDialog, setEditTopUpDialog] = useState(false);
  const [topUpToEdit, setTopUpToEdit] = useState<any>(null);
  const [editTopUpAmount, setEditTopUpAmount] = useState("");
  const [editTopUpPurpose, setEditTopUpPurpose] = useState("");
  const [editTopUpSubmitting, setEditTopUpSubmitting] = useState(false);
  const [deleteTopUpDialog, setDeleteTopUpDialog] = useState(false);
  const [topUpToDelete, setTopUpToDelete] = useState<any>(null);
  const [deleteTopUpSubmitting, setDeleteTopUpSubmitting] = useState(false);
  
  const { toast } = useToast();
  const { session, role } = useAuth();
  const { refreshKey } = useRefresh();

  // Edit mode state
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [loanToEdit, setLoanToEdit] = useState<Loan | null>(null);
  const [currentEditGuarantors, setCurrentEditGuarantors] = useState<any[]>([]);
  const [editForm, setEditForm] = useState({
    disbursementDate: "",
    outstandingBalance: "",
    termMonths: "",
    guarantorshipType: "NORMAL",
    guarantors: [] as Array<{ employeeId: string; pledgeAmount: number; isNew?: boolean }>
  });
  const [editSubmitting, setEditSubmitting] = useState(false);
  
  // Guarantor management state for edit dialog
  const [removedGuarantorIds, setRemovedGuarantorIds] = useState<number[]>([]); // IDs of removed guarantors

  // Phase A: Low-risk field editing state
  const [phaseAEditOpen, setPhaseAEditOpen] = useState(false);
  const [phaseAForm, setPhaseAForm] = useState({
    loanStatus: "",
    disbursementDate: "",
    interestRate: "",
    outstandingBalance: "",
    interestCollected: "",
    purpose: ""
  });
  const [phaseASubmitting, setPhaseASubmitting] = useState(false);
  const [phaseAErrors, setPhaseAErrors] = useState<Record<string, string>>({});
  // Separate state for each guarantor's input value to prevent shared state issues
  const [guarantorInputValues, setGuarantorInputValues] = useState<Record<number, string>>({});
  const [editedGuarantorAmounts, setEditedGuarantorAmounts] = useState<Record<number, number>>({}); // Updated pledge amounts for kept guarantors
  const [newGuarantorsForEdit, setNewGuarantorsForEdit] = useState<Array<{ id: string; employeeId: string; pledgeAmount: number }>>([]); // New guarantors to add
  const [guarantorsLoading, setGuarantorsLoading] = useState(false); // Loading state for guarantor fetch

  // Reassign Guarantors mode state
  const [reassignDialogOpen, setReassignDialogOpen] = useState(false);
  const [loanForReassign, setLoanForReassign] = useState<Loan | null>(null);
  const [reassignData, setReassignData] = useState<any>(null);
  const [reassignLoading, setReassignLoading] = useState(false);
  const [newGuarantors, setNewGuarantors] = useState<Array<{ memberId: number; guaranteeAmount: number }>>([]);
  const [reassignSubmitting, setReassignSubmitting] = useState(false);

  const canCreateLoans = role === "LOAN_OFFICER" || role === "TELLER";
  const canApproveLoans = role === "CREDIT_COMMITTEE";
  const canDisburseLoans = role === "TREASURER";

  const fetchLoans = async () => {
    setLoading(true);
    try {
      let url = `${API_BASE_URL}/loans`;
      if (statusFilter !== "all") {
        url = `${API_BASE_URL}/loans/status/${statusFilter}`;
      }
      
      if (!session?.token) {
        console.error("No authorization token available");
        toast({ title: "Error", description: "Authentication token missing. Please log in again.", variant: "destructive" });
        setLoading(false);
        return;
      }
      
      const response = await fetch(url, {
        headers: { "Authorization": `Bearer ${session.token}` },
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error("Error fetching loans:", {
          status: response.status,
          statusText: response.statusText,
          url,
          error: errorData
        });
        toast({ 
          title: "Error", 
          description: errorData.message || `Failed to fetch loans: ${response.statusText}`, 
          variant: "destructive" 
        });
      } else {
        const data = await response.json();
        setLoans(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching loans:", error);
      toast({ 
        title: "Error", 
        description: error instanceof Error ? error.message : "Failed to fetch loans", 
        variant: "destructive" 
      });
    }
    setLoading(false);
  };

  const fetchMembers = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/members/status/ACTIVE`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setMembers(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching members:", error);
    }
  };

  const fetchProducts = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/loan-products`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setProducts((data.data || []).filter((p: LoanProduct) => p.isActive));
      }
    } catch (error) {
      console.error("Error fetching products:", error);
    }
  };

  useEffect(() => {
    if (session) {
      // Run all independent fetches in parallel
      Promise.all([
        fetchMembers(),
        fetchProducts(),
        fetch(`${API_BASE_URL}/loan-eligibility-rules`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }).then(r => r.json()).then(data => {
          if (data?.data?.maxLoanTermMonths) {
            setMaxLoanTermMonths(data.data.maxLoanTermMonths);
          }
        }).catch(() => {}),
      ]);
    }
  }, [session]);

  // Separate effect to handle loan fetching when statusFilter changes
  useEffect(() => {
    if (session) {
      fetchLoans();
    }
  }, [session, statusFilter, refreshKey]);

  const handleApplyStatusFilter = () => {
    fetchLoans();
  };

  const handleClearFilters = () => {
    setStatusFilter("all");
    setSearch("");
  };

  const [form, setForm] = useState({ 
    memberId: "", 
    loanProductId: "", 
    amount: "", 
    termMonths: "", 
    purpose: "",
    guarantorIds: [] as number[]
  });
  const [selectedProduct, setSelectedProduct] = useState<LoanProduct | null>(null);
  const [selectedGuarantors, setSelectedGuarantors] = useState<number[]>([]);
  const [maxLoanTermMonths, setMaxLoanTermMonths] = useState<number>(72);
  const [guarantorEmployeeIdInput, setGuarantorEmployeeIdInput] = useState("");
  const [guarantorLookupLoading, setGuarantorLookupLoading] = useState(false);
  const [guarantorLookupResult, setGuarantorLookupResult] = useState<Member | null>(null);
  const [guarantorAmountInput, setGuarantorAmountInput] = useState("");
  const [guarantorEligibilityMap, setGuarantorEligibilityMap] = useState<Record<number, any>>({});
  const [guarantorAmountMap, setGuarantorAmountMap] = useState<Record<number, number>>({});

  // Next of Kin (NOK) Guarantor State
  const [nokGuarantorMap, setNokGuarantorMap] = useState<Record<number, number>>({});  // primaryGuarantorId -> nokGuarantorId
  const [nokEmployeeIdInput, setNokEmployeeIdInput] = useState<Record<number, string>>({});  // primaryGuarantorId -> employeeId input
  const [nokLookupResult, setNokLookupResult] = useState<Record<number, Member | null>>({});  // primaryGuarantorId -> Member
  const [nokLookupLoading, setNokLookupLoading] = useState<Record<number, boolean>>({});  // primaryGuarantorId -> loading state
  const [nokEligibilityMap, setNokEligibilityMap] = useState<Record<number, any>>({});  // nokGuarantorId -> eligibility

  const runPreCheck = async (memberId: string, amount: string, guarantorIds: number[]) => {
    if (!memberId || !amount || parseFloat(amount) <= 0) {
      setPreCheck(null);
      return;
    }
    setPreCheckLoading(true);
    try {
      const params = new URLSearchParams({ memberId, amount });
      guarantorIds.forEach(id => params.append("guarantorIds", String(id)));
      const response = await fetch(`${API_BASE_URL}/loans/pre-check?${params}`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setPreCheck(data.data);
      }
    } catch {
      // silently fail — pre-check is advisory only
    } finally {
      setPreCheckLoading(false);
    }
  };

  const lookupGuarantorByEmployeeId = async (employeeId: string) => {
    if (!employeeId.trim()) {
      setGuarantorLookupResult(null);
      return;
    }
    setGuarantorLookupLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/member/member-by-employee-id/${employeeId}`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        // Backend returns memberId, but we need id for consistency with Member interface
        const member: Member = {
          id: data.memberId,
          memberNumber: data.memberNumber,
          employeeId: data.employeeId,
          firstName: data.firstName,
          lastName: data.lastName,
          status: "ACTIVE"
        };
        setGuarantorLookupResult(member);
      } else {
        setGuarantorLookupResult(null);
        toast({ title: "Not Found", description: `No member found with employee ID: ${employeeId}`, variant: "destructive" });
      }
    } catch (error) {
      setGuarantorLookupResult(null);
      toast({ title: "Error", description: "Failed to lookup member", variant: "destructive" });
    } finally {
      setGuarantorLookupLoading(false);
    }
  };

  const checkGuarantorEligibility = async (guarantorId: number, guaranteeAmount: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/loans/validate-guarantor-eligibility?guarantorMemberId=${guarantorId}&guaranteeAmount=${guaranteeAmount}`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setGuarantorEligibilityMap({...guarantorEligibilityMap, [guarantorId]: data.data});
        return data.data;
      }
    } catch (error) {
      console.error("Error checking guarantor eligibility:", error);
    }
    return null;
  };

  // Next of Kin Guarantor Lookup
  const lookupNokByEmployeeId = async (primaryGuarantorId: number, employeeId: string) => {
    if (!employeeId.trim()) {
      setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: null});
      return;
    }
    setNokLookupLoading({...nokLookupLoading, [primaryGuarantorId]: true});
    try {
      const response = await fetch(`${API_BASE_URL}/member/member-by-employee-id/${employeeId}`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        const member: Member = {
          id: data.memberId,
          memberNumber: data.memberNumber,
          employeeId: data.employeeId,
          firstName: data.firstName,
          lastName: data.lastName,
          status: "ACTIVE"
        };
        
        // Validate NOK is not same as primary
        if (member.id === primaryGuarantorId) {
          toast({ title: "Invalid", description: "Next of kin cannot be the same as primary guarantor", variant: "destructive" });
          setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: null});
          return;
        }
        
        // Validate NOK is not already a primary guarantor
        if (selectedGuarantors.includes(member.id)) {
          toast({ title: "Invalid", description: "This member is already a primary guarantor", variant: "destructive" });
          setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: null});
          return;
        }
        
        setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: member});
        
        // Check eligibility automatically
        const guaranteeAmount = guarantorAmountMap[primaryGuarantorId];
        if (guaranteeAmount) {
          await checkNokEligibility(member.id, guaranteeAmount);
        }
      } else {
        setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: null});
        toast({ title: "Not Found", description: `No member found with employee ID: ${employeeId}`, variant: "destructive" });
      }
    } catch (error) {
      setNokLookupResult({...nokLookupResult, [primaryGuarantorId]: null});
      toast({ title: "Error", description: "Failed to lookup NOK member", variant: "destructive" });
    } finally {
      setNokLookupLoading({...nokLookupLoading, [primaryGuarantorId]: false});
    }
  };

  const checkNokEligibility = async (nokId: number, guaranteeAmount: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/loans/validate-guarantor-eligibility?guarantorMemberId=${nokId}&guaranteeAmount=${guaranteeAmount}`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setNokEligibilityMap({...nokEligibilityMap, [nokId]: data.data});
        return data.data;
      }
    } catch (error) {
      console.error("Error checking NOK eligibility:", error);
    }
    return null;
  };

  const handleProductChange = (productId: string) => {
    const product = products.find(p => p.id === parseInt(productId));
    setSelectedProduct(product || null);
    setForm({ ...form, loanProductId: productId });
  };

  // RESTRUCTURED: Don't calculate interest during application
  // Interest will be set by treasurer at final approval stage
  const calculateLoan = () => {
    return null; // No interest preview during application
  };

  const calc = calculateLoan();

  const effectiveMaxTerm = Math.min(selectedProduct?.maxTermMonths ?? maxLoanTermMonths, maxLoanTermMonths);

  const handleApply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;  // RESTRUCTURED: No longer checking calc since we don't calculate interest at application

    const termVal = parseInt(form.termMonths);
    const minTerm = selectedProduct.minTermMonths ?? 1;

    if (isNaN(termVal) || termVal < minTerm) {
      toast({ title: "Invalid Term", description: `Minimum term for this product is ${minTerm} months`, variant: "destructive" });
      return;
    }
    if (termVal > effectiveMaxTerm) {
      toast({ title: "Invalid Term", description: `Maximum term for this product is ${effectiveMaxTerm} months (${(effectiveMaxTerm / 12).toFixed(1)} yrs)`, variant: "destructive" });
      return;
    }

    // Next of kin is now optional - removed validation

    try {
      // Build guarantor requests with amounts and NOK
      const guarantorRequests = selectedGuarantors.map(guarantorId => ({
        guarantorId: guarantorId,
        guaranteeAmount: guarantorAmountMap[guarantorId] || 0,
        selfGuarantee: false,
        // Include NOK if assigned
        nextOfKinGuarantorId: nokGuarantorMap[guarantorId] || null,
        nextOfKinGuaranteeAmount: nokGuarantorMap[guarantorId] ? guarantorAmountMap[guarantorId] : null
      }));

      const response = await fetch(`${API_BASE_URL}/loans/apply`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          memberId: parseInt(form.memberId),
          loanProductId: parseInt(form.loanProductId),
          amount: parseFloat(form.amount),
          termMonths: parseInt(form.termMonths),
          purpose: form.purpose,
          guarantors: guarantorRequests,
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan application submitted successfully" });
        setDialogOpen(false);
        setForm({ memberId: "", loanProductId: "", amount: "", termMonths: "", purpose: "", guarantorIds: [] });
        setSelectedProduct(null);
        setSelectedGuarantors([]);
        setGuarantorAmountMap({});
        setGuarantorEligibilityMap({});
        setNokGuarantorMap({});
        setNokEmployeeIdInput({});
        setNokLookupResult({});
        setNokEligibilityMap({});
        fetchLoans();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to submit application", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to submit application", variant: "destructive" });
    }
  };

  const validateEligibilityBeforeApproval = async (loan: Loan) => {
    try {
      setValidatingLoan(loan);
      const response = await fetch(`${API_BASE_URL}/loans/${loan.id}/validate-approval`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setEligibilityValidation(data.data);
        setEligibilityValidationOpen(true);
        setActionDialog(null); // Close the action dialog
      } else {
        const errorData = await response.json();
        const errorMessage = errorData.message || "Failed to validate eligibility";
        toast({ title: "Error", description: errorMessage, variant: "destructive" });
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Failed to validate eligibility";
      toast({ title: "Error", description: errorMessage, variant: "destructive" });
    }
  };

  const handleEyeIconClick = async (loan: Loan) => {
    // Allow viewing any loan regardless of status
    const loanToDisplay = { ...loan };
    
    // DON'T override interestCollected from database
    // The backend already calculates it correctly as:
    // interestCollected = migration snapshot + post-migration repayments
    // Let the backend value be the source of truth
    
    setSelectedLoanForDetails(loanToDisplay);
    setLoanDetailsOpen(true);
    
    // Fetch top-up history if loan is disbursed/active
    if ((loan.status === "DISBURSED" || loan.status === "ACTIVE") && role === "TREASURER") {
      fetchTopUpHistory(loan.id);
    }
  };

  const handleAction = async () => {
    if (!actionDialog) return;
    const { loan, action } = actionDialog;

    setApprovalSubmitting(true);
    try {
      let url = "";
      let body: any = {};

      if (action === "approve") {
        url = `${API_BASE_URL}/loans/approve`;
        body = {
          loanId: loan.id,
          approved: true,
          comments: actionNotes || "Approved",
          // REDUCING BALANCE: No interest calculation at approval
          // Treasurer simply approves the loan for disbursement
        };
      } else if (action === "reject") {
        url = `${API_BASE_URL}/loans/approve`;
        body = {
          loanId: loan.id,
          approved: false,
          comments: actionNotes || "Rejected",
        };
      } else if (action === "disburse") {
        url = `${API_BASE_URL}/loans/disburse/${loan.id}`;
      }

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: action === "disburse" ? undefined : JSON.stringify(body),
      });

      if (response.ok) {
        toast({ title: "Success", description: `Loan ${action}d successfully` });
        setActionDialog(null);
        setActionNotes("");
        setLoanDetailsOpen(false);
        fetchLoans();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || `Failed to ${action} loan`, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: `Failed to ${action} loan`, variant: "destructive" });
    } finally {
      setApprovalSubmitting(false);
    }
  };

  // Phase A: Low-risk field editing handler
  const handlePhaseAEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoanForDetails) return;

    // Client-side validation
    const errors: Record<string, string> = {};
    
    if (phaseAForm.disbursementDate) {
      const selectedDate = new Date(phaseAForm.disbursementDate);
      if (selectedDate > new Date()) {
        errors.disbursementDate = "Disbursement date cannot be in the future";
      }
    }

    if (phaseAForm.interestRate) {
      const rate = parseFloat(phaseAForm.interestRate);
      if (isNaN(rate) || rate < 0) {
        errors.interestRate = "Interest rate must be >= 0";
      }
    }

    if (phaseAForm.outstandingBalance) {
      const balance = parseFloat(phaseAForm.outstandingBalance);
      if (isNaN(balance) || balance < 0) {
        errors.outstandingBalance = "Outstanding balance must be >= 0";
      }
      if (balance > selectedLoanForDetails.amount) {
        errors.outstandingBalance = `Outstanding balance cannot exceed principal (${selectedLoanForDetails.amount})`;
      }
    }

    if (phaseAForm.interestCollected) {
      const collected = parseFloat(phaseAForm.interestCollected);
      if (isNaN(collected) || collected < 0) {
        errors.interestCollected = "Interest collected must be >= 0";
      }
      // Interest collected cannot exceed total interest (will be validated by backend)
    }

    if (phaseAForm.loanStatus && !["PENDING", "APPROVED", "DISBURSED", "REPAID", "DEFAULTED"].includes(phaseAForm.loanStatus)) {
      errors.loanStatus = "Invalid loan status";
    }

    // Check if at least one field is filled
    const hasAtLeastOne = phaseAForm.loanStatus || phaseAForm.disbursementDate || 
                         phaseAForm.interestRate || phaseAForm.outstandingBalance || 
                         phaseAForm.interestCollected || phaseAForm.purpose;
    if (!hasAtLeastOne) {
      setPhaseAErrors({ _form: "At least one field must be updated" });
      return;
    }

    if (Object.keys(errors).length > 0) {
      setPhaseAErrors(errors);
      return;
    }

    // Prepare request body - only include non-empty fields
    const requestBody: any = {};
    if (phaseAForm.loanStatus) requestBody.loanStatus = phaseAForm.loanStatus;
    if (phaseAForm.disbursementDate) requestBody.disbursementDate = phaseAForm.disbursementDate;
    if (phaseAForm.interestRate) requestBody.interestRate = parseFloat(phaseAForm.interestRate);
    if (phaseAForm.outstandingBalance) requestBody.outstandingBalance = parseFloat(phaseAForm.outstandingBalance);
    if (phaseAForm.interestCollected) requestBody.interestCollected = parseFloat(phaseAForm.interestCollected);
    if (phaseAForm.purpose) requestBody.purpose = phaseAForm.purpose;

    setPhaseASubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${selectedLoanForDetails.id}/fields/update`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(requestBody),
      });

      if (response.ok) {
        const data = await response.json();
        toast({ 
          title: "Success", 
          description: "Loan fields updated successfully (Phase A - No guarantor data sent)",
          variant: "default"
        });
        
        // Update selectedLoanForDetails with the response data
        if (selectedLoanForDetails && data.data) {
          setSelectedLoanForDetails({
            ...selectedLoanForDetails,
            ...data.data,
            status: data.data.status || selectedLoanForDetails.status,
            disbursementDate: data.data.disbursementDate || selectedLoanForDetails.disbursementDate,
            interestRate: data.data.interestRate !== undefined ? data.data.interestRate : selectedLoanForDetails.interestRate,
            outstandingBalance: data.data.outstandingBalance !== undefined ? data.data.outstandingBalance : selectedLoanForDetails.outstandingBalance,
            interestCollected: data.data.interestCollected !== undefined ? data.data.interestCollected : selectedLoanForDetails.interestCollected,
            purpose: data.data.purpose || selectedLoanForDetails.purpose,
          });
        }
        
        setPhaseAEditOpen(false);
        setPhaseAForm({ loanStatus: "", disbursementDate: "", interestRate: "", outstandingBalance: "", interestCollected: "", purpose: "" });
        setPhaseAErrors({});
        fetchLoans();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to update loan fields", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to update loan fields", variant: "destructive" });
    } finally {
      setPhaseASubmitting(false);
    }
  };

  const filteredLoans = loans
    .filter(loan => loan.status !== 'REPAID') // Hide REPAID loans (zero-balance duplicates)
    .filter(loan =>
      !search ||
      loan.loanNumber?.toLowerCase().includes(search.toLowerCase()) ||
      `${loan.member?.firstName} ${loan.member?.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
      loan.member?.memberNumber?.toLowerCase().includes(search.toLowerCase())
    );

  const handleViewGuarantors = async (loan: Loan) => {
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${loan.id}/guarantors`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setSelectedLoanGuarantors(data.data || []);
        setSelectedLoanForGuarantors(loan);
        setGuarantorModalOpen(true);
      } else {
        toast({ title: "Error", description: "Failed to load guarantors", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to load guarantors", variant: "destructive" });
    }
  };

  const handleOpenEditDialog = (loan: Loan) => {
    setLoanToEdit(loan);
    setEditForm({
      disbursementDate: loan.disbursementDate ? new Date(loan.disbursementDate).toISOString().split('T')[0] : "",
      outstandingBalance: loan.outstandingBalance ? String(loan.outstandingBalance) : "",
      termMonths: loan.termMonths ? String(loan.termMonths) : "",
      guarantorshipType: "",
      guarantors: []
    });
    
    // Reset guarantor state BEFORE loading
    setRemovedGuarantorIds([]);
    setEditedGuarantorAmounts({});
    setGuarantorInputValues({}); // Reset input values
    setNewGuarantorsForEdit([]);
    setGuarantorsLoading(true);
    
    // Load current guarantors
    const loadGuarantors = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/loans/${loan.id}/guarantors`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        });
        if (response.ok) {
          const data = await response.json();
          const guarantors = data.data || [];
          setCurrentEditGuarantors(guarantors);
          setGuarantorsLoading(false);
        }
      } catch (error) {
        console.error("Failed to load guarantors", error);
        setGuarantorsLoading(false);
      }
    };
    
    loadGuarantors();
    setEditDialogOpen(true);
  };

  const handleEditLoan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loanToEdit) return;

    setEditSubmitting(true);
    try {
      const updatePayload: any = {};

      // Only include fields that have been filled/changed
      if (editForm.disbursementDate) {
        updatePayload.disbursementDate = editForm.disbursementDate;
      }
      if (editForm.outstandingBalance) {
        const outstanding = parseFloat(editForm.outstandingBalance);
        if (isNaN(outstanding) || outstanding < 0) {
          toast({ title: "Error", description: "Outstanding balance must be a valid number >= 0", variant: "destructive" });
          setEditSubmitting(false);
          return;
        }
        if (outstanding > loanToEdit.amount) {
          toast({ title: "Error", description: `Outstanding balance cannot exceed principal (KES ${loanToEdit.amount.toLocaleString()})`, variant: "destructive" });
          setEditSubmitting(false);
          return;
        }
        updatePayload.outstandingBalance = outstanding;
      }
      if (editForm.termMonths) {
        const term = parseInt(editForm.termMonths);
        if (isNaN(term) || term <= 0) {
          toast({ title: "Error", description: "Term months must be greater than 0", variant: "destructive" });
          setEditSubmitting(false);
          return;
        }
        updatePayload.termMonths = term;
      }

      // Handle guarantor changes
      const finalOutstanding = updatePayload.outstandingBalance || loanToEdit.outstandingBalance || loanToEdit.amount;
      
      // Check if any guarantor changes were made
      const hasGuarantorChanges = removedGuarantorIds.length > 0 || 
                                  Object.keys(editedGuarantorAmounts).length > 0 ||
                                  newGuarantorsForEdit.length > 0;

      if (hasGuarantorChanges) {
        // Collect all guarantors to submit (kept + new)
        const guarantorsToSubmit: Array<{ employeeId: string; pledgeAmount: number }> = [];

        // Add kept guarantors (all current guarantors NOT in removedGuarantorIds)
        currentEditGuarantors.forEach((guarantor: any) => {
          if (!removedGuarantorIds.includes(guarantor.guarantorId)) {
            const amount = editedGuarantorAmounts[guarantor.guarantorId] !== undefined
              ? editedGuarantorAmounts[guarantor.guarantorId]
              : guarantor.guaranteeAmount;
            
            if (amount > 0) {
              guarantorsToSubmit.push({
                employeeId: guarantor.memberNumber || guarantor.employeeId,
                pledgeAmount: amount
              });
            }
          }
        });

        // Add new guarantors (strip the temporary ID field)
        guarantorsToSubmit.push(...newGuarantorsForEdit
          .filter(g => g.employeeId && g.pledgeAmount > 0)
          .map(g => ({
            employeeId: g.employeeId,
            pledgeAmount: g.pledgeAmount
          })));

        // Final check for duplicate guarantors
        const allEmployeeIds = guarantorsToSubmit.map(g => g.employeeId);
        const uniqueIds = new Set(allEmployeeIds);
        if (allEmployeeIds.length !== uniqueIds.size) {
          toast({ 
            title: "Duplicate Guarantor", 
            description: "You have the same member as a guarantor multiple times. Please ensure each guarantor is unique.",
            variant: "destructive" 
          });
          setEditSubmitting(false);
          return;
        }

        // Validate total guarantees vs outstanding balance
        const totalGuarantees = guarantorsToSubmit.reduce((sum, g) => sum + g.pledgeAmount, 0);
        
        // Special case: If outstanding is 0, loan is fully paid - allow removing all guarantors
        if (finalOutstanding === 0 || finalOutstanding === null || isNaN(finalOutstanding)) {
          // No validation needed for fully paid loans
          // Allow proceeding with any guarantor configuration (including zero guarantors)
        } else {
          // Normal validation for active loans
          const percentage = (totalGuarantees / finalOutstanding) * 100;
          
          // Check if exceeds 100%
          if (percentage > 100) {
            const confirmed = window.confirm(
              `Warning: Total guarantor coverage is ${percentage.toFixed(1)}% (exceeds 100%).\n\n` +
              `Total pledged: KES ${totalGuarantees.toLocaleString()}\n` +
              `Outstanding balance: KES ${finalOutstanding.toLocaleString()}\n\n` +
              `Do you want to proceed anyway?`
            );
            if (!confirmed) {
              setEditSubmitting(false);
              return;
            }
          }
        }

        // Allow small rounding differences (within 1 unit) - but only for active loans
        if (finalOutstanding > 0 && !isNaN(finalOutstanding)) {
          const difference = Math.abs(totalGuarantees - loanToEdit.amount);
          if (difference > 1) {
            toast({ 
              title: "Validation Error", 
              description: `Total guarantees (KES ${totalGuarantees.toLocaleString()}) must equal principal amount (KES ${loanToEdit.amount.toLocaleString()})`, 
              variant: "destructive" 
            });
            setEditSubmitting(false);
            return;
          }

          if (guarantorsToSubmit.length === 0) {
            toast({ title: "Error", description: "At least one guarantor must be assigned for active loans", variant: "destructive" });
            setEditSubmitting(false);
            return;
          }
        }
        // For fully paid loans (outstanding = 0), allow any guarantor configuration including zero guarantors

        updatePayload.guarantorshipType = "NORMAL";
        updatePayload.guarantors = guarantorsToSubmit;
      }

      // Must have at least one field to update
      if (Object.keys(updatePayload).length === 0) {
        toast({ title: "Error", description: "Please fill in at least one field to update", variant: "destructive" });
        setEditSubmitting(false);
        return;
      }

      const response = await fetch(`${API_BASE_URL}/loans/${loanToEdit.id}/fields/update`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(updatePayload),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan updated successfully" });
        setEditDialogOpen(false);
        setLoanToEdit(null);
        setCurrentEditGuarantors([]);
        setRemovedGuarantorIds([]);
        setEditedGuarantorAmounts({});
        setNewGuarantorsForEdit([]);
        setEditForm({
          disbursementDate: "",
          outstandingBalance: "",
          termMonths: "",
          guarantorshipType: "",
          guarantors: []
        });
        fetchLoans();
        
        // Fetch the UPDATED loan from backend before displaying
        if (selectedLoanForDetails && selectedLoanForDetails.id === loanToEdit.id) {
          try {
            const loanResponse = await fetch(`${API_BASE_URL}/loans/${loanToEdit.id}`, {
              headers: { "Authorization": `Bearer ${session?.token}` },
            });
            if (loanResponse.ok) {
              const loanData = await loanResponse.json();
              const updatedLoan = loanData.data;
              // Refresh loan details with fresh data
              handleEyeIconClick(updatedLoan);
            }
          } catch (error) {
            console.error("Error fetching updated loan:", error);
          }
        }
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to update loan", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to update loan", variant: "destructive" });
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleOpenReassignGuarantorsDialog = async (loan: Loan) => {
    setLoanForReassign(loan);
    setReassignDialogOpen(true);
    setReassignLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${loan.id}/reassign-guarantors-data`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setReassignData(data.data);
        setNewGuarantors([]);
      } else {
        toast({ title: "Error", description: "Failed to load reassign data", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to load reassign data", variant: "destructive" });
    } finally {
      setReassignLoading(false);
    }
  };

  const handleSubmitReassignGuarantors = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loanForReassign || newGuarantors.length === 0) {
      toast({ title: "Error", description: "Please select at least one guarantor", variant: "destructive" });
      return;
    }

    setReassignSubmitting(true);
    try {
      const guarantorAssignments = newGuarantors.map(g => ({
        memberNumber: reassignData.availableMembers.find((m: any) => m.memberId === g.memberId)?.memberNumber,
        guaranteeAmount: g.guaranteeAmount
      }));

      const response = await fetch(`${API_BASE_URL}/loans/${loanForReassign.id}/reassign-guarantors`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(guarantorAssignments),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Guarantors reassigned successfully" });
        setReassignDialogOpen(false);
        setLoanForReassign(null);
        setReassignData(null);
        setNewGuarantors([]);
        fetchLoans();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to reassign guarantors", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to reassign guarantors", variant: "destructive" });
    } finally {
      setReassignSubmitting(false);
    }
  };

  // Delete loan handler
  const handleDeleteLoan = async () => {
    if (!loanToDelete || !deleteReason.trim()) {
      toast({ title: "Error", description: "Please provide a reason for deletion", variant: "destructive" });
      return;
    }

    setDeleteSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${loanToDelete.id}`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({ reason: deleteReason }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan deleted successfully" });
        setDeleteLoanDialog(false);
        setLoanToDelete(null);
        setDeleteReason("");
        fetchLoans();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to delete loan", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to delete loan", variant: "destructive" });
    } finally {
      setDeleteSubmitting(false);
    }
  };

  // Fetch top-up history
  const fetchTopUpHistory = async (loanId: number) => {
    setLoadingTopUpHistory(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${loanId}/topup-history`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        console.log('Top-up history for loan', loanId, ':', data);
        setTopUpHistory(data.data || data || []);
      } else {
        console.error('Failed to fetch top-up history:', response.status);
        setTopUpHistory([]);
      }
    } catch (error) {
      console.error("Error fetching top-up history:", error);
      setTopUpHistory([]);
    } finally {
      setLoadingTopUpHistory(false);
    }
  };

  // Preview top-up
  const previewTopUp = async (loanId: number, amount: string) => {
    if (!amount || parseFloat(amount) <= 0) {
      setTopUpPreview(null);
      return;
    }
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${loanId}/topup-preview?amount=${amount}`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setTopUpPreview(data.data);
      }
    } catch (error) {
      console.error("Error previewing top-up:", error);
    }
  };

  // Handle top-up submission
  const handleTopUpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoanForDetails || !topUpAmount || parseFloat(topUpAmount) <= 0) {
      toast({ title: "Error", description: "Please enter a valid top-up amount", variant: "destructive" });
      return;
    }

    // Validate guarantors if provided
    const validGuarantors = topUpGuarantors.filter(g => g.employeeId && g.pledgeAmount > 0);
    if (topUpGuarantors.length > 0 && validGuarantors.length === 0) {
      toast({ title: "Error", description: "Please complete all guarantor details or remove empty entries", variant: "destructive" });
      return;
    }

    // Final check for duplicate guarantors
    if (validGuarantors.length > 0) {
      const employeeIds = validGuarantors.map(g => g.employeeId);
      const uniqueIds = new Set(employeeIds);
      if (employeeIds.length !== uniqueIds.size) {
        toast({ 
          title: "Duplicate Guarantor", 
          description: "You have selected the same guarantor multiple times. Please ensure each guarantor is unique.",
          variant: "destructive" 
        });
        setTopUpSubmitting(false);
        return;
      }
    }

    // Check if guarantors exceed 100%
    if (validGuarantors.length > 0) {
      const totalPledged = validGuarantors.reduce((sum, g) => sum + g.pledgeAmount, 0);
      const percentage = (totalPledged / parseFloat(topUpAmount)) * 100;
      
      if (percentage > 100) {
        const confirmed = window.confirm(
          `Warning: Total guarantor coverage is ${percentage.toFixed(1)}% (exceeds 100%).\n\n` +
          `Total pledged: KES ${totalPledged.toLocaleString()}\n` +
          `Top-up amount: KES ${parseFloat(topUpAmount).toLocaleString()}\n\n` +
          `Do you want to proceed anyway?`
        );
        if (!confirmed) return;
      }
    }

    setTopUpSubmitting(true);
    try {
      // Map guarantors to backend format
      const newGuarantors = validGuarantors.map(g => ({
        guarantorMemberNumber: g.employeeId,
        guaranteeAmount: g.pledgeAmount
      }));

      const response = await fetch(`${API_BASE_URL}/loans/${selectedLoanForDetails.id}/add-topup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          topupAmount: parseFloat(topUpAmount),
          purpose: topUpPurpose || "Loan top-up",
          newGuarantors: newGuarantors.length > 0 ? newGuarantors : null
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan top-up added successfully" });
        setTopUpDialogOpen(false);
        setTopUpAmount("");
        setTopUpPurpose("");
        setTopUpGuarantors([]);
        setTopUpPreview(null);
        fetchLoans();
        // Refresh loan details
        handleEyeIconClick(selectedLoanForDetails);
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to add top-up", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to add top-up", variant: "destructive" });
    } finally {
      setTopUpSubmitting(false);
    }
  };

  // Edit top-up handler
  const handleEditTopUp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!topUpToEdit || !editTopUpAmount || parseFloat(editTopUpAmount) <= 0) {
      toast({ title: "Error", description: "Please enter a valid top-up amount", variant: "destructive" });
      return;
    }

    setEditTopUpSubmitting(true);
    try {
      // Backend expects query parameters, not request body
      const params = new URLSearchParams({
        topupAmount: editTopUpAmount,
        ...(editTopUpPurpose && { purpose: editTopUpPurpose })
      });
      
      const response = await fetch(`${API_BASE_URL}/loans/topup/${topUpToEdit.id}?${params}`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Top-up updated successfully" });
        setEditTopUpDialog(false);
        setTopUpToEdit(null);
        setEditTopUpAmount("");
        setEditTopUpPurpose("");
        fetchLoans();
        // Refresh loan details
        if (selectedLoanForDetails) {
          fetchTopUpHistory(selectedLoanForDetails.id);
          handleEyeIconClick(selectedLoanForDetails);
        }
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to update top-up", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to update top-up", variant: "destructive" });
    } finally {
      setEditTopUpSubmitting(false);
    }
  };

  // Delete top-up handler
  const handleDeleteTopUp = async () => {
    if (!topUpToDelete) return;

    setDeleteTopUpSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/topup/${topUpToDelete.id}`, {
        method: "DELETE",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Top-up deleted successfully" });
        setDeleteTopUpDialog(false);
        setTopUpToDelete(null);
        fetchLoans();
        // Refresh loan details
        if (selectedLoanForDetails) {
          fetchTopUpHistory(selectedLoanForDetails.id);
          handleEyeIconClick(selectedLoanForDetails);
        }
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to delete top-up", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to delete top-up", variant: "destructive" });
    } finally {
      setDeleteTopUpSubmitting(false);
    }
  };

  // Full edit handler
  const handleFullEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoanForDetails) return;

    // Validate at least one field is provided
    const hasChanges = fullEditForm.principal || fullEditForm.outstandingBalance || 
                      fullEditForm.interestRate || fullEditForm.termMonths || 
                      fullEditForm.totalInterest || fullEditForm.totalRepayable || 
                      fullEditForm.monthlyRepayment || fullEditForm.interestCollected ||
                      fullEditForm.principalRepaid;
    
    if (!hasChanges) {
      toast({ title: "Error", description: "Please provide at least one field to update", variant: "destructive" });
      return;
    }

    if (!fullEditForm.reason || !fullEditForm.reason.trim()) {
      toast({ title: "Error", description: "Please provide a reason for this edit", variant: "destructive" });
      return;
    }

    setFullEditSubmitting(true);
    try {
      const params = new URLSearchParams();
      
      // Include fields even if they're 0 (empty string means "no change", "0" means "set to zero")
      if (fullEditForm.principal !== "") params.append("principal", fullEditForm.principal);
      if (fullEditForm.outstandingBalance !== "") params.append("outstandingBalance", fullEditForm.outstandingBalance);
      if (fullEditForm.interestRate !== "") params.append("interestRate", fullEditForm.interestRate);
      if (fullEditForm.termMonths !== "") params.append("termMonths", fullEditForm.termMonths);
      if (fullEditForm.totalInterest !== "") params.append("totalInterest", fullEditForm.totalInterest);
      if (fullEditForm.totalRepayable !== "") params.append("totalRepayable", fullEditForm.totalRepayable);
      if (fullEditForm.monthlyRepayment !== "") params.append("monthlyRepayment", fullEditForm.monthlyRepayment);
      if (fullEditForm.interestCollected !== "") params.append("interestCollected", fullEditForm.interestCollected);
      if (fullEditForm.principalRepaid !== "") params.append("principalRepaid", fullEditForm.principalRepaid);
      params.append("reason", fullEditForm.reason);

      const response = await fetch(`${API_BASE_URL}/loans/${selectedLoanForDetails.id}/update-financials?${params}`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan financials updated successfully" });
        setFullEditDialog(false);
        setFullEditForm({
          principal: "",
          outstandingBalance: "",
          interestRate: "",
          termMonths: "",
          totalInterest: "",
          totalRepayable: "",
          monthlyRepayment: "",
          interestCollected: "",
          principalRepaid: "",
          reason: ""
        });
        fetchLoans();
        
        // Fetch the UPDATED loan from backend before displaying
        try {
          const loanResponse = await fetch(`${API_BASE_URL}/loans/${selectedLoanForDetails.id}`, {
            headers: { "Authorization": `Bearer ${session?.token}` },
          });
          if (loanResponse.ok) {
            const loanData = await loanResponse.json();
            const updatedLoan = loanData.data;
            // Refresh loan details with fresh data
            handleEyeIconClick(updatedLoan);
          } else {
            // Fallback: close details and let user reopen
            setLoanDetailsOpen(false);
          }
        } catch (error) {
          console.error("Error fetching updated loan:", error);
          setLoanDetailsOpen(false);
        }
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to update loan", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: error instanceof Error ? error.message : "Failed to update loan", variant: "destructive" });
    } finally {
      setFullEditSubmitting(false);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Loans</h1>
          <p className="text-muted-foreground">Manage loan applications, approvals, and repayments</p>
        </div>
        {canCreateLoans && (
          <Dialog open={dialogOpen} onOpenChange={(open) => { setDialogOpen(open); if (!open) { setPreCheck(null); setSelectedGuarantors([]); } }}>
            <DialogTrigger asChild>
              <Button><Plus className="mr-2 h-4 w-4" />New Loan Application</Button>
            </DialogTrigger>
            <DialogContent className="max-w-lg max-h-[85vh] overflow-y-auto p-4">
              <DialogHeader className="pb-2"><DialogTitle className="text-base">New Loan Application</DialogTitle></DialogHeader>
              <form onSubmit={handleApply} className="space-y-3">
                <div className="space-y-2">
                  <Label>Member *</Label>
                  <Select
                    value={form.memberId}
                    onValueChange={v => {
                      setForm({...form, memberId: v});
                      runPreCheck(v, form.amount, selectedGuarantors);
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select member" />
                    </SelectTrigger>
                    <SelectContent>
                      {members.length === 0 ? (
                        <div className="p-2 text-sm text-muted-foreground">No active members found</div>
                      ) : (
                        members.map(m => (
                          <SelectItem key={m.id} value={m.id.toString()}>
                            {m.employeeId || m.memberNumber} — {m.firstName} {m.lastName}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Loan Product *</Label>
                  <Select value={form.loanProductId} onValueChange={handleProductChange}>
                    <SelectTrigger><SelectValue placeholder="Select product" /></SelectTrigger>
                    <SelectContent>
                      {products.map(p => (
                        <SelectItem key={p.id} value={p.id.toString()}>
                          {p.name} ({p.interestRate}% p.a.)
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                {selectedProduct && (
                  <Alert>
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription className="text-xs">
                      Range: KES {selectedProduct.minAmount.toLocaleString()} — KES {selectedProduct.maxAmount.toLocaleString()} | 
                      Term: {selectedProduct.minTermMonths}–{Math.min(selectedProduct.maxTermMonths, maxLoanTermMonths)} months
                      {selectedProduct.maxTermMonths > maxLoanTermMonths && ` (capped at SACCO policy max of ${maxLoanTermMonths} months)`}
                    </AlertDescription>
                  </Alert>
                )}
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Amount (KES) *</Label>
                    <Input 
                      type="number" 
                      value={form.amount} 
                      onChange={e => {
                        // Allow any input while typing
                        setForm({...form, amount: e.target.value});
                      }}
                      onBlur={e => {
                        const val = parseFloat(e.target.value);
                        if (!isNaN(val) && selectedProduct) {
                          // Clamp to product range on blur
                          const clamped = Math.min(Math.max(val, selectedProduct.minAmount), selectedProduct.maxAmount);
                          setForm({...form, amount: String(clamped)});
                          runPreCheck(form.memberId, String(clamped), selectedGuarantors);
                        } else if (e.target.value !== "") {
                          runPreCheck(form.memberId, e.target.value, selectedGuarantors);
                        }
                      }}
                      required 
                      placeholder={selectedProduct ? `${selectedProduct.minAmount.toLocaleString()} – ${selectedProduct.maxAmount.toLocaleString()}` : "Enter amount"}
                      className={form.amount && selectedProduct && (parseFloat(form.amount) < selectedProduct.minAmount || parseFloat(form.amount) > selectedProduct.maxAmount) ? "border-red-500" : ""}
                    />
                    {form.amount && selectedProduct && (
                      <>
                        {parseFloat(form.amount) < selectedProduct.minAmount && (
                          <p className="text-xs text-red-500">Below minimum of KES {selectedProduct.minAmount.toLocaleString()}</p>
                        )}
                        {parseFloat(form.amount) > selectedProduct.maxAmount && (
                          <p className="text-xs text-red-500">Exceeds maximum of KES {selectedProduct.maxAmount.toLocaleString()}</p>
                        )}
                      </>
                    )}
                  </div>
                  <div className="space-y-2">
                    <Label>Term (months) * <span className="text-xs text-muted-foreground">max {effectiveMaxTerm} months ({(effectiveMaxTerm / 12).toFixed(1)} yrs)</span></Label>
                    <Input 
                      type="number" 
                      value={form.termMonths} 
                      onChange={e => {
                        const val = e.target.value;
                        // Allow typing but clamp on blur; just store raw value
                        setForm({...form, termMonths: val});
                      }}
                      onBlur={e => {
                        const val = parseInt(e.target.value);
                        const min = selectedProduct?.minTermMonths ?? 1;
                        if (!isNaN(val)) {
                          const clamped = Math.min(Math.max(val, min), effectiveMaxTerm);
                          setForm({...form, termMonths: String(clamped)});
                        }
                      }}
                      required 
                      min={selectedProduct?.minTermMonths ?? 1}
                      max={effectiveMaxTerm}
                      placeholder={`${selectedProduct?.minTermMonths ?? 1} – ${effectiveMaxTerm}`}
                      className={form.termMonths && (parseInt(form.termMonths) > effectiveMaxTerm || parseInt(form.termMonths) < (selectedProduct?.minTermMonths ?? 1)) ? "border-red-500" : ""}
                    />
                    {form.termMonths && parseInt(form.termMonths) > effectiveMaxTerm && (
                      <p className="text-xs text-red-500">Exceeds max of {effectiveMaxTerm} months</p>
                    )}
                  </div>
                </div>
                {calc && (
                  <div className="bg-accent p-3 rounded-md text-sm space-y-1">
                    <div className="bg-blue-50 border border-blue-200 rounded-md p-3">
                      <p className="text-sm text-blue-900">
                        <strong>Interest Calculation:</strong> Interest will be reviewed and set by the Treasurer at the final approval stage. 
                        You will be notified with the complete repayment details once the interest is confirmed.
                      </p>
                    </div>
                  </div>
                )}
                <div className="space-y-2">
                  <Label>Guarantors (Optional - Max 3)</Label>
                  
                  {/* Employee ID Lookup with Amount Input */}
                  <div className="space-y-2 border rounded-md p-3 bg-slate-50">
                    <p className="text-xs font-medium text-muted-foreground">Add by Employee ID</p>
                    <div className="flex gap-2">
                      <Input
                        placeholder="Enter employee ID (e.g., EMP009)"
                        value={guarantorEmployeeIdInput}
                        onChange={(e) => setGuarantorEmployeeIdInput(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            lookupGuarantorByEmployeeId(guarantorEmployeeIdInput);
                          }
                        }}
                        className="text-sm"
                      />
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => lookupGuarantorByEmployeeId(guarantorEmployeeIdInput)}
                        disabled={guarantorLookupLoading || !guarantorEmployeeIdInput.trim()}
                      >
                        {guarantorLookupLoading ? "..." : "Search"}
                      </Button>
                    </div>
                    
                    {guarantorLookupResult && (
                      <div className="bg-white border border-green-200 rounded p-3 space-y-3">
                        <div>
                          <p className="text-sm font-medium">
                            {guarantorLookupResult.firstName} {guarantorLookupResult.lastName}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {guarantorLookupResult.employeeId} • {guarantorLookupResult.memberNumber}
                          </p>
                        </div>
                        
                        {/* Guarantee Amount Input */}
                        <div className="space-y-2">
                          <Label className="text-xs">Guarantee Amount (KES)</Label>
                          <div className="flex gap-2">
                            <Input
                              type="number"
                              placeholder="Enter amount to guarantee"
                              value={guarantorAmountInput}
                              onChange={(e) => {
                                setGuarantorAmountInput(e.target.value);
                                // Trigger live eligibility check when amount changes
                                if (e.target.value && parseFloat(e.target.value) > 0 && guarantorLookupResult?.id) {
                                  checkGuarantorEligibility(guarantorLookupResult.id, parseFloat(e.target.value));
                                }
                              }}
                              min="0"
                              step="1000"
                              className="text-sm"
                            />
                            <Button
                              type="button"
                              size="sm"
                              onClick={async () => {
                                if (!guarantorAmountInput || guarantorAmountInput.trim() === "" || isNaN(parseFloat(guarantorAmountInput)) || parseFloat(guarantorAmountInput) <= 0) {
                                  toast({ title: "Error", description: "Please enter a valid amount", variant: "destructive" });
                                  return;
                                }
                                
                                const guaranteeAmount = parseFloat(guarantorAmountInput);
                                const loanAmount = parseFloat(form.amount);
                                
                                if (!guarantorLookupResult || !guarantorLookupResult.id) {
                                  toast({ title: "Error", description: "Please select a guarantor first", variant: "destructive" });
                                  return;
                                }
                                
                                // Calculate total guaranteed amount including this new guarantor
                                const currentTotalGuaranteed = selectedGuarantors.reduce((sum, gId) => sum + (guarantorAmountMap[gId] || 0), 0);
                                const newTotalGuaranteed = currentTotalGuaranteed + guaranteeAmount;
                                
                                // Validate total guaranteed amount doesn't exceed loan amount
                                if (newTotalGuaranteed > loanAmount) {
                                  toast({ 
                                    title: "Exceeds Loan Amount", 
                                    description: `Total guaranteed (KES ${newTotalGuaranteed.toLocaleString()}) cannot exceed loan amount (KES ${loanAmount.toLocaleString()})`, 
                                    variant: "destructive" 
                                  });
                                  return;
                                }
                                
                                // Check eligibility
                                const eligibility = await checkGuarantorEligibility(guarantorLookupResult.id, guaranteeAmount);
                                
                                if (eligibility && eligibility.eligible) {
                                  // Add guarantor with amount
                                  const updated = [...selectedGuarantors, guarantorLookupResult.id];
                                  setSelectedGuarantors(updated);
                                  setGuarantorAmountMap({...guarantorAmountMap, [guarantorLookupResult.id]: guaranteeAmount});
                                  setGuarantorEmployeeIdInput("");
                                  setGuarantorAmountInput("");
                                  setGuarantorLookupResult(null);
                                  toast({ title: "Success", description: `${guarantorLookupResult.firstName} added as guarantor for KES ${guaranteeAmount.toLocaleString()}` });
                                  runPreCheck(form.memberId, form.amount, updated);
                                } else {
                                  toast({ 
                                    title: "Not Eligible", 
                                    description: eligibility?.errors?.[0] || "This guarantor cannot guarantee this amount", 
                                    variant: "destructive" 
                                  });
                                }
                              }}
                            >
                              Add
                            </Button>
                          </div>
                          
                          {/* Live Eligibility Check */}
                          {guarantorAmountInput && parseFloat(guarantorAmountInput) > 0 && (
                            <div className="text-xs space-y-1">
                              {guarantorEligibilityMap[guarantorLookupResult?.id] ? (
                                guarantorEligibilityMap[guarantorLookupResult.id].eligible ? (
                                  <div className="flex items-center gap-1 text-green-600">
                                    <CheckCircle className="h-3 w-3" />
                                    <span>✓ Eligible to guarantee KES {parseFloat(guarantorAmountInput).toLocaleString()}</span>
                                  </div>
                                ) : (
                                  <div className="flex items-center gap-1 text-red-600">
                                    <XCircle className="h-3 w-3" />
                                    <span>✗ {guarantorEligibilityMap[guarantorLookupResult.id].errors?.[0] || "Not eligible"}</span>
                                  </div>
                                )
                              ) : null}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Added Guarantors List */}
                  {selectedGuarantors.length > 0 && (
                    <div className="space-y-2 border rounded-md p-3 bg-blue-50">
                      <p className="text-xs font-medium text-muted-foreground">Added Guarantors ({selectedGuarantors.length}/3)</p>
                      {selectedGuarantors.map((guarantorId) => {
                        const guarantor = members.find(m => m.id === guarantorId);
                        const amount = guarantorAmountMap[guarantorId] || 0;
                        const eligibility = guarantorEligibilityMap[guarantorId];
                        const nokMember = nokLookupResult[guarantorId];
                        const nokId = nokGuarantorMap[guarantorId];
                        const nokEligibility = nokId ? nokEligibilityMap[nokId] : null;
                        
                        return (
                          <div key={guarantorId} className="bg-white p-3 rounded border border-blue-200 space-y-3">
                            {/* Primary Guarantor Info */}
                            <div className="flex items-center justify-between">
                              <div className="flex-1">
                                <p className="text-sm font-medium">{guarantor?.firstName} {guarantor?.lastName}</p>
                                <p className="text-xs text-muted-foreground">
                                  {guarantor?.employeeId} • Guaranteeing: KES {amount.toLocaleString()}
                                </p>
                              </div>
                              <div className="flex items-center gap-2">
                                {eligibility?.eligible ? (
                                  <CheckCircle className="h-4 w-4 text-green-600" />
                                ) : (
                                  <XCircle className="h-4 w-4 text-red-600" />
                                )}
                                <Button
                                  type="button"
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => {
                                    const updated = selectedGuarantors.filter(id => id !== guarantorId);
                                    setSelectedGuarantors(updated);
                                    const newAmountMap = {...guarantorAmountMap};
                                    delete newAmountMap[guarantorId];
                                    setGuarantorAmountMap(newAmountMap);
                                    // Clear NOK data
                                    const newNokMap = {...nokGuarantorMap};
                                    delete newNokMap[guarantorId];
                                    setNokGuarantorMap(newNokMap);
                                    runPreCheck(form.memberId, form.amount, updated);
                                  }}
                                >
                                  Remove
                                </Button>
                              </div>
                            </div>

                            {/* Next of Kin Guarantor Section */}
                            <div className="border-t pt-3 space-y-2">
                              <Label className="text-xs font-semibold text-blue-700">
                                Next of Kin (Backup) Guarantor (Optional)
                              </Label>
                              
                              {!nokId ? (
                                <div className="space-y-2">
                                  <div className="flex gap-2">
                                    <Input
                                      placeholder="Enter NOK employee ID"
                                      value={nokEmployeeIdInput[guarantorId] || ""}
                                      onChange={(e) => setNokEmployeeIdInput({...nokEmployeeIdInput, [guarantorId]: e.target.value})}
                                      onKeyDown={(e) => {
                                        if (e.key === "Enter") {
                                          lookupNokByEmployeeId(guarantorId, nokEmployeeIdInput[guarantorId] || "");
                                        }
                                      }}
                                      className="text-sm"
                                    />
                                    <Button
                                      type="button"
                                      size="sm"
                                      onClick={() => lookupNokByEmployeeId(guarantorId, nokEmployeeIdInput[guarantorId] || "")}
                                      disabled={nokLookupLoading[guarantorId] || !nokEmployeeIdInput[guarantorId]?.trim()}
                                    >
                                      {nokLookupLoading[guarantorId] ? "..." : "Search"}
                                    </Button>
                                  </div>
                                  
                                  {nokMember && (
                                    <div className="bg-green-50 border border-green-200 rounded p-2 space-y-2">
                                      <div>
                                        <p className="text-sm font-medium">{nokMember.firstName} {nokMember.lastName}</p>
                                        <p className="text-xs text-muted-foreground">
                                          {nokMember.employeeId} • Will cover: KES {amount.toLocaleString()}
                                        </p>
                                      </div>
                                      
                                      {nokEligibility && (
                                        nokEligibility.eligible ? (
                                          <div className="flex items-center gap-1 text-xs text-green-600">
                                            <CheckCircle className="h-3 w-3" />
                                            <span>✓ Eligible as NOK</span>
                                          </div>
                                        ) : (
                                          <div className="flex items-center gap-1 text-xs text-red-600">
                                            <XCircle className="h-3 w-3" />
                                            <span>✗ {nokEligibility.errors?.[0] || "Not eligible"}</span>
                                          </div>
                                        )
                                      )}
                                      
                                      <Button
                                        type="button"
                                        size="sm"
                                        className="w-full"
                                        onClick={() => {
                                          if (nokEligibility?.eligible) {
                                            setNokGuarantorMap({...nokGuarantorMap, [guarantorId]: nokMember.id});
                                            setNokEmployeeIdInput({...nokEmployeeIdInput, [guarantorId]: ""});
                                            setNokLookupResult({...nokLookupResult, [guarantorId]: null});
                                            toast({ title: "Success", description: `${nokMember.firstName} added as NOK for ${guarantor?.firstName}` });
                                          } else {
                                            toast({ title: "Not Eligible", description: "This member cannot be NOK guarantor", variant: "destructive" });
                                          }
                                        }}
                                      >
                                        Add as NOK
                                      </Button>
                                    </div>
                                  )}
                                </div>
                              ) : (
                                <div className="bg-blue-50 border border-blue-200 rounded p-2 flex items-center justify-between">
                                  <div>
                                    <p className="text-sm font-medium">{members.find(m => m.id === nokId)?.firstName} {members.find(m => m.id === nokId)?.lastName}</p>
                                    <p className="text-xs text-muted-foreground">
                                      {members.find(m => m.id === nokId)?.employeeId} • NOK Backup
                                    </p>
                                  </div>
                                  <Button
                                    type="button"
                                    size="sm"
                                    variant="ghost"
                                    onClick={() => {
                                      const newNokMap = {...nokGuarantorMap};
                                      delete newNokMap[guarantorId];
                                      setNokGuarantorMap(newNokMap);
                                    }}
                                  >
                                    Remove
                                  </Button>
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
                <div className="space-y-2">
                  <Label>Purpose</Label>
                  <Textarea 
                    value={form.purpose} 
                    onChange={e => setForm({...form, purpose: e.target.value})} 
                    placeholder="Reason for loan application..."
                  />
                </div>

                {/* Live Eligibility Panel */}
                {preCheck && (
                  <div className="border rounded-md p-3 space-y-2 bg-slate-50 text-sm">
                    <p className="font-medium text-xs text-muted-foreground uppercase tracking-wide">Live Eligibility Check</p>

                    {/* Member */}
                    <div className={`flex items-start gap-2 p-2 rounded ${preCheck.member?.eligible ? "bg-green-50 border border-green-200" : "bg-red-50 border border-red-200"}`}>
                      <span className="mt-0.5">{preCheck.member?.eligible ? "✅" : "❌"}</span>
                      <div className="flex-1">
                        <p className="font-medium">{preCheck.member?.name} <span className="text-xs text-muted-foreground">({preCheck.member?.memberNumber})</span></p>
                        <p className="text-xs text-muted-foreground">
                          Savings: KES {Number(preCheck.member?.savingsBalance || 0).toLocaleString()} | 
                          Shares: KES {Number(preCheck.member?.sharesBalance || 0).toLocaleString()} | 
                          Active loans: {preCheck.member?.activeLoans}
                        </p>
                        {preCheck.member?.errors?.map((e: string, i: number) => (
                          <p key={i} className="text-xs text-red-600">• {e}</p>
                        ))}
                        {preCheck.member?.warnings?.map((w: string, i: number) => (
                          <p key={i} className="text-xs text-amber-600">⚠ {w}</p>
                        ))}
                      </div>
                    </div>

                    {/* Guarantors */}
                    {preCheck.guarantors?.length > 0 && preCheck.guarantors.map((g: any, i: number) => (
                      <div key={i} className={`flex items-start gap-2 p-2 rounded ${g.isEligible ? "bg-green-50 border border-green-200" : "bg-red-50 border border-red-200"}`}>
                        <span className="mt-0.5">{g.isEligible ? "✅" : "❌"}</span>
                        <div className="flex-1">
                          <p className="font-medium">Guarantor: {g.guarantorName}</p>
                          <p className="text-xs text-muted-foreground">
                            Savings: KES {Number(g.savingsBalance || 0).toLocaleString()} | 
                            Available capacity: KES {Number(g.availableGuaranteeCapacity || 0).toLocaleString()} | 
                            Outstanding: KES {Number(g.outstandingBalance || 0).toLocaleString()}
                          </p>
                          {g.errors?.map((e: string, j: number) => (
                            <p key={j} className="text-xs text-red-600">• {e}</p>
                          ))}
                          {g.warnings?.map((w: string, j: number) => (
                            <p key={j} className="text-xs text-amber-600">⚠ {w}</p>
                          ))}
                        </div>
                      </div>
                    ))}

                    {preCheckLoading && <p className="text-xs text-muted-foreground">Checking...</p>}
                  </div>
                )}

                <Button 
                  type="submit" 
                  className="w-full" 
                  disabled={
                    !form.amount || 
                    !form.loanProductId ||
                    (selectedProduct && (parseFloat(form.amount) < selectedProduct.minAmount || parseFloat(form.amount) > selectedProduct.maxAmount)) ||
                    (preCheck && (!preCheck.canProceed || preCheck.allGuarantorsEligible === false))
                  }
                >
                  {!form.amount || !form.loanProductId
                    ? "Fill in all required fields"
                    : selectedProduct && parseFloat(form.amount) < selectedProduct.minAmount
                    ? `Amount below minimum (KES ${selectedProduct.minAmount.toLocaleString()})`
                    : selectedProduct && parseFloat(form.amount) > selectedProduct.maxAmount
                    ? `Amount exceeds maximum (KES ${selectedProduct.maxAmount.toLocaleString()})`
                    : preCheck && !preCheck.canProceed
                    ? "Member Not Eligible — Cannot Submit"
                    : preCheck && preCheck.allGuarantorsEligible === false
                    ? "Ineligible Guarantor(s) — Cannot Submit"
                    : "Submit Application"}
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {!canCreateLoans && !canApproveLoans && !canDisburseLoans && (
        <Alert className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            You have view-only access to loan information.
          </AlertDescription>
        </Alert>
      )}

      {/* Loan Top-Up Review Section - Loan Officer, Credit Committee, and Treasurer */}
      {(role === "LOAN_OFFICER" || role === "CREDIT_COMMITTEE" || role === "TREASURER") && (
        <div className="mb-6">
          <LoanTopUpReviewSection />
        </div>
      )}

      {/* Filters */}
      <Card className="mb-6 border-none shadow-sm">
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input 
                placeholder="Search by loan number, member name..." 
                className="pl-10" 
                value={search} 
                onChange={e => setSearch(e.target.value)} 
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-48"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="PENDING_GUARANTOR_APPROVAL">Pending Guarantor Approval</SelectItem>
                <SelectItem value="PENDING_LOAN_OFFICER_REVIEW">Pending Loan Officer Review</SelectItem>
                <SelectItem value="PENDING_CREDIT_COMMITTEE">Pending Credit Committee</SelectItem>
                <SelectItem value="PENDING_TREASURER">Pending Treasurer</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
                <SelectItem value="DISBURSED">Disbursed</SelectItem>
                <SelectItem value="REPAID">Repaid</SelectItem>
                <SelectItem value="DEFAULTED">Defaulted</SelectItem>
              </SelectContent>
            </Select>
            <Button onClick={handleApplyStatusFilter} size="sm">Apply Filter</Button>
            <Button onClick={handleClearFilters} variant="outline" size="sm">Clear</Button>
          </div>
        </CardContent>
      </Card>

      {/* Loans Table */}
      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Loan No.</TableHead>
                <TableHead>Member</TableHead>
                <TableHead>Product</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">Loading...</TableCell>
                </TableRow>
              ) : filteredLoans.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">No loans found</TableCell>
                </TableRow>
              ) : filteredLoans.map(loan => (
                <TableRow key={loan.id} onClick={(e) => e.stopPropagation()}>
                  <TableCell className="font-mono text-sm">{loan.loanNumber || "—"}</TableCell>
                  <TableCell className="font-medium">
                    {loan.member?.fullName || `${loan.member?.firstName} ${loan.member?.lastName}`}
                  </TableCell>
                  <TableCell>{loan.loanProduct?.name}</TableCell>
                  <TableCell>KES {loan.amount.toLocaleString()}</TableCell>
                  <TableCell>
                    <Badge className={`${loanStatusColors[loan.status]} whitespace-normal sm:whitespace-nowrap text-center leading-tight max-w-[140px] sm:max-w-none`}>
                      <span className="block sm:hidden">
                        {/* Mobile: Shorter labels */}
                        {loan.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Officer Review' :
                         loan.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Guarantor Approval' :
                         loan.status === 'PENDING_GUARANTOR_REPLACEMENT' ? 'Replace Guarantor' :
                         loan.status === 'PENDING_GUARANTOR_REASSIGNMENT' ? 'Reassign Guarantor' :
                         loan.status === 'PENDING_CREDIT_COMMITTEE' ? 'Credit Committee' :
                         loan.status === 'PENDING_TREASURER' ? 'Treasurer' :
                         loan.status.replace(/_/g, " ")}
                      </span>
                      <span className="hidden sm:block">
                        {/* Desktop: Full labels */}
                        {loan.status.replace(/_/g, " ")}
                      </span>
                    </Badge>
                  </TableCell>
                  <TableCell>{new Date(loan.applicationDate).toLocaleDateString()}</TableCell>
                  <TableCell onClick={(e) => e.stopPropagation()}>
                    <div className="flex gap-1">
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          handleEyeIconClick(loan);
                        }} 
                        title="View Details" 
                        type="button"
                        className="text-gray-600 hover:text-gray-900 hover:bg-gray-100"
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          handleViewGuarantors(loan);
                        }} 
                        title="View Guarantors"
                        type="button"
                        className="text-blue-600 hover:bg-blue-50"
                      >
                        <Users className="h-4 w-4 mr-1" />
                        Guarantors
                      </Button>
                      {loan.status === "PENDING" && canApproveLoans && (
                        <>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            onClick={() => validateEligibilityBeforeApproval(loan)}
                            title="Approve"
                            className="text-green-600"
                            type="button"
                          >
                            <CheckCircle className="h-4 w-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            onClick={() => setActionDialog({ loan, action: "reject" })}
                            title="Reject"
                            className="text-red-600"
                            type="button"
                          >
                            <XCircle className="h-4 w-4" />
                          </Button>
                        </>
                      )}
                      {loan.status === "APPROVED" && canDisburseLoans && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={() => setActionDialog({ loan, action: "disburse" })}
                          title="Disburse"
                          className="text-purple-600"
                          type="button"
                        >
                          <DollarSign className="h-4 w-4" />
                        </Button>
                      )}

                      {loan.status === "PENDING_GUARANTOR_REASSIGNMENT" && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={() => handleOpenReassignGuarantorsDialog(loan)}
                          title="Reassign Guarantors"
                          className="text-purple-600"
                          type="button"
                        >
                          <AlertCircle className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Simple Loan Details Dialog - For Treasurer and others */}
      <Dialog open={loanDetailsOpen} onOpenChange={(open) => {
        setLoanDetailsOpen(open);
        if (!open) {
          setSelectedLoanForDetails(null);
        }
      }}>
        <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Loan Details</DialogTitle>
          </DialogHeader>
          {selectedLoanForDetails && (
            <div className="space-y-3">
              {/* Loan Summary */}
              <Card className="p-4">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="min-w-0">
                    <p className="text-xs text-gray-600 mb-1.5">ID</p>
                    <p className="font-medium text-sm">{selectedLoanForDetails.id}</p>
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs text-gray-600 mb-1.5">Member</p>
                    <p className="font-medium text-sm truncate" title={`${selectedLoanForDetails.member?.firstName} ${selectedLoanForDetails.member?.lastName}`}>
                      {selectedLoanForDetails.member?.firstName} {selectedLoanForDetails.member?.lastName}
                    </p>
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs text-gray-600 mb-1.5">Product</p>
                    <p className="font-medium text-sm truncate" title={selectedLoanForDetails.loanProduct?.name}>
                      {selectedLoanForDetails.loanProduct?.name}
                    </p>
                  </div>
                </div>
              </Card>

              {/* Loan Amount Details */}
              <Card className="p-2">
                <p className="font-semibold text-xs mb-1">Amount Details</p>
                <div className="grid grid-cols-3 gap-1 text-xs">
                  <div>
                    <p className="text-xs text-gray-600">Principal</p>
                    <p className="font-bold">KES {selectedLoanForDetails.amount?.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Rate</p>
                    <p className="font-medium">{selectedLoanForDetails.interestRate}%</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Term</p>
                    <p className="font-medium">{selectedLoanForDetails.termMonths} months</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Interest Collected</p>
                    <p className="font-medium text-blue-600">KES {(selectedLoanForDetails.interestCollected !== undefined ? selectedLoanForDetails.interestCollected : selectedLoanForDetails.totalInterest)?.toLocaleString() || "0"}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Outstanding</p>
                    <p className="font-bold text-red-600">KES {selectedLoanForDetails.outstandingBalance?.toLocaleString() || "0"}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Status</p>
                    <Badge className={`${loanStatusColors[selectedLoanForDetails.status]} text-[10px] py-0.5 px-2 whitespace-normal sm:whitespace-nowrap text-center leading-tight inline-block`}>
                      <span className="block sm:hidden">
                        {/* Mobile: Shorter labels */}
                        {selectedLoanForDetails.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Officer Review' :
                         selectedLoanForDetails.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Guarantor' :
                         selectedLoanForDetails.status === 'PENDING_GUARANTOR_REPLACEMENT' ? 'Replace Guarantor' :
                         selectedLoanForDetails.status === 'PENDING_GUARANTOR_REASSIGNMENT' ? 'Reassign' :
                         selectedLoanForDetails.status === 'PENDING_CREDIT_COMMITTEE' ? 'Committee' :
                         selectedLoanForDetails.status === 'PENDING_TREASURER' ? 'Treasurer' :
                         selectedLoanForDetails.status === 'APPROVED' ? 'Approved' :
                         selectedLoanForDetails.status === 'DISBURSED' ? 'Disbursed' :
                         selectedLoanForDetails.status === 'REJECTED' ? 'Rejected' :
                         selectedLoanForDetails.status.replace(/_/g, " ")}
                      </span>
                      <span className="hidden sm:block">
                        {/* Desktop: Full labels */}
                        {selectedLoanForDetails.status.replace(/_/g, " ")}
                      </span>
                    </Badge>
                  </div>
                </div>
              </Card>

              {/* Top-Up History Section - View Only */}
              {(selectedLoanForDetails.status === "DISBURSED" || selectedLoanForDetails.status === "ACTIVE") && topUpHistory.length > 0 && (
                <Card className="p-3 bg-gradient-to-r from-purple-50 to-indigo-50 border-purple-200">
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-semibold text-sm text-purple-900">💰 Loan Top-Up History</p>
                  </div>

                  {/* Top-Up History Table - View Only */}
                  {loadingTopUpHistory ? (
                    <p className="text-xs text-gray-600">Loading top-up history...</p>
                  ) : (
                    <div className="space-y-2">
                      <p className="text-xs font-medium text-purple-800">Top-Up History ({topUpHistory.length})</p>
                      <div className="max-h-48 overflow-y-auto space-y-1">
                        {topUpHistory.map((topup: any, idx: number) => (
                          <div key={idx} className="bg-white p-2 rounded border border-purple-200 text-xs">
                            <div className="flex justify-between items-start mb-1">
                              <div>
                                <p className="font-semibold text-purple-900">KES {topup.topupAmount?.toLocaleString()}</p>
                                <p className="text-gray-600">{new Date(topup.topupDate).toLocaleDateString()}</p>
                              </div>
                              <div className="flex items-center gap-1">
                                <Badge className="bg-purple-100 text-purple-800 text-xs">
                                  Top-Up #{topup.id}
                                </Badge>
                              </div>
                            </div>
                            <div className="grid grid-cols-2 gap-1 text-xs">
                              <div>
                                <span className="text-gray-600">Before:</span>
                                <span className="font-medium ml-1">KES {topup.outstandingBeforeTopup?.toLocaleString()}</span>
                              </div>
                              <div>
                                <span className="text-gray-600">After:</span>
                                <span className="font-medium ml-1">KES {topup.outstandingAfterTopup?.toLocaleString()}</span>
                              </div>
                            </div>
                            {topup.purpose && (
                              <p className="text-gray-700 mt-1">
                                <span className="font-medium">Purpose:</span> {topup.purpose}
                              </p>
                            )}
                            {topup.principalPaidBeforeTopup !== undefined && (
                              <p className="text-green-700 bg-green-50 p-1 rounded mt-1">
                                ✓ Principal paid before top-up: KES {topup.principalPaidBeforeTopup?.toLocaleString()}
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </Card>
              )}

              {/* Repayment Progress Tracker - For Active/Disbursed Loans */}
              {(selectedLoanForDetails.status === "ACTIVE" || selectedLoanForDetails.status === "DISBURSED") && (
                <Card className="p-2 bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200">
                  <p className="font-semibold text-xs mb-2">Repayment Progress</p>
                  <div className="space-y-2">
                    {/* Progress Bar */}
                    <div>
                      <div className="flex justify-between mb-1">
                        <span className="text-xs text-gray-600">Repayment Status</span>
                        <span className="text-xs font-medium">
                          {selectedLoanForDetails.repaymentPercentage !== undefined 
                            ? `${Number(selectedLoanForDetails.repaymentPercentage).toFixed(2)}%`
                            : "0%"}
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-green-500 h-2 rounded-full transition-all"
                          style={{
                            width: selectedLoanForDetails.repaymentPercentage !== undefined 
                              ? `${Math.min(Math.max(0, Number(selectedLoanForDetails.repaymentPercentage)), 100)}%`
                              : "0%"
                          }}
                        />
                      </div>
                    </div>

                    {/* Repayment Details Grid */}
                    <div className="grid grid-cols-5 gap-1 text-xs">
                      <div className="bg-white rounded p-1.5 border border-blue-100">
                        <p className="text-gray-600 text-xs">Principal</p>
                        <p className="font-bold text-blue-600">KES {selectedLoanForDetails.amount?.toLocaleString()}</p>
                      </div>
                      <div className="bg-white rounded p-1.5 border border-orange-100">
                        <p className="text-gray-600 text-xs">Interest Collected</p>
                        <p className="font-bold text-orange-600">KES {selectedLoanForDetails.interestCollected?.toLocaleString() || "0"}</p>
                      </div>
                      <div className="bg-white rounded p-1.5 border border-green-100">
                        <p className="text-gray-600 text-xs">Principal Repaid</p>
                        <p className="font-bold text-green-600">
                          KES {selectedLoanForDetails.principalRepaid?.toLocaleString() || "0"}
                        </p>
                      </div>
                      <div className="bg-white rounded p-1.5 border border-purple-100">
                        <p className="text-gray-600 text-xs">Total Repaid</p>
                        <p className="font-bold text-purple-600">
                          KES {selectedLoanForDetails.totalRepaid?.toLocaleString() || "0"}
                        </p>
                      </div>
                      <div className="bg-white rounded p-1.5 border border-red-100">
                        <p className="text-gray-600 text-xs">Outstanding</p>
                        <p className="font-bold text-red-600">KES {selectedLoanForDetails.outstandingBalance?.toLocaleString() || "0"}</p>
                      </div>
                    </div>

                    {/* Eligibility Recovery Info */}
                    <div className="bg-white rounded p-2 border border-purple-100 text-xs">
                      <p className="text-gray-600 mb-1">💡 Eligibility Impact</p>
                      <p className="text-gray-700">
                        As you repay this loan, your borrowing capacity increases. Each payment reduces your frozen savings and increases your available eligibility.
                      </p>
                    </div>
                  </div>
                </Card>
              )}

              {/* Purpose */}
              {selectedLoanForDetails.purpose && (
                <Card className="p-2">
                  <p className="text-xs text-gray-600 font-semibold">Purpose</p>
                  <p className="text-xs">{selectedLoanForDetails.purpose}</p>
                </Card>
              )}

              {/* Member Eligibility Status - Only show for active loans, not repaid/defaulted */}
              {selectedLoanForDetails.memberEligibilityStatus && 
               selectedLoanForDetails.status !== "REPAID" && 
               selectedLoanForDetails.status !== "DEFAULTED" && (
                <Card className={`p-2 ${selectedLoanForDetails.memberEligibilityStatus === "ELIGIBLE" ? "bg-green-50 border-green-200" : "bg-red-50 border-red-200"}`}>
                  <p className="font-semibold text-xs mb-2">
                    {selectedLoanForDetails.memberEligibilityStatus === "ELIGIBLE" ? "✅ Member Eligible" : "❌ Member Not Eligible"}
                  </p>
                  {selectedLoanForDetails.memberEligibilityErrors && (
                    <div className="text-xs space-y-1">
                      <p className="font-medium text-gray-700">Issues:</p>
                      {selectedLoanForDetails.memberEligibilityErrors.split(";").map((error: string, idx: number) => (
                        <p key={idx} className={selectedLoanForDetails.memberEligibilityStatus === "ELIGIBLE" ? "text-green-700" : "text-red-700"}>
                          • {error.trim()}
                        </p>
                      ))}
                    </div>
                  )}
                </Card>
              )}

              {/* Rejection Reason - if rejected */}
              {selectedLoanForDetails.status === "REJECTED" && selectedLoanForDetails.rejectionReason && (
                <Card className="p-2 border-red-200 bg-red-50">
                  <p className="text-xs text-red-600 font-semibold">Rejection Reason</p>
                  <p className="text-xs text-red-800">{selectedLoanForDetails.rejectionReason}</p>
                </Card>
              )}

              {/* Guarantors */}
              {selectedLoanForDetails.guarantors && selectedLoanForDetails.guarantors.length > 0 && (
                <Card className="p-2">
                  <p className="font-semibold text-xs mb-1">Guarantors ({selectedLoanForDetails.guarantors.length})</p>
                  <div className="space-y-1">
                    {selectedLoanForDetails.guarantors.map((guarantor: any, idx: number) => {
                      const eligibilityStatus = idx === 0
                        ? selectedLoanForDetails.guarantor1EligibilityStatus
                        : idx === 1
                        ? selectedLoanForDetails.guarantor2EligibilityStatus
                        : selectedLoanForDetails.guarantor3EligibilityStatus;
                      const eligibilityErrors = idx === 0
                        ? selectedLoanForDetails.guarantor1EligibilityErrors
                        : idx === 1
                        ? selectedLoanForDetails.guarantor2EligibilityErrors
                        : selectedLoanForDetails.guarantor3EligibilityErrors;
                      const isEligible = eligibilityStatus === "ELIGIBLE";
                      const hasStatus = !!eligibilityStatus;
                      const guaranteeAmount = guarantor.guaranteeAmount || selectedLoanForDetails.amount;
                      const isPartialGuarantee = guarantor.guaranteeAmount && guarantor.guaranteeAmount < selectedLoanForDetails.amount;
                      
                      return (
                        <div key={idx} className={`text-xs p-2 rounded border ${hasStatus ? (isEligible ? "bg-green-50 border-green-200" : "bg-red-50 border-red-200") : "bg-gray-50 border-gray-200"}`}>
                          <div className="flex items-center justify-between mb-1">
                            <div>
                              <p className="font-medium">{guarantor.member?.firstName} {guarantor.member?.lastName}</p>
                              <p className="text-gray-500">{guarantor.member?.memberNumber}</p>
                            </div>
                            {hasStatus && (
                              <span className={`font-semibold ${isEligible ? "text-green-700" : "text-red-700"}`}>
                                {isEligible ? "✅ Eligible" : "❌ Ineligible"}
                              </span>
                            )}
                            {!hasStatus && (
                              <span className="text-gray-400 text-xs">Pending review</span>
                            )}
                          </div>
                          
                          {/* Guarantee Amount */}
                          <div className="flex justify-between items-center mb-1">
                            <span className="text-gray-600">Guarantee Amount:</span>
                            <span className="font-semibold">KES {guaranteeAmount?.toLocaleString()}</span>
                          </div>
                          
                          {/* Partial Guarantee Badge */}
                          {isPartialGuarantee && (
                            <div className="mb-1 inline-block bg-blue-100 text-blue-700 px-2 py-0.5 rounded text-xs font-medium">
                              Partial Guarantee ({Math.round((guarantor.guaranteeAmount / selectedLoanForDetails.amount) * 100)}%)
                            </div>
                          )}
                          
                          {eligibilityErrors && (
                            <p className="text-red-600 mt-1">• {eligibilityErrors}</p>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </Card>
              )}



              {/* Approval/Rejection Section - For Loan Officer, Credit Committee, and Treasurer */}
              {(
                (selectedLoanForDetails.status === "PENDING_LOAN_OFFICER_REVIEW" && role === "LOAN_OFFICER") ||
                (selectedLoanForDetails.status === "PENDING_CREDIT_COMMITTEE" && role === "CREDIT_COMMITTEE") ||
                (selectedLoanForDetails.status === "PENDING_TREASURER" && role === "TREASURER")
              ) && (
                <Card className="p-3 border-blue-200 bg-blue-50">
                  <p className="font-semibold text-xs mb-2">
                    {role === "TREASURER" && selectedLoanForDetails.status === "PENDING_TREASURER" 
                      ? "Final Approval - Confirm Disbursement"
                      : "Approval Decision"}
                  </p>
                  <div className="space-y-2">
                    {/* Treasurer Approval Section - No Interest Input Anymore */}
                    {role === "TREASURER" && selectedLoanForDetails.status === "PENDING_TREASURER" && (
                      <div className="p-2 bg-white rounded border border-blue-300">
                        <p className="text-sm font-semibold mb-2">Loan Summary</p>
                        <div className="text-xs space-y-1">
                          <p>Member: {selectedLoanForDetails.member.firstName} {selectedLoanForDetails.member.lastName}</p>
                          <p>Amount: KES {(selectedLoanForDetails.amount || 0).toLocaleString()}</p>
                          <p>Term: {selectedLoanForDetails.termMonths} months</p>
                          <p className="text-gray-600 mt-2">Interest will be determined during repayments using reducing balance method.</p>
                        </div>
                      </div>
                    )}
                    <div>
                      <Label className="text-xs">Reason/Comments *</Label>
                      <Textarea
                        placeholder={role === "TREASURER" && selectedLoanForDetails.status === "PENDING_TREASURER" 
                          ? "Enter approval notes..." 
                          : "Enter your reason for approval or rejection..."}
                        value={actionNotes}
                        onChange={(e) => setActionNotes(e.target.value)}
                        className="text-xs min-h-20"
                      />
                    </div>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        className="h-8 text-xs flex-1 bg-green-600 hover:bg-green-700"
                        onClick={() => {
                          if (!actionNotes.trim()) {
                            toast({ title: "Required", description: "Please enter approval notes", variant: "destructive" });
                            return;
                          }
                          setActionDialog({ loan: selectedLoanForDetails, action: "approve" });
                        }}
                        disabled={approvalSubmitting}
                      >
                        {approvalSubmitting ? "..." : "Approve"}
                      </Button>
                      <Button
                        size="sm"
                        className="h-8 text-xs flex-1 bg-red-600 hover:bg-red-700"
                        onClick={() => {
                          if (!actionNotes.trim()) {
                            toast({ title: "Required", description: "Please enter a reason for rejection", variant: "destructive" });
                            return;
                          }
                          setActionDialog({ loan: selectedLoanForDetails, action: "reject" });
                        }}
                        disabled={approvalSubmitting}
                      >
                        {approvalSubmitting ? "..." : "Reject"}
                      </Button>
                    </div>
                  </div>
                </Card>
              )}

              <div className="flex justify-end gap-2 pt-1">
                <Button variant="outline" size="sm" className="h-8 text-xs" onClick={() => setLoanDetailsOpen(false)}>
                  Close
                </Button>
                {selectedLoanForDetails.status === "APPROVED" && canDisburseLoans && (
                  <Button 
                    size="sm"
                    className="h-8 text-xs bg-blue-600 hover:bg-blue-700"
                    onClick={() => {
                      setActionDialog({ loan: selectedLoanForDetails, action: "disburse" });
                      setLoanDetailsOpen(false);
                    }}
                  >
                    Disburse
                  </Button>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Approval/Rejection Confirmation Dialog */}
      <Dialog open={actionDialog?.action === "approve" || actionDialog?.action === "reject"} onOpenChange={(open) => {
        if (!open) {
          setActionDialog(null);
          setActionNotes("");
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionDialog?.action === "approve" ? "Confirm Loan Approval" : "Confirm Loan Rejection"}
            </DialogTitle>
          </DialogHeader>
          {(actionDialog?.action === "approve" || actionDialog?.action === "reject") && actionDialog?.loan && (
            <div className="space-y-4">
              <div className={`p-4 rounded-lg ${actionDialog.action === "approve" ? "bg-green-50" : "bg-red-50"}`}>
                <p className="text-sm text-gray-600">Member</p>
                <p className="font-semibold">{actionDialog.loan.member?.firstName} {actionDialog.loan.member?.lastName}</p>
                <p className="text-sm text-gray-600 mt-2">Loan Amount</p>
                <p className="font-bold text-lg">KES {actionDialog.loan.amount?.toLocaleString()}</p>
              </div>
              <div>
                <p className="text-sm font-medium mb-1">Reason/Comments</p>
                <p className="text-sm text-gray-700 p-2 bg-gray-50 rounded">{actionNotes}</p>
              </div>
              <p className="text-sm text-gray-700">
                {actionDialog.action === "approve" 
                  ? "Are you sure you want to approve this loan? The next stage will be notified."
                  : "Are you sure you want to reject this loan? The applicant will be notified."}
              </p>
              <div className="flex gap-2 justify-end">
                <Button variant="outline" onClick={() => setActionDialog(null)}>
                  Cancel
                </Button>
                <Button 
                  onClick={() => {
                    handleAction();
                  }}
                  className={actionDialog.action === "approve" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}
                >
                  {actionDialog.action === "approve" ? "Approve" : "Reject"}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Disburse Confirmation Dialog */}
      <Dialog open={actionDialog?.action === "disburse"} onOpenChange={(open) => {
        if (!open) {
          setActionDialog(null);
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm Loan Disbursement</DialogTitle>
          </DialogHeader>
          {actionDialog?.action === "disburse" && actionDialog?.loan && (
            <div className="space-y-4">
              <div className="p-4 bg-blue-50 rounded-lg">
                <p className="text-sm text-gray-600">Member</p>
                <p className="font-semibold">{actionDialog.loan.member?.firstName} {actionDialog.loan.member?.lastName}</p>
                <p className="text-sm text-gray-600 mt-2">Loan Amount</p>
                <p className="font-bold text-lg">KES {actionDialog.loan.amount?.toLocaleString()}</p>
              </div>
              <p className="text-sm text-gray-700">
                Are you sure you want to disburse this loan? The member account will be credited with the loan amount.
              </p>
              <div className="flex gap-2 justify-end">
                <Button variant="outline" onClick={() => setActionDialog(null)}>
                  Cancel
                </Button>
                <Button 
                  onClick={() => {
                    handleAction();
                  }}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  Confirm Disbursement
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Eligibility Validation Dialog - Opens on Eye Icon Click */}
      <Dialog open={eligibilityValidationOpen} onOpenChange={(open) => {
        setEligibilityValidationOpen(open);
        if (!open) {
          setEligibilityValidation(null);
          setValidatingLoan(null);
        }
      }}>
        <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Loan Eligibility Review</DialogTitle>
          </DialogHeader>
          {eligibilityValidation && (
            <div className="space-y-3">
              {/* Product Validation Alert */}
              {eligibilityValidation.productEnabled === false && (
                <Alert className="border-red-200 bg-red-50 py-2">
                  <AlertCircle className="h-4 w-4 text-red-600" />
                  <AlertDescription className="text-red-800 text-sm">
                    <strong>Product Not Enabled:</strong> {eligibilityValidation.productError}
                  </AlertDescription>
                </Alert>
              )}

              {/* LOAN DECISION SUMMARY */}
              <Card className={`p-3 ${eligibilityValidation.canApprove ? "border-green-300 bg-green-50" : "border-red-300 bg-red-50"}`}>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-semibold">Loan Decision</p>
                    <p className="text-xs text-gray-700">{eligibilityValidation.decisionReason}</p>
                  </div>
                  <Badge className={eligibilityValidation.canApprove ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                    {eligibilityValidation.canApprove ? "✓ APPROVE" : "✗ REJECT"}
                  </Badge>
                </div>
              </Card>

              {/* Loan Summary */}
              <Card className="p-3">
                <p className="font-semibold text-sm mb-2">Loan Info</p>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <p className="text-gray-600">Member</p>
                    <p className="font-medium">{eligibilityValidation.memberNumber}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Product</p>
                    <p className="font-medium">{eligibilityValidation.loanProductName}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Amount</p>
                    <p className="font-medium">{eligibilityValidation.loanAmount}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Purpose</p>
                    <p className="font-medium">{eligibilityValidation.purpose}</p>
                  </div>
                </div>
              </Card>

              {/* Member Eligibility */}
              {eligibilityValidation.memberInfo && (
                <Card className="p-3">
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-semibold text-sm">Member</p>
                    <Badge className={eligibilityValidation.memberInfo.isEligible ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                      {eligibilityValidation.memberInfo.isEligible ? "✓ ELIGIBLE" : "✗ NOT ELIGIBLE"}
                    </Badge>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <p className="text-gray-600">Name</p>
                      <p className="font-medium">{eligibilityValidation.memberInfo.memberName}</p>
                    </div>
                    <div>
                      <p className="text-gray-600">Status</p>
                      <p className="font-medium">{eligibilityValidation.memberInfo.status}</p>
                    </div>
                    <div>
                      <p className="text-gray-600">Savings</p>
                      <p className="font-medium">{eligibilityValidation.memberInfo.savingsBalance}</p>
                    </div>
                    <div>
                      <p className="text-gray-600">Shares</p>
                      <p className="font-medium">{eligibilityValidation.memberInfo.sharesBalance}</p>
                    </div>
                  </div>
                  {eligibilityValidation.memberInfo.errors && eligibilityValidation.memberInfo.errors.length > 0 && (
                    <div className="mt-2 p-2 bg-red-50 rounded text-xs">
                      {eligibilityValidation.memberInfo.errors.map((error: string, i: number) => (
                        <p key={i} className="text-red-800">✗ {error}</p>
                      ))}
                    </div>
                  )}
                </Card>
              )}

              {/* Guarantors Summary */}
              {eligibilityValidation.guarantorCount > 0 && (
                <Card className="p-3">
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-semibold text-sm">Guarantors ({eligibilityValidation.guarantorCount})</p>
                    <Badge className={eligibilityValidation.validationResults?.every((r: any) => r.isEligible) ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                      {eligibilityValidation.validationResults?.every((r: any) => r.isEligible) ? "✓ ALL OK" : "✗ ISSUES"}
                    </Badge>
                  </div>
                  {eligibilityValidation.validationResults && eligibilityValidation.validationResults.length > 0 && (
                    <div className="mt-2 space-y-2">
                      {eligibilityValidation.validationResults.map((result: any, idx: number) => (
                        <div key={idx} className={`text-xs p-2 rounded border ${result.isEligible ? "bg-green-50 border-green-200" : "bg-red-50 border-red-200"}`}>
                          <div className="flex justify-between items-center mb-1">
                            <span className="font-medium">{result.guarantorName}</span>
                            <Badge className={result.isEligible ? "bg-green-100 text-green-800 text-xs" : "bg-red-100 text-red-800 text-xs"}>
                              {result.isEligible ? "✓ ELIGIBLE" : "✗ NOT ELIGIBLE"}
                            </Badge>
                          </div>
                          <div className="grid grid-cols-3 gap-2 text-xs text-gray-600 mb-1">
                            {result.savingsBalance !== undefined && (
                              <div>
                                <span className="text-gray-500">Savings</span>
                                <p className="font-medium text-gray-800">{typeof result.savingsBalance === 'number' ? `KES ${result.savingsBalance.toLocaleString()}` : result.savingsBalance}</p>
                              </div>
                            )}
                            {result.sharesBalance !== undefined && (
                              <div>
                                <span className="text-gray-500">Shares (not used)</span>
                                <p className="font-medium text-gray-800">{typeof result.sharesBalance === 'number' ? `KES ${result.sharesBalance.toLocaleString()}` : result.sharesBalance}</p>
                              </div>
                            )}
                            {result.outstandingBalance !== undefined && (
                              <div>
                                <span className="text-gray-500">Outstanding</span>
                                <p className="font-medium text-gray-800">{typeof result.outstandingBalance === 'number' ? `KES ${result.outstandingBalance.toLocaleString()}` : result.outstandingBalance}</p>
                              </div>
                            )}
                          </div>
                          {result.errors && result.errors.length > 0 && (
                            <div className="mt-1 space-y-0.5">
                              {result.errors.map((err: string, i: number) => (
                                <p key={i} className="text-red-700">✗ {err}</p>
                              ))}
                            </div>
                          )}
                          {result.warnings && result.warnings.length > 0 && (
                            <div className="mt-1 space-y-0.5">
                              {result.warnings.map((warn: string, i: number) => (
                                <p key={i} className="text-yellow-700">⚠ {warn}</p>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" size="sm" onClick={() => setEligibilityValidationOpen(false)}>
                  Close
                </Button>
                <Button 
                  variant="destructive" 
                  size="sm"
                  onClick={() => {
                    setApprovalAction("reject");
                  }}
                >
                  Reject
                </Button>
                <Button 
                  size="sm"
                  onClick={() => {
                    setApprovalAction("approve");
                  }}
                  disabled={!eligibilityValidation.canApprove}
                  className={eligibilityValidation.canApprove ? "bg-green-600 hover:bg-green-700" : ""}
                >
                  Approve
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Approval Reason Dialog */}
      <Dialog open={!!approvalAction} onOpenChange={(open) => {
        if (!open) {
          setApprovalAction(null);
          setApprovalReason("");
        }
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {approvalAction === "approve" ? "Approve Loan" : "Reject Loan"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>{approvalAction === "reject" ? "Rejection Reason *" : "Comments (Optional)"}</Label>
              <Textarea 
                value={approvalReason} 
                onChange={e => setApprovalReason(e.target.value)} 
                placeholder={approvalAction === "reject" ? "Reason for rejection..." : "Add comments..."}
                required={approvalAction === "reject"}
              />
            </div>
            <div className="flex gap-2 justify-end">
              <Button variant="outline" onClick={() => {
                setApprovalAction(null);
                setApprovalReason("");
              }}>
                Cancel
              </Button>
              <Button onClick={async () => {
                if (approvalAction === "reject" && !approvalReason.trim()) {
                  toast({ title: "Error", description: "Rejection reason is required", variant: "destructive" });
                  return;
                }
                if (approvalSubmitting) return;
                setApprovalSubmitting(true);
                try {
                  let url = "";
                  let body: any = {};

                  if (approvalAction === "approve") {
                    url = `${API_BASE_URL}/loans/approve`;
                    body = {
                      loanId: validatingLoan!.id,
                      approved: true,
                      comments: approvalReason || "Approved",
                    };
                  } else if (approvalAction === "reject") {
                    url = `${API_BASE_URL}/loans/approve`;
                    body = {
                      loanId: validatingLoan!.id,
                      approved: false,
                      comments: approvalReason || "Rejected",
                    };
                  }

                  const response = await fetch(url, {
                    method: "POST",
                    headers: {
                      "Content-Type": "application/json",
                      "Authorization": `Bearer ${session?.token}`,
                    },
                    body: JSON.stringify(body),
                  });

                  if (response.ok) {
                    toast({ title: "Success", description: `Loan ${approvalAction}ed successfully` });
                    setApprovalAction(null);
                    setApprovalReason("");
                    setEligibilityValidationOpen(false);
                    setValidatingLoan(null);
                    setEligibilityValidation(null);
                    fetchLoans();
                  } else {
                    const error = await response.json();
                    toast({ title: "Error", description: error.message || `Failed to ${approvalAction} loan`, variant: "destructive" });
                  }
                } catch (error) {
                  toast({ title: "Error", description: "Failed to process request", variant: "destructive" });
                } finally {
                  setApprovalSubmitting(false);
                }
              }} disabled={approvalSubmitting} className={approvalAction === "approve" ? "bg-green-600 hover:bg-green-700" : ""}>
                {approvalSubmitting ? "Processing..." : approvalAction === "approve" ? "Confirm Approval" : "Confirm Rejection"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Guarantor Details Modal */}
      {selectedLoanForGuarantors && (
        <GuarantorDetailsModal
          isOpen={guarantorModalOpen}
          onClose={() => setGuarantorModalOpen(false)}
          guarantors={selectedLoanGuarantors}
          loanAmount={selectedLoanForGuarantors.amount}
        />
      )}

      {/* Edit Loan Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={(open) => {
        setEditDialogOpen(open);
        if (!open) {
          setLoanToEdit(null);
          setCurrentEditGuarantors([]);
          setRemovedGuarantorIds([]);
          setEditedGuarantorAmounts({});
          setGuarantorInputValues({}); // Reset input values
          setNewGuarantorsForEdit([]);
          setEditForm({
            disbursementDate: "",
            outstandingBalance: "",
            termMonths: "",
            guarantorshipType: "",
            guarantors: []
          });
        }
      }}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Edit Loan</DialogTitle>
            <DialogDescription>
              Edit low-risk loan fields (status, dates, interest, outstanding, purpose). No guarantor changes.
            </DialogDescription>
          </DialogHeader>
          {loanToEdit && (
            <form onSubmit={handleEditLoan} className="space-y-4">
              <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm">
                <p className="font-medium text-blue-900">Loan: {loanToEdit.loanNumber}</p>
                <p className="text-xs text-blue-800">{loanToEdit.member?.firstName} {loanToEdit.member?.lastName} — {loanToEdit.loanProduct?.name}</p>
                <p className="text-xs text-blue-800">Principal: KES {loanToEdit.amount.toLocaleString()} | Outstanding: KES {loanToEdit.outstandingBalance?.toLocaleString() || '0'} | Status: {loanToEdit.status}</p>
              </div>

              <div className="space-y-2">
                <Label>Disbursement Date (Optional)</Label>
                <Input
                  type="date"
                  value={editForm.disbursementDate}
                  onChange={(e) => setEditForm({...editForm, disbursementDate: e.target.value})}
                  className="text-sm"
                />
                <p className="text-xs text-muted-foreground">Leave blank to skip updating</p>
              </div>

              <div className="space-y-2">
                <Label>Outstanding Balance (Optional)</Label>
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  max={loanToEdit.amount}
                  value={editForm.outstandingBalance}
                  onChange={(e) => setEditForm({...editForm, outstandingBalance: e.target.value})}
                  placeholder="0.00"
                  className="text-sm"
                />
                <p className="text-xs text-muted-foreground">Max: KES {loanToEdit.amount.toLocaleString()} | Leave blank to skip updating</p>
              </div>

              <div className="space-y-2">
                <Label>Term Months (Optional)</Label>
                <Input
                  type="number"
                  min="1"
                  value={editForm.termMonths}
                  onChange={(e) => setEditForm({...editForm, termMonths: e.target.value})}
                  placeholder="e.g., 12"
                  className="text-sm"
                />
                <p className="text-xs text-muted-foreground">Leave blank to skip updating</p>
              </div>

              {/* Guarantor Management Section - Treasurer Workflow */}
              <div className="border-t pt-3 space-y-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="text-sm font-medium mb-2">Guarantor Management</p>
                    <p className="text-xs text-gray-600">
                      Outstanding Balance: <span className="font-semibold">KES {(parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount).toLocaleString()}</span>
                    </p>
                    <p className="text-xs text-gray-500 mt-1">Remove guarantors • Edit pledge amounts • Add new guarantors</p>
                  </div>
                  {/* Total Coverage Indicator */}
                  {(() => {
                    const outstanding = parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount;
                    
                    // Calculate total from current guarantors (not removed, with edited amounts)
                    const currentTotal = currentEditGuarantors
                      .filter((g: any) => !removedGuarantorIds.includes(g.guarantorId))
                      .reduce((sum: number, g: any) => {
                        const amount = editedGuarantorAmounts[g.guarantorId] !== undefined 
                          ? editedGuarantorAmounts[g.guarantorId] 
                          : g.guaranteeAmount || 0;
                        return sum + amount;
                      }, 0);
                    
                    // Calculate total from new guarantors
                    const newTotal = newGuarantorsForEdit
                      .filter(g => g.employeeId && g.pledgeAmount > 0)
                      .reduce((sum, g) => sum + g.pledgeAmount, 0);
                    
                    const totalPledged = currentTotal + newTotal;
                    
                    // Special case: If outstanding is 0, loan is fully paid - no guarantors needed
                    if (outstanding === 0 || outstanding === null || isNaN(outstanding)) {
                      if (currentEditGuarantors.length > 0 || newGuarantorsForEdit.length > 0) {
                        return (
                          <div className="text-right">
                            <p className="text-xs text-gray-600">Total Coverage</p>
                            <p className="text-sm font-bold text-blue-600">
                              N/A (Fully Paid)
                            </p>
                            <p className="text-xs text-blue-600">Remove all guarantors</p>
                          </div>
                        );
                      }
                      return null;
                    }
                    
                    const percentage = (totalPledged / outstanding) * 100;
                    const isOver100 = percentage > 100;
                    
                    if (currentEditGuarantors.length > 0 || newGuarantorsForEdit.length > 0) {
                      return (
                        <div className="text-right">
                          <p className="text-xs text-gray-600">Total Coverage</p>
                          <p className={`text-sm font-bold ${isOver100 ? 'text-red-600' : 'text-green-700'}`}>
                            {percentage.toFixed(1)}%
                          </p>
                          {isOver100 && (
                            <p className="text-xs text-red-600 font-medium">Exceeds 100%!</p>
                          )}
                        </div>
                      );
                    }
                    return null;
                  })()}
                </div>

                {/* Warning Alert for Over 100% OR Fully Paid */}
                {(() => {
                  const outstanding = parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount;
                  const currentTotal = currentEditGuarantors
                    .filter((g: any) => !removedGuarantorIds.includes(g.guarantorId))
                    .reduce((sum: number, g: any) => {
                      const amount = editedGuarantorAmounts[g.guarantorId] !== undefined 
                        ? editedGuarantorAmounts[g.guarantorId] 
                        : g.guaranteeAmount || 0;
                      return sum + amount;
                    }, 0);
                  const newTotal = newGuarantorsForEdit
                    .filter(g => g.employeeId && g.pledgeAmount > 0)
                    .reduce((sum, g) => sum + g.pledgeAmount, 0);
                  const totalPledged = currentTotal + newTotal;
                  
                  // Special case: Outstanding is 0 (fully paid)
                  if (outstanding === 0 || outstanding === null || isNaN(outstanding)) {
                    if (totalPledged > 0) {
                      return (
                        <Alert className="bg-blue-50 border-blue-200 py-2">
                          <AlertCircle className="h-4 w-4 text-blue-600" />
                          <AlertDescription className="text-xs text-blue-800">
                            <strong>Loan Fully Paid:</strong> Outstanding balance is KES 0. 
                            You can remove all guarantors (KES {totalPledged.toLocaleString()} currently pledged) - no coverage needed for paid loans.
                          </AlertDescription>
                        </Alert>
                      );
                    }
                    return null;
                  }

                  const percentage = (totalPledged / outstanding) * 100;

                  if (percentage > 100) {
                    return (
                      <Alert className="bg-red-50 border-red-200 py-2">
                        <AlertCircle className="h-4 w-4 text-red-600" />
                        <AlertDescription className="text-xs text-red-800">
                          <strong>Warning:</strong> Total guarantor coverage is {percentage.toFixed(1)}% (exceeds 100%). 
                          Total pledged: KES {totalPledged.toLocaleString()} of KES {outstanding.toLocaleString()} needed.
                        </AlertDescription>
                      </Alert>
                    );
                  }
                  return null;
                })()}

                {guarantorsLoading ? (
                  <div className="border rounded-md p-3 bg-gray-50 text-center">
                    <p className="text-xs text-gray-600">Loading guarantors...</p>
                  </div>
                ) : (
                  <>
                    {/* Section 1: Current Guarantors (Editable) */}
                    {currentEditGuarantors.length > 0 && (
                      <div className="border rounded-md p-3 space-y-2 bg-yellow-50">
                        <p className="text-sm font-medium text-yellow-900">Current Guarantors</p>
                        <div className="space-y-2 max-h-48 overflow-y-auto">
                          {currentEditGuarantors.map((guarantor: any) => {
                            // CRITICAL: Use guarantor.guarantorId (from API) - each guarantor is uniquely identified
                            const guarantorId = guarantor.guarantorId;
                            const isRemoved = removedGuarantorIds.includes(guarantorId);
                            const editedAmount = editedGuarantorAmounts[guarantorId];
                            const displayAmount = editedAmount !== undefined ? editedAmount : guarantor.guaranteeAmount;
                            
                            return (
                              <div 
                                key={`current-guarantor-${guarantorId}`}
                                data-guarantor-id={guarantorId}
                                className={`p-2 rounded border text-xs transition-all ${
                                  isRemoved 
                                    ? "bg-red-100 border-red-300 opacity-60" 
                                    : "bg-white border-yellow-300"
                                }`}
                              >
                                <div className="flex items-start justify-between gap-2 mb-2">
                                  <div className="flex-1">
                                    <p className="font-semibold">{guarantor.firstName} {guarantor.lastName}</p>
                                    <p className="text-gray-600">{guarantor.memberNumber || guarantor.employeeId}</p>
                                  </div>
                                  <Button
                                    type="button"
                                    variant={isRemoved ? "ghost" : "outline"}
                                    size="sm"
                                    onClick={(e) => {
                                      e.preventDefault();
                                      e.stopPropagation();
                                      if (isRemoved) {
                                        // Restore guarantor
                                        setRemovedGuarantorIds(removedGuarantorIds.filter(id => id !== guarantorId));
                                      } else {
                                        // Remove guarantor
                                        setRemovedGuarantorIds([...removedGuarantorIds, guarantorId]);
                                      }
                                    }}
                                    className={`text-xs h-8 whitespace-nowrap ${isRemoved ? "text-gray-600" : "text-red-600 border-red-300"}`}
                                  >
                                    {isRemoved ? "↻ Restore" : "✕ Remove"}
                                  </Button>
                                </div>

                                {!isRemoved && (
                                  <div className="space-y-1">
                                    <div className="flex gap-2 items-end">
                                      <div className="flex-1">
                                        <Label className="text-xs font-medium">Pledge Amount (KES)</Label>
                                        <Input
                                          key={`input-${guarantorId}`}
                                          type="number"
                                          step="0.01"
                                          min="0"
                                          // Use inputValues for controlled input to prevent syncing
                                          value={guarantorInputValues[guarantorId] !== undefined 
                                            ? guarantorInputValues[guarantorId]
                                            : (editedGuarantorAmounts[guarantorId] || guarantor.guaranteeAmount || 0)
                                          }
                                          onChange={(e) => {
                                            // Update the input display value first (this is separate from editedGuarantorAmounts)
                                            const inputValue = e.target.value;
                                            setGuarantorInputValues(prev => ({
                                              ...prev,
                                              [guarantorId]: inputValue
                                            }));
                                            
                                            // Also update editedGuarantorAmounts for submission
                                            if (inputValue === '' || inputValue === '0') {
                                              setEditedGuarantorAmounts(prev => {
                                                const updated = { ...prev };
                                                delete updated[guarantorId];
                                                return updated;
                                              });
                                            } else {
                                              const numValue = parseFloat(inputValue);
                                              if (!isNaN(numValue) && numValue > 0) {
                                                setEditedGuarantorAmounts(prev => ({
                                                  ...prev,
                                                  [guarantorId]: numValue
                                                }));
                                              }
                                            }
                                          }}
                                          onBlur={(e) => {
                                            // On blur, validate and clean up input value
                                            const inputValue = e.target.value.trim();
                                            if (inputValue === '' || inputValue === '0' || isNaN(parseFloat(inputValue))) {
                                              // Clear both if empty
                                              setGuarantorInputValues(prev => {
                                                const updated = { ...prev };
                                                delete updated[guarantorId];
                                                return updated;
                                              });
                                              setEditedGuarantorAmounts(prev => {
                                                const updated = { ...prev };
                                                delete updated[guarantorId];
                                                return updated;
                                              });
                                            }
                                          }}
                                          placeholder="0.00"
                                          className="text-xs h-8"
                                        />
                                      </div>
                                    </div>
                                    <p className="text-xs text-blue-600 bg-blue-50 p-1 rounded border border-blue-200">
                                      <span className="font-semibold">Frozen (proportional):</span> {(() => {
                                        try {
                                          const outstandingForCalc = parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount;
                                          const proportion = loanToEdit.amount > 0 ? outstandingForCalc / loanToEdit.amount : 1;
                                          // Use displayAmount which is specific to this guarantor
                                          const frozen = (displayAmount * proportion).toFixed(2);
                                          return `KES ${parseFloat(frozen).toLocaleString()}`;
                                        } catch (err) {
                                          return 'KES 0.00';
                                        }
                                      })()}
                                    </p>
                                  </div>
                                )}

                                {isRemoved && (
                                  <p className="text-xs text-red-600 italic">✗ This guarantor will be removed and unfrozen</p>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Section 2: Add New Guarantors */}
                    <div className="border rounded-md p-3 space-y-2 bg-green-50">
                      <p className="text-sm font-medium text-green-900">Add New Guarantors</p>
                      {newGuarantorsForEdit.length > 0 && (
                        <div className="space-y-2 mb-2">
                          {newGuarantorsForEdit.map((g, i) => {
                            const outstandingForCalc = parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount;
                            const proportion = loanToEdit.amount > 0 ? outstandingForCalc / loanToEdit.amount : 1;
                            const frozen = (g.pledgeAmount * proportion).toFixed(2);
                            return (
                              <div key={g.id} className="flex flex-col gap-2 bg-white p-2 rounded border border-green-300 text-sm">
                                <div className="flex gap-2 items-end">
                                  <div className="flex-1 space-y-1">
                                    <Label className="text-xs font-medium">Member Number/Employee ID</Label>
                                    <Input
                                      placeholder="e.g., EMP001 or MEM12345"
                                      value={g.employeeId}
                                      onChange={(e) => {
                                        const updated = [...newGuarantorsForEdit];
                                        updated[i].employeeId = e.target.value;
                                        setNewGuarantorsForEdit(updated);
                                      }}
                                      onBlur={(e) => {
                                        const value = e.target.value.trim();
                                        if (!value) return;
                                        
                                        // Check if this employeeId is in current guarantors (not removed)
                                        const isDuplicateInCurrent = currentEditGuarantors.some((g: any) => 
                                          !removedGuarantorIds.includes(g.guarantorId) && 
                                          (g.memberNumber === value || g.employeeId === value)
                                        );
                                        
                                        // Check if this employeeId is in new guarantors (excluding current one)
                                        const isDuplicateInNew = newGuarantorsForEdit.some((guarantor, idx) => 
                                          idx !== i && guarantor.employeeId === value
                                        );
                                        
                                        if (isDuplicateInCurrent || isDuplicateInNew) {
                                          toast({ 
                                            title: "Duplicate Guarantor", 
                                            description: "This member is already a guarantor for this loan. Please choose a different member.",
                                            variant: "destructive" 
                                          });
                                          // Clear the duplicate entry
                                          const updated = [...newGuarantorsForEdit];
                                          updated[i].employeeId = "";
                                          setNewGuarantorsForEdit(updated);
                                        }
                                      }}
                                      className="text-xs h-8"
                                    />
                                  </div>
                                  <div className="w-32 space-y-1">
                                    <Label className="text-xs font-medium">Pledge Amount</Label>
                                    <Input
                                      type="number"
                                      step="0.01"
                                      min="0"
                                      placeholder="0.00"
                                      value={g.pledgeAmount}
                                      onChange={(e) => {
                                        const updated = [...newGuarantorsForEdit];
                                        updated[i].pledgeAmount = parseFloat(e.target.value) || 0;
                                        setNewGuarantorsForEdit(updated);
                                      }}
                                      className="text-xs h-8"
                                    />
                                  </div>
                                  <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => {
                                      const updated = newGuarantorsForEdit.filter((_, idx) => idx !== i);
                                      setNewGuarantorsForEdit(updated);
                                    }}
                                    className="text-red-600 h-8 w-8 mb-1"
                                  >
                                    ✕
                                  </Button>
                                </div>
                                <p className="text-xs text-blue-600 bg-blue-50 p-1 rounded">
                                  Will freeze: KES {frozen}
                                </p>
                              </div>
                            );
                          })}
                        </div>
                      )}
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          // Check if already at 100% or more
                          const outstanding = parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount;
                          
                          // Calculate current total
                          const currentTotal = currentEditGuarantors
                            .filter((g: any) => !removedGuarantorIds.includes(g.guarantorId))
                            .reduce((sum: number, g: any) => {
                              const amount = editedGuarantorAmounts[g.guarantorId] !== undefined 
                                ? editedGuarantorAmounts[g.guarantorId] 
                                : g.guaranteeAmount || 0;
                              return sum + amount;
                            }, 0);
                          
                          // Calculate new guarantors total
                          const newTotal = newGuarantorsForEdit
                            .filter(g => g.employeeId && g.pledgeAmount > 0)
                            .reduce((sum, g) => sum + g.pledgeAmount, 0);
                          
                          const totalPledged = currentTotal + newTotal;
                          const percentage = (totalPledged / outstanding) * 100;
                          
                          if (percentage >= 100) {
                            const confirmed = window.confirm(
                              `Current guarantor coverage is already ${percentage.toFixed(1)}%.\n\n` +
                              `Total pledged: KES ${totalPledged.toLocaleString()}\n` +
                              `Outstanding: KES ${outstanding.toLocaleString()}\n\n` +
                              `Do you still want to add another guarantor?`
                            );
                            if (!confirmed) return;
                          }
                          
                          setNewGuarantorsForEdit([...newGuarantorsForEdit, { id: `new-${Date.now()}-${Math.random()}`, employeeId: "", pledgeAmount: 0 }]);
                        }}
                        className="w-full text-xs"
                      >
                        + Add New Guarantor
                      </Button>
                    </div>

                    {/* Real-time Balance Check */}
                    {(removedGuarantorIds.length > 0 || Object.keys(editedGuarantorAmounts).length > 0 || newGuarantorsForEdit.length > 0) && (
                      <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-xs space-y-2">
                        <p className="font-semibold text-blue-900">Guarantee Balance Summary</p>
                        {(() => {
                          // Calculate kept guarantors total (all current minus removed)
                          const keptTotal = currentEditGuarantors
                            .filter((g: any) => !removedGuarantorIds.includes(g.guarantorId))
                            .reduce((sum, g: any) => {
                              const amount = editedGuarantorAmounts[g.guarantorId] !== undefined 
                                ? editedGuarantorAmounts[g.guarantorId]
                                : g.guaranteeAmount;
                              return sum + amount;
                            }, 0);
                          
                          // Calculate new guarantors total
                          const newTotal = newGuarantorsForEdit.reduce((sum, g) => sum + g.pledgeAmount, 0);
                          
                          // Total guarantees
                          const totalGuarantees = keptTotal + newTotal;
                          
                          // Principal amount (not outstanding)
                          const outstandingForCalc = loanToEdit.amount;
                          
                          // Difference
                          const difference = totalGuarantees - outstandingForCalc;
                          const isValid = difference === 0;
                          
                          return (
                            <>
                              <div className="grid grid-cols-2 gap-2 text-xs">
                                <p className="text-gray-700">Kept guarantors:</p>
                                <p className="font-semibold text-right">KES {keptTotal.toLocaleString()}</p>
                                <p className="text-gray-700">New guarantors:</p>
                                <p className="font-semibold text-right">KES {newTotal.toLocaleString()}</p>
                                <p className="text-gray-700 font-semibold">Total:</p>
                                <p className="font-bold text-right">KES {totalGuarantees.toLocaleString()}</p>
                              </div>
                              <div className={`p-2 rounded font-semibold text-center ${
                                isValid 
                                  ? "bg-green-100 text-green-700 border border-green-300" 
                                  : difference > 0 
                                  ? "bg-orange-100 text-orange-700 border border-orange-300" 
                                  : "bg-red-100 text-red-700 border border-red-300"
                              }`}>
                                {isValid 
                                  ? "✓ Guarantees match outstanding balance" 
                                  : difference > 0 
                                  ? `⚠ Excess: +KES ${difference.toLocaleString()}` 
                                  : `✕ Shortfall: -KES ${Math.abs(difference).toLocaleString()}`
                                }
                              </div>
                              {!isValid && (
                                <p className="text-gray-700 text-[11px]">
                                  Outstanding: KES {outstandingForCalc.toLocaleString()}
                                </p>
                              )}
                            </>
                          );
                        })()}
                      </div>
                    )}
                  </>
                )}
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setEditDialogOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={editSubmitting}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  {editSubmitting ? "Saving..." : "Save Changes"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* Reassign Guarantors Dialog */}
      <Dialog open={reassignDialogOpen} onOpenChange={(open) => {
        setReassignDialogOpen(open);
        if (!open) {
          setLoanForReassign(null);
          setReassignData(null);
          setNewGuarantors([]);
        }
      }}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Reassign Guarantors</DialogTitle>
          </DialogHeader>
          {loanForReassign && reassignData && !reassignLoading && (
            <form onSubmit={handleSubmitReassignGuarantors} className="space-y-4">
              {/* Loan & Member Info */}
              <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm space-y-1">
                <p className="font-medium text-blue-900">Loan: {loanForReassign.loanNumber}</p>
                <p className="text-xs text-blue-800">{loanForReassign.member?.firstName} {loanForReassign.member?.lastName}</p>
                <p className="text-xs text-blue-800">Amount: KES {loanForReassign.amount.toLocaleString()} | Outstanding: KES {loanForReassign.outstandingBalance?.toLocaleString() || '0'}</p>
              </div>

              {/* Member Eligibility */}
              <div className="border rounded-md p-3 space-y-2">
                <p className="text-sm font-medium">Member Eligibility</p>
                <div className="text-xs space-y-1 bg-gray-50 p-2 rounded">
                  <p className="text-blue-900">Savings: <span className="font-semibold">KES {(reassignData.memberInfo.savingsBalance || 0).toLocaleString()}</span></p>
                  <p className="text-blue-900">Shares: <span className="font-semibold">KES {(reassignData.memberInfo.sharesBalance || 0).toLocaleString()}</span></p>
                  <p className="text-blue-900">Active Loans: <span className="font-semibold">{reassignData.memberInfo.activeLoans || 0}</span></p>
                  {reassignData.memberInfo.errors?.length > 0 && (
                    <div className="text-red-600 mt-1">
                      {reassignData.memberInfo.errors.map((err: string, i: number) => <p key={i}>⚠ {err}</p>)}
                    </div>
                  )}
                </div>
              </div>

              {/* Current Guarantors */}
              <div className="border rounded-md p-3 space-y-2">
                <p className="text-sm font-medium">Current Guarantors (Total: KES {reassignData.totalCurrentGuarantee?.toLocaleString() || '0'})</p>
                <div className="space-y-1 max-h-32 overflow-y-auto">
                  {reassignData.currentGuarantors?.map((g: any, i: number) => (
                    <div key={i} className="text-xs bg-yellow-50 p-2 rounded border border-yellow-200">
                      <p className="font-semibold">{g.firstName} {g.lastName} ({g.memberNumber})</p>
                      <p className="text-gray-700">Guarantee: KES {g.guaranteeAmount?.toLocaleString() || '0'} | Pledge: KES {g.pledgeAmount?.toLocaleString() || '0'} | Status: {g.status}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* New Guarantor Selection */}
              <div className="border rounded-md p-3 space-y-2">
                <p className="text-sm font-medium">Select New Guarantors</p>
                <div className="space-y-2">
                  {newGuarantors.length > 0 && (
                    <div className="space-y-2">
                      {newGuarantors.map((ng, i) => {
                        const member = reassignData.availableMembers?.find((m: any) => m.memberId === ng.memberId);
                        return (
                          <div key={i} className="flex gap-2 items-center bg-green-50 p-2 rounded border border-green-200 text-sm">
                            <div className="flex-1">
                              <p className="font-semibold">{member?.firstName} {member?.lastName}</p>
                              <p className="text-xs text-gray-600">
                                Employee ID: {member?.employeeId || member?.memberNumber} | Available Savings: KES {member?.availableSavings?.toLocaleString() || '0'}
                              </p>
                            </div>
                            <Input
                              type="number"
                              min="0"
                              placeholder="Guarantee"
                              value={ng.guaranteeAmount || ''}
                              onChange={(e) => {
                                const updated = [...newGuarantors];
                                updated[i].guaranteeAmount = parseFloat(e.target.value) || 0;
                                setNewGuarantors(updated);
                              }}
                              className="w-24 text-xs"
                            />
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => setNewGuarantors(newGuarantors.filter((_, idx) => idx !== i))}
                              className="text-red-600 h-7 w-7"
                            >
                              ×
                            </Button>
                          </div>
                        );
                      })}
                    </div>
                  )}
                  <Select onValueChange={(memberId) => {
                    const newMemberId = parseInt(memberId);
                    if (!newGuarantors.find(g => g.memberId === newMemberId)) {
                      setNewGuarantors([...newGuarantors, { memberId: newMemberId, guaranteeAmount: 0 }]);
                    }
                  }}>
                    <SelectTrigger className="text-sm">
                      <SelectValue placeholder="Add member as guarantor" />
                    </SelectTrigger>
                    <SelectContent>
                      {reassignData.availableMembers?.map((m: any) => (
                        <SelectItem key={m.memberId} value={m.memberId.toString()}>
                          {m.firstName} {m.lastName} ({m.employeeId || m.memberNumber}) - Available: KES {m.availableSavings?.toLocaleString() || '0'}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setReassignDialogOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={reassignSubmitting || newGuarantors.length === 0}
                  className="bg-amber-600 hover:bg-amber-700"
                >
                  {reassignSubmitting ? "Reassigning..." : "Reassign Guarantors"}
                </Button>
              </div>
            </form>
          )}
          {reassignLoading && (
            <div className="flex items-center justify-center py-8">
              <p className="text-sm text-muted-foreground">Loading guarantor data...</p>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Loan Dialog */}
      <Dialog open={deleteLoanDialog} onOpenChange={(open) => {
        setDeleteLoanDialog(open);
        if (!open) {
          setLoanToDelete(null);
          setDeleteReason("");
        }
      }}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-red-600">⚠️ Delete Loan</DialogTitle>
            <DialogDescription>
              Permanently delete this loan and all associated data including guarantors and repayment history.
            </DialogDescription>
          </DialogHeader>
          {loanToDelete && (
            <div className="space-y-4">
              <div className="bg-red-50 border border-red-200 rounded-md p-3">
                <p className="text-sm font-medium text-red-900">Loan: {loanToDelete.loanNumber}</p>
                <p className="text-xs text-red-800">{loanToDelete.member?.firstName} {loanToDelete.member?.lastName}</p>
                <p className="text-xs text-red-800">Amount: KES {loanToDelete.amount.toLocaleString()}</p>
                <p className="text-xs text-red-800">Status: {loanToDelete.status}</p>
              </div>
              <Alert className="bg-amber-50 border-amber-200">
                <AlertCircle className="h-4 w-4 text-amber-600" />
                <AlertDescription className="text-xs text-amber-800">
                  <strong>Warning:</strong> This action cannot be undone. All loan data, guarantors, and repayment history will be permanently deleted.
                </AlertDescription>
              </Alert>
              <div>
                <Label className="text-sm font-medium">Reason for Deletion *</Label>
                <Textarea
                  placeholder="Provide a detailed reason for deleting this loan..."
                  value={deleteReason}
                  onChange={(e) => setDeleteReason(e.target.value)}
                  className="mt-2 text-sm"
                  rows={4}
                />
              </div>
              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setDeleteLoanDialog(false)}
                >
                  Cancel
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={handleDeleteLoan}
                  disabled={deleteSubmitting || !deleteReason.trim()}
                  className="bg-red-600 hover:bg-red-700"
                >
                  {deleteSubmitting ? "Deleting..." : "Confirm Delete"}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Top-Up Dialog */}
      <Dialog open={topUpDialogOpen} onOpenChange={(open) => {
        setTopUpDialogOpen(open);
        if (!open) {
          setTopUpAmount("");
          setTopUpPurpose("");
          setTopUpGuarantors([]);
          setTopUpPreview(null);
        }
      }}>
        <DialogContent className="max-w-lg max-h-[85vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Add Loan Top-Up</DialogTitle>
            <DialogDescription>
              Add additional funds to an existing loan while preserving payment history.
            </DialogDescription>
          </DialogHeader>
          {selectedLoanForDetails && (
            <form onSubmit={handleTopUpSubmit} className="space-y-4">
              <div className="bg-purple-50 border border-purple-200 rounded-md p-3 text-sm">
                <p className="font-medium text-purple-900">Loan: {selectedLoanForDetails.loanNumber}</p>
                <p className="text-xs text-purple-800">{selectedLoanForDetails.member?.firstName} {selectedLoanForDetails.member?.lastName}</p>
                <p className="text-xs text-purple-800">Original Principal: KES {selectedLoanForDetails.amount.toLocaleString()}</p>
                <p className="text-xs text-purple-800">Current Outstanding: KES {selectedLoanForDetails.outstandingBalance?.toLocaleString() || '0'}</p>
              </div>

              <div>
                <Label className="text-sm font-medium">Top-Up Amount (KES) *</Label>
                <Input
                  type="number"
                  step="0.01"
                  min="1"
                  placeholder="Enter amount to add"
                  value={topUpAmount}
                  onChange={(e) => {
                    setTopUpAmount(e.target.value);
                    if (e.target.value && parseFloat(e.target.value) > 0 && selectedLoanForDetails) {
                      previewTopUp(selectedLoanForDetails.id, e.target.value);
                    }
                  }}
                  className="mt-2 text-sm"
                />
              </div>

              {topUpPreview && (
                <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-xs space-y-2">
                  <p className="font-semibold text-blue-900">Top-Up Preview</p>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <span className="text-gray-700">Current Outstanding:</span>
                      <p className="font-semibold">KES {topUpPreview.currentOutstanding?.toLocaleString()}</p>
                    </div>
                    <div>
                      <span className="text-gray-700">Top-Up Amount:</span>
                      <p className="font-semibold text-purple-700">+KES {topUpPreview.topupAmount?.toLocaleString()}</p>
                    </div>
                    <div>
                      <span className="text-gray-700">New Outstanding:</span>
                      <p className="font-bold text-red-600">KES {topUpPreview.newOutstanding?.toLocaleString()}</p>
                    </div>
                    <div>
                      <span className="text-gray-700">Principal Paid So Far:</span>
                      <p className="font-semibold text-green-600">KES {topUpPreview.principalPaidBeforeTopup?.toLocaleString()}</p>
                    </div>
                  </div>
                  <p className="text-gray-700 bg-white p-2 rounded">
                    ✓ This top-up will be added to the loan. Previous payments are preserved.
                  </p>
                </div>
              )}

              <div>
                <Label className="text-sm font-medium">Purpose (Optional)</Label>
                <Textarea
                  placeholder="Reason for top-up (e.g., Additional business capital)"
                  value={topUpPurpose}
                  onChange={(e) => setTopUpPurpose(e.target.value)}
                  className="mt-2 text-sm"
                  rows={3}
                />
              </div>

              {/* Guarantor Management for Top-Up */}
              <div className="border rounded-md p-3 space-y-2 bg-green-50">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-green-900">Add Guarantors for Top-Up (Optional)</p>
                    <p className="text-xs text-gray-600">Add new guarantors to support this top-up amount</p>
                  </div>
                  {topUpAmount && topUpGuarantors.length > 0 && (() => {
                    const totalPledged = topUpGuarantors.reduce((sum, g) => sum + (g.pledgeAmount || 0), 0);
                    const percentage = (totalPledged / parseFloat(topUpAmount)) * 100;
                    const isOver100 = percentage > 100;
                    return (
                      <div className="text-right">
                        <p className="text-xs text-gray-600">Total Guaranteed</p>
                        <p className={`text-sm font-bold ${isOver100 ? 'text-red-600' : 'text-green-700'}`}>
                          {percentage.toFixed(1)}%
                        </p>
                        {isOver100 && (
                          <p className="text-xs text-red-600 font-medium">Exceeds 100%!</p>
                        )}
                      </div>
                    );
                  })()}
                </div>

                {topUpAmount && topUpGuarantors.length > 0 && (() => {
                  const totalPledged = topUpGuarantors.reduce((sum, g) => sum + (g.pledgeAmount || 0), 0);
                  const percentage = (totalPledged / parseFloat(topUpAmount)) * 100;
                  if (percentage > 100) {
                    return (
                      <Alert className="bg-red-50 border-red-200 py-2">
                        <AlertCircle className="h-4 w-4 text-red-600" />
                        <AlertDescription className="text-xs text-red-800">
                          <strong>Warning:</strong> Total guarantor coverage is {percentage.toFixed(1)}% (exceeds 100%). 
                          Total pledged: KES {totalPledged.toLocaleString()} of KES {parseFloat(topUpAmount).toLocaleString()} needed.
                        </AlertDescription>
                      </Alert>
                    );
                  }
                })()}
                
                {topUpGuarantors.length > 0 && (
                  <div className="space-y-2 mb-2">
                    {topUpGuarantors.map((guarantor, i) => (
                      <div key={guarantor.id} className="bg-white border rounded p-2 space-y-2">
                        <div className="flex items-center justify-between">
                          <p className="text-xs font-medium text-gray-700">Guarantor {i + 1}</p>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="h-6 px-2 text-xs text-red-600 hover:bg-red-50"
                            onClick={() => {
                              const updated = topUpGuarantors.filter((_, index) => index !== i);
                              setTopUpGuarantors(updated);
                            }}
                          >
                            Remove
                          </Button>
                        </div>
                        <Select
                          value={guarantor.employeeId}
                          onValueChange={(value) => {
                            // Check if this guarantor is already selected
                            const isDuplicate = topUpGuarantors.some((g, idx) => idx !== i && g.employeeId === value);
                            if (isDuplicate) {
                              toast({ 
                                title: "Duplicate Guarantor", 
                                description: "This guarantor has already been selected. Please choose a different member.",
                                variant: "destructive" 
                              });
                              return;
                            }
                            
                            const updated = [...topUpGuarantors];
                            updated[i].employeeId = value;
                            setTopUpGuarantors(updated);
                          }}
                        >
                          <SelectTrigger className="text-xs h-8">
                            <SelectValue placeholder="Select guarantor" />
                          </SelectTrigger>
                          <SelectContent>
                            {members
                              .filter(m => m.id !== selectedLoanForDetails.member?.id)
                              .map((member) => {
                                const isAlreadySelected = topUpGuarantors.some((g, idx) => 
                                  idx !== i && g.employeeId === member.employeeId
                                );
                                return (
                                  <SelectItem 
                                    key={member.id} 
                                    value={member.employeeId || ""} 
                                    className="text-xs"
                                    disabled={isAlreadySelected}
                                  >
                                    {member.firstName} {member.lastName} ({member.employeeId})
                                    {isAlreadySelected && " - Already selected"}
                                  </SelectItem>
                                );
                              })}
                          </SelectContent>
                        </Select>
                        <div>
                          <Label className="text-xs">Pledge Amount (KES)</Label>
                          <Input
                            type="number"
                            step="0.01"
                            min="0"
                            placeholder="Enter amount"
                            value={guarantor.pledgeAmount || ""}
                            onChange={(e) => {
                              const updated = [...topUpGuarantors];
                              updated[i].pledgeAmount = parseFloat(e.target.value) || 0;
                              setTopUpGuarantors(updated);
                            }}
                            className="text-xs h-8 mt-1"
                          />
                          {topUpAmount && guarantor.pledgeAmount > 0 && (
                            <p className="text-xs text-gray-500 mt-1">
                              {((guarantor.pledgeAmount / parseFloat(topUpAmount)) * 100).toFixed(1)}% of top-up
                            </p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    // Check if already at 100% or more
                    if (topUpAmount && topUpGuarantors.length > 0) {
                      const totalPledged = topUpGuarantors.reduce((sum, g) => sum + (g.pledgeAmount || 0), 0);
                      const percentage = (totalPledged / parseFloat(topUpAmount)) * 100;
                      
                      if (percentage >= 100) {
                        const confirmed = window.confirm(
                          `Current guarantor coverage is already ${percentage.toFixed(1)}% (${percentage >= 100 ? 'at or above' : 'below'} 100%).\n\n` +
                          `Total pledged: KES ${totalPledged.toLocaleString()}\n` +
                          `Top-up amount: KES ${parseFloat(topUpAmount).toLocaleString()}\n\n` +
                          `Do you still want to add another guarantor?`
                        );
                        if (!confirmed) return;
                      }
                    }
                    
                    setTopUpGuarantors([
                      ...topUpGuarantors,
                      { id: `new-${Date.now()}`, employeeId: "", pledgeAmount: 0 }
                    ]);
                  }}
                  className="w-full text-xs"
                >
                  + Add Guarantor
                </Button>
              </div>

              <Alert className="bg-yellow-50 border-yellow-200">
                <AlertCircle className="h-4 w-4 text-yellow-600" />
                <AlertDescription className="text-xs text-yellow-800">
                  <strong>Note:</strong> Top-up preserves all existing loan data and payment history. Add guarantors above if needed for this top-up.
                </AlertDescription>
              </Alert>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setTopUpDialogOpen(false)}
                  type="button"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={topUpSubmitting || !topUpAmount || parseFloat(topUpAmount) <= 0}
                  className="bg-purple-600 hover:bg-purple-700"
                >
                  {topUpSubmitting ? "Adding..." : "Add Top-Up"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* Full Edit Dialog - Complete Financial Control */}
      <Dialog open={fullEditDialog} onOpenChange={(open) => {
        setFullEditDialog(open);
        if (!open) {
          setFullEditForm({
            principal: "",
            outstandingBalance: "",
            interestRate: "",
            termMonths: "",
            totalInterest: "",
            totalRepayable: "",
            monthlyRepayment: "",
            interestCollected: "",
            reason: ""
          });
        }
      }}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-base">Edit All Financial Fields</DialogTitle>
            <DialogDescription>
              Complete control over all loan financial fields. Can set outstanding to 0 for fully paid loans.
            </DialogDescription>
          </DialogHeader>
          {selectedLoanForDetails && (
            <form onSubmit={handleFullEdit} className="space-y-4">
              <div className="bg-orange-50 border border-orange-200 rounded-md p-3 text-sm">
                <p className="font-medium text-orange-900">Loan: {selectedLoanForDetails.loanNumber}</p>
                <p className="text-xs text-orange-800">{selectedLoanForDetails.member?.firstName} {selectedLoanForDetails.member?.lastName}</p>
                <p className="text-xs text-orange-800">Status: {selectedLoanForDetails.status}</p>
              </div>

              <Alert className="bg-red-50 border-red-200">
                <AlertCircle className="h-4 w-4 text-red-600" />
                <AlertDescription className="text-xs text-red-800">
                  <strong>Warning:</strong> This edit affects all financial calculations. Enter values carefully. You can set outstanding to 0 to mark as fully paid.
                </AlertDescription>
              </Alert>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm font-medium">Principal Amount (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.principal}
                    onChange={(e) => setFullEditForm({...fullEditForm, principal: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.amount.toLocaleString()}</p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Outstanding Balance (KES) *Can set to 0*</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Enter 0 for fully paid"
                    value={fullEditForm.outstandingBalance}
                    onChange={(e) => setFullEditForm({...fullEditForm, outstandingBalance: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.outstandingBalance?.toLocaleString() || '0'}</p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Interest Rate (%)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.interestRate}
                    onChange={(e) => setFullEditForm({...fullEditForm, interestRate: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: {selectedLoanForDetails.interestRate}%</p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Term (Months)</Label>
                  <Input
                    type="number"
                    min="1"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.termMonths}
                    onChange={(e) => setFullEditForm({...fullEditForm, termMonths: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: {selectedLoanForDetails.termMonths} months</p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Total Interest (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.totalInterest}
                    onChange={(e) => setFullEditForm({...fullEditForm, totalInterest: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.totalInterest?.toLocaleString() || '0'}</p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Total Repayable (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.totalRepayable}
                    onChange={(e) => setFullEditForm({...fullEditForm, totalRepayable: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.totalRepayable?.toLocaleString() || '0'}</p>
                </div>

                <div className="col-span-2">
                  <Label className="text-sm font-medium">Monthly Repayment (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.monthlyRepayment}
                    onChange={(e) => setFullEditForm({...fullEditForm, monthlyRepayment: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.monthlyRepayment?.toLocaleString() || '0'}</p>
                </div>

                <div className="col-span-2">
                  <Label className="text-sm font-medium">Interest Collected (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.interestCollected}
                    onChange={(e) => setFullEditForm({...fullEditForm, interestCollected: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.interestCollected?.toLocaleString() || '0'}</p>
                </div>

                <div className="col-span-2">
                  <Label className="text-sm font-medium">Principal Repaid (KES)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Leave blank to keep unchanged"
                    value={fullEditForm.principalRepaid}
                    onChange={(e) => setFullEditForm({...fullEditForm, principalRepaid: e.target.value})}
                    className="mt-2 text-sm"
                  />
                  <p className="text-xs text-gray-500 mt-1">Current: KES {selectedLoanForDetails.principalRepaid?.toLocaleString() || '0'}</p>
                  <p className="text-xs text-amber-600 mt-1">⚠️ Manual override - ignores top-ups and outstanding balance</p>
                </div>
              </div>

              <Alert className="bg-blue-50 border-blue-200">
                <AlertCircle className="h-4 w-4 text-blue-600" />
                <AlertDescription className="text-xs text-blue-800">
                  <strong>Complete Control:</strong> You can now edit Principal Repaid directly (ignores top-ups). Set to 0 for a fresh start. 
                  Total Repaid will recalculate as: Principal Repaid + Interest Collected.
                </AlertDescription>
              </Alert>

              <div>
                <Label className="text-sm font-medium">Reason for Edit *</Label>
                <Textarea
                  placeholder="Explain why you're making these changes (required)"
                  value={fullEditForm.reason}
                  onChange={(e) => setFullEditForm({...fullEditForm, reason: e.target.value})}
                  className="mt-2 text-sm"
                  rows={3}
                  required
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setFullEditDialog(false)}
                  type="button"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={fullEditSubmitting || !fullEditForm.reason}
                  className="bg-orange-600 hover:bg-orange-700"
                >
                  {fullEditSubmitting ? "Updating..." : "Update Loan"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
      {/* Edit Top-Up Dialog */}
      <Dialog open={editTopUpDialog} onOpenChange={setEditTopUpDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Edit Top-Up #{topUpToEdit?.id}</DialogTitle>
            <DialogDescription>
              Modify the top-up amount and purpose. This will update loan calculations.
            </DialogDescription>
          </DialogHeader>
          {topUpToEdit && (
            <form onSubmit={handleEditTopUp} className="space-y-4">
              <div>
                <Label className="text-sm font-medium">Top-Up Amount (KES) *</Label>
                <Input
                  type="number"
                  step="0.01"
                  min="1"
                  value={editTopUpAmount}
                  onChange={(e) => setEditTopUpAmount(e.target.value)}
                  className="mt-2"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">Current: KES {topUpToEdit.topupAmount?.toLocaleString()}</p>
              </div>

              <div>
                <Label className="text-sm font-medium">Purpose (Optional)</Label>
                <Textarea
                  value={editTopUpPurpose}
                  onChange={(e) => setEditTopUpPurpose(e.target.value)}
                  className="mt-2"
                  rows={2}
                  placeholder="e.g., Additional funds for business expansion"
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setEditTopUpDialog(false);
                    setTopUpToEdit(null);
                    setEditTopUpAmount("");
                    setEditTopUpPurpose("");
                  }}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={editTopUpSubmitting}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  {editTopUpSubmitting ? "Updating..." : "Update Top-Up"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Top-Up Confirmation Dialog */}
      <Dialog open={deleteTopUpDialog} onOpenChange={setDeleteTopUpDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Delete Top-Up #{topUpToDelete?.id}?</DialogTitle>
            <DialogDescription>
              This will permanently delete this top-up and recalculate the loan. This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          {topUpToDelete && (
            <div className="space-y-4">
              <div className="bg-red-50 border border-red-200 rounded p-3">
                <p className="text-sm font-medium text-red-900">Top-Up Details:</p>
                <p className="text-sm text-red-800 mt-1">Amount: KES {topUpToDelete.topupAmount?.toLocaleString()}</p>
                <p className="text-sm text-red-800">Date: {new Date(topUpToDelete.topupDate).toLocaleDateString()}</p>
                {topUpToDelete.purpose && (
                  <p className="text-sm text-red-800">Purpose: {topUpToDelete.purpose}</p>
                )}
              </div>

              <Alert className="bg-yellow-50 border-yellow-200">
                <AlertCircle className="h-4 w-4 text-yellow-600" />
                <AlertDescription className="text-xs text-yellow-800">
                  <strong>Warning:</strong> Deleting this top-up will reduce the loan amount and recalculate outstanding balance, 
                  total repayable, and monthly payment. Make sure this is what you want to do.
                </AlertDescription>
              </Alert>

              <div className="flex gap-2 justify-end pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setDeleteTopUpDialog(false);
                    setTopUpToDelete(null);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  type="button"
                  size="sm"
                  disabled={deleteTopUpSubmitting}
                  className="bg-red-600 hover:bg-red-700"
                  onClick={handleDeleteTopUp}
                >
                  {deleteTopUpSubmitting ? "Deleting..." : "Yes, Delete Top-Up"}
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Loans;
