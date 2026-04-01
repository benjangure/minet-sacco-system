import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { AlertCircle, CheckCircle, XCircle, Loader2, Download } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

const API_BASE_URL = "http://localhost:8080/api";

interface KycDocument {
  id: number;
  memberId: number;
  memberNumber: string;
  memberName: string;
  documentType: string;
  verificationStatus: string;
  uploadDate: string;
  uploadedByName: string;
}

interface MemberKycStatus {
  memberId: number;
  memberNumber: string;
  memberName: string;
  email: string;
  phone: string;
  kycCompletionStatus: string;
  documentsUploaded: number;
  documentsVerified: number;
  totalDocumentsRequired: number;
  allDocumentsComplete: boolean;
  allDocumentsVerified: boolean;
  documents: KycDocument[];
}

const KycApproval = () => {
  const [pendingDocuments, setPendingDocuments] = useState<KycDocument[]>([]);
  const [membersWithIncompleteKyc, setMembersWithIncompleteKyc] = useState<MemberKycStatus[]>([]);
  const [selectedMembers, setSelectedMembers] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"pending" | "incomplete">("pending");
  const [rejectionReason, setRejectionReason] = useState<string>("");
  const [selectedDocForRejection, setSelectedDocForRejection] = useState<number | null>(null);
  const { session, role } = useAuth();
  const { toast } = useToast();

  // Check if user is TELLER
  if (role !== "TELLER" && role !== "ADMIN") {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only TELLER can access the KYC Approval module.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  useEffect(() => {
    if (session) {
      fetchPendingDocuments();
      fetchMembersWithIncompleteKyc();
    }
  }, [session]);

  const fetchPendingDocuments = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/kyc-documents/pending`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const result = await response.json();
        console.log("Pending documents response:", result);
        const docs = result.data || [];
        setPendingDocuments(docs);
        console.log("Set pending documents:", docs.length);
      } else {
        console.error("Failed to fetch pending documents:", response.status, response.statusText);
        const errorText = await response.text();
        console.error("Error response:", errorText);
      }
    } catch (error) {
      console.error("Error fetching pending documents:", error);
      toast({
        title: "Error",
        description: "Failed to load pending documents",
        variant: "destructive",
      });
    }
  };

  const fetchMembersWithIncompleteKyc = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/kyc-documents/incomplete-members`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const result = await response.json();
        setMembersWithIncompleteKyc(result.data || []);
      }
    } catch (error) {
      console.error("Error fetching incomplete members:", error);
      toast({
        title: "Error",
        description: "Failed to load members with incomplete KYC",
        variant: "destructive",
      });
    }
  };

  const handleVerifyDocument = async (documentId: number) => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/kyc-documents/${documentId}/verify`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${session?.token}` },
      });

      if (response.ok) {
        toast({
          title: "Success",
          description: "Document verified successfully",
        });
        fetchPendingDocuments();
        fetchMembersWithIncompleteKyc();
      } else {
        toast({
          title: "Error",
          description: "Failed to verify document",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to verify document",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRejectDocument = async (documentId: number) => {
    if (!rejectionReason.trim()) {
      toast({
        title: "Error",
        description: "Please provide a rejection reason",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(
        `${API_BASE_URL}/kyc-documents/${documentId}/reject?reason=${encodeURIComponent(rejectionReason)}`,
        {
          method: "PUT",
          headers: { Authorization: `Bearer ${session?.token}` },
        }
      );

      if (response.ok) {
        toast({
          title: "Success",
          description: "Document rejected successfully",
        });
        setRejectionReason("");
        setSelectedDocForRejection(null);
        fetchPendingDocuments();
        fetchMembersWithIncompleteKyc();
      } else {
        toast({
          title: "Error",
          description: "Failed to reject document",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to reject document",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleBulkApprove = async () => {
    if (selectedMembers.size === 0) {
      toast({
        title: "Error",
        description: "Please select at least one member",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/kyc-documents/bulk-approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session?.token}`,
        },
        body: JSON.stringify(Array.from(selectedMembers)),
      });

      if (response.ok) {
        const result = await response.json();
        toast({
          title: "Success",
          description: result.message,
        });
        setSelectedMembers(new Set());
        fetchMembersWithIncompleteKyc();
      } else {
        toast({
          title: "Error",
          description: "Failed to approve members",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to approve members",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const toggleMemberSelection = (memberId: number) => {
    const newSelected = new Set(selectedMembers);
    if (newSelected.has(memberId)) {
      newSelected.delete(memberId);
    } else {
      newSelected.add(memberId);
    }
    setSelectedMembers(newSelected);
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "VERIFIED":
        return <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full">Verified</span>;
      case "REJECTED":
        return <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">Rejected</span>;
      default:
        return <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">Pending</span>;
    }
  };

  return (
    <div>
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">KYC Document Approval</h1>
          <p className="text-muted-foreground">Review and approve member KYC documents</p>
        </div>
        <Button 
          variant="outline"
          onClick={() => window.location.href = "/view-documents"}
        >
          View All Documents
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        <Button
          variant={activeTab === "pending" ? "default" : "outline"}
          onClick={() => setActiveTab("pending")}
        >
          Pending Documents ({pendingDocuments.length})
        </Button>
        <Button
          variant={activeTab === "incomplete" ? "default" : "outline"}
          onClick={() => setActiveTab("incomplete")}
        >
          Incomplete KYC ({membersWithIncompleteKyc.length})
        </Button>
      </div>

      {/* Pending Documents Tab */}
      {activeTab === "pending" && (
        <Card>
          <CardHeader>
            <CardTitle>Pending Document Verification</CardTitle>
          </CardHeader>
          <CardContent>
            {pendingDocuments.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500" />
                <p>No pending documents for verification</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Member</TableHead>
                      <TableHead>Document Type</TableHead>
                      <TableHead>Uploaded By</TableHead>
                      <TableHead>Upload Date</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {pendingDocuments.map((doc) => (
                      <TableRow key={doc.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{doc.memberName}</p>
                            <p className="text-xs text-muted-foreground">{doc.memberNumber}</p>
                          </div>
                        </TableCell>
                        <TableCell>{doc.documentType}</TableCell>
                        <TableCell>{doc.uploadedByName}</TableCell>
                        <TableCell>{new Date(doc.uploadDate).toLocaleDateString()}</TableCell>
                        <TableCell>{getStatusBadge(doc.verificationStatus)}</TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleVerifyDocument(doc.id)}
                              disabled={loading}
                            >
                              <CheckCircle className="h-4 w-4 mr-1" />
                              Verify
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setSelectedDocForRejection(doc.id)}
                              disabled={loading}
                            >
                              <XCircle className="h-4 w-4 mr-1" />
                              Reject
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Incomplete KYC Tab */}
      {activeTab === "incomplete" && (
        <div className="space-y-4">
          {membersWithIncompleteKyc.length > 0 && (
            <div className="flex justify-end">
              <Button
                onClick={handleBulkApprove}
                disabled={selectedMembers.size === 0 || loading}
              >
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Approving...
                  </>
                ) : (
                  <>
                    <CheckCircle className="mr-2 h-4 w-4" />
                    Approve Selected ({selectedMembers.size})
                  </>
                )}
              </Button>
            </div>
          )}

          {membersWithIncompleteKyc.length === 0 ? (
            <Card>
              <CardContent className="text-center py-12 text-muted-foreground">
                <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500" />
                <p>All members have complete KYC</p>
              </CardContent>
            </Card>
          ) : (
            membersWithIncompleteKyc.map((member) => (
              <Card key={member.memberId}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <Checkbox
                        checked={selectedMembers.has(member.memberId)}
                        onCheckedChange={() => toggleMemberSelection(member.memberId)}
                        disabled={!member.allDocumentsVerified}
                      />
                      <div>
                        <CardTitle className="text-lg">{member.memberName}</CardTitle>
                        <p className="text-sm text-muted-foreground">{member.memberNumber}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-medium">
                        {member.documentsVerified}/{member.totalDocumentsRequired} Documents Verified
                      </p>
                      <div className="w-32 h-2 bg-gray-200 rounded-full mt-2">
                        <div
                          className="h-full bg-green-500 rounded-full transition-all"
                          style={{
                            width: `${(member.documentsVerified / member.totalDocumentsRequired) * 100}%`,
                          }}
                        />
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {member.documents.map((doc) => (
                      <div key={doc.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                        <div>
                          <p className="font-medium text-sm">{doc.documentType}</p>
                          <p className="text-xs text-muted-foreground">
                            Uploaded: {new Date(doc.uploadDate).toLocaleDateString()}
                          </p>
                        </div>
                        <div className="flex items-center gap-3">
                          {getStatusBadge(doc.verificationStatus)}
                          {doc.verificationStatus === "PENDING" && (
                            <div className="flex gap-2">
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => handleVerifyDocument(doc.id)}
                                disabled={loading}
                              >
                                <CheckCircle className="h-4 w-4" />
                              </Button>
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => setSelectedDocForRejection(doc.id)}
                                disabled={loading}
                              >
                                <XCircle className="h-4 w-4" />
                              </Button>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}

      {/* Rejection Modal */}
      {selectedDocForRejection && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Reject Document</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">Rejection Reason</label>
                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="Enter reason for rejection..."
                  className="w-full p-2 border rounded-lg text-sm"
                  rows={4}
                />
              </div>
              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  onClick={() => {
                    setSelectedDocForRejection(null);
                    setRejectionReason("");
                  }}
                >
                  Cancel
                </Button>
                <Button
                  onClick={() => handleRejectDocument(selectedDocForRejection)}
                  disabled={loading || !rejectionReason.trim()}
                >
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Rejecting...
                    </>
                  ) : (
                    "Reject"
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default KycApproval;
