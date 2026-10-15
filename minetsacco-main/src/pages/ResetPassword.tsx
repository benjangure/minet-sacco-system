import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle2, AlertCircle, Eye, EyeOff } from "lucide-react";
import { getApiBaseUrl } from "@/config/api";
import { nativeFetch } from "@/utils/nativeHttp";

interface TokenInfo {
  valid: boolean;
  username?: string;
  email?: string;
  resetType?: string;
  expiresAt?: string;
  message: string;
}

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [tokenInfo, setTokenInfo] = useState<TokenInfo | null>(null);
  const [passwordErrors, setPasswordErrors] = useState<string[]>([]);
  const { toast } = useToast();
  const navigate = useNavigate();
  const token = searchParams.get("token");

  // Password validation rules
  const validatePassword = (pwd: string): string[] => {
    const errors: string[] = [];
    if (pwd.length < 8) errors.push("At least 8 characters");
    if (!/[A-Z]/.test(pwd)) errors.push("At least one uppercase letter");
    if (!/[a-z]/.test(pwd)) errors.push("At least one lowercase letter");
    if (!/\d/.test(pwd)) errors.push("At least one number");
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)) errors.push("At least one special character");
    return errors;
  };

  useEffect(() => {
    const validateToken = async () => {
      if (!token) {
        toast({
          title: "Invalid Link",
          description: "Password reset link is invalid or missing",
          variant: "destructive"
        });
        setTimeout(() => navigate("/login"), 2000);
        return;
      }

      try {
        const response = await nativeFetch(`${getApiBaseUrl()}/auth/password-reset/validate-token`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ token }),
        });

        const data: TokenInfo = await response.json();

        if (!data.valid) {
          toast({
            title: "Link Expired or Invalid",
            description: data.message || "This password reset link has expired. Please request a new one.",
            variant: "destructive"
          });
          setTimeout(() => navigate("/login"), 2000);
        } else {
          setTokenInfo(data);
        }
      } catch (err) {
        console.error("Token validation error:", err);
        toast({
          title: "Validation Error",
          description: "Failed to validate reset link. Please try again.",
          variant: "destructive"
        });
        setTimeout(() => navigate("/login"), 2000);
      } finally {
        setValidating(false);
      }
    };

    validateToken();
  }, [token, navigate, toast]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordErrors([]);

    // Validate passwords match
    if (password !== confirmPassword) {
      toast({
        title: "Error",
        description: "Passwords do not match",
        variant: "destructive"
      });
      return;
    }

    // Validate password strength
    const errors = validatePassword(password);
    if (errors.length > 0) {
      setPasswordErrors(errors);
      toast({
        title: "Password Not Strong Enough",
        description: "Please ensure your password meets all requirements",
        variant: "destructive"
      });
      return;
    }

    if (!token) {
      toast({
        title: "Error",
        description: "Invalid reset token",
        variant: "destructive"
      });
      return;
    }

    setLoading(true);

    try {
      const response = await nativeFetch(`${getApiBaseUrl()}/auth/password-reset/reset-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token,
          newPassword: password
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Failed to reset password");
      }

      toast({
        title: "Success",
        description: data.message || "Your password has been reset successfully!",
      });

      setTimeout(() => {
        const redirectUrl = tokenInfo?.resetType === "MEMBER" ? "/member/login" : "/login";
        navigate(redirectUrl);
      }, 2000);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : "Failed to reset password";
      toast({
        title: "Error",
        description: errorMsg,
        variant: "destructive"
      });
      console.error("Password reset error:", err);
    } finally {
      setLoading(false);
    }
  };

  if (validating) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 to-primary/10 p-4">
        <Card className="w-full max-w-md">
          <CardContent className="pt-8">
            <div className="flex justify-center mb-4">
              <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full"></div>
            </div>
            <p className="text-center text-muted-foreground">Validating reset link...</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!tokenInfo?.valid) {
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 to-primary/10 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">Reset Your Password</CardTitle>
          <CardDescription>
            {tokenInfo.username && `Resetting password for: ${tokenInfo.username}`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* New Password Field */}
            <div className="space-y-2">
              <Label htmlFor="password">New Password</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter new password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (e.target.value) {
                      setPasswordErrors(validatePassword(e.target.value));
                    }
                  }}
                  required
                  disabled={loading}
                  className="pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-2.5 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            {/* Password Requirements */}
            {password && (
              <div className="bg-gray-50 border border-gray-200 rounded-md p-3 space-y-2">
                <p className="text-xs font-medium text-gray-700">Password Requirements:</p>
                <ul className="text-xs space-y-1">
                  <li className={`flex items-center gap-2 ${password.length >= 8 ? 'text-green-600' : 'text-red-600'}`}>
                    <span className={`h-1 w-1 rounded-full ${password.length >= 8 ? 'bg-green-600' : 'bg-red-600'}`}></span>
                    At least 8 characters
                  </li>
                  <li className={`flex items-center gap-2 ${/[A-Z]/.test(password) ? 'text-green-600' : 'text-red-600'}`}>
                    <span className={`h-1 w-1 rounded-full ${/[A-Z]/.test(password) ? 'bg-green-600' : 'bg-red-600'}`}></span>
                    One uppercase letter (A-Z)
                  </li>
                  <li className={`flex items-center gap-2 ${/[a-z]/.test(password) ? 'text-green-600' : 'text-red-600'}`}>
                    <span className={`h-1 w-1 rounded-full ${/[a-z]/.test(password) ? 'bg-green-600' : 'bg-red-600'}`}></span>
                    One lowercase letter (a-z)
                  </li>
                  <li className={`flex items-center gap-2 ${/\d/.test(password) ? 'text-green-600' : 'text-red-600'}`}>
                    <span className={`h-1 w-1 rounded-full ${/\d/.test(password) ? 'bg-green-600' : 'bg-red-600'}`}></span>
                    One number (0-9)
                  </li>
                  <li className={`flex items-center gap-2 ${/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password) ? 'text-green-600' : 'text-red-600'}`}>
                    <span className={`h-1 w-1 rounded-full ${/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password) ? 'bg-green-600' : 'bg-red-600'}`}></span>
                    One special character (!@#$%^&* etc.)
                  </li>
                </ul>
              </div>
            )}

            {/* Confirm Password Field */}
            <div className="space-y-2">
              <Label htmlFor="confirmPassword">Confirm Password</Label>
              <div className="relative">
                <Input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  disabled={loading}
                  className="pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-2.5 text-muted-foreground hover:text-foreground"
                >
                  {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
              {confirmPassword && password !== confirmPassword && (
                <p className="text-sm text-destructive">Passwords do not match</p>
              )}
            </div>

            {/* Security Info */}
            <div className="bg-blue-50 border border-blue-200 rounded-md p-3">
              <p className="text-xs text-blue-800">
                <strong>Security Tip:</strong> Use a unique password that you don't use on other accounts. This helps protect your SACCO account and personal information.
              </p>
            </div>

            {/* Submit Button */}
            <Button
              type="submit"
              className="w-full"
              disabled={loading || passwordErrors.length > 0 || !password || !confirmPassword || password !== confirmPassword}
            >
              {loading ? (
                <>
                  <span className="animate-spin mr-2 h-4 w-4">⏳</span>
                  Resetting Password...
                </>
              ) : (
                "Reset Password"
              )}
            </Button>

            {/* Back to Login */}
            <div className="text-center">
              <button
                type="button"
                onClick={() => navigate("/login")}
                className="text-sm text-primary hover:underline"
              >
                Back to Login
              </button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default ResetPassword;
