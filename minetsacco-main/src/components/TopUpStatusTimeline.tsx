import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  CheckCircle2, Circle, Clock, XCircle,
  DollarSign, FileCheck, Users, Shield, Building, CreditCard
} from 'lucide-react';
import { format } from 'date-fns';

interface TopUpStatusTimelineProps {
  currentStatus: string;
  requestedDate?: string;
  reviewDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
  loanNumber?: string;
  requestedAmount?: number;
  className?: string;
}

const statusColors: Record<string, string> = {
  PENDING_GUARANTOR_APPROVAL: 'bg-yellow-500',
  PENDING_LOAN_OFFICER_REVIEW: 'bg-orange-500',
  PENDING_CREDIT_COMMITTEE: 'bg-orange-500',
  PENDING_TREASURER: 'bg-orange-500',
  APPROVED: 'bg-blue-500',
  DISBURSED: 'bg-green-500',
  REJECTED: 'bg-red-500',
  CANCELLED: 'bg-gray-500',
  // legacy
  PENDING_REVIEW: 'bg-orange-500',
};

const STEPS = [
  {
    status: 'PENDING_GUARANTOR_APPROVAL',
    label: 'Guarantor Approval',
    description: 'Waiting for your selected guarantors to review and approve',
    icon: Shield,
    completedWhen: (s: string) =>
      ['PENDING_LOAN_OFFICER_REVIEW','PENDING_CREDIT_COMMITTEE','PENDING_TREASURER','APPROVED','DISBURSED'].includes(s),
    currentWhen: (s: string) => s === 'PENDING_GUARANTOR_APPROVAL',
  },
  {
    status: 'PENDING_LOAN_OFFICER_REVIEW',
    label: 'Loan Officer Review',
    description: 'Loan officer is reviewing your top-up request',
    icon: Users,
    completedWhen: (s: string) =>
      ['PENDING_CREDIT_COMMITTEE','PENDING_TREASURER','APPROVED','DISBURSED'].includes(s),
    currentWhen: (s: string) =>
      s === 'PENDING_LOAN_OFFICER_REVIEW' || s === 'PENDING_REVIEW', // legacy
  },
  {
    status: 'PENDING_CREDIT_COMMITTEE',
    label: 'Credit Committee Review',
    description: 'Credit committee is evaluating the top-up request',
    icon: Building,
    completedWhen: (s: string) => ['PENDING_TREASURER','APPROVED','DISBURSED'].includes(s),
    currentWhen: (s: string) => s === 'PENDING_CREDIT_COMMITTEE',
  },
  {
    status: 'PENDING_TREASURER',
    label: 'Treasury Approval',
    description: 'Treasurer is giving final approval for this top-up',
    icon: CheckCircle2,
    completedWhen: (s: string) => ['APPROVED','DISBURSED'].includes(s),
    currentWhen: (s: string) => s === 'PENDING_TREASURER',
  },
  {
    status: 'APPROVED',
    label: 'Top-Up Approved',
    description: 'Your top-up has been approved and is ready for disbursement',
    icon: FileCheck,
    completedWhen: (s: string) => s === 'DISBURSED',
    currentWhen: (s: string) => s === 'APPROVED',
  },
  {
    status: 'DISBURSED',
    label: 'Top-Up Disbursed',
    description: 'Funds have been added to your loan balance',
    icon: DollarSign,
    completedWhen: () => false,
    currentWhen: (s: string) => s === 'DISBURSED',
  },
];

const TopUpStatusTimeline: React.FC<TopUpStatusTimelineProps> = ({
  currentStatus,
  requestedDate,
  reviewDate,
  disbursementDate,
  rejectionReason,
  loanNumber,
  requestedAmount,
  className = '',
}) => {
  const fmt = (d?: string) => {
    if (!d) return undefined;
    try { return format(new Date(d), 'MMM dd, yyyy'); } catch { return undefined; }
  };

  const isRejected  = currentStatus === 'REJECTED';
  const isCancelled = currentStatus === 'CANCELLED';
  const isTerminal  = isRejected || isCancelled;

  const getStepState = (step: typeof STEPS[0]) => {
    if (isTerminal) return 'muted';
    if (step.currentWhen(currentStatus)) return 'current';
    if (step.completedWhen(currentStatus)) return 'completed';
    return 'pending';
  };

  const formatCurrency = (n?: number) =>
    n != null
      ? new Intl.NumberFormat('en-KE', { style: 'currency', currency: 'KES' }).format(n)
      : '';

  return (
    <Card className={`w-full ${className}`}>
      <CardContent className="p-6">
        {/* Header */}
        <div className="mb-4">
          <h3 className="text-lg font-semibold">Top-Up Request Status</h3>
          <div className="flex flex-wrap items-center gap-2 mt-2">
            <Badge className={statusColors[currentStatus] ?? 'bg-gray-500'}>
              {currentStatus.replace(/_/g, ' ')}
            </Badge>
            {loanNumber && (
              <span className="text-sm text-muted-foreground">Loan #{loanNumber}</span>
            )}
            {requestedAmount != null && (
              <span className="text-sm font-medium text-purple-700">{formatCurrency(requestedAmount)}</span>
            )}
            {requestedDate && (
              <span className="text-sm text-muted-foreground">
                Requested {fmt(requestedDate)}
              </span>
            )}
          </div>
        </div>

        {/* Rejection / Cancellation banner */}
        {isRejected && (
          <div className="mb-5 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
            <XCircle className="w-5 h-5 text-red-600 mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold text-red-800">Top-Up Request Rejected</p>
              {rejectionReason && (
                <p className="text-sm text-red-700 mt-1">{rejectionReason}</p>
              )}
            </div>
          </div>
        )}
        {isCancelled && (
          <div className="mb-5 p-4 bg-gray-50 border border-gray-200 rounded-lg flex items-start gap-3">
            <XCircle className="w-5 h-5 text-gray-500 mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold text-gray-700">Top-Up Request Cancelled</p>
              {rejectionReason && (
                <p className="text-sm text-gray-600 mt-1">{rejectionReason}</p>
              )}
            </div>
          </div>
        )}

        {/* Timeline steps */}
        <div className="space-y-4">
          {STEPS.map((step) => {
            const state = getStepState(step);
            const Icon = step.icon;
            const isActive = state === 'completed' || state === 'current';

            return (
              <div key={step.status} className="flex items-start gap-4">
                <div className="shrink-0 mt-0.5">
                  {state === 'completed' ? (
                    <CheckCircle2 className="w-5 h-5 text-green-600" />
                  ) : state === 'current' ? (
                    <Clock className="w-5 h-5 text-blue-600 animate-pulse" />
                  ) : (
                    <Circle className="w-5 h-5 text-gray-300" />
                  )}
                </div>
                <div className={`flex-1 ${isActive ? 'opacity-100' : 'opacity-40'}`}>
                  <p className={`font-medium text-sm ${state === 'current' ? 'text-blue-700' : ''}`}>
                    {step.label}
                  </p>
                  <p className="text-xs text-muted-foreground mt-0.5">{step.description}</p>
                  {/* Show relevant dates */}
                  {step.status === 'PENDING_GUARANTOR_APPROVAL' && requestedDate && state === 'completed' && (
                    <p className="text-xs text-green-600 mt-0.5">Submitted {fmt(requestedDate)}</p>
                  )}
                  {step.status === 'APPROVED' && reviewDate && state !== 'pending' && (
                    <p className="text-xs text-muted-foreground mt-0.5">{fmt(reviewDate)}</p>
                  )}
                  {step.status === 'DISBURSED' && disbursementDate && (
                    <p className="text-xs text-muted-foreground mt-0.5">{fmt(disbursementDate)}</p>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Disbursed celebration banner */}
        {currentStatus === 'DISBURSED' && (
          <div className="mt-5 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
            <DollarSign className="w-5 h-5 text-green-600 mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold text-green-800">Top-Up Disbursed Successfully</p>
              <p className="text-sm text-green-700 mt-1">
                {requestedAmount != null && `${formatCurrency(requestedAmount)} has been `}
                Added to your loan balance.
                {disbursementDate && ` Disbursed on ${fmt(disbursementDate)}.`}
              </p>
            </div>
          </div>
        )}

        {/* Approved — waiting for disbursal */}
        {currentStatus === 'APPROVED' && (
          <div className="mt-5 p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-3">
            <CreditCard className="w-5 h-5 text-blue-600 mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold text-blue-800">Approved — Awaiting Disbursement</p>
              <p className="text-sm text-blue-700 mt-1">
                Your top-up has been approved. The Treasurer will disburse the funds shortly.
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default TopUpStatusTimeline;
