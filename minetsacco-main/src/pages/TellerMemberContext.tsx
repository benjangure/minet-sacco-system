import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useToast } from "@/hooks/use-toast";
import { Search, LogOut, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Member {
  id: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
}

interface MemberContext {
  id: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
}

const TellerMemberContext = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [selectedMember, setSelectedMember] = useState<MemberContext | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [memberSearchOpen, setMemberSearchOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const { session, role } = useAuth();

  const isTeller = role === "TELLER";

  useEffect(() => {
    if (!isTeller) return;
    fetchMembers();
    fetchCurrentContext();
  }, [session]);

  const fetchMembers = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/members`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setMembers(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching members:", error);
    }
  };

  const fetchCurrentContext = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/teller/current-member-context`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        if (data.data) {
          setSelectedMember(data.data);
        }
      }
    } catch (error) {
      console.error("Error fetching context:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectMember = async (member: Member) => {
    try {
      const response = await fetch(`${API_BASE_URL}/teller/set-member-context/${member.id}`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        setSelectedMember({
          id: member.id,
          memberNumber: member.memberNumber,
          firstName: member.firstName,
          lastName: member.lastName,
        });
        setMemberSearchOpen(false);
        toast({ title: "Success", description: "Member context set successfully" });
      } else {
        toast({ title: "Error", description: "Failed to set member context", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to set member context", variant: "destructive" });
    }
  };

  const handleClearContext = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/teller/clear-member-context`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${session?.token}` },
      });

      if (response.ok) {
        setSelectedMember(null);
        toast({ title: "Success", description: "Member context cleared" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to clear context", variant: "destructive" });
    }
  };

  const filteredMembers = members.filter(m =>
    m.memberNumber.toLowerCase().includes(searchInput.toLowerCase()) ||
    `${m.firstName} ${m.lastName}`.toLowerCase().includes(searchInput.toLowerCase())
  );

  if (!isTeller) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only Teller can access member context.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground">Member Context</h1>
        <p className="text-muted-foreground">Select a member to work with</p>
      </div>

      {/* Member Selection Dialog */}
      <Dialog open={memberSearchOpen} onOpenChange={setMemberSearchOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Select Member</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by member number or name..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="pl-10"
              />
            </div>
            <div className="max-h-96 overflow-y-auto border rounded-lg">
              {filteredMembers.length === 0 ? (
                <div className="p-4 text-center text-muted-foreground">No members found</div>
              ) : (
                filteredMembers.map(member => (
                  <div
                    key={member.id}
                    onClick={() => handleSelectMember(member)}
                    className="p-3 border-b hover:bg-accent cursor-pointer transition-colors"
                  >
                    <div className="font-medium">{member.firstName} {member.lastName}</div>
                    <div className="text-sm text-muted-foreground">
                      Member #{member.memberNumber}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Current Context Display */}
      {selectedMember ? (
        <Card className="border-green-200 bg-green-50">
          <CardHeader>
            <CardTitle className="text-base">Current Member Context</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">Member</p>
                <p className="text-lg font-semibold">{selectedMember.firstName} {selectedMember.lastName}</p>
                <p className="text-sm text-muted-foreground">#{selectedMember.memberNumber}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setMemberSearchOpen(true)}>
                  Change Member
                </Button>
                <Button variant="destructive" onClick={handleClearContext}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Clear Context
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            No member selected. Click the button below to select a member to work with.
          </AlertDescription>
        </Alert>
      )}

      <div className="mt-6">
        <Button onClick={() => setMemberSearchOpen(true)} className="w-full md:w-auto">
          <Search className="mr-2 h-4 w-4" />
          {selectedMember ? "Change Member" : "Select Member"}
        </Button>
      </div>

      {selectedMember && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-base">Available Actions</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              You can now access the following for {selectedMember.firstName}:
            </p>
            <ul className="space-y-2 text-sm">
              <li>✓ View member profile and account details</li>
              <li>✓ View transaction history</li>
              <li>✓ Record deposits and withdrawals</li>
              <li>✓ View loan applications and status</li>
              <li>✓ Record loan repayments</li>
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default TellerMemberContext;
