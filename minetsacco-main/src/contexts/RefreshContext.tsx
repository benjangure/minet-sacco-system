import { createContext, useContext, useState, useEffect, useCallback, useRef, ReactNode } from "react";

interface RefreshContextType {
  refreshKey: number;
  triggerRefresh: () => void;
}

const RefreshContext = createContext<RefreshContextType | undefined>(undefined);

/** How often screens re-read their data while someone is looking at them. */
const AUTO_REFRESH_MS = 30_000;

/**
 * True when refreshing would disturb the user: a dialog is open, or they are typing in a field.
 * Screens that react to refreshKey would otherwise reload underneath a half-filled form.
 */
function userIsBusy(): boolean {
  if (typeof document === "undefined") return false;
  if (document.querySelector('[role="dialog"], [role="alertdialog"]')) return true;
  const el = document.activeElement as HTMLElement | null;
  if (!el) return false;
  const tag = el.tagName;
  return tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || el.isContentEditable;
}

export function RefreshProvider({ children }: { children: ReactNode }) {
  const [refreshKey, setRefreshKey] = useState(0);
  const lastRefresh = useRef(Date.now());

  const triggerRefresh = useCallback(() => {
    lastRefresh.current = Date.now();
    setRefreshKey((prev) => prev + 1);
  }, []);

  useEffect(() => {
    // Periodic refresh while the tab is visible
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible" && !userIsBusy()) {
        triggerRefresh();
      }
    }, AUTO_REFRESH_MS);

    // Coming back to the tab / app or regaining connection: refresh if the data is more than 10s old
    const onWake = () => {
      if (document.visibilityState === "visible" && Date.now() - lastRefresh.current > 10_000 && !userIsBusy()) {
        triggerRefresh();
      }
    };
    document.addEventListener("visibilitychange", onWake);
    window.addEventListener("focus", onWake);
    window.addEventListener("online", onWake);

    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onWake);
      window.removeEventListener("focus", onWake);
      window.removeEventListener("online", onWake);
    };
  }, [triggerRefresh]);

  return (
    <RefreshContext.Provider value={{ refreshKey, triggerRefresh }}>
      {children}
    </RefreshContext.Provider>
  );
}

export function useRefresh() {
  const context = useContext(RefreshContext);
  if (!context) throw new Error("useRefresh must be used within RefreshProvider");
  return context;
}
