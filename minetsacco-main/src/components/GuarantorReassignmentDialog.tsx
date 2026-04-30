import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { API_BASE_URL } from '@/config/api';

interface Guarantor {
  id: number;
  member: {
    id: number;
    firstName: string;
    lastName: string;
    memberNumber?: string;
  };
  guaranteeAmount: number;
  previousGuaranteeAmount?: number;
  status: string;
}

interface Loan {
  id: number;
  loanNumber: string;
  amount: number;
  originalAmount?: number;
  status: string;
}

interface GuarantorReassignmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  loan: Loan | null;
  guarantors: Guarantor[];
  onReassignmentComplete: () => void;
}

interface GuarantorAssignment {
  guarantorId: number;
  guaranteeAmount: number;
}

export const GuarantorReassignmentDialog: React.FC<GuarantorReassignmentDialogProps> = ({
  open,
  onOpenChange,
  loan,
  guarantors,
  onReassignmentComplete,
}) => {
  const [assignments, setAssignments] = useState<GuarantorAssignment[]>([]);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);

  useEffect(() => {
    if (open && guarantors.length > 0) {
      // Initialize assignments with guarantors that need reassignment
      const initialAssignments = guarantors
        .filter(g => g.status === 'PENDING_REASSIGNMENT' || g.guaranteeAmount === 0)
        .map(g => ({
          guarantorId: g.id,
          guaranteeAmount: 0,
        }));
      setAssignments(initialAssignments);
      setErrors([]);
    }
  }, [open, guarantors]);

  const handleAmountChange = (guarantorId: number, amount: string) => {
    const numAmount = parseFloat(amount) || 0;
    setAssignments(prev =>
      prev.map(a =>
        a.guarantorId === guarantorId
          ? { ...a, guaranteeAmount: numAmount }
          : a
      )
    );
  };

  const validateAssignments = (): boolean => {
    const newErrors: string[] = [];

    // Check if all amounts are positive
    for (const assignment of assignments) {
      if (assignment.guaranteeAmount <= 0) {
        const guarantor = guarantors.find(g => g.id === assignment.guarantorId);
        newErrors.push(`${guarantor?.member.firstName} ${guarantor?.member.lastName}: Amount must be greater than 0`);
      }
      
      // Check if individual guarantee amount does not exceed loan amount
      if (loan && assignment.guaranteeAmount > loan.amount) {
        const guarantor = guarantors.find(g => g.id === assignment.guarantorId);
        newErrors.push(`${guarantor?.member.firstName} ${guarantor?.member.lastName}: Guarantee amount cannot exceed loan amount (KES ${loan.amount.toFixed(2)})`);
      }
    }

    // Check if total covers loan amount
    const totalAmount = assignments.reduce((sum, a) => sum + a.guaranteeAmount, 0);
    if (loan && totalAmount < loan.amount) {
      newErrors.push(
        `Total guarantee amount (KES ${totalAmount.toFixed(2)}) must be at least equal to loan amount (KES ${loan.amount.toFixed(2)})`
      );
    }

    setErrors(newErrors);
    return newErrors.length === 0;
  };

  const handleReassign = async () => {
    if (!validateAssignments() || !loan) {
      return;
    }

    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `${API_BASE_URL}/loans/${loan.id}/reassign-guarantors`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(assignments),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Failed to reassign guarantors');
      }

      toast.success('Guarantors reassigned successfully');
      onOpenChange(false);
      onReassignmentComplete();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred';
      toast.error(errorMessage);
      console.error('Error reassigning guarantors:', error);
    } finally {
      setLoading(false);
    }
  };

  if (!loan) return null;

  const totalAmount = assignments.reduce((sum, a) => sum + a.guaranteeAmount, 0);
  const isValid = totalAmount >= loan.amount && assignments.every(a => a.guaranteeAmount > 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Reassign Guarantors</DialogTitle>
          <DialogDescription>
            Your loan amount has been reduced. Please re-assign guarantors with new guarantee amounts.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Loan Summary */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Loan Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <div className="flex justify-between">
                <span className="text-gray-600">Loan Number:</span>
                <span className="font-semibold">{loan.loanNumber}</span>
              </div>
              {loan.originalAmount && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Original Amount:</span>
                  <span className="font-semibold">KES {loan.originalAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between border-t pt-2">
                <span className="text-gray-600">New Loan Amount:</span>
                <span className="font-semibold text-lg">KES {loan.amount.toFixed(2)}</span>
              </div>
            </CardContent>
          </Card>

          {/* Guarantor Assignments */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Guarantor Assignments</CardTitle>
              <CardDescription>
                Assign new guarantee amounts for each guarantor. Total must equal or exceed the loan amount.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {assignments.map((assignment) => {
                const guarantor = guarantors.find(g => g.id === assignment.guarantorId);
                if (!guarantor) return null;

                return (
                  <div key={assignment.guarantorId} className="space-y-2 p-4 border rounded-lg">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="font-semibold">
                          {guarantor.member.firstName} {guarantor.member.lastName}
                        </p>
                        <p className="text-sm text-gray-600">
                          Member: {guarantor.member.memberNumber}
                        </p>
                      </div>
                      {guarantor.previousGuaranteeAmount && (
                        <div className="text-right">
                          <p className="text-sm text-gray-600">Previous Amount:</p>
                          <p className="font-semibold">KES {guarantor.previousGuaranteeAmount.toFixed(2)}</p>
                        </div>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <label className="text-sm font-medium">New Guarantee Amount:</label>
                      <div className="flex items-center gap-1">
                        <span className="text-gray-600">KES</span>
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          value={assignment.guaranteeAmount || ''}
                          onChange={(e) =>
                            handleAmountChange(assignment.guarantorId, e.target.value)
                          }
                          placeholder="0.00"
                          className="w-32"
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>

          {/* Total Summary */}
          <Card className={isValid ? 'border-green-200 bg-green-50' : 'border-orange-200 bg-orange-50'}>
            <CardContent className="pt-6">
              <div className="flex justify-between items-center">
                <span className="font-semibold">Total Guarantee Amount:</span>
                <span className={`text-lg font-bold ${isValid ? 'text-green-600' : 'text-orange-600'}`}>
                  KES {totalAmount.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between items-center mt-2 text-sm">
                <span className="text-gray-600">Required Amount:</span>
                <span className="font-semibold">KES {loan.amount.toFixed(2)}</span>
              </div>
              {isValid && (
                <div className="flex items-center gap-2 mt-3 text-green-600">
                  <CheckCircle className="w-4 h-4" />
                  <span className="text-sm">Total guarantees cover the loan amount</span>
                </div>
              )}
              {!isValid && totalAmount > 0 && (
                <div className="flex items-center gap-2 mt-3 text-orange-600">
                  <AlertCircle className="w-4 h-4" />
                  <span className="text-sm">
                    Shortfall: KES {(loan.amount - totalAmount).toFixed(2)}
                  </span>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Errors */}
          {errors.length > 0 && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                <ul className="list-disc list-inside space-y-1">
                  {errors.map((error, idx) => (
                    <li key={idx}>{error}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}

          {/* Actions */}
          <div className="flex gap-3 justify-end">
            <Button
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              onClick={handleReassign}
              disabled={loading || !isValid}
              className="gap-2"
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              {loading ? 'Reassigning...' : 'Reassign Guarantors'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
