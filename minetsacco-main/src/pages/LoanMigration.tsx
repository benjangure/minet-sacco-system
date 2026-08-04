import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useRefresh } from "@/contexts/RefreshContext";
import { useToast } from "@/hooks/use-toast";
import api from "@/config/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Upload,
  FileSpreadsheet,
  CheckCircle,
  XCircle,
  AlertCircle,
  Download,
  Info,
} from "lucide-react";

interface MigrationResult {
  batchId: number;
  batchNumber: string;
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  status: string;
  totalPrincipal: number;
  message: string;
}

interface LoanProduct {
  id: number;
  name: string;
  interestRate: number;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  isActive: boolean;
}

interface MigrationItem {
  id: number;
  rowNumber: number;
  employeeId: string;
  loanProductName: string;
  principalAmount: number;
  termMonths: number;
  disbursementDate: string;
  loanStatus: string;
  outstandingBalance: number;
  guarantorshipType: string;
  status: string;
  errorMessage: string | null;
  totalInterest: number | null;
  totalRepayable: number | null;
  monthlyRepayment: number | null;
}

export default function LoanMigration() {
  const { session } = useAuth();
  const { toast } = useToast();
  const { refreshKey } = useRefresh();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<MigrationResult | null>(null);
  const [items, setItems] = useState<MigrationItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [loanProducts, setLoanProducts] = useState<LoanProduct[]>([]);

  // Fetch active loan products on mount
  useEffect(() => {
    api.get("/loan-products")
      .then((res) => {
        const products = res.data?.data || res.data || [];
        setLoanProducts(products.filter((p: LoanProduct) => p.isActive));
      })
      .catch(() => {
        // Non-critical - template will still work with placeholder names
      });
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
        toast({ title: "Invalid file", description: "Only Excel (.xlsx, .xls) files are supported.", variant: "destructive" });
        return;
      }
      setSelectedFile(file);
      setResult(null);
      setItems([]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploading(true);
    setResult(null);
    setItems([]);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      const response = await api.post("/loan-migration/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      const data: MigrationResult = response.data;
      setResult(data);

      // Load item details
      if (data.batchId) {
        await loadItems(data.batchId);
      }

      if (data.status === "COMPLETED") {
        toast({ title: "Migration complete", description: data.message });
      } else if (data.status === "PARTIALLY_COMPLETED") {
        toast({ title: "Partial success", description: data.message, variant: "destructive" });
      } else {
        toast({ title: "Migration failed", description: data.message, variant: "destructive" });
      }
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || "Upload failed";
      toast({ title: "Error", description: msg, variant: "destructive" });
    } finally {
      setUploading(false);
    }
  };

  const loadItems = async (batchId: number) => {
    setLoadingItems(true);
    try {
      const response = await api.get(`/loan-migration/batch/${batchId}/items`);
      setItems(response.data);
    } catch (err) {
      // Non-critical
    } finally {
      setLoadingItems(false);
    }
  };

  const downloadTemplate = async () => {
    try {
      const response = await api.get("/loan-migration/template/download", {
        responseType: "blob",
      });
      const url = URL.createObjectURL(response.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = "loan_migration_template.xlsx";
      a.click();
      URL.revokeObjectURL(url);
      toast({
        title: "Template Downloaded",
        description: "Loan migration template downloaded successfully.",
      });
    } catch (err) {
      toast({
        title: "Download Failed",
        description: "Could not download the template. Please try again.",
        variant: "destructive",
      });
    }
  };

  const formatCurrency = (amount: number | null) => {
    if (amount == null) return "-";
    return `KES ${amount.toLocaleString("en-KE", { minimumFractionDigits: 2 })}`;
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "SUCCESS":
        return <Badge className="bg-green-100 text-green-800">Imported</Badge>;
      case "FAILED":
        return <Badge variant="destructive">Failed</Badge>;
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  };

  const getLoanStatusBadge = (status: string) => {
    switch (status) {
      case "DISBURSED":
        return <Badge className="bg-blue-100 text-blue-800">Disbursed</Badge>;
      case "REPAID":
        return <Badge className="bg-green-100 text-green-800">Repaid</Badge>;
      case "DEFAULTED":
        return <Badge className="bg-red-100 text-red-800">Defaulted</Badge>;
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  };

  const failedItems = items.filter((i) => i.status === "FAILED");
  const successItems = items.filter((i) => i.status === "SUCCESS");

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Loan Migration</h1>
        <p className="text-muted-foreground mt-1">
          Import historical loan records (DISBURSED, REPAID, DEFAULTED) from the previous system.
          Active loans will automatically freeze guarantor savings and reduce eligibility.
        </p>
      </div>

      {/* Instructions */}
      <Alert>
        <Info className="h-4 w-4" />
        <AlertDescription>
          <strong>Before uploading:</strong> Ensure all members are already registered in the system.
          Guarantors must also be registered members. Pledges for NORMAL guarantorship must sum exactly to the principal amount.
        </AlertDescription>
      </Alert>

      {/* Active Loan Products - so user knows exact names to use */}
      {loanProducts.length > 0 && (
        <Card className="border-blue-200 bg-blue-50">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-blue-800 flex items-center gap-2">
              <Info className="h-4 w-4" />
              Active Loan Products — use these exact names in the "Loan Product Name" column
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-blue-700 mb-2">
              Interest rates shown below are used for all calculations (total repayable, monthly repayment). The interest rate column in the template is ignored.
            </p>
            <div className="flex flex-wrap gap-2">
              {loanProducts.map((p) => (
                <div key={p.id} className="bg-white border border-blue-200 rounded-md px-3 py-1.5 text-sm">
                  <span className="font-medium text-blue-900">{p.name}</span>
                  <span className="text-blue-600 ml-2 text-xs">
                    {p.interestRate}% p.a. | KES {p.minAmount?.toLocaleString()}–{p.maxAmount?.toLocaleString()} | {p.minTermMonths}–{p.maxTermMonths} months
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Upload Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Upload className="h-5 w-5" />
            Upload Loan Migration File
          </CardTitle>
          <CardDescription>
            Excel file (.xlsx or .xls) with one loan per row. Up to 6 guarantors supported.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <Button variant="outline" size="sm" onClick={downloadTemplate} className="gap-2">
              <Download className="h-4 w-4" />
              Download Template
            </Button>
            <span className="text-sm text-muted-foreground">Download the CSV template to see the required column format</span>
          </div>

          <div className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-6 text-center space-y-3">
            <FileSpreadsheet className="h-10 w-10 mx-auto text-muted-foreground" />
            <div>
              <label htmlFor="migration-file" className="cursor-pointer">
                <span className="text-primary font-medium hover:underline">Choose Excel file</span>
                <span className="text-muted-foreground"> or drag and drop</span>
              </label>
              <input
                id="migration-file"
                type="file"
                accept=".xlsx,.xls"
                className="hidden"
                onChange={handleFileChange}
              />
            </div>
            {selectedFile && (
              <p className="text-sm font-medium text-green-700">
                ✓ {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
              </p>
            )}
          </div>

          <Button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="w-full"
          >
            {uploading ? "Processing..." : "Upload & Import Loans"}
          </Button>
        </CardContent>
      </Card>

      {/* Result Summary */}
      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              {result.status === "COMPLETED" ? (
                <CheckCircle className="h-5 w-5 text-green-600" />
              ) : result.status === "PARTIALLY_COMPLETED" ? (
                <AlertCircle className="h-5 w-5 text-yellow-600" />
              ) : (
                <XCircle className="h-5 w-5 text-red-600" />
              )}
              Migration Result
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <div className="text-center p-3 bg-muted rounded-lg">
                <div className="text-2xl font-bold">{result.totalRecords}</div>
                <div className="text-sm text-muted-foreground">Total Rows</div>
              </div>
              <div className="text-center p-3 bg-green-50 rounded-lg">
                <div className="text-2xl font-bold text-green-700">{result.successfulRecords}</div>
                <div className="text-sm text-muted-foreground">Imported</div>
              </div>
              <div className="text-center p-3 bg-red-50 rounded-lg">
                <div className="text-2xl font-bold text-red-700">{result.failedRecords}</div>
                <div className="text-sm text-muted-foreground">Failed</div>
              </div>
              <div className="text-center p-3 bg-blue-50 rounded-lg">
                <div className="text-sm font-bold text-blue-700">{formatCurrency(result.totalPrincipal)}</div>
                <div className="text-sm text-muted-foreground">Total Principal</div>
              </div>
            </div>
            <p className="text-sm text-muted-foreground">{result.message}</p>
          </CardContent>
        </Card>
      )}

      {/* Failed Items - shown prominently */}
      {failedItems.length > 0 && (
        <Card className="border-red-200">
          <CardHeader>
            <CardTitle className="text-red-700 flex items-center gap-2">
              <XCircle className="h-5 w-5" />
              {failedItems.length} Row{failedItems.length > 1 ? "s" : ""} Failed - Fix and Re-upload
            </CardTitle>
            <CardDescription>
              Correct these errors in your Excel file and upload again. Successfully imported loans are not affected.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {failedItems.map((item) => (
                <div key={item.id} className="flex items-start gap-3 p-3 bg-red-50 rounded-lg text-sm">
                  <XCircle className="h-4 w-4 text-red-500 mt-0.5 shrink-0" />
                  <div>
                    <span className="font-medium">Row {item.rowNumber}</span>
                    {item.employeeId && <span className="text-muted-foreground"> ({item.employeeId})</span>}
                    <span className="text-red-700">: {item.errorMessage}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Successfully imported items */}
      {successItems.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CheckCircle className="h-5 w-5 text-green-600" />
              {successItems.length} Loan{successItems.length > 1 ? "s" : ""} Imported Successfully
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Row</TableHead>
                    <TableHead>Employee ID</TableHead>
                    <TableHead>Product</TableHead>
                    <TableHead>Principal</TableHead>
                    <TableHead>Term</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Outstanding</TableHead>
                    <TableHead>Monthly Repayment</TableHead>
                    <TableHead>Guarantorship</TableHead>
                    <TableHead>Result</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {successItems.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>{item.rowNumber}</TableCell>
                      <TableCell className="font-medium">{item.employeeId}</TableCell>
                      <TableCell>{item.loanProductName}</TableCell>
                      <TableCell>{formatCurrency(item.principalAmount)}</TableCell>
                      <TableCell>{item.termMonths}m</TableCell>
                      <TableCell>{getLoanStatusBadge(item.loanStatus)}</TableCell>
                      <TableCell>{formatCurrency(item.outstandingBalance)}</TableCell>
                      <TableCell>{formatCurrency(item.monthlyRepayment)}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{item.guarantorshipType}</Badge>
                      </TableCell>
                      <TableCell>{getStatusBadge(item.status)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
