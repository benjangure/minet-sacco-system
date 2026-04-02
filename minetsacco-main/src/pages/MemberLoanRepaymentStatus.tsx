import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, Upload, ArrowLeft } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { useNavigate, useParams } from 'react-router-dom';

interface RejectionDetails {
  requestId: number;
  loanId: number;
  loanNumber: string;
  requestedAmount: number;
  rejectionReason: string;
  rejectedBy: string;
  rejectedAt: string;
  outstandingBalance: number;
  canResubmit: boolean;
}

export default function MemberLoanRepaymentStatus() {
  const { requestId } = useParams<{ requestId: string }>();
  const navigate = useNavigate();
  const [details, setDetails] = useState<RejectionDetails | null>(null);
  const [loading, setLoading] = useState(false);
  const [resubmitting, setResubmitting] = useState(false);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [proofFile, setProofFile] = useState<File | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    if (requestId) {
      fetchRejectionDetails();
    }
  }, [requestId]);

  const fetchRejectionDetails = async () => {
    setLoading(true);
    try {
      const response = await api.get(`/member/loan-repayment-requests/${requestId}/rejection-details`);
      setDetails(response.data);
      setAmount(response.data.requestedAmount.toString());
    } catch (err: any) {
      console.error('Error fetching rejection details:', err);
      toast({
        title: 'Error',
        description: 'Failed to load rejection details',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleResubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!amount || !proofFile) {
      toast({
        title: 'Error',
        description: 'Please fill in all required fields',
        variant: 'destructive'
      });
      return;
    }

    const repaymentAmount = parseFloat(amount);
    if (repaymentAmount <= 0) {
      toast({
        title: 'Error',
        description: 'Amount must be greater than zero',
        variant: 'destructive'
      });
      return;
    }

    if (details && repaymentAmount > details.outstandingBalance) {
      toast({
        title: 'Error',
        description: `Amount cannot exceed outstanding balance of KES ${details.outstandingBalance.toLocaleString()}`,
        variant: 'destructive'
      });
      return;
    }

    setResubmitting(true);
    try {
      const formData = new FormData();
      formData.append('amount', repaymentAmount.toString());
      formData.append('description', description || 'Loan repayment (resubmitted)');
      formData.append('proofFile', proofFile);

      await api.post(
        `/member/loan-repayment-requests/${requestId}/resubmit`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );

      toast({
        title: 'Success',
        description: 'Repayment request resubmitted successfully. Teller will review shortly.'
      });

      // Navigate back to loan balances
      setTimeout(() => navigate('/member/loan-balances'), 2000);
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to resubmit request',
        variant: 'destructive'
      });
    } finally {
      setResubmitting(false);
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

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!details) {
    return (
      <div className="space-y-6">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Go Back
        </Button>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground">Rejection details not found</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="outline" size="icon" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">Repayment Request Rejected</h1>
          <p className="text-muted-foreground mt-2">
            Your bank transfer repayment request was not approved. Please review the reason and resubmit.
          </p>
        </div>
      </div>

      {/* Rejection Details */}
      <Card className="border-red-200 bg-red-50">
        <CardHeader>
          <CardTitle className="text-red-900">Rejection Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Loan</p>
              <p className="font-semibold">{details.loanNumber}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Outstanding Balance</p>
              <p className="font-semibold">{formatCurrency(details.outstandingBalance)}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Your Requested Amount</p>
              <p className="font-semibold">{formatCurrency(details.requestedAmount)}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Rejected On</p>
              <p className="font-semibold">{formatDate(details.rejectedAt)}</p>
            </div>
          </div>

          <div className="border-t pt-4">
            <p className="text-sm text-muted-foreground mb-2">Rejection Reason</p>
            <div className="bg-white p-3 rounded border border-red-200">
              <p className="text-red-900 font-medium">{details.rejectionReason}</p>
            </div>
            <p className="text-xs text-muted-foreground mt-2">
              Rejected by: {details.rejectedBy}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Resubmission Form */}
      {details.canResubmit && (
        <Card>
          <CardHeader>
            <CardTitle>Resubmit Repayment Request</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleResubmit} className="space-y-6">
              {/* Amount */}
              <div className="space-y-2">
                <Label htmlFor="amount">Repayment Amount (KES)</Label>
                <Input
                  id="amount"
                  type="number"
                  step="0.01"
                  min="0"
                  max={details.outstandingBalance}
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="Enter amount"
                />
                <p className="text-xs text-muted-foreground">
                  Maximum: {formatCurrency(details.outstandingBalance)}
                </p>
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">Description (Optional)</Label>
                <Input
                  id="description"
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g., Corrected bank transfer"
                />
              </div>

              {/* File Upload */}
              <div className="space-y-2">
                <Label htmlFor="proof">Upload Corrected Proof of Payment *</Label>
                <div className="border-2 border-dashed rounded-lg p-4 text-center cursor-pointer hover:bg-muted/50 transition"
                  onClick={() => document.getElementById('file-input')?.click()}>
                  <Upload className="h-6 w-6 mx-auto mb-2 text-muted-foreground" />
                  <p className="text-sm font-medium">
                    {proofFile ? proofFile.name : 'Click to upload or drag and drop'}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    PDF, JPG, PNG (Max 5MB)
                  </p>
                  <input
                    id="file-input"
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) {
                        if (file.size > 5 * 1024 * 1024) {
                          toast({
                            title: 'Error',
                            description: 'File size must be less than 5MB',
                            variant: 'destructive'
                          });
                        } else {
                          setProofFile(file);
                        }
                      }
                    }}
                    className="hidden"
                  />
                </div>
              </div>

              {/* Info Alert */}
              <Alert>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Please ensure your proof of payment is clear and matches the amount you're submitting. 
                  The teller will review and approve or reject your resubmission.
                </AlertDescription>
              </Alert>

              {/* Buttons */}
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate(-1)}
                  disabled={resubmitting}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={resubmitting || !proofFile}
                  className="flex-1"
                >
                  {resubmitting ? 'Resubmitting...' : 'Resubmit Request'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Tips */}
      <Card className="bg-blue-50 border-blue-200">
        <CardHeader>
          <CardTitle className="text-blue-900">Tips for Successful Resubmission</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-blue-900">
          <p>• Ensure the proof file is clear and legible</p>
          <p>• Verify the amount matches your bank transfer</p>
          <p>• Check that the recipient details are correct</p>
          <p>• Include transaction reference if available</p>
          <p>• Submit during business hours for faster processing</p>
        </CardContent>
      </Card>
    </div>
  );
}
