import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Plus, Edit, Trash2, AlertCircle } from "lucide-react";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface LoanProduct {
  id: number;
  name: string;
  description: string;
  interestRate: number;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  isActive: boolean;
}

const LoanProducts = () => {
  const [products, setProducts] = useState<LoanProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<LoanProduct | null>(null);
  const { toast } = useToast();
  const { session, role } = useAuth();

  const [form, setForm] = useState({
    name: "",
    description: "",
    interestRate: "",
    minAmount: "",
    maxAmount: "",
    minTermMonths: "",
    maxTermMonths: "",
    isActive: true,
  });

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/loan-products`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      
      if (response.ok) {
        const data = await response.json();
        setProducts(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching loan products:", error);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (session) {
      fetchProducts();
    }
  }, [session]);

  const resetForm = () => {
    setForm({
      name: "",
      description: "",
      interestRate: "",
      minAmount: "",
      maxAmount: "",
      minTermMonths: "",
      maxTermMonths: "",
      isActive: true,
    });
    setEditingProduct(null);
  };

  const handleEdit = (product: LoanProduct) => {
    setEditingProduct(product);
    setForm({
      name: product.name,
      description: product.description,
      interestRate: product.interestRate.toString(),
      minAmount: product.minAmount.toString(),
      maxAmount: product.maxAmount.toString(),
      minTermMonths: product.minTermMonths.toString(),
      maxTermMonths: product.maxTermMonths.toString(),
      isActive: product.isActive,
    });
    setDialogOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const payload = {
      name: form.name,
      description: form.description,
      interestRate: parseFloat(form.interestRate),
      minAmount: parseFloat(form.minAmount),
      maxAmount: parseFloat(form.maxAmount),
      minTermMonths: parseInt(form.minTermMonths),
      maxTermMonths: parseInt(form.maxTermMonths),
      isActive: form.isActive,
    };

    try {
      const url = editingProduct
        ? `${API_BASE_URL}/loan-products/${editingProduct.id}`
        : `${API_BASE_URL}/loan-products`;
      
      const response = await fetch(url, {
        method: editingProduct ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        toast({
          title: "Success",
          description: `Loan product ${editingProduct ? "updated" : "created"} successfully`,
        });
        setDialogOpen(false);
        resetForm();
        fetchProducts();
      } else {
        const error = await response.json();
        toast({
          title: "Error",
          description: error.message || "Failed to save loan product",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to save loan product",
        variant: "destructive",
      });
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this loan product?")) return;

    try {
      const response = await fetch(`${API_BASE_URL}/loan-products/${id}`, {
        method: "DELETE",
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        toast({ title: "Success", description: "Loan product deleted successfully" });
        fetchProducts();
      } else {
        const error = await response.json();
        toast({
          title: "Error",
          description: error.message || "Failed to delete loan product",
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to delete loan product",
        variant: "destructive",
      });
    }
  };

  if (role !== "ADMIN") {
    return (
      <div>
        <h1 className="text-3xl font-bold text-foreground mb-6">Loan Products</h1>
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            You do not have permission to manage loan products. Only administrators can access this page.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Loan Products</h1>
          <p className="text-muted-foreground">Manage loan product offerings and terms</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) resetForm();
        }}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              New Loan Product
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>
                {editingProduct ? "Edit Loan Product" : "New Loan Product"}
              </DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label>Product Name *</Label>
                <Input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="e.g., Emergency Loan"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Description</Label>
                <Textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Product description..."
                  rows={3}
                />
              </div>
              <div className="space-y-2">
                <Label>Interest Rate (% per annum) *</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={form.interestRate}
                  onChange={(e) => setForm({ ...form, interestRate: e.target.value })}
                  placeholder="e.g., 12.5"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Min Amount (KES) *</Label>
                  <Input
                    type="number"
                    value={form.minAmount}
                    onChange={(e) => setForm({ ...form, minAmount: e.target.value })}
                    placeholder="e.g., 5000"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label>Max Amount (KES) *</Label>
                  <Input
                    type="number"
                    value={form.maxAmount}
                    onChange={(e) => setForm({ ...form, maxAmount: e.target.value })}
                    placeholder="e.g., 500000"
                    required
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Min Term (months) *</Label>
                  <Input
                    type="number"
                    value={form.minTermMonths}
                    onChange={(e) => setForm({ ...form, minTermMonths: e.target.value })}
                    placeholder="e.g., 3"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label>Max Term (months) *</Label>
                  <Input
                    type="number"
                    value={form.maxTermMonths}
                    onChange={(e) => setForm({ ...form, maxTermMonths: e.target.value })}
                    placeholder="e.g., 36"
                    required
                  />
                </div>
              </div>
              <div className="flex items-center justify-between">
                <Label>Active</Label>
                <Switch
                  checked={form.isActive}
                  onCheckedChange={(checked) => setForm({ ...form, isActive: checked })}
                />
              </div>
              <Button type="submit" className="w-full">
                {editingProduct ? "Update Product" : "Create Product"}
              </Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <Card className="border-none shadow-sm">
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product Name</TableHead>
                <TableHead>Interest Rate</TableHead>
                <TableHead>Amount Range</TableHead>
                <TableHead>Term Range</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : products.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                    No loan products found. Create your first product to get started.
                  </TableCell>
                </TableRow>
              ) : (
                products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>
                      <div>
                        <div className="font-medium">{product.name}</div>
                        {product.description && (
                          <div className="text-xs text-muted-foreground line-clamp-1">
                            {product.description}
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>{product.interestRate}% p.a.</TableCell>
                    <TableCell>
                      KES {product.minAmount.toLocaleString()} - {product.maxAmount.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      {product.minTermMonths} - {product.maxTermMonths} months
                    </TableCell>
                    <TableCell>
                      <Badge variant={product.isActive ? "default" : "secondary"}>
                        {product.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleEdit(product)}
                          title="Edit"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDelete(product.id)}
                          title="Delete"
                          className="text-red-600 hover:text-red-700"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

export default LoanProducts;
