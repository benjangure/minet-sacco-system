import {
  LayoutDashboard, Users, Landmark, PiggyBank, FileText, Settings, Shield, LogOut, HelpCircle, Package, Upload, CheckCircle2, BarChart3,
} from "lucide-react";
import { NavLink } from "@/components/NavLink";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent, SidebarGroupLabel,
  SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarHeader, SidebarFooter, useSidebar,
} from "@/components/ui/sidebar";
import logo from '@/assets/images/logo.png';

const allMainItems = [
  { title: "Dashboard", url: "/", icon: LayoutDashboard, roles: ["admin", "treasurer", "loan_officer", "credit_committee", "auditor", "teller", "customer_support"] },
  { title: "Members", url: "/members", icon: Users, roles: ["admin", "treasurer", "loan_officer", "teller", "customer_support"] },
  { title: "Loans", url: "/loans", icon: Landmark, roles: ["admin", "loan_officer", "credit_committee", "treasurer"] },
  { title: "Loan Repayments", url: "/loan-repayment-recording", icon: CheckCircle2, roles: ["teller", "treasurer"] },
  { title: "Savings", url: "/savings", icon: PiggyBank, roles: ["admin", "treasurer", "teller"] },
  { title: "Member Transactions", url: "/member-transaction-history", icon: FileText, roles: ["admin", "treasurer", "loan_officer", "credit_committee", "auditor"] },
  { title: "Teller Member Context", url: "/teller-member-context", icon: Users, roles: ["teller"] },
  { title: "Customer Support", url: "/customer-support-portal", icon: FileText, roles: ["customer_support"] },
  { title: "Bulk Processing", url: "/bulk-processing", icon: Upload, roles: ["treasurer", "credit_committee"] },
  { title: "Reports", url: "/reports", icon: FileText, roles: ["admin", "treasurer", "auditor"] },
];

const kycItems = [
  { title: "Upload KYC Documents", url: "/kyc-upload", icon: Upload, roles: ["customer_support"] },
  { title: "Track KYC Uploads", url: "/kyc-upload-tracking", icon: FileText, roles: ["customer_support"] },
  { title: "Verify KYC Documents", url: "/kyc-approval", icon: CheckCircle2, roles: ["teller"] },
  { title: "View Member Documents", url: "/view-documents", icon: FileText, roles: ["teller"] },
];

const allAdminItems = [
  { title: "User Management", url: "/admin/users", icon: Shield, roles: ["admin", "treasurer"] },
  { title: "Loan Products", url: "/admin/loan-products", icon: Package, roles: ["admin"] },
  { title: "Fund Configuration", url: "/admin/fund-configuration", icon: Settings, roles: ["admin"] },
  { title: "Loan Eligibility Rules", url: "/admin/loan-eligibility-rules", icon: Settings, roles: ["admin"] },
  { title: "Audit Trail", url: "/admin/audit-trail", icon: BarChart3, roles: ["admin", "auditor"] },
  { title: "Audit Reports", url: "/audit-reports", icon: BarChart3, roles: ["admin", "auditor"] },
  { title: "Settings", url: "/settings", icon: Settings, roles: ["admin"] },
  { title: "User Guide", url: "/guide", icon: HelpCircle, roles: ["admin", "treasurer", "loan_officer", "credit_committee", "auditor", "teller", "customer_support"] },
];

const roleLabels: Record<string, string> = {
  admin: "Admin", treasurer: "Treasurer", loan_officer: "Loan Officer",
  credit_committee: "Committee", auditor: "Auditor", teller: "Teller", helpdesk: "Support",
};

export function AppSidebar() {
  const { state } = useSidebar();
  const collapsed = state === "collapsed";
  const { profile, role, signOut } = useAuth();

  const mainItems = allMainItems.filter(item => !role || item.roles.includes(role.toLowerCase()));
  const kycItemsFiltered = kycItems.filter(item => !role || item.roles.includes(role.toLowerCase()));
  const adminItems = allAdminItems.filter(item => !role || item.roles.includes(role.toLowerCase()));

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="border-b border-sidebar-border px-4 py-4">
        <div className="flex items-center gap-2">
          <img src={logo} alt="Minet SACCO" className="h-8 w-auto shrink-0" />
          {!collapsed && (
            <span className="text-lg font-bold tracking-tight text-foreground">
              Minet <span className="text-primary">SACCO</span>
            </span>
          )}
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Main Menu</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {mainItems.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton asChild>
                    <NavLink to={item.url} end={item.url === "/"} className="hover:bg-accent" activeClassName="bg-accent text-accent-foreground font-medium">
                      <item.icon className="mr-2 h-4 w-4" />
                      {!collapsed && <span>{item.title}</span>}
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        {adminItems.length > 0 && (
          <SidebarGroup>
            <SidebarGroupLabel>Administration</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {adminItems.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton asChild>
                      <NavLink to={item.url} className="hover:bg-accent" activeClassName="bg-accent text-accent-foreground font-medium">
                        <item.icon className="mr-2 h-4 w-4" />
                        {!collapsed && <span>{item.title}</span>}
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}

        {kycItemsFiltered.length > 0 && (
          <SidebarGroup>
            <SidebarGroupLabel>KYC Management</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {kycItemsFiltered.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton asChild>
                      <NavLink to={item.url} className="hover:bg-accent" activeClassName="bg-accent text-accent-foreground font-medium">
                        <item.icon className="mr-2 h-4 w-4" />
                        {!collapsed && <span>{item.title}</span>}
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border p-3">
        {!collapsed ? (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-primary/10 text-primary text-xs">
                  {profile?.full_name?.split(" ").map(n => n[0]).join("").slice(0, 2) || "U"}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{profile?.full_name || "User"}</p>
                {role && <Badge variant="outline" className="text-[10px] px-1.5 py-0">{roleLabels[role] || role}</Badge>}
              </div>
            </div>
            <Button variant="ghost" size="sm" className="w-full justify-start text-muted-foreground hover:text-foreground" onClick={signOut}>
              <LogOut className="mr-2 h-4 w-4" />Sign Out
            </Button>
          </div>
        ) : (
          <Button variant="ghost" size="icon" onClick={signOut} className="mx-auto">
            <LogOut className="h-4 w-4" />
          </Button>
        )}
      </SidebarFooter>
    </Sidebar>
  );
}
