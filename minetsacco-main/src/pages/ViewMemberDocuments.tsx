import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileCheck, AlertCircle, Loader2, Users, Eye, Download } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { downloadAndOpenFile } from "@/utils/downloadHelper";

const API_BASE_URL = "http://localhost:8080/api";

interface Member {
  id: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
  status: string;
}

interface KycDocument {
  id: number;
  documentType: string;
  fileName: string;
  verificationStatus: string;
  uploadDate: string;
  uploadedByName: string;
}

const ViewMemberDocuments = () => {
  const { session, role } = useAuth();
  const { toast } = useToast();
  const [members, setMembers] = useState<Member[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState<string>("");
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchingDocs, setFetchingDocs] = useState(false);

  useEffect(() => {
    fetchMembers();
  }, [session]);

  useEffect(() => {
    if (selectedMemberId) {
      fetchMemberDocuments();
    }
  }, [selectedMemberId]);

  const fetchMembers = async () => {
    if (!session?.token) return;
    try {
      const response = await fetch(`${API_BASE_URL}/members`, {
        headers: { Authorization: `Bearer ${session.token}` },
      });
      if (response.ok) {
        const result = await response.json();
        setMembers(result.data || []);
      }
    } catch (error) {
      console.error("Error fetching members:", error);
      toast({
        title: "Error",
        description: "Failed to load members",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchMemberDocuments = async () => {
    if (!session?.token || !selectedMemberId) return;
    setFetchingDocs(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/kyc-documents/member/${selectedMemberId}`,
        {
          headers: { Authorization: `Bearer ${session.token}` },
        }
      );
      if (response.ok) {
        const result = await response.json();
        // The API returns a MemberKycStatusDTO with documents array inside
        const memberStatus = result.data;
        setDocuments(memberStatus?.documents || []);
      }
    } catch (error) {
      console.error("Error fetching documents:", error);
      toast({
        title: "Error",
        description: "Failed to load documents",
        variant: "destructive",
      });
    } finally {
      setFetchingDocs(false);
    }
  };

  const handleViewDocument = (documentId: number) => {
    if (!session?.token) {
      toast({
        title: "Error",
        description: "Not authenticated",
        variant: "destructive",
      });
      return;
    }

    const url = `${API_BASE_URL}/kyc-documents/${documentId}/download`;
    fetch(url, {
      headers: { Authorization: `Bearer ${session.token}` },
    })
      .then(async res => {
        if (!res.ok) {
          const errorText = await res.text();
          try {
            const errorObj = JSON.parse(errorText);
            throw new Error(errorObj.message || `HTTP ${res.status}`);
          } catch {
            throw new Error(`HTTP ${res.status}: ${errorText}`);
          }
        }
        return res.blob();
      })
      .then(blob => {
        downloadAndOpenFile(
          blob,
          'document.pdf',
          (message) => toast({ title: 'Success', description: message }),
          (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
        );
      })
      .catch(error => {
        console.error('Error downloading document:', error);
        toast({
          title: "Error",
          description: error.message || "Failed to download document",
          variant: "destructive",
        });
      });
  };

  const selectedMember = members.find((m) => m.id.toString() === selectedMemberId);

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

  const uploadProgress = documents.length > 0 ? (documents.length / 5) * 100 : 0;

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">View Member KYC Documents</h1>
        <p className="text-muted-foreground">Review KYC documents uploaded for members</p>
      </div>

      <div className="grid gap-6">
        {/* Member Selection */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-5 w-5" />
              Select Member
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Select value={selectedMemberId} onValueChange={setSelectedMemberId}>
              <SelectTrigger>
                <SelectValue placeholder="Select a member to view documents" />
              </SelectTrigger>
              <SelectContent>
                {members.map((member) => (
                  <SelectItem key={member.id} value={member.id.toString()}>
                    {member.memberNumber} - {member.firstName} {member.lastName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </CardContent>
        </Card>

        {/* Member Documents */}
        {selectedMember && (
          <>
            <Card>
              <CardHeader>
                <CardTitle>Member Information</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-muted-foreground">Member Number</p>
                    <p className="font-semibold">{selectedMember.memberNumber}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Name</p>
                    <p className="font-semibold">{selectedMember.firstName} {selectedMember.lastName}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Status</p>
                    <p className="font-semibold">{selectedMember.status}</p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Documents Uploaded</p>
                    <p className="font-semibold">{documents.length}/5</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Upload Progress */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileCheck className="h-5 w-5" />
                  KYC Documents Progress
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div>
                    <div className="flex justify-between mb-2">
                      <span className="text-sm font-medium">Overall Progress</span>
                      <span className="text-sm text-muted-foreground">{documents.length}/5 documents</span>
                    </div>
                    <div className="w-full h-3 bg-gray-200 rounded-full">
                      <div
                        className="h-full bg-blue-500 rounded-full transition-all"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Documents List */}
            {fetchingDocs ? (
              <Card>
                <CardContent className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </CardContent>
              </Card>
            ) : documents.length === 0 ? (
              <Card>
                <CardContent className="text-center py-12 text-muted-foreground">
                  <AlertCircle className="h-12 w-12 mx-auto mb-4 text-yellow-500" />
                  <p>No documents uploaded yet for this member</p>
                </CardContent>
              </Card>
            ) : (
              <Card>
                <CardHeader>
                  <CardTitle>Uploaded Documents</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {documents.map((doc) => (
                      <div
                        key={doc.id}
                        className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition"
                      >
                        <div className="flex-1">
                          <p className="font-medium text-sm">{doc.documentType}</p>
                          <p className="text-xs text-muted-foreground mt-1">
                            Uploaded by {doc.uploadedByName} on {new Date(doc.uploadDate).toLocaleDateString()}
                          </p>
                          <p className="text-xs text-gray-500">{doc.fileName}</p>
                        </div>
                        <div className="flex items-center gap-3">
                          {getStatusBadge(doc.verificationStatus)}
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleViewDocument(doc.id)}
                          >
                            <Eye className="h-4 w-4 mr-1" />
                            View
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default ViewMemberDocuments;
