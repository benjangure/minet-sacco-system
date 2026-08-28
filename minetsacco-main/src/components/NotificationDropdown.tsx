import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Check, ExternalLink } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { notificationService, Notification } from '@/services/notificationService';
import { useToast } from '@/hooks/use-toast';

interface NotificationDropdownProps {
  unreadCount?: number;
  variant?: 'default' | 'light'; // 'light' for mobile header with dark background
}

export default function NotificationDropdown({ unreadCount = 0, variant = 'default' }: NotificationDropdownProps) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  const isLight = variant === 'light';

  useEffect(() => {
    if (open) {
      fetchNotifications();
    }
  }, [open]);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const data = await notificationService.getNotifications();
      // Show only the latest 5 unread notifications
      const unread = (data || []).filter(n => !n.read).slice(0, 5);
      setNotifications(unread);
    } catch (error) {
      console.error('Error fetching notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(notifications.filter(n => n.id !== notificationId));
      toast({
        title: 'Marked as read',
        duration: 2000,
      });
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    // Mark as read
    if (!notification.read) {
      try {
        await notificationService.markAsRead(notification.id);
      } catch (error) {
        console.error('Error marking as read:', error);
      }
    }

    // Close dropdown
    setOpen(false);

    // Navigate based on type
    const type = notification.type || notification.category || '';
    const message = notification.message?.toLowerCase() || '';

    if (type.includes('LOAN') || message.includes('loan')) {
      navigate('/member/dashboard?tab=loans');
    } else if (type.includes('DEPOSIT') || message.includes('deposit')) {
      navigate('/member/dashboard?tab=deposits');
    } else if (type.includes('GUARANTOR') || message.includes('guarantee')) {
      navigate('/member/guarantor-approvals');
    } else {
      navigate('/member/dashboard?tab=notifications');
    }
  };

  const handleViewAll = () => {
    setOpen(false);
    navigate('/member/dashboard?tab=notifications');
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return date.toLocaleDateString();
  };

  const truncateMessage = (message: string, maxLength: number = 80) => {
    if (message.length <= maxLength) return message;
    return message.substring(0, maxLength) + '...';
  };

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className={`relative ${isLight ? 'text-white hover:text-white hover:bg-white/10' : ''}`}
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className={`absolute -top-1 -right-1 text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold ${
              isLight 
                ? 'bg-white text-red-600' 
                : 'bg-red-500 text-white'
            }`}>
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80 max-h-[500px] overflow-y-auto">
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Notifications</span>
          {unreadCount > 0 && (
            <span className="text-xs font-normal text-muted-foreground">
              {unreadCount} unread
            </span>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        {loading ? (
          <div className="p-4 text-center text-sm text-muted-foreground">
            Loading...
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-4 text-center text-sm text-muted-foreground">
            <Bell className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p>No new notifications</p>
          </div>
        ) : (
          <>
            {notifications.map((notification) => (
              <DropdownMenuItem
                key={notification.id}
                className="p-3 cursor-pointer hover:bg-accent focus:bg-accent"
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="flex gap-2 w-full">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <span className="text-xs font-semibold text-purple-600 uppercase">
                        {notification.type || 'Notification'}
                      </span>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">
                        {formatTime(notification.createdAt)}
                      </span>
                    </div>
                    <p
                      className="text-sm text-foreground line-clamp-2"
                      dangerouslySetInnerHTML={{ __html: truncateMessage(notification.message) }}
                    />
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0 shrink-0"
                    onClick={(e) => handleMarkAsRead(notification.id, e)}
                    title="Mark as read"
                  >
                    <Check className="h-4 w-4" />
                  </Button>
                </div>
              </DropdownMenuItem>
            ))}
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="text-center justify-center font-medium text-primary cursor-pointer"
              onClick={handleViewAll}
            >
              <ExternalLink className="h-4 w-4 mr-2" />
              View All Notifications
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
