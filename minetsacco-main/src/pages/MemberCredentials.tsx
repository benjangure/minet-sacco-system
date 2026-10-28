import { useState, useEffect, useMemo } from 'react';
import { useAuth } from "@/contexts/AuthContext";
import { nativeFetch } from '@/utils/nativeHttp';
import { useRefresh } from "@/contexts/RefreshContext";
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  Eye,
  EyeOff,
  Copy,
  Check,
  AlertCircle,
  Search,
  RefreshCw
} from 'lucide-react';
import { formatName } from '@/lib/utils';
import { useToast } from '@/hooks/use-toast';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { getApiBaseUrl } from "../config/api";

const API_BASE_URL = getApiBaseUrl();

interface MemberCredential {
  id: number;
  memberId: number;
  username: string;
  memberName: string;
  email?: string;
  hasNationalId: boolean;
  emailSent: boolean;
  emailSentAt?: string;
  passwordChanged: boolean;
  passwordChangedAt?: string;
  createdAt: string;
}

export default function MemberCredentials() {
  const [credentials, setCredentials] = useState<MemberCredential[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [syncing, setSyncing] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [hrOnly, setHrOnly] = useState(false);
  const [purging, setPurging] = useState(false);

  useEffect(() => {
    nativeFetch(`${API_BASE_URL}/auth/config`, { method: 'GET' })
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => setHrOnly(!!d?.data?.hrOnly))
      .catch(() => {});
  }, []);
  const [selectedCredential, setSelectedCredential] = useState<MemberCredential | null>(null);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [password, setPassword] = useState<string | null>(null);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const { toast } = useToast();
  const { session, role } = useAuth();
  const canSync = role === 'ADMIN' || role === 'TREASURER';
  const { refreshKey } = useRefresh();

  useEffect(() => {
    if (session) {
      fetchCredentials();
    }
  }, [session, refreshKey]);

  const fetchCredentials = async () => {
    setLoading(true);
    
    // Verify session exists before fetching
    if (!session || !session.token) {
      toast({
        title: "Not Authenticated",
        description: "Please login to access credentials",
        variant: "destructive",
      });
      setLoading(false);
      return;
    }
    
    try {
      const response = await nativeFetch(`${API_BASE_URL}/member-credentials`, {
        headers: {
          "Authorization": `Bearer ${session.token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const data = await response.json();
        setCredentials(data.data || []);
      } else if (response.status === 401) {
        toast({
          title: "Unauthorized",
          description: "Your session has expired. Please login again.",
          variant: "destructive",
        });
      } else {
        throw new Error('Failed to fetch credentials');
      }
    } catch (error) {
      console.error('Error fetching credentials:', error);
      toast({
        title: "Error",
        description: "Failed to load member credentials",
        variant: "destructive",
      });
    }
    setLoading(false);
  };

  const handleViewCredential = async (credential: MemberCredential) => {
    setSelectedCredential(credential);
    
    if (!session || !session.token) {
      toast({
        title: "Not Authenticated",
        description: "Please login to view credentials",
        variant: "destructive",
      });
      return;
    }
    
    try {
      const response = await nativeFetch(`${API_BASE_URL}/member-credentials/${credential.id}/password`, {
        headers: {
          "Authorization": `Bearer ${session.token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const data = await response.json();
        setPassword(data.data?.password || null);
        setShowPasswordModal(true);
      } else if (response.status === 401) {
        toast({
          title: "Unauthorized",
          description: "Your session has expired. Please login again.",
          variant: "destructive",
        });
      } else {
        throw new Error('Failed to fetch password');
      }
    } catch (error) {
      console.error('Error fetching password:', error);
      toast({
        title: "Error",
        description: "Failed to load password",
        variant: "destructive",
      });
      setPassword(null);
      setShowPasswordModal(true);
    }
  };

  const copyToClipboard = (text: string, field: string) => {
    // Try modern clipboard API first
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        setCopiedField(field);
        toast({
          title: "Copied",
          description: `${field === "username" ? "Username" : "Password"} copied to clipboard`,
        });
        setTimeout(() => setCopiedField(null), 2000);
      }).catch((err) => {
        console.error('Clipboard API failed:', err);
        fallbackCopyToClipboard(text, field);
      });
    } else {
      // Fallback for non-HTTPS contexts
      fallbackCopyToClipboard(text, field);
    }
  };

  const fallbackCopyToClipboard = (text: string, field: string) => {
    // Create a temporary textarea
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
      document.execCommand('copy');
      setCopiedField(field);
      toast({
        title: "Copied",
        description: `${field === "username" ? "Username" : "Password"} copied to clipboard`,
      });
      setTimeout(() => setCopiedField(null), 2000);
    } catch (err) {
      console.error('Fallback copy failed:', err);
      toast({
        title: "Copy Failed",
        description: "Please manually select and copy the text",
        variant: "destructive",
      });
    }
    
    textArea.remove();
  };

  const filteredCredentials = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return credentials;
    return credentials.filter((c) =>
      c.memberName?.toLowerCase().includes(q) ||
      c.username?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q)
    );
  }, [credentials, search]);

  const handleResetPassword = async () => {
    if (!selectedCredential || !session?.token) return;
    const name = formatName(selectedCredential.memberName);
    if (!window.confirm(`Reset the password for ${name}? They will be given a new temporary password and must choose their own at next login.`)) {
      return;
    }
    setResetting(true);
    try {
      const response = await nativeFetch(`${API_BASE_URL}/member-credentials/${selectedCredential.id}/reset-password`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${session.token}`,
          "Content-Type": "application/json",
        },
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data?.message || 'Reset failed');
      }
      setPassword(data.data?.password || null);
      setShowPassword(true);
      setSelectedCredential({ ...selectedCredential, passwordChanged: false });
      toast({
        title: "Password reset",
        description: "A new temporary password is shown. Share it with the member securely.",
      });
      await fetchCredentials();
    } catch (error: any) {
      toast({
        title: "Reset failed",
        description: error?.message || "Could not reset the password",
        variant: "destructive",
      });
    }
    setResetting(false);
  };

  const handlePurgePasswords = async () => {
    if (!session?.token) return;
    const typed = window.prompt(
      'This permanently removes every MEMBER password stored in this system (staff are not affected). ' +
      'Members will sign in through HR only. This cannot be undone. ' +
      'Type DELETE MEMBER PASSWORDS to continue.'
    );
    if (typed !== 'DELETE MEMBER PASSWORDS') return;
    setPurging(true);
    try {
      const response = await nativeFetch(`${API_BASE_URL}/member-credentials/purge-passwords`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${session.token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ confirm: 'DELETE MEMBER PASSWORDS' }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data?.message || 'Request failed');
      toast({
        title: 'Member passwords removed',
        description: `${data.data?.memberLoginsCleared ?? 0} logins and ${data.data?.credentialRecordsCleared ?? 0} dashboard records cleared.`,
      });
      await fetchCredentials();
    } catch (error: any) {
      toast({ title: 'Not done', description: error?.message || 'Could not remove passwords', variant: 'destructive' });
    }
    setPurging(false);
  };

  const handleSync = async () => {
    if (!session?.token) return;
    setSyncing(true);
    try {
      const response = await nativeFetch(`${API_BASE_URL}/member-credentials/sync`, {
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${session.token}`,
          "Content-Type": "application/json",
        },
      });
      if (!response.ok) throw new Error('Sync failed');
      const data = await response.json();
      const r = data.data || {};
      toast({
        title: "Credentials synchronised",
        description: `${r.created ?? 0} missing record(s) created, ${r.namesNormalised ?? 0} name(s) corrected.`,
      });
      await fetchCredentials();
    } catch (error) {
      toast({ title: "Sync failed", description: "Could not synchronise member credentials", variant: "destructive" });
    }
    setSyncing(false);
  };

  const getStatusBadge = (credential: MemberCredential) => {
    if (credential.passwordChanged) {
      return <Badge className="bg-green-100 text-green-800">Password Changed</Badge>;
    } else if (credential.emailSent) {
      return <Badge className="bg-blue-100 text-blue-800">Email Sent</Badge>;
    } else {
      return <Badge className="bg-yellow-100 text-yellow-800">Pending Delivery</Badge>;
    }
  };

  if (loading && credentials.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold">Member Credentials Dashboard</h1>
          <p className="text-muted-foreground">View and manage member login credentials</p>
        </div>
        {role === 'ADMIN' && hrOnly && (
          <Button variant="destructive" onClick={handlePurgePasswords} disabled={purging} className="w-full sm:w-auto">
            {purging ? 'Removing...' : 'Remove stored member passwords'}
          </Button>
        )}
        {canSync && (
          <Button variant="outline" onClick={handleSync} disabled={syncing} className="w-full sm:w-auto">
            <RefreshCw className={`h-4 w-4 mr-2 ${syncing ? 'animate-spin' : ''}`} />
            {syncing ? 'Syncing...' : 'Sync missing credentials'}
          </Button>
        )}
      </div>

      {/* Info Alert */}
      <Alert>
        <AlertCircle className="h-4 w-4" />
        <AlertDescription>
          Member credentials are automatically created during registration or bulk upload. Use this dashboard to retrieve and share credentials with members.
          Members are required to set a new password on their first login.
        </AlertDescription>
      </Alert>

      {/* Search */}
      <Card className="border-none shadow-sm">
        <CardContent className="pt-6">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by member name, username, or email..."
                className="pl-10"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Credentials Table */}
      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="text-left p-3 font-semibold">Member Name</th>
                  <th className="text-left p-3 font-semibold">Username</th>
                  <th className="text-left p-3 font-semibold">Email</th>
                  <th className="text-left p-3 font-semibold">Password Type</th>
                  <th className="text-left p-3 font-semibold">Status</th>
                  <th className="text-left p-3 font-semibold">Created</th>
                  <th className="text-left p-3 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCredentials.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center py-8 text-muted-foreground">
                      No member credentials found
                    </td>
                  </tr>
                ) : (
                  filteredCredentials.map((credential) => (
                    <tr key={credential.id} className="border-b hover:bg-muted/50">
                      <td className="p-3 font-medium">{formatName(credential.memberName)}</td>
                      <td className="p-3 font-mono text-sm">{credential.username}</td>
                      <td className="p-3">{credential.email || "—"}</td>
                      <td className="p-3">
                        <Badge variant={credential.hasNationalId ? "secondary" : "outline"}>
                          {credential.hasNationalId ? "National ID" : "Generated"}
                        </Badge>
                      </td>
                      <td className="p-3">
                        {getStatusBadge(credential)}
                      </td>
                      <td className="p-3 text-sm">{new Date(credential.createdAt).toLocaleDateString()}</td>
                      <td className="p-3">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleViewCredential(credential)}
                          title="View Credentials"
                          className="text-blue-600"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile card list */}
          <div className="md:hidden divide-y">
            {filteredCredentials.length === 0 ? (
              <p className="text-center py-8 text-muted-foreground">No member credentials found</p>
            ) : (
              filteredCredentials.map((credential) => (
                <div key={credential.id} className="p-4 flex items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <p className="font-semibold truncate">{formatName(credential.memberName)}</p>
                    <p className="font-mono text-sm text-muted-foreground">{credential.username}</p>
                    <p className="text-xs text-muted-foreground truncate">{credential.email || "—"}</p>
                    <div className="pt-1">{getStatusBadge(credential)}</div>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleViewCredential(credential)}
                    title="View Credentials"
                    className="text-blue-600 shrink-0"
                  >
                    <Eye className="h-5 w-5" />
                  </Button>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>

      {/* Credential Detail Modal */}
      <Dialog open={showPasswordModal} onOpenChange={setShowPasswordModal}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Member Credentials</DialogTitle>
            <DialogDescription>
              View and copy member login credentials
            </DialogDescription>
          </DialogHeader>

          {selectedCredential && (
            <div className="space-y-6 py-4">
              {/* Member Info */}
              <div className="bg-slate-50 p-4 rounded-lg space-y-2">
                <div className="text-sm">
                  <p className="text-muted-foreground">Member Name</p>
                  <p className="font-semibold">{formatName(selectedCredential.memberName)}</p>
                </div>
                <div className="text-sm">
                  <p className="text-muted-foreground">Email</p>
                  <p className="font-semibold">{selectedCredential.email || "Not provided"}</p>
                </div>
              </div>

              {/* Credentials */}
              <div className="space-y-4">
                {/* Username */}
                <div className="space-y-2">
                  <p className="text-sm font-medium">Username (Login ID)</p>
                  <div className="flex gap-2">
                    <Input
                      type="text"
                      value={selectedCredential.username}
                      readOnly
                      className="font-mono"
                    />
                    <Button
                      type="button"
                      size="icon"
                      variant="outline"
                      onClick={() => copyToClipboard(selectedCredential.username, "username")}
                      className="shrink-0"
                    >
                      {copiedField === "username" ? (
                        <Check className="h-4 w-4 text-green-600" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                </div>

                {/* Password */}
                {password ? (
                  <div className="space-y-2">
                    <p className="text-sm font-medium">
                      {selectedCredential.hasNationalId ? "Temporary password (National ID)" : "Temporary password"}
                    </p>
                    <div className="flex gap-2">
                      <Input
                        type={showPassword ? "text" : "password"}
                        value={password}
                        readOnly
                        className="font-mono"
                      />
                      <Button
                        type="button"
                        size="icon"
                        variant="outline"
                        onClick={() => setShowPassword(!showPassword)}
                        className="shrink-0"
                      >
                        {showPassword ? (
                          <EyeOff className="h-4 w-4" />
                        ) : (
                          <Eye className="h-4 w-4" />
                        )}
                      </Button>
                      <Button
                        type="button"
                        size="icon"
                        variant="outline"
                        onClick={() => copyToClipboard(password, "password")}
                        className="shrink-0"
                      >
                        {copiedField === "password" ? (
                          <Check className="h-4 w-4 text-green-600" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <Alert>
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription className="text-xs">
                      {selectedCredential.passwordChanged
                        ? "The member has set their own password, so it cannot be shown. Use Reset password if they cannot sign in."
                        : "The original password for this login is not on record. Use Reset password to issue a new temporary password."}
                    </AlertDescription>
                  </Alert>
                )}
              </div>

              {/* Status Info */}
              <Alert className="bg-blue-50 border-blue-200">
                <AlertCircle className="h-4 w-4 text-blue-600" />
                <AlertDescription className="text-xs text-blue-800">
                  <strong>Status:</strong>
                  <ul className="list-disc list-inside mt-1 space-y-1">
                    <li>Password Changed: {selectedCredential.passwordChanged ? "Yes" : "No"}</li>
                    <li>Email Sent: {selectedCredential.emailSent ? "Yes" : "No"}</li>
                    <li>Created: {new Date(selectedCredential.createdAt).toLocaleDateString()}</li>
                  </ul>
                </AlertDescription>
              </Alert>
            </div>
          )}

          <div className="flex flex-col-reverse gap-2 sm:flex-row">
            <Button onClick={() => setShowPasswordModal(false)} variant="outline" className="flex-1">
              Close
            </Button>
            {!hrOnly && (
            <Button onClick={handleResetPassword} disabled={resetting} className="flex-1">
              <RefreshCw className={`h-4 w-4 mr-2 ${resetting ? 'animate-spin' : ''}`} />
              {resetting ? 'Resetting...' : 'Reset password'}
            </Button>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}