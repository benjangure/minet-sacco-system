import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRefresh } from '@/contexts/RefreshContext';
import api from '@/config/api';
import { formatCurrency } from '@/lib/utils';
import { downloadAndOpenFile } from '@/utils/downloadHelper';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Download, ArrowLeft } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';
import MemberLayout from '@/components/MemberLayout';

interface Transaction {
  id: number;
  transactionType: string;
  amount: number;
  description: string;
  transactionDate: string;
  accountType: string;
}

export default function MemberAccountStatement() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [memberFirstName, setMemberFirstName] = useState<string>('Member');
  const navigate = useNavigate();
  const { toast } = useToast();
  const { refreshKey } = useRefresh();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('session');
    navigate('/member/login');
  };

  useEffect(() => {
    fetchMemberInfo();
    fetchTransactions();
  }, [refreshKey]);

  const fetchMemberInfo = async () => {
    try {
      const response = await api.get('/member/dashboard');
      if (response.data && (response.data.fullName || response.data.firstName)) {
        setMemberFirstName(response.data.fullName || response.data.firstName);
      }
    } catch (err) {
      console.error('Error fetching member info:', err);
    }
  };

  const fetchTransactions = async () => {
    try {
      const response = await api.get('/member/transactions');
      setTransactions(response.data || []);
    } catch (err) {
      console.error('Error fetching transactions:', err);
      toast({ title: 'Error', description: 'Failed to load transactions', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  // Money in (credit) / money out (debit) for a transaction type; other types do not move the balance
  const direction = (type: string): 'in' | 'out' | 'none' => {
    if (type === 'DEPOSIT' || type === 'LOAN_DISBURSEMENT') return 'in';
    if (type === 'WITHDRAWAL' || type === 'LOAN_REPAYMENT') return 'out';
    return 'none';
  };

  const csvCell = (value: unknown): string => {
    const text = value === null || value === undefined ? '' : String(value);
    return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  };

  const downloadStatement = async () => {
    const rows = [
      ['Date', 'Type', 'Description', 'Money in (KES)', 'Money out (KES)', 'Account'],
      ...transactions.map(t => {
        const dir = direction(t.transactionType);
        const amount = Number(t.amount || 0).toFixed(2);
        return [
          new Date(t.transactionDate).toISOString().slice(0, 10),
          t.transactionType,
          t.description || '',
          dir === 'in' ? amount : '',
          dir === 'out' ? amount : '',
          t.accountType,
        ];
      }),
    ];
    // BOM so Excel opens UTF-8 correctly
    const csv = '\uFEFF' + rows.map(r => r.map(csvCell).join(',')).join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    await downloadAndOpenFile(
      blob,
      `account-statement-${new Date().toISOString().split('T')[0]}.csv`,
      (message) => toast({ title: 'Success', description: message }),
      (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
    );
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <MemberLayout memberName={memberFirstName} onLogout={handleLogout}>
        <div className="space-y-4 max-w-7xl mx-auto px-4 lg:px-0">
          <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
            <ArrowLeft className="h-4 w-4" />
            Back
          </Button>
          <Card>
            <CardContent className="pt-6 text-center">
              <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
              <p className="text-muted-foreground">Loading transactions...</p>
            </CardContent>
          </Card>
        </div>
      </MemberLayout>
    );
  }

  return (
    <MemberLayout memberName={memberFirstName} onLogout={handleLogout}>
      <div className="space-y-4 max-w-7xl mx-auto px-4 lg:px-0">
      <div className="flex items-center justify-between">
        <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        <Button onClick={downloadStatement} className="gap-2">
          <Download className="h-4 w-4" />
          Download CSV
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Account Statement</CardTitle>
        </CardHeader>
        <CardContent>
          {transactions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b">
                  <tr>
                    <th className="text-left py-2 px-2">Date</th>
                    <th className="text-left py-2 px-2">Type</th>
                    <th className="text-left py-2 px-2">Description</th>
                    <th className="text-right py-2 px-2">Amount</th>
                    <th className="text-left py-2 px-2">Account</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((transaction) => (
                    <tr key={transaction.id} className="border-b hover:bg-muted/50">
                      <td className="py-2 px-2">{formatDate(transaction.transactionDate)}</td>
                      <td className="py-2 px-2">{transaction.transactionType}</td>
                      <td className="py-2 px-2">{transaction.description}</td>
                      <td className={`py-2 px-2 text-right font-semibold ${
                        direction(transaction.transactionType) === 'out' ? 'text-red-600'
                          : direction(transaction.transactionType) === 'in' ? 'text-green-600'
                          : 'text-muted-foreground'}`}>
                        {direction(transaction.transactionType) === 'out' ? '-'
                          : direction(transaction.transactionType) === 'in' ? '+' : ''}
                        {formatCurrency(transaction.amount)}
                      </td>
                      <td className="py-2 px-2">{transaction.accountType}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-muted-foreground text-center py-8">No transactions found</p>
          )}
        </CardContent>
      </Card>
    </div>
    </MemberLayout>
  );
}
