
-- Add created_by to user_roles to track who assigned the role
ALTER TABLE public.user_roles ADD COLUMN IF NOT EXISTS created_by uuid REFERENCES auth.users(id);

-- Add created_by to profiles to track who created the staff user
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS created_by uuid REFERENCES auth.users(id);

-- Create a function to check role hierarchy - who can create whom
CREATE OR REPLACE FUNCTION public.can_manage_role(_manager_id uuid, _target_role app_role)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT CASE
    -- Admin can create: treasurer, loan_officer, credit_committee, auditor
    WHEN has_role(_manager_id, 'admin') AND _target_role IN ('treasurer', 'loan_officer', 'credit_committee', 'auditor') THEN true
    -- Treasurer can create: teller, helpdesk
    WHEN has_role(_manager_id, 'treasurer') AND _target_role IN ('teller', 'helpdesk') THEN true
    ELSE false
  END
$$;

-- Create function to check if user created a specific user_role record
CREATE OR REPLACE FUNCTION public.is_creator(_creator_id uuid, _target_user_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles
    WHERE user_id = _target_user_id AND created_by = _creator_id
  )
$$;

-- Drop old user_roles policies
DROP POLICY IF EXISTS "Admins can manage all roles" ON public.user_roles;
DROP POLICY IF EXISTS "Users can view their own role" ON public.user_roles;

-- New policies for user_roles
-- Everyone authenticated can view roles (needed for sidebar display)
CREATE POLICY "Authenticated users can view roles"
ON public.user_roles FOR SELECT TO authenticated
USING (true);

-- Insert: only if you can manage the target role
CREATE POLICY "Managers can insert roles"
ON public.user_roles FOR INSERT TO authenticated
WITH CHECK (can_manage_role(auth.uid(), role));

-- Update: only if you created the record or are admin
CREATE POLICY "Managers can update roles they created"
ON public.user_roles FOR UPDATE TO authenticated
USING (created_by = auth.uid() OR has_role(auth.uid(), 'admin'));

-- Delete: only the creator can delete, except admins who can only delete roles they created
CREATE POLICY "Creators can delete roles"
ON public.user_roles FOR DELETE TO authenticated
USING (created_by = auth.uid());
