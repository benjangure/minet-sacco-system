import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Upload, Eye, CheckCircle, XCircle, AlertCircle, FileSpreadsheet, FileDown, Info } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Textarea } from "@/components/ui/textarea";

const API_BASE_URL = "http://localhost:8080/api";

interface BulkBatch {
  id: number;
  batchNumber: string;
  batchType: string;
  fileName: string;
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  totalAmount: number;
  status: string;
  uploadedByUsername: string;
  uploadedAt: string;
  approvedByUsername?: string;
  approvedAt?: string;
  processedAt?: string;
}

interface BulkTransactionItem {
  id: number;
  rowNumber: number;
  memberNumber: string;
  savingsAmount: number;
  sharesAmount: number;
  loanRepaymentAmount: number;
  loanNumber?: string;
  benevolentFundAmount: number;
  developmentFundAmount: number;
  schoolFeesAmount: number;
  holidayFundAmount: number;
  emergencyFundAmount: number;
  status: string;
  errorMessage?: string;
  processedAt?: string;
}

interface BulkMemberItem {
  id: number;
  rowNumber: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  nationalId: string;
  dateOfBirth: string;
  department: string;
  employeeId: string;
  status: string;
  errorMessage?: string;
  processedAt?: string;
}

interface BulkLoanItem {
  id: number;
  rowNumber: number;
  memberNumber: string;
  loanProductName: string;
  amount: number;
  purpose: string;
  guarantor1: string;
  guarantor2: string;
  status: string;
  errorMessage?: string;
  processedAt?: string;
  totalInterest?: number;
  totalRepayable?: number;
  monthlyRepayment?: number;
  guarantor1EligibilityStatus?: string;
  guarantor2EligibilityStatus?: string;
}

const statusColors: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  VALIDATION_FAILED: "bg-red-100 text-red-800",
  APPROVED: "bg-blue-100 text-blue-800",
  PROCESSING: "bg-purple-100 text-purple-800",
  COMPLETED: "bg-green-100 text-green-800",
  PARTIALLY_COMPLETED: "bg-orange-100 text-orange-800",
  REJECTED: "bg-red-100 text-red-800",
};

interface Fund {
  id: number;
  fundType: string;
  displayName: string;
  enabled: boolean;
  displayOrder: number;
  description?: string;
  minimumAmount?: number;
  maximumAmount?: number;
}

export default function BulkProcessing() {
  const { session, user } = useAuth();
  const { toast } = useToast();
  const [batches, setBatches] = useState<BulkBatch[]>([]);
  const [selectedBatch, setSelectedBatch] = useState<BulkBatch | null>(null);
  const [batchItems, setBatchItems] = useState<BulkTransactionItem[]>([]);
  const [memberItems, setMemberItems] = useState<BulkMemberItem[]>([]);
  const [loanItems, setLoanItems] = useState<BulkLoanItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [guarantorValidationDialogOpen, setGuarantorValidationDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [batchType, setBatchType] = useState("MONTHLY_CONTRIBUTIONS");
  const [rejectionReason, setRejectionReason] = useState("");
  const [enabledFunds, setEnabledFunds] = useState<Fund[]>([]);
  const [guarantorValidationResult, setGuarantorValidationResult] = useState<any>(null);
  const [selectedLoanItemId, setSelectedLoanItemId] = useState<number | null>(null);
  const [rejectLoanItemDialogOpen, setRejectLoanItemDialogOpen] = useState(false);
  const [loanItemRejectionReason, setLoanItemRejectionReason] = useState("");
  const [selectedLoanItemIds, setSelectedLoanItemIds] = useState<Set<number>>(new Set());
  const [bulkApprovalDialogOpen, setBulkApprovalDialogOpen] = useState(false);
  const [bulkApprovalResult, setBulkApprovalResult] = useState<any>(null);
  const [approvedLoanItems, setApprovedLoanItems] = useState<BulkLoanItem[]>([]);
  const [selectedDisbursementItemIds, setSelectedDisbursementItemIds] = useState<Set<number>>(new Set());
  const [bulkDisbursementDialogOpen, setBulkDisbursementDialogOpen] = useState(false);
  const [bulkDisbursementResult, setBulkDisbursementResult] = useState<any>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [pageSize, setPageSize] = useState(5);
  const [currentPage, setCurrentPage] = useState(1);
  const [pendingPageSize, setPendingPageSize] = useState(5);
  const [pendingCurrentPage, setPendingCurrentPage] = useState(1);
  const [completedPageSize, setCompletedPageSize] = useState(5);
  const [completedCurrentPage, setCompletedCurrentPage] = useState(1);

  const token = session?.token;

  // Filter and search batches
  const filteredBatches = batches.filter((batch) => {
    const searchLower = searchQuery.toLowerCase();
    return (
      batch.batchNumber.toLowerCase().includes(searchLower) ||
      batch.batchType.toLowerCase().includes(searchLower) ||
      batch.fileName.toLowerCase().includes(searchLower) ||
      batch.status.toLowerCase().includes(searchLower) ||
      batch.uploadedByUsername.toLowerCase().includes(searchLower)
    );
  });

  // Pagination for All Batches
  const totalPages = Math.ceil(filteredBatches.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedBatches = filteredBatches.slice(startIndex, endIndex);

  // Pagination for Pending Batches
  const pendingBatches = batches.filter((b) => b.status === "PENDING");
  const pendingTotalPages = Math.ceil(pendingBatches.length / pendingPageSize);
  const pendingStartIndex = (pendingCurrentPage - 1) * pendingPageSize;
  const pendingEndIndex = pendingStartIndex + pendingPageSize;
  const paginatedPendingBatches = pendingBatches.slice(pendingStartIndex, pendingEndIndex);

  // Pagination for Completed Batches
  const completedBatches = batches.filter((b) => b.status === "COMPLETED" || b.status === "PARTIALLY_COMPLETED");
  const completedTotalPages = Math.ceil(completedBatches.length / completedPageSize);
  const completedStartIndex = (completedCurrentPage - 1) * completedPageSize;
  const completedEndIndex = completedStartIndex + completedPageSize;
  const paginatedCompletedBatches = completedBatches.slice(completedStartIndex, completedEndIndex);

  // Reset to first page when search changes
  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setCurrentPage(1);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
    setCurrentPage(1);
  };

  const handlePendingPageSizeChange = (size: number) => {
    setPendingPageSize(size);
    setPendingCurrentPage(1);
  };

  const handleCompletedPageSizeChange = (size: number) => {
    setCompletedPageSize(size);
    setCompletedCurrentPage(1);
  };

  // Restrict access: only TREASURER and CREDIT_COMMITTEE can access bulk processing
  if (user?.role !== "TREASURER" && user?.role !== "CREDIT_COMMITTEE") {
    return (
      <div className="container mx-auto p-6">
        <Card className="border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle className="text-red-900">Access Denied</CardTitle>
          </CardHeader>
          <CardContent className="text-red-800">
            Only Treasurer and Credit Committee members can access bulk processing.
          </CardContent>
        </Card>
      </div>
    );
  }

  useEffect(() => {
    fetchBatches();
    fetchEnabledFunds();
    if (user?.role === "TREASURER") {
      fetchApprovedLoans();
    }
  }, []);

  const fetchEnabledFunds = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/fund-configurations/enabled`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await response.json();
      if (data.success) {
        setEnabledFunds(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching enabled funds:", error);
    }
  };

  const fetchBatches = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/batches`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await response.json();
      if (data.success) {
        // Filter batches based on user role
        let filteredBatches = data.data;
        if (user?.role === "CREDIT_COMMITTEE") {
          // Credit Committee only sees loan batches
          filteredBatches = data.data.filter((batch: BulkBatch) => 
            batch.batchType === "LOAN_APPLICATIONS" || batch.batchType === "LOAN_DISBURSEMENTS"
          );
        }
        setBatches(filteredBatches);
      }
    } catch (error) {
      console.error("Error fetching batches:", error);
    }
  };

  const fetchApprovedLoans = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/approved`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await response.json();
      if (data.success) {
        setApprovedLoanItems(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching approved loans:", error);
    }
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      // Validate file type
      const validTypes = [
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
        "text/csv",
      ];
      if (!validTypes.includes(file.type)) {
        toast({
          title: "Invalid File Type",
          description: "Please upload an Excel (.xlsx, .xls) or CSV file",
          variant: "destructive",
        });
        return;
      }

      // Validate file size (5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast({
          title: "File Too Large",
          description: "File size must not exceed 5MB",
          variant: "destructive",
        });
        return;
      }

      setSelectedFile(file);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      toast({
        title: "No File Selected",
        description: "Please select a file to upload",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append("file", selectedFile);
    formData.append("batchType", batchType);

    try {
      const response = await fetch(`${API_BASE_URL}/bulk/upload`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      const data = await response.json();
      console.log("Upload response:", data);
      console.log("Response status:", response.status);
      
      if (data.success) {
        toast({
          title: "Upload Successful",
          description: `Batch ${data.data.batchNumber} uploaded successfully`,
        });
        setUploadDialogOpen(false);
        setSelectedFile(null);
        fetchBatches();
      } else {
        console.error("Upload error details:", data.message);
        
        // Parse error message to show validation errors clearly
        let errorMessage = data.message;
        let errorDetails = "";
        
        // Check if it's a validation error with multiple errors
        if (data.message && data.message.includes("Row")) {
          // Extract all validation errors
          const errors = data.message.split(";").filter((e: string) => e.trim());
          errorMessage = "Validation errors found:";
          errorDetails = errors.map((e: string) => `• ${e.trim()}`).join("\n");
        } else if (data.message && data.message.includes("Duplicate")) {
          errorMessage = "Duplicate data found in your file";
          errorDetails = data.message;
        } else if (data.message && data.message.includes("already exists")) {
          errorMessage = "Some records already exist in the system";
          errorDetails = data.message;
        } else if (data.message && data.message.includes("Invalid")) {
          errorMessage = "Invalid data format";
          errorDetails = data.message;
        }
        
        toast({
          title: "Upload Failed",
          description: errorMessage,
          variant: "destructive",
        });
        
        // Show detailed errors in a dialog if there are multiple errors
        if (errorDetails) {
          alert(`Validation Errors:\n\n${errorDetails}`);
        }
      }
    } catch (error) {
      let errorMessage = "Failed to upload file. Please try again.";
      
      if (error instanceof Error) {
        errorMessage = error.message;
      }
      
      toast({
        title: "Upload Error",
        description: errorMessage,
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const viewBatchDetails = async (batch: BulkBatch) => {
    setSelectedBatch(batch);
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/batches/${batch.id}/items`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.message || `Server error: ${response.status}`);
      }

      const data = await response.json();
      if (data.success) {
        switch (batch.batchType) {
          case "MEMBER_REGISTRATION":
            setMemberItems(data.data || []);
            break;
          case "LOAN_APPLICATIONS":
            setLoanItems(data.data || []);
            break;
          default:
            setBatchItems(data.data || []);
        }
        setDetailsDialogOpen(true);
      } else {
        throw new Error(data.message || "Failed to load batch details");
      }
    } catch (error) {
      console.error("viewBatchDetails error:", error);
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to load batch details",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const validateLoanGuarantors = async (loanItemId: number) => {
    setLoading(true);
    setGuarantorValidationResult(null); // Clear old data first
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/${loanItemId}/validate-guarantors`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      console.log("Guarantor validation response:", data);
      if (data.success) {
        console.log("Validation result data:", data.data);
        setGuarantorValidationResult(data.data);
        setGuarantorValidationDialogOpen(true);
      } else {
        toast({
          title: "Validation Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      console.error("Guarantor validation error:", error);
      toast({
        title: "Error",
        description: "Failed to validate guarantors",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleApproveLoanItem = async (itemId: number) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/${itemId}/approve`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      if (data.success) {
        const decision = data.data.status;
        let message = "";
        let variant = "default";

        if (decision === "APPROVED") {
          message = "Loan approved - All eligibility criteria met";
          variant = "default";
        } else if (decision === "CONDITIONAL_APPROVAL") {
          message = "Conditional approval - Member eligible but some guarantors not eligible. Review required.";
          variant = "default";
        } else if (decision === "REJECTED") {
          message = "Loan rejected - " + (data.data.details ? data.data.details[0] : "Eligibility criteria not met");
          variant = "destructive";
        }

        toast({
          title: decision === "REJECTED" ? "Loan Rejected" : "Decision Made",
          description: message,
          variant: variant as any,
        });

        // Refresh the batch items
        if (selectedBatch) {
          const itemsResponse = await fetch(`${API_BASE_URL}/bulk/batches/${selectedBatch.id}/items`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });
          const itemsData = await itemsResponse.json();
          if (itemsData.success) {
            setLoanItems(itemsData.data);
          }
        }
      } else {
        toast({
          title: "Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to approve loan item",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRejectLoanItem = async () => {
    if (!selectedLoanItemId || !loanItemRejectionReason.trim()) {
      toast({
        title: "Error",
        description: "Please provide a rejection reason",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/${selectedLoanItemId}/reject?reason=${encodeURIComponent(loanItemRejectionReason)}`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Success",
          description: "Loan item rejected",
        });
        setRejectLoanItemDialogOpen(false);
        setLoanItemRejectionReason("");
        setSelectedLoanItemId(null);
        // Refresh the batch items
        if (selectedBatch) {
          const itemsResponse = await fetch(`${API_BASE_URL}/bulk/batches/${selectedBatch.id}/items`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });
          const itemsData = await itemsResponse.json();
          if (itemsData.success) {
            setLoanItems(itemsData.data);
          }
        }
      } else {
        toast({
          title: "Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to reject loan item",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleBulkApproveLoanItems = async () => {
    if (selectedLoanItemIds.size === 0) {
      toast({
        title: "Error",
        description: "Please select at least one loan item to approve",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      const itemIds = Array.from(selectedLoanItemIds);
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/bulk-approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(itemIds),
      });

      const data = await response.json();
      if (data.success) {
        setBulkApprovalResult(data.data);
        setBulkApprovalDialogOpen(true);

        toast({
          title: "Bulk Approval Completed",
          description: `${data.data.approvedCount} approved, ${data.data.flaggedCount} flagged for review`,
        });

        // Refresh the batch items
        if (selectedBatch) {
          const itemsResponse = await fetch(`${API_BASE_URL}/bulk/batches/${selectedBatch.id}/items`, {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });
          const itemsData = await itemsResponse.json();
          if (itemsData.success) {
            setLoanItems(itemsData.data);
          }
        }

        // Clear selection
        setSelectedLoanItemIds(new Set());
      } else {
        toast({
          title: "Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to bulk approve loan items",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const toggleLoanItemSelection = (itemId: number) => {
    const newSelection = new Set(selectedLoanItemIds);
    if (newSelection.has(itemId)) {
      newSelection.delete(itemId);
    } else {
      newSelection.add(itemId);
    }
    setSelectedLoanItemIds(newSelection);
  };

  const toggleAllLoanItemsSelection = () => {
    if (selectedLoanItemIds.size === loanItems.length) {
      setSelectedLoanItemIds(new Set());
    } else {
      setSelectedLoanItemIds(new Set(loanItems.map(item => item.id)));
    }
  };

  const handleBulkDisburseLoanItems = async () => {
    if (selectedDisbursementItemIds.size === 0) {
      toast({
        title: "Error",
        description: "Please select at least one loan item to disburse",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      const itemIds = Array.from(selectedDisbursementItemIds);
      const response = await fetch(`${API_BASE_URL}/bulk/loan-items/bulk-disburse`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(itemIds),
      });

      const data = await response.json();
      if (data.success) {
        setBulkDisbursementResult(data.data);
        setBulkDisbursementDialogOpen(true);

        toast({
          title: "Bulk Disbursement Completed",
          description: `${data.data.successfulCount} disbursed, ${data.data.failedCount} failed`,
        });

        // Refresh approved loans
        fetchApprovedLoans();

        // Clear selection
        setSelectedDisbursementItemIds(new Set());
      } else {
        toast({
          title: "Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to bulk disburse loan items",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const toggleDisbursementItemSelection = (itemId: number) => {
    const newSelection = new Set(selectedDisbursementItemIds);
    if (newSelection.has(itemId)) {
      newSelection.delete(itemId);
    } else {
      newSelection.add(itemId);
    }
    setSelectedDisbursementItemIds(newSelection);
  };

  const toggleAllDisbursementItemsSelection = () => {
    if (selectedDisbursementItemIds.size === approvedLoanItems.length) {
      setSelectedDisbursementItemIds(new Set());
    } else {
      setSelectedDisbursementItemIds(new Set(approvedLoanItems.map(item => item.id)));
    }
  };

  const handleApproveBatch = async (batchId: number) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/bulk/batches/${batchId}/approve`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Batch Approved",
          description: "Batch has been approved and processing started",
        });
        setDetailsDialogOpen(false);
        fetchBatches();
      } else {
        toast({
          title: "Approval Failed",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to approve batch",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRejectBatch = async () => {
    if (!selectedBatch || !rejectionReason.trim()) {
      toast({
        title: "Rejection Reason Required",
        description: "Please provide a reason for rejection",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/bulk/batches/${selectedBatch.id}/reject?reason=${encodeURIComponent(rejectionReason)}`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Batch Rejected",
          description: "Batch has been rejected",
        });
        setRejectDialogOpen(false);
        setDetailsDialogOpen(false);
        setRejectionReason("");
        fetchBatches();
      } else {
        toast({
          title: "Rejection Failed",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to reject batch",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const formatBatchType = (batchType: string) => {
    if (batchType === "LOAN_APPLICATIONS") return "Loan Migration (Historical)";
    return batchType.replace(/_/g, " ");
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-KE", {
      style: "currency",
      currency: "KES",
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-KE");
  };

  const downloadTemplate = () => {
    // Import XLSX dynamically
    import("xlsx").then((XLSX) => {
      let data: any[] = [];
      let filename = "";
      let sheetName = "Template";

      switch (batchType) {
        case "MONTHLY_CONTRIBUTIONS":
          // Build dynamic fund columns based on enabled funds
          const fundColumns: Record<string, number> = {};
          enabledFunds.forEach((fund) => {
            fundColumns[fund.displayName] = 200; // Example amount
          });

          // Base columns (always included) - SHARES removed as this SACCO does not accept share deposits
          const baseRow = {
            "Employee ID": "EMP001",
            "Savings": 5000,
            ...fundColumns,
          };

          // Add loan columns only if member has active loans (optional)
          const rowWithLoan = {
            "Employee ID": "EMP002",
            "Savings": 8000,
            "Loan Repayment": 5000,
            "Loan Number": "LN-2026-002",
            ...fundColumns,
          };

          const rowWithoutLoan = {
            "Employee ID": "EMP003",
            "Savings": 6000,
            ...fundColumns,
          };

          data = [baseRow, rowWithLoan, rowWithoutLoan];
          filename = "Monthly_Contributions_Template.xlsx";
          sheetName = "Contributions";
          break;

        case "LOAN_DISBURSEMENTS":
          data = [
            {
              "Member Number": "M-2026-001",
              "Loan Number": "LN-2026-001",
              "Disbursement Amount": 50000,
              "Disbursement Account": "SAVINGS",
            },
            {
              "Member Number": "M-2026-002",
              "Loan Number": "LN-2026-002",
              "Disbursement Amount": 100000,
              "Disbursement Account": "SAVINGS",
            },
            {
              "Member Number": "M-2026-003",
              "Loan Number": "LN-2026-003",
              "Disbursement Amount": 75000,
              "Disbursement Account": "SHARES",
            },
          ];
          filename = "Loan_Disbursements_Template.xlsx";
          sheetName = "Disbursements";
          break;

        case "MEMBER_REGISTRATION":
          data = [
            {
              "First Name": "John",
              "Last Name": "Doe",
              "Email": "john.doe@email.com",
              "Phone": "0712345678",
              "National ID": "12345678",
              "Date of Birth": "1990-01-15",
              "Department": "IT",
              "Employee ID": "EMP001",
              "Employer": "Minet Insurance",
              "Bank Name": "KCB Bank",
              "Bank Account": "1234567890",
              "Next of Kin": "Jane Doe",
              "NOK Phone": "0723456789",
            },
            {
              "First Name": "Jane",
              "Last Name": "Smith",
              "Email": "jane.smith@email.com",
              "Phone": "0723456789",
              "National ID": "23456789",
              "Date of Birth": "1985-05-20",
              "Department": "Finance",
              "Employee ID": "EMP002",
              "Employer": "Minet Insurance",
              "Bank Name": "Equity Bank",
              "Bank Account": "2345678901",
              "Next of Kin": "John Smith",
              "NOK Phone": "0734567890",
            },
            {
              "First Name": "Peter",
              "Last Name": "Kamau",
              "Email": "peter.kamau@email.com",
              "Phone": "0734567890",
              "National ID": "34567890",
              "Date of Birth": "1992-08-10",
              "Department": "HR",
              "Employee ID": "EMP003",
              "Employer": "Minet Insurance",
              "Bank Name": "NCBA Bank",
              "Bank Account": "3456789012",
              "Next of Kin": "Mary Kamau",
              "NOK Phone": "0745678901",
            },
          ];
          filename = "Member_Registration_Template.xlsx";
          sheetName = "Members";
          break;

        case "LOAN_APPLICATIONS":
          data = [
            {
              "Member Number": "M-2026-001",
              "Loan Product": "Personal Loan",
              "Amount": 50000,
              "Term Months": 12,
              "Purpose": "Business",
              "Guarantor 1": "M-2026-002",
              "Guarantor 2": "M-2026-003",
            },
            {
              "Member Number": "M-2026-002",
              "Loan Product": "Business Loan",
              "Amount": 100000,
              "Term Months": 24,
              "Purpose": "Expansion",
              "Guarantor 1": "M-2026-001",
              "Guarantor 2": "M-2026-004",
            },
            {
              "Member Number": "M-2026-003",
              "Loan Product": "Emergency Loan",
              "Amount": 25000,
              "Term Months": 6,
              "Purpose": "Medical",
              "Guarantor 1": "M-2026-004",
              "Guarantor 2": "",
            },
          ];
          filename = "Loan_Applications_Template.xlsx";
          sheetName = "Loans";
          break;

        default:
          data = [
            {
              "Member Number": "M-2026-001",
              "Savings Amount": 5000,
              "Shares Amount": 2000,
              "Loan Repayment Amount": 3000,
              "Loan Number": "LN-2026-001",
            },
          ];
          filename = "Bulk_Template.xlsx";
      }

      // Create workbook and worksheet
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, sheetName);

      // Add instructions sheet for member registration
      if (batchType === "MEMBER_REGISTRATION") {
        const instructionsData = [
          ["IMPORTANT: Date Format Instructions"],
          [""],
          ["The 'Date of Birth' column must be in one of these formats:"],
          ["• YYYY-MM-DD (e.g., 1990-01-15) - RECOMMENDED"],
          ["• DD/MM/YYYY (e.g., 15/01/1990)"],
          ["• MM/DD/YYYY (e.g., 01/15/1990)"],
          [""],
          ["⚠️ IMPORTANT: If Excel auto-formats the date to M/D/YYYY (e.g., 1/15/1990):"],
          ["1. Right-click the Date of Birth column"],
          ["2. Select 'Format Cells'"],
          ["3. Choose 'Text' format"],
          ["4. Re-enter the dates in YYYY-MM-DD format"],
          ["5. Save and upload"],
          [""],
          ["This prevents Excel from auto-converting your dates to system locale format."],
        ];
        
        const wsInstructions = XLSX.utils.aoa_to_sheet(instructionsData);
        XLSX.utils.book_append_sheet(wb, wsInstructions, "Instructions");
      }

      // Auto-size columns
      const colWidths = Object.keys(data[0] || {}).map(() => 18);
      ws["!cols"] = colWidths.map((width) => ({ wch: width }));

      // Download
      XLSX.writeFile(wb, filename);

      toast({
        title: "Template Downloaded",
        description: batchType === "MEMBER_REGISTRATION" 
          ? "Member Registration template downloaded. See 'Instructions' sheet for date format guidance."
          : `${batchType.replace(/_/g, " ")} template downloaded as Excel file. Fill in your data and upload.`,
      });
    });
  };


  const canApproveBatch = (batch: BulkBatch) => {
    if (batch.status !== "PENDING" || batch.uploadedByUsername === user?.username) {
      return false;
    }

    // Loan batches require CREDIT_COMMITTEE approval
    if (batch.batchType === "LOAN_APPLICATIONS" || batch.batchType === "LOAN_DISBURSEMENTS") {
      return user?.role === "CREDIT_COMMITTEE";
    }

    // Other batches (contributions, member registration) require TREASURER approval
    return user?.role === "TREASURER";
  };

  return (
    <div className="w-full p-4">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h1 className="text-2xl font-bold">Bulk Processing</h1>
          <p className="text-gray-600 text-sm mt-1">
            {user?.role === "CREDIT_COMMITTEE" 
              ? "Review and approve loan batches" 
              : "Upload bulk transactions and migrate historical loan data during digitization"}
          </p>
        </div>
        <div className="flex gap-2">
          {user?.role === "TREASURER" && (
            <>
              <Button variant="outline" onClick={downloadTemplate}>
                <FileDown className="mr-2 h-4 w-4" />
                Download Template
              </Button>
              <Dialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
                <DialogTrigger asChild>
                  <Button>
                    <Upload className="mr-2 h-4 w-4" />
                    Upload Batch
                  </Button>
                </DialogTrigger>

      {/* Instructions Card - Dynamic based on batch type - Only for TREASURER */}
      {user?.role === "TREASURER" && (
      <Card className="mb-4 border-blue-200 bg-blue-50">
        <CardHeader className="py-3 px-4">
          <CardTitle className="flex items-center text-blue-900 text-base">
            <Info className="mr-2 h-4 w-4" />
            Template Instructions
          </CardTitle>
        </CardHeader>
        <CardContent className="text-xs space-y-2 py-3 px-4">
          <Tabs value={batchType} onValueChange={setBatchType} className="w-full">
            <TabsList className="grid w-full grid-cols-3 h-8">
              <TabsTrigger value="MONTHLY_CONTRIBUTIONS" className="text-xs">Monthly</TabsTrigger>
              <TabsTrigger value="MEMBER_REGISTRATION" className="text-xs">Members</TabsTrigger>
              <TabsTrigger value="LOAN_APPLICATIONS" className="text-xs">Loan Migration</TabsTrigger>
            </TabsList>

            <TabsContent value="MONTHLY_CONTRIBUTIONS" className="mt-2">
              <div className="grid md:grid-cols-2 gap-2">
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Columns:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• <strong>Employee ID:</strong> EMP001, EMP002, etc.</li>
                    <li>• <strong>Savings:</strong> Amount (e.g., 5000)</li>
                    <li>• <strong>Shares:</strong> Amount (e.g., 2000)</li>
                    {enabledFunds.length > 0 ? (
                      enabledFunds.map((fund) => (
                        <li key={fund.id}>• <strong>{fund.displayName}:</strong> Optional</li>
                      ))
                    ) : (
                      <li className="text-gray-500 italic">No optional funds enabled</li>
                    )}
                  </ul>
                </div>
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Rules:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• Member must be APPROVED</li>
                    <li>• Savings OR Shares must be &gt; 0</li>
                    <li>• All amounts optional (use 0 or blank)</li>
                    <li>• Loan fields OPTIONAL</li>
                    <li>• Max per contribution: KES 1M</li>
                    <li>• Max file size: 5MB</li>
                  </ul>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="LOAN_DISBURSEMENTS" className="mt-2">
              <div className="grid md:grid-cols-2 gap-2">
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Columns:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• <strong>Member Number:</strong> M-YYYY-XXXXX</li>
                    <li>• <strong>Loan Number:</strong> LN-YYYY-XXXXX</li>
                    <li>• <strong>Amount:</strong> Numbers only</li>
                    <li>• <strong>Account:</strong> SAVINGS or SHARES</li>
                  </ul>
                </div>
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Rules:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• Loan must be APPROVED</li>
                    <li>• Member must be APPROVED</li>
                    <li>• Amount must match loan</li>
                    <li>• No currency symbols</li>
                    <li>• Max file size: 5MB</li>
                  </ul>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="MEMBER_REGISTRATION" className="mt-2">
              <div className="grid md:grid-cols-2 gap-2">
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Columns:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• First Name, Last Name, Email</li>
                    <li>• Phone (10 digits)</li>
                    <li>• National ID (8 digits)</li>
                    <li>• Date of Birth (YYYY-MM-DD)</li>
                    <li>• Department, Employer</li>
                    <li>• Bank details, Next of Kin</li>
                  </ul>
                </div>
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Rules:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• Members created as ACTIVE</li>
                    <li>• Member numbers auto-assigned</li>
                    <li>• Email &amp; ID must be unique</li>
                    <li>• Phone must be valid</li>
                    <li>• Member must be 18+</li>
                    <li>• Max file size: 5MB</li>
                  </ul>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="LOAN_APPLICATIONS" className="mt-2">
              <Alert className="mb-2 border-amber-200 bg-amber-50 py-2">
                <Info className="h-4 w-4 text-amber-600" />
                <AlertDescription className="text-amber-800 text-xs">
                  <strong>Historical Data Migration Only.</strong> Use this to import existing loans during system digitization. Once the member mobile app is live, members will apply directly through the app.
                </AlertDescription>
              </Alert>
              <div className="grid md:grid-cols-2 gap-2">
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Columns:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• <strong>Member Number:</strong> M-YYYY-XXXXX</li>
                    <li>• <strong>Loan Product:</strong> Product name</li>
                    <li>• <strong>Amount:</strong> Numbers only</li>
                    <li>• <strong>Purpose:</strong> Loan purpose</li>
                    <li>• <strong>Guarantor 1 &amp; 2:</strong> Member numbers</li>
                  </ul>
                </div>
                <div>
                  <p className="font-semibold text-blue-900 mb-1">Rules:</p>
                  <ul className="space-y-0.5 text-blue-800">
                    <li>• Member must be APPROVED</li>
                    <li>• Product must exist &amp; active</li>
                    <li>• Amount within product limits</li>
                    <li>• Guarantors must be APPROVED</li>
                    <li>• At least 1 guarantor required</li>
                    <li>• Max file size: 5MB</li>
                  </ul>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
      )}

          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Upload Bulk Transaction File</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <Alert>
                <FileSpreadsheet className="h-4 w-4" />
                <AlertDescription>
                  Upload an Excel (.xlsx, .xls) or CSV file with transaction data. Maximum file size: 5MB.
                  <br />
                  <button
                    onClick={downloadTemplate}
                    className="text-blue-600 hover:underline mt-2 inline-flex items-center"
                  >
                    <FileDown className="h-3 w-3 mr-1" />
                    Download template to get started
                  </button>
                </AlertDescription>
              </Alert>

              <div className="space-y-2">
                <Label htmlFor="batchType">Batch Type</Label>
                <Select value={batchType} onValueChange={setBatchType}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MONTHLY_CONTRIBUTIONS">Monthly Contributions</SelectItem>
                    <SelectItem value="MEMBER_REGISTRATION">Member Registration</SelectItem>
                    <SelectItem value="LOAN_APPLICATIONS">Loan Migration (Historical Data)</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-gray-600">
                  Select the type of bulk operation you want to perform.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="file">Select File</Label>
                <input
                  id="file"
                  type="file"
                  accept=".xlsx,.xls,.csv"
                  onChange={handleFileSelect}
                  className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                />
                {selectedFile && (
                  <p className="text-sm text-gray-600">
                    Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(2)} KB)
                  </p>
                )}
              </div>

              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  <strong>Excel Template Format (10 columns):</strong>
                  <br />
                  Employee ID | Savings | Shares | Loan Repayment | Loan Number | Benevolent | Development | School Fees | Holiday | Emergency
                  <br />
                  <span className="text-xs text-gray-600">All fund contributions are optional. Use 0 or leave blank if not applicable.</span>
                </AlertDescription>
              </Alert>

              <div className="flex justify-end space-x-2">
                <Button variant="outline" onClick={() => setUploadDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleUpload} disabled={!selectedFile || loading}>
                  {loading ? "Uploading..." : "Upload"}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
            </>
          )}
        </div>
      </div>

      {/* Disbursement Section for TREASURER */}
      {user?.role === "TREASURER" && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Loan Disbursement - Approved Loans Ready for Disbursement</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {approvedLoanItems.length === 0 ? (
              <p className="text-gray-600">No approved loans available for disbursement</p>
            ) : (
              <>
                {/* Bulk Disbursement Actions */}
                <div className="flex gap-2 p-4 bg-green-50 rounded-lg border border-green-200">
                  <Button
                    onClick={() => {
                      fetchApprovedLoans();
                    }}
                    variant="outline"
                    className="text-xs"
                  >
                    Refresh
                  </Button>
                  <Button
                    onClick={handleBulkDisburseLoanItems}
                    disabled={selectedDisbursementItemIds.size === 0 || loading}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    Bulk Disburse ({selectedDisbursementItemIds.size} selected)
                  </Button>
                  {selectedDisbursementItemIds.size > 0 && (
                    <Button
                      variant="outline"
                      onClick={() => setSelectedDisbursementItemIds(new Set())}
                    >
                      Clear Selection
                    </Button>
                  )}
                </div>

                {/* Approved Loans Table */}
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-12">
                          <input
                            type="checkbox"
                            checked={selectedDisbursementItemIds.size === approvedLoanItems.length && approvedLoanItems.length > 0}
                            onChange={toggleAllDisbursementItemsSelection}
                            className="rounded"
                          />
                        </TableHead>
                        <TableHead>Member</TableHead>
                        <TableHead>Loan Product</TableHead>
                        <TableHead>Amount</TableHead>
                        <TableHead>Purpose</TableHead>
                        <TableHead>Guarantor 1</TableHead>
                        <TableHead>Guarantor 2</TableHead>
                        <TableHead>Status</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {approvedLoanItems.map((item) => (
                        <TableRow key={item.id}>
                          <TableCell>
                            <input
                              type="checkbox"
                              checked={selectedDisbursementItemIds.has(item.id)}
                              onChange={() => toggleDisbursementItemSelection(item.id)}
                              className="rounded"
                            />
                          </TableCell>
                          <TableCell className="font-medium">{item.memberNumber}</TableCell>
                          <TableCell>{item.loanProductName}</TableCell>
                          <TableCell className="text-xs">{formatCurrency(item.amount)}</TableCell>
                          <TableCell className="text-xs">{item.purpose}</TableCell>
                          <TableCell className="text-xs">{item.guarantor1 || "-"}</TableCell>
                          <TableCell className="text-xs">{item.guarantor2 || "-"}</TableCell>
                          <TableCell>
                            <Badge className="bg-green-100 text-green-800">Approved</Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="all" className="space-y-4">
        <TabsList>
          <TabsTrigger value="all">All Batches</TabsTrigger>
          <TabsTrigger value="pending">Pending Approval</TabsTrigger>
          <TabsTrigger value="completed">Completed</TabsTrigger>
        </TabsList>

        <TabsContent value="all">
          <Card>
            <CardHeader>
              <CardTitle>All Batches</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Search and Filter Controls */}
              <div className="flex flex-col md:flex-row gap-2 items-end mb-3">
                <div className="flex-1">
                  <label className="text-xs font-medium text-gray-700 mb-1 block">Search</label>
                  <input
                    type="text"
                    placeholder="Search batches..."
                    value={searchQuery}
                    onChange={(e) => handleSearch(e.target.value)}
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-700 mb-1 block">Per Page</label>
                  <select
                    value={pageSize}
                    onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                    className="px-2 py-1.5 text-sm border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value={5}>5</option>
                    <option value={10}>10</option>
                    <option value={25}>25</option>
                    <option value={50}>50</option>
                    <option value={100}>100</option>
                  </select>
                </div>
              </div>

              {/* Results Info */}
              <div className="text-xs text-gray-600 mb-2">
                Showing {startIndex + 1} to {Math.min(endIndex, filteredBatches.length)} of {filteredBatches.length} batches
                {searchQuery && ` (filtered from ${batches.length} total)`}
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
              <Table className="text-xs">
                <TableHeader>
                  <TableRow>
                    <TableHead className="px-2 py-2">Batch #</TableHead>
                    <TableHead className="px-2 py-2">Type</TableHead>
                    <TableHead className="px-2 py-2">File</TableHead>
                    <TableHead className="px-2 py-2 text-right">Records</TableHead>
                    <TableHead className="px-2 py-2 text-right">Amount</TableHead>
                    <TableHead className="px-2 py-2">Status</TableHead>
                    <TableHead className="px-2 py-2">By</TableHead>
                    <TableHead className="px-2 py-2">Date</TableHead>
                    <TableHead className="px-2 py-2">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedBatches.map((batch) => (
                    <TableRow key={batch.id} className="text-xs">
                      <TableCell className="px-2 py-2 font-medium truncate max-w-xs">{batch.batchNumber}</TableCell>
                      <TableCell className="px-2 py-2 truncate">{formatBatchType(batch.batchType)}</TableCell>
                      <TableCell className="px-2 py-2 truncate max-w-xs text-xs">{batch.fileName}</TableCell>
                      <TableCell className="px-2 py-2 text-right">
                        {batch.totalRecords}
                        {batch.status === "COMPLETED" || batch.status === "PARTIALLY_COMPLETED" ? (
                          <span className="text-xs text-gray-500 ml-1">
                            ({batch.successfulRecords}✓/{batch.failedRecords}✗)
                          </span>
                        ) : null}
                      </TableCell>
                      <TableCell className="px-2 py-2 text-right">{formatCurrency(batch.totalAmount)}</TableCell>
                      <TableCell className="px-2 py-2">
                        <Badge className={`${statusColors[batch.status]} text-xs py-0.5`}>{batch.status.replace(/_/g, " ")}</Badge>
                      </TableCell>
                      <TableCell className="px-2 py-2 truncate text-xs">{batch.uploadedByUsername}</TableCell>
                      <TableCell className="px-2 py-2 text-xs whitespace-nowrap">{new Date(batch.uploadedAt).toLocaleDateString()}</TableCell>
                      <TableCell className="px-2 py-2">
                        <Button variant="outline" size="sm" onClick={() => viewBatchDetails(batch)} className="text-xs h-7 px-2">
                          <Eye className="h-3 w-3 mr-1" />
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {paginatedBatches.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={9} className="text-center text-gray-500 py-4 text-xs">
                        {searchQuery ? "No batches match your search criteria." : "No batches found. Upload your first batch to get started."}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-3">
                  <div className="text-xs text-gray-600">
                    Page {currentPage} of {totalPages}
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                      disabled={currentPage === 1}
                      className="h-7 px-2 text-xs"
                    >
                      Prev
                    </Button>
                    <div className="flex gap-0.5">
                      {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                        let page;
                        if (totalPages <= 5) {
                          page = i + 1;
                        } else if (currentPage <= 3) {
                          page = i + 1;
                        } else if (currentPage >= totalPages - 2) {
                          page = totalPages - 4 + i;
                        } else {
                          page = currentPage - 2 + i;
                        }
                        return (
                          <Button
                            key={page}
                            variant={currentPage === page ? "default" : "outline"}
                            size="sm"
                            onClick={() => setCurrentPage(page)}
                            className="w-7 h-7 p-0 text-xs"
                          >
                            {page}
                          </Button>
                        );
                      })}
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                      disabled={currentPage === totalPages}
                      className="h-7 px-2 text-xs"
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="pending">
          <Card>
            <CardHeader className="py-3 px-4">
              <CardTitle className="text-base">Pending Approval</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 py-3 px-4">
              {/* Filter Controls */}
              <div>
                <label className="text-xs font-medium text-gray-700 mb-1 block">Per Page</label>
                <select
                  value={pendingPageSize}
                  onChange={(e) => handlePendingPageSizeChange(Number(e.target.value))}
                  className="px-2 py-1.5 text-sm border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value={5}>5</option>
                  <option value={10}>10</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>

              {/* Results Info */}
              <div className="text-xs text-gray-600">
                Showing {pendingStartIndex + 1} to {Math.min(pendingEndIndex, pendingBatches.length)} of {pendingBatches.length} pending batches
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
              <Table className="text-xs">
                <TableHeader>
                  <TableRow>
                    <TableHead className="px-2 py-2">Batch #</TableHead>
                    <TableHead className="px-2 py-2">Type</TableHead>
                    <TableHead className="px-2 py-2 text-right">Records</TableHead>
                    <TableHead className="px-2 py-2 text-right">Amount</TableHead>
                    <TableHead className="px-2 py-2">By</TableHead>
                    <TableHead className="px-2 py-2">Date</TableHead>
                    <TableHead className="px-2 py-2">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedPendingBatches.map((batch) => (
                    <TableRow key={batch.id} className="text-xs">
                      <TableCell className="px-2 py-2 font-medium truncate max-w-xs">{batch.batchNumber}</TableCell>
                      <TableCell className="px-2 py-2 truncate">{formatBatchType(batch.batchType)}</TableCell>
                      <TableCell className="px-2 py-2 text-right">{batch.totalRecords}</TableCell>
                      <TableCell className="px-2 py-2 text-right">{formatCurrency(batch.totalAmount)}</TableCell>
                      <TableCell className="px-2 py-2 truncate text-xs">{batch.uploadedByUsername}</TableCell>
                      <TableCell className="px-2 py-2 text-xs whitespace-nowrap">{new Date(batch.uploadedAt).toLocaleDateString()}</TableCell>
                      <TableCell className="px-2 py-2">
                        <Button variant="outline" size="sm" onClick={() => viewBatchDetails(batch)} className="text-xs h-7 px-2">
                          <Eye className="h-3 w-3 mr-1" />
                          Review
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {paginatedPendingBatches.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-gray-500 py-4 text-xs">
                        No pending batches
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
              </div>

              {/* Pagination Controls */}
              {pendingTotalPages > 1 && (
                <div className="flex items-center justify-between mt-3">
                  <div className="text-xs text-gray-600">
                    Page {pendingCurrentPage} of {pendingTotalPages}
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPendingCurrentPage(Math.max(1, pendingCurrentPage - 1))}
                      disabled={pendingCurrentPage === 1}
                      className="h-7 px-2 text-xs"
                    >
                      Prev
                    </Button>
                    <div className="flex gap-0.5">
                      {Array.from({ length: Math.min(5, pendingTotalPages) }, (_, i) => {
                        let page;
                        if (pendingTotalPages <= 5) {
                          page = i + 1;
                        } else if (pendingCurrentPage <= 3) {
                          page = i + 1;
                        } else if (pendingCurrentPage >= pendingTotalPages - 2) {
                          page = pendingTotalPages - 4 + i;
                        } else {
                          page = pendingCurrentPage - 2 + i;
                        }
                        return (
                          <Button
                            key={page}
                            variant={pendingCurrentPage === page ? "default" : "outline"}
                            size="sm"
                            onClick={() => setPendingCurrentPage(page)}
                            className="w-7 h-7 p-0 text-xs"
                          >
                            {page}
                          </Button>
                        );
                      })}
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPendingCurrentPage(Math.min(pendingTotalPages, pendingCurrentPage + 1))}
                      disabled={pendingCurrentPage === pendingTotalPages}
                      className="h-7 px-2 text-xs"
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="completed">
          <Card>
            <CardHeader className="py-3 px-4">
              <CardTitle className="text-base">Completed Batches</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 py-3 px-4">
              {/* Filter Controls */}
              <div>
                <label className="text-xs font-medium text-gray-700 mb-1 block">Per Page</label>
                <select
                  value={completedPageSize}
                  onChange={(e) => handleCompletedPageSizeChange(Number(e.target.value))}
                  className="px-2 py-1.5 text-sm border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value={5}>5</option>
                  <option value={10}>10</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>

              {/* Results Info */}
              <div className="text-xs text-gray-600">
                Showing {completedStartIndex + 1} to {Math.min(completedEndIndex, completedBatches.length)} of {completedBatches.length} completed batches
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
              <Table className="text-xs">
                <TableHeader>
                  <TableRow>
                    <TableHead className="px-2 py-2">Batch #</TableHead>
                    <TableHead className="px-2 py-2">Type</TableHead>
                    <TableHead className="px-2 py-2 text-right">Records</TableHead>
                    <TableHead className="px-2 py-2">Success/Failed</TableHead>
                    <TableHead className="px-2 py-2 text-right">Amount</TableHead>
                    <TableHead className="px-2 py-2">Processed</TableHead>
                    <TableHead className="px-2 py-2">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedCompletedBatches.map((batch) => (
                    <TableRow key={batch.id} className="text-xs">
                      <TableCell className="px-2 py-2 font-medium truncate max-w-xs">{batch.batchNumber}</TableCell>
                      <TableCell className="px-2 py-2 truncate">{formatBatchType(batch.batchType)}</TableCell>
                      <TableCell className="px-2 py-2 text-right">{batch.totalRecords}</TableCell>
                      <TableCell className="px-2 py-2">
                        <span className="text-green-600">{batch.successfulRecords}✓</span> / <span className="text-red-600">{batch.failedRecords}✗</span>
                      </TableCell>
                      <TableCell className="px-2 py-2 text-right">{formatCurrency(batch.totalAmount)}</TableCell>
                      <TableCell className="px-2 py-2 text-xs whitespace-nowrap">{batch.processedAt ? new Date(batch.processedAt).toLocaleDateString() : "-"}</TableCell>
                      <TableCell className="px-2 py-2">
                        <Button variant="outline" size="sm" onClick={() => viewBatchDetails(batch)} className="text-xs h-7 px-2">
                          <Eye className="h-3 w-3 mr-1" />
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {paginatedCompletedBatches.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-gray-500 py-4 text-xs">
                        No completed batches
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
              </div>

              {/* Pagination Controls */}
              {completedTotalPages > 1 && (
                <div className="flex items-center justify-between mt-3">
                  <div className="text-xs text-gray-600">
                    Page {completedCurrentPage} of {completedTotalPages}
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCompletedCurrentPage(Math.max(1, completedCurrentPage - 1))}
                      disabled={completedCurrentPage === 1}
                      className="h-7 px-2 text-xs"
                    >
                      Prev
                    </Button>
                    <div className="flex gap-0.5">
                      {Array.from({ length: Math.min(5, completedTotalPages) }, (_, i) => {
                        let page;
                        if (completedTotalPages <= 5) {
                          page = i + 1;
                        } else if (completedCurrentPage <= 3) {
                          page = i + 1;
                        } else if (completedCurrentPage >= completedTotalPages - 2) {
                          page = completedTotalPages - 4 + i;
                        } else {
                          page = completedCurrentPage - 2 + i;
                        }
                        return (
                          <Button
                            key={page}
                            variant={completedCurrentPage === page ? "default" : "outline"}
                            size="sm"
                            onClick={() => setCompletedCurrentPage(page)}
                            className="w-7 h-7 p-0 text-xs"
                          >
                            {page}
                          </Button>
                        );
                      })}
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCompletedCurrentPage(Math.min(completedTotalPages, completedCurrentPage + 1))}
                      disabled={completedCurrentPage === completedTotalPages}
                      className="h-7 px-2 text-xs"
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Batch Details Dialog */}
      <Dialog open={detailsDialogOpen} onOpenChange={setDetailsDialogOpen}>
        <DialogContent className="max-w-7xl max-h-[85vh] overflow-y-auto p-4">
          <DialogHeader className="pb-2">
            <DialogTitle className="text-lg">Batch Details: {selectedBatch?.batchNumber}</DialogTitle>
          </DialogHeader>
          {selectedBatch && (
            <div className="space-y-3">
              {/* Batch Summary - Compact */}
              <Card className="p-3">
                <div className="grid grid-cols-2 md:grid-cols-5 gap-2 text-sm">
                  <div>
                    <p className="text-xs text-gray-600">Type</p>
                    <p className="font-medium text-sm">{formatBatchType(selectedBatch.batchType)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Records</p>
                    <p className="font-medium text-sm">{selectedBatch.totalRecords}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Amount</p>
                    <p className="font-medium text-sm">{formatCurrency(selectedBatch.totalAmount)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">Status</p>
                    <Badge className={`${statusColors[selectedBatch.status]} text-xs py-0.5`}>
                      {selectedBatch.status.replace(/_/g, " ")}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-xs text-gray-600">By</p>
                    <p className="font-medium text-sm">{selectedBatch.uploadedByUsername}</p>
                  </div>
                </div>

                {selectedBatch.status === "COMPLETED" || selectedBatch.status === "PARTIALLY_COMPLETED" ? (
                  <div className="mt-2 p-2 bg-gray-50 rounded flex justify-around text-xs">
                    <div className="text-center">
                      <p className="text-gray-600">Successful</p>
                      <p className="font-bold text-green-600">{selectedBatch.successfulRecords}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-gray-600">Failed</p>
                      <p className="font-bold text-red-600">{selectedBatch.failedRecords}</p>
                    </div>
                  </div>
                ) : null}
              </Card>

              {/* Transaction Items */}
              <Card className="p-3">
                <p className="font-semibold text-sm mb-2">
                  {selectedBatch?.batchType === "MEMBER_REGISTRATION" 
                    ? "Member Items" 
                    : selectedBatch?.batchType === "LOAN_APPLICATIONS"
                    ? "Loan Application Items"
                    : "Transaction Items"}
                </p>
                <div className="overflow-x-auto text-xs">
                  {selectedBatch?.batchType === "MONTHLY_CONTRIBUTIONS" && (
                    <Table className="text-xs">
                      <TableHeader>
                        <TableRow>
                            <TableHead>Row</TableHead>
                            <TableHead>Member</TableHead>
                            <TableHead>Savings</TableHead>
                            <TableHead>Shares</TableHead>
                            <TableHead>Loan</TableHead>
                            <TableHead>Loan #</TableHead>
                            {enabledFunds.map((fund) => (
                              <TableHead key={fund.id}>{fund.displayName.substring(0, 8)}</TableHead>
                            ))}
                            <TableHead>Total</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Error</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {batchItems.map((item) => {
                            // Calculate total including all fund amounts
                            let total = (item.savingsAmount || 0) + 
                                       (item.sharesAmount || 0) + 
                                       (item.loanRepaymentAmount || 0);
                            
                            // Add dynamic fund amounts
                            enabledFunds.forEach((fund) => {
                              const fundKey = fund.fundType.toLowerCase() + "FundAmount";
                              const fundAmount = (item as any)[fundKey] || 0;
                              total += fundAmount;
                            });

                            return (
                              <TableRow key={item.id}>
                                <TableCell>{item.rowNumber}</TableCell>
                                <TableCell className="font-medium">{item.memberNumber}</TableCell>
                                <TableCell className="text-xs">{item.savingsAmount > 0 ? formatCurrency(item.savingsAmount) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.sharesAmount > 0 ? formatCurrency(item.sharesAmount) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.loanRepaymentAmount > 0 ? formatCurrency(item.loanRepaymentAmount) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.loanNumber || "-"}</TableCell>
                                {enabledFunds.map((fund) => {
                                  const fundKey = fund.fundType.toLowerCase() + "FundAmount";
                                  const fundAmount = (item as any)[fundKey] || 0;
                                  return (
                                    <TableCell key={fund.id} className="text-xs">
                                      {fundAmount > 0 ? formatCurrency(fundAmount) : "-"}
                                    </TableCell>
                                  );
                                })}
                                <TableCell className="font-semibold text-xs">{formatCurrency(total)}</TableCell>
                                <TableCell>
                                  {item.status === "SUCCESS" ? (
                                    <Badge className="bg-green-100 text-green-800">Success</Badge>
                                  ) : item.status === "FAILED" ? (
                                    <Badge className="bg-red-100 text-red-800">Failed</Badge>
                                  ) : (
                                    <Badge className="bg-gray-100 text-gray-800">Pending</Badge>
                                  )}
                                </TableCell>
                                <TableCell className="max-w-xs truncate text-xs" title={item.errorMessage}>
                                  {item.errorMessage || "-"}
                                </TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    )}

                    {selectedBatch?.batchType === "MEMBER_REGISTRATION" && (
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Row</TableHead>
                            <TableHead>First Name</TableHead>
                            <TableHead>Last Name</TableHead>
                            <TableHead>Email</TableHead>
                            <TableHead>Phone</TableHead>
                            <TableHead>National ID</TableHead>
                            <TableHead>DOB</TableHead>
                            <TableHead>Department</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Error</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {memberItems.map((item) => (
                            <TableRow key={item.id}>
                              <TableCell>{item.rowNumber}</TableCell>
                              <TableCell>{item.firstName}</TableCell>
                              <TableCell>{item.lastName}</TableCell>
                              <TableCell className="text-xs">{item.email}</TableCell>
                              <TableCell className="text-xs">{item.phone}</TableCell>
                              <TableCell className="text-xs">{item.nationalId}</TableCell>
                              <TableCell className="text-xs">{item.dateOfBirth}</TableCell>
                              <TableCell className="text-xs">{item.department}</TableCell>
                              <TableCell>
                                {item.status === "SUCCESS" ? (
                                  <Badge className="bg-green-100 text-green-800">Success</Badge>
                                ) : item.status === "FAILED" ? (
                                  <Badge className="bg-red-100 text-red-800">Failed</Badge>
                                ) : (
                                  <Badge className="bg-gray-100 text-gray-800">Pending</Badge>
                                )}
                              </TableCell>
                              <TableCell className="max-w-xs truncate text-xs" title={item.errorMessage}>
                                {item.errorMessage || "-"}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    )}
                    {selectedBatch?.batchType === "LOAN_APPLICATIONS" && (
                      <div className="space-y-4">
                        {/* Individual Approval Only - No Bulk Approve */}
                        {user?.role === "CREDIT_COMMITTEE" && (
                          <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-800">
                            <p className="font-semibold">Individual Approval Required</p>
                            <p className="text-xs mt-1">Each loan must be approved individually by clicking the checkmark icon. This ensures enhanced verification and due diligence.</p>
                          </div>
                        )}

                        <Table>
                          <TableHeader>
                            <TableRow>
                              {user?.role === "CREDIT_COMMITTEE" && (
                                <TableHead className="w-12">
                                  <input
                                    type="checkbox"
                                    checked={selectedLoanItemIds.size === loanItems.filter(item => item.status === "PENDING").length && loanItems.filter(item => item.status === "PENDING").length > 0}
                                    onChange={toggleAllLoanItemsSelection}
                                    className="rounded"
                                  />
                                </TableHead>
                              )}
                              <TableHead>Row</TableHead>
                              <TableHead>Member</TableHead>
                              <TableHead>Loan Product</TableHead>
                              <TableHead>Amount</TableHead>
                              <TableHead>Interest</TableHead>
                              <TableHead>Total Repayable</TableHead>
                              <TableHead>Monthly</TableHead>
                              <TableHead>Purpose</TableHead>
                              <TableHead>Guarantor 1</TableHead>
                              <TableHead>Guarantor 2</TableHead>
                              <TableHead>Status</TableHead>
                              <TableHead>Error</TableHead>
                              <TableHead className="whitespace-nowrap">Actions</TableHead>
                            </TableRow>
                          </TableHeader>
                          <TableBody>
                            {loanItems.map((item) => (
                              <TableRow key={item.id}>
                                {user?.role === "CREDIT_COMMITTEE" && (
                                  <TableCell>
                                    {item.status === "PENDING" && (
                                      <input
                                        type="checkbox"
                                        checked={selectedLoanItemIds.has(item.id)}
                                        onChange={() => toggleLoanItemSelection(item.id)}
                                        className="rounded"
                                      />
                                    )}
                                  </TableCell>
                                )}
                                <TableCell>{item.rowNumber}</TableCell>
                                <TableCell className="font-medium">{item.memberNumber}</TableCell>
                                <TableCell>{item.loanProductName}</TableCell>
                                <TableCell className="text-xs">{formatCurrency(item.amount)}</TableCell>
                                <TableCell className="text-xs">{item.totalInterest ? formatCurrency(item.totalInterest) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.totalRepayable ? formatCurrency(item.totalRepayable) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.monthlyRepayment ? formatCurrency(item.monthlyRepayment) : "-"}</TableCell>
                                <TableCell className="text-xs">{item.purpose}</TableCell>
                                <TableCell className="text-xs">{item.guarantor1 || "-"}</TableCell>
                                <TableCell className="text-xs">{item.guarantor2 || "-"}</TableCell>
                                <TableCell>
                                  {item.status === "SUCCESS" ? (
                                    <Badge className="bg-green-100 text-green-800">Success</Badge>
                                  ) : item.status === "FAILED" ? (
                                    <Badge className="bg-red-100 text-red-800">Failed</Badge>
                                  ) : item.status === "APPROVED" ? (
                                    <Badge className="bg-green-100 text-green-800">Approved</Badge>
                                  ) : item.status === "FLAGGED" ? (
                                    <Badge className="bg-orange-100 text-orange-800">Flagged</Badge>
                                  ) : item.status === "CONDITIONAL_APPROVAL" ? (
                                    <Badge className="bg-yellow-100 text-yellow-800">Conditional</Badge>
                                  ) : item.status === "REJECTED" ? (
                                    <Badge className="bg-red-100 text-red-800">Rejected</Badge>
                                  ) : (
                                    <Badge className="bg-gray-100 text-gray-800">Pending</Badge>
                                  )}
                                </TableCell>
                                <TableCell className="max-w-xs truncate text-xs" title={item.errorMessage}>
                                  {item.errorMessage || "-"}
                                </TableCell>
                                <TableCell>
                                  {user?.role === "CREDIT_COMMITTEE" && (item.status === "Pending" || item.status === "PENDING") && (
                                    <div className="flex gap-1">
                                      <Button 
                                        variant="outline" 
                                        size="sm" 
                                        onClick={() => validateLoanGuarantors(item.id)}
                                        disabled={loading}
                                        className="text-xs"
                                        title="View member and guarantor eligibility"
                                      >
                                        <Eye className="h-3 w-3" />
                                      </Button>
                                      <Button 
                                        variant="outline" 
                                        size="sm" 
                                        onClick={() => handleApproveLoanItem(item.id)}
                                        disabled={loading}
                                        className="text-xs text-green-600 hover:text-green-700"
                                        title="Approve this loan"
                                      >
                                        <CheckCircle className="h-3 w-3" />
                                      </Button>
                                      <Button 
                                      variant="outline" 
                                      size="sm" 
                                      onClick={() => {
                                        setSelectedLoanItemId(item.id);
                                        setRejectLoanItemDialogOpen(true);
                                      }}
                                      disabled={loading}
                                      className="text-xs text-red-600 hover:text-red-700"
                                      title="Reject this loan"
                                    >
                                      <XCircle className="h-3 w-3" />
                                    </Button>
                                  </div>
                                )}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                      </div>
                    )}
                  </div>
              </Card>

              {/* Batch-level approval removed - only individual item approval allowed */}
              {selectedBatch.status === "PENDING" && (
                <Alert className="border-blue-200 bg-blue-50">
                  <AlertCircle className="h-4 w-4 text-blue-600" />
                  <AlertDescription className="text-blue-800">
                    <strong>Individual Approval Only:</strong> Each loan item must be approved individually by clicking the checkmark icon. This ensures enhanced verification and due diligence.
                  </AlertDescription>
                </Alert>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Reject Loan Item Dialog */}
      <Dialog open={rejectLoanItemDialogOpen} onOpenChange={setRejectLoanItemDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject Loan Item</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="loanItemRejectionReason">Rejection Reason</Label>
              <Textarea
                id="loanItemRejectionReason"
                placeholder="Provide a reason for rejecting this loan item..."
                value={loanItemRejectionReason}
                onChange={(e) => setLoanItemRejectionReason(e.target.value)}
                rows={4}
              />
            </div>
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => {
                setRejectLoanItemDialogOpen(false);
                setLoanItemRejectionReason("");
                setSelectedLoanItemId(null);
              }}>
                Cancel
              </Button>
              <Button variant="destructive" onClick={handleRejectLoanItem} disabled={loading || !loanItemRejectionReason.trim()}>
                {loading ? "Rejecting..." : "Reject Item"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Guarantor Validation Dialog */}
      <Dialog open={guarantorValidationDialogOpen} onOpenChange={(open) => {
        setGuarantorValidationDialogOpen(open);
        if (!open) {
          setGuarantorValidationResult(null); // Clear data when closing
        }
      }}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Loan Application Review - Member & Guarantor Eligibility</DialogTitle>
          </DialogHeader>
          {guarantorValidationResult && (
            <div className="space-y-6">
              {/* Product Validation Alert */}
              {guarantorValidationResult.productEnabled === false && (
                <Alert className="border-red-200 bg-red-50">
                  <AlertCircle className="h-4 w-4 text-red-600" />
                  <AlertDescription className="text-red-800">
                    <strong>Product Not Enabled:</strong> {guarantorValidationResult.productError}
                  </AlertDescription>
                </Alert>
              )}

              {/* LOAN DECISION SUMMARY */}
              {(() => {
                const memberEligible = guarantorValidationResult.memberInfo?.isEligible;
                const allGuarantorsEligible = guarantorValidationResult.validationResults?.every((r: any) => r.isEligible);
                const canApprove = memberEligible && allGuarantorsEligible && guarantorValidationResult.productEnabled !== false;
                
                let decisionText = "";
                let decisionColor = "";
                let decisionReason = "";
                
                if (guarantorValidationResult.productEnabled === false) {
                  decisionText = "CANNOT APPROVE";
                  decisionColor = "bg-red-100 text-red-800";
                  decisionReason = "Loan product is not enabled";
                } else if (!memberEligible) {
                  decisionText = "CANNOT APPROVE";
                  decisionColor = "bg-red-100 text-red-800";
                  decisionReason = "Member does not meet eligibility criteria";
                } else if (!allGuarantorsEligible) {
                  decisionText = "CANNOT APPROVE";
                  decisionColor = "bg-red-100 text-red-800";
                  const ineligibleGuarantors = guarantorValidationResult.validationResults
                    .filter((r: any) => !r.isEligible)
                    .map((r: any) => r.guarantorName)
                    .join(", ");
                  decisionReason = `Guarantor(s) not eligible: ${ineligibleGuarantors}`;
                } else {
                  decisionText = "CAN APPROVE";
                  decisionColor = "bg-green-100 text-green-800";
                  decisionReason = "Member and all guarantors meet eligibility criteria";
                }
                
                return (
                  <Card className={decisionColor === "bg-green-100 text-green-800" ? "border-green-300 bg-green-50" : "border-red-300 bg-red-50"}>
                    <CardHeader>
                      <CardTitle className="text-lg flex items-center justify-between">
                        <span>Loan Decision</span>
                        <Badge className={decisionColor}>
                          {decisionText === "CAN APPROVE" ? "✓" : "✗"} {decisionText}
                        </Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <p className="text-sm font-medium">{decisionReason}</p>
                    </CardContent>
                  </Card>
                );
              })()}

              {/* Loan Summary */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Loan Application Summary</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <p className="text-sm text-gray-600">Member Number</p>
                      <p className="font-medium">{guarantorValidationResult.memberNumber}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Loan Product</p>
                      <p className="font-medium">
                        {guarantorValidationResult.loanProductName}
                        {guarantorValidationResult.productEnabled === false && (
                          <span className="ml-2 text-red-600 text-xs font-semibold">(NOT ENABLED)</span>
                        )}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Loan Amount</p>
                      <p className="font-medium">{formatCurrency(guarantorValidationResult.loanAmount)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Purpose</p>
                      <p className="font-medium">{guarantorValidationResult.purpose}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Member Eligibility */}
              {guarantorValidationResult.memberInfo && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base flex items-center justify-between">
                      <span>Member Eligibility</span>
                      <Badge className={guarantorValidationResult.memberInfo.isEligible ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                        {guarantorValidationResult.memberInfo.isEligible ? "✓ ELIGIBLE" : "✗ NOT ELIGIBLE"}
                      </Badge>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">Member Name</p>
                        <p className="font-medium">{guarantorValidationResult.memberInfo.memberName}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Status</p>
                        <p className="font-medium">{guarantorValidationResult.memberInfo.status}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Savings Balance</p>
                        <p className="font-medium">{formatCurrency(guarantorValidationResult.memberInfo.savingsBalance)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Shares Balance</p>
                        <p className="font-medium">{formatCurrency(guarantorValidationResult.memberInfo.sharesBalance)}</p>
                      </div>
                      <div className="col-span-2 p-3 bg-blue-50 rounded">
                        <p className="text-sm text-gray-600">Total Balance (Savings + Shares)</p>
                        <p className="font-bold text-lg text-blue-900">{formatCurrency(guarantorValidationResult.memberInfo.totalBalance)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Outstanding Loans</p>
                        <p className="font-medium">{formatCurrency(guarantorValidationResult.memberInfo.totalOutstandingBalance)}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Active Loans</p>
                        <p className="font-medium">{guarantorValidationResult.memberInfo.activeLoans}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Defaulted Loans</p>
                        <p className="font-medium text-red-600">{guarantorValidationResult.memberInfo.defaultedLoans}</p>
                      </div>
                    </div>

                    {guarantorValidationResult.memberInfo.errors && guarantorValidationResult.memberInfo.errors.length > 0 && (
                      <div className="p-3 bg-red-50 rounded">
                        <p className="text-sm font-semibold text-red-900 mb-2">Issues:</p>
                        {guarantorValidationResult.memberInfo.errors.map((error: string, i: number) => (
                          <p key={i} className="text-sm text-red-800">✗ {error}</p>
                        ))}
                      </div>
                    )}

                    {guarantorValidationResult.memberInfo.warnings && guarantorValidationResult.memberInfo.warnings.length > 0 && (
                      <div className="p-3 bg-yellow-50 rounded">
                        <p className="text-sm font-semibold text-yellow-900 mb-2">Warnings:</p>
                        {guarantorValidationResult.memberInfo.warnings.map((warning: string, i: number) => (
                          <p key={i} className="text-sm text-yellow-800">⚠ {warning}</p>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}

              {/* Guarantor Validation Summary */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base flex items-center justify-between">
                    <span>Guarantor Eligibility</span>
                    <Badge className={guarantorValidationResult.validationResults?.every((r: any) => r.isEligible) ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                      {guarantorValidationResult.validationResults?.every((r: any) => r.isEligible) ? "✓ ALL ELIGIBLE" : "✗ SOME NOT ELIGIBLE"}
                    </Badge>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-600">Guarantor Count</p>
                      <p className="font-medium">{guarantorValidationResult.guarantorCount}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Detailed Validation Results */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Guarantor Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {guarantorValidationResult.validationResults && guarantorValidationResult.validationResults.map((result: any, idx: number) => (
                    <div key={idx} className="p-4 border rounded-lg">
                      <div className="flex justify-between items-start mb-3">
                        <div>
                          <p className="font-semibold">{result.guarantorName}</p>
                          <p className="text-sm text-gray-600">ID: {result.guarantorId}</p>
                        </div>
                        <Badge className={result.isEligible ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                          {result.isEligible ? "✓ ELIGIBLE" : "✗ NOT ELIGIBLE"}
                        </Badge>
                      </div>

                      <div className="grid grid-cols-2 gap-2 mb-3 text-sm">
                        <div>
                          <p className="text-gray-600">Savings Balance</p>
                          <p className="font-medium">{formatCurrency(result.savingsBalance)}</p>
                        </div>
                        <div>
                          <p className="text-gray-600">Shares Balance</p>
                          <p className="font-medium">{formatCurrency(result.sharesBalance || 0)}</p>
                        </div>
                        <div className="col-span-2 p-2 bg-blue-50 rounded">
                          <p className="text-gray-600">Total Balance (Savings + Shares)</p>
                          <p className="font-bold">{formatCurrency(result.totalBalance || 0)}</p>
                        </div>
                        <div>
                          <p className="text-gray-600">Outstanding Balance</p>
                          <p className="font-medium">{formatCurrency(result.outstandingBalance)}</p>
                        </div>
                      </div>

                      {result.errors && result.errors.length > 0 && (
                        <div className="mb-2 p-2 bg-red-50 rounded">
                          <p className="text-sm font-semibold text-red-900 mb-1">Issues:</p>
                          {result.errors.map((error: string, i: number) => (
                            <p key={i} className="text-sm text-red-800">✗ {error}</p>
                          ))}
                        </div>
                      )}

                      {result.warnings && result.warnings.length > 0 && (
                        <div className="p-2 bg-yellow-50 rounded">
                          <p className="text-sm font-semibold text-yellow-900 mb-1">Warnings:</p>
                          {result.warnings.map((warning: string, i: number) => (
                            <p key={i} className="text-sm text-yellow-800">⚠ {warning}</p>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </CardContent>
              </Card>


              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setGuarantorValidationDialogOpen(false)}>
                  Close
                </Button>
                {(() => {
                  const memberEligible = guarantorValidationResult.memberInfo?.isEligible;
                  const allGuarantorsEligible = guarantorValidationResult.validationResults?.every((r: any) => r.isEligible);
                  const canApprove = memberEligible && allGuarantorsEligible && guarantorValidationResult.productEnabled !== false;
                  
                  return (
                    <>
                      <Button 
                        variant="destructive" 
                        onClick={() => {
                          setSelectedLoanItemId(guarantorValidationResult.itemId);
                          setRejectLoanItemDialogOpen(true);
                          setGuarantorValidationDialogOpen(false);
                        }}
                        disabled={loading}
                      >
                        Reject
                      </Button>
                      <Button 
                        onClick={() => handleApproveLoanItem(guarantorValidationResult.itemId)}
                        disabled={!canApprove || loading}
                        className={canApprove ? "bg-green-600 hover:bg-green-700" : ""}
                      >
                        {loading ? "Approving..." : "Approve"}
                      </Button>
                    </>
                  );
                })()}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Bulk Approval Result Dialog */}
      <Dialog open={bulkApprovalDialogOpen} onOpenChange={setBulkApprovalDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Bulk Approval Results</DialogTitle>
          </DialogHeader>
          {bulkApprovalResult && (
            <div className="space-y-6">
              {/* Summary */}
              <div className="grid grid-cols-3 gap-4">
                <Card>
                  <CardContent className="pt-6">
                    <div className="text-center">
                      <p className="text-sm text-gray-600">Total Processed</p>
                      <p className="text-2xl font-bold">{bulkApprovalResult.totalProcessed}</p>
                    </div>
                  </CardContent>
                </Card>
                <Card className="border-green-200 bg-green-50">
                  <CardContent className="pt-6">
                    <div className="text-center">
                      <p className="text-sm text-green-700 font-semibold">Approved</p>
                      <p className="text-2xl font-bold text-green-700">{bulkApprovalResult.approvedCount}</p>
                    </div>
                  </CardContent>
                </Card>
                <Card className="border-orange-200 bg-orange-50">
                  <CardContent className="pt-6">
                    <div className="text-center">
                      <p className="text-sm text-orange-700 font-semibold">Flagged for Review</p>
                      <p className="text-2xl font-bold text-orange-700">{bulkApprovalResult.flaggedCount}</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Approved Items */}
              {bulkApprovalResult.approvedItems && bulkApprovalResult.approvedItems.length > 0 && (
                <Card className="border-green-200">
                  <CardHeader>
                    <CardTitle className="text-base text-green-700">✓ Successfully Approved ({bulkApprovalResult.approvedItems.length})</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      {bulkApprovalResult.approvedItems.map((item: any, idx: number) => (
                        <div key={idx} className="p-3 bg-green-50 rounded border border-green-200">
                          <p className="font-medium">{item.memberNumber}</p>
                          <p className="text-sm text-gray-600">Amount: {formatCurrency(item.loanAmount)}</p>
                          <p className="text-xs text-green-700">Ready for TREASURER to disburse</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Flagged Items */}
              {bulkApprovalResult.flaggedItems && bulkApprovalResult.flaggedItems.length > 0 && (
                <Card className="border-orange-200">
                  <CardHeader>
                    <CardTitle className="text-base text-orange-700">⚠ Flagged for Review ({bulkApprovalResult.flaggedItems.length})</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      {bulkApprovalResult.flaggedItems.map((item: any, idx: number) => (
                        <div key={idx} className="p-3 bg-orange-50 rounded border border-orange-200">
                          <p className="font-medium">{item.memberNumber}</p>
                          <p className="text-sm text-gray-600">Amount: {formatCurrency(item.loanAmount)}</p>
                          <p className="text-sm text-orange-700">{item.reason}</p>
                          {item.details && (
                            <div className="mt-2 text-xs text-orange-800">
                              {Array.isArray(item.details) ? (
                                item.details.map((detail: string, i: number) => (
                                  <p key={i}>• {detail}</p>
                                ))
                              ) : (
                                <p>• {item.details}</p>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setBulkApprovalDialogOpen(false)}>
                  Close
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Bulk Disbursement Result Dialog */}
      <Dialog open={bulkDisbursementDialogOpen} onOpenChange={setBulkDisbursementDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Bulk Disbursement Results</DialogTitle>
          </DialogHeader>
          {bulkDisbursementResult && (
            <div className="space-y-6">
              {/* Summary */}
              <div className="grid grid-cols-2 gap-4">
                <Card className="border-green-200 bg-green-50">
                  <CardContent className="pt-6">
                    <div className="text-center">
                      <p className="text-sm text-green-700 font-semibold">Successfully Disbursed</p>
                      <p className="text-2xl font-bold text-green-700">{bulkDisbursementResult.successfulCount}</p>
                    </div>
                  </CardContent>
                </Card>
                <Card className="border-red-200 bg-red-50">
                  <CardContent className="pt-6">
                    <div className="text-center">
                      <p className="text-sm text-red-700 font-semibold">Failed</p>
                      <p className="text-2xl font-bold text-red-700">{bulkDisbursementResult.failedCount}</p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Successful Disbursements */}
              {bulkDisbursementResult.successfulDisbursements && bulkDisbursementResult.successfulDisbursements.length > 0 && (
                <Card className="border-green-200">
                  <CardHeader>
                    <CardTitle className="text-base text-green-700">✓ Successfully Disbursed ({bulkDisbursementResult.successfulDisbursements.length})</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      {bulkDisbursementResult.successfulDisbursements.map((item: any, idx: number) => (
                        <div key={idx} className="p-3 bg-green-50 rounded border border-green-200">
                          <p className="font-medium">{item.memberNumber}</p>
                          <p className="text-sm text-gray-600">Amount: {formatCurrency(item.loanAmount)}</p>
                          <p className="text-xs text-green-700">Loan ID: {item.loanId}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Failed Disbursements */}
              {bulkDisbursementResult.failedDisbursements && bulkDisbursementResult.failedDisbursements.length > 0 && (
                <Card className="border-red-200">
                  <CardHeader>
                    <CardTitle className="text-base text-red-700">✗ Failed ({bulkDisbursementResult.failedDisbursements.length})</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      {bulkDisbursementResult.failedDisbursements.map((item: any, idx: number) => (
                        <div key={idx} className="p-3 bg-red-50 rounded border border-red-200">
                          <p className="font-medium">Item ID: {item.itemId}</p>
                          <p className="text-sm text-red-700">{item.error}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setBulkDisbursementDialogOpen(false)}>
                  Close
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
