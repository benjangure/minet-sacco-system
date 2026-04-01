import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import GuarantorApprovalModal from './GuarantorApprovalModal';

interface GuarantorRequest {
  id: number;
  loan: any;
  member: any;
  status: string;
}

interface GuarantorApprovalDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onApprovalChange?: () => void;
}

export default function GuarantorApprovalDialog({
  open,
  onOpenChange,
  onApprovalChange
}: GuarantorApprovalDialogProps) {
  const [requests, setRequests] = useState<GuarantorRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<GuarantorRequest | null>(null);
  const [approvalModalOpen, setApprovalModalOpen] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (open) {
      fetchPendingRequests();
    }
  }, [open]);

  const fetchPendingRequests = async () => {
    // Get token from localStorage (member portal uses localStorage, not AuthContext)
    const token = localStorage.getItem('token');
    
    if (!token) {
      console.error('No token available');
      toast({
        title: 'Error',
        description: 'Authentication token not found',
        variant: 'destructive'
      });
      return;
    }

    setLoading(true);
    try {
      const response = await api.get(
        '/loans/member/guarantor-requests'
      );
      setRequests(response.data.data || []);
    } catch (err: any) {
      console.error('Error fetching guarantor requests:', err);
      toast({
        title: 'Error',
        description: 'Failed to load guarantor requests',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRequestClick = (request: GuarantorRequest) => {
    setSelectedRequest(request);
    setApprovalModalOpen(true);
  };

  const handleApprovalSuccess = () => {
    setApprovalModalOpen(false);
    setSelectedRequest(null);
    fetchPendingRequests();
    onApprovalChange?.();
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Guarantor Requests</DialogTitle>
            <DialogDescription>
              Review and respond to pending guarantor requests
            </DialogDescription>
          </DialogHeader>

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
            </div>
          ) : requests.length === 0 ? (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                You have no pending guarantor requests at this time.
              </AlertDescription>
            </Alert>
          ) : (
            <div className="space-y-4">
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  You have {requests.length} pending guarantor request{requests.length !== 1 ? 's' : ''}.
                </AlertDescription>
              </Alert>

              <div className="space-y-3">
                {requests.map((request) => (
                  <Card
                    key={request.id}
                    className="cursor-pointer hover:shadow-md transition-shadow"
                    onClick={() => handleRequestClick(request)}
                  >
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <p className="font-semibold">
                            Loan #{request.loan?.loanNumber || 'N/A'}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            Member: {request.loan?.member?.memberNumber} - {request.loan?.member?.firstName} {request.loan?.member?.lastName}
                          </p>
                          <p className="text-sm text-muted-foreground">
                            Amount: KES {request.loan?.amount?.toLocaleString()}
                          </p>
                        </div>
                        <div className="text-right">
                          <div className="inline-flex items-center gap-2 px-3 py-1 bg-yellow-50 text-yellow-700 rounded-full text-sm font-medium">
                            <AlertCircle className="h-4 w-4" />
                            Pending
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              <Button
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="w-full"
              >
                Close
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {selectedRequest && (
        <GuarantorApprovalModal
          open={approvalModalOpen}
          onOpenChange={setApprovalModalOpen}
          guarantor={selectedRequest}
          loan={selectedRequest.loan}
          onSuccess={handleApprovalSuccess}
        />
      )}
    </>
  );
}
