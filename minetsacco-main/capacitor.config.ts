import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.minetsacco.memberportal',
  appName: 'Minet SACCO',
  webDir: 'dist',
  server: {
    cleartext: true,
    androidScheme: 'http',
    allowNavigation: [
      '*' // Allow navigation to any domain for flexibility
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
    allowMixedContent: true,
    captureInput: true
  }
};

export default config;
