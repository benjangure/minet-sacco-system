import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Check, CheckCheck, Trash2, Bell, HandshakeIcon, ExternalLink } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { notificationService, Notification } from '@/services/notificationService';
import GuarantorApprovalDialog from './GuarantorApprovalDialog';

export default function MemberNotificationsView() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [guarantorDialogOpen, setGuarantorDialogOpen] = useState(false);
  const { toast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const data = await notificationService.getNotifications();
      setNotifications(data || []);
    } catch (error) {
      console.error('Error fetching notifications:', error);
      toast({
        title: 'Error',
        description: 'Failed to load notifications',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(
        notifications.map((n) =>
          n.id === notificationId ? { ...n, read: true } : n
        )
      );
      toast({
        title: 'Success',
        description: 'Notification marked as read',
      });
    } catch (error) {
      console.error('Error marking notification as read:', error);
      toast({
        title: 'Error',
        description: 'Failed to mark notification as read',
        variant: 'destructive',
      });
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(notifications.map((n) => ({ ...n, read: true })));
      toast({
        title: 'Success',
        description: 'All notifications marked as read',
      });
    } catch (error) {
      console.error('Error marking all as read:', error);
      toast({
        title: 'Error',
        description: 'Failed to mark all as read',
        variant: 'destructive',
      });
    }
  };

  const handleDelete = async (notificationId: number) => {
    try {
      await notificationService.deleteNotification(notificationId);
      setNotifications(notifications.filter((n) => n.id !== notificationId));
      toast({
        title: 'Success',
        description: 'Notification deleted',
      });
    } catch (error) {
      console.error('Error deleting notification:', error);
      toast({
        title: 'Error',
        description: 'Failed to delete notification',
        variant: 'destructive',
      });
    }
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const handleNotificationClick = (notification: Notification) => {
    // Mark as read when clicked
    if (!notification.read) {
      handleMarkAsRead(notification.id);
    }

    // Navigate based on notification type/category
    const type = notification.type || notification.category || '';
    const message = notification.message?.toLowerCase() || '';

    // Guarantor requests
    if (type.includes('GUARANTOR') || message.includes('guarantee') || message.includes('guarantor')) {
      setGuarantorDialogOpen(true);
      return;
    }

    // Loan notifications
    if (type.includes('LOAN') || message.includes('loan')) {
      navigate('/member/dashboard?tab=loans');
      return;
    }

    // Deposit notifications
    if (type.includes('DEPOSIT') || message.includes('deposit')) {
      navigate('/member/dashboard?tab=deposits');
      return;
    }

    // Top-up notifications
    if (type.includes('TOPUP') || type.includes('TOP_UP') || message.includes('top-up')) {
      navigate('/member/dashboard?tab=loans');
      return;
    }

    // Security/Device login notifications
    if (type.includes('SECURITY') || type.includes('DEVICE') || message.includes('login') || message.includes('device')) {
      // Stay on notifications page for security alerts
      return;
    }

    // Default: stay on notifications
  };

  const isGuarantorNotification = (notification: Notification) => {
    const type = notification.type || notification.category || '';
    const message = notification.message?.toLowerCase() || '';
    return type.includes('GUARANTOR') || message.includes('guarantee') || message.includes('guarantor');
  };

  const isClickableNotification = (notification: Notification) => {
    // All notifications are now clickable
    return true;
  };

  const getNotificationIcon = (notification: Notification) => {
    const type = notification.type || notification.category || '';
    const message = notification.message?.toLowerCase() || '';

    if (type.includes('GUARANTOR') || message.includes('guarantee')) {
      return <HandshakeIcon className="h-4 w-4 text-purple-600" />;
    }
    if (type.includes('LOAN') || message.includes('loan')) {
      return <span className="text-lg">💰</span>;
    }
    if (type.includes('DEPOSIT') || message.includes('deposit')) {
      return <span className="text-lg">💵</span>;
    }
    if (type.includes('SECURITY') || message.includes('device') || message.includes('login')) {
      return <span className="text-lg">🔐</span>;
    }
    return <Bell className="h-4 w-4 text-blue-600" />;
  };

  const getNotificationLabel = (notification: Notification) => {
    const type = notification.type || notification.category || '';
    const message = notification.message?.toLowerCase() || '';

    if (type.includes('GUARANTOR') || message.includes('guarantee')) {
      return 'GUARANTOR REQUEST - CLICK TO REVIEW';
    }
    if (type.includes('LOAN') || message.includes('loan')) {
      return 'LOAN UPDATE - CLICK TO VIEW';
    }
    if (type.includes('DEPOSIT') || message.includes('deposit')) {
      return 'DEPOSIT UPDATE - CLICK TO VIEW';
    }
    if (type.includes('SECURITY') || message.includes('device')) {
      return 'SECURITY ALERT - CLICK FOR DETAILS';
    }
    return 'CLICK TO VIEW DETAILS';
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Loading notifications...</p>
      </div>
    );
  }

  if (notifications.length === 0) {
    return (
      <Card className="border-none shadow-sm">
        <CardContent className="flex flex-col items-center justify-center h-64">
          <Bell className="h-12 w-12 text-muted-foreground/50 mb-4" />
          <p className="text-muted-foreground">No notifications yet</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {unreadCount > 0 && (
        <div className="flex justify-between items-center">
          <p className="text-sm text-muted-foreground">
            {unreadCount} unread notification{unreadCount !== 1 ? 's' : ''}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllAsRead}
            className="gap-2"
          >
            <CheckCheck className="h-4 w-4" />
            Mark all as read
          </Button>
        </div>
      )}

      <div className="space-y-2">
        {notifications.map((notification) => {
          const isGuarantorReq = isGuarantorNotification(notification);
          const isClickable = isClickableNotification(notification);
          
          return (
            <Card
              key={notification.id}
              className={`border-none shadow-sm transition-all ${
                !notification.read ? 'bg-blue-50 border-l-4 border-l-blue-500' : ''
              } ${isClickable ? 'cursor-pointer hover:shadow-lg hover:scale-[1.01]' : ''}`}
              onClick={() => isClickable && handleNotificationClick(notification)}
            >
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      {getNotificationIcon(notification)}
                      <span className="text-xs font-semibold text-purple-600 uppercase">
                        {getNotificationLabel(notification)}
                      </span>
                      {isClickable && <ExternalLink className="h-3 w-3 text-muted-foreground" />}
                    </div>
                    <p className="text-sm font-medium text-foreground">
                      {notification.message}
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-xs text-muted-foreground">
                        {notification.type}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {formatTime(notification.createdAt)}
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {!notification.read && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkAsRead(notification.id);
                        }}
                        className="text-blue-600 hover:bg-blue-100"
                        title="Mark as read"
                      >
                        <Check className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(notification.id);
                      }}
                      className="text-red-600 hover:bg-red-100"
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Guarantor Approval Dialog */}
      <GuarantorApprovalDialog
        open={guarantorDialogOpen}
        onOpenChange={setGuarantorDialogOpen}
        onApprovalChange={fetchNotifications}
      />
    </div>
  );
}
