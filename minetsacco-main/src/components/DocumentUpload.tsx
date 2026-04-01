import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Upload, FileText, Image, CheckCircle, Eye, X, Download, RefreshCw } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { Badge } from "@/components/ui/badge";
import { downloadAndOpenFile } from "@/utils/downloadHelper";

const API_BASE_URL = "http://localhost:8080/api";

interface DocumentUploadProps {
  memberId: number;
  token: string;
  documents: {
    idDocumentPath?: string;
    photoPath?: string;
    applicationLetterPath?: string;
    kraPinPath?: string;
  };
  onUploadComplete: () => void;
  readOnly?: boolean;
}

const REQUIRED_DOCUMENTS = [
  { type: "id", label: "National ID / Passport", icon: FileText, accept: "image/jpeg,image/png,application/pdf" },
  { type: "photo", label: "Passport Photo", icon: Image, accept: "image/jpeg,image/png" },
  { type: "application", label: "Application Letter (Signed)", icon: FileText, accept: "application/pdf,image/jpeg,image/png" },
  { type: "kra", label: "KRA PIN Certificate", icon: FileText, accept: "application/pdf,image/jpeg,image/png" },
];

export function DocumentUpload({ memberId, token, documents, onUploadComplete, readOnly = false }: DocumentUploadProps) {
  const [uploading, setUploading] = useState<string | null>(null);
  const [previewDoc, setPreviewDoc] = useState<{ type: string; url: string; label: string; contentType?: string } | null>(null);
  const [thumbnails, setThumbnails] = useState<Record<string, string>>({});
  const { toast } = useToast();

  const getDocumentPath = (type: string) => {
    switch (type) {
      case "id": return documents.idDocumentPath;
      case "photo": return documents.photoPath;
      case "application": return documents.applicationLetterPath;
      case "kra": return documents.kraPinPath;
      default: return undefined;
    }
  };

  const getFileName = (path?: string) => {
    if (!path) return null;
    const parts = path.split('/');
    return parts[parts.length - 1];
  };

  const isImageFile = (path?: string) => {
    if (!path) return false;
    return path.toLowerCase().match(/\.(jpg|jpeg|png)$/);
  };

  const isPdfFile = (path?: string) => {
    if (!path) return false;
    return path.toLowerCase().endsWith('.pdf');
  };

  // Load thumbnails for uploaded images
  useEffect(() => {
    const loadThumbnails = async () => {
      for (const doc of REQUIRED_DOCUMENTS) {
        const path = getDocumentPath(doc.type);
        if (path && isImageFile(path)) {
          try {
            const response = await fetch(`${API_BASE_URL}/members/${memberId}/document/${doc.type}`, {
              headers: { "Authorization": `Bearer ${token}` },
            });
            if (response.ok) {
              const blob = await response.blob();
              const url = URL.createObjectURL(blob);
              setThumbnails(prev => ({ ...prev, [doc.type]: url }));
            }
          } catch (error) {
            console.error(`Failed to load thumbnail for ${doc.type}:`, error);
          }
        }
      }
    };

    loadThumbnails();

    // Cleanup blob URLs on unmount
    return () => {
      Object.values(thumbnails).forEach(url => URL.revokeObjectURL(url));
    };
  }, [memberId, token, documents]);

  const handleFileUpload = async (file: File, documentType: string) => {
    if (file.size > 5 * 1024 * 1024) {
      toast({ title: "Error", description: "File size must not exceed 5MB", variant: "destructive" });
      return;
    }

    setUploading(documentType);
    const formData = new FormData();
    formData.append("file", file);
    formData.append("documentType", documentType);

    try {
      const response = await fetch(`${API_BASE_URL}/members/${memberId}/upload-document`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
        },
        body: formData,
      });

      if (response.ok) {
        toast({ title: "Success", description: "Document uploaded successfully" });
        onUploadComplete();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Upload failed", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Upload failed", variant: "destructive" });
    } finally {
      setUploading(null);
    }
  };

  const handlePreview = async (type: string, label: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/members/${memberId}/document/${type}`, {
        headers: {
          "Authorization": `Bearer ${token}`,
        },
      });
      
      if (response.ok) {
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const contentType = response.headers.get('content-type') || '';
        setPreviewDoc({ type, url, label, contentType });
      } else {
        toast({ title: "Error", description: "Failed to load document", variant: "destructive" });
      }
    } catch (error) {
      console.error("Preview error:", error);
      toast({ title: "Error", description: "Failed to load document", variant: "destructive" });
    }
  };

  const handleDownload = async (type: string, label: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/members/${memberId}/document/${type}`, {
        headers: {
          "Authorization": `Bearer ${token}`,
        },
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
        `${label}_Member${memberId}`,
        (message) => toast({ title: 'Success', description: message }),
        (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
      );
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to download document';
      console.error("Download error:", error);
      toast({ title: "Error", description: errorMsg, variant: "destructive" });
    }
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {REQUIRED_DOCUMENTS.map((doc) => {
          const isUploaded = !!getDocumentPath(doc.type);
          const documentPath = getDocumentPath(doc.type);
          const fileName = getFileName(documentPath);
          const isImage = isImageFile(documentPath);
          const isPdf = isPdfFile(documentPath);
          const Icon = doc.icon;

          return (
            <Card key={doc.type} className={isUploaded ? "border-green-200 bg-green-50/50" : "border-dashed"}>
              <CardContent className="pt-4">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <Icon className="h-4 w-4 text-muted-foreground" />
                    <Label className="text-sm font-medium">{doc.label}</Label>
                  </div>
                  {isUploaded && (
                    <Badge variant="outline" className="bg-green-100 text-green-800 border-green-200">
                      <CheckCircle className="h-3 w-3 mr-1" />
                      Uploaded
                    </Badge>
                  )}
                </div>

                {/* Document Preview/Info */}
                {isUploaded && (
                  <div className="mb-3 p-3 bg-white rounded border">
                    {isImage ? (
                      <div className="flex items-center gap-3">
                        {thumbnails[doc.type] ? (
                          <img 
                            src={thumbnails[doc.type]}
                            alt={doc.label}
                            className="h-16 w-16 object-cover rounded border cursor-pointer hover:opacity-80"
                            onClick={() => handlePreview(doc.type, doc.label)}
                          />
                        ) : (
                          <div className="h-16 w-16 bg-gray-100 rounded border flex items-center justify-center">
                            <Image className="h-8 w-8 text-gray-400 animate-pulse" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-medium truncate">{fileName}</p>
                          <p className="text-xs text-muted-foreground">Image file</p>
                        </div>
                      </div>
                    ) : isPdf ? (
                      <div className="flex items-center gap-3">
                        <div className="h-16 w-16 bg-red-50 rounded border border-red-200 flex items-center justify-center">
                          <FileText className="h-8 w-8 text-red-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-medium truncate">{fileName}</p>
                          <p className="text-xs text-muted-foreground">PDF document</p>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center gap-3">
                        <div className="h-16 w-16 bg-gray-50 rounded border flex items-center justify-center">
                          <FileText className="h-8 w-8 text-gray-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-medium truncate">{fileName}</p>
                          <p className="text-xs text-muted-foreground">Document</p>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex gap-2">
                  {!readOnly && (
                    <Button
                      variant={isUploaded ? "outline" : "default"}
                      size="sm"
                      className="flex-1"
                      disabled={uploading === doc.type}
                      onClick={() => {
                        const input = document.createElement("input");
                        input.type = "file";
                        input.accept = doc.accept;
                        input.onchange = (e) => {
                          const file = (e.target as HTMLInputElement).files?.[0];
                          if (file) handleFileUpload(file, doc.type);
                        };
                        input.click();
                      }}
                    >
                      {uploading === doc.type ? (
                        <>
                          <RefreshCw className="h-3 w-3 mr-1 animate-spin" />
                          Uploading...
                        </>
                      ) : isUploaded ? (
                        <>
                          <Upload className="h-3 w-3 mr-1" />
                          Replace
                        </>
                      ) : (
                        <>
                          <Upload className="h-3 w-3 mr-1" />
                          Upload
                        </>
                      )}
                    </Button>
                  )}
                  {isUploaded && (
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePreview(doc.type, doc.label)}
                        title="View document"
                      >
                        <Eye className="h-3 w-3" />
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDownload(doc.type, doc.label)}
                        title="Download document"
                      >
                        <Download className="h-3 w-3" />
                      </Button>
                    </>
                  )}
                </div>

                {!isUploaded && (
                  <p className="text-xs text-muted-foreground mt-2">
                    Accepted: {doc.accept.split(',').map(t => t.split('/')[1].toUpperCase()).join(', ')} (Max 5MB)
                  </p>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Document Preview Modal */}
      {previewDoc && (
        <div className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4" onClick={() => {
          URL.revokeObjectURL(previewDoc.url);
          setPreviewDoc(null);
        }}>
          <div className="bg-white rounded-lg max-w-5xl w-full max-h-[95vh] overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b bg-gray-50">
              <h3 className="font-semibold text-lg">{previewDoc.label}</h3>
              <div className="flex gap-2">
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => handleDownload(previewDoc.type, previewDoc.label)}
                >
                  <Download className="h-4 w-4 mr-1" />
                  Download
                </Button>
                <Button variant="ghost" size="icon" onClick={() => {
                  URL.revokeObjectURL(previewDoc.url);
                  setPreviewDoc(null);
                }}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
            <div className="flex-1 overflow-auto bg-gray-100 p-4">
              <div className="bg-white rounded shadow-lg flex items-center justify-center min-h-[80vh]">
                {previewDoc.contentType?.startsWith('image/') ? (
                  <img 
                    src={previewDoc.url} 
                    alt={previewDoc.label}
                    className="max-w-full max-h-[80vh] object-contain"
                  />
                ) : (
                  <iframe
                    src={previewDoc.url}
                    className="w-full h-[80vh] border-0"
                    title="Document Preview"
                  />
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
