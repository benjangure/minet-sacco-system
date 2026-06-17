import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Copy, Check, Eye, EyeOff, AlertCircle } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

interface MemberCreationResponseDTO {
  memberId: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
  username: string;
  password: string | null;
  hasNationalId: boolean;
  passwordType: string; // "NATIONAL_ID" or "GENERATED"
  message: string;
}

interface CredentialDisplayModalProps {
  isOpen: boolean;
  onClose: () => void;
  credential: MemberCreationResponseDTO | null;
}

export const CredentialDisplayModal = ({ isOpen, onClose, credential }: CredentialDisplayModalProps) => {
  const [showPassword, setShowPassword] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const { toast } = useToast();

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    toast({
      title: "Copied",
      description: `${field === "username" ? "Username" : "Password"} copied to clipboard`,
    });
    setTimeout(() => setCopiedField(null), 2000);
  };

  if (!credential) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Member Credentials Successfully Created</DialogTitle>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Member Info */}
          <div className="bg-slate-50 p-4 rounded-lg space-y-2">
            <div className="text-sm">
              <p className="text-muted-foreground">Member Name</p>
              <p className="font-semibold">{credential.firstName} {credential.lastName}</p>
            </div>
            <div className="text-sm">
              <p className="text-muted-foreground">Member Number</p>
              <p className="font-semibold">{credential.memberNumber}</p>
            </div>
          </div>

          {/* Credentials */}
          <div className="space-y-4">
            {/* Username */}
            <div className="space-y-2">
              <Label>Username (Login ID)</Label>
              <div className="flex gap-2">
                <Input 
                  type="text" 
                  value={credential.username} 
                  readOnly 
                  className="font-mono"
                />
                <Button
                  type="button"
                  size="icon"
                  variant="outline"
                  onClick={() => copyToClipboard(credential.username, "username")}
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
            <div className="space-y-2">
              <Label>
                {credential.passwordType === "GENERATED" ? "Temporary Password" : "Password"}
              </Label>
              <div className="flex gap-2">
                <Input 
                  type={showPassword ? "text" : "password"}
                  value={credential.passwordType === "GENERATED" ? credential.password : credential.password || "••••••••"}
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
                {credential.passwordType === "GENERATED" && (
                  <Button
                    type="button"
                    size="icon"
                    variant="outline"
                    onClick={() => copyToClipboard(credential.password || "", "password")}
                    className="shrink-0"
                  >
                    {copiedField === "password" ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </Button>
                )}
              </div>
            </div>
          </div>

          {/* Info Alert */}
          {credential.passwordType === "NATIONAL_ID" && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription className="text-xs">
                <strong>Password Type:</strong> Member will use their National ID as the initial password on first login.
              </AlertDescription>
            </Alert>
          )}

          {credential.passwordType === "GENERATED" && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription className="text-xs">
                <strong>Temporary Password:</strong> This is a secure temporary password. The member must change it on their first login. Store this securely and share with the member through a secure channel.
              </AlertDescription>
            </Alert>
          )}

          {/* Instructions */}
          <Alert className="bg-blue-50 border-blue-200">
            <AlertCircle className="h-4 w-4 text-blue-600" />
            <AlertDescription className="text-xs text-blue-800">
              <strong>Next Steps:</strong>
              <ul className="list-disc list-inside mt-1 space-y-1">
                <li>Share these credentials with the member securely</li>
                <li>Member logs in via the mobile app with username and {credential.passwordType === "GENERATED" ? "temporary password" : "national ID"}</li>
                <li>On first login, member will be prompted to set up a new password</li>
              </ul>
            </AlertDescription>
          </Alert>
        </div>

        <div className="flex gap-2">
          <Button onClick={onClose} className="flex-1">
            Done
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
