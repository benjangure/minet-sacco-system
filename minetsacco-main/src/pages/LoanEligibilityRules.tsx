import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { AlertCircle, Save } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface LoanRules {
  id: number;
  minMemberSavings: number;
  minMemberShares: number;
  minSavingsToLoanRatio: number;
  maxOutstandingToSavingsRatio: number;
  maxActiveLoans: number;
  minGuarantorSavings: number;
  minGuarantorShares: number;
  minGuarantorSavingsToLoanRatio: number;
  maxGuarantorOutstandingToSavingsRatio: number;
  maxGuarantorCommitments: number;
  allowDefaulters: boolean;
  allowExitedMembers: boolean;
  maxLoanTermMonths: number;
  maxLoanToSavingsMultiplier: number;
}

export default function LoanEligibilityRules() {
  const { session, user } = useAuth();
  const { toast } = useToast();
  const [rules, setRules] = useState<LoanRules | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const token = session?.token;

  // Restrict access to ADMIN only
  if (user?.role !== "ADMIN") {
    return (
      <div className="container mx-auto p-6">
        <Card className="border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle className="text-red-900">Access Denied</CardTitle>
          </CardHeader>
          <CardContent className="text-red-800">
            Only administrators can manage loan eligibility rules.
          </CardContent>
        </Card>
      </div>
    );
  }

  useEffect(() => {
    fetchRules();
  }, []);

  const fetchRules = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loan-eligibility-rules`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await response.json();
      if (data.success) {
        setRules(data.data);
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to load eligibility rules",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!rules) return;

    setSaving(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loan-eligibility-rules`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(rules),
      });

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Success",
          description: "Eligibility rules updated successfully",
        });
        setRules(data.data);
      } else {
        toast({
          title: "Error",
          description: data.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to save eligibility rules",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (field: keyof LoanRules, value: any) => {
    if (rules) {
      setRules({
        ...rules,
        [field]: value,
      });
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <Card>
          <CardContent className="p-6">Loading eligibility rules...</CardContent>
        </Card>
      </div>
    );
  }

  if (!rules) {
    return (
      <div className="container mx-auto p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-6 text-red-800">
            Failed to load eligibility rules
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Loan Eligibility Rules</h1>
        <p className="text-gray-600 mt-1">
          Configure the rules that determine member and guarantor eligibility for loans
        </p>
      </div>

      <Alert className="mb-6 bg-blue-50 border-blue-200">
        <AlertCircle className="h-4 w-4 text-blue-600" />
        <AlertDescription className="text-blue-800">
          These rules apply to all loan applications. Adjust them according to your SACCO's policies.
        </AlertDescription>
      </Alert>

      <div className="grid gap-6">
        {/* Member Eligibility Rules */}
        <Card>
          <CardHeader>
            <CardTitle>Member Eligibility Rules</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="minMemberSavings">Minimum Member Savings (KES)</Label>
                <Input
                  id="minMemberSavings"
                  type="number"
                  value={rules.minMemberSavings}
                  onChange={(e) => handleChange("minMemberSavings", parseFloat(e.target.value))}
                  step="1000"
                />
              </div>
              <div>
                <Label htmlFor="minMemberShares">Minimum Member Shares (KES)</Label>
                <Input
                  id="minMemberShares"
                  type="number"
                  value={rules.minMemberShares}
                  onChange={(e) => handleChange("minMemberShares", parseFloat(e.target.value))}
                  step="1000"
                />
              </div>
              <div>
                <Label htmlFor="minSavingsToLoanRatio">Min Savings to Loan Ratio (%)</Label>
                <Input
                  id="minSavingsToLoanRatio"
                  type="number"
                  value={rules.minSavingsToLoanRatio * 100}
                  onChange={(e) => handleChange("minSavingsToLoanRatio", parseFloat(e.target.value) / 100)}
                  step="5"
                  min="0"
                  max="100"
                />
              </div>
              <div>
                <Label htmlFor="maxOutstandingToSavingsRatio">Max Outstanding to Savings Ratio (%)</Label>
                <Input
                  id="maxOutstandingToSavingsRatio"
                  type="number"
                  value={rules.maxOutstandingToSavingsRatio * 100}
                  onChange={(e) => handleChange("maxOutstandingToSavingsRatio", parseFloat(e.target.value) / 100)}
                  step="5"
                  min="0"
                  max="100"
                />
              </div>
              <div>
                <Label htmlFor="maxActiveLoans">Max Active Loans per Member</Label>
                <Input
                  id="maxActiveLoans"
                  type="number"
                  value={rules.maxActiveLoans}
                  onChange={(e) => handleChange("maxActiveLoans", parseInt(e.target.value))}
                  min="1"
                />
              </div>
              <div>
                <Label htmlFor="maxLoanToSavingsMultiplier">Max Loan to Savings Multiplier (e.g., 3 = 3x savings)</Label>
                <Input
                  id="maxLoanToSavingsMultiplier"
                  type="number"
                  value={rules.maxLoanToSavingsMultiplier}
                  onChange={(e) => handleChange("maxLoanToSavingsMultiplier", parseFloat(e.target.value))}
                  step="0.5"
                  min="1"
                  max="10"
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Kenyan SACCO standard: 3x savings. Member with KES 50,000 savings can borrow max KES {(50000 * rules.maxLoanToSavingsMultiplier).toLocaleString('en-KE')}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Guarantor Eligibility Rules */}
        <Card>
          <CardHeader>
            <CardTitle>Guarantor Eligibility Rules</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="minGuarantorSavings">Minimum Guarantor Savings (KES)</Label>
                <Input
                  id="minGuarantorSavings"
                  type="number"
                  value={rules.minGuarantorSavings}
                  onChange={(e) => handleChange("minGuarantorSavings", parseFloat(e.target.value))}
                  step="1000"
                />
              </div>
              <div>
                <Label htmlFor="minGuarantorShares">Minimum Guarantor Shares (KES)</Label>
                <Input
                  id="minGuarantorShares"
                  type="number"
                  value={rules.minGuarantorShares}
                  onChange={(e) => handleChange("minGuarantorShares", parseFloat(e.target.value))}
                  step="1000"
                />
              </div>
              <div>
                <Label htmlFor="minGuarantorSavingsToLoanRatio">Min Guarantor Savings to Loan Ratio (%)</Label>
                <Input
                  id="minGuarantorSavingsToLoanRatio"
                  type="number"
                  value={rules.minGuarantorSavingsToLoanRatio * 100}
                  onChange={(e) => handleChange("minGuarantorSavingsToLoanRatio", parseFloat(e.target.value) / 100)}
                  step="5"
                  min="0"
                  max="100"
                />
              </div>
              <div>
                <Label htmlFor="maxGuarantorOutstandingToSavingsRatio">Max Guarantor Outstanding to Savings Ratio (%)</Label>
                <Input
                  id="maxGuarantorOutstandingToSavingsRatio"
                  type="number"
                  value={rules.maxGuarantorOutstandingToSavingsRatio * 100}
                  onChange={(e) => handleChange("maxGuarantorOutstandingToSavingsRatio", parseFloat(e.target.value) / 100)}
                  step="5"
                  min="0"
                  max="100"
                />
              </div>
              <div>
                <Label htmlFor="maxGuarantorCommitments">Max Guarantor Commitments</Label>
                <Input
                  id="maxGuarantorCommitments"
                  type="number"
                  value={rules.maxGuarantorCommitments}
                  onChange={(e) => handleChange("maxGuarantorCommitments", parseInt(e.target.value))}
                  min="1"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Loan Term Policy */}
        <Card>
          <CardHeader>
            <CardTitle>Loan Term Policy</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Sets the global maximum repayment period for all loan products. Standard Kenyan SACCO practice is 72 months (6 years) for development loans.
            </p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="maxLoanTermMonths">Maximum Loan Term (months)</Label>
                <Input
                  id="maxLoanTermMonths"
                  type="number"
                  value={rules.maxLoanTermMonths}
                  onChange={(e) => handleChange("maxLoanTermMonths", parseInt(e.target.value))}
                  min="1"
                  max="120"
                  step="6"
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Currently: {rules.maxLoanTermMonths} months ({(rules.maxLoanTermMonths / 12).toFixed(1)} years)
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Special Conditions */}
        <Card>
          <CardHeader>
            <CardTitle>Special Conditions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="allowDefaulters"
                checked={rules.allowDefaulters}
                onChange={(e) => handleChange("allowDefaulters", e.target.checked)}
                className="w-4 h-4"
              />
              <Label htmlFor="allowDefaulters" className="cursor-pointer">
                Allow members with defaulted loans to be guarantors
              </Label>
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="allowExitedMembers"
                checked={rules.allowExitedMembers}
                onChange={(e) => handleChange("allowExitedMembers", e.target.checked)}
                className="w-4 h-4"
              />
              <Label htmlFor="allowExitedMembers" className="cursor-pointer">
                Allow exited members to be guarantors
              </Label>
            </div>
          </CardContent>
        </Card>

        {/* Save Button */}
        <div className="flex justify-end">
          <Button onClick={handleSave} disabled={saving} size="lg">
            <Save className="mr-2 h-4 w-4" />
            {saving ? "Saving..." : "Save Rules"}
          </Button>
        </div>
      </div>
    </div>
  );
}
