import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.minetsacco.memberportal',
  appName: 'Minet SACCO',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
    allowNavigation: [
      '*'
    ]
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 4000,
      launchAutoHide: true,
      backgroundColor: '#ffffff',
      showSpinner: false
    }
  },
  ios: {
    scheme: 'MinetSACCO'
  },
  android: {
    // allowMixedContent lets the native WebView call http:// backend
    // while still loading the app assets over the internal https scheme
    allowMixedContent: true,
    captureInput: true
  }
};

export default config;
