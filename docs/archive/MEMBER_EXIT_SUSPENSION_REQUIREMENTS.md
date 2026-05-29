# Member Exit & Suspension System Requirements

## **Business Requirements**

### **Role-Based Workflow**
- **Credit Committee** (acting as HR): Initiates member suspension & exit
- **Treasurer**: Approves member exit & validates suspension  
- **Admin**: View-only access to reports (cannot initiate actions)

**Reasoning for Credit Committee as HR:**
In this company system, the actual HR person will be assigned the CREDIT COMMITTEE role in the sacco system. When a member wants to leave the company or retire, the HR department is the one who has knowledge of this information. Therefore, the HR person (acting as CREDIT COMMITTEE) should initiate member exit and suspension actions. The Treasurer then approves these actions to ensure proper audit trail and financial oversight.

### **Employee ID Format**
- System must accept text-based employee IDs like "EMP001"
- Not numeric-only IDs

### **Security Requirements**
- Suspended members cannot login to member portal
- Exited members cannot login to member portal (after Treasurer approval)
- Complete audit trail for all suspension/exit actions

### **Member Reactivation**
- Credit Committee can reactivate suspended members
- Treasury cannot block reactivation

### **Confirmation Dialogs**
- Permanent actions (member exit) require confirmation before execution

## **Technical Implementation**

### **Backend Changes Required**
1. **Controllers**: Accept String employee ID, convert to Long member ID
2. **Services**: Handle Credit Committee initiation, Treasurer approval
3. **Entities**: Add validation/approval fields
4. **Authentication**: Block suspended/exited members from login
5. **Audit**: Log all actions with proper role attribution

### **Frontend Changes Required**
1. **Input Fields**: Accept text employee IDs (EMP001)
2. **Permission Checks**: Show/hide based on role
3. **Confirmation Dialogs**: Before permanent actions
4. **Reactivation UI**: For Credit Committee

### **Database Schema Updates**
- MemberSuspension: Add validation fields (validatedBy, validationNotes, validatedAt)
- MemberExit: Add approval fields (approvalNotes, status field)
