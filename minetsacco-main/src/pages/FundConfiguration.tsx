import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Edit2, Power, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Fund {
  id: number;
  fundType: string;
  displayName: string;
  enabled: boolean;
  displayOrder: number;
  description?: string;
  minimumAmount?: number;
  maximumAmount?: number;
}

export default function FundConfiguration() {
  const { session } = useAuth();
  const { toast } = useToast();
  const [funds, setFunds] = useState<Fund[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingFund, setEditingFund] = useState<Fund | null>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    displayName: "",
    description: "",
    minimumAmount: "",
    maximumAmount: "",
    displayOrder: "",
  });

  const token = session?.token;

  useEffect(() => {
    fetchFunds();
  }, []);

  const fetchFunds = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/fund-configurations`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const data = await response.json();
      console.log("Fund Configuration Response:", data);
      console.log("Response Status:", response.status);
      if (data.success) {
        console.log("Funds loaded:", data.data);
        setFunds(data.data);
      } else {
        console.error("API returned success: false", data);
        console.error("Error message:", data.message);
        toast({
          title: "Error",
          description: data.message || "Failed to load funds",
          variant: "destructive",
        });
      }
    } catch (error) {
      console.error("Error fetching funds:", error);
      toast({
        title: "Error",
        description: "Failed to load funds: " + (error instanceof Error ? error.message : String(error)),
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleEditClick = (fund: Fund) => {
    setEditingFund(fund);
    setFormData({
      displayName: fund.displayName,
      description: fund.description || "",
      minimumAmount: fund.minimumAmount?.toString() || "",
      maximumAmount: fund.maximumAmount?.toString() || "",
      displayOrder: fund.displayOrder?.toString() || "",
    });
    setEditDialogOpen(true);
  };

  const handleSaveChanges = async () => {
    if (!editingFund) return;

    if (!formData.displayName.trim()) {
      toast({
        title: "Validation Error",
        description: "Display name is required",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/fund-configurations/${editingFund.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          displayName: formData.displayName,
          description: formData.description,
          minimumAmount: formData.minimumAmount ? parseFloat(formData.minimumAmount) : null,
          maximumAmount: formData.maximumAmount ? parseFloat(formData.maximumAmount) : null,
          displayOrder: formData.displayOrder ? parseInt(formData.displayOrder) : null,
          enabled: editingFund.enabled,
        }),
      });

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Success",
          description: "Fund updated successfully",
        });
        setEditDialogOpen(false);
        setEditingFund(null);
        fetchFunds();
      } else {
        toast({
          title: "Error",
          description: data.message || "Failed to update fund",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to update fund",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleToggleFund = async (fund: Fund) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/fund-configurations/${fund.id}/toggle`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      if (data.success) {
        toast({
          title: "Success",
          description: `Fund ${data.data.enabled ? "enabled" : "disabled"} successfully`,
        });
        fetchFunds();
      } else {
        toast({
          title: "Error",
          description: data.message || "Failed to toggle fund",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to toggle fund",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return "-";
    return new Intl.NumberFormat("en-KE", {
      style: "currency",
      currency: "KES",
    }).format(amount);
  };

  return (
    <div className="container mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Fund Configuration</h1>
        <p className="text-gray-600 mt-1">Manage optional contribution funds for monthly contributions</p>
      </div>

      <Alert className="mb-6 border-blue-200 bg-blue-50">
        <AlertCircle className="h-4 w-4 text-blue-600" />
        <AlertDescription className="text-blue-800">
          Enable or disable optional funds that members can contribute to during monthly contributions. Changes will be reflected in the bulk processing template immediately.
        </AlertDescription>
      </Alert>

      <Card>
        <CardHeader>
          <CardTitle>Available Funds</CardTitle>
        </CardHeader>
        <CardContent>
          {loading && funds.length === 0 ? (
            <div className="text-center py-8 text-gray-500">Loading funds...</div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fund Type</TableHead>
                    <TableHead>Display Name</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Min Amount</TableHead>
                    <TableHead>Max Amount</TableHead>
                    <TableHead>Order</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {funds.map((fund) => (
                    <TableRow key={fund.id}>
                      <TableCell className="font-medium">{fund.fundType}</TableCell>
                      <TableCell>{fund.displayName}</TableCell>
                      <TableCell className="text-sm text-gray-600 max-w-xs truncate">
                        {fund.description || "-"}
                      </TableCell>
                      <TableCell>{formatCurrency(fund.minimumAmount)}</TableCell>
                      <TableCell>{formatCurrency(fund.maximumAmount)}</TableCell>
                      <TableCell>{fund.displayOrder || "-"}</TableCell>
                      <TableCell>
                        <Badge className={fund.enabled ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"}>
                          {fund.enabled ? "Enabled" : "Disabled"}
                        </Badge>
                      </TableCell>
                      <TableCell className="space-x-2">
                        <Dialog open={editDialogOpen && editingFund?.id === fund.id} onOpenChange={setEditDialogOpen}>
                          <DialogTrigger asChild>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleEditClick(fund)}
                            >
                              <Edit2 className="h-4 w-4 mr-1" />
                              Edit
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="max-w-md">
                            <DialogHeader>
                              <DialogTitle>Edit Fund: {fund.displayName}</DialogTitle>
                            </DialogHeader>
                            <div className="space-y-4 py-4">
                              <div className="space-y-2">
                                <Label>Display Name</Label>
                                <Input
                                  value={formData.displayName}
                                  onChange={(e) =>
                                    setFormData({ ...formData, displayName: e.target.value })
                                  }
                                  placeholder="e.g., School Fees Fund"
                                />
                              </div>
                              <div className="space-y-2">
                                <Label>Description</Label>
                                <Textarea
                                  value={formData.description}
                                  onChange={(e) =>
                                    setFormData({ ...formData, description: e.target.value })
                                  }
                                  placeholder="Brief description of the fund"
                                  rows={3}
                                />
                              </div>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                  <Label>Minimum Amount (KES)</Label>
                                  <Input
                                    type="number"
                                    value={formData.minimumAmount}
                                    onChange={(e) =>
                                      setFormData({ ...formData, minimumAmount: e.target.value })
                                    }
                                    placeholder="0"
                                  />
                                </div>
                                <div className="space-y-2">
                                  <Label>Maximum Amount (KES)</Label>
                                  <Input
                                    type="number"
                                    value={formData.maximumAmount}
                                    onChange={(e) =>
                                      setFormData({ ...formData, maximumAmount: e.target.value })
                                    }
                                    placeholder="1000000"
                                  />
                                </div>
                              </div>
                              <div className="space-y-2">
                                <Label>Display Order</Label>
                                <Input
                                  type="number"
                                  value={formData.displayOrder}
                                  onChange={(e) =>
                                    setFormData({ ...formData, displayOrder: e.target.value })
                                  }
                                  placeholder="1"
                                />
                              </div>
                              <div className="flex justify-end space-x-2 pt-4">
                                <Button
                                  variant="outline"
                                  onClick={() => setEditDialogOpen(false)}
                                >
                                  Cancel
                                </Button>
                                <Button onClick={handleSaveChanges} disabled={loading}>
                                  {loading ? "Saving..." : "Save Changes"}
                                </Button>
                              </div>
                            </div>
                          </DialogContent>
                        </Dialog>
                        <Button
                          variant={fund.enabled ? "destructive" : "default"}
                          size="sm"
                          onClick={() => handleToggleFund(fund)}
                          disabled={loading}
                        >
                          <Power className="h-4 w-4 mr-1" />
                          {fund.enabled ? "Disable" : "Enable"}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="mt-6 border-blue-200 bg-blue-50">
        <CardHeader>
          <CardTitle className="text-blue-900">How It Works</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-blue-800 space-y-2">
          <p>
            • <strong>Enable/Disable Funds:</strong> Use the toggle button to enable or disable funds. Disabled funds will not appear in the monthly contributions template.
          </p>
          <p>
            • <strong>Edit Fund Details:</strong> Click Edit to change the display name, description, and amount limits for each fund.
          </p>
          <p>
            • <strong>Display Order:</strong> Set the order in which funds appear in the template and member contribution forms.
          </p>
          <p>
            • <strong>Immediate Effect:</strong> Changes take effect immediately. The bulk processing template will reflect the current enabled funds.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
