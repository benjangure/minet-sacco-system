import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Eye } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '@/hooks/use-toast';
import GuarantorDetailsModal from '@/components/GuarantorDetailsModal';

interface Guarantee {
  guarantorId: number;
  loanId: number;
  loanNumber: string;
  memberName: string;
  memberNumber: string;
  loanAmount: number;
  guaranteeAmount: number;
  frozenPledge: number;
  status: string;
  loanStatus: string;
  isSelfGuarantee: boolean;
}

export default function MyGuarantees() {
  const [guarantees, setGuarantees] = useState<Guarantee[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedLoanId, setSelectedLoanId] = useState<number | null>(null);
  const [selectedLoanGuarantors, setSelectedLoanGuarantors] = useState<any[]>([]);
  const [showModal, setShowModal] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    fetchGuarantees();
  }, []);

  const fetchGuarantees = async () => {
    try {
      setLoading(true);
      const response = await api.get('/loans/member/guarantees');
      setGuarantees(response.data.data || []);
    } catch (err: any) {
      console.error('Error fetching guarantees:', err);
      toast({
        title: 'Error',
        description: 'Failed to load your guarantees',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleViewGuarantors = async (loanId: number) => {
    try {
      const response = await api.get(`/loans/${loanId}/guarantors`);
      setSelectedLoanGuarantors(response.data.data || []);
      setSelectedLoanId(loanId);
      setShowModal(true);
    } catch (err: any) {
      console.error('Error fetching guarantors:', err);
      toast({
        title: 'Error',
        description: 'Failed to load guarantor details',
        variant: 'destructive'
      });
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DISBURSED':
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'PENDING':
      case 'PENDING_GUARANTOR_APPROVAL':
        return 'bg-yellow-100 text-yellow-800';
      case 'APPROVED':
        return 'bg-blue-100 text-blue-800';
      case 'REPAID':
        return 'bg-gray-100 text-gray-800';
      case 'DEFAULTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getTotalGuaranteeAmount = () => {
    return guarantees.reduce((sum, g) => sum + g.guaranteeAmount, 0);
  };

  const getTotalFrozenAmount = () => {
    return guarantees.reduce((sum, g) => sum + g.frozenPledge, 0);
  };

  const getActiveGuarantees = () => {
    return guarantees.filter(g => g.loanStatus === 'DISBURSED' || g.loanStatus === 'ACTIVE');
  };

  return (
    <div className="space-y-4 max-w-4xl mx-auto">
      <Button variant="ghost" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" />
        Back
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>My Guarantees</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8">
              <p className="text-gray-500">Loading your guarantees...</p>
            </div>
          ) : guarantees.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-500">You are not currently guaranteeing any loans</p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
                  <p className="text-sm text-gray-600">Total Guarantees</p>
                  <p className="text-2xl font-bold text-blue-600">{guarantees.length}</p>
                </div>
                <div className="bg-purple-50 p-4 rounded-lg border border-purple-200">
                  <p className="text-sm text-gray-600">Total Guarantee Amount</p>
                  <p className="text-2xl font-bold text-purple-600">{formatCurrency(getTotalGuaranteeAmount())}</p>
                </div>
                <div className="bg-red-50 p-4 rounded-lg border border-red-200">
                  <p className="text-sm text-gray-600">Total Frozen Pledges</p>
                  <p className="text-2xl font-bold text-red-600">{formatCurrency(getTotalFrozenAmount())}</p>
                </div>
              </div>

              {/* Active Guarantees Alert */}
              {getActiveGuarantees().length > 0 && (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
                  <p className="text-sm font-semibold text-yellow-800">
                    ⚠️ You have {getActiveGuarantees().length} active guarantee(s) with frozen pledges
                  </p>
                </div>
              )}

              {/* Guarantees Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-semibold">Borrower</th>
                      <th className="text-left py-3 px-4 font-semibold">Loan Amount</th>
                      <th className="text-left py-3 px-4 font-semibold">Guarantee Amount</th>
                      <th className="text-left py-3 px-4 font-semibold">Frozen Pledge</th>
                      <th className="text-left py-3 px-4 font-semibold">Loan Status</th>
                      <th className="text-left py-3 px-4 font-semibold">Type</th>
                      <th className="text-left py-3 px-4 font-semibold">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {guarantees.map((guarantee) => (
                      <tr key={guarantee.guarantorId} className="border-b hover:bg-gray-50">
                        <td className="py-3 px-4">
                          <div>
                            <p className="font-medium">{guarantee.memberName}</p>
                            <p className="text-xs text-gray-500">{guarantee.memberNumber}</p>
                          </div>
                        </td>
                        <td className="py-3 px-4 font-medium">{formatCurrency(guarantee.loanAmount)}</td>
                        <td className="py-3 px-4 font-medium">{formatCurrency(guarantee.guaranteeAmount)}</td>
                        <td className="py-3 px-4">
                          <span className={guarantee.frozenPledge > 0 ? 'text-red-600 font-medium' : 'text-gray-600'}>
                            {formatCurrency(guarantee.frozenPledge)}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <Badge className={getStatusColor(guarantee.loanStatus)}>
                            {guarantee.loanStatus}
                          </Badge>
                        </td>
                        <td className="py-3 px-4">
                          {guarantee.isSelfGuarantee ? (
                            <Badge className="bg-purple-100 text-purple-800">Self</Badge>
                          ) : (
                            <Badge className="bg-blue-100 text-blue-800">External</Badge>
                          )}
                        </td>
                        <td className="py-3 px-4">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewGuarantors(guarantee.loanId)}
                            className="gap-1"
                          >
                            <Eye className="h-4 w-4" />
                            View
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Information */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-4">
                <p className="text-sm text-blue-800">
                  <strong>Note:</strong> Your frozen pledges will be reduced proportionally as the borrower repays the loan. 
                  Once the loan is fully repaid, all pledges will be released and your eligibility will be restored.
                </p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Guarantor Details Modal */}
      {selectedLoanId && (
        <GuarantorDetailsModal
          isOpen={showModal}
          onClose={() => setShowModal(false)}
          guarantors={selectedLoanGuarantors}
          loanAmount={guarantees.find(g => g.loanId === selectedLoanId)?.loanAmount || 0}
        />
      )}
    </div>
  );
}
