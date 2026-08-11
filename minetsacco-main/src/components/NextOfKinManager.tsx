import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Trash2, Plus, AlertCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import api from '@/config/api';

interface NextOfKin {
  id?: number;
  fullName: string;
  relationship: string;
  phone: string;
  email?: string;
  idNumber?: string;
  percentage: number;
  isPrimary: boolean;
}

interface NextOfKinManagerProps {
  memberId: number;
  onSave?: () => void;
}

export function NextOfKinManager({ memberId, onSave }: NextOfKinManagerProps) {
  const { toast } = useToast();
  const [nextOfKinList, setNextOfKinList] = useState<NextOfKin[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [totalPercentage, setTotalPercentage] = useState(0);

  useEffect(() => {
    if (memberId) {
      fetchNextOfKin();
    }
  }, [memberId]);

  useEffect(() => {
    const total = nextOfKinList.reduce((sum, nok) => sum + Number(nok.percentage || 0), 0);
    setTotalPercentage(total);
  }, [nextOfKinList]);

  const fetchNextOfKin = async () => {
    setLoading(true);
    try {
      console.log('Fetching next of kin for member:', memberId);
      const response = await api.get(`/next-of-kin/member/${memberId}`);
      console.log('Next of kin response:', response.data);
      
      if (response.data.nextOfKin && response.data.nextOfKin.length > 0) {
        setNextOfKinList(response.data.nextOfKin);
      } else {
        // Initialize with one empty entry
        setNextOfKinList([createEmptyNextOfKin()]);
      }
    } catch (error: any) {
      console.error('Error fetching next of kin:', error);
      console.error('Error response:', error.response?.data);
      
      // Initialize with one empty entry on error
      setNextOfKinList([createEmptyNextOfKin()]);
      
      // Only show error if it's not a 404 (which means no data exists yet)
      if (error.response?.status !== 404) {
        toast({
          title: 'Error',
          description: 'Failed to load next of kin data',
          variant: 'destructive',
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const createEmptyNextOfKin = (): NextOfKin => ({
    fullName: '',
    relationship: '',
    phone: '',
    email: '',
    idNumber: '',
    percentage: 0,
    isPrimary: false,
  });

  const addNextOfKin = () => {
    if (totalPercentage >= 100) {
      toast({
        title: 'Cannot Add',
        description: 'Total percentage already at 100%',
        variant: 'destructive',
      });
      return;
    }
    setNextOfKinList([...nextOfKinList, createEmptyNextOfKin()]);
  };

  const removeNextOfKin = (index: number) => {
    if (nextOfKinList.length === 1) {
      toast({
        title: 'Cannot Remove',
        description: 'At least one next of kin is required',
        variant: 'destructive',
      });
      return;
    }
    const updated = nextOfKinList.filter((_, i) => i !== index);
    setNextOfKinList(updated);
  };

  const updateNextOfKin = (index: number, field: keyof NextOfKin, value: any) => {
    const updated = [...nextOfKinList];
    updated[index] = { ...updated[index], [field]: value };
    setNextOfKinList(updated);
  };

  const handleSave = async () => {
    // Validation
    if (nextOfKinList.length === 0) {
      toast({
        title: 'Validation Error',
        description: 'At least one next of kin is required',
        variant: 'destructive',
      });
      return;
    }

    for (const nok of nextOfKinList) {
      if (!nok.fullName || !nok.relationship || !nok.phone) {
        toast({
          title: 'Validation Error',
          description: 'All next of kin must have name, relationship, and phone',
          variant: 'destructive',
        });
        return;
      }
      if (nok.percentage <= 0) {
        toast({
          title: 'Validation Error',
          description: 'All percentages must be greater than 0',
          variant: 'destructive',
        });
        return;
      }
    }

    if (totalPercentage !== 100) {
      toast({
        title: 'Validation Error',
        description: `Total percentage must equal 100%. Current: ${totalPercentage}%`,
        variant: 'destructive',
      });
      return;
    }

    setSaving(true);
    try {
      await api.post(`/next-of-kin/member/${memberId}/bulk`, nextOfKinList);
      toast({
        title: 'Success',
        description: 'Next of kin information saved successfully',
      });
      if (onSave) onSave();
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.response?.data?.error || 'Failed to save next of kin',
        variant: 'destructive',
      });
    } finally {
      setSaving(false);
    }
  };

  const remainingPercentage = 100 - totalPercentage;

  if (loading) {
    return <div className="text-center py-4">Loading...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Next of Kin & Beneficiaries</h3>
        <div className="flex items-center gap-2">
          <span className={`text-sm font-medium ${totalPercentage === 100 ? 'text-green-600' : 'text-orange-600'}`}>
            Total: {totalPercentage}% / 100%
          </span>
          {remainingPercentage > 0 && (
            <span className="text-sm text-muted-foreground">
              ({remainingPercentage}% remaining)
            </span>
          )}
        </div>
      </div>

      {totalPercentage !== 100 && (
        <div className="flex items-center gap-2 p-3 bg-orange-50 border border-orange-200 rounded-md">
          <AlertCircle className="h-4 w-4 text-orange-600" />
          <span className="text-sm text-orange-800">
            Total percentage must equal 100% before saving
          </span>
        </div>
      )}

      <div className="space-y-4">
        {nextOfKinList.map((nok, index) => (
          <Card key={index}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">
                  Beneficiary #{index + 1}
                  {nok.isPrimary && (
                    <span className="ml-2 text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                      Primary
                    </span>
                  )}
                </CardTitle>
                {nextOfKinList.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeNextOfKin(index)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor={`name-${index}`}>Full Name *</Label>
                  <Input
                    id={`name-${index}`}
                    value={nok.fullName}
                    onChange={(e) => updateNextOfKin(index, 'fullName', e.target.value)}
                    placeholder="e.g., John Kamau"
                  />
                </div>
                <div>
                  <Label htmlFor={`relationship-${index}`}>Relationship *</Label>
                  <Input
                    id={`relationship-${index}`}
                    value={nok.relationship}
                    onChange={(e) => updateNextOfKin(index, 'relationship', e.target.value)}
                    placeholder="e.g., Son, Daughter, Spouse"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor={`phone-${index}`}>Phone Number *</Label>
                  <Input
                    id={`phone-${index}`}
                    value={nok.phone}
                    onChange={(e) => updateNextOfKin(index, 'phone', e.target.value)}
                    placeholder="0712345678"
                  />
                </div>
                <div>
                  <Label htmlFor={`percentage-${index}`}>
                    Allocation Percentage * 
                    <span className="text-xs text-muted-foreground ml-1">
                      (How much they inherit)
                    </span>
                  </Label>
                  <div className="flex items-center gap-2">
                    <Input
                      id={`percentage-${index}`}
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      value={nok.percentage}
                      onChange={(e) => updateNextOfKin(index, 'percentage', parseFloat(e.target.value) || 0)}
                      className="flex-1"
                    />
                    <span className="text-sm font-medium">%</span>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor={`email-${index}`}>Email (Optional)</Label>
                  <Input
                    id={`email-${index}`}
                    type="email"
                    value={nok.email || ''}
                    onChange={(e) => updateNextOfKin(index, 'email', e.target.value)}
                    placeholder="email@example.com"
                  />
                </div>
                <div>
                  <Label htmlFor={`id-${index}`}>ID Number (Optional)</Label>
                  <Input
                    id={`id-${index}`}
                    value={nok.idNumber || ''}
                    onChange={(e) => updateNextOfKin(index, 'idNumber', e.target.value)}
                    placeholder="12345678"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id={`primary-${index}`}
                  checked={nok.isPrimary}
                  onChange={(e) => {
                    // Only one can be primary
                    const updated = nextOfKinList.map((n, i) => ({
                      ...n,
                      isPrimary: i === index ? e.target.checked : false,
                    }));
                    setNextOfKinList(updated);
                  }}
                  className="rounded border-gray-300"
                />
                <Label htmlFor={`primary-${index}`} className="text-sm cursor-pointer">
                  Set as primary contact
                </Label>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="flex items-center justify-between pt-4">
        <Button
          type="button"
          variant="outline"
          onClick={addNextOfKin}
          disabled={totalPercentage >= 100}
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Another Beneficiary
        </Button>

        <Button
          onClick={handleSave}
          disabled={saving || totalPercentage !== 100}
        >
          {saving ? 'Saving...' : 'Save Next of Kin'}
        </Button>
      </div>
    </div>
  );
}
