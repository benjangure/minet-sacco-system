import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { getBackendUrl } from "@/config/api";

type AppRole = "ADMIN" | "TREASURER" | "LOAN_OFFICER" | "CREDIT_COMMITTEE" | "AUDITOR" | "TELLER" | "CUSTOMER_SUPPORT" | "MEMBER";

interface User {
  id: number;
  username: string;
  email: string;
  role: AppRole;
}

interface Session {
  role: string;
  token: string;
  user: User;
}

interface AuthContextType {
  session: Session | null;
  user: User | null;
  role: AppRole | null;
  profile: { full_name: string; email: string; avatar_url: string | null } | null;
  loading: boolean;
  signIn: (username: string, password: string) => Promise<{ error: Error | null }>;
  memberSignIn: (username: string, password: string) => Promise<{ error: Error | null; firstLogin?: boolean; username?: string; password?: string }>;
  signUp: (email: string, password: string, fullName: string) => Promise<{ error: Error | null }>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const APP_VERSION = "1.0.0"; // Increment this to force logout on all users

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [role, setRole] = useState<AppRole | null>(null);
  const [profile, setProfile] = useState<AuthContextType["profile"]>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  
  // Get dynamic API base URL from config
  const getApiBaseUrl = () => `${getBackendUrl()}/api`;

  useEffect(() => {
    // Check if app version has changed (forces logout on updates)
    const storedVersion = localStorage.getItem("appVersion");
    if (storedVersion !== APP_VERSION) {
      // Version mismatch, clear all sessions
      localStorage.removeItem("session");
      localStorage.removeItem("token");
      localStorage.setItem("appVersion", APP_VERSION);
      setLoading(false);
      return;
    }
    
    // Check for existing session in localStorage
    const storedSession = localStorage.getItem("session");
    if (storedSession) {
      try {
        const parsedSession = JSON.parse(storedSession);
        
        // Validate the token exists and is not empty
        if (!parsedSession.token || typeof parsedSession.token !== 'string' || parsedSession.token.trim() === '') {
          localStorage.removeItem("session");
          setLoading(false);
          return;
        }
        
        // Try to validate the token by checking its structure
        const tokenParts = parsedSession.token.split('.');
        if (tokenParts.length !== 3) {
          // Invalid JWT format
          localStorage.removeItem("session");
          setLoading(false);
          return;
        }
        
        // Try to decode and validate expiration if available
        try {
          const payload = JSON.parse(atob(tokenParts[1]));
          
          // If token has exp claim, check if it's expired
          if (payload.exp) {
            const expirationTime = payload.exp * 1000; // Convert to milliseconds
            const currentTime = Date.now();
            
            if (currentTime > expirationTime) {
              // Token is expired
              localStorage.removeItem("session");
              setLoading(false);
              return;
            }
          }
        } catch (e) {
          // If we can't parse the token payload, it's invalid
          localStorage.removeItem("session");
          setLoading(false);
          return;
        }
        
        // Token appears valid, restore session
        setSession(parsedSession);
        setUser(parsedSession.user);
        setRole(parsedSession.user.role);
        setProfile({
          full_name: parsedSession.user.username,
          email: parsedSession.user.email,
          avatar_url: null,
        });
      } catch (error) {
        localStorage.removeItem("session");
      }
    }
    setLoading(false);
  }, []);

  const memberSignIn = async (username: string, password: string) => {
    try {
      console.log('DEBUG: memberSignIn - fetching from', `${getApiBaseUrl()}/auth/member/login`);
      
      const response = await fetch(`${getApiBaseUrl()}/auth/member/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password }),
      });

      console.log('DEBUG: memberSignIn - response status:', response.status);

      if (!response.ok) {
        const errorData = await response.json();
        console.error('DEBUG: memberSignIn - error response:', errorData);
        throw new Error(errorData.message || "Login failed");
      }

      const data = await response.json();
      console.log('DEBUG: memberSignIn - success response received, firstLogin:', data.firstLogin);
      
      // Check if this is a first login
      if (data.firstLogin) {
        console.log('DEBUG: memberSignIn - first login detected');
        return { 
          error: null, 
          firstLogin: true, 
          username: username,
          password: password 
        };
      }
      
      // Decode JWT to get user info (basic decode, not verification)
      const tokenParts = data.token.split('.');
      const payload = JSON.parse(atob(tokenParts[1]));
      
      const sessionData: Session = {
        token: data.token,
        user: {
          id: data.memberId || 0,
          username: payload.sub,
          email: username,
          role: (payload.role || "MEMBER") as AppRole,
        },
        role: ""
      };

      console.log('DEBUG: memberSignIn - setting session for user:', payload.sub);

      setSession(sessionData);
      setUser(sessionData.user);
      setRole(sessionData.user.role);
      setProfile({
        full_name: sessionData.user.username,
        email: sessionData.user.email,
        avatar_url: null,
      });

      // Store under "session" (used by AuthContext's own restore logic)
      // AND under "token" (used by MemberSettings.tsx, notificationService.ts,
      // and other member-portal pages that read the raw token directly)
      localStorage.setItem("session", JSON.stringify(sessionData));
      localStorage.setItem("token", data.token);

      console.log('DEBUG: memberSignIn - session saved to localStorage');

      return { error: null, firstLogin: false };
    } catch (error) {
      console.error('DEBUG: memberSignIn - catch block error:', error);
      return { error: error as Error };
    }
  };

  const signIn = async (username: string, password: string) => {
    try {
      const response = await fetch(`${getApiBaseUrl()}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Login failed");
      }

      const data = await response.json();
      
      // Decode JWT to get user info (basic decode, not verification)
      const tokenParts = data.token.split('.');
      const payload = JSON.parse(atob(tokenParts[1]));
      
      const sessionData: Session = {
        token: data.token,
        user: {
          id: 0,
          username: payload.sub,
          email: username,
          role: (payload.role || "ADMIN") as AppRole,
        },
        role: ""
      };

      setSession(sessionData);
      setUser(sessionData.user);
      setRole(sessionData.user.role);
      setProfile({
        full_name: sessionData.user.username,
        email: sessionData.user.email,
        avatar_url: null,
      });

      localStorage.setItem("session", JSON.stringify(sessionData));

      return { error: null };
    } catch (error) {
      return { error: error as Error };
    }
  };

  const signUp = async (email: string, password: string, fullName: string) => {
    // Not implemented for backend yet
    return { error: new Error("Sign up not implemented") };
  };

  const signOut = async () => {
    setSession(null);
    setUser(null);
    setRole(null);
    setProfile(null);
    localStorage.removeItem("session");
    localStorage.removeItem("token");
    // Clear browser history and navigate to login
    navigate("/login", { replace: true });
    // Clear the history stack to prevent back button from returning to previous pages
    window.history.replaceState(null, "", "/login");
  };

  return (
    <AuthContext.Provider value={{ session, user, role, profile, loading, signIn, memberSignIn, signUp, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}