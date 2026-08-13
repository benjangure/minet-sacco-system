import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.minetsacco.memberportal',
  appName: 'Minet SACCO',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
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
    allowMixedContent: false,
    captureInput: true
  }
};

export default config;
