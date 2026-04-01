import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, LogIn, Eye, EyeOff } from 'lucide-react';
import logo from '@/assets/images/logo.png';

export default function MemberLogin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [accessError, setAccessError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    // Check if there's an access error from ProtectedRoute
    const error = localStorage.getItem('accessError');
    if (error) {
      setAccessError(error);
      localStorage.removeItem('accessError');
    }
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.post('/auth/member/login', {
        username,
        password
      });

      const token = response.data.token;
      const decoded: any = jwtDecode(token);

      // Verify user is a member
      if (decoded.role !== 'MEMBER') {
        setError('Invalid credentials for member login');
        setLoading(false);
        return;
      }

      // Store token and user info
      localStorage.setItem('token', token);
      localStorage.setItem('userRole', decoded.role);
      // Use memberId from token if available, otherwise use username
      if (decoded.memberId) {
        localStorage.setItem('memberId', decoded.memberId);
      }
      localStorage.setItem('username', decoded.sub);

      // Redirect to member dashboard
      navigate('/member/dashboard');
    } catch (err: any) {
      console.error('Login error:', err);
      console.error('Error response:', err.response);
      console.error('Error message:', err.message);
      
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

  return (
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
            <form onSubmit={handleLogin} className="space-y-4">
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
  );
}
