import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Upload, FileCheck, AlertCircle, Loader2, Users, CheckCircle2, Eye } from "lucide-react";
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

interface UploadedDoc {
  id: number;
  documentType: string;
  fileName: string;
  verificationStatus: string;
  fileUrl?: string;
}

const DOCUMENT_TYPES = [
  { value: "NATIONAL_ID", label: "National ID" },
  { value: "PASSPORT", label: "Passport" },
  { value: "PASSPORT_PHOTO", label: "Passport Photo" },
  { value: "APPLICATION_LETTER", label: "Application Letter" },
  { value: "KRA_PIN_CERTIFICATE", label: "KRA PIN Certificate" },
];

const KycDocumentUpload = () => {
  const { session, role } = useAuth();
  const { toast } = useToast();
  const [members, setMembers] = useState<Member[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState<string>("");
  const [selectedDocType, setSelectedDocType] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadedDocs, setUploadedDocs] = useState<UploadedDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploadedDocTypes, setUploadedDocTypes] = useState<Set<string>>(new Set());

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
        const docs = memberStatus?.documents || [];
        setUploadedDocs(docs);
        // Track which document types have been uploaded
        const uploadedTypes = new Set(docs.map((doc: UploadedDoc) => doc.documentType));
        setUploadedDocTypes(uploadedTypes);
      }
    } catch (error) {
      console.error("Error fetching member documents:", error);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      if (selectedFile.size > 10 * 1024 * 1024) {
        toast({
          title: "Error",
          description: "File size exceeds 10MB limit",
          variant: "destructive",
        });
        return;
      }

      const allowedTypes = [
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      ];

      if (!allowedTypes.includes(selectedFile.type)) {
        toast({
          title: "Error",
          description: "File type not allowed. Use PDF, JPEG, PNG, DOC, or DOCX",
          variant: "destructive",
        });
        return;
      }

      setFile(selectedFile);
    }
  };

  const handleUpload = async () => {
    if (!selectedMemberId || !selectedDocType || !file) {
      toast({
        title: "Error",
        description: "Please select member, document type, and file",
        variant: "destructive",
      });
      return;
    }

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append("documentType", selectedDocType);
      formData.append("file", file);

      const response = await fetch(
        `${API_BASE_URL}/kyc-documents/upload/${selectedMemberId}`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${session?.token}`,
          },
          body: formData,
        }
      );

      if (response.ok) {
        const result = await response.json();
        toast({
          title: "Success",
          description: "Document uploaded successfully",
        });
        setFile(null);
        setSelectedDocType("");
        // Refresh the document list
        fetchMemberDocuments();
      } else {
        const error = await response.json();
        toast({
          title: "Error",
          description: error.message || "Failed to upload document",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to upload document",
        variant: "destructive",
      });
    } finally {
      setUploading(false);
    }
  };

  const handleViewDocument = (doc: UploadedDoc) => {
    if (!session?.token) {
      toast({
        title: "Error",
        description: "Not authenticated",
        variant: "destructive",
      });
      return;
    }
    
    const url = `${API_BASE_URL}/kyc-documents/${doc.id}/download`;
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
          doc.fileName || 'document.pdf',
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

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">Upload KYC Documents</h1>
        <p className="text-muted-foreground">Attach KYC documents to members for compliance verification</p>
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
                <SelectValue placeholder="Select a member to upload documents for" />
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

        {/* Document Upload */}
        {selectedMember && (
          <Card>
            <CardHeader>
              <CardTitle>Upload KYC Documents</CardTitle>
              <p className="text-sm text-muted-foreground mt-2">
                Uploading for: <strong>{selectedMember.memberNumber}</strong> - {selectedMember.firstName} {selectedMember.lastName}
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex gap-3">
                <AlertCircle className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-blue-800">
                  <p className="font-medium">Required Documents</p>
                  <p className="mt-1">
                    Please upload all 5 required documents: National ID, Passport, Passport Photo, Application Letter, and KRA PIN Certificate
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Document Type</label>
                  <Select value={selectedDocType} onValueChange={setSelectedDocType}>
                    <SelectTrigger className={uploadedDocTypes.has(selectedDocType) ? "border-amber-300 bg-amber-50" : ""}>
                      <SelectValue placeholder="Select document type" />
                    </SelectTrigger>
                    <SelectContent>
                      {DOCUMENT_TYPES.map((doc) => {
                        const isUploaded = uploadedDocTypes.has(doc.value);
                        return (
                          <SelectItem key={doc.value} value={doc.value} disabled={isUploaded}>
                            {isUploaded ? `✓ ${doc.label} (uploaded)` : doc.label}
                          </SelectItem>
                        );
                      })}
                    </SelectContent>
                  </Select>
                  {selectedDocType && uploadedDocTypes.has(selectedDocType) && (
                    <div className="mt-2 p-2 bg-amber-50 border border-amber-200 rounded text-sm text-amber-800 flex items-start gap-2">
                      <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="font-medium">Document Already Uploaded</p>
                        <p className="text-xs">Upload a new version to replace it, or click the Update button below.</p>
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">Select File</label>
                  <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-gray-400 transition">
                    <input
                      type="file"
                      onChange={handleFileSelect}
                      className="hidden"
                      id="file-input"
                      accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                    />
                    <label htmlFor="file-input" className="cursor-pointer">
                      <Upload className="h-8 w-8 mx-auto mb-2 text-gray-400" />
                      <p className="text-sm font-medium text-gray-700">
                        {file ? file.name : "Click to select or drag and drop"}
                      </p>
                      <p className="text-xs text-gray-500 mt-1">
                        PDF, JPEG, PNG, DOC, DOCX (Max 10MB)
                      </p>
                    </label>
                  </div>
                </div>

                <Button
                  onClick={handleUpload}
                  disabled={!selectedMemberId || !selectedDocType || !file || uploading}
                  className="w-full"
                >
                  {uploading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Uploading...
                    </>
                  ) : uploadedDocTypes.has(selectedDocType) ? (
                    <>
                      <Upload className="mr-2 h-4 w-4" />
                      Update Document
                    </>
                  ) : (
                    <>
                      <Upload className="mr-2 h-4 w-4" />
                      Upload Document
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Uploaded Documents */}
        {uploadedDocs.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileCheck className="h-5 w-5" />
                Uploaded Documents ({uploadedDocs.length}/5)
              </CardTitle>
              <div className="mt-3 w-full h-2 bg-gray-200 rounded-full">
                <div
                  className="h-full bg-green-500 rounded-full transition-all"
                  style={{ width: `${(uploadedDocs.length / 5) * 100}%` }}
                />
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {uploadedDocs.map((doc) => (
                  <div
                    key={doc.id}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex-1">
                      <p className="font-medium text-sm">{doc.documentType}</p>
                      <p className="text-xs text-gray-500">{doc.fileName}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span
                        className={`text-xs px-2 py-1 rounded-full ${
                          doc.verificationStatus === "VERIFIED"
                            ? "bg-green-100 text-green-800"
                            : doc.verificationStatus === "REJECTED"
                            ? "bg-red-100 text-red-800"
                            : "bg-yellow-100 text-yellow-800"
                        }`}
                      >
                        {doc.verificationStatus}
                      </span>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleViewDocument(doc)}
                        title="View/Download document"
                      >
                        <Eye className="h-4 w-4 mr-1" />
                        View
                      </Button>
                      {role === "CUSTOMER_SUPPORT" && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              setSelectedDocType(doc.documentType);
                              setFile(null);
                            }}
                            title="Update document"
                          >
                            <Upload className="h-4 w-4 mr-1" />
                            Update
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                            onClick={async () => {
                              if (confirm("Are you sure you want to delete this document?")) {
                                try {
                                  const response = await fetch(
                                    `${API_BASE_URL}/kyc-documents/${doc.id}`,
                                    {
                                      method: "DELETE",
                                      headers: { Authorization: `Bearer ${session?.token}` },
                                    }
                                  );
                                  if (response.ok) {
                                    toast({
                                      title: "Success",
                                      description: "Document deleted successfully",
                                    });
                                    fetchMemberDocuments();
                                  }
                                } catch (error) {
                                  toast({
                                    title: "Error",
                                    description: "Failed to delete document",
                                    variant: "destructive",
                                  });
                                }
                              }
                            }}
                            title="Delete document"
                          >
                            <span className="text-xs mr-1">✕</span>
                            Delete
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default KycDocumentUpload;
