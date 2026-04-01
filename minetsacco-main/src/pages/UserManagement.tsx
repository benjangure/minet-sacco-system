import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Shield, UserPlus, AlertTriangle, Power, Eye, Settings } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const API_BASE_URL = "http://localhost:8080/api";

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  createdBy: number | null;
  createdByUsername: string;
  createdAt: string;
}

interface ActivityLog {
  id: number;
  action: string;
  details: string;
  createdAt: string;
}

const roleLabels: Record<string, string> = {
  ADMIN: "System Administrator",
  TREASURER: "Treasurer / Finance",
  LOAN_OFFICER: "Loan Officer",
  CREDIT_COMMITTEE: "Credit Committee",
  AUDITOR: "Auditor / Compliance",
  TELLER: "Teller / Data Entry",
  CUSTOMER_SUPPORT: "Customer Support",
};

const roleBadgeColors: Record<string, string> = {
  ADMIN: "bg-red-100 text-red-800",
  TREASURER: "bg-blue-100 text-blue-800",
  LOAN_OFFICER: "bg-green-100 text-green-800",
  CREDIT_COMMITTEE: "bg-yellow-100 text-yellow-800",
  AUDITOR: "bg-purple-100 text-purple-800",
  TELLER: "bg-indigo-100 text-indigo-800",
  CUSTOMER_SUPPORT: "bg-orange-100 text-orange-800",
};

const roleHierarchy: Record<string, string[]> = {
  ADMIN: ["ADMIN", "TREASURER", "LOAN_OFFICER", "CREDIT_COMMITTEE", "AUDITOR"],
  TREASURER: ["TELLER", "CUSTOMER_SUPPORT"],
};

const UserManagement = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [createUserOpen, setCreateUserOpen] = useState(false);
  const [deactivateDialogOpen, setDeactivateDialogOpen] = useState(false);
  const [activityLogDialogOpen, setActivityLogDialogOpen] = useState(false);
  const [changeRoleDialogOpen, setChangeRoleDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [deactivateReason, setDeactivateReason] = useState("");
  const [newRoleValue, setNewRoleValue] = useState("");
  const [changeRoleReason, setChangeRoleReason] = useState("");
  const [newUsername, setNewUsername] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newRole, setNewRole] = useState("");
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const { toast } = useToast();
  const { role: currentUserRole, session } = useAuth();

  const allowedRoles = currentUserRole ? (roleHierarchy[currentUserRole] || []) : [];

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/users`, {
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setUsers(data.data || []);
        
        // Get current user ID from the users list
        if (session?.user?.username) {
          const currentUser = (data.data || []).find((u: User) => u.username === session.user.username);
          if (currentUser) {
            setCurrentUserId(currentUser.id);
            console.log("Current user ID set to:", currentUser.id);
            console.log("Current username:", session.user.username);
          } else {
            console.log("Current user not found in users list");
          }
        } else {
          console.log("No session username available");
        }
        
        // Debug: Log all users with their createdBy
        console.log("Users loaded:", (data.data || []).map((u: User) => ({
          username: u.username,
          id: u.id,
          createdBy: u.createdBy,
          role: u.role
        })));
      }
    } catch (error) {
      console.error("Error fetching users:", error);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (session) {
      fetchUsers();
    }
  }, [session, currentUserRole]);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername || !newEmail || !newPassword || !newRole) return;

    try {
      const response = await fetch(`${API_BASE_URL}/users`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          username: newUsername,
          email: newEmail,
          password: newPassword,
          role: newRole,
          enabled: true,
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: `User ${newUsername} created successfully` });
        setCreateUserOpen(false);
        setNewUsername("");
        setNewEmail("");
        setNewPassword("");
        setNewRole("");
        fetchUsers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to create user", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to create user", variant: "destructive" });
    }
  };



  const handleDeactivateUser = async () => {
    if (!selectedUser || !deactivateReason.trim()) {
      toast({ title: "Error", description: "Please provide a reason for deactivation", variant: "destructive" });
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/users/${selectedUser.id}/deactivate?reason=${encodeURIComponent(deactivateReason)}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: `User ${selectedUser.username} deactivated successfully` });
        setDeactivateDialogOpen(false);
        setSelectedUser(null);
        setDeactivateReason("");
        fetchUsers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to deactivate user", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to deactivate user", variant: "destructive" });
    }
  };

  const handleReactivateUser = async (user: User) => {
    try {
      const response = await fetch(`${API_BASE_URL}/users/${user.id}/reactivate?reason=Admin reactivation`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: `User ${user.username} reactivated successfully` });
        fetchUsers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to reactivate user", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to reactivate user", variant: "destructive" });
    }
  };

  const handleViewActivityLog = async (user: User) => {
    setSelectedUser(user);
    try {
      const response = await fetch(`${API_BASE_URL}/users/${user.id}/activity-log`, {
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        setActivityLogs(data.data || []);
        setActivityLogDialogOpen(true);
      } else {
        toast({ title: "Error", description: "Failed to fetch activity log", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to fetch activity log", variant: "destructive" });
    }
  };

  const openDeactivateDialog = (user: User) => {
    setSelectedUser(user);
    setDeactivateReason("");
    setDeactivateDialogOpen(true);
  };

  const handleChangeRole = async () => {
    if (!selectedUser || !newRoleValue.trim() || !changeRoleReason.trim()) {
      toast({ title: "Error", description: "Please select a role and provide a reason", variant: "destructive" });
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/users/${selectedUser.id}/change-role?newRole=${encodeURIComponent(newRoleValue)}&reason=${encodeURIComponent(changeRoleReason)}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${session?.token}`,
        },
      });

      if (response.ok) {
        toast({ title: "Success", description: `User role changed to ${roleLabels[newRoleValue]}` });
        setChangeRoleDialogOpen(false);
        setSelectedUser(null);
        setNewRoleValue("");
        setChangeRoleReason("");
        fetchUsers();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to change role", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to change role", variant: "destructive" });
    }
  };

  const openChangeRoleDialog = (user: User) => {
    setSelectedUser(user);
    setNewRoleValue(user.role);
    setChangeRoleReason("");
    setChangeRoleDialogOpen(true);
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground">User Management</h1>
          <p className="text-muted-foreground">Manage staff accounts with Maker-Checker approval</p>
        </div>
      </div>

      {currentUserRole && allowedRoles.length > 0 && (
        <Card className="mb-6 border-l-4 border-l-primary border-none shadow-sm">
          <CardContent className="pt-4">
            <div className="flex items-start gap-3">
              <Shield className="h-5 w-5 text-primary mt-0.5" />
              <div>
                <p className="font-medium text-sm">Your Role: {roleLabels[currentUserRole]}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  You can create: {allowedRoles.map(r => roleLabels[r]).join(", ")}
                </p>
                <p className="text-xs text-muted-foreground">
                  Users can be deactivated (disabled) instead of deleted to preserve data for audit compliance.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="users">
        <TabsList>
          <TabsTrigger value="users">Staff Users</TabsTrigger>
        </TabsList>

        <TabsContent value="users" className="space-y-4">
          <div className="flex justify-end gap-2">
            {allowedRoles.length > 0 && (
              <Dialog open={createUserOpen} onOpenChange={setCreateUserOpen}>
                <DialogTrigger asChild>
                  <Button><UserPlus className="mr-2 h-4 w-4" />Create Staff User</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader><DialogTitle>Create New Staff User</DialogTitle></DialogHeader>
                  <form onSubmit={handleCreateUser} className="space-y-4">
                    <div className="space-y-2">
                      <Label>Username *</Label>
                      <Input value={newUsername} onChange={e => setNewUsername(e.target.value)} required placeholder="e.g. jmwangi" />
                    </div>
                    <div className="space-y-2">
                      <Label>Email *</Label>
                      <Input type="email" value={newEmail} onChange={e => setNewEmail(e.target.value)} required placeholder="jane@minet.co.ke" />
                    </div>
                    <div className="space-y-2">
                      <Label>Password *</Label>
                      <Input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={8} placeholder="Min 8 characters" />
                    </div>
                    <div className="space-y-2">
                      <Label>Role *</Label>
                      <Select value={newRole} onValueChange={setNewRole}>
                        <SelectTrigger><SelectValue placeholder="Select role" /></SelectTrigger>
                        <SelectContent>
                          {allowedRoles.map(r => (
                            <SelectItem key={r} value={r}>{roleLabels[r]}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <Button type="submit" className="w-full">Create User & Assign Role</Button>
                  </form>
                </DialogContent>
              </Dialog>
            )}
          </div>

          <Card className="border-none shadow-sm">
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Created By</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Joined</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow><TableCell colSpan={7} className="text-center py-8">Loading...</TableCell></TableRow>
                  ) : users.length === 0 ? (
                    <TableRow><TableCell colSpan={7} className="text-center py-8 text-muted-foreground">No users found</TableCell></TableRow>
                  ) : users.map(user => (
                    <TableRow key={user.id}>
                      <TableCell className="font-medium">{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <Badge className={roleBadgeColors[user.role]}>{roleLabels[user.role]}</Badge>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-muted-foreground">{user.createdByUsername}</span>
                      </TableCell>
                      <TableCell>
                        <Badge className={user.enabled ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}>
                          {user.enabled ? "Active" : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell>{new Date(user.createdAt).toLocaleDateString()}</TableCell>
                      <TableCell className="text-right">
                        {user.role !== "ADMIN" && user.createdBy === currentUserId ? (
                          <div className="flex gap-1 justify-end">
                            {user.enabled ? (
                              <Button 
                                variant="ghost" 
                                size="sm" 
                                onClick={() => openDeactivateDialog(user)} 
                                className="text-orange-600 hover:text-orange-700 hover:bg-orange-50"
                                title="Deactivate user"
                              >
                                <Power className="h-4 w-4" />
                              </Button>
                            ) : (
                              <Button 
                                variant="ghost" 
                                size="sm" 
                                onClick={() => handleReactivateUser(user)} 
                                className="text-green-600 hover:text-green-700 hover:bg-green-50"
                                title="Reactivate user"
                              >
                                <Power className="h-4 w-4" />
                              </Button>
                            )}
                            {currentUserRole === "ADMIN" && (
                              <Button 
                                variant="ghost" 
                                size="sm" 
                                onClick={() => openChangeRoleDialog(user)} 
                                className="text-purple-600 hover:text-purple-700 hover:bg-purple-50"
                                title="Change role"
                              >
                                <Settings className="h-4 w-4" />
                              </Button>
                            )}
                            <Button 
                              variant="ghost" 
                              size="sm" 
                              onClick={() => handleViewActivityLog(user)} 
                              className="text-blue-600 hover:text-blue-700 hover:bg-blue-50"
                              title="View activity log"
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                          </div>
                        ) : user.role !== "ADMIN" ? (
                          <span className="text-xs text-muted-foreground">Not your user</span>
                        ) : (
                          <span className="text-xs text-muted-foreground">Protected</span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Deactivate User Dialog */}
      <Dialog open={deactivateDialogOpen} onOpenChange={setDeactivateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Power className="h-5 w-5 text-orange-500" />
              Deactivate User
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              You are deactivating user: <strong>{selectedUser?.username}</strong>
            </p>
            <p className="text-sm text-muted-foreground">
              The user will not be able to log in, but their account and data will be preserved.
            </p>
            <div className="space-y-2">
              <Label>Reason for Deactivation *</Label>
              <Textarea 
                value={deactivateReason}
                onChange={e => setDeactivateReason(e.target.value)}
                placeholder="e.g., On leave, temporary suspension, etc."
                rows={4}
                required
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeactivateDialogOpen(false)}>Cancel</Button>
            <Button variant="default" onClick={handleDeactivateUser} className="bg-orange-600 hover:bg-orange-700">Deactivate User</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Change Role Dialog */}
      <Dialog open={changeRoleDialogOpen} onOpenChange={setChangeRoleDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Settings className="h-5 w-5 text-purple-600" />
              Change User Role
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Changing role for: <strong>{selectedUser?.username}</strong>
            </p>
            <p className="text-sm text-muted-foreground">
              Current role: <strong>{selectedUser ? roleLabels[selectedUser.role] : ""}</strong>
            </p>
            <div className="space-y-2">
              <Label>New Role *</Label>
              <Select value={newRoleValue} onValueChange={setNewRoleValue}>
                <SelectTrigger>
                  <SelectValue placeholder="Select new role" />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(roleLabels).map(([role, label]) => (
                    role !== "ADMIN" && (
                      <SelectItem key={role} value={role}>{label}</SelectItem>
                    )
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Reason for Change *</Label>
              <Textarea 
                value={changeRoleReason}
                onChange={e => setChangeRoleReason(e.target.value)}
                placeholder="e.g., Promotion, Department transfer, etc."
                rows={3}
                required
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setChangeRoleDialogOpen(false)}>Cancel</Button>
            <Button variant="default" onClick={handleChangeRole} className="bg-purple-600 hover:bg-purple-700">Change Role</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Activity Log Dialog */}
      <Dialog open={activityLogDialogOpen} onOpenChange={setActivityLogDialogOpen}>
        <DialogContent className="max-w-2xl max-h-96 overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Eye className="h-5 w-5 text-blue-600" />
              Activity Log - {selectedUser?.username}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            {activityLogs.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-4">No activity recorded</p>
            ) : (
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {activityLogs.map((log) => (
                  <div key={log.id} className="border rounded p-3 bg-slate-50">
                    <div className="flex justify-between items-start gap-2">
                      <div className="flex-1">
                        <p className="font-medium text-sm">{log.action}</p>
                        <p className="text-xs text-muted-foreground">{log.details}</p>
                      </div>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">
                        {new Date(log.createdAt).toLocaleString()}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setActivityLogDialogOpen(false)}>Close</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default UserManagement;
