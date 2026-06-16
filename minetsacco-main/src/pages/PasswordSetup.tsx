import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '@/config/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, Lock, Eye, EyeOff, CheckCircle, X } from 'lucide-react';
import logo from '@/assets/images/logo.png';

interface LocationState {
  username: string;
  currentPassword: string;
}

export default function PasswordSetup() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState;

  // Redirect if no state provided
  if (!state?.username || !state?.currentPassword) {
    navigate('/member/login', { replace: true });
    return null;
  }

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Password validation
  const getPasswordValidation = (password: string) => {
    return {
      length: password.length >= 6,
      uppercase: /[A-Z]/.test(password),
      lowercase: /[a-z]/.test(password),
      number: /\d/.test(password),
      special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
    };
  };

  const validation = getPasswordValidation(newPassword);
  const isPasswordValid = Object.values(validation).every(v => v);
  const passwordsMatch = newPassword === confirmPassword && confirmPassword.length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!isPasswordValid) {
      setError('Please ensure your password meets all requirements');
      return;
    }

    if (!passwordsMatch) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);

    try {
      await api.post('/auth/member/setup-password', {
        username: state.username,
        currentPassword: state.currentPassword,
        newPassword: newPassword
      });

      // Password setup successful - redirect to login to use new password
      navigate('/member/login', {
        replace: true,
        state: { message: 'Password setup successful! Please login with your new password.' }
      });
    } catch (err: any) {
      console.error('Password setup error:', err);
      
      let errorMsg = 'Failed to setup password. Please try again.';
      
      if (err.response?.data?.message) {
        errorMsg = err.response.data.message;
      } else if (err.message) {
        errorMsg = err.message;
      }
      
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const ValidationItem = ({ isValid, text }: { isValid: boolean; text: string }) => (
    <div className={`flex items-center gap-2 text-sm ${isValid ? 'text-green-600' : 'text-gray-500'}`}>
      {isValid ? (
        <CheckCircle className="h-4 w-4" />
      ) : (
        <X className="h-4 w-4" />
      )}
      <span>{text}</span>
    </div>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 to-primary/5 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <Card className="border-none shadow-lg">
          <CardHeader className="space-y-2 text-center">
            <div className="flex justify-center mb-4">
              <img src={logo} alt="Minet SACCO" className="h-16 w-auto" />
            </div>
            <CardTitle className="text-2xl">Set Up Your Password</CardTitle>
            <p className="text-sm text-muted-foreground">
              Welcome! Please create a secure password for your account.
            </p>
          </CardHeader>

          <CardContent className="space-y-6">
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="new-password">New Password</Label>
                <div className="relative">
                  <Input
                    id="new-password"
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter your new password"
                    required
                    disabled={loading}
                    className="h-10 pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    disabled={loading}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground disabled:opacity-50"
                  >
                    {showNewPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>

                {/* Password Requirements */}
                {newPassword && (
                  <div className="space-y-2 p-3 bg-muted rounded-lg">
                    <p className="text-sm font-medium text-muted-foreground">Password must contain:</p>
                    <ValidationItem isValid={validation.length} text="At least 6 characters" />
                    <ValidationItem isValid={validation.uppercase} text="One uppercase letter (A-Z)" />
                    <ValidationItem isValid={validation.lowercase} text="One lowercase letter (a-z)" />
                    <ValidationItem isValid={validation.number} text="One number (0-9)" />
                    <ValidationItem isValid={validation.special} text="One special character (!@#$%^&*)" />
                  </div>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm Password</Label>
                <div className="relative">
                  <Input
                    id="confirm-password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm your new password"
                    required
                    disabled={loading}
                    className="h-10 pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    disabled={loading}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground disabled:opacity-50"
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>

                {/* Password Match Indicator */}
                {confirmPassword && (
                  <div className={`text-sm ${passwordsMatch ? 'text-green-600' : 'text-red-500'}`}>
                    {passwordsMatch ? (
                      <div className="flex items-center gap-1">
                        <CheckCircle className="h-4 w-4" />
                        Passwords match
                      </div>
                    ) : (
                      <div className="flex items-center gap-1">
                        <X className="h-4 w-4" />
                        Passwords do not match
                      </div>
                    )}
                  </div>
                )}
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
                disabled={loading || !isPasswordValid || !passwordsMatch}
              >
                <Lock className="h-4 w-4" />
                {loading ? 'Setting up...' : 'Set Password'}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}