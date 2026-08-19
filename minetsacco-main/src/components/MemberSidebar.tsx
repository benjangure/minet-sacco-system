import { useState } from 'react';
import { Menu, X, Home, Send, User, FileText, Bell, LogOut, Handshake, Settings, ChevronLeft, ChevronRight, History } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useNavigate, useLocation } from 'react-router-dom';
import logo from '@/assets/images/logo.png';
import logoCollapsed from '/Minet-Logo1.png';

interface MemberSidebarProps {
  onLogout: () => void;
  memberName: string;
  unreadNotifications?: number;
  hideMobileToggle?: boolean;
  isOpen?: boolean;
  onClose?: () => void;
  isCollapsed?: boolean;
  onToggleCollapse?: () => void;
}

export default function MemberSidebar({ 
  onLogout, 
  memberName, 
  unreadNotifications = 0, 
  hideMobileToggle = false, 
  isOpen: controlledIsOpen, 
  onClose,
  isCollapsed = false,
  onToggleCollapse 
}: MemberSidebarProps) {
  const [internalIsOpen, setInternalIsOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const sidebarIsOpen = controlledIsOpen !== undefined ? controlledIsOpen : internalIsOpen;
  const setSidebarIsOpen = controlledIsOpen !== undefined && onClose ? onClose : setInternalIsOpen;

  const handleMenuClick = (id: string) => {
    setSidebarIsOpen(false);
    
    // Get target path based on menu item
    let targetPath = '/member/dashboard';
    switch(id) {
      case 'home':
        targetPath = '/member/dashboard';
        break;
      case 'transact':
        targetPath = '/member/dashboard?tab=transact';
        break;
      case 'transaction-history':
        targetPath = '/member/dashboard?tab=transaction-history';
        break;
      case 'account':
        targetPath = '/member/dashboard?tab=account';
        break;
      case 'loans':
        targetPath = '/member/dashboard?tab=loans';
        break;
      case 'guarantees':
        targetPath = '/member/my-guarantees';
        break;
      case 'reports':
        targetPath = '/member/dashboard?tab=reports';
        break;
      case 'notifications':
        targetPath = '/member/dashboard?tab=notifications';
        break;
      case 'settings':
        targetPath = '/member/settings';
        break;
    }
    
    // Check if we're already on the target path
    const currentFullPath = location.pathname + location.search;
    if (currentFullPath !== targetPath) {
      // Use push navigation for better history management
      navigate(targetPath);
    }
  };

  const menuItems = [
    { icon: Home, label: 'Home', id: 'home' },
    { icon: Send, label: 'Transactions', id: 'transact' },
    { icon: History, label: 'Transaction History', id: 'transaction-history' },
    { icon: User, label: 'My Account', id: 'account' },
    { icon: FileText, label: 'Loans', id: 'loans' },
    { icon: Handshake, label: 'My Guarantees', id: 'guarantees' },
    { icon: FileText, label: 'Reports', id: 'reports' },
    { icon: Bell, label: 'Notifications', id: 'notifications', badge: unreadNotifications },
    { icon: Settings, label: 'Settings', id: 'settings' },
  ];

  return (
    <>
      {!hideMobileToggle && (
        <div className="lg:hidden fixed top-4 left-4 z-50">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSidebarIsOpen(!sidebarIsOpen)}
            className="bg-primary text-white hover:bg-primary/90"
          >
            {sidebarIsOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </Button>
        </div>
      )}

      {sidebarIsOpen && (
        <div
          className="fixed inset-0 bg-black/50 lg:hidden z-40"
          onClick={() => setSidebarIsOpen(false)}
        />
      )}

      <aside
        className={`fixed left-0 top-0 h-screen bg-gradient-to-b from-primary to-primary/90 text-white transform transition-all duration-500 ease-in-out z-40 lg:relative lg:translate-x-0 lg:z-0 flex flex-col ${
          hideMobileToggle ? 'translate-x-0' : (sidebarIsOpen ? 'translate-x-0' : '-translate-x-full')
        } ${isCollapsed ? 'lg:w-16' : 'lg:w-64'} w-64`}
      >
        {/* Collapse/Expand Button for Desktop */}
        {!hideMobileToggle && (
          <button
            onClick={onToggleCollapse}
            className="hidden lg:flex absolute -right-3 top-6 bg-white text-primary rounded-full p-2 shadow-lg hover:shadow-xl hover:scale-110 transition-all duration-300 ease-in-out z-[100] items-center justify-center border-2 border-primary"
          >
            {isCollapsed ? (
              <ChevronRight className="h-4 w-4 transition-transform duration-300" />
            ) : (
              <ChevronLeft className="h-4 w-4 transition-transform duration-300" />
            )}
          </button>
        )}

        <div className="flex flex-col h-full overflow-hidden">
          {/* Logo/Header - Fixed height */}
          <div className={`flex items-center flex-shrink-0 transition-all duration-500 ease-in-out h-20 ${isCollapsed ? 'justify-center px-2' : 'gap-3 px-6'}`}>
            {!isCollapsed ? (
              <>
                <img src={logo} alt="Minet SACCO" className="h-10 w-auto transition-all duration-500" />
                <div className="transition-all duration-500 ease-in-out">
                  <h1 className="text-xl font-bold whitespace-nowrap">Minet SACCO</h1>
                  <p className="text-white/80 text-xs whitespace-nowrap">Member Portal</p>
                </div>
              </>
            ) : (
              <img src={logoCollapsed} alt="Minet SACCO" className="h-10 w-auto transition-all duration-500" />
            )}
          </div>

          {/* Member Info */}
          <div className={`transition-all duration-500 ease-in-out overflow-hidden ${
            isCollapsed ? 'max-h-0 opacity-0 mb-0' : 'max-h-24 opacity-100 mb-6'
          }`}>
            <div className="bg-white/10 rounded-lg p-3 mx-6">
              <p className="text-white/80 text-xs uppercase tracking-wide">Welcome</p>
              <p className="font-semibold truncate">{memberName}</p>
            </div>
          </div>

          {/* Navigation Menu - Scrollable if needed */}
          <nav className={`space-y-1 flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-white/20 scrollbar-track-transparent transition-all duration-300 ${isCollapsed ? 'px-2' : 'px-6'}`}>
            {menuItems.map((item) => (
              <button
                key={item.id}
                onClick={() => handleMenuClick(item.id)}
                className={`w-full flex items-center rounded-lg hover:bg-white/10 transition-all duration-300 ease-in-out hover:scale-105 relative group text-left ${
                  isCollapsed ? 'justify-center p-2.5' : 'gap-3 px-3 py-2.5'
                }`}
                title={isCollapsed ? item.label : undefined}
              >
                <item.icon className={`h-5 w-5 flex-shrink-0 transition-all duration-300 ${isCollapsed ? '' : 'group-hover:scale-110'}`} />
                <span className={`font-medium text-sm transition-all duration-500 ease-in-out whitespace-nowrap ${
                  isCollapsed ? 'w-0 opacity-0 overflow-hidden' : 'w-auto opacity-100'
                }`}>
                  {item.label}
                </span>
                {!isCollapsed && item.badge && item.badge > 0 && (
                  <span className="ml-auto bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center flex-shrink-0 font-bold transition-all duration-300 animate-pulse">
                    {item.badge > 9 ? '9+' : item.badge}
                  </span>
                )}
                {isCollapsed && item.badge && item.badge > 0 && (
                  <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-bold text-[10px] animate-pulse">
                    {item.badge > 9 ? '9+' : item.badge}
                  </span>
                )}
              </button>
            ))}
          </nav>

          {/* Logout Button */}
          <div className={`pt-4 border-t border-white/20 flex-shrink-0 transition-all duration-300 ${isCollapsed ? 'px-2' : 'px-6'}`}>
            <Button
              onClick={() => {
                onLogout();
                setSidebarIsOpen(false);
              }}
              variant="ghost"
              className={`w-full text-white hover:bg-white/10 py-2.5 transition-all duration-300 ease-in-out hover:scale-105 ${
                isCollapsed ? 'justify-center px-0' : 'justify-start gap-3'
              }`}
              title={isCollapsed ? 'Logout' : undefined}
            >
              <LogOut className={`h-5 w-5 transition-all duration-300 ${isCollapsed ? '' : 'group-hover:scale-110'}`} />
              <span className={`text-sm font-medium transition-all duration-500 ease-in-out whitespace-nowrap ${
                isCollapsed ? 'w-0 opacity-0 overflow-hidden' : 'w-auto opacity-100'
              }`}>
                Logout
              </span>
            </Button>
          </div>
        </div>
      </aside>
    </>
  );
}