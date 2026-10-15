import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { FileText, Download, Loader2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { getAuthToken, getApiBaseUrl } from '@/config/api';
import { nativeFetch } from '@/utils/nativeHttp';
import { downloadAndOpenFile } from '@/utils/downloadHelper';

type ReportKey = 'account-statement' | 'loan-statement' | 'transaction-history';

const REPORTS: { key: ReportKey; title: string; description: string; filename: string }[] = [
  {
    key: 'account-statement',
    title: 'Account Statement',
    description: 'Savings and shares with opening balance, running balance and closing balance',
    filename: 'account-statement.pdf',
  },
  {
    key: 'loan-statement',
    title: 'Loan Statement',
    description: 'Your loans with terms, outstanding balance and repayments',
    filename: 'loan-statement.pdf',
  },
  {
    key: 'transaction-history',
    title: 'Transaction History',
    description: 'Every transaction in the period with totals',
    filename: 'transaction-history.pdf',
  },
];

const toIso = (d: Date) => d.toISOString().slice(0, 10);

export default function MemberReportsView() {
  const [busy, setBusy] = useState<ReportKey | null>(null);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const { toast } = useToast();

  const setPreset = (preset: 'all' | 'year' | '12m' | '3m') => {
    const today = new Date();
    if (preset === 'all') {
      setStartDate('');
      setEndDate('');
      return;
    }
    const start = new Date(today);
    if (preset === 'year') start.setMonth(0, 1);
    if (preset === '12m') start.setFullYear(start.getFullYear() - 1);
    if (preset === '3m') start.setMonth(start.getMonth() - 3);
    setStartDate(toIso(start));
    setEndDate(toIso(today));
  };

  const downloadReport = async (report: (typeof REPORTS)[number]) => {
    if (startDate && endDate && startDate > endDate) {
      toast({ title: 'Check the dates', description: 'Start date must be before the end date.', variant: 'destructive' });
      return;
    }
    try {
      setBusy(report.key);
      const token = getAuthToken();
      if (!token) {
        throw new Error('Your session has expired. Please sign in again.');
      }

      const params = new URLSearchParams();
      if (startDate) params.set('startDate', startDate);
      if (endDate) params.set('endDate', endDate);
      const qs = params.toString();
      const url = `${getApiBaseUrl()}/member/${report.key}${qs ? `?${qs}` : ''}`;

      const response = await nativeFetch(url, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) {
        let message = `Could not generate the report (error ${response.status}).`;
        try {
          const body = await response.json();
          if (body?.message) message = body.message;
        } catch {
          /* keep default message */
        }
        throw new Error(message);
      }

      const blob = await response.blob();
      if (!blob || blob.size === 0) {
        throw new Error('The report came back empty. Please try again.');
      }

      const pdf = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
      await downloadAndOpenFile(
        pdf,
        report.filename,
        (message) => toast({ title: 'Report ready', description: message }),
        (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
      );
    } catch (error) {
      const msg = error instanceof Error ? error.message : 'Failed to download report';
      toast({ title: 'Error', description: msg, variant: 'destructive' });
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Available Reports</CardTitle>
          <CardDescription>
            Choose a period (leave blank for everything on record), then download a PDF.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1 space-y-1">
              <Label htmlFor="report-start" className="text-xs">From</Label>
              <Input id="report-start" type="date" value={startDate} max={endDate || undefined}
                onChange={(e) => setStartDate(e.target.value)} />
            </div>
            <div className="flex-1 space-y-1">
              <Label htmlFor="report-end" className="text-xs">To</Label>
              <Input id="report-end" type="date" value={endDate} min={startDate || undefined}
                onChange={(e) => setEndDate(e.target.value)} />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setPreset('3m')}>Last 3 months</Button>
            <Button type="button" variant="outline" size="sm" onClick={() => setPreset('12m')}>Last 12 months</Button>
            <Button type="button" variant="outline" size="sm" onClick={() => setPreset('year')}>This year</Button>
            <Button type="button" variant="outline" size="sm" onClick={() => setPreset('all')}>All time</Button>
          </div>

          <div className="grid gap-3 md:grid-cols-1 lg:grid-cols-3">
            {REPORTS.map((report) => (
              <Card key={report.key} className="border shadow-sm">
                <CardHeader className="pb-3">
                  <CardTitle className="text-sm font-medium flex items-center gap-2">
                    <FileText className="h-4 w-4 text-primary" />
                    {report.title}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-xs text-muted-foreground mb-3">{report.description}</p>
                  <Button
                    size="sm"
                    onClick={() => downloadReport(report)}
                    disabled={busy !== null}
                    className="w-full"
                  >
                    {busy === report.key ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                        Preparing...
                      </>
                    ) : (
                      <>
                        <Download className="h-4 w-4 mr-1" />
                        Download PDF
                      </>
                    )}
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
