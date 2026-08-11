// ADD THIS COMPONENT TO MemberLoanApplication.tsx
// Place it RIGHT BEFORE the submit button in BOTH the loan application form AND the top-up form

// First, add this helper function at the top of the component (after other functions):
const formatPhoneNumber = (value: string): string => {
  let digits = value.replace(/\D/g, '');
  if (digits.startsWith('0')) {
    digits = '254' + digits.substring(1);
  }
  if (digits.startsWith('254')) {
    return '+' + digits.substring(0, 12);
  }
  if (digits.startsWith('7') || digits.startsWith('1')) {
    return '+254' + digits.substring(0, 9);
  }
  return '+254' + digits.substring(0, 9);
};

// ==============================================================================
// FOR LOAN APPLICATION FORM (Apply Tab)
// ==============================================================================
// Add this RIGHT BEFORE: <Button type="submit" className="w-full" disabled={submitting}>
// (Around line 1300)

            {/* Next of Kin as Optional Guarantor */}
            <Card className="border-2 border-dashed border-gray-300">
              <CardHeader>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="useNextOfKinLoan"
                    checked={useNextOfKinGuarantorLoan}
                    onChange={(e) => setUseNextOfKinGuarantorLoan(e.target.checked)}
                    className="h-5 w-5 rounded border-gray-300"
                  />
                  <div className="flex-1">
                    <Label htmlFor="useNextOfKinLoan" className="text-base font-semibold cursor-pointer">
                      Add Next of Kin as Optional Guarantor
                    </Label>
                    <p className="text-sm text-gray-600 mt-1">
                      Optionally provide next of kin details as backup contact for this loan
                    </p>
                  </div>
                </div>
              </CardHeader>
              
              {useNextOfKinGuarantorLoan && (
                <CardContent className="space-y-4 pt-0">
                  <Alert className="bg-blue-50 border-blue-200">
                    <AlertDescription className="text-sm">
                      ℹ️ Next of kin information is optional and will be used as backup contact for loan recovery purposes. They don't need to be a SACCO member.
                    </AlertDescription>
                  </Alert>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Full Name *</Label>
                      <Input
                        value={nextOfKinNameLoan}
                        onChange={(e) => setNextOfKinNameLoan(e.target.value)}
                        placeholder="e.g. Jane Doe"
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label>Phone Number *</Label>
                      <Input
                        value={nextOfKinPhoneLoan}
                        onChange={(e) => {
                          const formatted = formatPhoneNumber(e.target.value);
                          setNextOfKinPhoneLoan(formatted);
                        }}
                        placeholder="+254712345678"
                        maxLength={13}
                      />
                      <p className="text-xs text-gray-500">Format: +254XXXXXXXXX (9 digits)</p>
                    </div>
                    
                    <div className="space-y-2 md:col-span-2">
                      <Label>Relationship *</Label>
                      <Select value={nextOfKinRelationshipLoan} onValueChange={setNextOfKinRelationshipLoan}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select relationship" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="Spouse">Spouse</SelectItem>
                          <SelectItem value="Parent">Parent</SelectItem>
                          <SelectItem value="Sibling">Sibling</SelectItem>
                          <SelectItem value="Child">Child</SelectItem>
                          <SelectItem value="Friend">Friend</SelectItem>
                          <SelectItem value="Other">Other</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </CardContent>
              )}
            </Card>

// ==============================================================================
// FOR TOP-UP FORM (TopUp Tab)
// ==============================================================================
// Add this RIGHT BEFORE: <Button type="submit" ...> (the top-up submit button)
// (Around line 1530)

            {/* Next of Kin as Optional Guarantor */}
            <Card className="border-2 border-dashed border-gray-300">
              <CardHeader>
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    id="useNextOfKinTopup"
                    checked={useNextOfKinGuarantor}
                    onChange={(e) => setUseNextOfKinGuarantor(e.target.checked)}
                    className="h-5 w-5 rounded border-gray-300"
                  />
                  <div className="flex-1">
                    <Label htmlFor="useNextOfKinTopup" className="text-base font-semibold cursor-pointer">
                      Add Next of Kin as Optional Guarantor
                    </Label>
                    <p className="text-sm text-gray-600 mt-1">
                      Optionally provide next of kin details as backup contact for this top-up
                    </p>
                  </div>
                </div>
              </CardHeader>
              
              {useNextOfKinGuarantor && (
                <CardContent className="space-y-4 pt-0">
                  <Alert className="bg-blue-50 border-blue-200">
                    <AlertDescription className="text-sm">
                      ℹ️ Next of kin information is optional and will be used as backup contact. They don't need to be a SACCO member.
                    </AlertDescription>
                  </Alert>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Full Name *</Label>
                      <Input
                        value={nextOfKinName}
                        onChange={(e) => setNextOfKinName(e.target.value)}
                        placeholder="e.g. Jane Doe"
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label>Phone Number *</Label>
                      <Input
                        value={nextOfKinPhone}
                        onChange={(e) => {
                          const formatted = formatPhoneNumber(e.target.value);
                          setNextOfKinPhone(formatted);
                        }}
                        placeholder="+254712345678"
                        maxLength={13}
                      />
                      <p className="text-xs text-gray-500">Format: +254XXXXXXXXX (9 digits)</p>
                    </div>
                    
                    <div className="space-y-2 md:col-span-2">
                      <Label>Relationship *</Label>
                      <Select value={nextOfKinRelationship} onValueChange={setNextOfKinRelationship}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select relationship" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="Spouse">Spouse</SelectItem>
                          <SelectItem value="Parent">Parent</SelectItem>
                          <SelectItem value="Sibling">Sibling</SelectItem>
                          <SelectItem value="Child">Child</SelectItem>
                          <SelectItem value="Friend">Friend</SelectItem>
                          <SelectItem value="Other">Other</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </CardContent>
              )}
            </Card>

// ==============================================================================
// SCROLLING ISSUE FIX
// ==============================================================================
// The white space scrolling issue is likely caused by the MemberLayout having
// excessive height. Check the MemberLayout component and ensure it has:
//
// 1. Remove any min-height: 100vh from the main container
// 2. Add overflow-x: hidden to prevent horizontal scroll
// 3. Ensure the form container has proper max-width and padding
//
// Add this to the root div in MemberLayout:
// <div className="min-h-screen bg-gray-50">  // Remove if min-h-screen is causing issues
//
// Or wrap the content in:
// <div className="h-full overflow-x-hidden">
