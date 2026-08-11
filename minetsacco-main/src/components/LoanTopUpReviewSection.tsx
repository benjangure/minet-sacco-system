import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle,
  DialogDescription, DialogFooter,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useToast } from '@/hooks/use-toast';
import { CheckCircle, XCircle, Eye, AlertCircle, ArrowUp, DollarSign } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/config/api';

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

interface TopUpRequest {
  id: number;
  loanId: number;
  loanNumber: string;
  loan: {
    id: number;
    loanNumber: string;
    amount: number;
    outstandingBalance: number;
    member: {
      memberNumber: string;
      firstName: string;
      lastName: string;
    };
  };
  member: {
    memberNumber: string;
    firstName: string;
    lastName: string;
  };
  requestedAmount: number;
  purpose: string;
  status: string;
  requestedDate: string;
  reviewDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
  allGuarantorsApproved: boolean;
  guarantors: Array<{
    id: number;
    member: { firstName: string; lastName: string; memberNumber: string };
    guaranteeAmount: number;
    status: string;
  }>;
}

// ─────────────────────────────────────────────────────────────────────────────
// Status helpers
// ─────────────────────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<string, string> = {
  PENDING_GUARANTOR_APPROVAL: 'bg-yellow-100 text-yellow-800',
  PENDING_LOAN_OFFICER_REVIEW: 'bg-blue-100 text-blue-800',
  PENDING_REVIEW: 'bg-blue-100 text-blue-800', // legacy alias
  PENDING_CREDIT_COMMITTEE: 'bg-indigo-100 text-indigo-800',
  PENDING_TREASURER: 'bg-orange-100 text-orange-800',
  APPROVED: 'bg-teal-100 text-teal-800',
  DISBURSED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING_GUARANTOR_APPROVAL: 'Pending Guarantor Approval',
  PENDING_LOAN_OFFICER_REVIEW: 'Pending Loan Officer Review',
  PENDING_REVIEW: 'Pending Loan Officer Review',
  PENDING_CREDIT_COMMITTEE: 'Pending Credit Committee',
  PENDING_TREASURER: 'Pending Treasurer',
  APPROVED: 'Approved — Awaiting Disbursement',
  DISBURSED: 'Disbursed',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
};

// Which status each role acts on
const ROLE_ACTS_ON: Record<string, string[]> = {
  LOAN_OFFICER:     ['PENDING_LOAN_OFFICER_REVIEW', 'PENDING_REVIEW'],
  CREDIT_COMMITTEE: ['PENDING_CREDIT_COMMITTEE'],
  TREASURER:        ['PENDING_TREASURER', 'APPROVED'],
  ADMIN:            ['PENDING_LOAN_OFFICER_REVIEW', 'PENDING_REVIEW', 'PENDING_CREDIT_COMMITTEE', 'PENDING_TREASURER', 'APPROVED'],
};

const ROLE_SECTION_TITLE: Record<string, string> = {
  LOAN_OFFICER:     'Top-Up Requests — Loan Officer Review',
  CREDIT_COMMITTEE: 'Top-Up Requests — Credit Committee Review',
  TREASURER:        'Top-Up Requests — Treasury Review & Disbursement',
  ADMIN:            'Top-Up Requests — All Pending',
};

// ─────────────────────────────────────────────────────────────────────────────
// Component
// ─────────────────────────────────────────────────────────────────────────────

export default function LoanTopUpReviewSection() {
  const { role } = useAuth();
  const { toast } = useToast();

  const [requests, setRequests] = useState<TopUpRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<TopUpRequest | null>(null);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [actionOpen, setActionOpen] = useState(false);
  const [actionType, setActionType] = useState<'approve' | 'reject' | 'disburse'>('approve');
  const [reason, setReason] = useState('');
  const [processing, setProcessing] = useState(false);

  // Only render for roles that participate in the top-up review pipeline
  const activeRoles = ['LOAN_OFFICER', 'CREDIT_COMMITTEE', 'TREASURER', 'ADMIN'];
  if (!role || !activeRoles.includes(role)) return null;

  const sectionTitle = ROLE_SECTION_TITLE[role] ?? 'Top-Up Requests';
  const actingOnStatuses = ROLE_ACTS_ON[role] ?? [];

  useEffect(() => {
    fetchRequests();
  }, [role]);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/topup-requests/pending');
      setRequests(res.data.data || []);
    } catch (err: any) {
      console.error('Error fetching top-up requests:', err);
      toast({ title: 'Error', description: 'Failed to load top-up requests', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (n: number) =>
    new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES' }).format(n || 0);

  const formatDate = (d: string) =>
    new Date(d).toLocaleDateString('en-KE', { year: 'numeric', month: 'short', day: 'numeric' });

  const approvedGuarantors = (req: TopUpRequest) =>
    (req.guarantors || []).filter(g => g.status === 'APPROVED').length;

  const canAct = (req: TopUpRequest) => actingOnStatuses.includes(req.status);
  const canDisburse = (req: TopUpRequest) =>
    role === 'TREASURER' && req.status === 'APPROVED';

  // ── Action handlers ───────────────────────────────────────────────────────

  const openApprove = (req: TopUpRequest) => {
    setSelectedRequest(req);
    setActionType('approve');
    setReason('');
    setActionOpen(true);
  };

  const openReject = (req: TopUpRequest) => {
    setSelectedRequest(req);
    setActionType('reject');
    setReason('');
    setActionOpen(true);
  };

  const openDisburse = (req: TopUpRequest) => {
    setSelectedRequest(req);
    setActionType('disburse');
    setReason('');
    setActionOpen(true);
  };

  const handleApprove = async () => {
    if (!selectedRequest) return;
    try {
      setProcessing(true);
      await api.post(`/admin/topup-requests/${selectedRequest.id}/approve`, { comments: reason });
      toast({ title: 'Success', description: 'Top-up request approved' });
      setActionOpen(false);
      fetchRequests();
    } catch (err: any) {
      toast({ title: 'Error', description: err.response?.data?.message || 'Failed to approve', variant: 'destructive' });
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest || !reason.trim()) {
      toast({ title: 'Error', description: 'Please provide a rejection reason', variant: 'destructive' });
      return;
    }
    try {
      setProcessing(true);
      await api.post(`/admin/topup-requests/${selectedRequest.id}/reject`, { reason });
      toast({ title: 'Success', description: 'Top-up request rejected' });
      setActionOpen(false);
      fetchRequests();
    } catch (err: any) {
      toast({ title: 'Error', description: err.response?.data?.message || 'Failed to reject', variant: 'destructive' });
    } finally {
      setProcessing(false);
    }
  };

  const handleDisburse = async () => {
    if (!selectedRequest) return;
    try {
      setProcessing(true);
      await api.post(`/admin/topup-requests/${selectedRequest.id}/disburse`);
      toast({ title: 'Success', description: 'Top-up disbursed successfully' });
      setActionOpen(false);
      fetchRequests();
    } catch (err: any) {
      toast({ title: 'Error', description: err.response?.data?.message || 'Failed to disburse', variant: 'destructive' });
    } finally {
      setProcessing(false);
    }
  };

  const handleAction = () => {
    if (actionType === 'approve') handleApprove();
    else if (actionType === 'reject') handleReject();
    else handleDisburse();
  };

  // ── What label/description to show in the action confirmation ─────────────

  const actionConfig = {
    approve: {
      title: 'Approve Top-Up Request',
      desc: selectedRequest
        ? `Approve top-up of ${formatCurrency(selectedRequest.requestedAmount)} for loan #${selectedRequest.loanNumber ?? selectedRequest.loan?.loanNumber}?`
        : '',
      buttonLabel: 'Approve',
      buttonClass: 'bg-green-600 hover:bg-green-700',
      roleNote: role === 'LOAN_OFFICER'
        ? 'This will forward the request to the Credit Committee.'
        : role === 'CREDIT_COMMITTEE'
        ? 'This will forward the request to the Treasurer.'
        : 'This will mark the request as Approved and ready for disbursement.',
    },
    reject: {
      title: 'Reject Top-Up Request',
      desc: 'Please provide a reason for rejection.',
      buttonLabel: 'Reject',
      buttonClass: 'bg-red-600 hover:bg-red-700',
      roleNote: role === 'CREDIT_COMMITTEE'
        ? 'The request will be sent back to the Loan Officer for review.'
        : role === 'TREASURER'
        ? 'The request will be sent back to the Credit Committee.'
        : 'The request will be permanently rejected.',
    },
    disburse: {
      title: 'Disburse Top-Up',
      desc: selectedRequest
        ? `Disburse ${formatCurrency(selectedRequest.requestedAmount)} to loan #${selectedRequest.loanNumber ?? selectedRequest.loan?.loanNumber}? This will update the outstanding balance and freeze guarantor savings.`
        : '',
      buttonLabel: 'Disburse',
      buttonClass: 'bg-purple-600 hover:bg-purple-700',
      roleNote: 'This action cannot be undone. The funds will be added to the loan immediately.',
    },
  };

  const cfg = actionConfig[actionType];

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ArrowUp className="h-5 w-5 text-purple-600" />
            {sectionTitle}
            {requests.length > 0 && (
              <Badge className="bg-purple-100 text-purple-800 ml-2">{requests.length}</Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
            </div>
          ) : requests.length === 0 ? (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>No pending top-up requests in your queue.</AlertDescription>
            </Alert>
          ) : (
            <div className="space-y-4">
              {requests.map((req) => (
                <Card key={req.id} className="border-purple-200">
                  <CardContent className="pt-5">
                    <div className="space-y-3">
                      {/* Header row */}
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1">
                          <div className="flex flex-wrap items-center gap-2 mb-2">
                            <Badge className={STATUS_COLORS[req.status] ?? 'bg-gray-100 text-gray-800'}>
                              {STATUS_LABEL[req.status] ?? req.status.replace(/_/g, ' ')}
                            </Badge>
                            <span className="text-xs text-muted-foreground">
                              Requested: {formatDate(req.requestedDate)}
                            </span>
                          </div>

                          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
                            <div>
                              <p className="text-xs text-muted-foreground">Loan</p>
                              <p className="font-semibold text-sm">
                                #{req.loanNumber ?? req.loan?.loanNumber ?? 'N/A'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Member</p>
                              <p className="font-medium text-sm">
                                {req.member?.firstName} {req.member?.lastName}
                              </p>
                              <p className="text-xs text-muted-foreground">{req.member?.memberNumber}</p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Top-Up Amount</p>
                              <p className="font-semibold text-purple-700">
                                {formatCurrency(req.requestedAmount)}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-muted-foreground">Guarantors</p>
                              <p className="font-medium text-sm">
                                {approvedGuarantors(req)}/{(req.guarantors || []).length} approved
                              </p>
                            </div>
                          </div>

                          {/* Balance summary */}
                          <div className="bg-blue-50 border border-blue-200 rounded p-3 space-y-1 text-sm">
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Current Outstanding:</span>
                              <span className="font-medium">
                                {formatCurrency(req.loan?.outstandingBalance ?? 0)}
                              </span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Top-Up Amount:</span>
                              <span className="font-medium text-purple-700">
                                +{formatCurrency(req.requestedAmount)}
                              </span>
                            </div>
                            <div className="flex justify-between border-t pt-1">
                              <span className="font-semibold">New Outstanding After Top-Up:</span>
                              <span className="font-bold">
                                {formatCurrency(
                                  (req.loan?.outstandingBalance ?? 0) + (req.requestedAmount ?? 0)
                                )}
                              </span>
                            </div>
                          </div>

                          {/* Purpose */}
                          {req.purpose && (
                            <p className="text-xs text-muted-foreground mt-2">
                              <span className="font-medium">Purpose:</span> {req.purpose}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Action buttons */}
                      <div className="flex flex-wrap gap-2 pt-2 border-t">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => { setSelectedRequest(req); setDetailsOpen(true); }}
                          className="gap-1"
                        >
                          <Eye className="h-4 w-4" />
                          View Details
                        </Button>

                        {/* Approve / Reject — only when this role can act */}
                        {canAct(req) && req.status !== 'APPROVED' && (
                          <>
                            <Button
                              size="sm"
                              onClick={() => openApprove(req)}
                              className="gap-1 bg-green-600 hover:bg-green-700"
                            >
                              <CheckCircle className="h-4 w-4" />
                              {role === 'LOAN_OFFICER'
                                ? 'Approve → CC'
                                : role === 'CREDIT_COMMITTEE'
                                ? 'Approve → Treasurer'
                                : 'Approve'}
                            </Button>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => openReject(req)}
                              className="gap-1"
                            >
                              <XCircle className="h-4 w-4" />
                              {role === 'CREDIT_COMMITTEE'
                                ? 'Return to LO'
                                : role === 'TREASURER'
                                ? 'Return to CC'
                                : 'Reject'}
                            </Button>
                          </>
                        )}

                        {/* Disburse — Treasurer only, when status = APPROVED */}
                        {canDisburse(req) && (
                          <Button
                            size="sm"
                            onClick={() => openDisburse(req)}
                            className="gap-1 bg-purple-600 hover:bg-purple-700"
                          >
                            <DollarSign className="h-4 w-4" />
                            Disburse Top-Up
                          </Button>
                        )}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Details modal ─────────────────────────────────────────────────── */}
      {selectedRequest && (
        <Dialog open={detailsOpen} onOpenChange={setDetailsOpen}>
          <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Top-Up Request #{selectedRequest.id}</DialogTitle>
              <DialogDescription>
                Loan #{selectedRequest.loanNumber ?? selectedRequest.loan?.loanNumber}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-5">
              {/* Member */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="font-semibold mb-3 text-sm">Member Information</h3>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-muted-foreground text-xs">Name</p>
                    <p className="font-medium">
                      {selectedRequest.member?.firstName} {selectedRequest.member?.lastName}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs">Member Number</p>
                    <p className="font-medium">{selectedRequest.member?.memberNumber}</p>
                  </div>
                </div>
              </div>

              {/* Loan & amounts */}
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <h3 className="font-semibold mb-3 text-sm">Loan & Top-Up Details</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Loan Number</span>
                    <span className="font-medium">
                      #{selectedRequest.loanNumber ?? selectedRequest.loan?.loanNumber}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Current Outstanding</span>
                    <span className="font-medium">
                      {formatCurrency(selectedRequest.loan?.outstandingBalance ?? 0)}
                    </span>
                  </div>
                  <div className="flex justify-between border-t pt-2">
                    <span className="text-muted-foreground">Top-Up Amount</span>
                    <span className="font-medium text-purple-700">
                      {formatCurrency(selectedRequest.requestedAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-semibold">New Outstanding</span>
                    <span className="font-bold text-lg">
                      {formatCurrency(
                        (selectedRequest.loan?.outstandingBalance ?? 0) +
                          (selectedRequest.requestedAmount ?? 0)
                      )}
                    </span>
                  </div>
                </div>
              </div>

              {/* Purpose */}
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold mb-1 text-sm">Purpose</h3>
                <p className="text-sm text-muted-foreground">{selectedRequest.purpose || 'N/A'}</p>
              </div>

              {/* Guarantors */}
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <h3 className="font-semibold mb-3 text-sm">
                  Guarantors ({(selectedRequest.guarantors || []).length})
                </h3>
                <div className="space-y-2">
                  {(selectedRequest.guarantors || []).map((g) => (
                    <div
                      key={g.id}
                      className="flex items-center justify-between p-2 bg-white rounded border text-sm"
                    >
                      <div>
                        <p className="font-medium">
                          {g.member?.firstName} {g.member?.lastName}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {g.member?.memberNumber} — Guarantee: {formatCurrency(g.guaranteeAmount)}
                        </p>
                      </div>
                      <Badge
                        className={
                          g.status === 'APPROVED'
                            ? 'bg-green-100 text-green-800'
                            : g.status === 'REJECTED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-yellow-100 text-yellow-800'
                        }
                      >
                        {g.status}
                      </Badge>
                    </div>
                  ))}
                </div>
              </div>

              {/* Rejection reason if any */}
              {selectedRequest.rejectionReason && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <h3 className="font-semibold mb-1 text-sm text-red-800">Rejection Reason</h3>
                  <p className="text-sm text-red-700">{selectedRequest.rejectionReason}</p>
                </div>
              )}
            </div>
          </DialogContent>
        </Dialog>
      )}

      {/* ── Approve / Reject / Disburse confirmation modal ────────────────── */}
      {selectedRequest && (
        <Dialog open={actionOpen} onOpenChange={setActionOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{cfg.title}</DialogTitle>
              <DialogDescription>{cfg.desc}</DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <Alert className="bg-blue-50 border-blue-200">
                <AlertCircle className="h-4 w-4 text-blue-600" />
                <AlertDescription className="text-blue-800 text-sm">{cfg.roleNote}</AlertDescription>
              </Alert>

              {actionType === 'reject' && (
                <div>
                  <Label htmlFor="reason">Reason *</Label>
                  <Textarea
                    id="reason"
                    placeholder="Explain why this top-up request is being rejected..."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={4}
                    className="mt-1"
                  />
                </div>
              )}

              {actionType === 'approve' && (
                <div>
                  <Label htmlFor="comments">Comments (optional)</Label>
                  <Textarea
                    id="comments"
                    placeholder="Add any notes for the next reviewer..."
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={3}
                    className="mt-1"
                  />
                </div>
              )}
            </div>

            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={() => setActionOpen(false)} disabled={processing}>
                Cancel
              </Button>
              <Button
                onClick={handleAction}
                disabled={processing || (actionType === 'reject' && !reason.trim())}
                className={cfg.buttonClass}
              >
                {processing ? 'Processing...' : cfg.buttonLabel}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}
