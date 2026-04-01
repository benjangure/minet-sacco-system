const CACHE_NAME = 'minet-sacco-v1';
const OFFLINE_CACHE = 'minet-sacco-offline-v1';

// Critical pages to cache for offline access
const CRITICAL_PAGES = [
  '/member/dashboard',
  '/member/apply-loan',
  '/member/account-statement',
  '/member/loan-balances'
];

// Install event - cache critical pages
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(OFFLINE_CACHE).then(cache => {
      return cache.addAll(CRITICAL_PAGES).catch(() => {
        // Silently fail if pages can't be cached yet
        console.log('Could not cache critical pages during install');
      });
    }).then(() => self.skipWaiting())
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME && cacheName !== OFFLINE_CACHE) {
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Fetch event - Network first with offline fallback
self.addEventListener('fetch', event => {
  // Skip non-GET requests
  if (event.request.method !== 'GET') {
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
              cache.put(event.request, responseClone);
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
            cache.put(event.request, responseClone);
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
