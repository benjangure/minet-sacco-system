import React, { useState, useEffect } from 'react';
import { useRefresh } from '@/contexts/RefreshContext';
import {
  AlertCircle,
  Plus,
  CheckCircle,
  XCircle,
  Trash2,
  RefreshCw,
} from 'lucide-react';
import glManualEntryService, {
  GLManualEntry,
  GLManualEntryRequest,
  GLAccount,
} from '../services/glManualEntryService';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Alert, AlertDescription } from '@/components/ui/alert';

export default function GLManualEntries() {
  const { session } = useAuth();
  const { refreshKey } = useRefresh();
  const [entries, setEntries] = useState<GLManualEntry[]>([]);
  const [glAccounts, setGlAccounts] = useState<GLAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'pending'>('pending');
  const role = session?.user?.role?.toUpperCase() ?? '';
  const isAdmin = role === 'ADMIN';
  const isTreasurer = role === 'TREASURER';

  // Form state
  const [formData, setFormData] = useState<GLManualEntryRequest>({
    glAccountId: 0,
    entryDate: new Date().toISOString().split('T')[0],
    description: '',
    amount: 0,
    isDebit: true,
    entryReason: 'ACCRUAL',
  });

  useEffect(() => {
    if (session?.user) {
      loadGLAccounts();
      loadEntries();
    }
  }, [activeTab, session, refreshKey]);

  const loadGLAccounts = async () => {
    try {
      const accounts = await glManualEntryService.getGLAccounts();
      setGlAccounts(accounts);
    } catch (err: any) {
      if (err.response?.status !== 401 && err.response?.status !== 403) {
        setError('Failed to load GL accounts');
      }
    }
  };

  const loadEntries = async () => {
    setLoading(true);
    setError(null);
    try {
      const data =
        activeTab === 'pending'
          ? await glManualEntryService.getPendingEntries()
          : await glManualEntryService.getAllEntries();
      setEntries(data);
    } catch (err: any) {
      if (err.response?.status !== 401 && err.response?.status !== 403) {
        setError('Failed to load entries');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCreateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await glManualEntryService.createManualEntry(formData);
      setSuccess('Entry created and submitted for Admin approval.');
      setShowForm(false);
      setFormData({
        glAccountId: 0,
        entryDate: new Date().toISOString().split('T')[0],
        description: '',
        amount: 0,
        isDebit: true,
        entryReason: 'ACCRUAL',
      });
      loadEntries();
    } catch {
      setError('Failed to create entry');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveEntry = async (entry: GLManualEntry) => {
    setLoading(true);
    setError(null);
    try {
      if (entry.entrySource === 'PERIOD_ENTRY') {
        await glManualEntryService.approvePeriodEntry(entry.id);
      } else {
        await glManualEntryService.approveEntry(entry.id);
      }
      setSuccess('Entry approved — it is now included in GL calculations.');
      loadEntries();
    } catch {
      setError('Failed to approve entry');
    } finally {
      setLoading(false);
    }
  };

  const handleRejectEntry = async (entry: GLManualEntry) => {
    const rejectReason =
      entry.entrySource === 'PERIOD_ENTRY'
        ? window.prompt('Enter rejection reason (optional):') ?? ''
        : '';
    setLoading(true);
    setError(null);
    try {
      if (entry.entrySource === 'PERIOD_ENTRY') {
        await glManualEntryService.rejectPeriodEntry(entry.id, rejectReason);
      } else {
        await glManualEntryService.rejectEntry(entry.id);
      }
      setSuccess('Entry rejected and returned to Treasurer.');
      loadEntries();
    } catch {
      setError('Failed to reject entry');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEntry = async (entryId: number) => {
    if (!window.confirm('Delete this entry? This cannot be undone.')) return;
    setLoading(true);
    setError(null);
    try {
      await glManualEntryService.deleteEntry(entryId);
      setSuccess('Entry deleted.');
      loadEntries();
    } catch {
      setError('Failed to delete entry');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'DRAFT':    return 'bg-muted text-muted-foreground';
      case 'PENDING':  return 'bg-yellow-100 text-yellow-800';
      case 'POSTED':   return 'bg-accent text-accent-foreground';
      case 'APPROVED': return 'bg-green-100 text-green-800';
      case 'LOCKED':   return 'bg-purple-100 text-purple-800';
      case 'REJECTED': return 'bg-destructive/10 text-destructive';
      default:         return 'bg-muted text-muted-foreground';
    }
  };

  const getDisplayStatus = (entry: GLManualEntry) =>
    entry.workflowStatus || entry.periodStatus || entry.approvalStatus;

  const selectedAccount = glAccounts.find(a => a.id === formData.glAccountId);

  const pendingCount = activeTab === 'pending' ? entries.length : 0;

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-3xl font-bold text-foreground">GL Manual Entries</h1>
        <p className="text-muted-foreground">
          Enter accruals, adjustments, and other manual GL entries
        </p>
      </div>

      {/* Alerts */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {success && (
        <Alert className="border-green-200 bg-green-50 text-green-900">
          <CheckCircle className="h-4 w-4 text-green-600" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Treasurer pending notice */}
      {isTreasurer && activeTab === 'pending' && entries.length > 0 && (
        <Alert className="border-yellow-300 bg-yellow-50">
          <AlertCircle className="h-4 w-4 text-yellow-600" />
          <AlertDescription className="text-yellow-900">
            <strong>{entries.length} entry/entries</strong> are waiting for Admin approval and
            will not affect GL reports until approved. Ask your <strong>Admin</strong> to log in
            and approve from this page.
          </AlertDescription>
        </Alert>
      )}

      {/* Tabs + action buttons */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex gap-2">
          <button
            onClick={() => setActiveTab('pending')}
            className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === 'pending'
                ? 'bg-primary text-primary-foreground shadow-sm'
                : 'bg-muted text-muted-foreground hover:bg-muted/70'
            }`}
          >
            {isAdmin ? 'Awaiting My Approval' : 'Pending Approval'}
            {pendingCount > 0 && (
              <span className={`text-xs px-1.5 py-0.5 rounded-full font-bold ${
                activeTab === 'pending'
                  ? 'bg-primary-foreground/20 text-primary-foreground'
                  : 'bg-yellow-400 text-yellow-900'
              }`}>
                {pendingCount}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('all')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === 'all'
                ? 'bg-primary text-primary-foreground shadow-sm'
                : 'bg-muted text-muted-foreground hover:bg-muted/70'
            }`}
          >
            All Entries
          </button>
        </div>

        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadEntries} disabled={loading}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Refresh
          </Button>
          <Button size="sm" onClick={() => setShowForm(!showForm)}>
            <Plus className="mr-2 h-4 w-4" />
            New Entry
          </Button>
        </div>
      </div>

      {/* New Entry Form */}
      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>Create Manual GL Entry</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateEntry} className="space-y-4">
              <div className="grid sm:grid-cols-2 gap-4">
                {/* GL Account */}
                <div className="space-y-1.5">
                  <Label>GL Account *</Label>
                  <Select
                    value={formData.glAccountId ? formData.glAccountId.toString() : ''}
                    onValueChange={v => setFormData(p => ({ ...p, glAccountId: parseInt(v) }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select an account…" />
                    </SelectTrigger>
                    <SelectContent>
                      {glAccounts.map(acc => (
                        <SelectItem key={acc.id} value={acc.id.toString()}>
                          {acc.code} — {acc.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Entry Date */}
                <div className="space-y-1.5">
                  <Label>Entry Date *</Label>
                  <Input
                    type="date"
                    value={formData.entryDate}
                    onChange={e => setFormData(p => ({ ...p, entryDate: e.target.value }))}
                    required
                  />
                </div>

                {/* Amount */}
                <div className="space-y-1.5">
                  <Label>Amount (KES) *</Label>
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.amount || ''}
                    onChange={e => setFormData(p => ({ ...p, amount: parseFloat(e.target.value) || 0 }))}
                    placeholder="0.00"
                    required
                  />
                </div>

                {/* Entry Reason */}
                <div className="space-y-1.5">
                  <Label>Entry Reason *</Label>
                  <Select
                    value={formData.entryReason}
                    onValueChange={v => setFormData(p => ({ ...p, entryReason: v as any }))}
                  >
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ACCRUAL">Accrual</SelectItem>
                      <SelectItem value="ADJUSTMENT">Adjustment</SelectItem>
                      <SelectItem value="ALLOCATION">Allocation</SelectItem>
                      <SelectItem value="RECLASSIFICATION">Reclassification</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Debit / Credit */}
                <div className="space-y-1.5">
                  <Label>Entry Side *</Label>
                  <div className="flex gap-4 pt-1">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                      <input
                        type="radio"
                        checked={formData.isDebit === true}
                        onChange={() => setFormData(p => ({ ...p, isDebit: true }))}
                        className="accent-primary"
                      />
                      Debit
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                      <input
                        type="radio"
                        checked={formData.isDebit === false}
                        onChange={() => setFormData(p => ({ ...p, isDebit: false }))}
                        className="accent-primary"
                      />
                      Credit
                    </label>
                  </div>
                </div>
              </div>

              {/* Description */}
              <div className="space-y-1.5">
                <Label>Description</Label>
                <Textarea
                  value={formData.description}
                  onChange={e => setFormData(p => ({ ...p, description: e.target.value }))}
                  placeholder="Provide details about this entry…"
                  rows={3}
                />
              </div>

              {/* Summary preview */}
              {selectedAccount && (
                <div className="p-3 rounded-lg bg-accent border border-border text-sm space-y-1">
                  <p><span className="font-medium">Account:</span> {selectedAccount.code} — {selectedAccount.name}</p>
                  <p>
                    <span className="font-medium">Amount:</span>{' '}
                    KES {(formData.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}{' '}
                    <span className={formData.isDebit ? 'text-destructive' : 'text-green-700'}>
                      ({formData.isDebit ? 'Debit' : 'Credit'})
                    </span>
                  </p>
                  <p className="text-muted-foreground text-xs">
                    This entry will be created as PENDING and sent for Admin approval before affecting GL reports.
                  </p>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <Button type="submit" disabled={loading || !formData.glAccountId}>
                  Submit for Approval
                </Button>
                <Button type="button" variant="outline" onClick={() => setShowForm(false)}>
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Entries Table */}
      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          {loading && entries.length === 0 ? (
            <div className="p-12 text-center text-muted-foreground">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary mb-3" />
              <p>Loading entries…</p>
            </div>
          ) : entries.length === 0 ? (
            <div className="p-12 text-center text-muted-foreground">
              {activeTab === 'pending' ? (
                isAdmin ? (
                  <>
                    <CheckCircle className="mx-auto mb-3 h-10 w-10 text-green-400" />
                    <p className="font-medium">Nothing awaiting your approval</p>
                    <p className="text-xs mt-1">All manual entries have been processed.</p>
                  </>
                ) : (
                  <>
                    <CheckCircle className="mx-auto mb-3 h-10 w-10 text-muted-foreground/40" />
                    <p className="font-medium">No pending entries</p>
                    <p className="text-xs mt-1">
                      Create a new entry above — it will appear here once submitted for Admin approval.
                    </p>
                  </>
                )
              ) : (
                <>
                  <p className="font-medium">No entries found</p>
                  <p className="text-xs mt-1">No GL manual entries have been created yet.</p>
                </>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Account Code</TableHead>
                    <TableHead>Account Name</TableHead>
                    <TableHead className="text-center">Source</TableHead>
                    <TableHead className="text-center">Period</TableHead>
                    <TableHead>Reason</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                    <TableHead className="text-center">Type</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-center">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {entries.map(entry => {
                    const displayStatus = getDisplayStatus(entry);
                    const canAdminAct = isAdmin && (
                      (entry.entrySource === 'MANUAL_ENTRY' && displayStatus === 'PENDING') ||
                      (entry.entrySource === 'PERIOD_ENTRY'  && displayStatus === 'POSTED')
                    );
                    const canTreasurerDelete =
                      isTreasurer &&
                      entry.entrySource === 'MANUAL_ENTRY' &&
                      displayStatus === 'PENDING';

                    return (
                      <TableRow key={entry.id}>
                        <TableCell className="text-sm">
                          {new Date(entry.entryDate).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="font-mono text-sm">{entry.glAccountCode}</TableCell>
                        <TableCell className="text-sm">{entry.glAccountName}</TableCell>
                        <TableCell className="text-center">
                          <Badge variant="secondary" className="text-[10px]">
                            {entry.entrySource === 'PERIOD_ENTRY' ? 'Period Entry' : 'Manual Entry'}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-center text-sm text-muted-foreground">
                          {entry.periodMonth && entry.periodYear
                            ? `${entry.periodMonth}/${entry.periodYear}`
                            : '—'}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {entry.entryReason}
                        </TableCell>
                        <TableCell className="text-sm max-w-[200px] truncate" title={entry.description}>
                          {entry.description || '—'}
                        </TableCell>
                        <TableCell className="text-sm text-right font-medium">
                          KES {entry.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                        </TableCell>
                        <TableCell className="text-center">
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                            entry.isDebit
                              ? 'bg-destructive/10 text-destructive'
                              : 'bg-green-100 text-green-800'
                          }`}>
                            {entry.isDebit ? 'Debit' : 'Credit'}
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getStatusBadgeClass(displayStatus)}`}>
                            {displayStatus}
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex justify-center gap-1">
                            {canAdminAct && (
                              <>
                                <button
                                  onClick={() => handleApproveEntry(entry)}
                                  disabled={loading}
                                  title="Approve"
                                  className="p-1 rounded hover:bg-green-100 transition disabled:opacity-50"
                                >
                                  <CheckCircle size={16} className="text-green-600" />
                                </button>
                                <button
                                  onClick={() => handleRejectEntry(entry)}
                                  disabled={loading}
                                  title="Reject"
                                  className="p-1 rounded hover:bg-destructive/10 transition disabled:opacity-50"
                                >
                                  <XCircle size={16} className="text-destructive" />
                                </button>
                              </>
                            )}
                            {canTreasurerDelete && (
                              <button
                                onClick={() => handleDeleteEntry(entry.id)}
                                disabled={loading}
                                title="Delete"
                                className="p-1 rounded hover:bg-destructive/10 transition disabled:opacity-50"
                              >
                                <Trash2 size={16} className="text-destructive" />
                              </button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Role-aware footer */}
      <div className="p-4 rounded-xl border border-border bg-accent/50 space-y-2">
        {isTreasurer && (
          <div className="flex items-start gap-2">
            <AlertCircle size={15} className="text-primary mt-0.5 shrink-0" />
            <p className="text-sm text-foreground">
              <span className="font-semibold">Your role (Treasurer):</span> Create entries and delete your own PENDING entries.
              Once submitted, entries stay <span className="font-semibold">PENDING</span> until an <span className="font-semibold">Admin</span> approves them — only then do they appear in GL reports.
            </p>
          </div>
        )}
        {isAdmin && (
          <div className="flex items-start gap-2">
            <CheckCircle size={15} className="text-green-600 mt-0.5 shrink-0" />
            <p className="text-sm text-foreground">
              <span className="font-semibold">Your role (Admin):</span> Approve or reject entries in the <span className="font-semibold">Awaiting My Approval</span> tab.
              Manual entries are actionable when <span className="font-semibold">PENDING</span>; period entries when <span className="font-semibold">POSTED</span>.
              Approved entries are immediately included in GL calculations and all financial reports.
            </p>
          </div>
        )}
        <p className="text-xs text-muted-foreground border-t border-border/60 pt-2">
          <span className="font-medium">Workflow:</span> Manual Entry → PENDING → Admin approves → APPROVED (affects reports).
          Period Entry → DRAFT → Treasurer submits → POSTED → Admin approves → APPROVED → Admin locks → LOCKED.
        </p>
      </div>
    </div>
  );
}
