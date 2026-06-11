import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Edit2, Plus, ToggleLeft, ToggleRight } from "lucide-react";
import { getApiBaseUrl } from '@/config/api';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const API_BASE_URL = getApiBaseUrl();

interface GLAccount {
  id: number;
  code: string;
  name: string;
  accountType: string;
  balanceCalculationType: string;
  normalBalance: string;
  sectionLabel?: string;
  periodSensitive: boolean;
  isActive: boolean;
  displayOrder: number;
  calculationConfig?: Record<string, any>;
}

interface DataSource {
  sourceType: string;
  label: string;
  requiresProductSelection: boolean;
  loanProducts?: { id: number; name: string }[];
}

interface GLPeriodEntry {
  glAccountId: number;
  code: string;
  name: string;
  accountType: string;
  normalBalance: string;
  sectionLabel?: string;
  sourceType: string;
  amount: number;
  periodStatus?: string;
  entryId?: number;
  readOnly: boolean;
}

const normalBalanceDefaults: Record<string, string> = {
  ASSET: "DEBIT",
  EXPENSE: "DEBIT",
  LIABILITY: "CREDIT",
  EQUITY: "CREDIT",
  REVENUE: "CREDIT",
};

export default function GLConfiguration() {
  const { session } = useAuth();
  const { toast } = useToast();
  const token = session?.token;

  // Tab 1 State
  const [accounts, setAccounts] = useState<GLAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<GLAccount | null>(null);
  const [dataSources, setDataSources] = useState<DataSource[]>([]);

  const [formData, setFormData] = useState({
    code: "",
    name: "",
    accountType: "ASSET",
    source: "AGGREGATION",
    dataSource: "",
    loanProductId: "",
    normalBalance: "",
    sectionLabel: "",
    periodSensitive: false,
    displayOrder: "100",
  });

  // Tab 2 State
  const [periodEntries, setPeriodEntries] = useState<GLPeriodEntry[]>([]);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [periodLoading, setPeriodLoading] = useState(false);
  const [editedAmounts, setEditedAmounts] = useState<Record<number, number>>({});
  const [draftEntries, setDraftEntries] = useState<Set<number>>(new Set());

  useEffect(() => {
    fetchAccounts();
    fetchDataSources();
  }, []);

  // ============ Tab 1: GL Accounts ============
  const fetchAccounts = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/gl/account-configuration`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      if (data.success) {
        setAccounts(data.data);
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to load accounts",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchDataSources = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/gl/data-sources`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      if (data.success) {
        setDataSources(data.data.sources);
      }
    } catch (error) {
      console.error("Failed to fetch data sources:", error);
    }
  };

  const resetForm = () => {
    setFormData({
      code: "",
      name: "",
      accountType: "ASSET",
      source: "AGGREGATION",
      dataSource: "",
      loanProductId: "",
      normalBalance: normalBalanceDefaults["ASSET"],
      sectionLabel: "",
      periodSensitive: false,
      displayOrder: "100",
    });
  };

  const handleAddAccount = async () => {
    if (!formData.code.trim() || !formData.name.trim()) {
      toast({ title: "Error", description: "Code and Name are required", variant: "destructive" });
      return;
    }

    const body: any = {
      code: formData.code,
      name: formData.name,
      accountType: formData.accountType,
      balanceCalculationType: formData.source,
      normalBalance: formData.normalBalance,
      sectionLabel: formData.sectionLabel || null,
      periodSensitive: formData.periodSensitive,
      displayOrder: parseInt(formData.displayOrder),
    };

    if (formData.source === "AGGREGATION") {
      body.dataSource = formData.dataSource;
      if (formData.dataSource === "LOANS" && formData.loanProductId) {
        body.loanProductId = parseInt(formData.loanProductId);
      }
    }

    try {
      const response = await fetch(`${API_BASE_URL}/gl/account-configuration`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
      const data = await response.json();
      if (data.success) {
        toast({ title: "Success", description: "GL Account created successfully" });
        setAddDialogOpen(false);
        resetForm();
        fetchAccounts();
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to create account",
        variant: "destructive",
      });
    }
  };

  const handleEditAccount = (account: GLAccount) => {
    setEditingAccount(account);
    setFormData({
      code: account.code,
      name: account.name,
      accountType: account.accountType,
      source: account.balanceCalculationType,
      dataSource: "",
      loanProductId: "",
      normalBalance: account.normalBalance,
      sectionLabel: account.sectionLabel || "",
      periodSensitive: account.periodSensitive,
      displayOrder: account.displayOrder.toString(),
    });
    setEditDialogOpen(true);
  };

  const handleUpdateAccount = async () => {
    if (!editingAccount) return;

    const body = {
      name: formData.name,
      sectionLabel: formData.sectionLabel || null,
      periodSensitive: formData.periodSensitive,
      displayOrder: parseInt(formData.displayOrder),
      isActive: editingAccount.isActive,
    };

    try {
      const response = await fetch(`${API_BASE_URL}/gl/account-configuration/${editingAccount.id}`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
      const data = await response.json();
      if (data.success) {
        toast({ title: "Success", description: "GL Account updated successfully" });
        setEditDialogOpen(false);
        setEditingAccount(null);
        fetchAccounts();
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to update account",
        variant: "destructive",
      });
    }
  };

  // ============ Tab 2: Period Entry ============
  const fetchPeriodEntries = async () => {
    setPeriodLoading(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/gl/period-entry?periodMonth=${selectedMonth}&periodYear=${selectedYear}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      const data = await response.json();
      if (data.success) {
        setPeriodEntries(data.data);
        setEditedAmounts({});
        setDraftEntries(new Set());
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to load period entries",
        variant: "destructive",
      });
    } finally {
      setPeriodLoading(false);
    }
  };

  const handleAmountChange = (glAccountId: number, amount: number) => {
    setEditedAmounts({ ...editedAmounts, [glAccountId]: amount });
    setDraftEntries((prev) => new Set(prev).add(glAccountId));
  };

  const handleSaveDrafts = async () => {
    // draftEntries stores glAccountId values (not entryId)
    // For each modified account, POST to create/update the entry using glAccountId
    const entriesToSave = Array.from(draftEntries).map((glAccountId) => {
      const entry = periodEntries.find((e) => e.glAccountId === glAccountId);
      return {
        glAccountId: glAccountId,
        amount: editedAmounts[glAccountId] ?? entry?.amount ?? 0,
        periodMonth: selectedMonth,
        periodYear: selectedYear,
        description: `Period entry for ${entry?.name ?? glAccountId}`,
        entryReason: "ADJUSTMENT",
      };
    });

    try {
      for (const entry of entriesToSave) {
        const response = await fetch(`${API_BASE_URL}/gl/period-entry`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(entry),
        });
        if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          throw new Error(errData.message || `Failed to save entry for account ${entry.glAccountId}`);
        }
      }
      toast({ title: "Success", description: "Drafts saved successfully" });
      setDraftEntries(new Set());
      setEditedAmounts({});
      fetchPeriodEntries();
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to save drafts",
        variant: "destructive",
      });
    }
  };

  const handleSubmitForApproval = async () => {
    // Step 1: Save any unsaved draft amounts first
    if (draftEntries.size > 0) {
      await handleSaveDrafts();
      // Wait for fetchPeriodEntries to complete by refetching directly
      // before submitting — we need the updated entryIds
    }

    // Step 2: Re-fetch the period entries to get fresh entryIds after save
    let freshEntries: GLPeriodEntry[] = [];
    try {
      const response = await fetch(
        `${API_BASE_URL}/gl/period-entry?periodMonth=${selectedMonth}&periodYear=${selectedYear}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const data = await response.json();
      if (data.success) {
        freshEntries = data.data;
      }
    } catch {
      freshEntries = periodEntries;
    }

    const draftIds = freshEntries
      .filter((e) => e.periodStatus === "DRAFT" && e.entryId)
      .map((e) => e.entryId!);

    if (draftIds.length === 0) {
      toast({ title: "Info", description: "No draft entries to submit for approval", variant: "default" });
      return;
    }

    try {
      for (const id of draftIds) {
        const response = await fetch(`${API_BASE_URL}/gl/period-entry/${id}/submit`, {
          method: "PUT",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          throw new Error(errData.message || `Failed to submit entry ${id}`);
        }
      }
      toast({ title: "Success", description: `${draftIds.length} entries submitted for approval` });
      fetchPeriodEntries();
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to submit entries",
        variant: "destructive",
      });
    }
  };

  const handleApproveEntry = async (entryId: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/gl/period-entry/${entryId}/approve`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      if (data.success) {
        toast({ title: "Success", description: "Entry approved" });
        fetchPeriodEntries();
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to approve entry",
        variant: "destructive",
      });
    }
  };

  const handleRejectEntry = async (entryId: number) => {
    const reason = prompt("Enter rejection reason (optional):");
    try {
      const response = await fetch(`${API_BASE_URL}/gl/period-entry/${entryId}/reject`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ rejectReason: reason || "" }),
      });
      const data = await response.json();
      if (data.success) {
        toast({ title: "Success", description: "Entry rejected and returned to draft" });
        fetchPeriodEntries();
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to reject entry",
        variant: "destructive",
      });
    }
  };

  const handleLockEntry = async (entryId: number) => {
    try {
      const response = await fetch(`${API_BASE_URL}/gl/period-entry/${entryId}/lock`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      if (data.success) {
        toast({ title: "Success", description: "Entry locked" });
        fetchPeriodEntries();
      } else {
        toast({ title: "Error", description: data.message, variant: "destructive" });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to lock entry",
        variant: "destructive",
      });
    }
  };

  const getStatusBadgeColor = (status?: string) => {
    switch (status) {
      case "DRAFT":
        return "bg-gray-200 text-gray-800";
      case "POSTED":
        return "bg-blue-200 text-blue-800";
      case "APPROVED":
        return "bg-green-200 text-green-800";
      case "LOCKED":
        return "bg-red-200 text-red-800";
      default:
        return "bg-gray-100 text-gray-700";
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">GL Configuration</h1>
      </div>

      <Tabs defaultValue="accounts" className="w-full">
        <TabsList>
          <TabsTrigger value="accounts">GL Accounts</TabsTrigger>
          <TabsTrigger value="period-entry">Period Entry</TabsTrigger>
        </TabsList>

        {/* Tab 1: GL Accounts */}
        <TabsContent value="accounts" className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold">GL Accounts</h2>
            <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
              <DialogTrigger asChild>
                <Button onClick={() => resetForm()}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Account
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-md">
                <DialogHeader>
                  <DialogTitle>Add GL Account</DialogTitle>
                </DialogHeader>
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="code">Code *</Label>
                    <Input
                      id="code"
                      value={formData.code}
                      onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                      placeholder="e.g., NORMAL_LOAN"
                    />
                  </div>
                  <div>
                    <Label htmlFor="name">Name *</Label>
                    <Input
                      id="name"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="e.g., Normal Loan"
                    />
                  </div>
                  <div>
                    <Label htmlFor="accountType">Account Type *</Label>
                    <Select
                      value={formData.accountType}
                      onValueChange={(value) => {
                        setFormData({
                          ...formData,
                          accountType: value,
                          normalBalance: normalBalanceDefaults[value],
                        });
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ASSET">Asset</SelectItem>
                        <SelectItem value="LIABILITY">Liability</SelectItem>
                        <SelectItem value="EQUITY">Equity</SelectItem>
                        <SelectItem value="REVENUE">Revenue</SelectItem>
                        <SelectItem value="EXPENSE">Expense</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="source">Source *</Label>
                    <Select value={formData.source} onValueChange={(value) => setFormData({ ...formData, source: value })}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="AGGREGATION">Auto Aggregation</SelectItem>
                        <SelectItem value="MANUAL_ENTRY">Manual Entry</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {formData.source === "AGGREGATION" && (
                    <>
                      <div>
                        <Label htmlFor="dataSource">Data Source *</Label>
                        <Select value={formData.dataSource} onValueChange={(value) => setFormData({ ...formData, dataSource: value })}>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {dataSources.map((source) => (
                              <SelectItem key={source.sourceType} value={source.sourceType}>
                                {source.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      {formData.dataSource === "LOANS" && (
                        <div>
                          <Label htmlFor="loanProduct">Loan Product</Label>
                          <Select value={formData.loanProductId} onValueChange={(value) => setFormData({ ...formData, loanProductId: value })}>
                            <SelectTrigger>
                              <SelectValue placeholder="Select loan product (optional)" />
                            </SelectTrigger>
                            <SelectContent>
                              {dataSources
                                .find((s) => s.sourceType === "LOANS")
                                ?.loanProducts?.map((product) => (
                                  <SelectItem key={product.id} value={product.id.toString()}>
                                    {product.name}
                                  </SelectItem>
                                ))}
                            </SelectContent>
                          </Select>
                        </div>
                      )}
                    </>
                  )}

                  <div>
                    <Label htmlFor="normalBalance">Normal Balance *</Label>
                    <Select value={formData.normalBalance} onValueChange={(value) => setFormData({ ...formData, normalBalance: value })}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="DEBIT">Debit</SelectItem>
                        <SelectItem value="CREDIT">Credit</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div>
                    <Label htmlFor="sectionLabel">Section Label</Label>
                    <Input
                      id="sectionLabel"
                      value={formData.sectionLabel}
                      onChange={(e) => setFormData({ ...formData, sectionLabel: e.target.value })}
                      placeholder="e.g., Cash and Cash Equivalents"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="periodSensitive"
                      checked={formData.periodSensitive}
                      onChange={(e) => setFormData({ ...formData, periodSensitive: e.target.checked })}
                      disabled={formData.source !== "MANUAL_ENTRY"}
                    />
                    <Label htmlFor="periodSensitive" className="cursor-pointer">
                      Period Sensitive
                    </Label>
                  </div>

                  <div>
                    <Label htmlFor="displayOrder">Display Order</Label>
                    <Input
                      id="displayOrder"
                      type="number"
                      value={formData.displayOrder}
                      onChange={(e) => setFormData({ ...formData, displayOrder: e.target.value })}
                    />
                  </div>

                  <div className="flex gap-2 justify-end">
                    <Button variant="outline" onClick={() => setAddDialogOpen(false)}>
                      Cancel
                    </Button>
                    <Button onClick={handleAddAccount}>Create Account</Button>
                  </div>
                </div>
              </DialogContent>
            </Dialog>
          </div>

          <Card>
            <CardContent className="pt-6">
              {loading ? (
                <div className="flex justify-center py-8">Loading...</div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Code</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Balance</TableHead>
                      <TableHead>Section</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {accounts.map((account) => (
                      <TableRow key={account.id}>
                        <TableCell className="font-mono text-sm">{account.code}</TableCell>
                        <TableCell>{account.name}</TableCell>
                        <TableCell>{account.accountType}</TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {account.balanceCalculationType === "AGGREGATION" ? "AUTO" : "MANUAL"}
                          </Badge>
                        </TableCell>
                        <TableCell>{account.normalBalance}</TableCell>
                        <TableCell>{account.sectionLabel || "-"}</TableCell>
                        <TableCell>
                          <Badge variant={account.isActive ? "default" : "secondary"}>
                            {account.isActive ? "Active" : "Inactive"}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleEditAccount(account)}
                          >
                            <Edit2 className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tab 2: Period Entry */}
        <TabsContent value="period-entry" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Period Entry</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-4 items-end">
                <div>
                  <Label htmlFor="month">Month</Label>
                  <Select value={selectedMonth.toString()} onValueChange={(v) => setSelectedMonth(parseInt(v))}>
                    <SelectTrigger className="w-[120px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                        <SelectItem key={m} value={m.toString()}>
                          {new Date(2024, m - 1).toLocaleString("default", { month: "long" })}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="year">Year</Label>
                  <Select value={selectedYear.toString()} onValueChange={(v) => setSelectedYear(parseInt(v))}>
                    <SelectTrigger className="w-[120px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i).map((y) => (
                        <SelectItem key={y} value={y.toString()}>
                          {y}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <Button onClick={fetchPeriodEntries} disabled={periodLoading}>
                  {periodLoading ? "Loading..." : "Load"}
                </Button>
              </div>

              {periodEntries.length > 0 && (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Section</TableHead>
                        <TableHead>Code</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead>Source</TableHead>
                        <TableHead>Amount</TableHead>
                        <TableHead>Status</TableHead>
                        {session?.user?.role?.toUpperCase() === "ADMIN" && <TableHead>Actions</TableHead>}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {periodEntries.map((entry) => (
                        <TableRow key={entry.entryId || entry.glAccountId}>
                          <TableCell>{entry.sectionLabel || "-"}</TableCell>
                          <TableCell className="font-mono text-sm">{entry.code}</TableCell>
                          <TableCell>{entry.name}</TableCell>
                          <TableCell>{entry.accountType}</TableCell>
                          <TableCell>
                            <Badge variant={entry.sourceType === "AUTO" ? "secondary" : "outline"}>
                              {entry.sourceType}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            {entry.readOnly ? (
                              <span className="text-gray-500">{entry.amount.toFixed(2)}</span>
                            ) : (
                              <Input
                                type="number"
                                value={editedAmounts[entry.glAccountId] ?? entry.amount}
                                onChange={(e) =>
                                  handleAmountChange(entry.glAccountId, parseFloat(e.target.value) || 0)
                                }
                                className="w-32"
                              />
                            )}
                          </TableCell>
                          <TableCell>
                            {entry.periodStatus && (
                              <Badge className={getStatusBadgeColor(entry.periodStatus)}>
                                {entry.periodStatus}
                              </Badge>
                            )}
                          </TableCell>
                          {session?.user?.role?.toUpperCase() === "ADMIN" && (
                            <TableCell>
                              {entry.periodStatus === "POSTED" && (
                                <div className="flex gap-2">
                                  <Button 
                                    size="sm" 
                                    variant="outline"
                                    onClick={() => handleApproveEntry(entry.entryId!)}
                                  >
                                    Approve
                                  </Button>
                                  <Button 
                                    size="sm" 
                                    variant="outline"
                                    onClick={() => handleRejectEntry(entry.entryId!)}
                                  >
                                    Reject
                                  </Button>
                                </div>
                              )}
                              {entry.periodStatus === "APPROVED" && (
                                <Button 
                                  size="sm" 
                                  variant="outline"
                                  onClick={() => handleLockEntry(entry.entryId!)}
                                >
                                  Lock
                                </Button>
                              )}
                            </TableCell>
                          )}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}

              <div className="flex gap-2 justify-end mt-4">
                {draftEntries.size > 0 && (
                  <Button onClick={handleSaveDrafts} variant="outline">
                    Save Drafts
                  </Button>
                )}
                {periodEntries.some((e) => e.periodStatus === "DRAFT") && (
                  <Button onClick={handleSubmitForApproval}>
                    Submit All for Approval
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Edit Account Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Edit GL Account</DialogTitle>
          </DialogHeader>
          {editingAccount && (
            <div className="space-y-4">
              <div>
                <Label>Code (Read-only)</Label>
                <Input value={formData.code} disabled />
              </div>
              <div>
                <Label htmlFor="edit-name">Name *</Label>
                <Input
                  id="edit-name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
              <div>
                <Label>Account Type (Read-only)</Label>
                <Input value={formData.accountType} disabled />
              </div>
              <div>
                <Label>Source (Read-only)</Label>
                <Input value={formData.source} disabled />
              </div>
              <div>
                <Label htmlFor="edit-sectionLabel">Section Label</Label>
                <Input
                  id="edit-sectionLabel"
                  value={formData.sectionLabel}
                  onChange={(e) => setFormData({ ...formData, sectionLabel: e.target.value })}
                />
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="edit-periodSensitive"
                  checked={formData.periodSensitive}
                  onChange={(e) => setFormData({ ...formData, periodSensitive: e.target.checked })}
                  disabled={formData.source !== "MANUAL_ENTRY"}
                />
                <Label htmlFor="edit-periodSensitive" className="cursor-pointer">
                  Period Sensitive
                </Label>
              </div>
              <div>
                <Label htmlFor="edit-displayOrder">Display Order</Label>
                <Input
                  id="edit-displayOrder"
                  type="number"
                  value={formData.displayOrder}
                  onChange={(e) => setFormData({ ...formData, displayOrder: e.target.value })}
                />
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="edit-isActive"
                  checked={editingAccount.isActive}
                  onChange={(e) => setEditingAccount({ ...editingAccount, isActive: e.target.checked })}
                />
                <Label htmlFor="edit-isActive" className="cursor-pointer">
                  Active
                </Label>
              </div>
              <div className="flex gap-2 justify-end">
                <Button variant="outline" onClick={() => setEditDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleUpdateAccount}>Update Account</Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
