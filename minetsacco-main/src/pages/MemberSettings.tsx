import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle, Settings } from 'lucide-react';
import { getBackendUrl, setBackendUrl } from '@/config/api';
import MemberLayout from '@/components/MemberLayout';

export default function MemberSettings() {
  const [backendUrl, setBackendUrlLocal] = useState('');
  const [tempUrl, setTempUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const navigate = useNavigate();

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

  return (
    <MemberLayout memberName="Member" onLogout={() => {
      localStorage.removeItem('token');
      navigate('/member');
    }}>
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold text-foreground flex items-center gap-2">
            <Settings className="h-8 w-8" />
            Settings
          </h1>
          <p className="text-muted-foreground">Configure your app settings</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Backend Configuration</CardTitle>
            <CardDescription>
              Change the backend server URL. This is useful when testing on different networks.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Current URL Display */}
            <div className="space-y-2">
              <Label className="text-sm text-muted-foreground">Current Backend URL</Label>
              <div className="p-3 bg-muted rounded-lg font-mono text-sm break-all">
                {backendUrl}
              </div>
            </div>

            {/* URL Input */}
            <div className="space-y-2">
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
            <div className="space-y-2">
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
            {message && (
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
            <div className="flex gap-3">
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
        <Card className="bg-blue-50 border-blue-200">
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
      </div>
    </MemberLayout>
  );
}
