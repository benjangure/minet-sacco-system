import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useRefresh } from '@/contexts/RefreshContext';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Wallet, TrendingUp, DollarSign, Plus, HandshakeIcon, FileText, Send, Upload, Eye, ArrowUp } from 'lucide-react';
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
import NotificationPrompt from '@/components/NotificationPrompt';
import { API_BASE_URL } from '@/config/api';
import { downloadAndOpenFile } from '@/utils/downloadHelper';
import LoanStatusTimeline from '@/components/LoanStatusTimeline';
import TopUpStatusTimeline from '@/components/TopUpStatusTimeline';
import LoanTopUpRequestDialog from '@/components/LoanTopUpRequestDialog';
import TopUpGuarantorApprovalModal from '@/components/TopUpGuarantorApprovalModal';

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
  principalAmount?: number;
  interestAmount?: number;
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
  principalRepaid?: number;
  repaymentPercentage?: number;
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
  const { refreshKey } = useRefresh();

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
      // Get token from session object first (where AuthContext stores it)
      let token = localStorage.getItem('token');
      if (!token) {
        const sessionStr = localStorage.getItem('session');
        if (sessionStr) {
          try {
            const session = JSON.parse(sessionStr);
            token = session.token;
          } catch (e) {
            console.error('Failed to parse session:', e);
          }
        }
      }

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
  const [selectedGuarantee, setSelectedGuarantee] = useState<any>(null);
  const [topUpGuarantorDialogOpen, setTopUpGuarantorDialogOpen] = useState(false);
  const [selectedTopUpGuarantee, setSelectedTopUpGuarantee] = useState<any>(null);
  const [pendingGuarantees, setPendingGuarantees] = useState<any[]>([]);
  const [repaymentFormOpen, setRepaymentFormOpen] = useState(false);
  const [depositRequestOpen, setDepositRequestOpen] = useState(false);
  // // const [mpesaDepositOpen, setMpesaDepositOpen] = useState(false); // TODO: MPesa deposit not implemented
  // const [mpesaWithdrawOpen, setMpesaWithdrawOpen] = useState(false); // TODO: MPesa withdraw not implemented
  
  // Dummy variables to prevent TypeScript errors for commented MPesa functionality
  const [mpesaDepositOpen] = useState(false);
  const [setMpesaDepositOpen] = useState(() => {});
  const [mpesaWithdrawOpen] = useState(false);
  const [setMpesaWithdrawOpen] = useState(() => {});
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
  const [topUpDialogOpen, setTopUpDialogOpen] = useState(false);
  const [topUpLoan, setTopUpLoan] = useState<LoanWithRepayments | null>(null);
  const [loanTopUpHistory, setLoanTopUpHistory] = useState<Map<number, any[]>>(new Map());
  const [loadingTopUpHistory, setLoadingTopUpHistory] = useState<Set<number>>(new Set());
  const [loanRepayments, setLoanRepayments] = useState<Map<number, any[]>>(new Map());
  const [loadingRepayments, setLoadingRepayments] = useState<Set<number>>(new Set());
  const [topUpRequests, setTopUpRequests] = useState<any[]>([]);
  const [topUpRequestsLoading, setTopUpRequestsLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { toast } = useToast();
  const { refreshKey } = useRefresh();

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
    // Run all independent fetches in parallel — no reason to wait for each other
    Promise.all([
      fetchDashboard(),
      fetchUnreadNotifications(),
      fetchEligibility(),
      fetchActiveLoans(),
      fetchPendingGuarantees(),
      fetchTopUpRequests(),
    ]);
  }, [refreshKey]);

  const fetchDashboard = async () => {
    try {
      // Get token from session object first (where AuthContext stores it)
      let token = localStorage.getItem('token');
      if (!token) {
        const sessionStr = localStorage.getItem('session');
        if (sessionStr) {
          try {
            const session = JSON.parse(sessionStr);
            token = session.token;
          } catch (e) {
            console.error('Failed to parse session:', e);
          }
        }
      }

      if (!token) {
        console.log('DEBUG: No token found, redirecting to member login');
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
        localStorage.removeItem('session');
        navigate('/member');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('session');  // ✅ Also remove session
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
        // Fetch repayments + top-up history on first expand only
        fetchLoanTopUpHistory(loanId);
        fetchLoanRepayments(loanId);
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
      // Get token from session object first (where AuthContext stores it)
      let token = localStorage.getItem('token');
      if (!token) {
        const sessionStr = localStorage.getItem('session');
        if (sessionStr) {
          try {
            const session = JSON.parse(sessionStr);
            token = session.token;
          } catch (e) {
            console.error('Failed to parse session:', e);
          }
        }
      }
      
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
        
        // Sort by disbursement date (most recent first), then by application date
        const loans = allLoans.sort((a: any, b: any) => {
          const dateA = a.disbursementDate || a.applicationDate;
          const dateB = b.disbursementDate || b.applicationDate;
          return new Date(dateB).getTime() - new Date(dateA).getTime();
        });
        
        // repayments are fetched lazily only when a loan card is expanded
        // to avoid making N API calls on page load
        const loansWithRepayments = loans.map((loan: any) => ({
          ...loan,
          repayments: loan.repayments || []
        }));
        
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

  const fetchLoanTopUpHistory = async (loanId: number) => {
    setLoadingTopUpHistory(prev => new Set(prev).add(loanId));
    try {
      const response = await api.get(`/loans/${loanId}/topup-history`);
      const history = response.data?.data || response.data || [];
      setLoanTopUpHistory(prev => new Map(prev).set(loanId, history));
    } catch (error) {
      console.error(`Error fetching top-up history for loan ${loanId}:`, error);
      setLoanTopUpHistory(prev => new Map(prev).set(loanId, []));
    } finally {
      setLoadingTopUpHistory(prev => {
        const next = new Set(prev);
        next.delete(loanId);
        return next;
      });
    }
  };

  const fetchLoanRepayments = async (loanId: number) => {
    // Skip if already loaded
    if (loanRepayments.has(loanId)) return;
    setLoadingRepayments(prev => new Set(prev).add(loanId));
    try {
      const response = await api.get(`/member/loans/${loanId}/repayments`);
      const repayments = Array.isArray(response.data) ? response.data : (response.data?.data || []);
      setLoanRepayments(prev => new Map(prev).set(loanId, repayments));
    } catch (error) {
      console.error(`Error fetching repayments for loan ${loanId}:`, error);
      setLoanRepayments(prev => new Map(prev).set(loanId, []));
    } finally {
      setLoadingRepayments(prev => {
        const next = new Set(prev);
        next.delete(loanId);
        return next;
      });
    }
  };

  const fetchTopUpRequests = async () => {    setTopUpRequestsLoading(true);
    try {
      const response = await api.get('/member/topup-requests');
      const requests = response.data?.data || response.data || [];
      setTopUpRequests(requests);
    } catch (error) {
      console.error('Error fetching top-up requests:', error);
      setTopUpRequests([]);
    } finally {
      setTopUpRequestsLoading(false);
    }
  };

  const fetchPendingGuarantees = async () => {
    try {
      const [loanResponse, topUpResponse] = await Promise.all([
        api.get('/loans/member/guarantor-requests'),
        api.get('/member/pending-topup-guarantees')
      ]);
      
      // Handle response data safely - backend might return { data: [...] } or just [...]
      const loanData = Array.isArray(loanResponse.data) 
        ? loanResponse.data 
        : (loanResponse.data?.data || []);
      
      const topUpData = Array.isArray(topUpResponse.data) 
        ? topUpResponse.data 
        : (topUpResponse.data?.data || []);
      
      // Combine both loan and top-up guarantees
      const loanGuarantees = loanData.map((g: any) => ({ ...g, type: 'loan' }));
      const topUpGuarantees = topUpData.map((g: any) => ({ ...g, type: 'topup' }));
      
      setPendingGuarantees([...loanGuarantees, ...topUpGuarantees]);
    } catch (error) {
      console.error('Error fetching pending guarantees:', error);
      setPendingGuarantees([]);
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
      <MemberLayout memberName="Member" onLogout={handleLogout} unreadNotifications={0}>
        <div className="space-y-4 w-full max-w-7xl mx-auto">
          {/* Header Skeleton */}
          <div className="space-y-2">
            <div className="h-10 bg-gray-200 rounded animate-pulse w-64"></div>
            <div className="h-5 bg-gray-200 rounded animate-pulse w-32"></div>
          </div>

          {/* Stats Cards Skeleton */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="border rounded-lg p-6 space-y-3 bg-white">
                <div className="h-4 bg-gray-200 rounded animate-pulse w-24"></div>
                <div className="h-8 bg-gray-200 rounded animate-pulse w-32"></div>
                <div className="h-3 bg-gray-200 rounded animate-pulse w-20"></div>
              </div>
            ))}
          </div>

          {/* Recent Transactions Skeleton */}
          <div className="border rounded-lg p-6 space-y-4 bg-white">
            <div className="h-6 bg-gray-200 rounded animate-pulse w-48"></div>
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="flex items-center justify-between border-b pb-3">
                  <div className="space-y-2 flex-1">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-32"></div>
                    <div className="h-3 bg-gray-200 rounded animate-pulse w-24"></div>
                  </div>
                  <div className="h-5 bg-gray-200 rounded animate-pulse w-20"></div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </MemberLayout>
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
      {/* Desktop Notification Prompt - shows once per session */}
      <NotificationPrompt />
      
      <div className="space-y-4 w-full max-w-7xl mx-auto">
        <div className="space-y-2">
          <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-foreground">Welcome, {dashboard?.firstName}!</h1>
          <p className="text-sm sm:text-base text-muted-foreground">Member #{dashboard?.memberNumber}</p>
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
          <TabsContent value="home" className="space-y-4 sm:space-y-6">
            {/* Pending Guarantor Requests Section */}
            {pendingGuarantees.length > 0 && (
              <Card className="border-purple-200 bg-purple-50">
                <CardHeader className="p-4 sm:p-6">
                  <CardTitle className="flex items-center gap-2 text-purple-900 text-base sm:text-lg">
                    <HandshakeIcon className="h-5 w-5 flex-shrink-0" />
                    <span>Pending Guarantor Requests ({pendingGuarantees.length})</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-4 sm:p-6 pt-0 sm:pt-0">
                  <p className="text-xs sm:text-sm text-purple-800 mb-4">
                    You have been asked to guarantee the following loan applications. Click on each request to review and approve/reject.
                  </p>
                  <div className="grid gap-3 sm:gap-4 sm:grid-cols-2 lg:grid-cols-2">
                    {pendingGuarantees.map((guarantee: any) => (
                      <Card 
                        key={guarantee.id} 
                        className="border-purple-300 cursor-pointer hover:shadow-md transition-shadow bg-white"
                        onClick={() => {
                          if (guarantee.type === 'topup') {
                            setSelectedTopUpGuarantee(guarantee);
                            setTopUpGuarantorDialogOpen(true);
                          } else {
                            setSelectedGuarantee(guarantee);
                            setGuarantorDialogOpen(true);
                          }
                        }}
                      >
                        <CardContent className="p-3 sm:p-4">
                          <div className="space-y-2">
                            <div className="flex items-start justify-between gap-2">
                              <div className="flex-1 min-w-0">
                                <p className="font-semibold text-sm truncate">
                                  {guarantee.loan?.member?.firstName} {guarantee.loan?.member?.lastName}
                                </p>
                                <p className="text-xs text-muted-foreground">
                                  Member: {guarantee.loan?.member?.memberNumber}
                                </p>
                              </div>
                              <span className="bg-purple-100 text-purple-800 text-xs px-2 py-1 rounded font-medium whitespace-nowrap flex-shrink-0">
                                {guarantee.type === 'topup' ? 'Top-Up' : 'New Loan'}
                              </span>
                            </div>
                            <div className="bg-gray-50 rounded p-2 space-y-1 text-xs">
                              <div className="flex justify-between gap-2">
                                <span className="text-muted-foreground">
                                  {guarantee.type === 'topup' ? 'Top-Up Amount:' : 'Loan Amount:'}
                                </span>
                                <span className="font-medium">
                                  {formatCurrency(guarantee.type === 'topup' ? guarantee.topUpRequest?.requestedAmount : guarantee.loan?.amount)}
                                </span>
                              </div>
                              <div className="flex justify-between gap-2">
                                <span className="text-muted-foreground">Your Guarantee:</span>
                                <span className="font-semibold text-purple-700">
                                  {formatCurrency(guarantee.guaranteeAmount)}
                                </span>
                              </div>
                            </div>
                            <div className="pt-2 border-t">
                              <p className="text-xs text-purple-700 font-medium">
                                Click to Review →
                              </p>
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}

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

            {/* Loan Awaiting Guarantor Approval Alert */}
            {activeLoans.some(l => l.status === 'PENDING_GUARANTOR_APPROVAL') && (
              <Card className="border-blue-200 bg-blue-50">
                <CardContent className="pt-6">
                  <div className="flex items-start gap-4">
                    <div className="flex-1">
                      <h3 className="font-semibold text-blue-900 mb-2">Loan Application Submitted</h3>
                      <p className="text-sm text-blue-800 mb-3">
                        You have {activeLoans.filter(l => l.status === 'PENDING_GUARANTOR_APPROVAL').length} loan application(s) awaiting guarantor approval.
                        The loan details will be available once the guarantor(s) approve.
                      </p>
                      <Button 
                        size="sm"
                        onClick={() => setActiveTab('loans')}
                        className="bg-blue-600 hover:bg-blue-700"
                      >
                        View Loan Status
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
                {/* MPesa Cards - TODO: Not Implemented - Completely hidden */}
                {/* <Card className="border-none shadow-sm cursor-not-allowed hover:shadow-md transition-shadow"
                  // TODO: MPesa deposit not implemented
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
                  className="border-none shadow-sm cursor-not-allowed hover:shadow-md transition-shadow"
                  // TODO: MPesa withdraw not implemented
                >
                  <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                    <CardTitle className="text-xs md:text-sm font-medium text-muted-foreground">Withdraw via M-Pesa</CardTitle>
                    <Send className="h-4 w-4 md:h-5 md:w-5 text-primary" />
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground text-xs md:text-sm">Withdraw from savings account</p>
                  </CardContent>
                </Card> */}

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
                              <p className="font-semibold text-red-600">
                                {loan.outstandingBalance !== null && loan.outstandingBalance !== undefined 
                                  ? formatCurrency(loan.outstandingBalance) 
                                  : 'Awaiting Approval'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Monthly Payment</p>
                              <p className="font-semibold">
                                {loan.monthlyRepayment !== null && loan.monthlyRepayment !== undefined 
                                  ? formatCurrency(loan.monthlyRepayment) 
                                  : 'Pending'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Repayments Made</p>
                              <p className="font-semibold">{loan.repayments?.length || 0}</p>
                            </div>
                          </div>

                          {/* Top-Up Information - Show details when expanded */}
                          {(loan.status === 'ACTIVE' || loan.status === 'DISBURSED' || loan.status === 'APPROVED') && loan.outstandingBalance > 0 && (
                            <div className="border-t pt-4">
                              <div className="bg-purple-50 border border-purple-200 rounded-lg p-3">
                                <p className="text-xs text-purple-700">
                                  💡 <strong>Top-Up Info:</strong> You can request additional funds on this loan. The same guarantee approval process applies.
                                </p>
                              </div>
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
                <h3 className="text-lg font-semibold text-foreground">Your Loans</h3>
                {activeLoans
                  .filter(l => l.status !== 'PENDING_GUARANTOR_REPLACEMENT' && l.status !== 'PENDING_GUARANTOR_REASSIGNMENT')
                  .map((loan) => (
                    <Card key={loan.id} className={loan.status === 'REPAID' ? 'border-gray-300 bg-gray-50' : 'border-blue-200'}>
                      <CardHeader 
                        className="cursor-pointer hover:bg-gray-50 transition-colors"
                        onClick={() => toggleLoanExpansion(loan.id)}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <CardTitle className="text-base">Loan #{loan.loanNumber}</CardTitle>
                            <p className="text-sm text-muted-foreground">Amount: {formatCurrency(loan.amount)}</p>
                            
                            {/* Top-Up Quick Action - Visible even when collapsed */}
                            {(loan.status === 'ACTIVE' || loan.status === 'DISBURSED' || loan.status === 'APPROVED') && loan.outstandingBalance > 0 && (
                              <Button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setTopUpLoan(loan);
                                  setTopUpDialogOpen(true);
                                }}
                                className="bg-purple-600 hover:bg-purple-700 gap-2 mt-2"
                                size="sm"
                              >
                                <ArrowUp className="h-4 w-4" />
                                Request Top-Up
                              </Button>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            <span className={`px-3 py-1 rounded-full text-xs sm:text-sm font-medium whitespace-normal sm:whitespace-nowrap text-center leading-tight ${
                              loan.status === 'REPAID'
                                ? 'bg-gray-200 text-gray-800'
                                : loan.status === 'DEFAULTED'
                                ? 'bg-red-100 text-red-800'
                                : loan.status === 'APPROVED' || loan.status === 'DISBURSED' || loan.status === 'ACTIVE' 
                                ? 'bg-green-50 text-green-700' 
                                : loan.status === 'REJECTED'
                                ? 'bg-red-50 text-red-700'
                                : 'bg-yellow-50 text-yellow-700'
                            }`}>
                              <span className="hidden sm:inline">
                                {/* Desktop: Full labels */}
                                {loan.status === 'DISBURSED' ? 'Disbursed' :
                                loan.status === 'ACTIVE' ? 'Active' :
                                loan.status === 'REPAID' ? 'Fully Repaid' :
                                loan.status === 'DEFAULTED' ? 'Defaulted' :
                                loan.status === 'PENDING' ? 'Pending' :
                                loan.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Pending Guarantor Approval' :
                                loan.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Pending Loan Officer Review' :
                                loan.status === 'PENDING_CREDIT_COMMITTEE' ? 'Pending Credit Committee' :
                                loan.status === 'PENDING_TREASURER' ? 'Pending Treasurer' :
                                loan.status === 'APPROVED' ? 'Approved' :
                                loan.status}
                              </span>
                              <span className="inline sm:hidden">
                                {/* Mobile: Shorter labels */}
                                {loan.status === 'DISBURSED' ? 'Disbursed' :
                                loan.status === 'ACTIVE' ? 'Active' :
                                loan.status === 'REPAID' ? 'Repaid' :
                                loan.status === 'DEFAULTED' ? 'Defaulted' :
                                loan.status === 'PENDING' ? 'Pending' :
                                loan.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Guarantor' :
                                loan.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Officer Review' :
                                loan.status === 'PENDING_CREDIT_COMMITTEE' ? 'Committee' :
                                loan.status === 'PENDING_TREASURER' ? 'Treasurer' :
                                loan.status === 'APPROVED' ? 'Approved' :
                                loan.status}
                              </span>
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
                              <p className="font-semibold text-red-600">
                                {loan.outstandingBalance !== null && loan.outstandingBalance !== undefined 
                                  ? formatCurrency(loan.outstandingBalance) 
                                  : 'Awaiting Approval'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Monthly Payment</p>
                              <p className="font-semibold">
                                {loan.monthlyRepayment !== null && loan.monthlyRepayment !== undefined 
                                  ? formatCurrency(loan.monthlyRepayment) 
                                  : 'Pending'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Repayments Made</p>
                              <p className="font-semibold">{loan.repayments?.length || 0}</p>
                            </div>
                          </div>

                          {/* Repayment Progress */}
                          <div className="border-t pt-4">
                            <p className="text-sm font-semibold mb-3">Repayment Progress</p>
                            {loan.totalRepayable === null || loan.totalRepayable === undefined ? (
                              <div className="p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">
                                This loan is awaiting approval. Repayment details will be available once approved by the treasurer.
                              </div>
                            ) : (
                            <div className="space-y-3">
                              <div>
                                <div className="flex justify-between text-sm mb-1">
                                  <span className="text-muted-foreground">Progress</span>
                                  <span className="font-medium">
                                    {loan.principalRepaid !== undefined && loan.amount
                                      ? `${formatCurrency(loan.principalRepaid)} / ${formatCurrency(loan.amount)}`
                                      : `${formatCurrency(loan.amount)} [No calc]`}
                                  </span>
                                </div>
                                <div className="w-full bg-gray-200 rounded-full h-2">
                                  <div 
                                    className={`h-2 rounded-full transition-all duration-300 ${
                                      loan.status === 'REPAID' ? 'bg-green-600' : 'bg-green-600'
                                    }`}
                                    style={{ width: loan.repaymentPercentage !== undefined
                                      ? `${Math.min(Math.max(0, Number(loan.repaymentPercentage)), 100)}%`
                                      : "0%" }}
                                  />
                                </div>
                                <div className="flex justify-between text-xs text-muted-foreground mt-1">
                                  <span>{loan.repaymentPercentage !== undefined
                                    ? `${Number(loan.repaymentPercentage).toFixed(2)}% repaid`
                                    : "0% repaid"}</span>
                                  <span>
                                    {loan.status === 'REPAID' 
                                      ? '✓ Fully Repaid' 
                                      : formatCurrency(loan.outstandingBalance) + ' remaining'
                                    }
                                  </span>
                                </div>
                              </div>
                            </div>
                            )}
                          </div>

                          {/* Top-Up History */}
                          {(loan.status === 'ACTIVE' || loan.status === 'DISBURSED' || loan.status === 'APPROVED') && (
                            <div className="border-t pt-4">
                              <p className="text-sm font-semibold mb-3 text-purple-900">💰 Top-Up History</p>
                              {loadingTopUpHistory.has(loan.id) ? (
                                <p className="text-xs text-muted-foreground">Loading top-up history...</p>
                              ) : loanTopUpHistory.get(loan.id) && loanTopUpHistory.get(loan.id)!.length > 0 ? (
                                <div className="space-y-2 max-h-48 overflow-y-auto">
                                  {loanTopUpHistory.get(loan.id)!.map((topup: any) => (
                                    <div key={topup.id} className="p-3 bg-purple-50 border border-purple-200 rounded text-xs">
                                      <div className="flex justify-between items-start mb-2">
                                        <div>
                                          <p className="font-semibold text-purple-900">KES {topup.topupAmount?.toLocaleString()}</p>
                                          <p className="text-gray-600">{new Date(topup.topupDate).toLocaleDateString('en-KE')}</p>
                                        </div>
                                        <span className="px-2 py-1 bg-purple-100 text-purple-800 rounded text-xs font-medium">
                                          Top-Up #{topup.id}
                                        </span>
                                      </div>
                                      <div className="grid grid-cols-2 gap-2 text-xs mb-2">
                                        <div>
                                          <span className="text-gray-600">Before:</span>
                                          <span className="font-medium ml-1">KES {topup.outstandingBeforeTopup?.toLocaleString()}</span>
                                        </div>
                                        <div>
                                          <span className="text-gray-600">After:</span>
                                          <span className="font-medium ml-1">KES {topup.outstandingAfterTopup?.toLocaleString()}</span>
                                        </div>
                                      </div>
                                      {topup.purpose && (
                                        <p className="text-gray-700 mt-1">
                                          <span className="font-medium">Purpose:</span> {topup.purpose}
                                        </p>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <p className="text-xs text-muted-foreground">No top-ups for this loan yet</p>
                              )}
                            </div>
                          )}

                          {/* Repayment History */}
                          {loadingRepayments.has(loan.id) ? (
                            <div className="border-t pt-4">
                              <p className="text-sm text-muted-foreground">Loading repayments...</p>
                            </div>
                          ) : loanRepayments.get(loan.id) && loanRepayments.get(loan.id)!.length > 0 ? (
                            <div className="border-t pt-4">
                              <p className="text-sm font-semibold mb-3">Repayment History</p>
                              <div className="space-y-2 max-h-48 overflow-y-auto">
                                {loanRepayments.get(loan.id)!.map((repayment: any) => (
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

            {/* Your Top-Ups Section */}
            <div className="space-y-3 mt-6">
              <h3 className="text-lg font-semibold text-foreground text-purple-900">💰 Your Top-Up Requests</h3>
              {topUpRequestsLoading ? (
                <Card>
                  <CardContent className="pt-6 text-center">
                    <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4" />
                    <p className="text-muted-foreground">Loading your top-up requests...</p>
                  </CardContent>
                </Card>
              ) : topUpRequests.length === 0 ? (
                <Card>
                  <CardContent className="pt-6 text-center">
                    <p className="text-muted-foreground">No top-up requests yet</p>
                  </CardContent>
                </Card>
              ) : (
                topUpRequests.map((topup: any) => (
                  <Card key={topup.id} className="border-purple-200 bg-purple-50/30">
                    <CardHeader className="cursor-pointer hover:bg-purple-50 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <CardTitle className="text-base text-purple-900">
                            Top-Up Request #{topup.id}
                          </CardTitle>
                          <p className="text-sm text-muted-foreground">
                            Loan: {topup.loan?.loanNumber} | Amount: {formatCurrency(topup.requestedAmount)}
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">
                            Requested: {new Date(topup.requestedDate).toLocaleDateString('en-KE')}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`px-3 py-1 rounded-full text-xs sm:text-sm font-medium whitespace-normal sm:whitespace-nowrap text-center leading-tight ${
                            topup.status === 'DISBURSED'
                              ? 'bg-green-100 text-green-800'
                              : topup.status === 'PENDING_GUARANTOR_APPROVAL'
                              ? 'bg-yellow-100 text-yellow-800'
                              : topup.status === 'PENDING_REVIEW' || topup.status === 'PENDING_LOAN_OFFICER_REVIEW'
                              ? 'bg-blue-100 text-blue-800'
                              : topup.status === 'PENDING_CREDIT_COMMITTEE'
                              ? 'bg-indigo-100 text-indigo-800'
                              : topup.status === 'PENDING_TREASURER'
                              ? 'bg-orange-100 text-orange-800'
                              : topup.status === 'APPROVED'
                              ? 'bg-teal-100 text-teal-800'
                              : topup.status === 'REJECTED'
                              ? 'bg-red-100 text-red-800'
                              : topup.status === 'CANCELLED'
                              ? 'bg-gray-100 text-gray-800'
                              : 'bg-purple-100 text-purple-800'
                          }`}>
                            <span className="hidden sm:inline">
                              {/* Desktop: Full labels */}
                              {topup.status === 'DISBURSED' ? 'Disbursed' :
                              topup.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Pending Guarantor Approval' :
                              topup.status === 'PENDING_REVIEW' || topup.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Pending Loan Officer Review' :
                              topup.status === 'PENDING_CREDIT_COMMITTEE' ? 'Pending Credit Committee' :
                              topup.status === 'PENDING_TREASURER' ? 'Pending Treasurer' :
                              topup.status === 'APPROVED' ? 'Approved — Awaiting Disbursement' :
                              topup.status === 'REJECTED' ? 'Rejected' :
                              topup.status === 'CANCELLED' ? 'Cancelled' :
                              topup.status.replace(/_/g, ' ')}
                            </span>
                            <span className="inline sm:hidden">
                              {/* Mobile: Shorter labels */}
                              {topup.status === 'DISBURSED' ? 'Disbursed' :
                              topup.status === 'PENDING_GUARANTOR_APPROVAL' ? 'Guarantor' :
                              topup.status === 'PENDING_REVIEW' || topup.status === 'PENDING_LOAN_OFFICER_REVIEW' ? 'Officer Review' :
                              topup.status === 'PENDING_CREDIT_COMMITTEE' ? 'Committee' :
                              topup.status === 'PENDING_TREASURER' ? 'Treasurer' :
                              topup.status === 'APPROVED' ? 'Approved' :
                              topup.status === 'REJECTED' ? 'Rejected' :
                              topup.status === 'CANCELLED' ? 'Cancelled' :
                              topup.status.replace(/_/g, ' ')}
                            </span>
                          </span>
                        </div>
                      </div>
                      
                      {/* Top-Up Details */}
                      <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4 bg-white p-3 rounded border border-purple-200">
                        <div>
                          <p className="text-xs text-muted-foreground">Requested Amount</p>
                          <p className="font-semibold text-purple-900">{formatCurrency(topup.requestedAmount)}</p>
                        </div>
                        {topup.reviewedBy && (
                          <div>
                            <p className="text-xs text-muted-foreground">Reviewed By</p>
                            <p className="font-medium text-sm">{topup.reviewedBy.username}</p>
                          </div>
                        )}
                        {topup.reviewDate && (
                          <div>
                            <p className="text-xs text-muted-foreground">Review Date</p>
                            <p className="font-medium text-sm">{new Date(topup.reviewDate).toLocaleDateString('en-KE')}</p>
                          </div>
                        )}
                        {topup.disbursementDate && (
                          <div>
                            <p className="text-xs text-muted-foreground">Disbursement Date</p>
                            <p className="font-medium text-sm">{new Date(topup.disbursementDate).toLocaleDateString('en-KE')}</p>
                          </div>
                        )}
                      </div>

                      {/* Purpose */}
                      {topup.purpose && (
                        <div className="mt-3 bg-white p-3 rounded border border-purple-200">
                          <p className="text-xs text-muted-foreground mb-1">Purpose</p>
                          <p className="text-sm">{topup.purpose}</p>
                        </div>
                      )}

                      {/* Guarantors */}
                      {topup.guarantors && topup.guarantors.length > 0 && (
                        <div className="mt-3 bg-white p-3 rounded border border-purple-200">
                          <p className="text-xs text-muted-foreground mb-2">Guarantors ({topup.guarantors.length})</p>
                          <div className="space-y-2">
                            {topup.guarantors.map((guarantor: any, idx: number) => (
                              <div key={idx} className="flex items-center justify-between text-sm">
                                <div>
                                  <p className="font-medium">
                                    {guarantor.member?.firstName} {guarantor.member?.lastName}
                                  </p>
                                  <p className="text-xs text-muted-foreground">
                                    {guarantor.member?.memberNumber}
                                  </p>
                                </div>
                                <div className="text-right">
                                  <p className="font-semibold">{formatCurrency(guarantor.guaranteeAmount)}</p>
                                  <span className={`text-xs px-2 py-1 rounded ${
                                    guarantor.status === 'APPROVED'
                                      ? 'bg-green-100 text-green-700'
                                      : guarantor.status === 'REJECTED'
                                      ? 'bg-red-100 text-red-700'
                                      : 'bg-yellow-100 text-yellow-700'
                                  }`}>
                                    {guarantor.status}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Top-Up Status Timeline */}
                      <div className="mt-4 border-t pt-4">
                        <TopUpStatusTimeline
                          currentStatus={topup.status}
                          requestedDate={topup.requestedDate}
                          reviewDate={topup.reviewDate}
                          disbursementDate={topup.disbursementDate}
                          rejectionReason={topup.rejectionReason}
                          loanNumber={topup.loan?.loanNumber}
                          requestedAmount={topup.requestedAmount}
                        />
                      </div>
                    </CardHeader>
                  </Card>
                ))
              )}
            </div>
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
          onOpenChange={(open) => {
            setGuarantorDialogOpen(open);
            if (!open) {
              setSelectedGuarantee(null);
              fetchPendingGuarantees();
            }
          }}
          onApprovalChange={() => {
            fetchDashboard();
            fetchPendingGuarantees();
          }}
          selectedGuarantee={selectedGuarantee}
        />

        {/* Top-Up Guarantor Approval Modal */}
        <TopUpGuarantorApprovalModal
          open={topUpGuarantorDialogOpen}
          onOpenChange={(open) => {
            setTopUpGuarantorDialogOpen(open);
            if (!open) {
              setSelectedTopUpGuarantee(null);
              fetchPendingGuarantees();
            }
          }}
          topUpRequest={selectedTopUpGuarantee?.topUpRequest}
          guaranteeAmount={selectedTopUpGuarantee?.guaranteeAmount || 0}
          topUpRequestId={selectedTopUpGuarantee?.topUpRequest?.id || 0}
          onSuccess={() => {
            fetchDashboard();
            fetchPendingGuarantees();
            setTopUpGuarantorDialogOpen(false);
            setSelectedTopUpGuarantee(null);
          }}
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

        {/* M-Pesa Deposit - TODO: Not Implemented */}
        {/* <MpesaTransaction
          open={mpesaDepositOpen}
          onOpenChange={setMpesaDepositOpen}
          type="deposit"
          onSuccess={fetchDashboard}
        /> */}

        {/* M-Pesa Withdraw - TODO: Not Implemented */}
        {/* <MpesaTransaction
          open={mpesaWithdrawOpen}
          onOpenChange={setMpesaWithdrawOpen}
          type="withdraw"
          onSuccess={fetchDashboard}
        /> */}

        {/* Loan Top-Up Request Dialog */}
        {topUpLoan && (
          <LoanTopUpRequestDialog
            isOpen={topUpDialogOpen}
            onClose={() => {
              setTopUpDialogOpen(false);
              setTopUpLoan(null);
            }}
            loanId={topUpLoan.id}
            loanNumber={topUpLoan.loanNumber}
            currentOutstanding={topUpLoan.outstandingBalance || 0}
            onSuccess={() => {
              fetchActiveLoans();
              fetchDashboard();
            }}
          />
        )}
      </div>
    </MemberLayout>
  );
}
