import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Wallet, TrendingUp, DollarSign, Plus, HandshakeIcon, FileText, Send, Upload, Eye } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import MemberLayout from '@/components/MemberLayout';
import GuarantorApprovalDialog from '@/components/GuarantorApprovalDialog';
import GuarantorRejectionOptionsDialog from '@/components/GuarantorRejectionOptionsDialog';
import { GuarantorReassignmentDialog } from '@/components/GuarantorReassignmentDialog';
import LoanRepaymentForm from '@/components/LoanRepaymentForm';
import DepositRequestForm from '@/components/DepositRequestForm';
import MpesaTransaction from '@/components/MpesaTransaction';
import MemberNotificationsView from '@/components/MemberNotificationsView';
import MemberReportsView from '@/components/MemberReportsView';
import { API_BASE_URL } from '@/config/api';
import { downloadAndOpenFile } from '@/utils/downloadHelper';
import LoanStatusTimeline from '@/components/LoanStatusTimeline';

interface Dashboard {
  memberNumber: string;
  firstName: string;
  lastName: string;
  savingsBalance: number;
  sharesBalance: number;
  totalBalance: number;
  activeLoans: number;
  totalOutstanding: number;
  pendingApplications: number;
  recentTransactions: Transaction[];
}
interface Transaction {
  id: number;
  transactionType: string;
  amount: number;
  description: string;
  transactionDate: string;
  accountType: string;
}

interface LoanRepayment {
  id: number;
  amount: number;
  paymentMethod: string;
  referenceNumber: string;
  paymentDate: string;
  recordedBy: {
    username: string;
  };
}

interface LoanWithRepayments {
  id: number;
  loanNumber: string;
  amount: number;
  totalRepayable: number;
  outstandingBalance: number;
  monthlyRepayment: number;
  status: string;
  repayments: LoanRepayment[];
  applicationDate?: string;
  approvalDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
}

interface Guarantor {
  id: number;
  member: {
    id: number;
    firstName: string;
    lastName: string;
    memberNumber?: string;
  };
  guaranteeAmount: number;
  previousGuaranteeAmount?: number;
  status: string;
  rejectionReason?: string;
}

interface DepositRequest {
  id: number;
  claimedAmount: number;
  confirmedAmount?: number;
  description: string;
  receiptFileName: string;
  status: string;
  createdAt: string;
}

function MemberDepositsView() {
  const [deposits, setDeposits] = useState<DepositRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    fetchDeposits();
  }, []);

  const fetchDeposits = async () => {
    try {
      const response = await api.get('/member/deposit-requests');
      setDeposits(response.data || []);
    } catch (err) {
      console.error('Error fetching deposits:', err);
      toast({ title: 'Error', description: 'Failed to load deposits', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const handleViewReceipt = async (depositId: number) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URL}/member/deposit-requests/${depositId}/receipt/download`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (response.ok) {
        const blob = await response.blob();
        const filename = response.headers.get('content-disposition')?.split('filename="')[1]?.split('"')[0] || `receipt-${depositId}.pdf`;
        
        await downloadAndOpenFile(
          blob,
          filename,
          (message) => toast({ title: 'Success', description: message }),
          (error) => toast({ title: 'Error', description: error, variant: 'destructive' })
        );
      } else {
        toast({ title: 'Error', description: 'Failed to download receipt', variant: 'destructive' });
      }
    } catch (error) {
      console.error('Download error:', error);
      toast({ title: 'Error', description: 'Failed to download receipt', variant: 'destructive' });
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
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED': return 'bg-green-50 text-green-700';
      case 'REJECTED': return 'bg-red-50 text-red-700';
      case 'PENDING': return 'bg-yellow-50 text-yellow-700';
      default: return 'bg-gray-50 text-gray-700';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>My Deposit Requests</CardTitle>
      </CardHeader>
      <CardContent>
        {deposits.length === 0 ? (
          <p className="text-muted-foreground">No deposit requests yet</p>
        ) : (
          <div className="space-y-3">
            {deposits.map((deposit) => (
              <div key={deposit.id} className="border rounded-lg p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">Claimed: {formatCurrency(deposit.claimedAmount)}</p>
                    {deposit.confirmedAmount && (
                      <p className="text-sm text-muted-foreground">Confirmed: {formatCurrency(deposit.confirmedAmount)}</p>
                    )}
                  </div>
                  <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(deposit.status)}`}>
                    {deposit.status}
                  </span>
                </div>
                <p className="text-sm text-muted-foreground">{deposit.description}</p>
                <div className="flex items-center justify-between pt-2">
                  <p className="text-xs text-muted-foreground">{formatDate(deposit.createdAt)}</p>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleViewReceipt(deposit.id)}
                  >
                    <Eye className="h-4 w-4 mr-1" />
                    View Receipt
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default function MemberDashboard() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('home');
  const [guarantorDialogOpen, setGuarantorDialogOpen] = useState(false);
  const [repaymentFormOpen, setRepaymentFormOpen] = useState(false);
  const [depositRequestOpen, setDepositRequestOpen] = useState(false);
  const [mpesaDepositOpen, setMpesaDepositOpen] = useState(false);
  const [mpesaWithdrawOpen, setMpesaWithdrawOpen] = useState(false);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [eligibility, setEligibility] = useState<any>(null);
  const [eligibilityLoading, setEligibilityLoading] = useState(false);
  const [activeLoans, setActiveLoans] = useState<LoanWithRepayments[]>([]);
  const [loansLoading, setLoansLoading] = useState(false);
  const [expandedLoans, setExpandedLoans] = useState<Set<number>>(new Set());
  const [rejectionDialogOpen, setRejectionDialogOpen] = useState(false);
  const [rejectedLoan, setRejectedLoan] = useState<LoanWithRepayments | null>(null);
  const [rejectedGuarantor, setRejectedGuarantor] = useState<Guarantor | null>(null);
  const [remainingGuarantors, setRemainingGuarantors] = useState<Guarantor[]>([]);
  const [reassignmentDialogOpen, setReassignmentDialogOpen] = useState(false);
  const [reassignmentLoan, setReassignmentLoan] = useState<LoanWithRepayments | null>(null);
  const [reassignmentGuarantors, setReassignmentGuarantors] = useState<Guarantor[]>([]);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { toast } = useToast();

  useEffect(() => {
    // Check if tab is specified in URL
    const tabParam = searchParams.get('tab');
    if (tabParam) {
      setActiveTab(tabParam);
    } else {
      // Default to home tab
      setActiveTab('home');
    }
  }, [searchParams]);

  useEffect(() => {
    fetchDashboard();
    fetchUnreadNotifications();
    fetchEligibility();
    fetchActiveLoans();
  }, []);

  const fetchDashboard = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/member');
        return;
      }

      const response = await api.get('/member/dashboard');

      setDashboard(response.data);
      setError('');
    } catch (err: any) {
      console.error('Error fetching dashboard:', err);
      setError('Failed to load dashboard');
      if (err.response?.status === 401) {
        localStorage.removeItem('token');
        navigate('/member');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userRole');
    localStorage.removeItem('memberId');
    localStorage.removeItem('username');
    navigate('/member');
  };

  const toggleLoanExpansion = (loanId: number) => {
    setExpandedLoans(prev => {
      const newSet = new Set(prev);
      if (newSet.has(loanId)) {
        newSet.delete(loanId);
      } else {
        newSet.add(loanId);
      }
      return newSet;
    });
  };

  const fetchGuarantorDataForRejection = async (loan: LoanWithRepayments) => {
    try {
      console.log('Fetching guarantor data for loan:', loan.id);
      const response = await api.get(`/loans/${loan.id}/guarantors`);
      console.log('Guarantor response:', response.data);
      
      // The backend returns ApiResponse wrapper, so we need to access response.data.data
      const guarantorsData = response.data?.data || [];
      console.log('Guarantors data:', guarantorsData);
      
      // Map the DTO to our Guarantor interface
      const guarantors = guarantorsData.map((g: any) => ({
        id: g.guarantorId,
        member: {
          id: g.memberId,
          firstName: g.firstName,
          lastName: g.lastName,
        },
        guaranteeAmount: g.guaranteeAmount,
        status: g.status,
        rejectionReason: g.rejectionReason,
      }));
      
      console.log('Mapped guarantors:', guarantors);
      
      // Find the rejected guarantor (status = REJECTED)
      const rejected = guarantors.find((g: Guarantor) => g.status === 'REJECTED');
      console.log('Rejected guarantor:', rejected);
      
      // Get remaining guarantors (status = ACCEPTED or ACTIVE)
      const remaining = guarantors.filter((g: Guarantor) => 
        g.status === 'ACCEPTED' || g.status === 'ACTIVE'
      );
      console.log('Remaining guarantors:', remaining);
      
      if (rejected) {
        console.log('Setting rejection dialog state');
        setRejectedLoan(loan);
        setRejectedGuarantor(rejected);
        setRemainingGuarantors(remaining);
        setRejectionDialogOpen(true);
        console.log('Dialog should now be open');
      } else {
        console.warn('No rejected guarantor found');
      }
    } catch (error) {
      console.error('Error fetching guarantor data:', error);
    }
  };

  const fetchGuarantorDataForReassignment = async (loan: LoanWithRepayments) => {
    try {
      console.log('Fetching guarantor data for reassignment:', loan.id);
      const response = await api.get(`/loans/${loan.id}/guarantors`);
      console.log('Guarantor response:', response.data);
      
      // The backend returns ApiResponse wrapper, so we need to access response.data.data
      const guarantorsData = response.data?.data || [];
      console.log('Guarantors data:', guarantorsData);
      
      // Map the DTO to our Guarantor interface
      const guarantors = guarantorsData.map((g: any) => ({
        id: g.guarantorId,
        member: {
          id: g.memberId,
          firstName: g.firstName,
          lastName: g.lastName,
          memberNumber: g.memberNumber,
        },
        guaranteeAmount: g.guaranteeAmount,
        previousGuaranteeAmount: g.previousGuaranteeAmount,
        status: g.status,
      }));
      
      console.log('Mapped guarantors for reassignment:', guarantors);
      
      // Get guarantors that need reassignment (status = PENDING_REASSIGNMENT)
      const needsReassignment = guarantors.filter((g: Guarantor) => 
        g.status === 'PENDING_REASSIGNMENT'
      );
      console.log('Guarantors needing reassignment:', needsReassignment);
      
      if (needsReassignment.length > 0) {
        console.log('Setting reassignment dialog state');
        setReassignmentLoan(loan);
        setReassignmentGuarantors(needsReassignment);
        setReassignmentDialogOpen(true);
        console.log('Reassignment dialog should now be open');
      } else {
        console.warn('No guarantors needing reassignment found');
      }
    } catch (error) {
      console.error('Error fetching guarantor data for reassignment:', error);
    }
  };

  const fetchUnreadNotifications = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) return;

      const response = await api.get('/member/notifications/unread-count');
      
      setUnreadNotifications(response.data?.data || 0);
    } catch (error) {
      console.error('Error fetching unread notifications:', error);
    }
  };

  const fetchEligibility = async () => {
    setEligibilityLoading(true);
    try {
      const response = await api.get('/member/eligibility');
      if (response.data && response.data.data) {
        setEligibility(response.data.data);
      }
    } catch (error) {
      console.error('Error fetching eligibility:', error);
      setEligibility(null);
    } finally {
      setEligibilityLoading(false);
    }
  };

  const fetchActiveLoans = async () => {
    setLoansLoading(true);
    try {
      const response = await api.get('/member/loans');
      
      if (response.data && Array.isArray(response.data)) {
        const allLoans = response.data || [];
        
        // Debug: Log all loan data to identify the issue
        console.log('All loans from backend:', allLoans.map(l => ({
          id: l.id,
          loanNumber: l.loanNumber,
          status: l.status,
          outstandingBalance: l.outstandingBalance,
          amount: l.amount,
          totalRepayable: l.totalRepayable
        })));
        
        // Exclude fully repaid loans and only show active/pending loans
        const loans = allLoans.filter((l: any) => 
          ['DISBURSED', 'ACTIVE', 'PENDING', 'PENDING_GUARANTOR_APPROVAL', 'PENDING_LOAN_OFFICER_REVIEW', 'PENDING_CREDIT_COMMITTEE', 'PENDING_TREASURER', 'APPROVED', 'PENDING_GUARANTOR_REPLACEMENT', 'PENDING_GUARANTOR_REASSIGNMENT'].includes(l.status) &&
          l.outstandingBalance > 0 // Only show loans with outstanding balance
        );
        
        console.log('Filtered loans (should exclude fully repaid):', loans.map(l => ({
          id: l.id,
          loanNumber: l.loanNumber,
          status: l.status,
          outstandingBalance: l.outstandingBalance
        })));
        
        // Fetch repayment history for each loan
        const loansWithRepayments = await Promise.all(
          loans.map(async (loan: any) => {
            try {
              const repaymentRes = await api.get(`/member/loans/${loan.id}/repayments`);
              return {
                ...loan,
                repayments: Array.isArray(repaymentRes.data) ? repaymentRes.data : (repaymentRes.data?.data || [])
              };
            } catch (err) {
              console.error(`Error fetching repayments for loan ${loan.id}:`, err);
              return {
                ...loan,
                repayments: []
              };
            }
          })
        );
        
        setActiveLoans(loansWithRepayments);
        
        // Don't automatically open dialogs - let user click action buttons instead
        // Dialogs will open only when user clicks "Take Action" or "Reassign Guarantors" buttons
      }
    } catch (error) {
      console.error('Error fetching active loans:', error);
      setActiveLoans([]);
    } finally {
      setLoansLoading(false);
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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-muted-foreground">Loading your dashboard...</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-red-600 font-medium mb-4">{error}</p>
              <Button onClick={fetchDashboard} className="w-full">
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground">No dashboard data available</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <MemberLayout memberName={dashboard?.firstName || 'Member'} onLogout={handleLogout} unreadNotifications={unreadNotifications}>
      <div className="space-y-4 max-w-7xl mx-auto">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-foreground">Welcome, {dashboard?.firstName}!</h1>
          <p className="text-muted-foreground">Member #{dashboard?.memberNumber}</p>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="hidden lg:grid w-full grid-cols-7 bg-primary/10 h-auto">
            <TabsTrigger value="home" data-tab="home" className="text-xs lg:text-sm py-2">Home</TabsTrigger>
            <TabsTrigger value="transact" data-tab="transact" className="text-xs lg:text-sm py-2">Transact</TabsTrigger>
            <TabsTrigger value="account" data-tab="account" className="text-xs lg:text-sm py-2">My Account</TabsTrigger>
            <TabsTrigger value="loans" data-tab="loans" className="text-xs lg:text-sm py-2">Loans</TabsTrigger>
            <TabsTrigger value="deposits" data-tab="deposits" className="text-xs lg:text-sm py-2">Deposits</TabsTrigger>
            <TabsTrigger value="reports" data-tab="reports" className="text-xs lg:text-sm py-2">Reports</TabsTrigger>
            <TabsTrigger value="notifications" data-tab="notifications" className="text-xs lg:text-sm py-2">Notifications</TabsTrigger>
          </TabsList>

          {/* HOME TAB */}
          <TabsContent value="home" className="space-y-4 md:space-y-6">
            {/* Action Required Alert */}
            {activeLoans.some(l => l.status === 'PENDING_GUARANTOR_REPLACEMENT' || l.status === 'PENDING_GUARANTOR_REASSIGNMENT') && (
              <Card className="border-red-200 bg-red-50">
                <CardContent className="pt-6">
                  <div className="flex items-start gap-4">
                    <div className="flex-1">
                      <h3 className="font-semibold text-red-900 mb-2">Action Required on Your Loan</h3>
                      <p className="text-sm text-red-800 mb-3">
                        {activeLoans.filter(l => l.status === 'PENDING_GUARANTOR_REPLACEMENT').length > 0 && 
                          'One of your guarantors has rejected your loan application. '}
                        {activeLoans.filter(l => l.status === 'PENDING_GUARANTOR_REASSIGNMENT').length > 0 && 
                          'You need to reassign guarantors for your reduced loan amount. '}
                        Please visit the Loans page to take action.
                      </p>
                      <Button 
                        size="sm"
                        onClick={() => setActiveTab('loans')}
                        className="bg-red-600 hover:bg-red-700"
                      >
                        Go to Loans
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            <div className="grid gap-3 md:gap-4 md:grid-cols-2 lg:grid-cols-3">
              <Card className="border-none shadow-sm">
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Savings Balance</CardTitle>
                  <Wallet className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-lg md:text-2xl font-bold text-foreground">{formatCurrency(dashboard?.savingsBalance || 0)}</div>
                </CardContent>
              </Card>

              <Card className="border-none shadow-sm">
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Shares Balance</CardTitle>
                  <TrendingUp className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-lg md:text-2xl font-bold text-foreground">{formatCurrency(dashboard?.sharesBalance || 0)}</div>
                </CardContent>
              </Card>

              <Card className="border-none shadow-sm">
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Total Balance</CardTitle>
                  <DollarSign className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-lg md:text-2xl font-bold text-foreground">{formatCurrency(dashboard?.totalBalance || 0)}</div>
                </CardContent>
              </Card>
            </div>

            {/* Quick Actions */}
            <div>
              <h2 className="text-base md:text-lg font-semibold text-foreground mb-3 md:mb-4">Quick Actions</h2>
              <div className="grid gap-2 md:gap-3 md:grid-cols-2 lg:grid-cols-3">
                <Card 
                  className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                  onClick={() => setMpesaDepositOpen(true)}
                >
                  <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                    <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Deposit via M-Pesa</CardTitle>
                    <DollarSign className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-xs md:text-sm">Add funds to your account</p>
                  </CardContent>
                </Card>

                <Card 
                  className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                  onClick={() => setMpesaWithdrawOpen(true)}
                >
                  <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                    <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Withdraw via M-Pesa</CardTitle>
                    <Send className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-xs md:text-sm">Withdraw from savings account</p>
                  </CardContent>
                </Card>

                <Card 
                  className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                  onClick={() => setDepositRequestOpen(true)}
                >
                  <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                    <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Submit Deposit</CardTitle>
                    <Upload className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-xs md:text-sm">Upload receipt for verification</p>
                  </CardContent>
                </Card>
              </div>
            </div>

            {/* Quick link to Apply for Loan */}
            <div className="flex justify-center">
              <Button 
                size="lg"
                onClick={() => navigate('/member/apply-loan')}
                className="gap-2"
              >
                <span>Apply for Loan</span>
              </Button>
            </div>

            {dashboard?.recentTransactions && dashboard.recentTransactions.length > 0 && (
              <div>
                <h2 className="text-base md:text-lg font-semibold text-foreground mb-3 md:mb-4">Recent Transactions</h2>
                <Card className="border-none shadow-sm">
                  <CardContent className="pt-4 md:pt-6">
                    <div className="space-y-2 md:space-y-3">
                      {dashboard.recentTransactions.slice(0, 5).map((transaction) => (
                        <div key={transaction.id} className="flex items-center justify-between p-2 md:p-3 bg-muted/50 rounded-lg text-xs md:text-sm">
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-foreground truncate">{transaction.transactionType}</p>
                            <p className="text-muted-foreground text-xs">{formatDate(transaction.transactionDate)}</p>
                          </div>
                          <p className={`font-semibold ml-2 whitespace-nowrap text-xs md:text-sm ${transaction.transactionType === 'WITHDRAWAL' ? 'text-red-600' : 'text-green-600'}`}>
                            {transaction.transactionType === 'WITHDRAWAL' ? '-' : '+'}
                            {formatCurrency(transaction.amount)}
                          </p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}
          </TabsContent>

          {/* TRANSACT TAB */}
          <TabsContent value="transact" className="space-y-3 md:space-y-4">
            <div className="grid gap-2 md:gap-3 md:grid-cols-2 lg:grid-cols-3">
              <Button
                variant="outline"
                className="h-auto py-3 md:py-4 lg:py-6 flex flex-col items-center gap-2 text-xs md:text-sm"
                onClick={() => setDepositRequestOpen(true)}
              >
                <Upload className="h-4 w-4 md:h-5 md:w-5 lg:h-6 lg:w-6 text-primary" />
                <span className="font-medium">Submit Deposit</span>
              </Button>

              <Button
                variant="outline"
                className="h-auto py-3 md:py-4 lg:py-6 flex flex-col items-center gap-2 text-xs md:text-sm"
                onClick={() => setRepaymentFormOpen(true)}
              >
                <DollarSign className="h-4 w-4 md:h-5 md:w-5 lg:h-6 lg:w-6 text-primary" />
                <span className="font-medium">Repay Loan</span>
              </Button>

              <Button
                variant="outline"
                className="h-auto py-3 md:py-4 lg:py-6 flex flex-col items-center gap-2 text-xs md:text-sm"
                onClick={() => setGuarantorDialogOpen(true)}
              >
                <HandshakeIcon className="h-4 w-4 md:h-5 md:w-5 lg:h-6 lg:w-6 text-primary" />
                <span className="font-medium">Guarantor Requests</span>
              </Button>
            </div>

            <Card>
              <CardHeader>
                <CardTitle className="text-base md:text-lg lg:text-xl">Recent Transactions</CardTitle>
              </CardHeader>
              <CardContent>
                {dashboard?.recentTransactions && dashboard.recentTransactions.length > 0 ? (
                  <div className="space-y-2 md:space-y-3">
                    {dashboard.recentTransactions.map((transaction) => {
                      const isLoanRepayment = transaction.transactionType === 'LOAN_REPAYMENT' || 
                                              transaction.description?.toLowerCase().includes('repayment') ||
                                              transaction.description?.toLowerCase().includes('loan');
                      
                      return (
                        <div 
                          key={transaction.id} 
                          className={`flex items-center justify-between p-2 md:p-4 border rounded-lg text-xs md:text-sm ${
                            isLoanRepayment ? 'bg-blue-50 border-blue-200' : ''
                          }`}
                        >
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <p className="font-medium truncate">{transaction.transactionType}</p>
                              {isLoanRepayment && (
                                <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full font-medium">
                                  Loan Repayment
                                </span>
                              )}
                            </div>
                            <p className="text-muted-foreground text-xs">{formatDate(transaction.transactionDate)}</p>
                            {transaction.description && (
                              <p className="text-muted-foreground text-xs truncate mt-1">{transaction.description}</p>
                            )}
                          </div>
                          <div className="text-right ml-2">
                            <p className={`font-semibold whitespace-nowrap text-xs md:text-sm ${
                              transaction.transactionType === 'WITHDRAWAL' ? 'text-red-600' : 'text-green-600'
                            }`}>
                              {transaction.transactionType === 'WITHDRAWAL' ? '-' : '+'}
                              {formatCurrency(transaction.amount)}
                            </p>
                            {transaction.accountType && (
                              <p className="text-muted-foreground text-xs mt-1">{transaction.accountType}</p>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-muted-foreground text-sm">No transactions yet</p>
                )}
                
                {/* Show loan repayment summary if active loans exist */}
                {activeLoans.length > 0 && (
                  <div className="mt-4 pt-4 border-t">
                    <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-green-800 text-sm">Recent Loan Repayments</p>
                          <p className="text-green-700 text-xs mt-1">
                            {activeLoans.reduce((count, loan) => count + (loan.repayments?.length || 0), 0)} repayments across {activeLoans.length} loan(s)
                          </p>
                        </div>
                        <Button 
                          size="sm" 
                          variant="outline"
                          onClick={() => navigate('/member/loan-balances')}
                          className="text-green-700 border-green-300 hover:bg-green-100"
                        >
                          View All
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* MY ACCOUNT TAB */}
          <TabsContent value="account" className="space-y-3 md:space-y-4">
            <div className="grid gap-2 md:gap-3 md:grid-cols-2">
              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => toast({ title: 'Account Balances', description: 'Savings: ' + formatCurrency(dashboard?.savingsBalance || 0) + ' | Shares: ' + formatCurrency(dashboard?.sharesBalance || 0) })}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Account Balances</CardTitle>
                  <Wallet className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">View all your account balances</p>
                </CardContent>
              </Card>

              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => navigate('/member/account-statement')}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Account Statement</CardTitle>
                  <FileText className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">Download your account statement</p>
                </CardContent>
              </Card>

              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => setActiveTab('deposits')}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">My Deposits</CardTitle>
                  <Upload className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">View your submitted deposits</p>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* LOANS TAB */}
          <TabsContent value="loans" className="space-y-3 md:space-y-4">
            <div className="grid gap-2 md:gap-3 md:grid-cols-2">
              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => navigate('/member/apply-loan')}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Apply Loan</CardTitle>
                  <Plus className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">Submit a new loan application</p>
                </CardContent>
              </Card>

              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => setRepaymentFormOpen(true)}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Pay Loan</CardTitle>
                  <DollarSign className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">Make a loan repayment</p>
                </CardContent>
              </Card>

              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => navigate('/member/loan-balances')}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Loan Balances</CardTitle>
                  <TrendingUp className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">View your active loans</p>
                </CardContent>
              </Card>

              <Card 
                className="border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => navigate('/member/account-statement')}
              >
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Loan Statement</CardTitle>
                  <FileText className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-xs md:text-sm">Download loan statement</p>
                </CardContent>
              </Card>
            </div>

            {/* Loans Requiring Action */}
            {loansLoading ? (
              <Card>
                <CardContent className="pt-6 text-center">
                  <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
                  <p className="text-muted-foreground">Loading your loans...</p>
                </CardContent>
              </Card>
            ) : activeLoans.filter(l => l.status === 'PENDING_GUARANTOR_REPLACEMENT' || l.status === 'PENDING_GUARANTOR_REASSIGNMENT').length > 0 && (
              <div className="space-y-3">
                <h3 className="text-lg font-semibold text-foreground">⚠️ Action Required</h3>
                {activeLoans
                  .filter(l => l.status === 'PENDING_GUARANTOR_REPLACEMENT' || l.status === 'PENDING_GUARANTOR_REASSIGNMENT')
                  .map((loan) => (
                    <Card key={loan.id} className="border-red-200 bg-red-50">
                      <CardHeader 
                        className="cursor-pointer hover:bg-red-100 transition-colors"
                        onClick={() => toggleLoanExpansion(loan.id)}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <CardTitle className="text-base">Loan #{loan.loanNumber}</CardTitle>
                            <p className="text-sm text-muted-foreground">Amount: {formatCurrency(loan.amount)}</p>
                            {loan.status === 'PENDING_GUARANTOR_REPLACEMENT' && (
                              <p className="text-sm text-red-700 font-medium mt-1">Guarantor Rejected - Choose an action</p>
                            )}
                            {loan.status === 'PENDING_GUARANTOR_REASSIGNMENT' && (
                              <p className="text-sm text-red-700 font-medium mt-1">Reassign Guarantors with New Amounts</p>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            {loan.status === 'PENDING_GUARANTOR_REPLACEMENT' && (
                              <Button
                                size="sm"
                                variant="destructive"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  fetchGuarantorDataForRejection(loan);
                                }}
                              >
                                Take Action
                              </Button>
                            )}
                            {loan.status === 'PENDING_GUARANTOR_REASSIGNMENT' && (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  fetchGuarantorDataForReassignment(loan);
                                }}
                              >
                                Reassign Guarantors
                              </Button>
                            )}
                            <div className={`transform transition-transform duration-200 ${
                              expandedLoans.has(loan.id) ? 'rotate-180' : ''
                            }`}>
                              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                              </svg>
                            </div>
                          </div>
                        </div>
                      </CardHeader>
                      
                      {expandedLoans.has(loan.id) && (
                        <CardContent className="space-y-4 border-t">
                          {/* Loan Summary */}
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <div>
                              <p className="text-xs text-muted-foreground">Loan Amount</p>
                              <p className="font-semibold">{formatCurrency(loan.amount)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Outstanding</p>
                              <p className="font-semibold text-red-600">{formatCurrency(loan.outstandingBalance)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Monthly Payment</p>
                              <p className="font-semibold">{formatCurrency(loan.monthlyRepayment)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Repayments Made</p>
                              <p className="font-semibold">{loan.repayments?.length || 0}</p>
                            </div>
                          </div>

                          {/* Loan Status Timeline */}
                          <div className="border-t pt-4">
                            <LoanStatusTimeline
                              currentStatus={loan.status}
                              applicationDate={loan.applicationDate}
                              approvalDate={loan.approvalDate}
                              disbursementDate={loan.disbursementDate}
                              rejectionReason={loan.rejectionReason}
                            />
                          </div>
                        </CardContent>
                      )}
                    </Card>
                  ))}
              </div>
            )}

            {/* All Active Loans */}

            {/* All Active Loans */}
            {loansLoading ? (
              <Card>
                <CardContent className="pt-6 text-center">
                  <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
                  <p className="text-muted-foreground">Loading your loans...</p>
                </CardContent>
              </Card>
            ) : activeLoans.filter(l => l.status !== 'PENDING_GUARANTOR_REPLACEMENT' && l.status !== 'PENDING_GUARANTOR_REASSIGNMENT').length === 0 && activeLoans.filter(l => l.status === 'PENDING_GUARANTOR_REPLACEMENT' || l.status === 'PENDING_GUARANTOR_REASSIGNMENT').length === 0 ? (
              <Card>
                <CardContent className="pt-6 text-center">
                  <p className="text-muted-foreground">No active loans</p>
                </CardContent>
              </Card>
            ) : activeLoans.filter(l => l.status !== 'PENDING_GUARANTOR_REPLACEMENT' && l.status !== 'PENDING_GUARANTOR_REASSIGNMENT').length > 0 && (
              <div className="space-y-3">
                <h3 className="text-lg font-semibold text-foreground">Your Active Loans</h3>
                {activeLoans
                  .filter(l => l.status !== 'PENDING_GUARANTOR_REPLACEMENT' && l.status !== 'PENDING_GUARANTOR_REASSIGNMENT')
                  .map((loan) => (
                    <Card key={loan.id} className="border-blue-200">
                      <CardHeader 
                        className="cursor-pointer hover:bg-gray-50 transition-colors"
                        onClick={() => toggleLoanExpansion(loan.id)}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <CardTitle className="text-base">Loan #{loan.loanNumber}</CardTitle>
                            <p className="text-sm text-muted-foreground">Amount: {formatCurrency(loan.amount)}</p>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                              loan.status === 'APPROVED' || loan.status === 'DISBURSED' || loan.status === 'ACTIVE' 
                                ? 'bg-green-50 text-green-700' 
                                : loan.status === 'REJECTED'
                                ? 'bg-red-50 text-red-700'
                                : 'bg-yellow-50 text-yellow-700'
                            }`}>
                              {loan.status === 'DISBURSED' ? 'Disbursed' :
                              loan.status === 'ACTIVE' ? 'Active' :
                              loan.status === 'PENDING' ? 'Pending' :
                              loan.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Pending Guarantor Approval' :
                              loan.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Pending Loan Officer Review' :
                              loan.status === 'PENDING_CREDIT_COMMITTEE' ? 'Pending Credit Committee' :
                              loan.status === 'PENDING_TREASURER' ? 'Pending Treasurer' :
                              loan.status === 'APPROVED' ? 'Approved' :
                              loan.status}
                            </span>
                            <div className={`transform transition-transform duration-200 ${
                              expandedLoans.has(loan.id) ? 'rotate-180' : ''
                            }`}>
                              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                              </svg>
                            </div>
                          </div>
                        </div>
                      </CardHeader>
                      
                      {expandedLoans.has(loan.id) && (
                        <CardContent className="space-y-4 border-t">
                          {/* Loan Summary */}
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <div>
                              <p className="text-xs text-muted-foreground">Loan Amount</p>
                              <p className="font-semibold">{formatCurrency(loan.amount)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Outstanding</p>
                              <p className="font-semibold text-red-600">{formatCurrency(loan.outstandingBalance)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Monthly Payment</p>
                              <p className="font-semibold">{formatCurrency(loan.monthlyRepayment)}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Repayments Made</p>
                              <p className="font-semibold">{loan.repayments?.length || 0}</p>
                            </div>
                          </div>

                          {/* Repayment Progress */}
                          <div className="border-t pt-4">
                            <p className="text-sm font-semibold mb-3">Repayment Progress</p>
                            <div className="space-y-3">
                              <div>
                                <div className="flex justify-between text-sm mb-1">
                                  <span className="text-muted-foreground">Progress</span>
                                  <span className="font-medium">
                                    {formatCurrency(loan.totalRepayable - loan.outstandingBalance)} / {formatCurrency(loan.totalRepayable)}
                                  </span>
                                </div>
                                <div className="w-full bg-gray-200 rounded-full h-2">
                                  <div 
                                    className="bg-green-600 h-2 rounded-full transition-all duration-300"
                                    style={{ width: `${Math.min(((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100, 100)}%` }}
                                  />
                                </div>
                                <div className="flex justify-between text-xs text-muted-foreground mt-1">
                                  <span>{Math.round(((loan.totalRepayable - loan.outstandingBalance) / loan.totalRepayable) * 100)}% repaid</span>
                                  <span>{formatCurrency(loan.outstandingBalance)} remaining</span>
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* Repayment History */}
                          {loan.repayments && loan.repayments.length > 0 ? (
                            <div className="border-t pt-4">
                              <p className="text-sm font-semibold mb-3">Repayment History</p>
                              <div className="space-y-2 max-h-48 overflow-y-auto">
                                {loan.repayments.map((repayment) => (
                                  <div key={repayment.id} className="flex items-center justify-between p-2 bg-muted/50 rounded text-sm">
                                    <div>
                                      <p className="font-medium">{formatCurrency(repayment.amount)}</p>
                                      <p className="text-xs text-muted-foreground">
                                        {new Date(repayment.paymentDate).toLocaleDateString('en-KE')} {repayment.paymentMethod}
                                      </p>
                                    </div>
                                    <p className="text-xs text-muted-foreground">By: {repayment.recordedBy?.username}</p>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ) : (
                            <div className="border-t pt-4">
                              <p className="text-sm text-muted-foreground">No repayments recorded yet</p>
                            </div>
                          )}

                          {/* Loan Status Timeline */}
                          <div className="border-t pt-4">
                            <LoanStatusTimeline
                              currentStatus={loan.status}
                              applicationDate={loan.applicationDate}
                              approvalDate={loan.approvalDate}
                              disbursementDate={loan.disbursementDate}
                              rejectionReason={loan.rejectionReason}
                            />
                          </div>
                        </CardContent>
                      )}
                    </Card>
                  ))}
              </div>
            )}
          </TabsContent>

          {/* DEPOSITS TAB */}
          <TabsContent value="deposits" className="space-y-4">
            <MemberDepositsView />
          </TabsContent>

          {/* REPORTS TAB */}
          <TabsContent value="reports" className="space-y-4">
            <MemberReportsView />
          </TabsContent>

          {/* NOTIFICATIONS TAB */}
          <TabsContent value="notifications" className="space-y-4">
            <MemberNotificationsView />
          </TabsContent>
        </Tabs>

        {/* Guarantor Approval Dialog */}
        <GuarantorApprovalDialog
          open={guarantorDialogOpen}
          onOpenChange={setGuarantorDialogOpen}
          onApprovalChange={fetchDashboard}
        />

        {/* Guarantor Rejection Options Dialog */}
        <GuarantorRejectionOptionsDialog
          open={rejectionDialogOpen}
          onOpenChange={setRejectionDialogOpen}
          loanId={rejectedLoan?.id || 0}
          loanAmount={rejectedLoan?.amount || 0}
          rejectedGuarantor={rejectedGuarantor ? {
            id: rejectedGuarantor.id,
            firstName: rejectedGuarantor.member.firstName,
            lastName: rejectedGuarantor.member.lastName,
            guaranteeAmount: rejectedGuarantor.guaranteeAmount,
          } : null}
          remainingGuarantors={remainingGuarantors.map(g => ({
            id: g.id,
            firstName: g.member.firstName,
            lastName: g.member.lastName,
            guaranteeAmount: g.guaranteeAmount,
          }))}
          onSuccess={() => {
            setRejectionDialogOpen(false);
            setRejectedLoan(null);
            setRejectedGuarantor(null);
            setRemainingGuarantors([]);
            fetchActiveLoans();
          }}
        />

        {/* Guarantor Reassignment Dialog */}
        <GuarantorReassignmentDialog
          open={reassignmentDialogOpen}
          onOpenChange={setReassignmentDialogOpen}
          loan={reassignmentLoan}
          guarantors={reassignmentGuarantors as any}
          onReassignmentComplete={() => {
            setReassignmentDialogOpen(false);
            setReassignmentLoan(null);
            setReassignmentGuarantors([]);
            fetchActiveLoans();
          }}
        />

        {/* Loan Repayment Form */}
        <LoanRepaymentForm
          open={repaymentFormOpen}
          onOpenChange={setRepaymentFormOpen}
          onRepaymentSuccess={fetchDashboard}
        />

        {/* Deposit Request Form */}
        <DepositRequestForm
          open={depositRequestOpen}
          onOpenChange={setDepositRequestOpen}
          onSuccess={fetchDashboard}
        />

        {/* M-Pesa Deposit */}
        <MpesaTransaction
          open={mpesaDepositOpen}
          onOpenChange={setMpesaDepositOpen}
          type="deposit"
          onSuccess={fetchDashboard}
        />

        {/* M-Pesa Withdraw */}
        <MpesaTransaction
          open={mpesaWithdrawOpen}
          onOpenChange={setMpesaWithdrawOpen}
          type="withdraw"
          onSuccess={fetchDashboard}
        />
      </div>
    </MemberLayout>
  );
}
