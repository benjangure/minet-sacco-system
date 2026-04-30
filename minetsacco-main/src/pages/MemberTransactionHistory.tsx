import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Search, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Member {
  id: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
  status: string;
}

interface Transaction {
  id: number;
  account: {
    member: {
      memberNumber: string;
      firstName: string;
      lastName: string;
    };
  };
  transactionType: string;
  amount: number;
  description: string;
  transactionDate: string;
}

const transactionTypeColors: Record<string, string> = {
  DEPOSIT: "bg-green-100 text-green-800",
  WITHDRAWAL: "bg-red-100 text-red-800",
  LOAN_DISBURSEMENT: "bg-blue-100 text-blue-800",
  LOAN_REPAYMENT: "bg-purple-100 text-purple-800",
  INTEREST: "bg-yellow-100 text-yellow-800",
  LOAN_DEFAULT_DEBIT: "bg-red-200 text-red-900",
};

const MemberTransactionHistory = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [transactionTypeFilter, setTransactionTypeFilter] = useState("all");
  const { toast } = useToast();
  const { session, role } = useAuth();

  // Check if user has access
  const canAccessTransactionHistory = ["ADMIN", "TREASURER", "LOAN_OFFICER", "CREDIT_COMMITTEE", "AUDITOR"].includes(role || "");

  useEffect(() => {
    if (!canAccessTransactionHistory) {
      return;
    }
    fetchMembers();
  }, [session]);

  const fetchMembers = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/members`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setMembers(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching members:", error);
    }
  };

  const fetchTransactions = async (memberId: number) => {
    setLoading(true);
    try {
      let url = `${API_BASE_URL}/members/${memberId}/transactions`;
      const params = new URLSearchParams();
      
      if (startDate) params.append("startDate", startDate);
      if (endDate) params.append("endDate", endDate);
      if (transactionTypeFilter && transactionTypeFilter !== "all") params.append("transactionType", transactionTypeFilter);
      
      if (params.toString()) {
        url += "?" + params.toString();
      }

      const response = await fetch(url, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data.data || []);
      } else {
        toast({ title: "Error", description: "Failed to fetch transactions", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to fetch transactions", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const handleSelectMember = (member: Member) => {
    setSelectedMember(member);
    setTransactions([]);
    setStartDate("");
    setEndDate("");
    setTransactionTypeFilter("all");
    // Fetch all transactions for this member
    fetchTransactionsForMember(member.id);
  };

  const fetchTransactionsForMember = async (memberId: number) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/members/${memberId}/transactions`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching transactions:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleApplyFilters = () => {
    if (selectedMember) {
      fetchTransactions(selectedMember.id);
    }
  };

  const handleClearFilters = () => {
    setStartDate("");
    setEndDate("");
    setTransactionTypeFilter("all");
    if (selectedMember) {
      fetchTransactionsForMember(selectedMember.id);
    }
  };

  const handleChangeMember = () => {
    setSelectedMember(null);
    setTransactions([]);
  };

  const filteredMembers = members.filter(m =>
    m.memberNumber.toLowerCase().includes(searchInput.toLowerCase()) ||
    m.firstName.toLowerCase().includes(searchInput.toLowerCase()) ||
    m.lastName.toLowerCase().includes(searchInput.toLowerCase())
  );

  const calculateTotals = () => {
    const deposits = transactions
      .filter(t => t.transactionType === "DEPOSIT")
      .reduce((sum, t) => sum + t.amount, 0);
    const withdrawals = transactions
      .filter(t => t.transactionType === "WITHDRAWAL")
      .reduce((sum, t) => sum + t.amount, 0);
    const loanDisbursements = transactions
      .filter(t => t.transactionType === "LOAN_DISBURSEMENT")
      .reduce((sum, t) => sum + t.amount, 0);
    const loanRepayments = transactions
      .filter(t => t.transactionType === "LOAN_REPAYMENT")
      .reduce((sum, t) => sum + t.amount, 0);

    return { deposits, withdrawals, loanDisbursements, loanRepayments };
  };

  if (!canAccessTransactionHistory) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only Admin, Treasurer, Loan Officer, Credit Committee, and Auditor can access this page.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const totals = calculateTotals();

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground">Member Transaction History</h1>
        <p className="text-muted-foreground">View all transactions for a member</p>
      </div>

      {!selectedMember && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base">Select Member</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="relative mb-4">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by member number, name..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="pl-10"
              />
            </div>

            <div className="border rounded-lg overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Member</TableHead>
                    <TableHead>Member No.</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredMembers.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-muted-foreground py-6">
                        No members found
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredMembers.map((member) => (
                      <TableRow
                        key={member.id}
                        className="cursor-pointer hover:bg-accent"
                        onClick={() => handleSelectMember(member)}
                      >
                        <TableCell className="font-medium">
                          {member.firstName} {member.lastName}
                        </TableCell>
                        <TableCell>{member.memberNumber}</TableCell>
                        <TableCell>
                          <Badge variant="outline">{member.status}</Badge>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Member Selected - Show Transactions */}
      {selectedMember && (
        <>
          {/* Member Info Card */}
          <Card className="mb-6 border-blue-200 bg-blue-50">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">Selected Member</p>
                  <p className="text-lg font-semibold">{selectedMember.firstName} {selectedMember.lastName}</p>
                  <p className="text-sm text-muted-foreground">{selectedMember.memberNumber}</p>
                </div>
                <Button variant="outline" onClick={handleChangeMember}>
                  Change Member
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Filters */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-base">Filters</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <Label className="text-xs">Start Date</Label>
                  <Input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div>
                  <Label className="text-xs">End Date</Label>
                  <Input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div>
                  <Label className="text-xs">Transaction Type</Label>
                  <Select value={transactionTypeFilter || "all"} onValueChange={(value) => setTransactionTypeFilter(value === "all" ? "" : value)}>
                    <SelectTrigger className="text-sm">
                      <SelectValue placeholder="All types" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Types</SelectItem>
                      <SelectItem value="DEPOSIT">Deposit</SelectItem>
                      <SelectItem value="WITHDRAWAL">Withdrawal</SelectItem>
                      <SelectItem value="LOAN_DISBURSEMENT">Loan Disbursement</SelectItem>
                      <SelectItem value="LOAN_REPAYMENT">Loan Repayment</SelectItem>
                      <SelectItem value="INTEREST">Interest</SelectItem>
                      <SelectItem value="LOAN_DEFAULT_DEBIT">Loan Default Debit</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex items-end gap-2">
                  <Button size="sm" onClick={handleApplyFilters} className="flex-1">
                    Apply Filters
                  </Button>
                  <Button size="sm" variant="outline" onClick={handleClearFilters} className="flex-1">
                    Clear
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Summary Cards */}
          {transactions.length > 0 && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Total Deposits</p>
                  <p className="text-lg font-bold text-green-600">KES {totals.deposits.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Total Withdrawals</p>
                  <p className="text-lg font-bold text-red-600">KES {totals.withdrawals.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Loan Disbursements</p>
                  <p className="text-lg font-bold text-blue-600">KES {totals.loanDisbursements.toLocaleString()}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="pt-6">
                  <p className="text-xs text-muted-foreground">Loan Repayments</p>
                  <p className="text-lg font-bold text-purple-600">KES {totals.loanRepayments.toLocaleString()}</p>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Transactions Table */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Transactions ({transactions.length})</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading transactions...</div>
              ) : transactions.length === 0 ? (
                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>No transactions found for this member</AlertDescription>
                </Alert>
              ) : (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Date</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead>Description</TableHead>
                        <TableHead className="text-right">Amount</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {transactions.map(transaction => (
                        <TableRow key={transaction.id}>
                          <TableCell className="text-sm">
                            {new Date(transaction.transactionDate).toLocaleDateString()}
                          </TableCell>
                          <TableCell>
                            <Badge className={transactionTypeColors[transaction.transactionType] || "bg-gray-100 text-gray-800"}>
                              {transaction.transactionType.replace(/_/g, " ")}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm">{transaction.description}</TableCell>
                          <TableCell className="text-right font-medium">
                            KES {transaction.amount.toLocaleString()}
                          </TableCell>
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

export default MemberTransactionHistory;
