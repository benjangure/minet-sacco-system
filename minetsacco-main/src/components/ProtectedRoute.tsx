import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
}

/**
 * Checks whether a JWT token string belongs to a MEMBER role.
 * Handles both "MEMBER" and "ROLE_MEMBER" claim formats.
 */
function isMemberToken(token: string): boolean {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return false;
    const payload = JSON.parse(atob(parts[1]));
    // Check expiry
    if (payload.exp && Date.now() > payload.exp * 1000) return false;
    const role: string = (payload.role || '').replace('ROLE_', '');
    return role === 'MEMBER';
  } catch {
    return false;
  }
}

/**
 * Resolve a member token from localStorage — checks 'token' key first,
 * then parses 'member_session', matching the same priority order used
 * by memberSignIn when it stores the session.
 */
function resolveMemberToken(): string | null {
  const raw = localStorage.getItem('token');
  if (raw) return raw;

  try {
    const sessionStr = localStorage.getItem('member_session');
    if (sessionStr) {
      const parsed = JSON.parse(sessionStr);
      if (parsed?.token) return parsed.token;
    }
  } catch {}

  return null;
}

export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { session, loading } = useAuth();

  // Show spinner only while AuthContext is still restoring the session on first load
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  // Member portal routes — validate token directly from localStorage.
  // This works immediately after login because memberSignIn writes to
  // localStorage synchronously before navigate() is called.
  if (requiredRole === 'MEMBER') {
    const token = resolveMemberToken();
    if (!token || !isMemberToken(token)) {
      console.debug('[ProtectedRoute] No valid member token — redirecting to member login');
      return <Navigate to="/member/login" replace />;
    }
    return <>{children}</>;
  }

  // Staff / admin routes — rely on AuthContext session
  if (!session) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
