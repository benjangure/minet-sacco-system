import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { SplashScreen } from '@capacitor/splash-screen';

// Show splash immediately
SplashScreen.show({
  showDuration: 3000,
  autoHide: true,
  backgroundColor: '#ef4444',
  spinnerStyle: 'large',
  spinnerColor: '#ffffff',
});

createRoot(document.getElementById("root")!).render(<App />);
