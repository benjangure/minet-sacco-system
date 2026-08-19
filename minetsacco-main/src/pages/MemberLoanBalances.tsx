import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';
import MemberLayout from '@/components/MemberLayout';

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

interface LoanRepayment {
  id: number;
  amount: number;
  paymentMethod: string;
  referenceNumber: string;
  paymentDate: string;
  loan: {
    id: number;
    loanNumber: string;
  };
}

export default function MemberLoanBalances() {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [repayments, setRepayments] = useState<LoanRepayment[]>([]);
  const [loading, setLoading] = useState(true);
  const [memberFirstName, setMemberFirstName] = useState<string>('Member');
  const navigate = useNavigate();
  const { toast } = useToast();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('session');
    navigate('/member/login');
  };

  useEffect(() => {
    fetchMemberInfo();
    fetchLoans();
    fetchRepayments();
  }, []);

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

  const fetchRepayments = async () => {
    try {
      const response = await api.get('/member/loan-repayments');
      setRepayments(response.data || []);
    } catch (err) {
      console.error('Error fetching repayments:', err);
      toast({ title: 'Error', description: 'Failed to load repayments', variant: 'destructive' });
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
      <MemberLayout memberName={memberFirstName} onLogout={handleLogout}>
        <div className="space-y-4 max-w-7xl mx-auto px-4 lg:px-0">
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
      </MemberLayout>
    );
  }

  return (
    <MemberLayout memberName={memberFirstName} onLogout={handleLogout}>
      <div className="space-y-4 max-w-7xl mx-auto px-4 lg:px-0">
      <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" />
        Back
      </Button>

      <div className="space-y-6">
        {/* Loan Repayments Section */}
        <Card>
          <CardHeader>
            <CardTitle>Loan Repayments</CardTitle>
          </CardHeader>
          <CardContent>
            {repayments.length > 0 ? (
              <div className="space-y-3">
                {repayments.map((repayment) => (
                  <div key={repayment.id} className="border rounded-lg p-4 space-y-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium">Loan #{repayment.loan.loanNumber}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatDate(repayment.paymentDate)}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold text-green-600">{formatCurrency(repayment.amount)}</p>
                        <p className="text-xs text-muted-foreground">{repayment.paymentMethod}</p>
                      </div>
                    </div>
                    {repayment.referenceNumber && (
                      <p className="text-xs text-muted-foreground">Ref: {repayment.referenceNumber}</p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground text-center py-4">No loan repayments found</p>
            )}
          </CardContent>
        </Card>

        {/* Active Loans Section */}
        {loans.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Active Loans</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {loans.map((loan) => (
                <div key={loan.id} className="border rounded-lg p-4 space-y-4">
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
                </div>
              ))}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
    </MemberLayout>
  );
}
