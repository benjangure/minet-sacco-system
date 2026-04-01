
-- ============================================
-- MINET SACCO SYSTEM - COMPLETE DATABASE SCHEMA
-- ============================================

-- 1. ROLES ENUM & USER ROLES TABLE
CREATE TYPE public.app_role AS ENUM (
  'admin',
  'treasurer',
  'loan_officer',
  'credit_committee',
  'auditor',
  'teller',
  'helpdesk'
);

CREATE TABLE public.user_roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  role app_role NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, role)
);

ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

-- Security definer function to check roles (avoids RLS recursion)
CREATE OR REPLACE FUNCTION public.has_role(_user_id UUID, _role app_role)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles
    WHERE user_id = _user_id AND role = _role
  )
$$;

-- Function to get user role
CREATE OR REPLACE FUNCTION public.get_user_role(_user_id UUID)
RETURNS app_role
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT role FROM public.user_roles
  WHERE user_id = _user_id
  LIMIT 1
$$;

-- RLS: Admins can manage roles, others can view their own
CREATE POLICY "Admins can manage all roles" ON public.user_roles
  FOR ALL TO authenticated
  USING (public.has_role(auth.uid(), 'admin'));

CREATE POLICY "Users can view their own role" ON public.user_roles
  FOR SELECT TO authenticated
  USING (user_id = auth.uid());

-- 2. PROFILES TABLE
CREATE TABLE public.profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
  full_name TEXT NOT NULL,
  email TEXT,
  phone TEXT,
  avatar_url TEXT,
  department TEXT,
  job_title TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Profiles viewable by authenticated users" ON public.profiles
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can update own profile" ON public.profiles
  FOR UPDATE TO authenticated USING (user_id = auth.uid());

CREATE POLICY "Admins can manage all profiles" ON public.profiles
  FOR ALL TO authenticated USING (public.has_role(auth.uid(), 'admin'));

-- Auto-create profile on signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (user_id, full_name, email)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.email),
    NEW.email
  );
  RETURN NEW;
END;
$$;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 3. MEMBER STATUS ENUM & MEMBERS TABLE
CREATE TYPE public.member_status AS ENUM ('active', 'dormant', 'suspended', 'exited');

CREATE TABLE public.members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_number TEXT NOT NULL UNIQUE,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  email TEXT,
  phone TEXT NOT NULL,
  national_id TEXT NOT NULL UNIQUE,
  date_of_birth DATE,
  gender TEXT,
  marital_status TEXT,
  employer TEXT DEFAULT 'Minet Insurance Brokers',
  department TEXT,
  job_title TEXT,
  employment_number TEXT,
  postal_address TEXT,
  physical_address TEXT,
  county TEXT,
  next_of_kin_name TEXT,
  next_of_kin_phone TEXT,
  next_of_kin_relationship TEXT,
  beneficiary_name TEXT,
  beneficiary_phone TEXT,
  beneficiary_relationship TEXT,
  status member_status NOT NULL DEFAULT 'active',
  membership_fee_paid BOOLEAN NOT NULL DEFAULT false,
  join_date DATE NOT NULL DEFAULT CURRENT_DATE,
  exit_date DATE,
  notes TEXT,
  created_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.members ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view members" ON public.members
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Staff can insert members" ON public.members
  FOR INSERT TO authenticated
  WITH CHECK (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'teller')
  );

CREATE POLICY "Staff can update members" ON public.members
  FOR UPDATE TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'teller')
  );

-- 4. MEMBER ACCOUNTS (SAVINGS)
CREATE TABLE public.accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE NOT NULL UNIQUE,
  share_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
  savings_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
  total_deposits NUMERIC(15,2) NOT NULL DEFAULT 0,
  total_withdrawals NUMERIC(15,2) NOT NULL DEFAULT 0,
  interest_earned NUMERIC(15,2) NOT NULL DEFAULT 0,
  minimum_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view accounts" ON public.accounts
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Treasurer/Admin can manage accounts" ON public.accounts
  FOR ALL TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'teller')
  );

-- 5. TRANSACTIONS
CREATE TYPE public.transaction_type AS ENUM ('deposit', 'withdrawal', 'loan_disbursement', 'loan_repayment', 'interest', 'fee', 'dividend');

CREATE TABLE public.transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE NOT NULL,
  account_id UUID REFERENCES public.accounts(id) ON DELETE CASCADE NOT NULL,
  type transaction_type NOT NULL,
  amount NUMERIC(15,2) NOT NULL,
  running_balance NUMERIC(15,2),
  description TEXT,
  reference_number TEXT,
  payment_method TEXT,
  processed_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view transactions" ON public.transactions
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Staff can insert transactions" ON public.transactions
  FOR INSERT TO authenticated
  WITH CHECK (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'teller')
  );

-- 6. LOAN PRODUCTS
CREATE TABLE public.loan_products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  max_amount NUMERIC(15,2) NOT NULL,
  min_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
  interest_rate NUMERIC(5,2) NOT NULL,
  max_term_months INT NOT NULL,
  min_term_months INT NOT NULL DEFAULT 1,
  requires_guarantors BOOLEAN NOT NULL DEFAULT true,
  min_guarantors INT NOT NULL DEFAULT 2,
  max_multiplier NUMERIC(5,2) NOT NULL DEFAULT 3,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.loan_products ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view loan products" ON public.loan_products
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Admins can manage loan products" ON public.loan_products
  FOR ALL TO authenticated USING (public.has_role(auth.uid(), 'admin'));

-- 7. LOANS
CREATE TYPE public.loan_status AS ENUM ('draft', 'submitted', 'under_review', 'approved', 'rejected', 'disbursed', 'repaying', 'fully_paid', 'defaulted', 'written_off');

CREATE TABLE public.loans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_number TEXT NOT NULL UNIQUE,
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE NOT NULL,
  product_id UUID REFERENCES public.loan_products(id) NOT NULL,
  amount NUMERIC(15,2) NOT NULL,
  interest_rate NUMERIC(5,2) NOT NULL,
  term_months INT NOT NULL,
  monthly_repayment NUMERIC(15,2),
  total_interest NUMERIC(15,2),
  total_repayable NUMERIC(15,2),
  outstanding_balance NUMERIC(15,2),
  amount_paid NUMERIC(15,2) NOT NULL DEFAULT 0,
  status loan_status NOT NULL DEFAULT 'draft',
  purpose TEXT,
  application_date DATE NOT NULL DEFAULT CURRENT_DATE,
  approval_date DATE,
  disbursement_date DATE,
  maturity_date DATE,
  approved_by UUID REFERENCES auth.users(id),
  disbursed_by UUID REFERENCES auth.users(id),
  rejection_reason TEXT,
  notes TEXT,
  created_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.loans ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view loans" ON public.loans
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Loan officers/admin can insert loans" ON public.loans
  FOR INSERT TO authenticated
  WITH CHECK (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'loan_officer') OR
    public.has_role(auth.uid(), 'teller')
  );

CREATE POLICY "Staff can update loans" ON public.loans
  FOR UPDATE TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'loan_officer') OR
    public.has_role(auth.uid(), 'credit_committee')
  );

-- 8. GUARANTORS
CREATE TYPE public.guarantor_status AS ENUM ('pending', 'accepted', 'rejected');

CREATE TABLE public.guarantors (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES public.loans(id) ON DELETE CASCADE NOT NULL,
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE NOT NULL,
  amount_guaranteed NUMERIC(15,2) NOT NULL,
  status guarantor_status NOT NULL DEFAULT 'pending',
  response_date DATE,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.guarantors ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view guarantors" ON public.guarantors
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Staff can manage guarantors" ON public.guarantors
  FOR ALL TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'loan_officer')
  );

-- 9. LOAN REPAYMENTS
CREATE TABLE public.loan_repayments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES public.loans(id) ON DELETE CASCADE NOT NULL,
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE NOT NULL,
  amount NUMERIC(15,2) NOT NULL,
  principal_amount NUMERIC(15,2),
  interest_amount NUMERIC(15,2),
  penalty_amount NUMERIC(15,2) DEFAULT 0,
  payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
  payment_method TEXT,
  reference_number TEXT,
  running_balance NUMERIC(15,2),
  processed_by UUID REFERENCES auth.users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.loan_repayments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view repayments" ON public.loan_repayments
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "Staff can insert repayments" ON public.loan_repayments
  FOR INSERT TO authenticated
  WITH CHECK (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'treasurer') OR
    public.has_role(auth.uid(), 'teller')
  );

-- 10. AUDIT LOGS
CREATE TABLE public.audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id),
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT,
  old_values JSONB,
  new_values JSONB,
  ip_address TEXT,
  user_agent TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins and auditors can view audit logs" ON public.audit_logs
  FOR SELECT TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'auditor')
  );

CREATE POLICY "System can insert audit logs" ON public.audit_logs
  FOR INSERT TO authenticated WITH CHECK (true);

-- 11. NOTIFICATIONS
CREATE TYPE public.notification_channel AS ENUM ('sms', 'email', 'push', 'in_app');
CREATE TYPE public.notification_status AS ENUM ('pending', 'sent', 'failed');

CREATE TABLE public.notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id UUID REFERENCES public.members(id) ON DELETE CASCADE,
  channel notification_channel NOT NULL,
  status notification_status NOT NULL DEFAULT 'pending',
  subject TEXT,
  message TEXT NOT NULL,
  recipient TEXT NOT NULL,
  sent_at TIMESTAMPTZ,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins can view notifications" ON public.notifications
  FOR SELECT TO authenticated
  USING (
    public.has_role(auth.uid(), 'admin') OR
    public.has_role(auth.uid(), 'helpdesk')
  );

CREATE POLICY "System can insert notifications" ON public.notifications
  FOR INSERT TO authenticated WITH CHECK (true);

-- 12. UPDATED_AT TRIGGER FUNCTION
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- Apply updated_at triggers
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_members_updated_at BEFORE UPDATE ON public.members FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON public.accounts FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_loan_products_updated_at BEFORE UPDATE ON public.loan_products FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_loans_updated_at BEFORE UPDATE ON public.loans FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- 13. AUTO-CREATE ACCOUNT FOR NEW MEMBERS
CREATE OR REPLACE FUNCTION public.handle_new_member()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.accounts (member_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$;

CREATE TRIGGER on_member_created
  AFTER INSERT ON public.members
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_member();

-- 14. INDEXES FOR PERFORMANCE
CREATE INDEX idx_members_status ON public.members(status);
CREATE INDEX idx_members_national_id ON public.members(national_id);
CREATE INDEX idx_members_member_number ON public.members(member_number);
CREATE INDEX idx_loans_member_id ON public.loans(member_id);
CREATE INDEX idx_loans_status ON public.loans(status);
CREATE INDEX idx_transactions_member_id ON public.transactions(member_id);
CREATE INDEX idx_transactions_type ON public.transactions(type);
CREATE INDEX idx_audit_logs_user_id ON public.audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity_type ON public.audit_logs(entity_type);
CREATE INDEX idx_loan_repayments_loan_id ON public.loan_repayments(loan_id);

-- 15. SEED LOAN PRODUCTS
INSERT INTO public.loan_products (name, description, max_amount, min_amount, interest_rate, max_term_months, min_term_months, requires_guarantors, min_guarantors, max_multiplier) VALUES
  ('Development Loan', 'General purpose development loan for members. Up to 3x shares.', 3000000, 10000, 12.00, 48, 6, true, 2, 3.0),
  ('Emergency Loan', 'Quick emergency loan with fast approval. Up to 1x shares.', 500000, 5000, 12.00, 12, 1, false, 0, 1.0),
  ('School Fees Loan', 'Education loan for school fees and related expenses.', 1000000, 10000, 10.00, 12, 3, true, 1, 2.0),
  ('Asset Loan', 'Loan for purchase of assets like vehicles, electronics, etc.', 2000000, 20000, 14.00, 36, 6, true, 2, 2.5);

-- 16. MEMBER NUMBER SEQUENCE
CREATE SEQUENCE public.member_number_seq START 1001;

CREATE OR REPLACE FUNCTION public.generate_member_number()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.member_number IS NULL OR NEW.member_number = '' THEN
    NEW.member_number := 'MNT-' || LPAD(nextval('public.member_number_seq')::TEXT, 5, '0');
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER set_member_number
  BEFORE INSERT ON public.members
  FOR EACH ROW EXECUTE FUNCTION public.generate_member_number();

-- 17. LOAN NUMBER SEQUENCE
CREATE SEQUENCE public.loan_number_seq START 1001;

CREATE OR REPLACE FUNCTION public.generate_loan_number()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.loan_number IS NULL OR NEW.loan_number = '' THEN
    NEW.loan_number := 'LN-' || LPAD(nextval('public.loan_number_seq')::TEXT, 6, '0');
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER set_loan_number
  BEFORE INSERT ON public.loans
  FOR EACH ROW EXECUTE FUNCTION public.generate_loan_number();

-- 18. STORAGE BUCKET FOR MEMBER DOCUMENTS
INSERT INTO storage.buckets (id, name, public) VALUES ('member-documents', 'member-documents', false);

CREATE POLICY "Authenticated users can view member documents" ON storage.objects
  FOR SELECT TO authenticated USING (bucket_id = 'member-documents');

CREATE POLICY "Staff can upload member documents" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'member-documents');
