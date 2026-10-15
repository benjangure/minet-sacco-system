/**
 * Native HTTP Wrapper for Android
 * Uses Capacitor's CapacitorHttp from @capacitor/core for better Android network compatibility
 */

import { Capacitor, CapacitorHttp, HttpResponse } from '@capacitor/core';

interface FetchOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: string | FormData;
}

interface NativeHttpResponse {
  ok: boolean;
  status: number;
  statusText?: string;
  json: () => Promise<any>;
  text: () => Promise<string>;
  blob: () => Promise<Blob>;
  headers: {
    get: (name: string) => string | null;
  };
}

/**
 * Wrapper around fetch that uses CapacitorHttp on Android for better SSL/network handling
 */
export async function nativeFetch(url: string, options: FetchOptions = {}): Promise<NativeHttpResponse> {
  const method = options.method || 'GET';

  // On Android, use CapacitorHttp for better network/SSL handling
  if (Capacitor.getPlatform() === "android" && !(options.body instanceof FormData)) {
    try {
      console.log('[NativeHttp] Using CapacitorHttp for Android:', url);
      
      // Always ask the native layer for the raw body (base64). Decoding lazily keeps PDFs / Excel
      // files intact; asking for text would corrupt any binary payload.
      const response: HttpResponse = await CapacitorHttp.request({
        url: url,
        method: method,
        headers: options.headers || {},
        data: typeof options.body === "string" ? JSON.parse(options.body) : undefined,
        responseType: 'blob',
      });

      console.log('[NativeHttp] CapacitorHttp response status:', response.status);

      const headerGet = (name: string): string | null => {
        const h = (response.headers || {}) as Record<string, string>;
        const wanted = name.toLowerCase();
        for (const k of Object.keys(h)) {
          if (k.toLowerCase() === wanted) return h[k];
        }
        return null;
      };

      // Raw bytes of the body (base64 from the native layer; tolerate plugins that return text/objects)
      const toBytes = (): Uint8Array => {
        const data: unknown = response.data;
        if (data === undefined || data === null) return new Uint8Array(0);
        if (typeof data === 'string') {
          try {
            const bin = atob(data);
            const out = new Uint8Array(bin.length);
            for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
            return out;
          } catch {
            return new TextEncoder().encode(data);
          }
        }
        return new TextEncoder().encode(JSON.stringify(data));
      };

      const toText = (): string => new TextDecoder('utf-8').decode(toBytes());

      return {
        ok: response.status >= 200 && response.status < 300,
        status: response.status,
        json: async () => JSON.parse(toText()),
        text: async () => toText(),
        blob: async () => new Blob([toBytes()], { type: headerGet('content-type') || 'application/octet-stream' }),
        headers: {
          get: (name: string) => headerGet(name),
        },
      };
    } catch (error) {
      console.error('[NativeHttp] CapacitorHttp error:', error);
      // If CapacitorHttp fails, fall back to regular fetch
      console.log('[NativeHttp] Falling back to regular fetch');
    }
  }

  // For web and iOS, or if Android CapacitorHttp failed, use regular fetch
  console.log('[NativeHttp] Using regular fetch:', url);
  const response = await fetch(url, { ...options, method });
  
  return {
    ok: response.ok,
    status: response.status,
    json: () => response.json(),
    text: () => response.text(),
    blob: () => response.blob(),
    headers: response.headers
  };
}
