import { ReactNode } from 'react';
import MemberSidebar from './MemberSidebar';

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
  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar */}
      <MemberSidebar
        memberName={memberName}
        onLogout={onLogout}
        unreadNotifications={unreadNotifications}
      />

      {/* Main Content */}
      <main className="flex-1 overflow-auto lg:ml-0 w-full">
        <div className="p-4 lg:p-8 pt-20 lg:pt-8 w-full max-w-full">
          {children}
        </div>
      </main>
    </div>
  );
}
