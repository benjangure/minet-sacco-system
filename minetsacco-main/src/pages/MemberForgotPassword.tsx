import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, UserRound, CheckCircle2, AlertCircle } from "lucide-react";
import { Link } from "react-router-dom";
import { getApiBaseUrl } from "@/config/api";
import { nativeFetch } from "@/utils/nativeHttp";

const MemberForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const { toast } = useToast();
  const [hrManaged, setHrManaged] = useState(false);

  // When members sign in with their HR credentials, the password is managed by HR, not here
  useEffect(() => {
    let cancelled = false;
    nativeFetch(`${getApiBaseUrl()}/auth/config`, { method: "GET" })
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => {
        if (!cancelled && d?.data?.hrAuthEnabled && d?.data?.localAuthFallback === false) {
          setHrManaged(true);
        }
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const identifier = email.trim();
    if (!identifier) {
      setError("Please enter your member number or email address");
      toast({
        title: "Validation Error",
        description: "Please enter your member number or email address",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);

    try {
      const response = await nativeFetch(`${getApiBaseUrl()}/auth/password-reset/member/request`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email: identifier }),
      });

      if (!response.ok) {
        throw new Error("Failed to send password reset email");
      }

      setSent(true);
      toast({
        title: "Success",
        description: "If we found your account, a reset link has been sent or the SACCO office has been notified.",
      });
    } catch (err) {
      setError("Failed to process password reset request. Please try again.");
      toast({
        title: "Error",
        description: "Failed to send password reset email. Please try again later.",
        variant: "destructive"
      });
      console.error("Member password reset error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 to-primary/10 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">Reset Your Password</CardTitle>
          <CardDescription>
            {sent
              ? "Your request has been received"
              : "Enter your member number or email to reset your password"}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {hrManaged ? (
            <div className="space-y-4">
              <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm text-blue-800">
                <p className="font-medium mb-1">Your password is managed by Minet HR</p>
                <p className="text-xs">
                  You sign in to the SACCO portal with the same credentials you use for the HR system.
                  To reset your password, please use the HR system's password reset, or contact the HR / IT help desk.
                </p>
              </div>
              <Link to="/member/login">
                <Button className="w-full">Return to Member Login</Button>
              </Link>
            </div>
          ) : !sent ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Member Number or Email</Label>
                <div className="relative">
                  <UserRound className="absolute left-3 top-2.5 h-5 w-5 text-muted-foreground" />
                  <Input
                    id="email"
                    type="text"
                    inputMode="email"
                    autoCapitalize="none"
                    autoCorrect="off"
                    spellCheck={false}
                    autoComplete="username"
                    placeholder="e.g. 13012 or your.email@example.com"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                    }}
                    required
                    disabled={loading}
                    className="pl-10"
                  />
                </div>
                {error && <p className="text-sm text-destructive">{error}</p>}
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm text-blue-800">
                <p className="font-medium mb-1">How to reset your password:</p>
                <ul className="text-xs space-y-1 list-disc list-inside">
                  <li>Enter your member number (or the email on your account)</li>
                  <li>If a real email is on file, we'll send a secure reset link</li>
                  <li>If not, the SACCO office is notified and will give you a temporary password</li>
                  <li>For security, a reset link expires in 30 minutes</li>
                </ul>
              </div>

              <Button
                type="submit"
                className="w-full"
                disabled={loading || !email}
              >
                {loading ? (
                  <>
                    <span className="animate-spin mr-2 h-4 w-4">⏳</span>
                    Sending Reset Link...
                  </>
                ) : (
                  "Send Reset Link"
                )}
              </Button>

              <div className="text-center">
                <Link
                  to="/member/login"
                  className="text-sm text-primary hover:underline inline-flex items-center gap-1"
                >
                  <ArrowLeft className="h-3 w-3" />
                  Back to Member Login
                </Link>
              </div>
            </form>
          ) : (
            <div className="space-y-4">
              <div className="flex justify-center">
                <CheckCircle2 className="h-12 w-12 text-green-600" />
              </div>
              <div className="text-center space-y-2">
                <p className="font-medium text-green-700">Reset Link Sent</p>
                <p className="text-sm text-muted-foreground">
                  If your account has an email address on file, a reset link will arrive shortly. Otherwise the SACCO office has been notified and will contact you with a temporary password.
                </p>
                <p className="text-xs text-muted-foreground">
                  The reset link will expire in 30 minutes for security reasons.
                </p>
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-md p-3">
                <div className="flex gap-2 text-sm text-amber-800">
                  <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-medium mb-1">Not received the email?</p>
                    <ul className="text-xs space-y-1 list-disc list-inside">
                      <li>Check your spam or junk folder</li>
                      <li>Wait a few minutes for the email</li>
                      <li>Verify you used the correct member number or email</li>
                      <li>Contact the SACCO office if you still cannot sign in</li>
                    </ul>
                  </div>
                </div>
              </div>

              <Button
                onClick={() => {
                  setSent(false);
                  setEmail("");
                  setError("");
                }}
                variant="outline"
                className="w-full"
              >
                Try Again
              </Button>

              <Link to="/member/login">
                <Button className="w-full">Return to Member Login</Button>
              </Link>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default MemberForgotPassword;
