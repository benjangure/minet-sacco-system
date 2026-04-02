import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getBackendUrl, setBackendUrl } from '@/config/api';
import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function MemberSettings() {
  const navigate = useNavigate();
  const [backendUrl, setLocalBackendUrl] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    setLocalBackendUrl(getBackendUrl());
  }, []);

  const handleSave = () => {
    if (!backendUrl.trim()) {
      setMessage('Backend URL cannot be empty');
      return;
    }

    if (!backendUrl.startsWith('http://') && !backendUrl.startsWith('https://')) {
      setMessage('URL must start with http:// or https://');
      return;
    }

    setIsSaving(true);
    try {
      setBackendUrl(backendUrl);
      setMessage('Backend URL updated successfully. Reloading...');
    } catch (error) {
      setMessage('Error updating backend URL');
    } finally {
      setIsSaving(false);
    }
  };

  const handleReset = () => {
    setLocalBackendUrl('http://192.168.0.195:8080');
    setMessage('');
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-md mx-auto">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <button
            onClick={() => navigate('/member/dashboard')}
            className="p-2 hover:bg-gray-200 rounded-lg"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-2xl font-bold">Settings</h1>
        </div>

        {/* Backend URL Configuration */}
        <Card>
          <CardHeader>
            <CardTitle>Backend Configuration</CardTitle>
            <CardDescription>
              Configure the backend server URL for your network
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Backend URL</label>
              <Input
                type="text"
                value={backendUrl}
                onChange={(e) => setLocalBackendUrl(e.target.value)}
                placeholder="http://192.168.0.195:8080"
                className="w-full"
              />
              <p className="text-xs text-gray-500 mt-2">
                Enter the backend server URL. Format: http://IP_ADDRESS:PORT
              </p>
            </div>

            {message && (
              <div className={`p-3 rounded text-sm ${
                message.includes('Error') || message.includes('cannot')
                  ? 'bg-red-100 text-red-700'
                  : 'bg-green-100 text-green-700'
              }`}>
                {message}
              </div>
            )}

            <div className="flex gap-2">
              <Button
                onClick={handleSave}
                disabled={isSaving}
                className="flex-1 bg-blue-600 hover:bg-blue-700"
              >
                {isSaving ? 'Saving...' : 'Save'}
              </Button>
              <Button
                onClick={handleReset}
                variant="outline"
                className="flex-1"
              >
                Reset to Default
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Instructions */}
        <Card className="mt-4">
          <CardHeader>
            <CardTitle className="text-base">How to Find Your Backend URL</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2">
            <p>1. Find your computer's IP address on the network</p>
            <p>2. Ensure your phone is on the same WiFi network</p>
            <p>3. Enter the URL: <code className="bg-gray-100 px-2 py-1 rounded">http://YOUR_IP:8080</code></p>
            <p>4. Example: <code className="bg-gray-100 px-2 py-1 rounded">http://192.168.1.100:8080</code></p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
