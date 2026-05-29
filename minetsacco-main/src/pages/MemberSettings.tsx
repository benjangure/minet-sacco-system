import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle, Settings, Lock, Eye, EyeOff } from 'lucide-react';
import { getBackendUrl, setBackendUrl } from '@/config/api';
import MemberLayout from '@/components/MemberLayout';
import { useToast } from '@/hooks/use-toast';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

const API_BASE_URL = "http://localhost:8080/api";

export default function MemberSettings() {
  const [backendUrl, setBackendUrlLocal] = useState('');
  const [tempUrl, setTempUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const navigate = useNavigate();
  const { toast } = useToast();
  
  // Password change state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  useEffect(() => {
    const currentUrl = getBackendUrl();
    setBackendUrlLocal(currentUrl);
    setTempUrl(currentUrl);
  }, []);

  const validateUrl = (url: string): boolean => {
    try {
      new URL(url);
      return url.startsWith('http://') || url.startsWith('https://');
    } catch {
      return false;
    }
  };

  const testConnection = async () => {
    if (!validateUrl(tempUrl)) {
      setMessage({ type: 'error', text: 'Invalid URL format. Use http://IP:PORT or https://domain' });
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${tempUrl}/api/auth/member/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'test', password: 'test' })
      });

      // We expect a 400 or 401 error (bad credentials), which means the server is reachable
      if (response.status === 400 || response.status === 401 || response.status === 403) {
        setMessage({ type: 'success', text: 'Backend is reachable!' });
      } else if (response.ok) {
        setMessage({ type: 'success', text: 'Backend is reachable!' });
      } else {
        setMessage({ type: 'error', text: `Server responded with status ${response.status}` });
      }
    } catch (error) {
      setMessage({ type: 'error', text: 'Cannot reach backend. Check URL and network connection.' });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = () => {
    if (!validateUrl(tempUrl)) {
      setMessage({ type: 'error', text: 'Invalid URL format. Use http://IP:PORT or https://domain' });
      return;
    }

    setBackendUrl(tempUrl);
    setBackendUrlLocal(tempUrl);
    setMessage({ type: 'success', text: 'Backend URL updated successfully!' });
    
    // Clear message after 2 seconds
    setTimeout(() => setMessage(null), 2000);
  };

  const handleReset = () => {
    setTempUrl(backendUrl);
    setMessage(null);
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!currentPassword.trim()) {
      toast({ title: "Error", description: "Please enter your current password", variant: "destructive" });
      return;
    }

    if (!newPassword.trim()) {
      toast({ title: "Error", description: "Please enter a new password", variant: "destructive" });
      return;
    }

    if (newPassword !== confirmPassword) {
      toast({ title: "Error", description: "New passwords do not match", variant: "destructive" });
      return;
    }

    if (newPassword.length < 8) {
      toast({ title: "Error", description: "Password must be at least 8 characters", variant: "destructive" });
      return;
    }

    if (currentPassword === newPassword) {
      toast({ title: "Error", description: "New password must be different from current password", variant: "destructive" });
      return;
    }

    setPasswordLoading(true);
    try {
      // Get token from localStorage (member token storage)
      const token = localStorage.getItem('token');
      if (!token) {
        toast({ 
          title: "Error", 
          description: "Authentication token not found. Please log in again.", 
          variant: "destructive" 
        });
        setPasswordLoading(false);
        return;
      }

      const response = await fetch(`${API_BASE_URL}/member/change-password`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        toast({ 
          title: "Success", 
          description: data.message || "Password changed successfully" 
        });
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        setShowCurrentPassword(false);
        setShowNewPassword(false);
        setShowConfirmPassword(false);
      } else if (response.status === 401) {
        toast({ 
          title: "Session Expired", 
          description: "Your session has expired. Please log out and log back in to continue.", 
          variant: "destructive" 
        });
      } else {
        try {
          const error = await response.json();
          toast({ 
            title: "Error", 
            description: error.message || error.error || "Failed to change password", 
            variant: "destructive" 
          });
        } catch (jsonError) {
          // If response is not JSON, show generic error
          toast({ 
            title: "Error", 
            description: `Server error (${response.status}). Please try again.`, 
            variant: "destructive" 
          });
        }
      }
    } catch (error) {
      console.error("Password change error:", error);
      toast({ 
        title: "Error", 
        description: "Failed to change password. Please check your connection and try again.", 
        variant: "destructive" 
      });
    }
    setPasswordLoading(false);
  };

  return (
    <MemberLayout memberName="Member" onLogout={() => {
      localStorage.removeItem('token');
      navigate('/member');
    }}>
      <div className="max-w-4xl mx-auto space-y-6 pb-20">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-foreground flex items-center gap-2">
            <Settings className="h-8 w-8" />
            Settings
          </h1>
          <p className="text-muted-foreground">Configure your app settings and account</p>
        </div>

        <Tabs defaultValue="security" className="space-y-6">
          <TabsList>
            {/* Backend Configuration tab commented out for production deployment */}
            {/* <TabsTrigger value="backend">Backend Configuration</TabsTrigger> */}
            <TabsTrigger value="security">Security</TabsTrigger>
          </TabsList>

          {/* Backend Configuration section commented out - not needed in production */}
          {/* 
          <TabsContent value="backend" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Backend Configuration</CardTitle>
                <CardDescription>
                  Change the backend server URL. This is useful when testing on different networks.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Current URL Display */}
                {/* <div className="space-y-2">
                  <Label className="text-sm text-muted-foreground">Current Backend URL</Label>
                  <div className="p-3 bg-muted rounded-lg font-mono text-sm break-all">
                    {backendUrl}
                  </div>
                </div>

                {/* URL Input */}
                {/* <div className="space-y-2">
                  <Label htmlFor="backend-url">New Backend URL</Label>
                  <Input
                    id="backend-url"
                    type="text"
                    value={tempUrl}
                    onChange={(e) => setTempUrl(e.target.value)}
                    placeholder="http://192.168.0.41:8080"
                    className="font-mono text-sm"
                  />
                  <p className="text-xs text-muted-foreground">
                    Format: http://IP:PORT or https://domain
                  </p>
                </div>

                {/* Examples */}
                {/* <div className="space-y-2">
                  <Label className="text-sm text-muted-foreground">Common Examples</Label>
                  <div className="space-y-2">
                    <div className="p-2 bg-muted rounded text-xs font-mono">
                      http://192.168.0.50:8080
                    </div>
                    <div className="p-2 bg-muted rounded text-xs font-mono">
                      http://192.168.1.1:8080
                    </div>
                    <div className="p-2 bg-muted rounded text-xs font-mono">
                      https://api.minetsacco.com
                    </div>
                  </div>
                </div>

                {/* Messages */}
                {/* {message && (
                  <Alert variant={message.type === 'error' ? 'destructive' : 'default'}>
                    {message.type === 'error' ? (
                      <AlertCircle className="h-4 w-4" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    <AlertDescription>{message.text}</AlertDescription>
                  </Alert>
                )}

                {/* Buttons */}
                {/* <div className="flex gap-3">
                  <Button
                    onClick={testConnection}
                    disabled={loading}
                    variant="outline"
                    className="flex-1"
                  >
                    {loading ? 'Testing...' : 'Test Connection'}
                  </Button>
                  <Button
                    onClick={handleSave}
                    disabled={loading || tempUrl === backendUrl}
                    className="flex-1"
                  >
                    Save Changes
                  </Button>
                  <Button
                    onClick={handleReset}
                    disabled={tempUrl === backendUrl}
                    variant="ghost"
                  >
                    Reset
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Info Card */}
            {/* <Card className="bg-blue-50 border-blue-200">
              <CardHeader>
                <CardTitle className="text-base text-blue-900">How to Find Your Backend URL</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-blue-800 space-y-2">
                <p>
                  <strong>On your laptop:</strong> Run <code className="bg-white px-2 py-1 rounded">ipconfig</code> in PowerShell and look for "IPv4 Address" under your WiFi adapter.
                </p>
                <p>
                  <strong>Example:</strong> If your laptop IP is 192.168.0.50, use <code className="bg-white px-2 py-1 rounded">http://192.168.0.50:8080</code>
                </p>
                <p>
                  <strong>Production:</strong> When deployed, use your production domain like <code className="bg-white px-2 py-1 rounded">https://api.minetsacco.com</code>
                </p>
              </CardContent>
            </Card>
          </TabsContent>
          */}

          <TabsContent value="security" className="space-y-6">
            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Lock className="h-5 w-5" />
                  Change Password
                </CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleChangePassword} className="space-y-4">
                  <div className="space-y-2">
                    <Label>Current Password</Label>
                    <div className="relative">
                      <Input
                        type={showCurrentPassword ? "text" : "password"}
                        value={currentPassword}
                        onChange={e => setCurrentPassword(e.target.value)}
                        required
                        placeholder="Enter current password"
                      />
                      <button
                        type="button"
                        onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      >
                        {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label>New Password</Label>
                    <div className="relative">
                      <Input
                        type={showNewPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={e => setNewPassword(e.target.value)}
                        required
                        minLength={8}
                        placeholder="Enter new password (min 8 characters)"
                      />
                      <button
                        type="button"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      >
                        {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label>Confirm New Password</Label>
                    <div className="relative">
                      <Input
                        type={showConfirmPassword ? "text" : "password"}
                        value={confirmPassword}
                        onChange={e => setConfirmPassword(e.target.value)}
                        required
                        minLength={8}
                        placeholder="Confirm new password"
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      >
                        {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                  </div>
                  <Button type="submit" disabled={passwordLoading}>
                    {passwordLoading ? "Changing Password..." : "Change Password"}
                  </Button>
                </form>
              </CardContent>
            </Card>

            <Card className="border-none shadow-sm">
              <CardHeader>
                <CardTitle>Security Tips</CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li>• Use a strong password with at least 8 characters</li>
                  <li>• Include uppercase, lowercase, numbers, and special characters</li>
                  <li>• Don't share your password with anyone</li>
                  <li>• Change your password regularly</li>
                  <li>• Log out when you're done using the system</li>
                </ul>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </MemberLayout>
  );
}
