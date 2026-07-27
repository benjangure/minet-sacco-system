import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { CheckCircle2, Circle, Clock, XCircle, DollarSign, FileCheck, Users, Shield, Building } from 'lucide-react';
import { format } from 'date-fns';

interface LoanStatusTimelineProps {
  currentStatus: string;
  applicationDate?: string;
  approvalDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
  className?: string;
}

interface TimelineStep {
  status: string;
  label: string;
  description: string;
  icon: React.ComponentType<any>;
  getDate?: () => string | undefined;
  isCompleted: (current: string) => boolean;
  isCurrent: (current: string) => boolean;
  isRejected: (current: string) => boolean;
}

const LoanStatusTimeline: React.FC<LoanStatusTimelineProps> = ({
  currentStatus,
  applicationDate,
  approvalDate,
  disbursementDate,
  rejectionReason,
  className = ""
}) => {
  const formatDate = (dateString?: string) => {
    if (!dateString) return undefined;
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch {
      return undefined;
    }
  };

  const timelineSteps: TimelineStep[] = [
    {
      status: 'PENDING',
      label: 'Application Submitted',
      description: 'Your loan application has been received and is under initial review',
      icon: FileCheck,
      getDate: () => formatDate(applicationDate),
      isCompleted: (current) => current !== 'PENDING',
      isCurrent: (current) => current === 'PENDING',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'PENDING_GUARANTOR_APPROVAL',
      label: 'Guarantor Approval',
      description: 'Waiting for guarantors to review and approve the guarantee request',
      icon: Shield,
      getDate: () => undefined,
      isCompleted: (current) => ['PENDING_LOAN_OFFICER_REVIEW', 'PENDING_CREDIT_COMMITTEE', 'PENDING_TREASURER', 'APPROVED', 'DISBURSED', 'REPAID'].includes(current),
      isCurrent: (current) => current === 'PENDING_GUARANTOR_APPROVAL',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'PENDING_LOAN_OFFICER_REVIEW',
      label: 'Loan Officer Review',
      description: 'Loan officer is reviewing your application and documents',
      icon: Users,
      getDate: () => undefined,
      isCompleted: (current) => ['PENDING_CREDIT_COMMITTEE', 'PENDING_TREASURER', 'APPROVED', 'DISBURSED', 'REPAID'].includes(current),
      isCurrent: (current) => current === 'PENDING_LOAN_OFFICER_REVIEW',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'PENDING_CREDIT_COMMITTEE',
      label: 'Credit Committee Review',
      description: 'Credit committee is evaluating the loan application for approval',
      icon: Building,
      getDate: () => undefined,
      isCompleted: (current) => ['PENDING_TREASURER', 'APPROVED', 'DISBURSED', 'REPAID'].includes(current),
      isCurrent: (current) => current === 'PENDING_CREDIT_COMMITTEE',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'PENDING_TREASURER',
      label: 'Treasury Approval',
      description: 'Treasurer is reviewing the approved loan for final disbursement approval',
      icon: CheckCircle2,
      getDate: () => undefined,
      isCompleted: (current) => ['APPROVED', 'DISBURSED', 'REPAID'].includes(current),
      isCurrent: (current) => current === 'PENDING_TREASURER',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'APPROVED',
      label: 'Loan Approved',
      description: 'Your loan has been approved and is ready for disbursement',
      icon: FileCheck,
      getDate: () => formatDate(approvalDate),
      isCompleted: (current) => ['DISBURSED', 'REPAID'].includes(current),
      isCurrent: (current) => current === 'APPROVED',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'DISBURSED',
      label: 'Loan Disbursed',
      description: 'Loan amount has been transferred to your bank account',
      icon: DollarSign,
      getDate: () => formatDate(disbursementDate),
      isCompleted: (current) => current === 'REPAID',
      isCurrent: (current) => current === 'DISBURSED',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'REJECTED',
      label: 'Application Rejected',
      description: rejectionReason || 'Your loan application was not approved',
      icon: XCircle,
      getDate: () => undefined,
      isCompleted: () => false,
      isCurrent: (current) => current === 'REJECTED',
      isRejected: (current) => current === 'REJECTED'
    },
    {
      status: 'REPAID',
      label: 'Loan Fully Repaid',
      description: 'Congratulations! Your loan has been fully repaid',
      icon: CheckCircle2,
      getDate: () => undefined,
      isCompleted: () => true,
      isCurrent: (current) => current === 'REPAID',
      isRejected: (current) => current === 'DEFAULTED'
    }
  ];

  const getStepStatus = (step: TimelineStep) => {
    if (step.isRejected(currentStatus)) return 'rejected';
    if (step.isCurrent(currentStatus)) return 'current';
    if (step.isCompleted(currentStatus)) return 'completed';
    return 'pending';
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DISBURSED':
      case 'REPAID':
        return 'bg-green-500';
      case 'APPROVED':
        return 'bg-blue-500';
      case 'PENDING_GUARANTOR_APPROVAL':
      case 'PENDING_LOAN_OFFICER_REVIEW':
      case 'PENDING_CREDIT_COMMITTEE':
      case 'PENDING_TREASURER':
      case 'PENDING':
        return 'bg-orange-500';
      case 'REJECTED':
        return 'bg-red-500';
      case 'DEFAULTED':
        return 'bg-red-600';
      default:
        return 'bg-gray-500';
    }
  };

  const getTimelineIcon = (step: TimelineStep) => {
    const stepStatus = getStepStatus(step);
    const Icon = step.icon;
    
    if (stepStatus === 'completed') {
      return <CheckCircle2 className="w-5 h-5 text-green-600" />;
    } else if (stepStatus === 'current') {
      return <Clock className="w-5 h-5 text-blue-600 animate-pulse" />;
    } else if (stepStatus === 'rejected') {
      return <XCircle className="w-5 h-5 text-red-600" />;
    } else {
      return <Circle className="w-5 h-5 text-gray-400" />;
    }
  };

  const currentStep = timelineSteps.find(step => step.isCurrent(currentStatus));
  const isRejected = currentStatus === 'REJECTED' || currentStatus === 'DEFAULTED';

  return (
    <Card className={`w-full ${className}`}>
      <CardContent className="p-6">
        <div className="mb-4">
          <h3 className="text-lg font-semibold">Loan Application Status</h3>
          <div className="flex items-center gap-2 mt-2">
            <Badge className={getStatusColor(currentStatus)}>
              {currentStatus.replace(/_/g, ' ')}
            </Badge>
            {currentStep?.getDate && (
              <span className="text-sm text-muted-foreground">
                {currentStep.getDate()}
              </span>
            )}
          </div>
        </div>

        {isRejected && rejectionReason && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-start gap-3">
              <XCircle className="w-5 h-5 text-red-600 mt-0.5" />
              <div>
                <h4 className="font-semibold text-red-800">Application Rejected</h4>
                <p className="text-sm text-red-700 mt-1">{rejectionReason}</p>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {timelineSteps
            .filter(step => !isRejected || step.status === 'REJECTED')
            .filter(step => step.status === 'REPAID' ? currentStatus === 'REPAID' : true) // Only show REPAID step when loan is actually repaid
            .map((step, index) => {
              const stepStatus = getStepStatus(step);
              const isActive = stepStatus === 'current' || stepStatus === 'completed';
              
              return (
                <div key={step.status} className="flex items-start gap-4">
                  <div className="flex-shrink-0 mt-1">
                    {getTimelineIcon(step)}
                  </div>
                  
                  <div className={`flex-1 ${isActive ? 'opacity-100' : 'opacity-50'}`}>
                    <div className="flex items-center justify-between">
                      <h4 className={`font-medium ${stepStatus === 'current' ? 'text-blue-600' : ''}`}>
                        {step.label}
                      </h4>
                      {step.getDate() && (
                        <span className="text-xs text-muted-foreground">
                          {step.getDate()}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">
                      {step.description}
                    </p>
                  </div>

                  {index < timelineSteps.length - 1 && (
                    <div className={`absolute left-6 mt-8 w-0.5 h-8 ${
                      isActive ? 'bg-blue-200' : 'bg-gray-200'
                    }`} />
                  )}
                </div>
              );
            })}
        </div>

        {currentStatus === 'DISBURSED' && (
          <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
            <div className="flex items-start gap-3">
              <DollarSign className="w-5 h-5 text-green-600 mt-0.5" />
              <div>
                <h4 className="font-semibold text-green-800">Loan Disbursed Successfully</h4>
                <p className="text-sm text-green-700 mt-1">
                  Your loan has been transferred to your bank account. Please check your bank balance.
                </p>
              </div>
            </div>
          </div>
        )}

        {currentStatus === 'APPROVED' && (
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-blue-600 mt-0.5" />
              <div>
                <h4 className="font-semibold text-blue-800">Loan Approved</h4>
                <p className="text-sm text-blue-700 mt-1">
                  Your loan has been approved and is being processed for disbursement. You will receive another notification when it's disbursed to your bank account.
                </p>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default LoanStatusTimeline;
