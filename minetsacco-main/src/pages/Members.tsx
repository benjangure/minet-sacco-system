import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { nativeFetch } from '@/utils/nativeHttp';
import { useRefresh } from "@/contexts/RefreshContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Plus, Search, Eye, UserCheck, UserX, Upload, AlertCircle, FileText, RefreshCw } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { DocumentUpload } from "@/components/DocumentUpload";
import { CredentialDisplayModal } from "@/components/CredentialDisplayModal";
import { NextOfKinManager } from "@/components/NextOfKinManager";

import { getApiBaseUrl } from "../config/api";
const API_BASE_URL = getApiBaseUrl();

interface Member {
  id: number;
  memberNumber: string;
  employeeId?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  nationalId: string;
  dateOfBirth: string;
  employmentStatus: string;
  employer: string;
  department: string;
  status: string;
  bankName?: string;
  bankAccountNumber?: string;
  nextOfKinName?: string;
  nextOfKinPhone?: string;
  createdBy?: number;
  approvedBy?: number;
  approvedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  idDocumentPath?: string;
  photoPath?: string;
  applicationLetterPath?: string;
  kraPinPath?: string;
  memberStatus?: string;
}

interface MemberCreationResponseDTO {
  memberId: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
  username: string;
  password: string | null;
  hasNationalId: boolean;
  passwordType: string;
  message: string;
}

const statusColors: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  APPROVED: "bg-blue-100 text-blue-800",
  ACTIVE: "bg-green-100 text-green-800",
  DORMANT: "bg-gray-100 text-gray-800",
  SUSPENDED: "bg-red-100 text-red-800",
  REJECTED: "bg-red-100 text-red-800",
  EXITED: "bg-muted text-muted-foreground",
};

const Members = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewMember, setViewMember] = useState<Member | null>(null);
  const [editingMember, setEditingMember] = useState<Member | null>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [uploadDialog, setUploadDialog] = useState<Member | null>(null);
  const [approveDialog, setApproveDialog] = useState<Member | null>(null);
  const [rejectDialog, setRejectDialog] = useState<Member | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");
  const [credentialModalOpen, setCredentialModalOpen] = useState(false);
  const [memberCredential, setMemberCredential] = useState<MemberCreationResponseDTO | null>(null);
  
  // Exit member state
  const [exitDialog, setExitDialog] = useState<Member | null>(null);
  const [exitReason, setExitReason] = useState("");
  const [exitNotes, setExitNotes] = useState("");
  const [exitDate, setExitDate] = useState(new Date().toISOString().split('T')[0]);
  const [exitImpact, setExitImpact] = useState<any>(null);
  const [exitLoading, setExitLoading] = useState(false);
  
  const { toast } = useToast();
  const { session, role } = useAuth();
  const { refreshKey } = useRefresh();

  // Check if user can create members (TELLER or CUSTOMER_SUPPORT only)
  const canCreateMembers = role === "TELLER" || role === "CUSTOMER_SUPPORT";
  
  // Check if user can approve members (TREASURER or ADMIN only)
  const canApproveMembers = role === "TREASURER" || role === "ADMIN";

  const fetchMembers = async () => {
    setLoading(true);
    try {
      let url = `${API_BASE_URL}/members`;
      if (statusFilter !== "all") {
        url = `${API_BASE_URL}/members/status/${statusFilter}`;
      }
      
      // Add cache-busting timestamp
      url += `?_t=${Date.now()}`;

      const response = await nativeFetch(url, {
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        let membersList = data.data || [];

        // Use member.status directly instead of fetching status for each member
        // The backend already returns status in the member object
        let membersWithStatus = membersList.map((member: Member) => ({
          ...member,
          memberStatus: member.status || "ACTIVE"
        }));

        // Client-side search filter
        if (search) {
          membersWithStatus = membersWithStatus.filter((m: Member) =>
            m.firstName?.toLowerCase().includes(search.toLowerCase()) ||
            m.lastName?.toLowerCase().includes(search.toLowerCase()) ||
            m.employeeId?.toLowerCase().includes(search.toLowerCase()) ||
            m.memberNumber?.toLowerCase().includes(search.toLowerCase()) ||
            m.nationalId?.toLowerCase().includes(search.toLowerCase()) ||
            m.email?.toLowerCase().includes(search.toLowerCase()) ||
            m.phone?.toLowerCase().includes(search.toLowerCase())
          );
        }
        
        setMembers(membersWithStatus);
      } else {
        const error = await response.json();
        console.error("Error fetching members:", error);
        toast({ 
          title: "Error", 
          description: error.message || "Failed to fetch members", 
          variant: "destructive" 
        });
      }
    } catch (error) {
      console.error("Error fetching members:", error);
      toast({ 
        title: "Error", 
        description: "Failed to fetch members. Please try again.", 
        variant: "destructive" 
        });
    }
    setLoading(false);
  };

  useEffect(() => {
    if (session) {
      fetchMembers();
    }
  }, [session, statusFilter, refreshKey]); // Added refreshKey to trigger refresh from header button

  useEffect(() => {
    const timer = setTimeout(() => {
      if (session) {
        fetchMembers();
      }
    }, 300); // Debounce search by 300ms
    return () => clearTimeout(timer);
  }, [search]);

  const handleClearFilters = () => {
    setStatusFilter("all");
    setSearch("");
  };

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    nationalId: "",
    dateOfBirth: "",
    employeeId: "",
    employmentStatus: "PERMANENT",
    employer: "Minet Insurance",
    department: "",
    bankName: "",
    bankAccountNumber: "",
    bankBranch: "",
    nextOfKinName: "",
    nextOfKinPhone: "",
    nextOfKinRelationship: "",
  });

  const [currentTab, setCurrentTab] = useState("personal");

  const validatePhone = (phone: string): boolean => {
    // Kenyan phone format: +254XXXXXXXXX (9 digits after +254)
    const phoneRegex = /^\+254[0-9]{9}$/;
    return phoneRegex.test(phone);
  };

  const formatPhoneNumber = (value: string): string => {
    // Remove all non-digits
    let digits = value.replace(/\D/g, '');
    
    // If starts with 0, replace with 254
    if (digits.startsWith('0')) {
      digits = '254' + digits.substring(1);
    }
    
    // If starts with 254, add +
    if (digits.startsWith('254')) {
      return '+' + digits.substring(0, 12); // +254 + 9 digits = 12 chars
    }
    
    // If starts with 7 or 1 (common Kenyan prefixes), add +254
    if (digits.startsWith('7') || digits.startsWith('1')) {
      return '+254' + digits.substring(0, 9);
    }
    
    return '+254' + digits.substring(0, 9);
  };

  const handlePhoneChange = (value: string, field: 'phone' | 'nextOfKinPhone') => {
    const formatted = formatPhoneNumber(value);
    setForm({...form, [field]: formatted});
  };

  const handleEmploymentStatusChange = (value: string) => {
    if (value === "SELF_EMPLOYED") {
      // Auto-fill employer as "Self Employed" and clear department
      setForm({
        ...form, 
        employmentStatus: value,
        employer: "Self Employed",
        department: ""
      });
    } else {
      // Reset to default employer if changing from self-employed
      setForm({
        ...form, 
        employmentStatus: value,
        employer: form.employmentStatus === "SELF_EMPLOYED" ? "Minet Insurance" : form.employer
      });
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Client-side validation
    if (!form.firstName.trim() || !form.lastName.trim()) {
      toast({ title: "Error", description: "First name and last name are required", variant: "destructive" });
      return;
    }
    
    if (!form.phone.trim()) {
      toast({ title: "Error", description: "Phone number is required", variant: "destructive" });
      return;
    }
    
    // Validate phone format
    if (!validatePhone(form.phone)) {
      toast({ 
        title: "Error", 
        description: "Phone number must be in format +254XXXXXXXXX (9 digits after +254)", 
        variant: "destructive" 
      });
      return;
    }
    
    // Validate next of kin phone if provided
    if (form.nextOfKinPhone && !validatePhone(form.nextOfKinPhone)) {
      toast({ 
        title: "Error", 
        description: "Next of kin phone must be in format +254XXXXXXXXX", 
        variant: "destructive" 
      });
      return;
    }
    
    if (!form.nationalId.trim()) {
      toast({ title: "Error", description: "National ID is required", variant: "destructive" });
      return;
    }

    if (!form.employeeId.trim()) {
      toast({ title: "Error", description: "Member No. is required", variant: "destructive" });
      return;
    }
    
    try {
      const response = await nativeFetch(`${API_BASE_URL}/members`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(form),
      });

      if (response.ok) {
        const data = await response.json();
        const createdMember = data.data;
        
        // Display credentials modal
        setMemberCredential(createdMember);
        setCredentialModalOpen(true);
        
        // Close registration dialog and reset form
        setDialogOpen(false);
        setForm({
          firstName: "", lastName: "", email: "", phone: "", nationalId: "",
          dateOfBirth: "", employeeId: "", employmentStatus: "PERMANENT", employer: "Minet Insurance",
          department: "", bankName: "", bankAccountNumber: "", bankBranch: "",
          nextOfKinName: "", nextOfKinPhone: "", nextOfKinRelationship: "",
        });
        
        // Show success toast
        toast({ 
          title: "Success", 
          description: "Member registered successfully. View their credentials in the modal." 
        });
        
        // Refresh members list
        fetchMembers();
      } else {
        const error = await response.json();
        console.error("Registration error:", error);
        toast({ 
          title: "Error", 
          description: error.message || error.error || "Failed to register member. Please check all required fields.", 
          variant: "destructive" 
        });
      }
    } catch (error) {
      console.error("Registration error:", error);
      toast({ title: "Error", description: "Failed to register member. Please try again.", variant: "destructive" });
    }
  };

  const handleApproveMember = async (memberId: number) => {
    try {
      const response = await nativeFetch(`${API_BASE_URL}/members/${memberId}/approve`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Member approved successfully. Default accounts created." });
        setApproveDialog(null);
        fetchMembers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to approve member", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to approve member", variant: "destructive" });
    }
  };

  const handleRejectMember = async (memberId: number) => {
    if (!rejectionReason.trim()) {
      toast({ title: "Error", description: "Please provide a rejection reason", variant: "destructive" });
      return;
    }

    try {
      const response = await nativeFetch(`${API_BASE_URL}/members/${memberId}/reject`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({ memberId, approved: false, rejectionReason }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Member application rejected" });
        setRejectDialog(null);
        setRejectionReason("");
        fetchMembers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to reject member", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to reject member", variant: "destructive" });
    }
  };

  const handleActivateMember = async (memberId: number) => {
    try {
      const response = await nativeFetch(`${API_BASE_URL}/members/${memberId}/activate`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Member activated successfully" });
        fetchMembers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to activate member", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to activate member", variant: "destructive" });
    }
  };

  const handleReactivateMember = async (memberId: number) => {
    try {
      const response = await nativeFetch(`${API_BASE_URL}/members/${memberId}/reactivate`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Member reactivated successfully. They can now use the system again." });
        fetchMembers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to reactivate member", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to reactivate member", variant: "destructive" });
    }
  };

  const handleUpdateMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMember) return;

    try {
      const response = await nativeFetch(`${API_BASE_URL}/members/${editingMember.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(editingMember),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Member information updated successfully" });
        setEditDialogOpen(false);
        setEditingMember(null);
        fetchMembers();
      } else {
        const error = await response.json();
        toast({ 
          title: "Error", 
          description: error.message || "Failed to update member", 
          variant: "destructive" 
        });
      }
    } catch (error) {
      console.error("Update error:", error);
      toast({ title: "Error", description: "Failed to update member. Please try again.", variant: "destructive" });
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Members</h1>
          <p className="text-muted-foreground">Manage SACCO member registrations and profiles</p>
        </div>
        {canCreateMembers && (
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button><Plus className="mr-2 h-4 w-4" />Register Member</Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>Register New Member Application</DialogTitle>
                <DialogDescription>
                  Fill in the member's details across the three tabs to complete the registration.
                </DialogDescription>
              </DialogHeader>
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Member will be registered immediately as Active. Savings and Shares accounts will be created automatically.
                </AlertDescription>
              </Alert>
              <form onSubmit={handleRegister} className="space-y-6">
                <Tabs value={currentTab} onValueChange={setCurrentTab}>
                  <TabsList className="grid w-full grid-cols-3">
                    <TabsTrigger value="personal">Personal Info</TabsTrigger>
                    <TabsTrigger value="employment">Employment</TabsTrigger>
                    <TabsTrigger value="other">Bank & Next of Kin</TabsTrigger>
                  </TabsList>
                  <TabsContent value="personal" className="space-y-4 pt-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>First Name *</Label>
                        <Input value={form.firstName} onChange={e => setForm({...form, firstName: e.target.value})} required />
                      </div>
                      <div className="space-y-2">
                        <Label>Last Name *</Label>
                        <Input value={form.lastName} onChange={e => setForm({...form, lastName: e.target.value})} required />
                      </div>
                      <div className="space-y-2">
                        <Label>Email</Label>
                        <Input type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} placeholder="optional" />
                      </div>
                      <div className="space-y-2">
                        <Label>Phone Number *</Label>
                        <Input 
                          value={form.phone} 
                          onChange={e => handlePhoneChange(e.target.value, 'phone')} 
                          required 
                          placeholder="+254712345678"
                          maxLength={13}
                        />
                        <p className="text-xs text-muted-foreground">Format: +254XXXXXXXXX (9 digits)</p>
                      </div>
                      <div className="space-y-2">
                        <Label>National ID *</Label>
                        <Input value={form.nationalId} onChange={e => setForm({...form, nationalId: e.target.value})} required />
                      </div>
                      <div className="space-y-2">
                        <Label>Member No. *</Label>
                        <Input 
                          value={form.employeeId} 
                          onChange={e => setForm({...form, employeeId: e.target.value})} 
                          required 
                          placeholder="e.g. EMP001"
                        />
                        <p className="text-xs text-muted-foreground">Enter the employee ID — this becomes the member's SACCO number and login username</p>
                      </div>
                      <div className="space-y-2">
                        <Label>Date of Birth</Label>
                        <Input type="date" value={form.dateOfBirth} onChange={e => setForm({...form, dateOfBirth: e.target.value})} />
                      </div>
                    </div>
                    <div className="flex justify-end">
                      <Button type="button" onClick={() => setCurrentTab("employment")}>
                        Next: Employment Info
                      </Button>
                    </div>
                  </TabsContent>
                  <TabsContent value="employment" className="space-y-4 pt-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>Employment Status</Label>
                        <Select value={form.employmentStatus} onValueChange={handleEmploymentStatusChange}>
                          <SelectTrigger><SelectValue /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="PERMANENT">Permanent</SelectItem>
                            <SelectItem value="CONTRACT">Contract</SelectItem>
                            <SelectItem value="TEMPORARY">Temporary</SelectItem>
                            <SelectItem value="SELF_EMPLOYED">Self Employed</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label>Employer</Label>
                        <Input 
                          value={form.employer} 
                          onChange={e => setForm({...form, employer: e.target.value})} 
                          disabled={form.employmentStatus === "SELF_EMPLOYED"}
                          className={form.employmentStatus === "SELF_EMPLOYED" ? "bg-muted" : ""}
                        />
                        {form.employmentStatus === "SELF_EMPLOYED" && (
                          <p className="text-xs text-muted-foreground">Auto-filled for self-employed</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <Label>Department</Label>
                        <Input 
                          value={form.department} 
                          onChange={e => setForm({...form, department: e.target.value})} 
                          disabled={form.employmentStatus === "SELF_EMPLOYED"}
                          placeholder={form.employmentStatus === "SELF_EMPLOYED" ? "N/A for self-employed" : ""}
                          className={form.employmentStatus === "SELF_EMPLOYED" ? "bg-muted" : ""}
                        />
                      </div>
                    </div>
                    <div className="flex justify-between">
                      <Button type="button" variant="outline" onClick={() => setCurrentTab("personal")}>
                        Back
                      </Button>
                      <Button type="button" onClick={() => setCurrentTab("other")}>
                        Next: Bank & Next of Kin
                      </Button>
                    </div>
                  </TabsContent>
                  <TabsContent value="other" className="space-y-4 pt-4">
                    <h3 className="font-semibold">Bank Details</h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>Bank Name</Label>
                        <Input value={form.bankName} onChange={e => setForm({...form, bankName: e.target.value})} />
                      </div>
                      <div className="space-y-2">
                        <Label>Account Number</Label>
                        <Input value={form.bankAccountNumber} onChange={e => setForm({...form, bankAccountNumber: e.target.value})} />
                      </div>
                      <div className="space-y-2">
                        <Label>Branch</Label>
                        <Input value={form.bankBranch} onChange={e => setForm({...form, bankBranch: e.target.value})} />
                      </div>
                    </div>
                    <h3 className="font-semibold mt-4">Next of Kin</h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>Name</Label>
                        <Input value={form.nextOfKinName} onChange={e => setForm({...form, nextOfKinName: e.target.value})} />
                      </div>
                      <div className="space-y-2">
                        <Label>Phone</Label>
                        <Input 
                          value={form.nextOfKinPhone} 
                          onChange={e => handlePhoneChange(e.target.value, 'nextOfKinPhone')} 
                          placeholder="+254712345678"
                          maxLength={13}
                        />
                        <p className="text-xs text-muted-foreground">Format: +254XXXXXXXXX</p>
                      </div>
                      <div className="space-y-2">
                        <Label>Relationship</Label>
                        <Input value={form.nextOfKinRelationship} onChange={e => setForm({...form, nextOfKinRelationship: e.target.value})} />
                      </div>
                    </div>
                    <div className="flex justify-between">
                      <Button type="button" variant="outline" onClick={() => setCurrentTab("employment")}>
                        Back
                      </Button>
                      <Button type="submit">Submit Application</Button>
                    </div>
                  </TabsContent>
                </Tabs>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {/* Role-based info alert */}
      {!canCreateMembers && !canApproveMembers && (
        <Alert className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            You have view-only access to member information. Contact Teller or Treasurer for member registration.
          </AlertDescription>
        </Alert>
      )}

      {/* Filters */}
      <Card className="mb-6 border-none shadow-sm">
        <CardContent className="pt-6">
          <div className="flex gap-4 items-end">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input placeholder="Search by name, member number, or national ID..." className="pl-10" value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="DORMANT">Dormant</SelectItem>
                <SelectItem value="SUSPENDED">Suspended</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
                <SelectItem value="EXITED">Exited</SelectItem>
              </SelectContent>
            </Select>
            {(statusFilter !== "all" || search !== "") && (
              <Button onClick={handleClearFilters} variant="outline" size="sm">Clear Filters</Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Approve Dialog */}
      <Dialog open={!!approveDialog} onOpenChange={() => setApproveDialog(null)}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Review & Approve Member Application</DialogTitle>
            <DialogDescription>
              Review the applicant's information and documents before approving or rejecting the application.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-6">
            {/* Member Information */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Applicant Information</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div><strong>Name:</strong> {approveDialog?.firstName} {approveDialog?.lastName}</div>
                  <div><strong>National ID:</strong> {approveDialog?.nationalId}</div>
                  <div><strong>Phone:</strong> {approveDialog?.phone}</div>
                  <div><strong>Email:</strong> {approveDialog?.email || "—"}</div>
                  <div><strong>Department:</strong> {approveDialog?.department || "—"}</div>
                  <div><strong>Employer:</strong> {approveDialog?.employer || "—"}</div>
                  <div><strong>Bank:</strong> {approveDialog?.bankName || "—"}</div>
                  <div><strong>Account:</strong> {approveDialog?.bankAccountNumber || "—"}</div>
                </div>
              </CardContent>
            </Card>

            {/* KYC Documents */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <FileText className="h-5 w-5" />
                  KYC Documents
                </CardTitle>
              </CardHeader>
              <CardContent>
                {approveDialog && session?.token && (
                  <DocumentUpload
                    memberId={approveDialog.id}
                    token={session.token}
                    documents={{
                      idDocumentPath: approveDialog.idDocumentPath,
                      photoPath: approveDialog.photoPath,
                      applicationLetterPath: approveDialog.applicationLetterPath,
                      kraPinPath: approveDialog.kraPinPath,
                    }}
                    onUploadComplete={fetchMembers}
                    readOnly={false}
                  />
                )}
              </CardContent>
            </Card>

            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Upon approval, default Savings & Shares accounts will be created. A mobile app login will also be provisioned — username: member number, default password: national ID.
              </AlertDescription>
            </Alert>

            <div className="flex gap-2">
              <Button onClick={() => approveDialog && handleApproveMember(approveDialog.id)} className="flex-1">
                <UserCheck className="mr-2 h-4 w-4" />Approve Application
              </Button>
              <Button variant="outline" onClick={() => setApproveDialog(null)} className="flex-1">Cancel</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Document Upload Dialog */}
      <Dialog open={!!uploadDialog} onOpenChange={() => setUploadDialog(null)}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Upload KYC Documents</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Upload all required documents for member: {uploadDialog?.firstName} {uploadDialog?.lastName}
              </AlertDescription>
            </Alert>
            {uploadDialog && session?.token && (
              <DocumentUpload
                memberId={uploadDialog.id}
                token={session.token}
                documents={{
                  idDocumentPath: uploadDialog.idDocumentPath,
                  photoPath: uploadDialog.photoPath,
                  applicationLetterPath: uploadDialog.applicationLetterPath,
                  kraPinPath: uploadDialog.kraPinPath,
                }}
                onUploadComplete={() => {
                  fetchMembers();
                  toast({ title: "Success", description: "Documents updated successfully" });
                }}
                readOnly={false}
              />
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={!!rejectDialog} onOpenChange={() => { setRejectDialog(null); setRejectionReason(""); }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject Member Application</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p>Provide a reason for rejecting this application:</p>
            <div className="text-sm space-y-1">
              <p><strong>Name:</strong> {rejectDialog?.firstName} {rejectDialog?.lastName}</p>
              <p><strong>National ID:</strong> {rejectDialog?.nationalId}</p>
            </div>
            <Textarea 
              placeholder="Enter rejection reason..." 
              value={rejectionReason}
              onChange={e => setRejectionReason(e.target.value)}
              rows={4}
            />
            <div className="flex gap-2">
              <Button 
                variant="destructive" 
                onClick={() => rejectDialog && handleRejectMember(rejectDialog.id)} 
                className="flex-1"
              >
                <UserX className="mr-2 h-4 w-4" />Reject Application
              </Button>
              <Button variant="outline" onClick={() => { setRejectDialog(null); setRejectionReason(""); }} className="flex-1">Cancel</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Exit Member Dialog */}
      <Dialog open={!!exitDialog} onOpenChange={() => { setExitDialog(null); setExitReason(""); setExitNotes(""); setExitImpact(null); }}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Mark Member as Exited</DialogTitle>
          </DialogHeader>
          {exitDialog && (
            <div className="space-y-4">
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  You are about to mark <strong>{exitDialog.firstName} {exitDialog.lastName}</strong> ({exitDialog.employeeId}) as EXITED from the SACCO.
                </AlertDescription>
              </Alert>

              <div className="space-y-3">
                <div className="space-y-2">
                  <Label>Exit Reason *</Label>
                  <Select value={exitReason} onValueChange={setExitReason}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select exit reason" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="RESIGNED">Resigned</SelectItem>
                      <SelectItem value="RETIRED">Retired</SelectItem>
                      <SelectItem value="TERMINATED">Terminated</SelectItem>
                      <SelectItem value="DECEASED">Deceased</SelectItem>
                      <SelectItem value="OTHER">Other</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Exit Date *</Label>
                  <Input
                    type="date"
                    value={exitDate}
                    onChange={(e) => setExitDate(e.target.value)}
                    max={new Date().toISOString().split('T')[0]}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Additional Notes</Label>
                  <Textarea
                    value={exitNotes}
                    onChange={(e) => setExitNotes(e.target.value)}
                    placeholder="Any additional notes about the exit..."
                    rows={3}
                  />
                </div>
              </div>

              {exitImpact && exitImpact.totalLoansAsGuarantor > 0 && (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 space-y-3">
                  <div className="font-semibold text-yellow-900">⚠️ Impact Analysis</div>
                  <p className="text-sm text-yellow-800">
                    This member is currently a PRIMARY guarantor for <strong>{exitImpact.totalLoansAsGuarantor} loan(s)</strong> totaling <strong>KES {exitImpact.totalGuaranteeAmount?.toLocaleString()}</strong>
                  </p>
                  
                  {exitImpact.loansAsGuarantor && exitImpact.loansAsGuarantor.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-xs font-medium text-yellow-900">Affected Loans:</p>
                      {exitImpact.loansAsGuarantor.map((loan: any, idx: number) => (
                        <div key={idx} className="bg-white rounded p-2 text-sm">
                          <div className="flex items-start justify-between">
                            <div>
                              <p className="font-medium">Loan #{loan.loanNumber} - {loan.borrowerName}</p>
                              <p className="text-xs text-gray-600">Guarantee Amount: KES {loan.guaranteeAmount?.toLocaleString()}</p>
                            </div>
                            <div className="text-right">
                              {loan.hasNok ? (
                                <div className="text-green-600 text-xs">
                                  ✓ NOK Available<br/>
                                  <span className="text-gray-600">{loan.nokName}</span>
                                </div>
                              ) : (
                                <div className="text-red-600 text-xs font-semibold">
                                  ⚠️ NO NOK
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {exitImpact.allLoansHaveNok ? (
                    <Alert className="bg-green-50 border-green-200">
                      <AlertDescription className="text-green-800">
                        ✓ All loans have Next of Kin guarantors. They will automatically be promoted to primary guarantors.
                      </AlertDescription>
                    </Alert>
                  ) : (
                    <Alert variant="destructive">
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        ⚠️ {exitImpact.loansWithoutNok} loan(s) do NOT have backup guarantors. Borrowers will need to find replacement guarantors.
                      </AlertDescription>
                    </Alert>
                  )}
                </div>
              )}

              <div className="flex gap-2">
                <Button
                  variant="destructive"
                  onClick={async () => {
                    if (!exitReason) {
                      toast({ title: "Error", description: "Exit reason is required", variant: "destructive" });
                      return;
                    }
                    if (!exitDate) {
                      toast({ title: "Error", description: "Exit date is required", variant: "destructive" });
                      return;
                    }

                    setExitLoading(true);
                    try {
                      const response = await nativeFetch(`${API_BASE_URL}/members/${exitDialog.id}/exit`, {
                        method: "POST",
                        headers: {
                          "Content-Type": "application/json",
                          "Authorization": `Bearer ${session?.token}`
                        },
                        body: JSON.stringify({
                          exitReason,
                          exitDate: new Date(exitDate).toISOString(),
                          exitNotes
                        })
                      });

                      if (response.ok) {
                        toast({ title: "Success", description: "Member marked as exited and guarantors replaced" });
                        setExitDialog(null);
                        setExitReason("");
                        setExitNotes("");
                        setExitImpact(null);
                        // Clear any filters and refresh
                        setStatusFilter("all");
                        setSearch("");
                        fetchMembers();
                      } else {
                        const error = await response.json();
                        toast({ title: "Error", description: error.message || "Failed to exit member", variant: "destructive" });
                      }
                    } catch (error) {
                      toast({ title: "Error", description: "Failed to exit member", variant: "destructive" });
                    } finally {
                      setExitLoading(false);
                    }
                  }}
                  disabled={exitLoading}
                  className="flex-1"
                >
                  {exitLoading ? "Processing..." : "Confirm Exit & Replace Guarantors"}
                </Button>
                <Button variant="outline" onClick={() => { setExitDialog(null); setExitReason(""); setExitNotes(""); setExitImpact(null); }} className="flex-1">
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Member Details Dialog */}
      <Dialog open={!!viewMember} onOpenChange={() => setViewMember(null)}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Member Profile — {viewMember?.employeeId || viewMember?.memberNumber || "Pending"}</DialogTitle>
          </DialogHeader>
          {viewMember && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Badge className={statusColors[viewMember.status]}>{viewMember.status}</Badge>
                {viewMember.status === "APPROVED" && canApproveMembers && (
                  <Button size="sm" onClick={() => handleActivateMember(viewMember.id)}>
                    Activate Member
                  </Button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div><span className="text-muted-foreground">Name:</span> {viewMember.firstName} {viewMember.lastName}</div>
                <div><span className="text-muted-foreground">Member No.:</span> {viewMember.employeeId || viewMember.memberNumber || "—"}</div>
                <div><span className="text-muted-foreground">Phone:</span> {viewMember.phone}</div>
                <div><span className="text-muted-foreground">Email:</span> {viewMember.email || "—"}</div>
                <div><span className="text-muted-foreground">National ID:</span> {viewMember.nationalId}</div>
                <div><span className="text-muted-foreground">Department:</span> {viewMember.department || "—"}</div>
                <div><span className="text-muted-foreground">Employer:</span> {viewMember.employer || "—"}</div>
                <div><span className="text-muted-foreground">Bank:</span> {viewMember.bankName || "—"}</div>
                <div><span className="text-muted-foreground">Account:</span> {viewMember.bankAccountNumber || "—"}</div>
                <div><span className="text-muted-foreground">Next of Kin:</span> {viewMember.nextOfKinName || "—"}</div>
                <div><span className="text-muted-foreground">NOK Phone:</span> {viewMember.nextOfKinPhone || "—"}</div>
                <div><span className="text-muted-foreground">Applied:</span> {new Date(viewMember.createdAt).toLocaleDateString()}</div>
                {viewMember.approvedAt && (
                  <div><span className="text-muted-foreground">Approved:</span> {new Date(viewMember.approvedAt).toLocaleDateString()}</div>
                )}
              </div>
              {viewMember.rejectionReason && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>
                    <strong>Rejection Reason:</strong> {viewMember.rejectionReason}
                  </AlertDescription>
                </Alert>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Members Table */}
      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Member No.</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Department</TableHead>
                <TableHead>Member Status</TableHead>
                <TableHead>Applied</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={7} className="text-center py-8 text-muted-foreground">Loading...</TableCell></TableRow>
              ) : members.length === 0 ? (
                <TableRow><TableCell colSpan={7} className="text-center py-8 text-muted-foreground">No members found</TableCell></TableRow>
              ) : members.map(member => (
                <TableRow key={member.id}>
                  <TableCell className="font-mono text-sm">{member.employeeId || member.memberNumber || "—"}</TableCell>
                  <TableCell className="font-medium">{member.fullName || member.firstName}</TableCell>
                  <TableCell>{member.phone}</TableCell>
                  <TableCell>{member.department || "—"}</TableCell>
                  <TableCell>
                    <Badge className={
                      member.memberStatus === "ACTIVE" ? "bg-green-100 text-green-800" :
                      member.memberStatus === "SUSPENDED" ? "bg-red-100 text-red-800" :
                      member.memberStatus === "EXITED" ? "bg-gray-100 text-gray-800" :
                      "bg-green-100 text-green-800"
                    }>
                      {member.memberStatus || "ACTIVE"}
                    </Badge>
                  </TableCell>
                  <TableCell>{new Date(member.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" onClick={() => setViewMember(member)} title="View Details">
                        <Eye className="h-4 w-4" />
                      </Button>
                      {(canCreateMembers || canApproveMembers) && member.status !== "EXITED" && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={() => { setEditingMember({...member}); setEditDialogOpen(true); }} 
                          title="Edit Member"
                          className="text-blue-600"
                        >
                          <span className="text-xs">✏️</span>
                        </Button>
                      )}
                      {(member.status === "PENDING" || member.status === "APPROVED") && canCreateMembers && (
                        <Button variant="ghost" size="icon" onClick={() => setUploadDialog(member)} title="Upload Documents" className="text-blue-600">
                          <Upload className="h-4 w-4" />
                        </Button>
                      )}
                      {member.status === "PENDING" && canApproveMembers && (
                        <>
                          <Button variant="ghost" size="icon" onClick={() => setApproveDialog(member)} className="text-green-600" title="Approve">
                            <UserCheck className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="icon" onClick={() => setRejectDialog(member)} className="text-red-600" title="Reject">
                            <UserX className="h-4 w-4" />
                          </Button>
                        </>
                      )}
                      {member.status === "ACTIVE" && canApproveMembers && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={async () => {
                            setExitDialog(member);
                            setExitDate(new Date().toISOString().split('T')[0]);
                            setExitReason("");
                            setExitNotes("");
                            setExitImpact(null);
                            // Fetch impact analysis
                            try {
                              const response = await nativeFetch(`${API_BASE_URL}/members/${member.id}/exit-impact`, {
                                headers: { "Authorization": `Bearer ${session?.token}` }
                              });
                              if (response.ok) {
                                const data = await response.json();
                                setExitImpact(data.data);
                              }
                            } catch (err) {
                              console.error("Failed to fetch exit impact", err);
                            }
                          }} 
                          className="text-orange-600" 
                          title="Mark as Exited"
                        >
                          <UserX className="h-4 w-4" />
                        </Button>
                      )}
                      {member.status === "EXITED" && canApproveMembers && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          onClick={() => {
                            if (window.confirm(`Are you sure you want to reactivate ${member.firstName} ${member.lastName}? They will regain access to the system.`)) {
                              handleReactivateMember(member.id);
                            }
                          }} 
                          className="text-green-600" 
                          title="Reactivate Member"
                        >
                          <RefreshCw className="h-4 w-4" />
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

      {/* Credential Display Modal */}
      <CredentialDisplayModal
        isOpen={credentialModalOpen}
        onClose={() => setCredentialModalOpen(false)}
        credential={memberCredential}
      />

      {/* Edit Member Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Member Information</DialogTitle>
          </DialogHeader>
          {editingMember && (
            <form onSubmit={handleUpdateMember} className="space-y-6">
              <Tabs defaultValue="personal">
                <TabsList className="grid w-full grid-cols-3">
                  <TabsTrigger value="personal">Personal Info</TabsTrigger>
                  <TabsTrigger value="employment">Employment</TabsTrigger>
                  <TabsTrigger value="other">Bank & Next of Kin</TabsTrigger>
                </TabsList>
                
                <TabsContent value="personal" className="space-y-4 pt-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>First Name</Label>
                      <Input 
                        value={editingMember.firstName} 
                        onChange={e => setEditingMember({...editingMember, firstName: e.target.value})} 
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Last Name</Label>
                      <Input 
                        value={editingMember.lastName} 
                        onChange={e => setEditingMember({...editingMember, lastName: e.target.value})} 
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Email</Label>
                      <Input 
                        type="email" 
                        value={editingMember.email} 
                        onChange={e => setEditingMember({...editingMember, email: e.target.value})} 
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Phone Number</Label>
                      <Input 
                        value={editingMember.phone} 
                        onChange={e => setEditingMember({...editingMember, phone: e.target.value})} 
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>National ID</Label>
                      <Input 
                        value={editingMember.nationalId || ""} 
                        onChange={e => setEditingMember({...editingMember, nationalId: e.target.value})} 
                        placeholder="Optional"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Date of Birth</Label>
                      <Input 
                        type="date" 
                        value={editingMember.dateOfBirth ? editingMember.dateOfBirth.split('T')[0] : ""} 
                        onChange={e => setEditingMember({...editingMember, dateOfBirth: e.target.value})} 
                      />
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="employment" className="space-y-4 pt-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Employment Status</Label>
                      <Select 
                        value={editingMember.employmentStatus} 
                        onValueChange={v => setEditingMember({...editingMember, employmentStatus: v})}
                      >
                        <SelectTrigger><SelectValue /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="PERMANENT">Permanent</SelectItem>
                          <SelectItem value="CONTRACT">Contract</SelectItem>
                          <SelectItem value="TEMPORARY">Temporary</SelectItem>
                          <SelectItem value="SELF_EMPLOYED">Self Employed</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-2">
                      <Label>Employer</Label>
                      <Input 
                        value={editingMember.employer || ""} 
                        onChange={e => setEditingMember({...editingMember, employer: e.target.value})} 
                      />
                    </div>
                    <div className="space-y-2 col-span-2">
                      <Label>Department</Label>
                      <Input 
                        value={editingMember.department || ""} 
                        onChange={e => setEditingMember({...editingMember, department: e.target.value})} 
                      />
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="other" className="space-y-6 pt-4">
                  <div className="space-y-4">
                    <h3 className="font-semibold text-lg">Bank Details</h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>Bank Name</Label>
                        <Input 
                          value={editingMember.bankName || ""} 
                          onChange={e => setEditingMember({...editingMember, bankName: e.target.value})} 
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>Account Number</Label>
                        <Input 
                          value={editingMember.bankAccountNumber || ""} 
                          onChange={e => setEditingMember({...editingMember, bankAccountNumber: e.target.value})} 
                        />
                      </div>
                    </div>
                  </div>
                  
                  <div className="border-t pt-6">
                    <NextOfKinManager 
                      key={editingMember.id}
                      memberId={editingMember.id}
                      onSave={() => {
                        toast({
                          title: "Success",
                          description: "Next of kin information updated"
                        });
                        fetchMembers();
                      }}
                    />
                  </div>
                </TabsContent>
              </Tabs>

              <div className="flex gap-2">
                <Button type="submit" className="flex-1">
                  Save Changes
                </Button>
                <Button 
                  type="button" 
                  variant="outline" 
                  onClick={() => { setEditDialogOpen(false); setEditingMember(null); }} 
                  className="flex-1"
                >
                  Cancel
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Members;
