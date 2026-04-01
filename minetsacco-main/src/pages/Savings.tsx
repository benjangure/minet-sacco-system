import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Plus, Search, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Account {
  id: number;
  member: {
    id: number;
    memberNumber: string;
    firstName: string;
    lastName: string;
  };
  accountType: string;
  balance: number;
  createdAt: string;
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

const Savings = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [depositOpen, setDepositOpen] = useState(false);
  const { toast } = useToast();
  const { session, role } = useAuth();

  // Check if user can process transactions
  const canProcessTransactions = role === "ADMIN" || role === "TREASURER" || role === "TELLER";

  // Show message if user doesn't have access
  if (!canProcessTransactions) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only TREASURER and TELLER can access the Savings module.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const fetchActiveMembers = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/members/status/ACTIVE`, {
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        // Members data available if needed for future features
      }
    } catch (error) {
      console.error("Error fetching members:", error);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/accounts`, {
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setAccounts(data.data || []);
      } else {
        console.error("Failed to fetch accounts:", response.status);
      }
    } catch (error) {
      console.error("Error fetching accounts:", error);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (session) {
      fetchData();
      fetchActiveMembers();
    }
  }, [session]);

  // Refresh data when dialog opens
  useEffect(() => {
    if (depositOpen && session) {
      fetchData();
    }
  }, [depositOpen]);

  const [form, setForm] = useState({
    accountId: "",
    amount: "",
    transactionType: "DEPOSIT" as "DEPOSIT" | "WITHDRAWAL",
    description: "",
  });

  const handleTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!form.accountId || !form.amount) {
      toast({ title: "Error", description: "Please fill all required fields", variant: "destructive" });
      return;
    }

    // Find the selected account to get member ID and account type
    const selectedAccount = accounts.find(a => a.id === parseInt(form.accountId));
    if (!selectedAccount) {
      toast({ title: "Error", description: "Invalid account selected", variant: "destructive" });
      return;
    }

    // Prevent withdrawals from SHARES account
    if (form.transactionType === "WITHDRAWAL" && selectedAccount.accountType === "SHARES") {
      toast({ 
        title: "Not Allowed", 
        description: "Withdrawals from SHARES account are not permitted. Shares can only be refunded when exiting the SACCO.", 
        variant: "destructive" 
      });
      return;
    }

    const endpoint = form.transactionType === "DEPOSIT" 
      ? `${API_BASE_URL}/accounts/deposit`
      : `${API_BASE_URL}/accounts/withdraw`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          memberId: selectedAccount.member.id,
          accountType: selectedAccount.accountType,
          amount: parseFloat(form.amount),
          description: form.description || `${form.transactionType} transaction`,
        }),
      });

      if (response.ok) {
        toast({ 
          title: "Success", 
          description: `${form.transactionType === "DEPOSIT" ? "Deposit" : "Withdrawal"} processed successfully` 
        });
        setDepositOpen(false);
        setForm({
          accountId: "",
          amount: "",
          transactionType: "DEPOSIT",
          description: "",
        });
        fetchData();
      } else {
        const error = await response.json();
        toast({ 
          title: "Error", 
          description: error.message || "Transaction failed", 
          variant: "destructive" 
        });
      }
    } catch (error) {
      toast({ title: "Error", description: "Transaction failed", variant: "destructive" });
    }
  };

  const filteredAccounts = accounts.filter(a =>
    !search || 
    `${a.member?.firstName} ${a.member?.lastName} ${a.member?.memberNumber}`.toLowerCase().includes(search.toLowerCase())
  );

  const totalSavings = accounts
    .filter(a => a.accountType === "SAVINGS")
    .reduce((sum, a) => sum + a.balance, 0);

  const totalShares = accounts
    .filter(a => a.accountType === "SHARES")
    .reduce((sum, a) => sum + a.balance, 0);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Savings & Deposits</h1>
          <p className="text-muted-foreground">Manage member shares, deposits, and withdrawals</p>
        </div>
        {canProcessTransactions && (
          <Dialog open={depositOpen} onOpenChange={setDepositOpen}>
            <DialogTrigger asChild>
              <Button><Plus className="mr-2 h-4 w-4" />New Transaction</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader><DialogTitle>Process Transaction</DialogTitle></DialogHeader>
              <form onSubmit={handleTransaction} className="space-y-4">
                <div className="space-y-2">
                  <Label>Member Account *</Label>
                  <Select
                    value={form.accountId}
                    onValueChange={v => setForm({...form, accountId: v})}
                    disabled={loading || accounts.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={
                        loading ? "Loading accounts..." : 
                        accounts.length === 0 ? "No accounts available" : 
                        "Select member account"
                      } />
                    </SelectTrigger>
                    <SelectContent>
                      {accounts.length === 0 ? (
                        <div className="p-2 text-sm text-muted-foreground text-center">
                          No accounts found. Approve members first to create accounts.
                        </div>
                      ) : (
                        accounts
                          .filter(account => form.transactionType === "DEPOSIT" ? account.accountType !== "SHARES" : true)
                          .map(account => (
                          <SelectItem key={account.id} value={account.id.toString()}>
                            {account.member?.memberNumber} — {account.member?.firstName} {account.member?.lastName} ({account.accountType}) - KES {account.balance.toLocaleString()}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                  {accounts.length === 0 && !loading && (
                    <p className="text-xs text-muted-foreground">
                      Accounts are automatically created when members are approved by Treasurer.
                    </p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label>Transaction Type *</Label>
                  <Select value={form.transactionType} onValueChange={v => setForm({...form, transactionType: v as any})}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DEPOSIT">Deposit</SelectItem>
                      <SelectItem value="WITHDRAWAL">Withdrawal</SelectItem>
                    </SelectContent>
                  </Select>
                  {form.transactionType === "DEPOSIT" && (
                    <Alert className="mt-2 bg-blue-50 border-blue-200">
                      <AlertCircle className="h-4 w-4 text-blue-600" />
                      <AlertDescription className="text-xs text-blue-700">
                        This SACCO does not accept share deposits. Only savings and other account types are available.
                      </AlertDescription>
                    </Alert>
                  )}
                  {form.transactionType === "WITHDRAWAL" && form.accountId && 
                   accounts.find(a => a.id === parseInt(form.accountId))?.accountType === "SHARES" && (
                    <Alert variant="destructive" className="mt-2">
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription className="text-xs">
                        Withdrawals from SHARES accounts are not allowed. Shares represent your ownership in the SACCO and can only be refunded when exiting.
                      </AlertDescription>
                    </Alert>
                  )}
                </div>
                <div className="space-y-2">
                  <Label>Amount (KES) *</Label>
                  <Input 
                    type="number" 
                    step="0.01"
                    value={form.amount} 
                    onChange={e => setForm({...form, amount: e.target.value})} 
                    required 
                    placeholder="0.00"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Description</Label>
                  <Input 
                    value={form.description} 
                    onChange={e => setForm({...form, description: e.target.value})} 
                    placeholder="Optional transaction note"
                  />
                </div>
                <Button type="submit" className="w-full">
                  Process {form.transactionType === "DEPOSIT" ? "Deposit" : "Withdrawal"}
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {!canProcessTransactions && (
        <Alert className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            You have view-only access. Contact Teller or Treasurer to process transactions.
          </AlertDescription>
        </Alert>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3 mb-6">
        <Card className="border-none shadow-sm">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-muted-foreground">Total Savings</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">KES {totalSavings.toLocaleString()}</div>
          </CardContent>
        </Card>
        <Card className="border-none shadow-sm">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-muted-foreground">Total Shares</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">KES {totalShares.toLocaleString()}</div>
          </CardContent>
        </Card>
        <Card className="border-none shadow-sm">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-muted-foreground">Active Accounts</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{accounts.length}</div>
          </CardContent>
        </Card>
      </div>

      {/* Search */}
      <Card className="mb-4 border-none shadow-sm">
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input 
              placeholder="Search by member name or number..." 
              className="pl-10" 
              value={search} 
              onChange={e => setSearch(e.target.value)} 
            />
          </div>
        </CardContent>
      </Card>

      {/* Accounts Table */}
      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Member Number</TableHead>
                <TableHead>Member Name</TableHead>
                <TableHead>Account Type</TableHead>
                <TableHead>Balance</TableHead>
                <TableHead>Opened</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : filteredAccounts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                    No accounts found. Accounts are created automatically when members are approved.
                  </TableCell>
                </TableRow>
              ) : (
                filteredAccounts.map(account => (
                  <TableRow key={account.id}>
                    <TableCell className="font-mono text-sm">
                      {account.member?.memberNumber || "—"}
                    </TableCell>
                    <TableCell className="font-medium">
                      {account.member?.firstName} {account.member?.lastName}
                    </TableCell>
                    <TableCell>
                      <Badge variant={account.accountType === "SAVINGS" ? "default" : "secondary"}>
                        {account.accountType}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-semibold">
                      KES {account.balance.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      {new Date(account.createdAt).toLocaleDateString()}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

export default Savings;
