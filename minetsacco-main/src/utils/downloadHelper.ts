import { Capacitor } from '@capacitor/core';
import { Filesystem, Directory } from '@capacitor/filesystem';

/**
 * Download and open a file on mobile or desktop
 * On mobile: Saves to app cache and opens with native app
 * On desktop: Downloads to Downloads folder
 */
export async function downloadAndOpenFile(
  blob: Blob,
  filename: string,
  onSuccess?: (message: string) => void,
  onError?: (error: string) => void
) {
  try {
    // Check if blob is actually a file or an error response
    if (blob.type === 'application/json' || blob.size === 0) {
      const text = await blob.text();
      try {
        const errorObj = JSON.parse(text);
        const errorMsg = errorObj.message || 'Failed to download file';
        console.error('Download error response:', errorObj);
        onError?.(errorMsg);
        return;
      } catch {
        // Not JSON, continue with download
      }
    }

    const isMobile = Capacitor.isNativePlatform();
    
    if (isMobile) {
      // Mobile: Use Capacitor Filesystem
      await downloadOnMobile(blob, filename, onSuccess, onError);
    } else {
      // Desktop: Use traditional download
      downloadOnDesktop(blob, filename, onSuccess, onError);
    }
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : 'Unknown error';
    console.error('Download error:', errorMsg);
    onError?.(errorMsg);
  }
}

/**
 * Download on mobile using Capacitor Filesystem
 */
async function downloadOnMobile(
  blob: Blob,
  filename: string,
  onSuccess?: (message: string) => void,
  onError?: (error: string) => void
) {
  try {
    // Convert blob to base64
    const base64 = await blobToBase64(blob);
    
    // Save to app's cache directory
    const result = await Filesystem.writeFile({
      path: filename,
      data: base64,
      directory: Directory.Cache,
    });
    
    console.log('File saved to:', result.uri);
    onSuccess?.(`File saved: ${filename}`);
    
    // Try to open with native app
    try {
      const { FileOpener } = await import('@capacitor-community/file-opener');
      await FileOpener.open({
        filePath: result.uri,
        contentType: blob.type || 'application/octet-stream',
      });
    } catch (openError) {
      // FileOpener not available, but file is saved
      console.log('FileOpener not available, but file is saved at:', result.uri);
      onSuccess?.(`File saved to cache: ${filename}`);
    }
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : 'Failed to save file';
    console.error('Mobile download error:', errorMsg);
    onError?.(errorMsg);
  }
}

/**
 * Download on desktop using traditional method
 */
function downloadOnDesktop(
  blob: Blob,
  filename: string,
  onSuccess?: (message: string) => void,
  onError?: (error: string) => void
) {
  try {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.style.display = 'none';
    document.body.appendChild(link);
    
    setTimeout(() => {
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      onSuccess?.('File downloaded successfully');
    }, 100);
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : 'Failed to download file';
    console.error('Desktop download error:', errorMsg);
    onError?.(errorMsg);
  }
}

/**
 * Convert blob to base64 string
 */
function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      const base64 = result.split(',')[1];
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}
