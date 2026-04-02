import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { AppLayout } from "@/components/AppLayout";
import ProtectedRoute from "@/components/ProtectedRoute";
import Index from "./pages/Index";
import Login from "./pages/Login";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import Members from "./pages/Members";
import Loans from "./pages/Loans";
import Savings from "./pages/Savings";
import Reports from "./pages/Reports";
import UserManagement from "./pages/UserManagement";
import Settings from "./pages/Settings";
import Guide from "./pages/Guide";
import LoanProducts from "./pages/LoanProducts";
import FundConfiguration from "./pages/FundConfiguration";
import LoanEligibilityRules from "./pages/LoanEligibilityRules";
import BulkProcessing from "./pages/BulkProcessing";
import KycApproval from "./pages/KycApproval";
import KycDocumentUpload from "./pages/KycDocumentUpload";
import KycUploadTracking from "./pages/KycUploadTracking";
import ViewMemberDocuments from "./pages/ViewMemberDocuments";
import NotFound from "./pages/NotFound";
import MemberLogin from "./pages/MemberLogin";
import MemberDashboard from "./pages/MemberDashboard";
import MemberAccountStatement from "./pages/MemberAccountStatement";
import MemberLoanBalances from "./pages/MemberLoanBalances";
import MemberLoanApplication from "./pages/MemberLoanApplication";
import { AuditTrail } from "./pages/AuditTrail";
import LoanRepaymentRequests from "./pages/LoanRepaymentRequests";
import MemberLoanRepaymentStatus from "./pages/MemberLoanRepaymentStatus";


const queryClient = new QueryClient();

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { session, loading } = useAuth();
  if (loading) return <div className="flex min-h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  if (session) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

function RootRoute() {
  const { session, loading, user } = useAuth();
  if (loading) return <div className="flex min-h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  
  // If logged in, redirect based on role
  if (session) {
    if (user?.role === 'MEMBER') {
      return <Navigate to="/member/dashboard" replace />;
    }
    return <Navigate to="/dashboard" replace />;
  }
  
  // If not logged in, show staff login
  return <Login />;
}

const AppRoutes = () => (
  <Routes>
    {/* Admin/Staff Routes */}
    <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
    <Route path="/admin" element={<PublicRoute><Login /></PublicRoute>} />
    <Route path="/forgot-password" element={<PublicRoute><ForgotPassword /></PublicRoute>} />
    <Route path="/reset-password" element={<ResetPassword />} />
    <Route path="/" element={<RootRoute />} />
    <Route path="/dashboard" element={<ProtectedRoute><AppLayout><Index /></AppLayout></ProtectedRoute>} />
    <Route path="/members" element={<ProtectedRoute><AppLayout><Members /></AppLayout></ProtectedRoute>} />
    <Route path="/loans" element={<ProtectedRoute><AppLayout><Loans /></AppLayout></ProtectedRoute>} />
    <Route path="/loan-repayment-requests" element={<ProtectedRoute><AppLayout><LoanRepaymentRequests /></AppLayout></ProtectedRoute>} />
    <Route path="/savings" element={<ProtectedRoute><AppLayout><Savings /></AppLayout></ProtectedRoute>} />
    <Route path="/reports" element={<ProtectedRoute><AppLayout><Reports /></AppLayout></ProtectedRoute>} />
    <Route path="/bulk-processing" element={<ProtectedRoute><AppLayout><BulkProcessing /></AppLayout></ProtectedRoute>} />
    <Route path="/kyc-approval" element={<ProtectedRoute><AppLayout><KycApproval /></AppLayout></ProtectedRoute>} />
    <Route path="/kyc-upload" element={<ProtectedRoute><AppLayout><KycDocumentUpload /></AppLayout></ProtectedRoute>} />
    <Route path="/kyc-upload-tracking" element={<ProtectedRoute><AppLayout><KycUploadTracking /></AppLayout></ProtectedRoute>} />
    <Route path="/view-documents" element={<ProtectedRoute><AppLayout><ViewMemberDocuments /></AppLayout></ProtectedRoute>} />
    <Route path="/admin/users" element={<ProtectedRoute><AppLayout><UserManagement /></AppLayout></ProtectedRoute>} />
    <Route path="/admin/loan-products" element={<ProtectedRoute><AppLayout><LoanProducts /></AppLayout></ProtectedRoute>} />
    <Route path="/admin/fund-configuration" element={<ProtectedRoute><AppLayout><FundConfiguration /></AppLayout></ProtectedRoute>} />
    <Route path="/admin/loan-eligibility-rules" element={<ProtectedRoute><AppLayout><LoanEligibilityRules /></AppLayout></ProtectedRoute>} />
    <Route path="/admin/audit-trail" element={<ProtectedRoute><AppLayout><AuditTrail /></AppLayout></ProtectedRoute>} />
    <Route path="/settings" element={<ProtectedRoute><AppLayout><Settings /></AppLayout></ProtectedRoute>} />
    <Route path="/guide" element={<ProtectedRoute><AppLayout><Guide /></AppLayout></ProtectedRoute>} />
    
    {/* Member Routes */}
    <Route path="/member" element={<MemberLogin />} />
    <Route 
      path="/member/dashboard" 
      element={
        <ProtectedRoute requiredRole="MEMBER">
          <MemberDashboard />
        </ProtectedRoute>
      } 
    />
    <Route 
      path="/member/account-statement" 
      element={
        <ProtectedRoute requiredRole="MEMBER">
          <MemberAccountStatement />
        </ProtectedRoute>
      } 
    />
    <Route 
      path="/member/loan-balances" 
      element={
        <ProtectedRoute requiredRole="MEMBER">
          <MemberLoanBalances />
        </ProtectedRoute>
      } 
    />
    <Route 
      path="/member/apply-loan" 
      element={
        <ProtectedRoute requiredRole="MEMBER">
          <MemberLoanApplication />
        </ProtectedRoute>
      } 
    />
    <Route 
      path="/member/loan-repayment-status/:requestId" 
      element={
        <ProtectedRoute requiredRole="MEMBER">
          <MemberLoanRepaymentStatus />
        </ProtectedRoute>
      } 
    />
    
    {/* 404 */}
    <Route path="*" element={<NotFound />} />
  </Routes>
);

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
