import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';

interface Loan {
  id: number;
  loanNumber: string;
  amount: number;
  outstandingBalance: number;
  status: string;
  interestRate: number;
  termMonths: number;
  monthlyRepayment: number;
  disbursementDate: string;
}

export default function MemberLoanBalances() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    fetchLoans();
  }, []);

  const fetchLoans = async () => {
    try {
      const response = await api.get('/member/loans');
      setLoans(response.data || []);
    } catch (err) {
      console.error('Error fetching loans:', err);
      toast({ title: 'Error', description: 'Failed to load loans', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DISBURSED':
        return 'bg-blue-100 text-blue-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        <Card>
          <CardContent className="pt-6 text-center">
            <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-muted-foreground">Loading loans...</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" />
        Back
      </Button>

      <div className="grid gap-4">
        {loans.length > 0 ? (
          loans.map((loan) => (
            <Card key={loan.id} className="border-l-4 border-l-primary">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="text-lg">Loan #{loan.loanNumber}</CardTitle>
                    <p className="text-sm text-muted-foreground mt-1">
                      {loan.disbursementDate ? `Disbursed: ${formatDate(loan.disbursementDate)}` : 'Status: Pending Disbursement'}
                    </p>
                  </div>
                  <Badge className={getStatusColor(loan.status)}>
                    {loan.status}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <p className="text-xs text-muted-foreground uppercase">Original Amount</p>
                    <p className="text-lg font-semibold">{formatCurrency(loan.amount)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground uppercase">Outstanding</p>
                    <p className="text-lg font-semibold text-red-600">{formatCurrency(loan.outstandingBalance)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground uppercase">Monthly Payment</p>
                    <p className="text-lg font-semibold">{formatCurrency(loan.monthlyRepayment)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground uppercase">Interest Rate</p>
                    <p className="text-lg font-semibold">{loan.interestRate}%</p>
                  </div>
                </div>

                <div className="pt-4 border-t">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-muted-foreground">Term: {loan.termMonths} months</p>
                    </div>
                    {loan.status === 'DISBURSED' && (
                      <Button 
                        size="sm"
                        onClick={() => navigate('/member/dashboard?tab=transact')}
                      >
                        Make Payment
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        ) : (
          <Card>
            <CardContent className="pt-6 text-center">
              <p className="text-muted-foreground">No active loans</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
