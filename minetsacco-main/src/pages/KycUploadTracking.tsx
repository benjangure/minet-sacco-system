import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { CheckCircle, XCircle, Clock, Download, Eye } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { downloadAndOpenFile } from "@/utils/downloadHelper";

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
  rejectionReason?: string;
}

const KycUploadTracking = () => {
  const [documents, setDocuments] = useState<KycDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const { session } = useAuth();
  const { toast } = useToast();

  useEffect(() => {
    if (session) {
      fetchMyUploadedDocuments();
    }
  }, [session]);

  const fetchMyUploadedDocuments = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/kyc-documents/my-uploads`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const result = await response.json();
        setDocuments(result.data || []);
      } else {
        toast({
          title: "Error",
          description: "Failed to load your uploaded documents",
          variant: "destructive",
        });
      }
    } catch (error) {
      console.error("Error fetching uploaded documents:", error);
      toast({
        title: "Error",
        description: "Failed to load your uploaded documents",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleViewDocument = async (documentId: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/kyc-documents/${documentId}/download`, {
        headers: { Authorization: `Bearer ${session?.token}` },
      });
      if (!response.ok) {
        const errorText = await response.text();
        try {
          const errorObj = JSON.parse(errorText);
          throw new Error(errorObj.message || `HTTP ${response.status}`);
        } catch {
          throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
      }
      const blob = await response.blob();
      await downloadAndOpenFile(
        blob,
        'document.pdf',
        (message) => toast({ title: 'Success', description: message }),
        (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
      );
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to download document';
      toast({
        title: "Error",
        description: errorMsg,
        variant: "destructive",
      });
    }
  };

  const getStatusBadge = (status: string, rejectionReason?: string) => {
    switch (status) {
      case "VERIFIED":
        return (
          <div className="flex items-center gap-2">
            <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full flex items-center gap-1">
              <CheckCircle className="h-3 w-3" />
              Verified
            </span>
          </div>
        );
      case "REJECTED":
        return (
          <div className="flex flex-col gap-1">
            <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full flex items-center gap-1">
              <XCircle className="h-3 w-3" />
              Rejected
            </span>
            {rejectionReason && (
              <span className="text-xs text-red-600 italic">Reason: {rejectionReason}</span>
            )}
          </div>
        );
      default:
        return (
          <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full flex items-center gap-1">
            <Clock className="h-3 w-3" />
            Pending
          </span>
        );
    }
  };

  const groupedByMember = documents.reduce((acc, doc) => {
    const key = `${doc.memberId}-${doc.memberNumber}`;
    if (!acc[key]) {
      acc[key] = {
        memberId: doc.memberId,
        memberNumber: doc.memberNumber,
        memberName: doc.memberName,
        documents: [],
      };
    }
    acc[key].documents.push(doc);
    return acc;
  }, {} as Record<string, any>);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <p className="text-muted-foreground">Loading your uploaded documents...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">KYC Upload Tracking</h1>
        <p className="text-muted-foreground">Track the status of KYC documents you have uploaded</p>
      </div>

      {documents.length === 0 ? (
        <Card>
          <CardContent className="text-center py-12 text-muted-foreground">
            <CheckCircle className="h-12 w-12 mx-auto mb-4 text-gray-400" />
            <p>You have not uploaded any KYC documents yet</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {Object.values(groupedByMember).map((group: any) => (
            <Card key={`${group.memberId}-${group.memberNumber}`}>
              <CardHeader>
                <CardTitle className="text-lg">
                  {group.memberName} ({group.memberNumber})
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Document Type</TableHead>
                        <TableHead>Upload Date</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {group.documents.map((doc: KycDocument) => (
                        <TableRow key={doc.id}>
                          <TableCell className="font-medium">{doc.documentType}</TableCell>
                          <TableCell>{new Date(doc.uploadDate).toLocaleDateString()}</TableCell>
                          <TableCell>{getStatusBadge(doc.verificationStatus, doc.rejectionReason)}</TableCell>
                          <TableCell>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleViewDocument(doc.id)}
                            >
                              <Eye className="h-4 w-4 mr-1" />
                              View
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export default KycUploadTracking;
