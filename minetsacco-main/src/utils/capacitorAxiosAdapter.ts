/**
 * Capacitor HTTP Adapter for Axios
 *
 * On Android (Capacitor native platform), replaces Axios's default XHR adapter
 * with one that uses CapacitorHttp.request(). This bypasses the WebView's CORS
 * enforcement, which is what blocks api.get/post calls after login.
 *
 * On web / iOS, falls through to Axios's standard adapter unchanged.
 */

import { Capacitor, CapacitorHttp } from '@capacitor/core';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import axios from 'axios';

// CapacitorHttp wants the body as a plain object for JSON payloads,
// but Axios may have already serialised it to a string.
function parseBody(data: any): any {
  if (data === undefined || data === null) return undefined;
  if (typeof data === 'string') {
    try { return JSON.parse(data); } catch { return data; }
  }
  return data;
}

// Convert an Axios headers object (which can be AxiosHeaders or a plain object)
// to a plain Record<string,string> that CapacitorHttp accepts.
function flattenHeaders(headers: any): Record<string, string> {
  if (!headers) return {};
  const out: Record<string, string> = {};
  const raw = typeof headers.toJSON === 'function' ? headers.toJSON() : headers;
  for (const [k, v] of Object.entries(raw)) {
    if (v !== null && v !== undefined) {
      out[k] = String(v);
    }
  }
  // Remove headers CapacitorHttp sets itself to avoid conflicts
  delete out['Content-Length'];
  return out;
}

export function buildCapacitorAdapter() {
  return async function capacitorAdapter(config: AxiosRequestConfig): Promise<AxiosResponse> {
    const method = (config.method || 'GET').toUpperCase();

    // Build the full URL (Axios normally does this internally)
    let url = config.url || '';
    if (config.baseURL && !url.startsWith('http')) {
      url = config.baseURL.replace(/\/$/, '') + '/' + url.replace(/^\//, '');
    }

    // Append query params if any
    if (config.params) {
      const search = new URLSearchParams();
      for (const [k, v] of Object.entries(config.params)) {
        if (v !== undefined && v !== null) search.append(k, String(v));
      }
      const qs = search.toString();
      if (qs) url += (url.includes('?') ? '&' : '?') + qs;
    }

    const headers = flattenHeaders(config.headers);

    console.log(`[CapacitorAdapter] ${method} ${url}`);

    const response = await CapacitorHttp.request({
      url,
      method,
      headers,
      data: ['GET', 'HEAD', 'DELETE'].includes(method) ? undefined : parseBody(config.data),
    });

    const status = response.status;
    const responseData = response.data;

    // Axios settle logic: reject on error status
    const settle = (resolve: Function, reject: Function, res: AxiosResponse) => {
      const validateStatus = config.validateStatus;
      if (!validateStatus || validateStatus(res.status)) {
        resolve(res);
      } else {
        reject(new axios.AxiosError(
          `Request failed with status code ${res.status}`,
          String(res.status),
          config as any,
          null,
          res,
        ));
      }
    };

    return new Promise((resolve, reject) => {
      settle(resolve, reject, {
        data: responseData,
        status,
        statusText: String(status),
        headers: response.headers || {},
        config: config as any,
        request: null,
      });
    });
  };
}

/**
 * Returns the correct adapter for the current platform.
 * On Android native: CapacitorHttp adapter.
 * Everywhere else: Axios default (XHR/HTTP).
 */
export function getPlatformAdapter() {
  if (Capacitor.isNativePlatform() && Capacitor.getPlatform() === 'android') {
    return buildCapacitorAdapter();
  }
  // Return undefined → Axios picks its default adapter (XHR in browser, http in Node)
  return undefined;
}
