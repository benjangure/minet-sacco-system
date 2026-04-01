import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useToast } from '@/hooks/use-toast';
import { CheckCircle, XCircle, AlertCircle, Clock } from 'lucide-react';

const API_BASE_URL = 'http://localhost:8080/api';

interface GuarantorRequest {
  id: number;
  loan: {
    id: number;
    loanNumber: string;
    amount: number;
    member: {
      firstName: string;
      lastName: string;
      memberNumber: string;
    };
    loanProduct: {
      name: string;
    };
    applicationDate: string;
  };
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'DECLINED' | 'RELEASED';
  pledgeAmount: number;
  rejectionReason?: string;
  createdAt: string;
  approvedAt?: string;
}

interface GuarantorEligibility {
  eligible: boolean;
  savingsBalance: number;
  sharesBalance: number;
  currentPledges: number;
  availableCapacity: number;
  activeGuarantorships: number;
  loanAmount: number;
  canGuarantee: boolean;
  errors: string[];
  warnings: string[];
}

export default function GuarantorApprovals() {
  const [requests, setRequests] = useState<GuarantorRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState<GuarantorRequest | null>(null);
  const [eligibility, setEligibility] = useState<GuarantorEligibility | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const { session } = useAuth();
  const { toast } = useToast();

  useEffect(() => {
    fetchPendingRequests();
  }, []);

  const fetchPendingRequests = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/member/guarantor-requests/pending`, {
        headers: { 'Authorization': `Bearer ${session?.token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setRequests(data.data || []);
      }
    } catch (error) {
      console.error('Error fetching requests:', error);
      toast({ title: 'Error', description: 'Failed to load guarantor requests', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const checkEligibility = async (request: GuarantorRequest) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/member/guarantor-eligibility/${session?.user?.id}/${request.loan.amount}`,
        { headers: { 'Authorization': `Bearer ${session?.token}` } }
      );

      if (response.ok) {
        const data = await response.json();
        setEligibility(data);
      }
    } catch (error) {
      console.error('Error checking eligibility:', error);
    }
  };

  const handleApprove = async (request: GuarantorRequest) => {
    setSelectedRequest(request);
    await checkEligibility(request);
  };

  const submitApproval = async () => {
    if (!selectedRequest) return;

    setSubmitting(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/member/guarantor-requests/${selectedRequest.id}/approve`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${session?.token}` }
        }
      );

      if (response.ok) {
        toast({ title: 'Success', description: 'Guarantor request approved successfully' });
        setSelectedRequest(null);
        setEligibility(null);
        fetchPendingRequests();
      } else {
        const error = await response.json();
        toast({ title: 'Error', description: error.message || 'Failed to approve request', variant: 'destructive' });
      }
    } catch (error) {
      console.error('Error approving request:', error);
      toast({ title: 'Error', description: 'Failed to approve request', variant: 'destructive' });
    } finally {
      setSubmitting(false);
    }
  };

  const submitRejection = async () => {
    if (!selectedRequest) return;

    setSubmitting(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/member/guarantor-requests/${selectedRequest.id}/reject?reason=${encodeURIComponent(rejectionReason)}`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${session?.token}` }
        }
      );

      if (response.ok) {
        toast({ title: 'Success', description: 'Guarantor request rejected successfully' });
        setSelectedRequest(null);
        setRejectionReason('');
        setRejectDialogOpen(false);
        fetchPendingRequests();
      } else {
        const error = await response.json();
        toast({ title: 'Error', description: error.message || 'Failed to reject request', variant: 'destructive' });
      }
    } catch (error) {
      console.error('Error rejecting request:', error);
      toast({ title: 'Error', description: 'Failed to reject request', variant: 'destructive' });
    } finally {
      setSubmitting(false);
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

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACCEPTED':
        return <CheckCircle className="h-5 w-5 text-green-600" />;
      case 'REJECTED':
      case 'DECLINED':
        return <XCircle className="h-5 w-5 text-red-600" />;
      case 'PENDING':
        return <Clock className="h-5 w-5 text-yellow-600" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-600" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACCEPTED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
      case 'DECLINED':
        return 'bg-red-100 text-red-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return <div className="flex justify-center items-center h-screen">Loading...</div>;
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div>
        <h1 className="text-3xl font-bold">Guarantor Approval Requests</h1>
        <p className="text-muted-foreground mt-2">Review and approve/reject loan guarantor requests</p>
      </div>

      {requests.length === 0 ? (
        <Card>
          <CardContent className="pt-6">
            <p className="text-center text-muted-foreground">No pending guarantor requests</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {requests.map((request) => (
            <Card key={request.id}>
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-muted-foreground">Borrower</p>
                    <p className="font-semibold">{request.loan.member.firstName} {request.loan.member.lastName}</p>
                    <p className="text-xs text-muted-foreground">Member #: {request.loan.member.memberNumber}</p>
                  </div>

                  <div>
                    <p className="text-sm text-muted-foreground">Loan Product</p>
                    <p className="font-semibold">{request.loan.loanProduct.name}</p>
                    <p className="text-xs text-muted-foreground">Amount: {formatCurrency(request.loan.amount)}</p>
                  </div>

                  <div>
                    <p className="text-sm text-muted-foreground">Pledge Amount</p>
                    <p className="font-semibold text-lg">{formatCurrency(request.pledgeAmount)}</p>
                  </div>

                  <div>
                    <p className="text-sm text-muted-foreground">Status</p>
                    <div className="flex items-center gap-2 mt-1">
                      {getStatusIcon(request.status)}
                      <Badge className={getStatusColor(request.status)}>
                        {request.status}
                      </Badge>
                    </div>
                  </div>

                  <div>
                    <p className="text-sm text-muted-foreground">Application Date</p>
                    <p className="font-semibold">{formatDate(request.loan.applicationDate)}</p>
                  </div>

                  {request.approvedAt && (
                    <div>
                      <p className="text-sm text-muted-foreground">Approved Date</p>
                      <p className="font-semibold">{formatDate(request.approvedAt)}</p>
                    </div>
                  )}
                </div>

                {request.rejectionReason && (
                  <Alert className="mt-4 bg-red-50 border-red-200">
                    <AlertCircle className="h-4 w-4 text-red-600" />
                    <AlertDescription className="text-red-800">
                      <strong>Rejection Reason:</strong> {request.rejectionReason}
                    </AlertDescription>
                  </Alert>
                )}

                {request.status === 'PENDING' && (
                  <div className="flex gap-2 mt-4">
                    <Dialog open={selectedRequest?.id === request.id && !rejectDialogOpen}>
                      <DialogTrigger asChild>
                        <Button
                          variant="default"
                          onClick={() => handleApprove(request)}
                        >
                          Approve
                        </Button>
                      </DialogTrigger>
                      <DialogContent>
                        <DialogHeader>
                          <DialogTitle>Approve Guarantor Request</DialogTitle>
                        </DialogHeader>

                        {eligibility && (
                          <div className="space-y-4">
                            <div className={`p-4 rounded-lg space-y-3 ${eligibility.canGuarantee ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                              <p className="font-semibold mb-3">Your Guarantor Eligibility</p>
                              
                              <div className="grid grid-cols-2 gap-4 text-sm">
                                <div>
                                  <p className="text-muted-foreground">Savings Balance</p>
                                  <p className="font-semibold">{formatCurrency(eligibility.savingsBalance)}</p>
                                </div>
                                <div>
                                  <p className="text-muted-foreground">Shares Balance</p>
                                  <p className="font-semibold">{formatCurrency(eligibility.sharesBalance)}</p>
                                </div>
                                <div>
                                  <p className="text-muted-foreground">Current Pledges</p>
                                  <p className="font-semibold text-orange-600">{formatCurrency(eligibility.currentPledges)}</p>
                                </div>
                                <div>
                                  <p className="text-muted-foreground">Available Capacity</p>
                                  <p className="font-semibold text-green-600">{formatCurrency(eligibility.availableCapacity)}</p>
                                </div>
                                <div>
                                  <p className="text-muted-foreground">Active Guarantorships</p>
                                  <p className="font-semibold">{eligibility.activeGuarantorships}</p>
                                </div>
                                <div>
                                  <p className="text-muted-foreground">Loan Amount to Guarantee</p>
                                  <p className="font-semibold text-blue-600">{formatCurrency(eligibility.loanAmount)}</p>
                                </div>
                              </div>

                              {eligibility.errors.length > 0 && (
                                <div className="mt-3 bg-red-100 border border-red-300 rounded p-2">
                                  <p className="text-sm font-semibold text-red-800 mb-1">Issues:</p>
                                  {eligibility.errors.map((error, idx) => (
                                    <p key={idx} className="text-xs text-red-700">• {error}</p>
                                  ))}
                                </div>
                              )}

                              {eligibility.warnings.length > 0 && (
                                <div className="mt-3 bg-yellow-100 border border-yellow-300 rounded p-2">
                                  <p className="text-sm font-semibold text-yellow-800 mb-1">Warnings:</p>
                                  {eligibility.warnings.map((warning, idx) => (
                                    <p key={idx} className="text-xs text-yellow-700">• {warning}</p>
                                  ))}
                                </div>
                              )}
                            </div>

                            <Alert>
                              <AlertDescription>
                                By approving, you pledge {formatCurrency(selectedRequest?.pledgeAmount || 0)} as guarantee for this loan. The borrower will be notified of your approval.
                              </AlertDescription>
                            </Alert>

                            <div className="flex gap-2">
                              <Button
                                variant="default"
                                onClick={submitApproval}
                                disabled={submitting || !eligibility.canGuarantee}
                              >
                                {submitting ? 'Approving...' : 'Confirm Approval'}
                              </Button>
                              <Button
                                variant="outline"
                                onClick={() => setSelectedRequest(null)}
                              >
                                Cancel
                              </Button>
                            </div>
                          </div>
                        )}
                      </DialogContent>
                    </Dialog>

                    <Dialog open={rejectDialogOpen && selectedRequest?.id === request.id}>
                      <DialogTrigger asChild>
                        <Button
                          variant="destructive"
                          onClick={() => {
                            setSelectedRequest(request);
                            setRejectDialogOpen(true);
                          }}
                        >
                          Reject
                        </Button>
                      </DialogTrigger>
                      <DialogContent>
                        <DialogHeader>
                          <DialogTitle>Reject Guarantor Request</DialogTitle>
                        </DialogHeader>

                        <div className="space-y-4">
                          <Alert>
                            <AlertDescription>
                              Please provide a reason for rejecting this guarantor request. The borrower will be notified.
                            </AlertDescription>
                          </Alert>

                          <div>
                            <Label htmlFor="reason">Rejection Reason</Label>
                            <Textarea
                              id="reason"
                              placeholder="Enter your reason for rejection..."
                              value={rejectionReason}
                              onChange={(e) => setRejectionReason(e.target.value)}
                              rows={4}
                            />
                          </div>

                          <div className="flex gap-2">
                            <Button
                              variant="destructive"
                              onClick={submitRejection}
                              disabled={submitting || !rejectionReason.trim()}
                            >
                              {submitting ? 'Rejecting...' : 'Confirm Rejection'}
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => {
                                setRejectDialogOpen(false);
                                setRejectionReason('');
                                setSelectedRequest(null);
                              }}
                            >
                              Cancel
                            </Button>
                          </div>
                        </div>
                      </DialogContent>
                    </Dialog>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
