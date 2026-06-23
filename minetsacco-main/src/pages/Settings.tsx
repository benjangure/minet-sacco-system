import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { User, Lock, Shield, Eye, EyeOff, Check, X } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

import { getApiBaseUrl } from "../config/api";
const API_BASE_URL = getApiBaseUrl();

interface UserProfile {
  id: number;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  role: string;
}

const Settings = () => {
  const { session, role } = useAuth();
  const { toast } = useToast();
  
  // Log token availability for debugging
  useEffect(() => {
    console.log("Settings: Session token available:", !!session?.token);
  }, [session?.token]);
  
  // Profile fields
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileFormData, setProfileFormData] = useState<Partial<UserProfile>>({});
  const [profileLoading, setProfileLoading] = useState(false);
  const [loadingProfile, setLoadingProfile] = useState(true);
  
  // Password fields
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Load profile data on mount
  useEffect(() => {
    const fetchProfile = async () => {
      if (!session?.token) {
        setLoadingProfile(false);
        return;
      }

      try {
        const response = await fetch(`${API_BASE_URL}/users/profile/me`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${session.token}`,
          },
        });

        if (response.ok) {
          const data = await response.json();
          const profileData = data.data || data;
          setProfile(profileData);
          setProfileFormData(profileData);
        } else {
          console.error("Failed to load profile, status:", response.status);
          toast({ title: "Error", description: "Failed to load profile", variant: "destructive" });
        }
      } catch (error) {
        console.error("Error loading profile:", error);
        toast({ title: "Error", description: "Failed to load profile. Please refresh.", variant: "destructive" });
      } finally {
        setLoadingProfile(false);
      }
    };

    fetchProfile();
  }, [session?.token, toast]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!session?.token) {
      toast({ title: "Error", description: "Authentication session expired. Please log in again.", variant: "destructive" });
      return;
    }

    if (!profileFormData.email?.trim()) {
      toast({ title: "Error", description: "Email is required", variant: "destructive" });
      return;
    }

    if (!profileFormData.email?.includes("@")) {
      toast({ title: "Error", description: "Please enter a valid email", variant: "destructive" });
      return;
    }

    setProfileLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/users/profile/me`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session.token}`,
        },
        body: JSON.stringify({
          firstName: profileFormData.firstName || null,
          lastName: profileFormData.lastName || null,
          email: profileFormData.email,
          phone: profileFormData.phone || null,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        const updatedProfile = data.data || data;
        setProfile(updatedProfile);
        setProfileFormData(updatedProfile);
        setEditingProfile(false);
        toast({ title: "Success", description: "Profile updated successfully" });
      } else if (response.status === 401) {
        // Authentication failed - token is invalid or expired
        // Similar to password change, redirect to re-login
        toast({ title: "Session Expired", description: "Your authentication session has expired. Please log in again to continue." });
        setTimeout(() => {
          window.location.href = "/login";
        }, 1500);
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to update profile", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to update profile", variant: "destructive" });
    }
    setProfileLoading(false);
  };

  const handleCancelEdit = () => {
    setProfileFormData(profile || {});
    setEditingProfile(false);
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      toast({ title: "Error", description: "New passwords do not match", variant: "destructive" });
      return;
    }

    if (newPassword.length < 8) {
      toast({ title: "Error", description: "Password must be at least 8 characters", variant: "destructive" });
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/users/change-password`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword,
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Password changed successfully. Please log in again with your new password." });
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        
        // Clear session and redirect to login since JWT is now invalid
        setTimeout(() => {
          window.location.href = "/login";
        }, 1500);
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to change password", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to change password. Feature may not be implemented yet.", variant: "destructive" });
    }
    setLoading(false);
  };

  const roleLabels: Record<string, string> = {
    ADMIN: "System Administrator",
    TREASURER: "Treasurer / Finance",
    LOAN_OFFICER: "Loan Officer",
    CREDIT_COMMITTEE: "Credit Committee",
    AUDITOR: "Auditor / Compliance",
    TELLER: "Teller / Data Entry",
    CUSTOMER_SUPPORT: "Customer Support",
  };

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">Settings</h1>
        <p className="text-muted-foreground">Manage your account settings and preferences</p>
      </div>

      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
        </TabsList>

        <TabsContent value="profile" className="space-y-6">
          {loadingProfile ? (
            <Card className="border-none shadow-sm">
              <CardContent className="pt-6">
                <p className="text-muted-foreground">Loading profile...</p>
              </CardContent>
            </Card>
          ) : (
            <>
              <Card className="border-none shadow-sm">
                <CardHeader className="flex flex-row items-center justify-between">
                  <CardTitle className="flex items-center gap-2">
                    <User className="h-5 w-5" />
                    Personal Information
                  </CardTitle>
                  {!editingProfile && (
                    <Button variant="outline" size="sm" onClick={() => setEditingProfile(true)}>
                      Edit
                    </Button>
                  )}
                </CardHeader>
                <CardContent className="space-y-4">
                  {editingProfile ? (
                    <form onSubmit={handleUpdateProfile} className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label>First Name</Label>
                          <Input
                            value={profileFormData.firstName || ""}
                            onChange={(e) => setProfileFormData({ ...profileFormData, firstName: e.target.value })}
                            placeholder="Enter first name"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label>Last Name</Label>
                          <Input
                            value={profileFormData.lastName || ""}
                            onChange={(e) => setProfileFormData({ ...profileFormData, lastName: e.target.value })}
                            placeholder="Enter last name"
                          />
                        </div>
                      </div>
                      
                      <div className="space-y-2">
                        <Label>Email Address</Label>
                        <Input
                          type="email"
                          value={profileFormData.email || ""}
                          onChange={(e) => setProfileFormData({ ...profileFormData, email: e.target.value })}
                          placeholder="Enter email address"
                          required
                        />
                      </div>

                      <div className="space-y-2">
                        <Label>Phone Number</Label>
                        <Input
                          value={profileFormData.phone || ""}
                          onChange={(e) => setProfileFormData({ ...profileFormData, phone: e.target.value })}
                          placeholder="Enter phone number"
                        />
                      </div>

                      <div className="flex gap-2 pt-4">
                        <Button type="submit" disabled={profileLoading} className="gap-2">
                          {profileLoading ? "Saving..." : (
                            <>
                              <Check className="h-4 w-4" />
                              Save Changes
                            </>
                          )}
                        </Button>
                        <Button type="button" variant="outline" onClick={handleCancelEdit} disabled={profileLoading}>
                          <X className="h-4 w-4" />
                          Cancel
                        </Button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-muted-foreground">First Name</Label>
                          <p className="text-sm font-medium">{profile?.firstName || "Not set"}</p>
                        </div>
                        <div className="space-y-2">
                          <Label className="text-muted-foreground">Last Name</Label>
                          <p className="text-sm font-medium">{profile?.lastName || "Not set"}</p>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <Label className="text-muted-foreground">Email Address</Label>
                        <p className="text-sm font-medium">{profile?.email || "Not set"}</p>
                      </div>

                      <div className="space-y-2">
                        <Label className="text-muted-foreground">Phone Number</Label>
                        <p className="text-sm font-medium">{profile?.phone || "Not set"}</p>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-muted-foreground">Username</Label>
                          <Input value={profile?.username || ""} disabled />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-muted-foreground">Role</Label>
                          <Input value={role || ""} disabled />
                        </div>
                      </div>
                    </>
                  )}
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>

        <TabsContent value="security" className="space-y-6">
          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Lock className="h-5 w-5" />
                Change Password
              </CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleChangePassword} className="space-y-4">
                <div className="space-y-2">
                  <Label>Current Password</Label>
                  <div className="relative">
                    <Input
                      type={showCurrentPassword ? "text" : "password"}
                      value={currentPassword}
                      onChange={e => setCurrentPassword(e.target.value)}
                      required
                      placeholder="Enter current password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>New Password</Label>
                  <div className="relative">
                    <Input
                      type={showNewPassword ? "text" : "password"}
                      value={newPassword}
                      onChange={e => setNewPassword(e.target.value)}
                      required
                      minLength={8}
                      placeholder="Enter new password (min 8 characters)"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Confirm New Password</Label>
                  <div className="relative">
                    <Input
                      type={showConfirmPassword ? "text" : "password"}
                      value={confirmPassword}
                      onChange={e => setConfirmPassword(e.target.value)}
                      required
                      minLength={8}
                      placeholder="Confirm new password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
                <Button type="submit" disabled={loading}>
                  {loading ? "Changing Password..." : "Change Password"}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader>
              <CardTitle>Security Tips</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>• Use a strong password with at least 8 characters</li>
                <li>• Include uppercase, lowercase, numbers, and special characters</li>
                <li>• Don't share your password with anyone</li>
                <li>• Change your password regularly</li>
                <li>• Log out when you're done using the system</li>
              </ul>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default Settings;
