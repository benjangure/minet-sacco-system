# Member Password Change - 400 Bad Request Error

## Problem
When a member tries to change their password, they get a **400 Bad Request** error:
```
Error: User account is not linked to a member. Please contact support. (User ID: 22)
```

## Root Cause
The member's user account in the database doesn't have a `memberId` field set. This field is required to link the user account to their member record.

**Database Issue:**
- User ID: 22 exists in the `users` table
- But `users.member_id` is NULL
- The system can't find which member this user belongs to

## Solution

### Option 1: Fix via Database (Direct SQL)
Run this SQL query to link the user to their member:

```sql
-- First, find the member ID for this user
SELECT u.id, u.username, m.id as member_id, m.member_number, m.first_name, m.last_name
FROM users u
LEFT JOIN members m ON u.member_id = m.id
WHERE u.id = 22;

-- If member_id is NULL, find the correct member and update:
UPDATE users 
SET member_id = (SELECT id FROM members WHERE member_number = 'MEMBER_NUMBER_HERE')
WHERE id = 22;
```

**Steps:**
1. Open your database client (MySQL, PostgreSQL, etc.)
2. Find the member number for this user
3. Run the UPDATE query with the correct member number
4. Try changing password again

### Option 2: Fix via Admin Panel (If Available)
1. Log in as Admin
2. Go to User Management
3. Find User ID 22
4. Link them to their member record
5. Save changes

### Option 3: Recreate User Account
If the user account is corrupted:
1. Delete the user account (User ID: 22)
2. Create a new member user account through the proper flow
3. Ensure `member_id` is set during creation

## How to Prevent This

When creating member user accounts, ensure:
1. **User is created with `member_id` set** to the member's ID
2. **Member record exists first** before creating the user
3. **Validation** checks that `member_id` is not NULL

## Database Schema
The `users` table should have:
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role ENUM('ADMIN', 'TREASURER', 'MEMBER', ...),
    member_id BIGINT,  -- ← This must be set for members
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
);
```

## Verification
After fixing, verify the user is linked:

```sql
SELECT u.id, u.username, u.member_id, m.member_number, m.first_name
FROM users u
JOIN members m ON u.member_id = m.id
WHERE u.id = 22;
```

Should return a row with all fields populated (not NULL).

## Testing
After fixing:
1. Log out completely
2. Log back in as the member
3. Go to Settings → Security
4. Try changing password again
5. Should work without errors

## Related Code
- Backend: `MemberPortalController.changeMemberPassword()` (line 1956)
- Method: `getCurrentMember()` (line 70)
- Check: `if (user.getMemberId() == null)` (line 82)

---

**Status**: ⚠️ Data issue - User account not linked to member record. Requires database fix.
