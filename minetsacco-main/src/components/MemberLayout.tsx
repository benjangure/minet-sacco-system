import { ReactNode, useState, useRef } from 'react';
import MemberSidebar from './MemberSidebar';
import { Menu, Bell, Home, Send, User, FileText, Handshake, ChevronLeft, ChevronRight, Settings, LogOut } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { LogoutConfirmationDialog } from './LogoutConfirmationDialog';

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
  const [activeTab, setActiveTab] = useState('home');
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const tabsRef = useRef<HTMLDivElement>(null);

  const tabs = [
    { icon: Home, label: 'Home', id: 'home' },
    { icon: Send, label: 'Transact', id: 'transact' },
    { icon: User, label: 'My Account', id: 'account' },
    { icon: FileText, label: 'Loans', id: 'loans' },
    { icon: FileText, label: 'Deposits', id: 'deposits' },
    { icon: FileText, label: 'Reports', id: 'reports' },
    { icon: Bell, label: 'Notifications', id: 'notifications' },
    { icon: Settings, label: 'Settings', id: 'settings' },
  ];

  const handleTabClick = (tabId: string) => {
    setActiveTab(tabId);
    switch(tabId) {
      case 'home':
        navigate('/member/dashboard');
        break;
      case 'transact':
        navigate('/member/dashboard?tab=transact');
        break;
      case 'account':
        navigate('/member/dashboard?tab=account');
        break;
      case 'loans':
        navigate('/member/dashboard?tab=loans');
        break;
      case 'deposits':
        navigate('/member/dashboard?tab=deposits');
        break;
      case 'reports':
        navigate('/member/dashboard?tab=reports');
        break;
      case 'notifications':
        navigate('/member/dashboard?tab=notifications');
        break;
      case 'settings':
        navigate('/member/settings');
        break;
    }
  };

  const handleNotificationClick = () => {
    setActiveTab('notifications');
    navigate('/member/dashboard?tab=notifications');
  };

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

  const scrollTabs = (direction: 'left' | 'right') => {
    if (tabsRef.current) {
      const scrollAmount = 150;
      if (direction === 'left') {
        tabsRef.current.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
      } else {
        tabsRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
      }
    }
  };

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar - Hidden on mobile, visible on desktop */}
      <div className="hidden lg:block">
        <MemberSidebar
          memberName={memberName}
          onLogout={onLogout}
          unreadNotifications={unreadNotifications}
        />
      </div>

      {/* Mobile Sidebar Drawer - Only visible on mobile when open */}
      {isSidebarOpen && (
        <>
          <div className="fixed inset-0 bg-black/50 z-50 lg:hidden" onClick={() => setIsSidebarOpen(false)} />
          <div className="fixed left-0 top-0 h-screen w-64 z-50 lg:hidden">
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

      {/* Main Content */}
      <main className="flex-1 overflow-auto w-full flex flex-col">
        {/* Mobile Top Navigation Bar - Only visible on mobile */}
        <header className="lg:hidden fixed top-0 left-0 right-0 z-40 bg-gradient-to-r from-red-600 to-red-700">
          {/* Top bar with hamburger, title, notification, and logout */}
          <div className="h-14 flex items-center px-4 gap-4">
            <button onClick={() => setIsSidebarOpen(true)} className="text-white">
              <Menu className="w-6 h-6" />
            </button>
            <h1 className="text-white font-semibold text-lg flex-1 text-center">Minet SACCO</h1>
            <button onClick={handleNotificationClick} className="relative">
              <Bell className="w-5 h-5 text-white" />
              {unreadNotifications > 0 && (
                <span className="absolute -top-1 -right-1 bg-white text-red-600 text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                  {unreadNotifications}
                </span>
              )}
            </button>
            <button onClick={handleLogoutClick} className="text-white hover:text-red-100 transition-colors">
              <LogOut className="w-5 h-5" />
            </button>
          </div>

          {/* Horizontal scrollable tabs with navigation buttons */}
          <div className="relative border-t border-red-500/30">
            <button
              onClick={() => scrollTabs('left')}
              className="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-red-700/80 text-white p-1 rounded-r shadow-md"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <div
              ref={tabsRef}
              className="flex overflow-x-auto scrollbar-hide px-8"
            >
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => handleTabClick(tab.id)}
                  className={`flex items-center gap-2 px-4 py-3 whitespace-nowrap transition-colors flex-shrink-0 ${
                    activeTab === tab.id
                      ? 'text-white border-b-2 border-white'
                      : 'text-white/70 hover:text-white'
                  }`}
                >
                  <tab.icon className="w-4 h-4" />
                  <span className="text-sm font-medium">{tab.label}</span>
                </button>
              ))}
            </div>
            <button
              onClick={() => scrollTabs('right')}
              className="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-red-700/80 text-white p-1 rounded-l shadow-md"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </header>

        {/* Content - Add top padding on mobile for fixed header */}
        <div className="px-4 lg:px-8 pt-28 lg:pt-8 w-full max-w-full flex-1">
          {children}
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
