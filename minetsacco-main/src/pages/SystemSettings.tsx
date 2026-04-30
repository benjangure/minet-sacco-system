import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Loader, Edit2, Save, X } from 'lucide-react';
import api from '../config/api';

interface Setting {
  id: number;
  settingKey: string;
  settingValue: string;
  settingType: string;
  description: string;
}

export default function SystemSettings() {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editValue, setEditValue] = useState('');

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    try {
      const response = await api.get('/admin/settings');
      if (response.data.success) {
        setSettings(response.data.data);
      }
    } catch (err: any) {
      setError('Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (setting: Setting) => {
    setEditingId(setting.id);
    setEditValue(setting.settingValue);
  };

  const handleSave = async (key: string) => {
    try {
      const response = await api.put(`/admin/settings/${key}`, { value: editValue });
      if (response.data.success) {
        setSettings(settings.map(s => s.settingKey === key ? response.data.data : s));
        setEditingId(null);
        setSuccess('Setting updated successfully');
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update setting');
    }
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditValue('');
  };

  const renderInput = (setting: Setting) => {
    if (editingId !== setting.id) {
      return <span className="text-gray-900">{setting.settingValue}</span>;
    }

    switch (setting.settingType) {
      case 'BOOLEAN':
        return (
          <select
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg"
          >
            <option value="true">True</option>
            <option value="false">False</option>
          </select>
        );
      case 'INTEGER':
        return (
          <input
            type="number"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg w-full"
          />
        );
      case 'DECIMAL':
        return (
          <input
            type="number"
            step="0.01"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg w-full"
          />
        );
      default:
        return (
          <input
            type="text"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg w-full"
          />
        );
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">System Settings</h1>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {success && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
          <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
          <p className="text-green-800">{success}</p>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Setting</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Type</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Value</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Description</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {settings.map((setting) => (
              <tr key={setting.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 text-sm font-medium text-gray-900">{setting.settingKey}</td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs">
                    {setting.settingType}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm">
                  {renderInput(setting)}
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">{setting.description}</td>
                <td className="px-6 py-4 text-sm">
                  {editingId === setting.id ? (
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleSave(setting.settingKey)}
                        className="text-green-600 hover:text-green-700"
                      >
                        <Save className="w-4 h-4" />
                      </button>
                      <button
                        onClick={handleCancel}
                        className="text-red-600 hover:text-red-700"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleEdit(setting)}
                      className="text-blue-600 hover:text-blue-700"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
