import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Loader2, Wifi, RefreshCw, CheckCircle, XCircle, Search } from 'lucide-react';
import { setBackendUrl, getBackendUrl } from '@/config/api';
import { testConnectivity, autoDiscoverBackend, getSuggestedIP } from '@/utils/NetworkUtils';
import { useToast } from '@/hooks/use-toast';
import { Capacitor } from '@capacitor/core';

interface BackendConnectionManagerProps {
  onConnectionSuccess?: () => void;
  showOnMount?: boolean;
  onClose?: () => void;
}

export const BackendConnectionManager: React.FC<BackendConnectionManagerProps> = ({
  onConnectionSuccess,
  showOnMount = false,
  onClose
}) => {
  const [currentUrl, setCurrentUrl] = useState('');
  const [testUrl, setTestUrl] = useState('');
  const [isTesting, setIsTesting] = useState(false);
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'idle' | 'testing' | 'connected' | 'failed'>('idle');
  const [discoveredIPs, setDiscoveredIPs] = useState<string[]>([]);
  const [showManager, setShowManager] = useState(showOnMount);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const { toast } = useToast();

  useEffect(() => {
    const isNative = Capacitor.isNativePlatform();
    console.log('BackendConnectionManager - Platform:', isNative ? 'Native (APK)' : 'Web');
    
    const url = getBackendUrl();
    console.log('BackendConnectionManager - Current URL:', url);
    setCurrentUrl(url);
    setTestUrl(url);
    
    // Load suggestions asynchronously
    const loadSuggestions = async () => {
      try {
        const suggestedIP = await getSuggestedIP();
        const baseSuggestions = [
          suggestedIP,
          '192.168.0.34',
          '192.168.0.41',
          '192.168.0.195',
          '192.168.1.34',
          '192.168.1.50',
          '192.168.1.55',
          '192.168.1.195',
          '192.168.100.34',
          '192.168.100.41',
          '192.168.100.50',
          '192.168.100.56',
          '192.168.100.195',
          '10.0.0.34',
          '10.0.0.50'
        ];
        setSuggestions(baseSuggestions);
      } catch (error) {
        console.warn('Failed to load suggestions:', error);
        // Fallback to static suggestions
        const fallbackSuggestions = [
          '192.168.0.34',
          '192.168.0.41',
          '192.168.0.195',
          '192.168.1.34',
          '192.168.1.50',
          '192.168.1.55',
          '192.168.1.195',
          '192.168.100.34',
          '192.168.100.41',
          '192.168.100.50',
          '192.168.100.56',
          '192.168.100.195',
          '10.0.0.34',
          '10.0.0.50'
        ];
        setSuggestions(fallbackSuggestions);
      }
    };
    
    loadSuggestions();
  }, []);

  const handleTestConnection = async (url?: string) => {
    const urlToTest = url || testUrl;
    if (!urlToTest) {
      toast({
        title: "Error",
        description: "Please enter a backend URL",
        variant: "destructive"
      });
      return;
    }

    let testIP = urlToTest;
    try {
      const urlObj = new URL(urlToTest);
      testIP = urlObj.hostname;
    } catch (error) {
      testIP = urlToTest;
    }

    console.log('Testing connection to:', testIP);
    setIsTesting(true);
    setConnectionStatus('testing');

    try {
      const isConnected = await testConnectivity(testIP);
      setConnectionStatus(isConnected ? 'connected' : 'failed');
      
      if (isConnected) {
        toast({
          title: "Connection Successful",
          description: `Connected to backend at ${urlToTest}`,
        });
        
        if (url && url !== currentUrl) {
          const fullUrl = urlToTest.includes('://') ? urlToTest : `http://${urlToTest}:8080`;
          setBackendUrl(fullUrl);
          setCurrentUrl(fullUrl);
          onConnectionSuccess?.();
        }
      } else {
        toast({
          title: "Connection Failed",
          description: `Cannot reach backend at ${urlToTest}`,
          variant: "destructive"
        });
      }
    } catch (error) {
      setConnectionStatus('failed');
      toast({
        title: "Connection Error",
        description: "Failed to test connection",
        variant: "destructive"
      });
    } finally {
      setIsTesting(false);
    }
  };

  const handleAutoDiscover = async () => {
    setIsDiscovering(true);
    toast({
      title: "Discovering Backend",
      description: "Scanning network for SACCO backend...",
    });

    try {
      const discoveredIP = await autoDiscoverBackend();
      if (discoveredIP) {
        const fullUrl = `http://${discoveredIP}:8080`;
        setTestUrl(fullUrl);
        setDiscoveredIPs([discoveredIP]);
        
        toast({
          title: "Backend Found",
          description: `Discovered backend at ${discoveredIP}:8080`,
        });
        
        await handleTestConnection(fullUrl);
      } else {
        toast({
          title: "No Backend Found",
          description: "Could not automatically discover backend server",
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "Discovery Failed",
        description: "Error during network discovery",
        variant: "destructive"
      });
    } finally {
      setIsDiscovering(false);
    }
  };

  const handleSaveConnection = () => {
    if (!testUrl) {
      toast({
        title: "Error",
        description: "Please enter a valid backend URL",
        variant: "destructive"
      });
      return;
    }

    const fullUrl = testUrl.includes('://') ? testUrl : `http://${testUrl}:8080`;
    setBackendUrl(fullUrl);
    setCurrentUrl(fullUrl);
    
    toast({
      title: "Backend URL Updated",
      description: `Backend URL set to ${fullUrl}`,
    });
    
    onConnectionSuccess?.();
    setShowManager(false);
  };

  const handleQuickConnect = (ip: string) => {
    const fullUrl = `http://${ip}:8080`;
    setTestUrl(fullUrl);
    handleTestConnection(fullUrl);
  };

  const handleClose = () => {
    console.log('BackendConnectionManager - Closing modal');
    setShowManager(false);
  };

  const getStatusIcon = () => {
    switch (connectionStatus) {
      case 'testing':
        return <Loader2 className="w-4 h-4 animate-spin" />;
      case 'connected':
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case 'failed':
        return <XCircle className="w-4 h-4 text-red-600" />;
      default:
        return <Wifi className="w-4 h-4" />;
    }
  };

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'testing':
        return 'bg-orange-100 text-orange-800';
      case 'connected':
        return 'bg-green-100 text-green-800';
      case 'failed':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (!showManager) {
    return (
      <div className="fixed bottom-4 right-4 z-50">
        <Button
          onClick={() => setShowManager(true)}
          variant="outline"
          size="sm"
          className="bg-white shadow-lg"
        >
          <Wifi className="w-4 h-4 mr-2" />
          Network Settings
        </Button>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Wifi className="w-5 h-5" />
              Backend Connection Manager
            </CardTitle>
            <Button variant="ghost" size="sm" onClick={handleClose}>
              ×
            </Button>
          </div>
        </CardHeader>
        
        <CardContent className="space-y-6">
          <Alert>
            <div className="flex items-center gap-2">
              {getStatusIcon()}
              <div>
                <AlertDescription className="font-medium">
                  Status: <Badge className={getStatusColor()}>{connectionStatus}</Badge>
                </AlertDescription>
                <AlertDescription className="text-sm mt-1">
                  Current: {currentUrl || 'Not set'}
                </AlertDescription>
              </div>
            </div>
          </Alert>

          <div className="flex gap-2">
            <Button
              onClick={handleAutoDiscover}
              disabled={isDiscovering}
              variant="outline"
              className="flex-1"
            >
              {isDiscovering ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Search className="w-4 h-4 mr-2" />
              )}
              Auto-Discover Backend
            </Button>
            
            <Button
              onClick={() => handleTestConnection()}
              disabled={isTesting || !testUrl}
              variant="outline"
              className="flex-1"
            >
              {isTesting ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4 mr-2" />
              )}
              Test Connection
            </Button>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium">Backend URL</label>
            <Input
              value={testUrl}
              onChange={(e) => setTestUrl(e.target.value)}
              placeholder="e.g., 192.168.0.34 or http://192.168.0.34:8080"
              className="w-full"
            />
            <p className="text-xs text-muted-foreground">
              Enter IP address or full URL. Port 8080 will be used if not specified.
            </p>
          </div>

          {suggestions.length > 0 && (
            <div className="space-y-2">
              <label className="text-sm font-medium">Quick Connect - Common IPs:</label>
              <div className="flex flex-wrap gap-2">
                {suggestions.map((ip) => (
                  <Button
                    key={`suggestion-${ip}`}
                    onClick={() => handleQuickConnect(ip)}
                    variant="outline"
                    size="sm"
                    className="text-xs"
                  >
                    {ip}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {discoveredIPs.length > 0 && (
            <div className="space-y-2">
              <label className="text-sm font-medium">Discovered Backends:</label>
              <div className="flex flex-wrap gap-2">
                {discoveredIPs.map((ip) => (
                  <Button
                    key={`discovered-${ip}`}
                    onClick={() => handleQuickConnect(ip)}
                    variant="outline"
                    size="sm"
                    className="text-xs bg-green-50 border-green-200"
                  >
                    <CheckCircle className="w-3 h-3 mr-1" />
                    {ip}
                  </Button>
                ))}
                <Button variant="outline" onClick={handleClose} key="discovered-back">
                  <div className="flex items-center gap-2">
                    <XCircle className="w-4 h-4" />
                    Back
                  </div>
                </Button>
              </div>
            <div className="text-xs text-muted-foreground space-y-1">
            </div>
            </div>
          )}
          <div className="flex gap-2 pt-4">
            <Button onClick={handleSaveConnection} className="flex-1">
              Save & Connect
            </Button>
            <Button variant="outline" onClick={handleClose} key="main-back">
              <div className="flex items-center gap-2">
                <XCircle className="w-4 h-4" />
                Back
              </div>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
