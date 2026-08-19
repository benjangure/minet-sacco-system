import { Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import { useAuth } from '@/contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
}

export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { session, loading } = useAuth();
  const [memberTokenValid, setMemberTokenValid] = useState<boolean | null>(null);
  const [hasChecked, setHasChecked] = useState(false);

  useEffect(() => {
    // Prevent infinite loop - only check once
    if (hasChecked) return;
    
    // For member routes, check if token is available in storage
    if (requiredRole === 'MEMBER') {
      let token = localStorage.getItem('token');
      
      if (!token) {
        const sessionStr = localStorage.getItem('member_session');
        if (sessionStr) {
          try {
            const parsedSession = JSON.parse(sessionStr);
            token = parsedSession.token;
          } catch (e) {
            console.error('Failed to parse member session:', e);
          }
        }
      }

      if (token) {
        try {
          const decoded: any = jwtDecode(token);
          const tokenRole = decoded.role || '';
          const normalizedTokenRole = tokenRole.replace('ROLE_', '');
          
          if (normalizedTokenRole === 'MEMBER') {
            setMemberTokenValid(true);
          } else {
            setMemberTokenValid(false);
          }
        } catch (e) {
          console.error('Invalid token:', e);
          setMemberTokenValid(false);
        }
      } else {
        // No token found
        setMemberTokenValid(false);
      }
      
      setHasChecked(true);
    }
  }, [requiredRole, hasChecked]);

  // Show loading spinner while checking auth
  if (loading || (requiredRole === 'MEMBER' && memberTokenValid === null)) {
    return <div className="flex min-h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  }

  // For admin routes (no requiredRole specified), use AuthContext
  if (!requiredRole) {
    if (!session && !loading) return <Navigate to="/login" replace />;
    return <>{children}</>;
  }

  // For member routes with requiredRole="MEMBER"
  if (requiredRole === 'MEMBER') {
    if (memberTokenValid !== true && !loading) {
      console.debug('DEBUG: ProtectedRoute - Member token invalid, redirecting to member login');
      return <Navigate to="/member/login" replace />;
    }
    return <>{children}</>;
  }

  // For other role-based routes (if needed in future)
  return <>{children}</>;
}

