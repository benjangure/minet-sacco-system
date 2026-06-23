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
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    // For member routes, check if token is available in storage
    if (requiredRole === 'MEMBER') {
      let token = localStorage.getItem('token');
      
      if (!token) {
        const sessionStr = localStorage.getItem('session');
        if (sessionStr) {
          try {
            const parsedSession = JSON.parse(sessionStr);
            token = parsedSession.token;
          } catch (e) {
            console.error('Failed to parse session:', e);
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
            setRetryCount(0);
          } else {
            setMemberTokenValid(false);
          }
        } catch (e) {
          console.error('Invalid token:', e);
          setMemberTokenValid(false);
        }
      } else {
        // Token not found - if we haven't retried yet, retry in 100ms (for race condition on initial login)
        if (retryCount < 1) {
          console.debug('DEBUG: ProtectedRoute - Token not found, retrying in 100ms (retry count:', retryCount, ')');
          const timeout = setTimeout(() => {
            setRetryCount(prev => prev + 1);
          }, 100);
          return () => clearTimeout(timeout);
        } else {
          console.debug('DEBUG: ProtectedRoute - Token not found after retry, marking invalid');
          setMemberTokenValid(false);
        }
      }
    }
  }, [requiredRole, retryCount]);

  // Show loading spinner while checking auth
  if (loading || (requiredRole === 'MEMBER' && memberTokenValid === null)) {
    return <div className="flex min-h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  }

  // For admin routes (no requiredRole specified), use AuthContext
  if (!requiredRole) {
    if (!session) return <Navigate to="/login" replace />;
    return <>{children}</>;
  }

  // For member routes with requiredRole="MEMBER"
  if (requiredRole === 'MEMBER') {
    if (memberTokenValid !== true) {
      console.debug('DEBUG: ProtectedRoute - Member token invalid, redirecting to member login');
      return <Navigate to="/member/login" replace />;
    }
    return <>{children}</>;
  }

  // For other role-based routes (if needed in future)
  return <>{children}</>;
}

