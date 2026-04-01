import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Shield, Users, Landmark, PiggyBank, FileText, Settings, ArrowRight } from "lucide-react";

const roleLabels: Record<string, string> = {
  admin: "System Administrator",
  treasurer: "Treasurer / Finance",
  loan_officer: "Loan Officer",
  credit_committee: "Credit Committee",
  auditor: "Auditor / Compliance",
  teller: "Teller / Data Entry",
  helpdesk: "Customer Support",
};

const Guide = () => {
  const { role } = useAuth();

  return (
    <div className="max-w-4xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">System User Guide</h1>
        <p className="text-muted-foreground mt-1">Learn how to operate the Minet SACCO Management System</p>
      </div>

      <Tabs defaultValue="overview">
        <TabsList className="flex-wrap h-auto">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="roles">Role Hierarchy</TabsTrigger>
          <TabsTrigger value="getting-started">Getting Started</TabsTrigger>
          <TabsTrigger value="modules">Modules</TabsTrigger>
          <TabsTrigger value="workflows">Workflows</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6 mt-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle>What is Minet SACCO?</CardTitle></CardHeader>
            <CardContent className="space-y-3 text-sm">
              <p>Minet SACCO Management System is a comprehensive platform for managing savings and credit cooperative operations. It handles member management, savings/deposits, loan processing, and financial reporting.</p>
              <p>The system uses <strong>Role-Based Access Control (RBAC)</strong> — each user sees only the features relevant to their role.</p>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle>Your Current Role</CardTitle></CardHeader>
            <CardContent>
              {role ? (
                <div className="space-y-2">
                  <Badge className="text-sm px-3 py-1">{roleLabels[role]}</Badge>
                  <p className="text-sm text-muted-foreground mt-2">
                    {role === "admin" && "You have full access to all modules. You can create Treasurers, Loan Officers, Credit Committee members, and Auditors."}
                    {role === "treasurer" && "You manage finances, deposits, withdrawals, and can create Tellers and Helpdesk staff."}
                    {role === "loan_officer" && "You process loan applications and manage member loan portfolios."}
                    {role === "credit_committee" && "You review and approve/reject loan applications."}
                    {role === "auditor" && "You have read-only access to all data for compliance and audit purposes."}
                    {role === "teller" && "You handle day-to-day member transactions — deposits and withdrawals."}
                    {role === "helpdesk" && "You assist members with inquiries and basic support."}
                  </p>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No role assigned yet. Contact your administrator.</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="roles" className="space-y-6 mt-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="flex items-center gap-2"><Shield className="h-5 w-5 text-primary" /> Role Hierarchy</CardTitle></CardHeader>
            <CardContent className="space-y-4 text-sm">
              <p>Users can only create and delete staff below their level:</p>
              
              <div className="space-y-3">
                <div className="p-3 rounded-lg bg-red-50 border border-red-200">
                  <p className="font-semibold text-red-800">System Administrator</p>
                  <p className="text-red-700 text-xs mt-1">Can create: Treasurer, Loan Officer, Credit Committee, Auditor</p>
                  <p className="text-red-700 text-xs">Access: All modules, settings, user management</p>
                </div>
                
                <div className="flex justify-center"><ArrowRight className="h-4 w-4 text-muted-foreground rotate-90" /></div>
                
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-lg bg-blue-50 border border-blue-200">
                    <p className="font-semibold text-blue-800">Treasurer</p>
                    <p className="text-blue-700 text-xs mt-1">Can create: Teller, Helpdesk</p>
                    <p className="text-blue-700 text-xs">Access: Savings, members, loans, reports, user mgmt</p>
                  </div>
                  <div className="p-3 rounded-lg bg-green-50 border border-green-200">
                    <p className="font-semibold text-green-800">Loan Officer</p>
                    <p className="text-green-700 text-xs mt-1">Cannot create users</p>
                    <p className="text-green-700 text-xs">Access: Members, loans</p>
                  </div>
                  <div className="p-3 rounded-lg bg-yellow-50 border border-yellow-200">
                    <p className="font-semibold text-yellow-800">Credit Committee</p>
                    <p className="text-yellow-700 text-xs mt-1">Cannot create users</p>
                    <p className="text-yellow-700 text-xs">Access: Loan approvals</p>
                  </div>
                  <div className="p-3 rounded-lg bg-purple-50 border border-purple-200">
                    <p className="font-semibold text-purple-800">Auditor</p>
                    <p className="text-purple-700 text-xs mt-1">Cannot create users</p>
                    <p className="text-purple-700 text-xs">Access: Reports (read-only)</p>
                  </div>
                </div>
                
                <div className="flex justify-center"><ArrowRight className="h-4 w-4 text-muted-foreground rotate-90" /></div>
                
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-lg bg-indigo-50 border border-indigo-200">
                    <p className="font-semibold text-indigo-800">Teller</p>
                    <p className="text-indigo-700 text-xs">Access: Members, savings transactions</p>
                  </div>
                  <div className="p-3 rounded-lg bg-orange-50 border border-orange-200">
                    <p className="font-semibold text-orange-800">Helpdesk</p>
                    <p className="text-orange-700 text-xs">Access: Member inquiries</p>
                  </div>
                </div>
              </div>

              <div className="p-3 bg-muted rounded-lg mt-4">
                <p className="font-medium text-sm">🔒 Important Rules:</p>
                <ul className="list-disc ml-5 text-xs space-y-1 mt-1">
                  <li>You can only <strong>delete</strong> users you personally created</li>
                  <li>You can <strong>see</strong> all users, but manage only those within your hierarchy</li>
                  <li>The Admin cannot delete users created by a Treasurer</li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="getting-started" className="space-y-6 mt-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle>Step-by-Step Setup</CardTitle></CardHeader>
            <CardContent className="space-y-4 text-sm">
              <div className="space-y-4">
                <div className="flex gap-3">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs font-bold">1</div>
                  <div>
                    <p className="font-medium">Create Staff Users</p>
                    <p className="text-muted-foreground">Go to User Management → Create Staff User. As Admin, create a Treasurer first.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs font-bold">2</div>
                  <div>
                    <p className="font-medium">Register Members</p>
                    <p className="text-muted-foreground">Go to Members → Register Member. Fill in personal, employment, and next of kin details.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs font-bold">3</div>
                  <div>
                    <p className="font-medium">Process Deposits</p>
                    <p className="text-muted-foreground">Go to Savings → New Transaction. Select a member and process their deposit.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs font-bold">4</div>
                  <div>
                    <p className="font-medium">Apply for Loans</p>
                    <p className="text-muted-foreground">Go to Loans → New Loan Application. Select member, product, amount, and term.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-xs font-bold">5</div>
                  <div>
                    <p className="font-medium">Approve & Disburse Loans</p>
                    <p className="text-muted-foreground">Submitted loans appear for review. Approve → then Disburse to complete the process.</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="modules" className="space-y-6 mt-6">
          {[
            { icon: Users, title: "Members", desc: "Register new SACCO members with KYC info. Each member automatically gets a savings account. You can search, filter by status, and view full profiles." },
            { icon: PiggyBank, title: "Savings & Deposits", desc: "Process deposits and withdrawals for members. View account balances and transaction history. Supports cash, M-Pesa, bank transfer, cheque, and payroll deduction." },
            { icon: Landmark, title: "Loans", desc: "Submit loan applications, track status through the workflow (submitted → approved → disbursed → repaying → fully paid). Each loan product has different rates and terms." },
            { icon: FileText, title: "Reports", desc: "Generate reports: Member listing, Loan portfolio, Savings summary, Transaction history, and Defaulted loans. Export to CSV." },
            { icon: Shield, title: "User Management", desc: "Create staff accounts and assign roles. View all users and their roles. Delete only users you created." },
            { icon: Settings, title: "Settings", desc: "Manage loan products — create, edit, and configure interest rates, limits, and guarantor requirements." },
          ].map(mod => (
            <Card key={mod.title} className="border-none shadow-sm">
              <CardHeader>
                <CardTitle className="flex items-center gap-2"><mod.icon className="h-5 w-5 text-primary" />{mod.title}</CardTitle>
              </CardHeader>
              <CardContent><p className="text-sm text-muted-foreground">{mod.desc}</p></CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="workflows" className="space-y-6 mt-6">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle>Loan Processing Workflow</CardTitle></CardHeader>
            <CardContent className="text-sm space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                {["Draft", "→", "Submitted", "→", "Under Review", "→", "Approved / Rejected", "→", "Disbursed", "→", "Repaying", "→", "Fully Paid"].map((step, i) =>
                  step === "→" ? <ArrowRight key={i} className="h-4 w-4 text-muted-foreground" /> :
                  <Badge key={i} variant="outline">{step}</Badge>
                )}
              </div>
              <ul className="list-disc ml-5 text-xs space-y-1 mt-3 text-muted-foreground">
                <li><strong>Loan Officer / Teller</strong> creates the application</li>
                <li><strong>Credit Committee / Admin</strong> approves or rejects</li>
                <li><strong>Treasurer / Admin</strong> disburses the approved loan</li>
                <li><strong>Teller</strong> records repayments</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle>Member Registration Workflow</CardTitle></CardHeader>
            <CardContent className="text-sm space-y-2">
              <ol className="list-decimal ml-5 space-y-1 text-muted-foreground">
                <li>Staff registers member with personal & employment details</li>
                <li>System auto-generates member number (MNT-XXXXX)</li>
                <li>System auto-creates savings account for the member</li>
                <li>Staff can immediately process deposits</li>
              </ol>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default Guide;
