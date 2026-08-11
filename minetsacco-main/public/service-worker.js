const CACHE_NAME = 'minet-sacco-v4';
const OFFLINE_CACHE = 'minet-sacco-offline-v4';
const LOGO_CACHE_VERSION = 'v4-minet-logo-final';

// Critical pages to cache for offline access
const CRITICAL_PAGES = [
  '/member/dashboard',
  '/member/apply-loan',
  '/member/account-statement',
  '/member/loan-balances'
];

// Install event - cache critical pages and force logo refresh
self.addEventListener('install', event => {
  console.log('[Service Worker] Installing v3 with new logo...');
  event.waitUntil(
    Promise.all([
      // Cache critical pages
      caches.open(OFFLINE_CACHE).then(cache => {
        return cache.addAll(CRITICAL_PAGES).catch(() => {
          console.log('[Service Worker] Could not cache critical pages during install');
        });
      }),
      // Force refresh logo and icon files
      caches.open(CACHE_NAME).then(cache => {
        const logoFiles = [
          '/Minet-Logo1.png',
          '/icon-192.png',
          '/icon-512.png',
          '/splash-1080x1920.png',
          '/splash-1125x2436.png',
          '/manifest.json'
        ];
        return Promise.all(
          logoFiles.map(url => {
            // Force network fetch with cache bypass
            return fetch(url, { cache: 'reload' })
              .then(response => {
                if (response.ok) {
                  return cache.put(url, response);
                }
              })
              .catch(err => {
                console.log('[Service Worker] Could not fetch logo file:', url, err);
              });
          })
        );
      })
    ]).then(() => {
      console.log('[Service Worker] Logo cache updated, forcing activation');
      return self.skipWaiting();
    })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
  console.log('[Service Worker] Activating...');
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME && cacheName !== OFFLINE_CACHE) {
            console.log('[Service Worker] Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Push event - handle incoming push notifications
self.addEventListener('push', event => {
  console.log('[Service Worker] Push notification received:', event);
  
  let notificationData = {
    title: 'Minet SACCO',
    body: 'You have a new notification',
    icon: '/icon-512.png',
    badge: '/icon-192.png',
    tag: 'default',
    requireInteraction: false,
    data: {
      url: '/member/dashboard?tab=notifications'
    }
  };

  // Parse push data if available
  if (event.data) {
    try {
      const payload = event.data.json();
      console.log('[Service Worker] Push payload:', payload);
      
      notificationData = {
        title: payload.title || notificationData.title,
        body: payload.body || payload.message || notificationData.body,
        icon: payload.icon || notificationData.icon,
        badge: payload.badge || notificationData.badge,
        tag: payload.tag || payload.type || notificationData.tag,
        requireInteraction: payload.requireInteraction || false,
        data: {
          url: payload.url || payload.clickAction || notificationData.data.url,
          type: payload.type,
          id: payload.id,
          ...payload.data
        },
        actions: payload.actions || []
      };
    } catch (error) {
      console.error('[Service Worker] Error parsing push data:', error);
      notificationData.body = event.data.text();
    }
  }

  event.waitUntil(
    self.registration.showNotification(notificationData.title, {
      body: notificationData.body,
      icon: notificationData.icon,
      badge: notificationData.badge,
      tag: notificationData.tag,
      requireInteraction: notificationData.requireInteraction,
      data: notificationData.data,
      actions: notificationData.actions,
      vibrate: [200, 100, 200],
      timestamp: Date.now()
    })
  );
});

// Notification click event - handle user interaction with notifications
self.addEventListener('notificationclick', event => {
  console.log('[Service Worker] Notification clicked:', event);
  
  event.notification.close();

  const clickAction = event.action;
  const notificationData = event.notification.data || {};
  let targetUrl = notificationData.url || '/member/dashboard?tab=notifications';

  // Handle action buttons if clicked
  if (clickAction === 'view') {
    targetUrl = notificationData.url || '/member/dashboard?tab=notifications';
  } else if (clickAction === 'dismiss') {
    return; // Just close the notification
  }

  // Deep linking based on notification type
  if (notificationData.type) {
    switch (notificationData.type) {
      case 'LOAN':
      case 'LOAN_APPROVED':
      case 'LOAN_REJECTED':
      case 'LOAN_DISBURSED':
      case 'LOAN_STATUS_CHANGED':
        targetUrl = notificationData.loanId 
          ? `/member/dashboard?tab=loans&loanId=${notificationData.loanId}`
          : '/member/dashboard?tab=loans';
        break;
        
      case 'DEPOSIT':
      case 'DEPOSIT_STATUS_CHANGED':
        targetUrl = notificationData.depositId 
          ? `/member/dashboard?tab=deposits&depositId=${notificationData.depositId}`
          : '/member/dashboard?tab=deposits';
        break;
        
      case 'GUARANTOR':
      case 'GUARANTOR_REQUEST':
        targetUrl = '/member/guarantor-approvals';
        break;
        
      case 'PAYMENT_DUE':
      case 'PAYMENT_OVERDUE':
        targetUrl = '/member/dashboard?tab=transact';
        break;
        
      case 'APPROVAL':
        targetUrl = '/member/dashboard?tab=notifications';
        break;
        
      case 'SYSTEM':
      case 'SECURITY_ALERT':
      case 'NEW_DEVICE_LOGIN':
        targetUrl = '/member/dashboard?tab=notifications';
        break;
    }
  }

  // Open the app and navigate to the target URL
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then(clientList => {
        // Check if there's already a window open
        for (const client of clientList) {
          if (client.url.includes('/member') && 'focus' in client) {
            client.focus();
            return client.navigate(targetUrl);
          }
        }
        // If no window is open, open a new one
        if (clients.openWindow) {
          return clients.openWindow(targetUrl);
        }
      })
  );
});

// Notification close event - track when users dismiss notifications
self.addEventListener('notificationclose', event => {
  console.log('[Service Worker] Notification closed:', event.notification.tag);
  
  // Optional: Send analytics to track dismissal rate
  const notificationData = event.notification.data || {};
  if (notificationData.id) {
    // Could send to analytics endpoint
    // fetch('/api/analytics/notification-dismissed', { ... })
  }
});

// Background sync event - handle offline actions when connection returns
self.addEventListener('sync', event => {
  console.log('[Service Worker] Background sync:', event.tag);
  
  if (event.tag === 'sync-notifications') {
    event.waitUntil(syncNotifications());
  } else if (event.tag === 'sync-offline-actions') {
    event.waitUntil(syncOfflineActions());
  }
});

// Sync notifications when back online
async function syncNotifications() {
  try {
    console.log('[Service Worker] Syncing notifications...');
    // Fetch latest notifications from server
    // This will be called when the device comes back online
    return Promise.resolve();
  } catch (error) {
    console.error('[Service Worker] Error syncing notifications:', error);
    return Promise.reject(error);
  }
}

// Sync offline actions when back online
async function syncOfflineActions() {
  try {
    console.log('[Service Worker] Syncing offline actions...');
    // Process any actions that were queued while offline
    return Promise.resolve();
  } catch (error) {
    console.error('[Service Worker] Error syncing offline actions:', error);
    return Promise.reject(error);
  }
}

// Fetch event - Network first with offline fallback
self.addEventListener('fetch', event => {
  // Skip non-GET requests
  if (event.request.method !== 'GET') {
    return;
  }

  // Skip chrome-extension and other non-http(s) schemes
  const url = new URL(event.request.url);
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Skip API calls - handle separately
  if (event.request.url.includes('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          if (response && response.status === 200) {
            const responseClone = response.clone();
            caches.open(CACHE_NAME).then(cache => {
              cache.put(event.request, responseClone).catch(() => {
                // Silently ignore cache errors
              });
            });
          }
          return response;
        })
        .catch(() => {
          // Try to return cached API response
          return caches.match(event.request)
            .then(response => {
              return response || new Response(
                JSON.stringify({ error: 'Offline - data may be outdated' }),
                {
                  status: 503,
                  headers: { 'Content-Type': 'application/json' }
                }
              );
            });
        })
    );
    return;
  }

  // For HTML pages - network first
  event.respondWith(
    fetch(event.request)
      .then(response => {
        if (response && response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(event.request, responseClone).catch(() => {
              // Silently ignore cache errors
            });
          });
        }
        return response;
      })
      .catch(() => {
        // Try cache first for offline pages
        return caches.match(event.request)
          .then(response => {
            if (response) {
              return response;
            }
            // Return offline page for critical routes
            if (CRITICAL_PAGES.some(page => event.request.url.includes(page))) {
              return caches.match('/member/dashboard')
                .then(response => response || new Response('Offline - please check your connection', {
                  status: 503,
                  headers: { 'Content-Type': 'text/plain' }
                }));
            }
            return new Response('Offline - please check your connection', {
              status: 503,
              headers: { 'Content-Type': 'text/plain' }
            });
          });
      })
  );
});

// Message event - handle messages from the application
self.addEventListener('message', event => {
  console.log('[Service Worker] Message received:', event.data);
  
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  } else if (event.data && event.data.type === 'CLEAR_CACHE') {
    event.waitUntil(
      caches.keys().then(cacheNames => {
        return Promise.all(
          cacheNames.map(cacheName => caches.delete(cacheName))
        );
      })
    );
  }
});
