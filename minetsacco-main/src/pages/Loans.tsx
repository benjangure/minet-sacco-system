import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Plus, Search, Eye, CheckCircle, XCircle, DollarSign, AlertCircle, Edit } from "lucide-react";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import GuarantorDetailsModal from "@/components/GuarantorDetailsModal";

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
  const { toast } = useToast();
  const { session, role } = useAuth();

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
      fetchMembers();
      fetchProducts();
      // Fetch global loan term limit set by admin
      fetch(`${API_BASE_URL}/loan-eligibility-rules`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      }).then(r => r.json()).then(data => {
        if (data?.data?.maxLoanTermMonths) {
          setMaxLoanTermMonths(data.data.maxLoanTermMonths);
        }
      }).catch(() => {});
    }
  }, [session]);

  // Separate effect to handle loan fetching when statusFilter changes
  useEffect(() => {
    if (session) {
      fetchLoans();
    }
  }, [session, statusFilter]);

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

    try {
      // Build guarantor requests with amounts
      const guarantorRequests = selectedGuarantors.map(guarantorId => ({
        guarantorId: guarantorId,
        guaranteeAmount: guarantorAmountMap[guarantorId] || 0,
        selfGuarantee: false
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
    
    // Fetch interest collected from actual repayments for this loan
    try {
      const repaymentRes = await fetch(`${API_BASE_URL}/loans/${loan.id}/repayments`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (repaymentRes.ok) {
        const repaymentData = await repaymentRes.json();
        const repayments = repaymentData.data || [];
        const repaymentInterest = repayments.reduce((sum: number, rep: any) => sum + (rep.interestAmount || 0), 0);
        loanToDisplay.interestCollected = Math.max(
          repaymentInterest,
          loan.interestCollected || 0
        );
      }
    } catch (error) {
      console.error("Error fetching repayments for interest calculation:", error);
      // Continue with existing data if repayment fetch fails
    }
    
    setSelectedLoanForDetails(loanToDisplay);
    setLoanDetailsOpen(true);
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

  const filteredLoans = loans.filter(loan =>
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

        // Validate total guarantees match principal amount
        const totalGuarantees = guarantorsToSubmit.reduce((sum, g) => sum + g.pledgeAmount, 0);
        // Allow small rounding differences (within 1 unit)
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
          toast({ title: "Error", description: "At least one guarantor must be assigned", variant: "destructive" });
          setEditSubmitting(false);
          return;
        }

        updatePayload.guarantorshipType = "NORMAL";
        updatePayload.guarantors = guarantorsToSubmit;
      }

      // Must have at least one field to update
      if (Object.keys(updatePayload).length === 0) {
        toast({ title: "Error", description: "Please fill in at least one field to update", variant: "destructive" });
        setEditSubmitting(false);
        return;
      }

      const response = await fetch(`${API_BASE_URL}/loans/${loanToEdit.id}/update`, {
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
                        return (
                          <div key={guarantorId} className="flex items-center justify-between bg-white p-2 rounded border border-blue-200">
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
                                  runPreCheck(form.memberId, form.amount, updated);
                                }}
                              >
                                Remove
                              </Button>
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
                    {loan.member?.firstName} {loan.member?.lastName}
                  </TableCell>
                  <TableCell>{loan.loanProduct?.name}</TableCell>
                  <TableCell>KES {loan.amount.toLocaleString()}</TableCell>
                  <TableCell>
                    <Badge className={loanStatusColors[loan.status]}>
                      {loan.status.replace("_", " ")}
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
                        className="text-blue-600"
                      >
                        <Eye className="h-4 w-4 mr-1" />
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
                      {(loan.status === "DISBURSED" || loan.status === "REPAID" || loan.status === "DEFAULTED") && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={() => handleOpenEditDialog(loan)}
                          title="Edit Loan"
                          className="text-amber-600"
                          type="button"
                        >
                          <Edit className="h-4 w-4" />
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
            <div className="space-y-2">
              {/* Loan Summary */}
              <Card className="p-2">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-1 text-xs">
                  <div>
                    <p className="text-xs text-gray-600">ID</p>
                    <p className="font-medium">{selectedLoanForDetails.id}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Status</p>
                    <Badge className={`${loanStatusColors[selectedLoanForDetails.status]} text-xs py-0.5`}>
                      {selectedLoanForDetails.status.replace("_", " ")}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Member</p>
                    <p className="font-medium text-xs">{selectedLoanForDetails.member?.firstName} {selectedLoanForDetails.member?.lastName}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Product</p>
                    <p className="font-medium text-xs">{selectedLoanForDetails.loanProduct?.name}</p>
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
                    <p className="font-medium">{selectedLoanForDetails.status}</p>
                  </div>
                </div>
              </Card>

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
                          KES {selectedLoanForDetails.amount && selectedLoanForDetails.outstandingBalance !== undefined
                            ? Math.max(0, (selectedLoanForDetails.amount - selectedLoanForDetails.outstandingBalance) + (selectedLoanForDetails.interestCollected || 0)).toLocaleString()
                            : "0"}
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

              {/* Phase A: Low-Risk Field Editing Section - Treasurer Only */}
              {role === "TREASURER" && (
                <Card className="p-3 border-indigo-200 bg-indigo-50">
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-semibold text-xs text-indigo-900">📝 Phase A: Edit Loan Fields</p>
                    <p className="text-xs text-indigo-700 font-medium">(No guarantor changes)</p>
                  </div>
                  
                  {phaseAEditOpen ? (
                    <form onSubmit={handlePhaseAEdit} className="space-y-3">
                      {phaseAErrors._form && (
                        <Alert className="bg-red-50 border-red-200">
                          <AlertCircle className="h-4 w-4 text-red-600" />
                          <AlertDescription className="text-xs text-red-700">{phaseAErrors._form}</AlertDescription>
                        </Alert>
                      )}

                      {/* Loan Status */}
                      <div>
                        <Label className="text-xs">Loan Status</Label>
                        <Select value={phaseAForm.loanStatus} onValueChange={(val) => setPhaseAForm({...phaseAForm, loanStatus: val})}>
                          <SelectTrigger className="h-8 text-xs">
                            <SelectValue placeholder="Leave unchanged" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="PENDING">PENDING</SelectItem>
                            <SelectItem value="APPROVED">APPROVED</SelectItem>
                            <SelectItem value="DISBURSED">DISBURSED</SelectItem>
                            <SelectItem value="REPAID">REPAID</SelectItem>
                            <SelectItem value="DEFAULTED">DEFAULTED</SelectItem>
                          </SelectContent>
                        </Select>
                        {phaseAErrors.loanStatus && <p className="text-xs text-red-600">{phaseAErrors.loanStatus}</p>}
                      </div>

                      {/* Disbursement Date */}
                      <div>
                        <Label className="text-xs">Disbursement Date</Label>
                        <Input
                          type="date"
                          value={phaseAForm.disbursementDate}
                          onChange={(e) => setPhaseAForm({...phaseAForm, disbursementDate: e.target.value})}
                          className="h-8 text-xs"
                          placeholder="Leave unchanged"
                        />
                        {phaseAErrors.disbursementDate && <p className="text-xs text-red-600">{phaseAErrors.disbursementDate}</p>}
                      </div>

                      {/* Interest Rate */}
                      <div>
                        <Label className="text-xs">Interest Rate (%)</Label>
                        <Input
                          type="number"
                          step="0.01"
                          min="0"
                          value={phaseAForm.interestRate}
                          onChange={(e) => setPhaseAForm({...phaseAForm, interestRate: e.target.value})}
                          className="h-8 text-xs"
                          placeholder="Leave unchanged"
                        />
                        {phaseAErrors.interestRate && <p className="text-xs text-red-600">{phaseAErrors.interestRate}</p>}
                      </div>

                      {/* Outstanding Balance */}
                      <div>
                        <Label className="text-xs">Outstanding Balance (KES) - Max: {selectedLoanForDetails.amount?.toLocaleString()}</Label>
                        <Input
                          type="number"
                          step="0.01"
                          min="0"
                          value={phaseAForm.outstandingBalance}
                          onChange={(e) => setPhaseAForm({...phaseAForm, outstandingBalance: e.target.value})}
                          className="h-8 text-xs"
                          placeholder="Leave unchanged"
                        />
                        {phaseAErrors.outstandingBalance && <p className="text-xs text-red-600">{phaseAErrors.outstandingBalance}</p>}
                      </div>

                      {/* Interest Collected - For Migrated Loans */}
                      {selectedLoanForDetails.migrationStatus === "MIGRATED" && (
                        <div>
                          <Label className="text-xs">Interest Collected (KES) <span className="text-blue-600">Migrated Loans Only</span></Label>
                          <Input
                            type="number"
                            step="0.01"
                            min="0"
                            value={phaseAForm.interestCollected}
                            onChange={(e) => setPhaseAForm({...phaseAForm, interestCollected: e.target.value})}
                            className="h-8 text-xs"
                            placeholder="Leave unchanged"
                          />
                          {phaseAErrors.interestCollected && <p className="text-xs text-red-600">{phaseAErrors.interestCollected}</p>}
                          <p className="text-xs text-gray-500 mt-1">
                            Interest already collected during loan repayment period. Updates interest remaining calculation.
                          </p>
                        </div>
                      )}

                      {/* Purpose */}
                      <div>
                        <Label className="text-xs">Purpose</Label>
                        <Textarea
                          value={phaseAForm.purpose}
                          onChange={(e) => setPhaseAForm({...phaseAForm, purpose: e.target.value})}
                          className="text-xs min-h-16"
                          placeholder="Leave unchanged"
                        />
                      </div>

                      <Alert className="bg-blue-50 border-blue-200">
                        <AlertCircle className="h-4 w-4 text-blue-600" />
                        <AlertDescription className="text-xs text-blue-700">
                          ✅ This form only sends Phase A fields. No guarantor data will be included in the request.
                        </AlertDescription>
                      </Alert>

                      <div className="flex gap-2">
                        <Button
                          type="submit"
                          size="sm"
                          className="h-8 text-xs flex-1 bg-indigo-600 hover:bg-indigo-700"
                          disabled={phaseASubmitting}
                        >
                          {phaseASubmitting ? "Updating..." : "Update Loan Fields"}
                        </Button>
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          className="h-8 text-xs flex-1"
                          onClick={() => {
                            setPhaseAEditOpen(false);
                            setPhaseAForm({ loanStatus: "", disbursementDate: "", interestRate: "", outstandingBalance: "", interestCollected: "", purpose: "" });
                            setPhaseAErrors({});
                          }}
                        >
                          Cancel
                        </Button>
                      </div>
                    </form>
                  ) : (
                    <Button
                      size="sm"
                      className="h-8 text-xs w-full bg-indigo-600 hover:bg-indigo-700"
                      onClick={() => {
                        if (selectedLoanForDetails) {
                          // Pre-fill form with current loan values
                          setPhaseAForm({
                            loanStatus: selectedLoanForDetails.status || "",
                            disbursementDate: selectedLoanForDetails.disbursementDate ? new Date(selectedLoanForDetails.disbursementDate).toISOString().split('T')[0] : "",
                            interestRate: selectedLoanForDetails.interestRate?.toString() || "",
                            outstandingBalance: selectedLoanForDetails.outstandingBalance?.toString() || "",
                            interestCollected: (selectedLoanForDetails.interestCollected !== undefined ? selectedLoanForDetails.interestCollected : "").toString(),
                            purpose: selectedLoanForDetails.purpose || ""
                          });
                          setPhaseAErrors({});
                        }
                        setPhaseAEditOpen(true);
                      }}
                    >
                      <Edit className="w-3 h-3 mr-1" />
                      Edit Loan Fields (Phase A)
                    </Button>
                  )}
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
                <div>
                  <p className="text-sm font-medium mb-2">Guarantor Management</p>
                  <p className="text-xs text-gray-600">
                    Outstanding Balance: <span className="font-semibold">KES {(parseFloat(editForm.outstandingBalance) || loanToEdit.outstandingBalance || loanToEdit.amount).toLocaleString()}</span>
                  </p>
                  <p className="text-xs text-gray-500 mt-1">Remove guarantors • Edit pledge amounts • Add new guarantors</p>
                </div>

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
                            .filter((g: any) => !removedGuarantorIds.includes(g.id))
                            .reduce((sum, g: any) => {
                              const amount = editedGuarantorAmounts[g.id] !== undefined 
                                ? editedGuarantorAmounts[g.id]
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
    </div>
  );
};

export default Loans;
