import { Navigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { useAuth } from '@/contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
}

export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { session, loading } = useAuth();

  // For admin routes (no requiredRole specified), use AuthContext
  if (!requiredRole) {
    if (loading) return <div className="flex min-h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
    if (!session) return <Navigate to="/login" replace />;
    return <>{children}</>;
  }

  // For member routes, check JWT token
  const token = localStorage.getItem('token');

  if (!token) {
    const loginPath = requiredRole === 'MEMBER' ? '/member' : '/login';
    return <Navigate to={loginPath} />;
  }

  try {
    const decoded: any = jwtDecode(token);
    
    // If a specific role is required, check it
    if (requiredRole) {
      // Handle both formats: "MEMBER" and "ROLE_MEMBER"
      const tokenRole = decoded.role || '';
      const normalizedTokenRole = tokenRole.replace('ROLE_', '');
      const normalizedRequiredRole = requiredRole.replace('ROLE_', '');
      
      if (normalizedTokenRole !== normalizedRequiredRole) {
        // Show error message based on role mismatch
        const errorMsg = requiredRole === 'MEMBER' 
          ? 'This is the member portal. Staff accounts cannot access this area. Please use the staff login.'
          : 'This is the staff portal. Member accounts cannot access this area. Please use the member login.';
        
        // Store error message and redirect
        localStorage.setItem('accessError', errorMsg);
        const loginPath = requiredRole === 'MEMBER' ? '/member' : '/login';
        return <Navigate to={loginPath} />;
      }
    }

    return <>{children}</>;
  } catch (error) {
    console.error('Invalid token:', error);
    localStorage.removeItem('token');
    localStorage.removeItem('userRole');
    localStorage.removeItem('memberId');
    const loginPath = requiredRole === 'MEMBER' ? '/member' : '/login';
    return <Navigate to={loginPath} />;
  }
}

