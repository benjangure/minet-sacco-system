/**
 * Native HTTP Wrapper for Android
 * Uses Capacitor's CapacitorHttp from @capacitor/core for better Android network compatibility
 */

import { Capacitor, CapacitorHttp, HttpResponse } from '@capacitor/core';

interface FetchOptions {
  method: string;
  headers?: Record<string, string>;
  body?: string;
}

interface NativeHttpResponse {
  ok: boolean;
  status: number;
  json: () => Promise<any>;
  text: () => Promise<string>;
}

/**
 * Wrapper around fetch that uses CapacitorHttp on Android for better SSL/network handling
 */
export async function nativeFetch(url: string, options: FetchOptions = { method: 'GET' }): Promise<NativeHttpResponse> {
  // On Android, use CapacitorHttp for better network/SSL handling
  if (Capacitor.getPlatform() === 'android') {
    try {
      console.log('[NativeHttp] Using CapacitorHttp for Android:', url);
      
      const response: HttpResponse = await CapacitorHttp.request({
        url: url,
        method: options.method,
        headers: options.headers || {},
        data: options.body ? JSON.parse(options.body) : undefined,
      });

      console.log('[NativeHttp] CapacitorHttp response status:', response.status);

      // Convert CapacitorHttp response to fetch-like response
      return {
        ok: response.status >= 200 && response.status < 300,
        status: response.status,
        json: async () => {
          if (typeof response.data === 'string') {
            return JSON.parse(response.data);
          }
          return response.data;
        },
        text: async () => {
          if (typeof response.data === 'string') {
            return response.data;
          }
          return JSON.stringify(response.data);
        }
      };
    } catch (error) {
      console.error('[NativeHttp] CapacitorHttp error:', error);
      // If CapacitorHttp fails, fall back to regular fetch
      console.log('[NativeHttp] Falling back to regular fetch');
    }
  }

  // For web and iOS, or if Android CapacitorHttp failed, use regular fetch
  console.log('[NativeHttp] Using regular fetch:', url);
  const response = await fetch(url, options);
  
  return {
    ok: response.ok,
    status: response.status,
    json: () => response.json(),
    text: () => response.text()
  };
}
