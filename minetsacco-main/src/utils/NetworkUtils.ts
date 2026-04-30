/**
 * Network utilities for IP detection and connectivity
 */

// Get current IP address from browser context
export const getCurrentIP = (): string => {
  // This will be populated by the backend health check
  return '';
};

// Detect if we're on the same network as the backend
export const detectBackendIP = async (): Promise<string[]> => {
  const commonIPRanges = [
    '192.168.0.', '192.168.1.', '192.168.2.', '192.168.3.',
    '192.168.4.', '192.168.5.', '192.168.6.', '192.168.7.',
    '192.168.8.', '192.168.9.', '192.168.10.', '192.168.11.',
    '10.0.0.', '10.0.1.', '10.0.2.', '10.0.3.',
    '172.16.0.', '172.16.1.', '172.16.2.', '172.16.3.'
  ];
  
  const candidateIPs: string[] = [];
  
  // Test common IP ranges
  for (const prefix of commonIPRanges) {
    for (let i = 1; i <= 254; i++) {
      const ip = `${prefix}${i}`;
      candidateIPs.push(ip);
    }
  }
  
  // Limit to reasonable number of candidates to avoid overwhelming the network
  const limitedCandidates = candidateIPs.slice(0, 50);
  
  return limitedCandidates;
};

// Test connectivity to a specific IP:port
export const testConnectivity = async (ip: string, port: number = 8080): Promise<boolean> => {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000); // 3 second timeout
    
    const response = await fetch(`http://${ip}:${port}/api/auth/health`, {
      method: 'GET',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    clearTimeout(timeoutId);
    return response.ok;
  } catch (error) {
    return false;
  }
};

// Auto-discover backend IP on the network
export const autoDiscoverBackend = async (): Promise<string | null> => {
  const candidates = await detectBackendIP();
  
  // Test candidates in parallel (batch of 5 at a time to avoid overwhelming)
  const batchSize = 5;
  for (let i = 0; i < candidates.length; i += batchSize) {
    const batch = candidates.slice(i, i + batchSize);
    
    const results = await Promise.allSettled(
      batch.map(async (ip) => {
        const isReachable = await testConnectivity(ip);
        return { ip, isReachable };
      })
    );
    
    // Find first reachable IP
    for (const result of results) {
      if (result.status === 'fulfilled' && result.value.isReachable) {
        return result.value.ip;
      }
    }
  }
  
  return null;
};

// Get suggested IP based on current network pattern
export const getSuggestedIP = async (): Promise<string> => {
  const stored = localStorage.getItem('backendUrl');
  if (stored) {
    try {
      const url = new URL(stored);
      const ip = url.hostname;
      const parts = ip.split('.');
      
      // If we have a stored IP, suggest the same subnet with common variations
      if (parts.length === 4) {
        const subnet = parts.slice(0, 3).join('.');
        const suggestions = [
          `${subnet}.1`, `${subnet}.10`, `${subnet}.20`, `${subnet}.30`,
          `${subnet}.40`, `${subnet}.50`, `${subnet}.100`, `${subnet}.195`
        ];
        return suggestions[0];
      }
    } catch (error) {
      // Ignore URL parsing errors
    }
  }
  
  // Enhanced default suggestions based on network detection
  // Try to detect current network pattern first
  const currentIP = await detectCurrentNetworkIP();
  if (currentIP) {
    const parts = currentIP.split('.');
    if (parts.length === 4) {
      const subnet = parts.slice(0, 3).join('.');
      return `${subnet}.41`; // Common server IP in the same subnet
    }
  }
  
  // Fallback to common patterns including all supported subnets
  return '192.168.1.41'; // Updated to 192.168.1.x subnet
};

// Detect current network IP for smarter suggestions
export const detectCurrentNetworkIP = (): Promise<string | null> => {
  try {
    // Try to get local IP from WebRTC (works in browsers)
    return new Promise((resolve) => {
      const pc = new RTCPeerConnection({ iceServers: [] });
      pc.createDataChannel('');
      pc.createOffer({})
        .catch(() => {}); // Handle potential errors
      
      pc.onicecandidate = (event) => {
        if (event.candidate && event.candidate.candidate) {
          const candidate = event.candidate.candidate;
          const match = candidate.match(/(\d+\.\d+\.\d+\.\d+)/);
          if (match) {
            resolve(match[1]);
            pc.close();
          }
        }
      };
      
      // Timeout after 2 seconds
      setTimeout(() => {
        resolve(null);
        pc.close();
      }, 2000);
    });
  } catch (error) {
    return Promise.resolve(null);
  }
};

// Validate IP address format
export const isValidIP = (ip: string): boolean => {
  const ipRegex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
  return ipRegex.test(ip);
};
