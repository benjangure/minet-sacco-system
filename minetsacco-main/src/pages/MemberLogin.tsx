import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, LogIn, Eye, EyeOff, Settings, ChevronDown, ChevronUp, CheckCircle } from 'lucide-react';
import logo from '@/assets/images/logo.png';
import { BackendConnectionManager } from '@/components/BackendConnectionManager';
import { getBackendUrl, setBackendUrl } from '@/config/api';

export default function MemberLogin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [accessError, setAccessError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [backendUrl, setBackendUrlLocal] = useState('');
  const [tempUrl, setTempUrl] = useState('');
  const [testingConnection, setTestingConnection] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { memberSignIn } = useAuth();

  useEffect(() => {
    // Check if there's an access error from ProtectedRoute
    const error = localStorage.getItem('accessError');
    if (error) {
      setAccessError(error);
      localStorage.removeItem('accessError');
    }

    // Check for success message from password setup
    if (location.state?.message) {
      setSuccessMessage(location.state.message);
    }

    // Load current backend URL
    const currentUrl = getBackendUrl();
    setBackendUrlLocal(currentUrl);
    setTempUrl(currentUrl);
  }, [location.state]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const result = await memberSignIn(username, password);

      if (result.error) {
        setError(result.error.message);
        return;
      }

      if (result.firstLogin) {
        // Redirect to password setup page with credentials
        navigate('/member/password-setup', { 
          state: { 
            username: result.username, 
            currentPassword: result.password 
          },
          replace: true
        });
        return;
      }

      // Normal login successful - redirect to dashboard
      navigate('/member/dashboard');
    } catch (err: any) {
      console.error('Login error:', err);
      
      let errorMsg = 'Login failed. Please check your credentials.';
      
      if (err.response?.status === 0) {
        errorMsg = 'Cannot connect to server. Check if backend is running and on same network.';
      } else if (err.response?.data?.message) {
        errorMsg = err.response.data.message;
      } else if (err.message) {
        errorMsg = err.message;
      }
      
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

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
      setConnectionStatus({ type: 'error', text: 'Invalid URL format' });
      return;
    }

    setTestingConnection(true);
    try {
      // Test the health check endpoint (no authentication required)
      const response = await fetch(`${tempUrl}/api/auth/health`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      });

      if (response.ok) {
        const data = await response.json();
        setConnectionStatus({ type: 'success', text: `✓ Backend is reachable! (${tempUrl})` });
      } else if (response.status === 404) {
        setConnectionStatus({ type: 'error', text: 'Backend not found at this URL' });
      } else {
        setConnectionStatus({ type: 'error', text: `Server error: ${response.status}` });
      }
    } catch (error) {
      setConnectionStatus({ type: 'error', text: 'Cannot reach backend - check URL and network' });
    } finally {
      setTestingConnection(false);
    }
  };

  const handleSaveUrl = () => {
    if (!validateUrl(tempUrl)) {
      setConnectionStatus({ type: 'error', text: 'Invalid URL format' });
      return;
    }

    setBackendUrl(tempUrl);
    setBackendUrlLocal(tempUrl);
    setConnectionStatus({ type: 'success', text: 'Backend URL updated!' });
    setTimeout(() => {
      setConnectionStatus(null);
      setShowSettings(false);
    }, 1500);
  };

  return (
    <>
      <div className="min-h-screen bg-gradient-to-br from-primary/10 to-primary/5 flex items-center justify-center p-4">
        <div className="w-full max-w-md">
          <Card className="border-none shadow-lg">
            <CardHeader className="space-y-2 text-center">
              <div className="flex justify-center mb-4">
                <img src={logo} alt="Minet SACCO" className="h-16 w-auto" />
              </div>
              <CardTitle className="text-2xl">Minet SACCO</CardTitle>
              <p className="text-sm text-muted-foreground">Member Portal</p>
            </CardHeader>

            <CardContent className="space-y-6">
              {/* Backend Settings Toggle Button - Commented out for production deployment */}
              {/* 
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setShowSettings(!showSettings)}
                className="w-full gap-2"
              >
                <Settings className="h-4 w-4" />
                {showSettings ? 'Hide Settings' : 'Backend Settings'}
                {showSettings ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
              </Button>

              {/* Settings Panel - Commented out for production */}
              {/* {showSettings && (
                <div className="space-y-4 p-4 bg-muted rounded-lg border">
                  <div className="space-y-2">
                    <Label className="text-xs text-muted-foreground">Current Backend</Label>
                    <div className="p-2 bg-background rounded text-xs font-mono break-all">
                      {backendUrl}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="backend-url" className="text-sm">Backend URL</Label>
                    <Input
                      id="backend-url"
                      type="text"
                      value={tempUrl}
                      onChange={(e) => setTempUrl(e.target.value)}
                      placeholder="http://192.168.0.41:8080"
                      className="text-xs font-mono"
                    />
                    <p className="text-xs text-muted-foreground">
                      Format: http://IP:PORT or https://domain
                    </p>
                  </div>

                  {connectionStatus && (
                    <Alert variant={connectionStatus.type === 'error' ? 'destructive' : 'default'}>
                      {connectionStatus.type === 'error' ? (
                        <AlertCircle className="h-4 w-4" />
                      ) : (
                        <CheckCircle className="h-4 w-4" />
                      )}
                      <AlertDescription className="text-xs">{connectionStatus.text}</AlertDescription>
                    </Alert>
                  )}

                  <div className="flex gap-2">
                    <Button
                      type="button"
                      onClick={testConnection}
                      disabled={testingConnection}
                      variant="outline"
                      size="sm"
                      className="flex-1 text-xs"
                    >
                      {testingConnection ? 'Testing...' : 'Test'}
                    </Button>
                    <Button
                      type="button"
                      onClick={handleSaveUrl}
                      disabled={testingConnection || tempUrl === backendUrl}
                      size="sm"
                      className="flex-1 text-xs"
                    >
                      Save
                    </Button>
                  </div>
                </div>
              )}
              */}

              <form onSubmit={handleLogin} className="space-y-4">
                {successMessage && (
                  <Alert>
                    <CheckCircle className="h-4 w-4" />
                    <AlertDescription className="text-green-700">{successMessage}</AlertDescription>
                  </Alert>
                )}
                {accessError && (
                  <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                    <p className="text-sm text-red-700">{accessError}</p>
                  </div>
                )}
                <div className="space-y-2">
                  <Label htmlFor="username">Phone Number or Employee ID</Label>
                  <Input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="e.g., 0722123456 or EMP001"
                    required
                    disabled={loading}
                    className="h-10"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">National ID (Initial Password)</Label>
                  <div className="relative">
                    <Input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Your National ID"
                      required
                      disabled={loading}
                      className="h-10 pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      disabled={loading}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground disabled:opacity-50"
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>

                {error && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{error}</AlertDescription>
                  </Alert>
                )}

                <Button 
                  type="submit" 
                  className="w-full h-10 gap-2"
                  disabled={loading}
                >
                  <LogIn className="h-4 w-4" />
                  {loading ? 'Logging in...' : 'Login'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* BackendConnectionManager component commented out for production deployment */}
      {/* 
      <BackendConnectionManager 
        showOnMount={false}
        onConnectionSuccess={() => {
          // Refresh the backend URL after successful connection
          const currentUrl = getBackendUrl();
          setBackendUrlLocal(currentUrl);
          setTempUrl(currentUrl);
        }}
      />
      */}
    </>
  );
}
