import { useState, useEffect } from 'react';
import api from '@/config/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, Smartphone, CheckCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { API_BASE_URL } from '@/config/api';

interface MpesaTransactionProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  type: 'deposit' | 'withdraw';
  onSuccess?: () => void;
}

export default function MpesaTransaction({
  open,
  onOpenChange,
  type,
  onSuccess
}: MpesaTransactionProps) {
  const [amount, setAmount] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [step, setStep] = useState<'entry' | 'confirm' | 'success'>('entry');
  const [processing, setProcessing] = useState(false);
  const [savingsBalance, setSavingsBalance] = useState(0);
  const [transactionRef, setTransactionRef] = useState('');
  const { toast } = useToast();

  useEffect(() => {
    if (open && type === 'withdraw') {
      fetchSavingsBalance();
    }
  }, [open, type]);

  const fetchSavingsBalance = async () => {
    try {
      const response = await api.get('/member/accounts');
      const savingsAccount = response.data?.find((a: any) => a.accountType === 'SAVINGS');
      if (savingsAccount) {
        setSavingsBalance(savingsAccount.balance);
      }
    } catch (err) {
      console.error('Error fetching balance:', err);
    }
  };

  const handleInitiateTransaction = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!amount || !phoneNumber) {
      toast({
        title: 'Error',
        description: 'Please enter amount and phone number',
        variant: 'destructive'
      });
      return;
    }

    const transactionAmount = parseFloat(amount);
    if (transactionAmount <= 0) {
      toast({
        title: 'Error',
        description: 'Amount must be greater than zero',
        variant: 'destructive'
      });
      return;
    }

    if (type === 'withdraw' && transactionAmount > savingsBalance) {
      toast({
        title: 'Error',
        description: `Insufficient balance. Available: KES ${savingsBalance.toLocaleString()}`,
        variant: 'destructive'
      });
      return;
    }

    setStep('confirm');
  };

  const handleConfirmTransaction = async () => {
    setProcessing(true);
    try {
      const endpoint = type === 'deposit' 
        ? '/mpesa/deposit/initiate'
        : '/mpesa/withdraw/initiate';

      const response = await api.post(
        endpoint,
        {
          amount: parseFloat(amount),
          phoneNumber
        }
      );

      if (response.data.success) {
        // Use the checkout request ID or conversation ID as reference
        const ref = response.data.checkoutRequestId || response.data.conversationId || `MINET${Date.now()}`;
        setTransactionRef(ref);
        setStep('success');
        // Don't call onSuccess here - only call it after payment is confirmed
      } else {
        toast({
          title: 'Error',
          description: response.data.message || `Failed to process ${type}`,
          variant: 'destructive'
        });
        setStep('entry');
      }
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || `Failed to process ${type}`,
        variant: 'destructive'
      });
      setStep('entry');
    } finally {
      setProcessing(false);
    }
  };

  const handleConfirmPayment = async () => {
    setProcessing(true);
    try {
      const response = await api.post(
        '/mpesa/deposit/confirm',
        {
          checkoutRequestId: transactionRef
        }
      );

      if (response.data.success) {
        toast({
          title: 'Success',
          description: response.data.message,
          variant: 'default'
        });
        
        // Refresh balance
        fetchSavingsBalance();
        
        // Call onSuccess only after payment is confirmed
        onSuccess?.();
        
        // Close after 2 seconds
        setTimeout(() => {
          handleClose();
        }, 2000);
      } else {
        toast({
          title: 'Error',
          description: response.data.message || 'Payment not confirmed yet',
          variant: 'destructive'
        });
      }
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to confirm payment',
        variant: 'destructive'
      });
    } finally {
      setProcessing(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES'
    }).format(amount);
  };

  const handleClose = () => {
    setStep('entry');
    setAmount('');
    setPhoneNumber('');
    setTransactionRef('');
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="w-[95vw] lg:w-full max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {type === 'deposit' ? 'Deposit via M-Pesa' : 'Withdraw via M-Pesa'}
          </DialogTitle>
          <DialogDescription>
            {type === 'deposit' ? 'Send money to your account via M-Pesa' : 'Withdraw funds to your M-Pesa account'}
          </DialogDescription>
        </DialogHeader>

        {step === 'entry' && (
          <form onSubmit={handleInitiateTransaction} className="space-y-6">
            <Alert>
              <Smartphone className="h-4 w-4" />
              <AlertDescription>
                {type === 'deposit' 
                  ? 'Enter the amount you want to deposit. You will send money via M-Pesa to our till number.'
                  : 'Enter the amount you want to withdraw from your savings account. We will send the money to your M-Pesa number.'}
              </AlertDescription>
            </Alert>

            {type === 'withdraw' && (
              <Card className="bg-muted/50">
                <CardContent className="pt-6">
                  <div>
                    <p className="text-sm text-muted-foreground">Available Balance (Savings)</p>
                    <p className="font-semibold text-lg text-primary">
                      {formatCurrency(savingsBalance)}
                    </p>
                  </div>
                </CardContent>
              </Card>
            )}

            <div className="space-y-2">
              <Label htmlFor="amount">Amount (KES)</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Enter amount"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone">M-Pesa Phone Number</Label>
              <Input
                id="phone"
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="e.g., 254712345678"
              />
              <p className="text-xs text-muted-foreground">
                Include country code (254 for Kenya)
              </p>
            </div>

            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={handleClose}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                className="flex-1"
              >
                Continue
              </Button>
            </div>
          </form>
        )}

        {step === 'confirm' && (
          <div className="space-y-6">
            <Card className="bg-primary/10">
              <CardContent className="pt-6">
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Type:</span>
                    <span className="font-semibold capitalize">{type}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Amount:</span>
                    <span className="font-semibold text-lg">{formatCurrency(parseFloat(amount))}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Phone:</span>
                    <span className="font-semibold">{phoneNumber}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                {type === 'deposit' 
                  ? 'An M-Pesa prompt will be sent to your phone. Enter your PIN to complete the payment.'
                  : 'We will send the amount to your M-Pesa number. Your transaction will be processed within 24 hours.'}
              </AlertDescription>
            </Alert>

            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setStep('entry')}
                disabled={processing}
                className="flex-1"
              >
                Back
              </Button>
              <Button
                onClick={handleConfirmTransaction}
                disabled={processing}
                className="flex-1"
              >
                {processing ? 'Processing...' : 'Confirm'}
              </Button>
            </div>
          </div>
        )}

        {step === 'success' && (
          <div className="space-y-6 text-center">
            <div className="flex justify-center">
              <CheckCircle className="h-16 w-16 text-green-600" />
            </div>

            <div>
              <h3 className="text-lg font-semibold mb-2">
                {type === 'deposit' ? 'STK Push Sent' : 'Withdrawal Initiated'}
              </h3>
              <p className="text-muted-foreground">
                {type === 'deposit' 
                  ? 'Check your phone for the M-Pesa prompt. Enter your PIN to complete the payment.'
                  : 'Money will be sent to ' + phoneNumber + '. Please allow a few moments for processing.'}
              </p>
            </div>

            <Card className="bg-muted/50">
              <CardContent className="pt-6">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Reference Number</p>
                  <p className="font-mono font-semibold text-lg">{transactionRef}</p>
                  <p className="text-xs text-muted-foreground mt-2">Save this for your records</p>
                </div>
              </CardContent>
            </Card>

            {type === 'deposit' && (
              <Button
                onClick={() => handleConfirmPayment()}
                className="w-full"
                variant="default"
              >
                I've Completed the Payment
              </Button>
            )}

            <Button
              onClick={handleClose}
              variant={type === 'deposit' ? 'outline' : 'default'}
              className="w-full"
            >
              {type === 'deposit' ? 'Close' : 'Done'}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
