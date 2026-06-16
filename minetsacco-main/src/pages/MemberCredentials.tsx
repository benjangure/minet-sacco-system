import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  Users, 
  Mail, 
  Key, 
  CheckCircle, 
  Clock, 
  AlertCircle,
  Eye,
  EyeOff,
  Copy
} from 'lucide-react';
import api from '@/config/api';

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

interface Statistics {
  totalCredentials: number;
  emailsSent: number;
  passwordsChanged: number;
  pendingDelivery: number;
  pendingPasswordSetup: number;
}

export default function MemberCredentials() {
  const [credentials, setCredentials] = useState<MemberCredential[]>([]);
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'pending-delivery' | 'pending-setup'>('all');
  const [showPasswords, setShowPasswords] = useState<{ [key: number]: boolean }>({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchData();
    fetchStatistics();
  }, [filter]);

  const fetchData = async () => {
    setLoading(true);
    try {
      let endpoint = '/admin/member-credentials';
      if (filter === 'pending-delivery') {
        endpoint += '/pending-delivery';
      } else if (filter === 'pending-setup') {
        endpoint += '/pending-setup';
      }

      const response = await api.get(endpoint);
      setCredentials(response.data.data || []);
    } catch (err: any) {
      setError('Failed to load member credentials');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const response = await api.get('/admin/member-credentials/statistics');
      setStatistics(response.data.data);
    } catch (err: any) {
      console.error('Error fetching statistics:', err);
    }
  };

  const markAsDelivered = async (credentialId: number) => {
    try {
      await api.post(`/admin/member-credentials/${credentialId}/mark-delivered`);
      setSuccess('Credential marked as delivered successfully');
      fetchData();
      fetchStatistics();
    } catch (err: any) {
      setError('Failed to mark credential as delivered');
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setSuccess('Copied to clipboard!');
  };

  const toggleShowPassword = (id: number) => {
    setShowPasswords(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const getPasswordDisplay = (credential: MemberCredential) => {
    if (credential.hasNationalId) {
      return 'Use National ID';
    }
    // For security, we don't store actual temp passwords
    // This would need to be generated fresh or stored securely
    return 'Generated Password (Check Console)';
  };

  const getStatusBadge = (credential: MemberCredential) => {
    if (credential.passwordChanged) {
      return <Badge variant="default" className="bg-green-100 text-green-800">Completed</Badge>;
    } else if (credential.emailSent) {
      return <Badge variant="secondary">Delivered</Badge>;
    } else {
      return <Badge variant="destructive">Pending Delivery</Badge>;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Member Credentials</h1>
          <p className="text-muted-foreground">Manage member login credentials and delivery status</p>
        </div>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-2">
                <Users className="h-4 w-4 text-blue-600" />
                <div>
                  <p className="text-sm font-medium">Total Members</p>
                  <p className="text-2xl font-bold">{statistics.totalCredentials}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-2">
                <Mail className="h-4 w-4 text-green-600" />
                <div>
                  <p className="text-sm font-medium">Delivered</p>
                  <p className="text-2xl font-bold">{statistics.emailsSent}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-2">
                <Key className="h-4 w-4 text-purple-600" />
                <div>
                  <p className="text-sm font-medium">Password Set</p>
                  <p className="text-2xl font-bold">{statistics.passwordsChanged}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-2">
                <Clock className="h-4 w-4 text-orange-600" />
                <div>
                  <p className="text-sm font-medium">Pending Delivery</p>
                  <p className="text-2xl font-bold">{statistics.pendingDelivery}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center space-x-2">
                <AlertCircle className="h-4 w-4 text-red-600" />
                <div>
                  <p className="text-sm font-medium">Awaiting Setup</p>
                  <p className="text-2xl font-bold">{statistics.pendingPasswordSetup}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <div className="flex space-x-2">
        <Button 
          variant={filter === 'all' ? 'default' : 'outline'}
          onClick={() => setFilter('all')}
        >
          All Members
        </Button>
        <Button 
          variant={filter === 'pending-delivery' ? 'default' : 'outline'}
          onClick={() => setFilter('pending-delivery')}
        >
          Pending Delivery
        </Button>
        <Button 
          variant={filter === 'pending-setup' ? 'default' : 'outline'}
          onClick={() => setFilter('pending-setup')}
        >
          Awaiting Password Setup
        </Button>
      </div>

      {/* Alerts */}
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {success && (
        <Alert>
          <CheckCircle className="h-4 w-4" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Credentials Table */}
      <Card>
        <CardHeader>
          <CardTitle>Member Credentials ({credentials.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {credentials.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">No credentials found for the selected filter.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left p-2">Member</th>
                    <th className="text-left p-2">Username</th>
                    <th className="text-left p-2">Email</th>
                    <th className="text-left p-2">Password Type</th>
                    <th className="text-left p-2">Status</th>
                    <th className="text-left p-2">Created</th>
                    <th className="text-left p-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {credentials.map((credential) => (
                    <tr key={credential.id} className="border-b hover:bg-muted/50">
                      <td className="p-2">
                        <div>
                          <p className="font-medium">{credential.memberName}</p>
                          <p className="text-sm text-muted-foreground">ID: {credential.memberId}</p>
                        </div>
                      </td>
                      <td className="p-2">
                        <div className="flex items-center space-x-2">
                          <span className="font-mono">{credential.username}</span>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => copyToClipboard(credential.username)}
                          >
                            <Copy className="h-3 w-3" />
                          </Button>
                        </div>
                      </td>
                      <td className="p-2">
                        {credential.email ? (
                          <div className="flex items-center space-x-2">
                            <span className="text-sm">{credential.email}</span>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => copyToClipboard(credential.email || '')}
                            >
                              <Copy className="h-3 w-3" />
                            </Button>
                          </div>
                        ) : (
                          <span className="text-muted-foreground">No email</span>
                        )}
                      </td>
                      <td className="p-2">
                        <Badge variant={credential.hasNationalId ? 'secondary' : 'outline'}>
                          {credential.hasNationalId ? 'National ID' : 'Generated'}
                        </Badge>
                      </td>
                      <td className="p-2">
                        {getStatusBadge(credential)}
                      </td>
                      <td className="p-2">
                        <span className="text-sm">
                          {new Date(credential.createdAt).toLocaleDateString()}
                        </span>
                      </td>
                      <td className="p-2">
                        <div className="flex space-x-2">
                          {!credential.emailSent && (
                            <Button
                              size="sm"
                              onClick={() => markAsDelivered(credential.id)}
                            >
                              Mark Delivered
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Instructions */}
      <Card>
        <CardHeader>
          <CardTitle>Instructions for Admins</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h4 className="font-medium">For Members with National ID:</h4>
            <p className="text-sm text-muted-foreground">Tell them to use their National ID as the initial password.</p>
          </div>
          <div>
            <h4 className="font-medium">For Members with Generated Passwords:</h4>
            <p className="text-sm text-muted-foreground">
              Check the server console logs for temporary passwords, or wait for email functionality to be set up.
            </p>
          </div>
          <div>
            <h4 className="font-medium">After sharing credentials:</h4>
            <p className="text-sm text-muted-foreground">
              Click "Mark Delivered" to track that you've shared the credentials with the member.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}