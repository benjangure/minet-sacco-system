import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';

interface GuarantorDetail {
  guarantorId: number;
  memberId: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
  status: string;
  guaranteeAmount: number;
  frozenPledge: number;
  selfGuarantee: boolean;
  createdAt: string;
  approvedAt?: string;
}

interface GuarantorDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  guarantors: GuarantorDetail[];
  loanAmount: number;
}

export default function GuarantorDetailsModal({
  isOpen,
  onClose,
  guarantors,
  loanAmount
}: GuarantorDetailsModalProps) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'ACCEPTED':
        return 'bg-blue-100 text-blue-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'RELEASED':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const totalGuaranteed = guarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0);
  const totalFrozen = guarantors.reduce((sum, g) => sum + g.frozenPledge, 0);

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Loan Guarantors</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Summary */}
          <div className="bg-blue-50 p-4 rounded-lg border border-blue-200 space-y-2">
            <p className="text-sm font-semibold">Guarantee Summary</p>
            <div className="flex justify-between text-sm">
              <span>Loan Amount:</span>
              <span className="font-medium">{formatCurrency(loanAmount)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Total Guaranteed:</span>
              <span className="font-medium">{formatCurrency(totalGuaranteed)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Total Frozen Pledges:</span>
              <span className="font-medium">{formatCurrency(totalFrozen)}</span>
            </div>
          </div>

          {/* Guarantors List */}
          <div className="space-y-3">
            <p className="text-sm font-semibold">Guarantors ({guarantors.length})</p>
            {guarantors.map((guarantor) => (
              <div key={guarantor.guarantorId} className="border rounded-lg p-4 space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-semibold">
                      {guarantor.firstName} {guarantor.lastName}
                      {guarantor.selfGuarantee && (
                        <Badge className="ml-2 bg-purple-100 text-purple-800">Self-Guarantee</Badge>
                      )}
                    </p>
                    <p className="text-sm text-gray-600">Member: {guarantor.memberNumber}</p>
                  </div>
                  <Badge className={getStatusColor(guarantor.status)}>
                    {guarantor.status}
                  </Badge>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-600">Guarantee Amount</p>
                    <p className="font-semibold">{formatCurrency(guarantor.guaranteeAmount)}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">Frozen Pledge</p>
                    <p className="font-semibold">{formatCurrency(guarantor.frozenPledge)}</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm border-t pt-3">
                  <div>
                    <p className="text-gray-600">Created</p>
                    <p className="text-xs">{new Date(guarantor.createdAt).toLocaleDateString()}</p>
                  </div>
                  {guarantor.approvedAt && (
                    <div>
                      <p className="text-gray-600">Approved</p>
                      <p className="text-xs">{new Date(guarantor.approvedAt).toLocaleDateString()}</p>
                    </div>
                  )}
                </div>

                {/* Guarantee Ratio */}
                <div className="bg-gray-50 p-2 rounded text-xs">
                  <p className="text-gray-600">Guarantee Ratio: {((guarantor.guaranteeAmount / loanAmount) * 100).toFixed(1)}%</p>
                </div>
              </div>
            ))}
          </div>

          {guarantors.length === 0 && (
            <div className="text-center py-8 text-gray-500">
              <p>No guarantors assigned to this loan</p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
