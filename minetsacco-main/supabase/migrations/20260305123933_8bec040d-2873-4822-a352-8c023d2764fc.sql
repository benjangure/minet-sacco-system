
-- Fix overly permissive INSERT policies
DROP POLICY "System can insert audit logs" ON public.audit_logs;
CREATE POLICY "Authenticated users can insert audit logs" ON public.audit_logs
  FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

DROP POLICY "System can insert notifications" ON public.notifications;
CREATE POLICY "Staff can insert notifications" ON public.notifications
  FOR INSERT TO authenticated
  WITH CHECK (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'helpdesk')
  );
