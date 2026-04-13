import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { Search, Plus, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const API_BASE_URL = "http://localhost:8080/api";

interface Member {
  id: number;
  memberNumber: string;
  firstName: string;
  lastName: string;
}

interface SupportTicket {
  id: number;
  member: Member;
  subject: string;
  description: string;
  status: string;
  priority: string;
  resolution?: string;
  createdAt: string;
  resolvedAt?: string;
}

const CustomerSupportPortal = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [memberSearchOpen, setMemberSearchOpen] = useState(false);
  const [ticketDialogOpen, setTicketDialogOpen] = useState(false);
  const [ticketForm, setTicketForm] = useState({
    subject: "",
    description: "",
    priority: "MEDIUM",
  });
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  const { session, role } = useAuth();

  const isCustomerSupport = role === "CUSTOMER_SUPPORT";

  useEffect(() => {
    if (!isCustomerSupport) return;
    fetchMembers();
    fetchMyTickets();
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

  const fetchMyTickets = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/support/tickets/my-tickets`, {
        headers: { "Authorization": `Bearer ${session?.token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setTickets(data.data || []);
      }
    } catch (error) {
      console.error("Error fetching tickets:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectMember = (member: Member) => {
    setSelectedMember(member);
    setMemberSearchOpen(false);
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMember) {
      toast({ title: "Error", description: "Please select a member", variant: "destructive" });
      return;
    }

    if (!ticketForm.subject || !ticketForm.description) {
      toast({ title: "Error", description: "Please fill in all fields", variant: "destructive" });
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/support/tickets`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${session?.token}`,
        },
        body: JSON.stringify({
          memberId: selectedMember.id,
          subject: ticketForm.subject,
          description: ticketForm.description,
          priority: ticketForm.priority,
        }),
      });

      if (response.ok) {
        toast({ title: "Success", description: "Support ticket created" });
        setTicketDialogOpen(false);
        setTicketForm({ subject: "", description: "", priority: "MEDIUM" });
        setSelectedMember(null);
        fetchMyTickets();
      } else {
        const error = await response.json();
        toast({ title: "Error", description: error.message || "Failed to create ticket", variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "Error", description: "Failed to create ticket", variant: "destructive" });
    } finally {
      setSubmitting(false);
    }
  };

  const filteredMembers = members.filter(m =>
    m.memberNumber.toLowerCase().includes(searchInput.toLowerCase()) ||
    `${m.firstName} ${m.lastName}`.toLowerCase().includes(searchInput.toLowerCase())
  );

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case "URGENT": return "bg-red-100 text-red-800";
      case "HIGH": return "bg-orange-100 text-orange-800";
      case "MEDIUM": return "bg-yellow-100 text-yellow-800";
      case "LOW": return "bg-green-100 text-green-800";
      default: return "bg-gray-100 text-gray-800";
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "OPEN": return "bg-blue-100 text-blue-800";
      case "IN_PROGRESS": return "bg-purple-100 text-purple-800";
      case "RESOLVED": return "bg-green-100 text-green-800";
      case "CLOSED": return "bg-gray-100 text-gray-800";
      default: return "bg-gray-100 text-gray-800";
    }
  };

  if (!isCustomerSupport) {
    return (
      <div className="flex items-center justify-center h-96">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <AlertCircle className="h-12 w-12 mx-auto mb-4 text-amber-500" />
            <h2 className="text-lg font-semibold mb-2">Access Restricted</h2>
            <p className="text-muted-foreground">Only Customer Support can access this portal.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground">Customer Support Portal</h1>
        <p className="text-muted-foreground">Create and manage support tickets for members</p>
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

      {/* Create Ticket Dialog */}
      <Dialog open={ticketDialogOpen} onOpenChange={setTicketDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Create Support Ticket</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreateTicket} className="space-y-4">
            <div>
              <Label className="text-xs">Member *</Label>
              <Button
                type="button"
                variant="outline"
                onClick={() => setMemberSearchOpen(true)}
                className="w-full justify-start text-left"
              >
                {selectedMember ? `${selectedMember.firstName} ${selectedMember.lastName}` : "Select member..."}
              </Button>
            </div>

            <div>
              <Label className="text-xs">Subject *</Label>
              <Input
                value={ticketForm.subject}
                onChange={(e) => setTicketForm({ ...ticketForm, subject: e.target.value })}
                placeholder="Issue subject"
                className="text-sm"
                required
              />
            </div>

            <div>
              <Label className="text-xs">Description *</Label>
              <Textarea
                value={ticketForm.description}
                onChange={(e) => setTicketForm({ ...ticketForm, description: e.target.value })}
                placeholder="Describe the issue..."
                className="text-sm"
                required
              />
            </div>

            <div>
              <Label className="text-xs">Priority</Label>
              <Select value={ticketForm.priority} onValueChange={(value) => setTicketForm({ ...ticketForm, priority: value })}>
                <SelectTrigger className="text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LOW">Low</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HIGH">High</SelectItem>
                  <SelectItem value="URGENT">Urgent</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex gap-2 pt-4">
              <Button type="button" variant="outline" onClick={() => setTicketDialogOpen(false)} className="flex-1">
                Cancel
              </Button>
              <Button type="submit" disabled={submitting} className="flex-1">
                {submitting ? "Creating..." : "Create Ticket"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Create Ticket Button */}
      <div className="mb-6">
        <Button onClick={() => setTicketDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create Support Ticket
        </Button>
      </div>

      {/* My Tickets */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">My Support Tickets ({tickets.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Loading...</div>
          ) : tickets.length === 0 ? (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>No support tickets created yet</AlertDescription>
            </Alert>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Member</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Priority</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {tickets.map(ticket => (
                    <TableRow key={ticket.id}>
                      <TableCell className="text-sm">
                        {ticket.member.firstName} {ticket.member.lastName}
                      </TableCell>
                      <TableCell className="text-sm">{ticket.subject}</TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(ticket.status)}>
                          {ticket.status.replace(/_/g, " ")}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={getPriorityColor(ticket.priority)}>
                          {ticket.priority}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm">
                        {new Date(ticket.createdAt).toLocaleDateString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default CustomerSupportPortal;
