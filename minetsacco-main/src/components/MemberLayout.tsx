import { ReactNode, useState, useEffect } from 'react';
import MemberSidebar from './MemberSidebar';
import NotificationDropdown from './NotificationDropdown';
import { Menu, LogOut } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { LogoutConfirmationDialog } from './LogoutConfirmationDialog';
import { Button } from '@/components/ui/button';
import { RefreshCw } from 'lucide-react';
import { MemberPageSkeleton } from './MemberPageSkeleton';
import { useRefresh } from '@/contexts/RefreshContext';

interface MemberLayoutProps {
  children: ReactNode;
  memberName: string;
  onLogout: () => void;
  unreadNotifications?: number;
}

export default function MemberLayout({
  children,
  memberName,
  onLogout,
  unreadNotifications = 0,
}: MemberLayoutProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isNavigating, setIsNavigating] = useState(false);
  const { triggerRefresh } = useRefresh();
  const navigate = useNavigate();
  const location = useLocation();

  // Show skeleton briefly when route changes
  useEffect(() => {
    setIsNavigating(true);
    const timer = setTimeout(() => {
      setIsNavigating(false);
    }, 300); // Show skeleton for 300ms during navigation

    return () => clearTimeout(timer);
  }, [location.pathname]);

  const handleLogoutClick = () => {
    setShowLogoutDialog(true);
  };

  const handleConfirmLogout = () => {
    setShowLogoutDialog(false);
    onLogout();
  };

  const handleCancelLogout = () => {
    setShowLogoutDialog(false);
  };

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
    <div className="flex h-screen bg-background overflow-hidden w-full">
      {/* Sidebar - Fixed on desktop, hidden on mobile */}
      <div className={`hidden lg:block flex-shrink-0 transition-all duration-500 ease-in-out ${isSidebarCollapsed ? 'w-16' : 'w-64'}`}>
        <MemberSidebar
          memberName={memberName}
          onLogout={onLogout}
          unreadNotifications={unreadNotifications}
          isCollapsed={isSidebarCollapsed}
          onToggleCollapse={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
        />
      </div>

      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <>
          <div 
            className="fixed inset-0 bg-black/50 z-40 lg:hidden" 
            onClick={() => setIsSidebarOpen(false)} 
          />
          <div className="fixed left-0 top-0 h-full w-64 z-50 lg:hidden">
            <MemberSidebar
              memberName={memberName}
              onLogout={onLogout}
              unreadNotifications={unreadNotifications}
              hideMobileToggle={true}
              isOpen={true}
              onClose={() => setIsSidebarOpen(false)}
            />
          </div>
        </>
      )}

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col h-screen overflow-hidden min-w-0">
        {/* Desktop Header */}
        <header className="hidden lg:flex h-14 items-center border-b bg-background px-4 lg:px-6 gap-4 flex-shrink-0">
          <h2 className="text-sm font-medium text-muted-foreground flex-1">
            Minet SACCO Member Portal
          </h2>
          <Button
            variant="ghost"
            size="icon"
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="text-muted-foreground hover:text-foreground transition-transform duration-200 hover:scale-110"
            title="Refresh"
          >
            <RefreshCw className={`h-5 w-5 ${isRefreshing ? 'animate-spin' : ''}`} />
          </Button>
          <NotificationDropdown unreadCount={unreadNotifications} />
        </header>

        {/* Mobile Header - Simplified */}
        <header className="lg:hidden flex-shrink-0 bg-gradient-to-r from-red-600 to-red-700">
          {/* Single Top bar */}
          <div className="h-14 flex items-center px-4 gap-3">
            <button onClick={() => setIsSidebarOpen(true)} className="text-white">
              <Menu className="w-6 h-6" />
            </button>
            <h1 className="text-white font-semibold text-base sm:text-lg flex-1">
              Minet SACCO
            </h1>
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="icon"
                onClick={handleRefresh}
                disabled={isRefreshing}
                className="text-white hover:bg-white/10 transition-transform duration-200 hover:scale-110"
                title="Refresh"
              >
                <RefreshCw className={`h-5 w-5 ${isRefreshing ? 'animate-spin' : ''}`} />
              </Button>
              <NotificationDropdown unreadCount={unreadNotifications} variant="light" />
              <button onClick={handleLogoutClick} className="text-white hover:text-red-100 transition-colors">
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </header>

        {/* Content Area - Scrollable */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden min-w-0">
          <div className="px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 w-full max-w-full">
            {isNavigating ? <MemberPageSkeleton /> : children}
          </div>
        </div>
      </main>

      {/* Logout Confirmation Dialog */}
      <LogoutConfirmationDialog
        isOpen={showLogoutDialog}
        onConfirm={handleConfirmLogout}
        onCancel={handleCancelLogout}
      />
    </div>
  );
}
