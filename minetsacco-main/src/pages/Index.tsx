import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, Landmark, PiggyBank, AlertTriangle, Shield, FileText } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import DepositApprovalPanel from "@/components/DepositApprovalPanel";

const API_BASE_URL = "http://localhost:8080/api";

const Index = () => {
  const { role, session } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalMembers: 0, activeMembers: 0, totalLoans: 0, activeLoans: 0,
    totalSavings: 0, totalShares: 0, pendingLoans: 0, defaultedLoans: 0,
    pendingMembers: 0,
    kycDocumentsPending: 0, kycDocumentsVerified: 0, membersWithIncompleteKyc: 0,
    myUploadedDocuments: 0,
    approvedLoansForDisbursement: 0,
    pendingDeposits: 0,
  });
  const [loansByProduct, setLoansByProduct] = useState<any[]>([]);
  const [membersByStatus, setMembersByStatus] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [depositPanelOpen, setDepositPanelOpen] = useState(false);

  useEffect(() => {
    const fetchDashboard = async () => {
      if (!session?.token) return;

      try {
        // Fetch members
        const membersRes = await fetch(`${API_BASE_URL}/members`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        });
        const membersData = membersRes.ok ? (await membersRes.json()).data || [] : [];

        // Fetch accounts
        const accountsRes = await fetch(`${API_BASE_URL}/accounts`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        });
        const accountsData = accountsRes.ok ? (await accountsRes.json()).data || [] : [];

        // Fetch loans (if endpoint exists)
        const loansRes = await fetch(`${API_BASE_URL}/loans`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const loansData = loansRes?.ok ? (await loansRes.json()).data || [] : [];

        // Fetch KYC data
        const kycPendingRes = await fetch(`${API_BASE_URL}/kyc-documents/pending`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const kycPendingData = kycPendingRes?.ok ? (await kycPendingRes.json()).data || [] : [];

        const kycIncompleteRes = await fetch(`${API_BASE_URL}/kyc-documents/incomplete-members`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const kycIncompleteData = kycIncompleteRes?.ok ? (await kycIncompleteRes.json()).data || [] : [];

        // Fetch my uploaded documents (for CUSTOMER_SUPPORT)
        const myUploadsRes = await fetch(`${API_BASE_URL}/kyc-documents/my-uploads`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const myUploadsData = myUploadsRes?.ok ? (await myUploadsRes.json()).data || [] : [];

        // Fetch approved loans (for TREASURER)
        const approvedLoansRes = await fetch(`${API_BASE_URL}/bulk/loan-items/approved`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const approvedLoansData = approvedLoansRes?.ok ? (await approvedLoansRes.json()).data || [] : [];

        // Fetch pending deposits (for TELLER)
        const pendingDepositsRes = await fetch(`${API_BASE_URL}/teller/deposit-requests/pending`, {
          headers: { "Authorization": `Bearer ${session.token}` },
        }).catch(() => null);
        const pendingDepositsData = pendingDepositsRes?.ok ? (await pendingDepositsRes.json()).data || [] : [];

        // Calculate stats
        const activeMembers = membersData.filter((m: any) => m.status === "ACTIVE").length;
        const pendingMembers = membersData.filter((m: any) => m.status === "PENDING").length;
        
        const savingsAccounts = accountsData.filter((a: any) => a.accountType === "SAVINGS");
        const sharesAccounts = accountsData.filter((a: any) => a.accountType === "SHARES");
        const totalSavings = savingsAccounts.reduce((sum: number, a: any) => sum + (a.balance || 0), 0);
        const totalShares = sharesAccounts.reduce((sum: number, a: any) => sum + (a.balance || 0), 0);

        const activeLoans = loansData.filter((l: any) => ["DISBURSED", "ACTIVE"].includes(l.status)).length;
        const pendingLoans = loansData.filter((l: any) => ["PENDING", "PENDING_GUARANTOR_APPROVAL", "PENDING_LOAN_OFFICER_REVIEW", "PENDING_CREDIT_COMMITTEE", "PENDING_TREASURER", "UNDER_REVIEW"].includes(l.status)).length;
        const defaultedLoans = loansData.filter((l: any) => l.status === "DEFAULTED").length;
        const approvedLoansForDisbursement = loansData.filter((l: any) => l.status === "APPROVED").length;

        setStats({
          totalMembers: membersData.length,
          activeMembers,
          pendingMembers,
          totalLoans: loansData.length,
          activeLoans,
          pendingLoans,
          defaultedLoans,
          totalSavings,
          totalShares,
          kycDocumentsPending: kycPendingData.length,
          kycDocumentsVerified: kycIncompleteData.reduce((sum: number, m: any) => sum + (m.documentsVerified || 0), 0),
          membersWithIncompleteKyc: kycIncompleteData.length,
          myUploadedDocuments: myUploadsData.length,
          approvedLoansForDisbursement,
          pendingDeposits: pendingDepositsData.length,
        });

        // Members by status for pie chart
        const statusCount: Record<string, number> = {};
        membersData.forEach((m: any) => {
          statusCount[m.status] = (statusCount[m.status] || 0) + 1;
        });
        setMembersByStatus(
          Object.entries(statusCount).map(([name, value]) => ({ name, value }))
        );

        // Loans by product (if available)
        if (loansData.length > 0) {
          const productMap: Record<string, { name: string; count: number; amount: number }> = {};
          loansData.forEach((l: any) => {
            const name = l.loanProduct?.name || "Unknown";
            if (!productMap[name]) productMap[name] = { name, count: 0, amount: 0 };
            productMap[name].count++;
            productMap[name].amount += Number(l.amount || 0);
          });
          setLoansByProduct(Object.values(productMap));
        }

      } catch (error) {
        console.error("Error fetching dashboard data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, [session]);

  const roleGreetings: Record<string, string> = {
    ADMIN: "System Administrator Dashboard",
    TREASURER: "Treasurer Dashboard — Finance & Accounts",
    LOAN_OFFICER: "Loan Officer Dashboard — Loan Applications",
    CREDIT_COMMITTEE: "Credit Committee Dashboard — Loan Approvals",
    AUDITOR: "Auditor Dashboard — Financial Oversight & Compliance",
    TELLER: "Teller Dashboard — Member Transactions",
    CUSTOMER_SUPPORT: "Support Dashboard — Member Inquiries",
  };

  // Get user's first name from session
  const getUserFirstName = () => {
    if (session?.user?.username) {
      return session.user.username.charAt(0).toUpperCase() + session.user.username.slice(1);
    }
    return "User";
  };

  // Role-specific stat cards
  const getStatCards = () => {
    const allCards = {
      members: { title: "Total Members", value: stats.totalMembers.toLocaleString(), icon: Users, sub: `${stats.activeMembers} active`, link: "/members" },
      pending: { title: "Pending Approvals", value: stats.approvedLoansForDisbursement.toLocaleString(), icon: FileText, sub: "Loans ready for disbursement", link: "/loans?status=APPROVED" },
      loans: { title: "Active Loans", value: stats.activeLoans.toLocaleString(), icon: Landmark, sub: `${stats.pendingLoans} pending approval`, link: "/loans" },
      savings: { title: "Total Savings", value: `KES ${stats.totalSavings.toLocaleString()}`, icon: PiggyBank, sub: `KES ${stats.totalShares.toLocaleString()} in shares`, link: "/savings" },
      defaulted: { title: "Defaulted Loans", value: stats.defaultedLoans.toLocaleString(), icon: AlertTriangle, sub: "Requires attention", link: "/loans" },
      kycPending: { title: "KYC Documents Pending", value: stats.kycDocumentsPending.toLocaleString(), icon: FileText, sub: "Awaiting verification", link: "/kyc-approval" },
      kycIncomplete: { title: "Members with Incomplete KYC", value: stats.membersWithIncompleteKyc.toLocaleString(), icon: Shield, sub: "Awaiting document upload", link: "/kyc-approval" },
      myUploads: { title: "My Uploaded Documents", value: stats.myUploadedDocuments.toLocaleString(), icon: FileText, sub: "Documents I have uploaded", link: "/kyc-upload-tracking" },
      deposits: { title: "Pending Deposits", value: stats.pendingDeposits.toLocaleString(), icon: PiggyBank, sub: "Awaiting approval", link: "#" },
    };

    switch (role) {
      case "ADMIN": return [allCards.members, allCards.loans, allCards.savings, allCards.defaulted];
      case "TREASURER": return [allCards.pending, allCards.savings, allCards.members, allCards.loans];
      case "LOAN_OFFICER": return [allCards.loans, allCards.pending, allCards.members, allCards.defaulted];
      case "CREDIT_COMMITTEE": return [allCards.loans, allCards.defaulted];
      case "AUDITOR": return [allCards.members, allCards.loans, allCards.savings, allCards.defaulted];
      case "TELLER": return [allCards.deposits, allCards.kycPending, allCards.kycIncomplete, allCards.members];
      case "CUSTOMER_SUPPORT": return [allCards.myUploads, allCards.members, allCards.savings, allCards.loans];
      default: return [allCards.members, allCards.loans, allCards.savings, allCards.defaulted];
    }
  };

  const statCards = getStatCards();
  const showCharts = ["ADMIN", "TREASURER", "AUDITOR", "LOAN_OFFICER", "CUSTOMER_SUPPORT"].includes(role || "");

  const COLORS = ["hsl(0, 72%, 51%)", "hsl(0, 0%, 70%)", "hsl(40, 90%, 50%)", "hsl(0, 0%, 45%)"];

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground">
          Welcome, {getUserFirstName()}
        </h1>
        <p className="text-muted-foreground mt-1">
          {role ? roleGreetings[role] : "Minet SACCO Management System"}
        </p>
      </div>

      {/* Auditor Role Info Card */}
      {role === "AUDITOR" && (
        <Card className="border-l-4 border-l-blue-500 mb-6 bg-blue-50">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <FileText className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div>
                <h3 className="font-semibold text-blue-900 mb-2">Your Responsibilities as Auditor</h3>
                <ul className="text-sm text-blue-800 space-y-1">
                  <li>• <strong>Financial Oversight:</strong> Review all financial transactions and account balances</li>
                  <li>• <strong>Compliance Monitoring:</strong> Ensure adherence to SACCO regulations and bylaws</li>
                  <li>• <strong>Risk Assessment:</strong> Identify financial irregularities and potential risks</li>
                  <li>• <strong>Report Generation:</strong> Create comprehensive audit and compliance reports</li>
                  <li>• <strong>Read-Only Access:</strong> View all data without modifying transactions</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Customer Support Role Info Card */}
      {role === "CUSTOMER_SUPPORT" && (
        <Card className="border-l-4 border-l-green-500 mb-6 bg-green-50">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <FileText className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div>
                <h3 className="font-semibold text-green-900 mb-2">Your Responsibilities as Customer Support</h3>
                <ul className="text-sm text-green-800 space-y-1">
                  <li>• <strong>KYC Document Collection:</strong> Upload KYC documents for each member</li>
                  <li>• <strong>Document Management:</strong> Organize and manage member documentation</li>
                  <li>• <strong>Member Support:</strong> Assist members with inquiries and document requirements</li>
                  <li>• <strong>Compliance Preparation:</strong> Ensure all required documents are collected</li>
                  <li>• <strong>Document Tracking:</strong> Monitor KYC completion status for all members</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Teller Role Info Card */}
      {role === "TELLER" && (
        <Card className="border-l-4 border-l-purple-500 mb-6 bg-purple-50">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <Shield className="h-5 w-5 text-purple-600 mt-0.5 flex-shrink-0" />
              <div>
                <h3 className="font-semibold text-purple-900 mb-2">Your Responsibilities as Teller</h3>
                <ul className="text-sm text-purple-800 space-y-1">
                  <li>• <strong>KYC Verification:</strong> Review and verify KYC documents submitted by members</li>
                  <li>• <strong>Document Approval:</strong> Approve or reject individual KYC documents</li>
                  <li>• <strong>Bulk Approval:</strong> Bulk approve members with complete KYC documentation</li>
                  <li>• <strong>Member Transactions:</strong> Process deposits, withdrawals, and transfers</li>
                  <li>• <strong>Compliance Assurance:</strong> Ensure all members meet KYC requirements</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <div className={`grid gap-6 md:grid-cols-2 lg:grid-cols-${Math.min(statCards.length, 4)} mb-8`}>
        {statCards.map((stat) => (
          <Card 
            key={stat.title} 
            className={`border-none shadow-sm cursor-pointer hover:shadow-md transition-shadow ${
              stat.title === "Pending Deposits" ? "border-2 border-yellow-300 bg-yellow-50" : ""
            }`}
            onClick={() => {
              if (stat.title === "Pending Deposits") {
                setDepositPanelOpen(true);
              } else {
                navigate(stat.link);
              }
            }}
          >
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{stat.title}</CardTitle>
              <stat.icon className={`h-5 w-5 ${stat.title === "Pending Deposits" ? "text-yellow-600" : "text-primary"}`} />
            </CardHeader>
            <CardContent>
              <div className={`text-2xl font-bold ${stat.title === "Pending Deposits" ? "text-yellow-900" : "text-foreground"}`}>
                {loading ? "..." : stat.value}
              </div>
              <p className={`text-xs mt-1 ${stat.title === "Pending Deposits" ? "text-yellow-700" : "text-muted-foreground"}`}>
                {stat.sub}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {showCharts && (
        <div className="grid gap-6 md:grid-cols-2">
          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-lg">Loans by Product</CardTitle></CardHeader>
            <CardContent>
              {loansByProduct.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={loansByProduct}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                    <YAxis />
                    <Tooltip formatter={(value: number) => value.toLocaleString()} />
                    <Bar dataKey="count" fill="hsl(0, 72%, 51%)" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[300px] flex items-center justify-center text-muted-foreground">No loan data yet</div>
              )}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm">
            <CardHeader><CardTitle className="text-lg">Members by Status</CardTitle></CardHeader>
            <CardContent>
              {membersByStatus.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie data={membersByStatus} cx="50%" cy="50%" outerRadius={100} dataKey="value" label={({ name, value }) => `${name} (${value})`}>
                      {membersByStatus.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[300px] flex items-center justify-center text-muted-foreground">No member data yet</div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Auditor Quick Access Tools */}
      {role === "AUDITOR" && (
        <Card className="border-none shadow-sm mt-6">
          <CardHeader>
            <CardTitle className="text-lg font-semibold text-foreground">Audit & Compliance Tools</CardTitle>
            <p className="text-sm text-muted-foreground">Quick access to auditing and reporting functions</p>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-3">
              <Button 
                variant="outline" 
                className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                onClick={() => navigate("/reports")}
              >
                <FileText className="h-8 w-8 text-primary" />
                <span className="font-semibold">Generate Reports</span>
                <span className="text-xs text-muted-foreground text-center">Export Members, Loans, and Accounts reports</span>
              </Button>
              <Button 
                variant="outline" 
                className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                onClick={() => navigate("/loans")}
              >
                <Landmark className="h-8 w-8 text-primary" />
                <span className="font-semibold">Review Loan Portfolio</span>
                <span className="text-xs text-muted-foreground text-center">Monitor loan applications and disbursements</span>
              </Button>
              <Button 
                variant="outline" 
                className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                onClick={() => navigate("/savings")}
              >
                <PiggyBank className="h-8 w-8 text-primary" />
                <span className="font-semibold">Review Transactions</span>
                <span className="text-xs text-muted-foreground text-center">Audit deposits, withdrawals, and balances</span>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Customer Support Quick Access Tools */}
      {role === "CUSTOMER_SUPPORT" && (
        <>
          <Card className="border-none shadow-sm mt-6">
            <CardHeader>
              <CardTitle className="text-lg font-semibold text-foreground">Members with Incomplete KYC</CardTitle>
              <p className="text-sm text-muted-foreground">Members awaiting KYC document uploads or resubmission</p>
            </CardHeader>
            <CardContent>
              {stats.membersWithIncompleteKyc > 0 ? (
                <div className="space-y-3">
                  <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
                    <p className="text-sm font-medium text-amber-900">
                      {stats.membersWithIncompleteKyc} member{stats.membersWithIncompleteKyc !== 1 ? 's' : ''} need KYC documents
                    </p>
                    <p className="text-xs text-amber-700 mt-1">
                      Click "Track KYC Uploads" to see details and rejection reasons
                    </p>
                  </div>
                  <Button 
                    variant="default" 
                    className="w-full"
                    onClick={() => navigate("/kyc-upload-tracking")}
                  >
                    View Details & Rejection Reasons
                  </Button>
                </div>
              ) : (
                <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm font-medium text-green-900">
                    ✓ All members have complete KYC documentation
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card className="border-none shadow-sm mt-6">
            <CardHeader>
              <CardTitle className="text-lg font-semibold text-foreground">KYC & Member Support Tools</CardTitle>
              <p className="text-sm text-muted-foreground">Quick access to KYC document management and member support</p>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-3">
                <Button 
                  variant="outline" 
                  className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                  onClick={() => navigate("/kyc-upload")}
                >
                  <FileText className="h-8 w-8 text-primary" />
                  <span className="font-semibold">Upload KYC Documents</span>
                  <span className="text-xs text-muted-foreground text-center">Attach KYC documents to members</span>
                </Button>
                <Button 
                  variant="outline" 
                  className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                  onClick={() => navigate("/members")}
                >
                  <Users className="h-8 w-8 text-primary" />
                  <span className="font-semibold">View Members</span>
                  <span className="text-xs text-muted-foreground text-center">Browse all members and their details</span>
                </Button>
                <Button 
                  variant="outline" 
                  className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                  onClick={() => navigate("/kyc-upload-tracking")}
                >
                  <Shield className="h-8 w-8 text-primary" />
                  <span className="font-semibold">Track KYC Uploads</span>
                  <span className="text-xs text-muted-foreground text-center">Monitor uploads and rejection reasons</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {/* Teller Quick Access Tools */}
      {role === "TELLER" && (
        <Card className="border-none shadow-sm mt-6">
          <CardHeader>
            <CardTitle className="text-lg font-semibold text-foreground">KYC Verification Tools</CardTitle>
            <p className="text-sm text-muted-foreground">Quick access to KYC document verification</p>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2">
              <Button 
                variant="outline" 
                className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                onClick={() => navigate("/kyc-approval")}
              >
                <FileText className="h-8 w-8 text-primary" />
                <span className="font-semibold">Verify KYC Documents</span>
                <span className="text-xs text-muted-foreground text-center">Review, approve, or reject KYC documents</span>
              </Button>
              <Button 
                variant="outline" 
                className="h-auto py-6 flex flex-col items-center gap-2 hover:bg-primary/5 hover:border-primary transition-colors" 
                onClick={() => navigate("/members")}
              >
                <Users className="h-8 w-8 text-primary" />
                <span className="font-semibold">View Members</span>
                <span className="text-xs text-muted-foreground text-center">Browse members and their KYC status</span>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Deposit Approval Panel */}
      <DepositApprovalPanel
        open={depositPanelOpen}
        onOpenChange={setDepositPanelOpen}
      />
    </div>
  );
};

export default Index;
