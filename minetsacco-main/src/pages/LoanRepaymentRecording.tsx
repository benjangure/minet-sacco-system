import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Search, Plus, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Loan {
  id: number;
  loanNumber: string;
  member: {
    id: number;
    memberNumber: string;
    firstName: string;
    lastName: string;
  };
  amount: number;
  outstandingBalance: number;
  monthlyRepayment: number;
  status: string;
}

interface LoanRepayment {
  id: number;
  amount: number;
  paymentMethod: string;
  referenceNumber: string;
  paymentDate: string;
  recordedBy: {
    username: string;
  };
}

interface AmortizationSchedule {
  loanId: number;
  principal: number;
  totalRepayable: number;
  totalRepaid: number;
  outstandingBalance: number;
  monthlyPayment: number;
  remainingMonths: number;
  totalMonths: number;
}

const LoanRepaymentRecording = () => {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [selectedLoan, setSelectedLoan] = useState<Loan | null>(null);
  const [repayments, setRepayments] = useState<LoanRepayment[]>([]);
  const [schedule, setSchedule] = useState<AmortizationSchedule | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [loanSearchOpen, setLoanSearchOpen] = useState(true);
  const [repaymentDialogOpen, setRepaymentDialogOpen] = useState(false);
  const [repaymentForm, setRepaymentForm] = useState({
    amount: "",
    paymentMethod: "CASH",
    referenceNumber: "",
    paymentDate: new Date().toISOString().split("T")[0],
  });
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();
  const { session, role } = useAuth();

  const canRecordRepayments = ["TELLER", "TREASURER"].includes(role || "");

  useEffect(() => {
    if (!canRecordRepayments) return;
    fetchActiveLoans();
  }, [session]);

  const fetchActiveLoans = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/loans`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        // Filter to only DISBURSED and ACTIVE loans
        const activeLoans = (data.data || []).filter((l: Loan) => 
          ["DISBURSED", "ACTIVE"].includes(l.status)
        );
        setLoans(activeLoans);
      }
    } catch (error) {
      console.error("Error fetching loans:", error);
    }
  };

  const fetchLoanDetails = async (loanId: number) => {
    setLoading(true);
    try {
      const [repaymentRes, scheduleRes] = await Promise.all([
        fetch(`${API_BASE_URL}/loans/${loanId}/repayments`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
        fetch(`${API_BASE_URL}/loans/${loanId}/schedule`, {
          headers: { "Authorization": `Bearer ${session?.token}` },
        }),
      ]);

      if (repaymentRes.ok) {
        const data = await repaymentRes.json();
        setRepayments(data.data || []);
      }

      if (scheduleRes.ok) {
        const data = await scheduleRes.json();
        setSchedule(data.data);
      }
    } catch (error) {
      console.error("Error fetching loan details:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectLoan = (loan: Loan) => {
    setSelectedLoan(loan);
    setLoanSearchOpen(false);
    fetchLoanDetails(loan.id);
  };

  const handleRecordRepayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoan) return;

    if (!repaymentForm.amount || parseFloat(repaymentForm.amount) <= 0) {
      toast({ title: "Error", description: "Please enter a valid amount", variant: "destructive" });
      return;
    }

    if (parseFloat(repaymentForm.amount) > selectedLoan.outstandingBalance) {
      toast({ 
        title: "Error", 
        description: `Amount cannot exceed outstanding balance of KES ${selectedLoan.outstandingBalance.toLocaleString()}`, 
        variant: "destructive" 
      });
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loans/${selectedLoan.id}/repay`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          amount: parseFloat(repaymentForm.amount),
          paymentMethod: repaymentForm.paymentMethod,
          referenceNumber: repaymentForm.referenceNumber,
          paymentDate: new Date(repaymentForm.paymentDate).toISOString(),
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan repayment recorded successfully" });
        setRepaymentDialogOpen(false);
        setRepaymentForm({
          amount: "",
          paymentMethod: "CASH",
          referenceNumber: "",
          paymentDate: new Date().toISOString().split("T")[0],
        });
        // Refresh loan details
        fetchLoanDetails(selectedLoan.id);
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to record repayment", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to record repayment", variant: "destructive" });
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeLoan = () => {
    setLoanSearchOpen(true);
    setSelectedLoan(null);
    setRepayments([]);
    setSchedule(null);
  };

  const filteredLoans = loans.filter(l =>
    l.loanNumber.toLowerCase().includes(searchInput.toLowerCase()) ||
    `${l.member.firstName} ${l.member.lastName}`.toLowerCase().includes(searchInput.toLowerCase()) ||
    l.member.memberNumber.toLowerCase().includes(searchInput.toLowerCase())
  );

  if (!canRecordRepayments) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only Teller and Treasurer can record loan repayments.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground">Loan Repayment Recording</h1>
        <p className="text-muted-foreground">Record and track loan repayments</p>
      </div>

      {/* Loan Selection Dialog */}
      <Dialog open={loanSearchOpen} onOpenChange={setLoanSearchOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Select Loan</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by loan number, member name..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="pl-10"
              />
            </div>
            <div className="max-h-96 overflow-y-auto border rounded-lg">
              {filteredLoans.length === 0 ? (
                <div className="p-4 text-center text-muted-foreground">No active loans found</div>
              ) : (
                filteredLoans.map(loan => (
                  <div
                    key={loan.id}
                    onClick={() => handleSelectLoan(loan)}
                    className="p-3 border-b hover:bg-accent cursor-pointer transition-colors"
                  >
                    <div className="font-medium">Loan #{loan.loanNumber}</div>
                    <div className="text-sm text-muted-foreground">
                      {loan.member.firstName} {loan.member.lastName} ({loan.member.memberNumber})
                    </div>
                    <div className="text-sm">
                      Outstanding: <span className="font-semibold">KES {loan.outstandingBalance.toLocaleString()}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Loan Selected - Show Details */}
      {selectedLoan && (
        <>
          {/* Loan Info Card */}
          <Card className="mb-6 border-blue-200 bg-blue-50">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">Selected Loan</p>
                  <p className="text-lg font-semibold">Loan #{selectedLoan.loanNumber}</p>
                  <p className="text-sm text-muted-foreground">
                    {selectedLoan.member.firstName} {selectedLoan.member.lastName}
                  </p>
                </div>
                <Button variant="outline" onClick={handleChangeLoan}>
                  Change Loan
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Amortization Schedule */}
          {schedule && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Principal</p>
                  <p className="text-lg font-bold">KES {schedule.principal.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Total Repaid</p>
                  <p className="text-lg font-bold text-green-600">KES {schedule.totalRepaid.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Outstanding</p>
                  <p className="text-lg font-bold text-red-600">KES {schedule.outstandingBalance.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Remaining Months</p>
                  <p className="text-lg font-bold">{schedule.remainingMonths} / {schedule.totalMonths}</p>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Record Repayment Button */}
          <div className="mb-6">
            <Button onClick={() => setRepaymentDialogOpen(true)} className="w-full md:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              Record Repayment
            </Button>
          </div>

          {/* Record Repayment Dialog */}
          <Dialog open={repaymentDialogOpen} onOpenChange={setRepaymentDialogOpen}>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Record Loan Repayment</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleRecordRepayment} className="space-y-4">
                <div>
                  <Label className="text-xs">Amount (KES) *</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={repaymentForm.amount}
                    onChange={(e) => setRepaymentForm({ ...repaymentForm, amount: e.target.value })}
                    placeholder={`Max: KES ${selectedLoan.outstandingBalance.toLocaleString()}`}
                    className="text-sm"
                    required
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Outstanding: KES {selectedLoan.outstandingBalance.toLocaleString()}
                  </p>
                </div>

                <div>
                  <Label className="text-xs">Payment Method *</Label>
                  <Select value={repaymentForm.paymentMethod} onValueChange={(value) => setRepaymentForm({ ...repaymentForm, paymentMethod: value })}>
                    <SelectTrigger className="text-sm">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CASH">Cash</SelectItem>
                      <SelectItem value="MPESA">M-Pesa</SelectItem>
                      <SelectItem value="BANK_TRANSFER">Bank Transfer</SelectItem>
                      <SelectItem value="CHEQUE">Cheque</SelectItem>
                      <SelectItem value="OTHER">Other</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label className="text-xs">Reference Number</Label>
                  <Input
                    value={repaymentForm.referenceNumber}
                    onChange={(e) => setRepaymentForm({ ...repaymentForm, referenceNumber: e.target.value })}
                    placeholder="Receipt/Transaction reference"
                    className="text-sm"
                  />
                </div>

                <div>
                  <Label className="text-xs">Payment Date *</Label>
                  <Input
                    type="date"
                    value={repaymentForm.paymentDate}
                    onChange={(e) => setRepaymentForm({ ...repaymentForm, paymentDate: e.target.value })}
                    className="text-sm"
                    required
                  />
                </div>

                <div className="flex gap-2 pt-4">
                  <Button type="button" variant="outline" onClick={() => setRepaymentDialogOpen(false)} className="flex-1">
                    Cancel
                  </Button>
                  <Button type="submit" disabled={submitting} className="flex-1">
                    {submitting ? "Recording..." : "Record Repayment"}
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>

          {/* Repayment History */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Repayment History ({repayments.length})</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading...</div>
              ) : repayments.length === 0 ? (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>No repayments recorded yet</AlertDescription>
                </Alert>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Date</TableHead>
                        <TableHead>Amount</TableHead>
                        <TableHead>Method</TableHead>
                        <TableHead>Reference</TableHead>
                        <TableHead>Recorded By</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {repayments.map(repayment => (
                        <TableRow key={repayment.id}>
                          <TableCell className="text-sm">
                            {new Date(repayment.paymentDate).toLocaleDateString()}
                          </TableCell>
                          <TableCell className="font-medium">
                            KES {repayment.amount.toLocaleString()}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline">{repayment.paymentMethod.replace(/_/g, " ")}</Badge>
                          </TableCell>
                          <TableCell className="text-sm">{repayment.referenceNumber || "—"}</TableCell>
                          <TableCell className="text-sm">{repayment.recordedBy.username}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
};

export default LoanRepaymentRecording;
