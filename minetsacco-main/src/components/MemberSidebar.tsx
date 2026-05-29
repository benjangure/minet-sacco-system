import { useState } from 'react';
import { Menu, X, Home, Send, User, FileText, Bell, LogOut, Handshake, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import logo from '@/assets/images/logo.png';

interface MemberSidebarProps {
  onLogout: () => void;
  memberName: string;
  unreadNotifications?: number;
  hideMobileToggle?: boolean;
  isOpen?: boolean;
  onClose?: () => void;
}

export default function MemberSidebar({ onLogout, memberName, unreadNotifications = 0, hideMobileToggle = false, isOpen: controlledIsOpen, onClose }: MemberSidebarProps) {
  const [internalIsOpen, setInternalIsOpen] = useState(false);
  const navigate = useNavigate();

  // Use controlled state if provided, otherwise use internal state
  const sidebarIsOpen = controlledIsOpen !== undefined ? controlledIsOpen : internalIsOpen;
  const setSidebarIsOpen = controlledIsOpen !== undefined && onClose ? onClose : setInternalIsOpen;

  const handleMenuClick = (id: string) => {
    setSidebarIsOpen(false);
    
    // Navigate based on menu item
    switch(id) {
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
      case 'guarantees':
        navigate('/member/my-guarantees');
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
      default:
        navigate('/member/dashboard');
    }
  };

  const menuItems = [
    { icon: Home, label: 'Home', id: 'home' },
    { icon: Send, label: 'Transactions', id: 'transact' },
    { icon: User, label: 'My Account', id: 'account' },
    { icon: FileText, label: 'Loans', id: 'loans' },
    { icon: Handshake, label: 'My Guarantees', id: 'guarantees' },
    { icon: FileText, label: 'Reports', id: 'reports' },
    { icon: Bell, label: 'Notifications', id: 'notifications', badge: unreadNotifications },
    { icon: Settings, label: 'Settings', id: 'settings' },
  ];

  return (
    <>
      {/* Mobile Toggle Button - only show if not hidden */}
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

      {/* Overlay for mobile */}
      {sidebarIsOpen && (
        <div
          className="fixed inset-0 bg-black/50 lg:hidden z-40"
          onClick={() => setSidebarIsOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-screen w-64 bg-gradient-to-b from-primary to-primary/90 text-white transform transition-transform duration-300 z-40 lg:relative lg:translate-x-0 lg:z-0 ${
          hideMobileToggle ? 'translate-x-0' : (sidebarIsOpen ? 'translate-x-0' : '-translate-x-full')
        }`}
      >
        <div className="p-6 space-y-8">
          {/* Logo/Header */}
          <div className="space-y-2 flex items-center gap-3">
            <img src={logo} alt="Minet SACCO" className="h-10 w-auto" />
            <div>
              <h1 className="text-2xl font-bold">Minet SACCO</h1>
              <p className="text-white/80 text-xs">Member Portal</p>
            </div>
          </div>

          {/* Member Info */}
          <div className="bg-white/10 rounded-lg p-4 space-y-2">
            <p className="text-white/80 text-xs uppercase tracking-wide">Welcome</p>
            <p className="font-semibold text-lg">{memberName}</p>
          </div>

          {/* Navigation Menu */}
          <nav className="space-y-2">
            {menuItems.map((item) => (
              <button
                key={item.id}
                onClick={() => handleMenuClick(item.id)}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-white/10 transition-colors relative group"
              >
                <item.icon className="h-5 w-5" />
                <span className="font-medium">{item.label}</span>
                {item.badge && item.badge > 0 && (
                  <span className="ml-auto bg-red-500 text-white text-xs rounded-full w-6 h-6 flex items-center justify-center">
                    {item.badge}
                  </span>
                )}
              </button>
            ))}
          </nav>

          {/* Logout Button */}
          <div className="pt-4 border-t border-white/20 space-y-2">
            <Button
              onClick={() => {
                onLogout();
                setSidebarIsOpen(false);
              }}
              variant="ghost"
              className="w-full justify-start gap-3 text-white hover:bg-white/10"
            >
              <LogOut className="h-5 w-5" />
              Logout
            </Button>
          </div>
        </div>
      </aside>
    </>
  );
}
