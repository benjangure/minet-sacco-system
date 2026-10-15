import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { ArrowLeft, Mail, CheckCircle2 } from "lucide-react";
import { Link } from "react-router-dom";
import { getApiBaseUrl } from "@/config/api";
import { nativeFetch } from "@/utils/nativeHttp";

const ForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const { toast } = useToast();

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // Validate email
    if (!email.trim()) {
      setError("Email address is required");
      toast({
        title: "Validation Error",
        description: "Please enter your email address",
        variant: "destructive"
      });
      return;
    }


    setLoading(true);

    try {
      const response = await nativeFetch(`${getApiBaseUrl()}/auth/password-reset/request`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email: email.trim() }),
      });

      if (!response.ok) {
        throw new Error("Failed to send password reset email");
      }

      setSent(true);
      toast({
        title: "Success",
        description: "If an account exists with this email, you will receive a password reset link shortly.",
      });
    } catch (err) {
      setError("Failed to process password reset request. Please try again.");
      toast({
        title: "Error",
        description: "Failed to send password reset email. Please try again later.",
        variant: "destructive"
      });
      console.error("Password reset error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 to-primary/10 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">Forgot Password</CardTitle>
          <CardDescription>
            {sent
              ? "Check your email for reset instructions"
              : "Enter your email to receive password reset instructions"}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {!sent ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email Address</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-2.5 h-5 w-5 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="your.email@minet.co.ke"
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
                <p className="font-medium mb-1">Password Reset Instructions:</p>
                <ul className="text-xs space-y-1 list-disc list-inside">
                  <li>Enter your registered email address</li>
                  <li>Check your email (including spam folder) for reset link</li>
                  <li>Click the link to create a new password</li>
                  <li>Link expires in 30 minutes for security</li>
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
                  to="/login"
                  className="text-sm text-primary hover:underline inline-flex items-center gap-1"
                >
                  <ArrowLeft className="h-3 w-3" />
                  Back to Login
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
                  If an account exists with that email address, you will receive a password reset link shortly.
                </p>
                <p className="text-xs text-muted-foreground">
                  The reset link will expire in 30 minutes for security reasons.
                </p>
              </div>
              <div className="bg-amber-50 border border-amber-200 rounded-md p-3 text-sm text-amber-800">
                <p className="font-medium mb-1">Not received the email?</p>
                <ul className="text-xs space-y-1 list-disc list-inside">
                  <li>Check your spam or junk folder</li>
                  <li>Wait a few minutes for the email to arrive</li>
                  <li>Verify you entered the correct email address</li>
                </ul>
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
                Try Another Email
              </Button>
              <Link to="/login">
                <Button className="w-full">Return to Login</Button>
              </Link>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default ForgotPassword;
