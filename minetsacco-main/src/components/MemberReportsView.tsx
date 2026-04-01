import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { FileText, Download, Loader2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';
import { downloadAndOpenFile } from '@/utils/downloadHelper';

export default function MemberReportsView() {
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const downloadReport = async (endpoint: string, filename: string) => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URL}/member/${endpoint}`, {
        headers: { Authorization: `Bearer ${token}` }
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
        filename,
        (message) => toast({ title: 'Success', description: message }),
        (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
      );
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to download report';
      toast({ title: 'Error', description: errorMsg, variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadAccountStatement = () => {
    downloadReport('account-statement', 'account-statement.pdf');
  };

  const handleDownloadLoanStatement = () => {
    downloadReport('loan-statement', 'loan-statement.pdf');
  };

  const handleDownloadTransactionHistory = () => {
    downloadReport('transaction-history', 'transaction-history.pdf');
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Available Reports</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid gap-3 md:grid-cols-1 lg:grid-cols-3">
            <Card className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                  <FileText className="h-4 w-4 text-primary" />
                  Account Statement
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground mb-3">Download your account statement with all transactions</p>
                <Button
                  size="sm"
                  onClick={handleDownloadAccountStatement}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                      Loading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4 mr-1" />
                      View
                    </>
                  )}
                </Button>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                  <FileText className="h-4 w-4 text-primary" />
                  Loan Statement
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground mb-3">Download your loan statement with repayment details</p>
                <Button
                  size="sm"
                  onClick={handleDownloadLoanStatement}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                      Loading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4 mr-1" />
                      View
                    </>
                  )}
                </Button>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                  <FileText className="h-4 w-4 text-primary" />
                  Transaction History
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground mb-3">Download your complete transaction history</p>
                <Button
                  size="sm"
                  onClick={handleDownloadTransactionHistory}
                  disabled={loading}
                  className="w-full"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                      Loading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4 mr-1" />
                      View
                    </>
                  )}
                </Button>
              </CardContent>
            </Card>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
