import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/AppSidebar";
import { NotificationBell } from "@/components/NotificationBell";
import { useAuth } from "@/contexts/AuthContext";
import { useRefresh } from "@/contexts/RefreshContext";
import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { useState } from "react";

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { profile, role } = useAuth();
  const { triggerRefresh } = useRefresh();
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = () => {
    setIsRefreshing(true);
    
    // Trigger context-based refresh - all pages listening will refetch data
    triggerRefresh();
    
    // Visual feedback for 800ms
    setTimeout(() => {
      setIsRefreshing(false);
    }, 800);
  };

  return (
    <SidebarProvider>
      <AppSidebar />
      <main className="flex flex-col w-full min-h-screen transition-all duration-300">
        <header className="h-14 flex items-center border-b bg-background px-3 sm:px-4 gap-2 sm:gap-4 sticky top-0 z-10 transition-all duration-300 overflow-visible">
          <SidebarTrigger className="transition-transform duration-200 hover:scale-110" />
          <h2 className="text-xs sm:text-sm font-medium text-muted-foreground flex-1 truncate">
            Minet SACCO Management System
          </h2>
          <Button
            variant="ghost"
            size="icon"
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="h-9 w-9 transition-transform duration-200 hover:scale-110"
            title="Refresh page"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </Button>
          <NotificationBell />
        </header>
        <div className="flex-1 p-3 sm:p-4 md:p-6 bg-muted/30 transition-all duration-300">{children}</div>
      </main>
    </SidebarProvider>
  );
}
