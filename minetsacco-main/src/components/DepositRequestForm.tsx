import { useState, useEffect } from 'react';
import axios from 'axios';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Upload, AlertCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';

interface Account {
  id: number;
  accountType: string;
  balance: number;
}

interface DepositRequestFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export default function DepositRequestForm({ open, onOpenChange, onSuccess }: DepositRequestFormProps) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [claimedAmount, setClaimedAmount] = useState('');
  const [description, setDescription] = useState('');
  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (open) {
      fetchAccounts();
    }
  }, [open]);

  const fetchAccounts = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_BASE_URL}/member/accounts`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      // Filter out SHARES account - this SACCO does not accept share deposits
      const filteredAccounts = (response.data || []).filter((a: Account) => a.accountType !== 'SHARES');
      setAccounts(filteredAccounts);
    } catch (err) {
      console.error('Error fetching accounts:', err);
      toast({ title: 'Error', description: 'Failed to load accounts', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf', 'text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
      if (!allowedTypes.includes(file.type)) {
        toast({ title: 'Error', description: 'Only images (JPG, PNG), PDF, TXT, and Word documents are allowed', variant: 'destructive' });
        return;
      }
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast({ title: 'Error', description: 'File size must be less than 5MB', variant: 'destructive' });
        return;
      }
      setReceiptFile(file);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedAccount || !claimedAmount || !description || !receiptFile) {
      toast({ title: 'Error', description: 'Please fill all fields and upload a receipt', variant: 'destructive' });
      return;
    }

    if (parseFloat(claimedAmount) <= 0) {
      toast({ title: 'Error', description: 'Amount must be greater than zero', variant: 'destructive' });
      return;
    }

    setSubmitting(true);
    try {
      const token = localStorage.getItem('token');
      
      // Create FormData to send file
      const formData = new FormData();
      formData.append('accountId', selectedAccount);
      formData.append('claimedAmount', claimedAmount);
      formData.append('description', description);
      formData.append('receiptFile', receiptFile);
      
      await axios.post(
        `${API_BASE_URL}/member/deposit-requests`,
        formData,
        {
          headers: { 
            Authorization: `Bearer ${token}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      toast({ title: 'Success', description: 'Deposit request submitted successfully. Teller will review shortly.' });
      setSelectedAccount('');
      setClaimedAmount('');
      setDescription('');
      setReceiptFile(null);
      onOpenChange(false);
      onSuccess?.();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to submit deposit request',
        variant: 'destructive'
      });
    } finally {
      setSubmitting(false);
    }
  };

  const selectedAccountData = accounts.find(a => a.id === parseInt(selectedAccount));

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Submit Deposit Request</DialogTitle>
          <DialogDescription>
            Upload a receipt of your payment for verification by the teller
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Upload a receipt (image or document) of your payment. The teller will verify and confirm the amount.
            </AlertDescription>
          </Alert>

          <div>
            <Label htmlFor="account">Select Account</Label>
            <Select value={selectedAccount} onValueChange={setSelectedAccount}>
              <SelectTrigger>
                <SelectValue placeholder="Select account" />
              </SelectTrigger>
              <SelectContent>
                {accounts.map((account) => (
                  <SelectItem key={account.id} value={account.id.toString()}>
                    {account.accountType} - Balance: {formatCurrency(account.balance)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label htmlFor="amount">Claimed Amount (KES)</Label>
            <Input
              id="amount"
              type="number"
              placeholder="Enter amount you paid"
              value={claimedAmount}
              onChange={(e) => setClaimedAmount(e.target.value)}
              min="0"
              step="100"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Enter the amount shown on your receipt. Teller will verify and confirm.
            </p>
          </div>

          <div>
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              placeholder="e.g., Bank deposit on 25th March, Reference: ABC123"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="min-h-20"
            />
          </div>

          <div>
            <Label htmlFor="receipt">Upload Receipt</Label>
            <div className="border-2 border-dashed rounded-lg p-6 text-center cursor-pointer hover:bg-muted/50 transition">
              <input
                id="receipt"
                type="file"
                onChange={handleFileChange}
                accept="image/*,.pdf,.txt,.doc,.docx"
                className="hidden"
              />
              <label htmlFor="receipt" className="cursor-pointer">
                <Upload className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                <p className="text-sm font-medium">Click to upload receipt</p>
                <p className="text-xs text-muted-foreground">JPG, PNG, PDF, TXT, or Word (max 5MB)</p>
              </label>
            </div>
            {receiptFile && (
              <Card className="mt-3 bg-green-50">
                <CardContent className="pt-4">
                  <p className="text-sm font-medium text-green-800">✓ {receiptFile.name}</p>
                  <p className="text-xs text-green-700">{(receiptFile.size / 1024).toFixed(2)} KB</p>
                </CardContent>
              </Card>
            )}
          </div>

          {selectedAccountData && (
            <Card className="bg-blue-50">
              <CardContent className="pt-4">
                <p className="text-sm"><span className="font-semibold">Account:</span> {selectedAccountData.accountType}</p>
                <p className="text-sm"><span className="font-semibold">Current Balance:</span> {formatCurrency(selectedAccountData.balance)}</p>
              </CardContent>
            </Card>
          )}

          <div className="flex gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={submitting || !selectedAccount || !claimedAmount || !description || !receiptFile}
              className="flex-1"
            >
              {submitting ? 'Submitting...' : 'Submit Request'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
