export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.1"
  }
  public: {
    Tables: {
      accounts: {
        Row: {
          created_at: string
          id: string
          interest_earned: number
          member_id: string
          minimum_balance: number
          savings_balance: number
          share_balance: number
          total_deposits: number
          total_withdrawals: number
          updated_at: string
        }
        Insert: {
          created_at?: string
          id?: string
          interest_earned?: number
          member_id: string
          minimum_balance?: number
          savings_balance?: number
          share_balance?: number
          total_deposits?: number
          total_withdrawals?: number
          updated_at?: string
        }
        Update: {
          created_at?: string
          id?: string
          interest_earned?: number
          member_id?: string
          minimum_balance?: number
          savings_balance?: number
          share_balance?: number
          total_deposits?: number
          total_withdrawals?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "accounts_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: true
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
        ]
      }
      audit_logs: {
        Row: {
          action: string
          created_at: string
          entity_id: string | null
          entity_type: string
          id: string
          ip_address: string | null
          new_values: Json | null
          old_values: Json | null
          user_agent: string | null
          user_id: string | null
        }
        Insert: {
          action: string
          created_at?: string
          entity_id?: string | null
          entity_type: string
          id?: string
          ip_address?: string | null
          new_values?: Json | null
          old_values?: Json | null
          user_agent?: string | null
          user_id?: string | null
        }
        Update: {
          action?: string
          created_at?: string
          entity_id?: string | null
          entity_type?: string
          id?: string
          ip_address?: string | null
          new_values?: Json | null
          old_values?: Json | null
          user_agent?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      guarantors: {
        Row: {
          amount_guaranteed: number
          created_at: string
          id: string
          loan_id: string
          member_id: string
          notes: string | null
          response_date: string | null
          status: Database["public"]["Enums"]["guarantor_status"]
        }
        Insert: {
          amount_guaranteed: number
          created_at?: string
          id?: string
          loan_id: string
          member_id: string
          notes?: string | null
          response_date?: string | null
          status?: Database["public"]["Enums"]["guarantor_status"]
        }
        Update: {
          amount_guaranteed?: number
          created_at?: string
          id?: string
          loan_id?: string
          member_id?: string
          notes?: string | null
          response_date?: string | null
          status?: Database["public"]["Enums"]["guarantor_status"]
        }
        Relationships: [
          {
            foreignKeyName: "guarantors_loan_id_fkey"
            columns: ["loan_id"]
            isOneToOne: false
            referencedRelation: "loans"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "guarantors_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: false
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
        ]
      }
      loan_products: {
        Row: {
          created_at: string
          description: string | null
          id: string
          interest_rate: number
          is_active: boolean
          max_amount: number
          max_multiplier: number
          max_term_months: number
          min_amount: number
          min_guarantors: number
          min_term_months: number
          name: string
          requires_guarantors: boolean
          updated_at: string
        }
        Insert: {
          created_at?: string
          description?: string | null
          id?: string
          interest_rate: number
          is_active?: boolean
          max_amount: number
          max_multiplier?: number
          max_term_months: number
          min_amount?: number
          min_guarantors?: number
          min_term_months?: number
          name: string
          requires_guarantors?: boolean
          updated_at?: string
        }
        Update: {
          created_at?: string
          description?: string | null
          id?: string
          interest_rate?: number
          is_active?: boolean
          max_amount?: number
          max_multiplier?: number
          max_term_months?: number
          min_amount?: number
          min_guarantors?: number
          min_term_months?: number
          name?: string
          requires_guarantors?: boolean
          updated_at?: string
        }
        Relationships: []
      }
      loan_repayments: {
        Row: {
          amount: number
          created_at: string
          id: string
          interest_amount: number | null
          loan_id: string
          member_id: string
          payment_date: string
          payment_method: string | null
          penalty_amount: number | null
          principal_amount: number | null
          processed_by: string | null
          reference_number: string | null
          running_balance: number | null
        }
        Insert: {
          amount: number
          created_at?: string
          id?: string
          interest_amount?: number | null
          loan_id: string
          member_id: string
          payment_date?: string
          payment_method?: string | null
          penalty_amount?: number | null
          principal_amount?: number | null
          processed_by?: string | null
          reference_number?: string | null
          running_balance?: number | null
        }
        Update: {
          amount?: number
          created_at?: string
          id?: string
          interest_amount?: number | null
          loan_id?: string
          member_id?: string
          payment_date?: string
          payment_method?: string | null
          penalty_amount?: number | null
          principal_amount?: number | null
          processed_by?: string | null
          reference_number?: string | null
          running_balance?: number | null
        }
        Relationships: [
          {
            foreignKeyName: "loan_repayments_loan_id_fkey"
            columns: ["loan_id"]
            isOneToOne: false
            referencedRelation: "loans"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "loan_repayments_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: false
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
        ]
      }
      loans: {
        Row: {
          amount: number
          amount_paid: number
          application_date: string
          approval_date: string | null
          approved_by: string | null
          created_at: string
          created_by: string | null
          disbursed_by: string | null
          disbursement_date: string | null
          id: string
          interest_rate: number
          loan_number: string
          maturity_date: string | null
          member_id: string
          monthly_repayment: number | null
          notes: string | null
          outstanding_balance: number | null
          product_id: string
          purpose: string | null
          rejection_reason: string | null
          status: Database["public"]["Enums"]["loan_status"]
          term_months: number
          total_interest: number | null
          total_repayable: number | null
          updated_at: string
        }
        Insert: {
          amount: number
          amount_paid?: number
          application_date?: string
          approval_date?: string | null
          approved_by?: string | null
          created_at?: string
          created_by?: string | null
          disbursed_by?: string | null
          disbursement_date?: string | null
          id?: string
          interest_rate: number
          loan_number: string
          maturity_date?: string | null
          member_id: string
          monthly_repayment?: number | null
          notes?: string | null
          outstanding_balance?: number | null
          product_id: string
          purpose?: string | null
          rejection_reason?: string | null
          status?: Database["public"]["Enums"]["loan_status"]
          term_months: number
          total_interest?: number | null
          total_repayable?: number | null
          updated_at?: string
        }
        Update: {
          amount?: number
          amount_paid?: number
          application_date?: string
          approval_date?: string | null
          approved_by?: string | null
          created_at?: string
          created_by?: string | null
          disbursed_by?: string | null
          disbursement_date?: string | null
          id?: string
          interest_rate?: number
          loan_number?: string
          maturity_date?: string | null
          member_id?: string
          monthly_repayment?: number | null
          notes?: string | null
          outstanding_balance?: number | null
          product_id?: string
          purpose?: string | null
          rejection_reason?: string | null
          status?: Database["public"]["Enums"]["loan_status"]
          term_months?: number
          total_interest?: number | null
          total_repayable?: number | null
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "loans_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: false
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "loans_product_id_fkey"
            columns: ["product_id"]
            isOneToOne: false
            referencedRelation: "loan_products"
            referencedColumns: ["id"]
          },
        ]
      }
      members: {
        Row: {
          beneficiary_name: string | null
          beneficiary_phone: string | null
          beneficiary_relationship: string | null
          county: string | null
          created_at: string
          created_by: string | null
          date_of_birth: string | null
          department: string | null
          email: string | null
          employer: string | null
          employment_number: string | null
          exit_date: string | null
          first_name: string
          gender: string | null
          id: string
          job_title: string | null
          join_date: string
          last_name: string
          marital_status: string | null
          member_number: string
          membership_fee_paid: boolean
          national_id: string
          next_of_kin_name: string | null
          next_of_kin_phone: string | null
          next_of_kin_relationship: string | null
          notes: string | null
          phone: string
          physical_address: string | null
          postal_address: string | null
          status: Database["public"]["Enums"]["member_status"]
          updated_at: string
        }
        Insert: {
          beneficiary_name?: string | null
          beneficiary_phone?: string | null
          beneficiary_relationship?: string | null
          county?: string | null
          created_at?: string
          created_by?: string | null
          date_of_birth?: string | null
          department?: string | null
          email?: string | null
          employer?: string | null
          employment_number?: string | null
          exit_date?: string | null
          first_name: string
          gender?: string | null
          id?: string
          job_title?: string | null
          join_date?: string
          last_name: string
          marital_status?: string | null
          member_number: string
          membership_fee_paid?: boolean
          national_id: string
          next_of_kin_name?: string | null
          next_of_kin_phone?: string | null
          next_of_kin_relationship?: string | null
          notes?: string | null
          phone: string
          physical_address?: string | null
          postal_address?: string | null
          status?: Database["public"]["Enums"]["member_status"]
          updated_at?: string
        }
        Update: {
          beneficiary_name?: string | null
          beneficiary_phone?: string | null
          beneficiary_relationship?: string | null
          county?: string | null
          created_at?: string
          created_by?: string | null
          date_of_birth?: string | null
          department?: string | null
          email?: string | null
          employer?: string | null
          employment_number?: string | null
          exit_date?: string | null
          first_name?: string
          gender?: string | null
          id?: string
          job_title?: string | null
          join_date?: string
          last_name?: string
          marital_status?: string | null
          member_number?: string
          membership_fee_paid?: boolean
          national_id?: string
          next_of_kin_name?: string | null
          next_of_kin_phone?: string | null
          next_of_kin_relationship?: string | null
          notes?: string | null
          phone?: string
          physical_address?: string | null
          postal_address?: string | null
          status?: Database["public"]["Enums"]["member_status"]
          updated_at?: string
        }
        Relationships: []
      }
      notifications: {
        Row: {
          channel: Database["public"]["Enums"]["notification_channel"]
          created_at: string
          error_message: string | null
          id: string
          member_id: string | null
          message: string
          recipient: string
          sent_at: string | null
          status: Database["public"]["Enums"]["notification_status"]
          subject: string | null
        }
        Insert: {
          channel: Database["public"]["Enums"]["notification_channel"]
          created_at?: string
          error_message?: string | null
          id?: string
          member_id?: string | null
          message: string
          recipient: string
          sent_at?: string | null
          status?: Database["public"]["Enums"]["notification_status"]
          subject?: string | null
        }
        Update: {
          channel?: Database["public"]["Enums"]["notification_channel"]
          created_at?: string
          error_message?: string | null
          id?: string
          member_id?: string | null
          message?: string
          recipient?: string
          sent_at?: string | null
          status?: Database["public"]["Enums"]["notification_status"]
          subject?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "notifications_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: false
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          avatar_url: string | null
          created_at: string
          created_by: string | null
          department: string | null
          email: string | null
          full_name: string
          id: string
          is_active: boolean
          job_title: string | null
          phone: string | null
          updated_at: string
          user_id: string
        }
        Insert: {
          avatar_url?: string | null
          created_at?: string
          created_by?: string | null
          department?: string | null
          email?: string | null
          full_name: string
          id?: string
          is_active?: boolean
          job_title?: string | null
          phone?: string | null
          updated_at?: string
          user_id: string
        }
        Update: {
          avatar_url?: string | null
          created_at?: string
          created_by?: string | null
          department?: string | null
          email?: string | null
          full_name?: string
          id?: string
          is_active?: boolean
          job_title?: string | null
          phone?: string | null
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
      transactions: {
        Row: {
          account_id: string
          amount: number
          created_at: string
          description: string | null
          id: string
          member_id: string
          payment_method: string | null
          processed_by: string | null
          reference_number: string | null
          running_balance: number | null
          type: Database["public"]["Enums"]["transaction_type"]
        }
        Insert: {
          account_id: string
          amount: number
          created_at?: string
          description?: string | null
          id?: string
          member_id: string
          payment_method?: string | null
          processed_by?: string | null
          reference_number?: string | null
          running_balance?: number | null
          type: Database["public"]["Enums"]["transaction_type"]
        }
        Update: {
          account_id?: string
          amount?: number
          created_at?: string
          description?: string | null
          id?: string
          member_id?: string
          payment_method?: string | null
          processed_by?: string | null
          reference_number?: string | null
          running_balance?: number | null
          type?: Database["public"]["Enums"]["transaction_type"]
        }
        Relationships: [
          {
            foreignKeyName: "transactions_account_id_fkey"
            columns: ["account_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "transactions_member_id_fkey"
            columns: ["member_id"]
            isOneToOne: false
            referencedRelation: "members"
            referencedColumns: ["id"]
          },
        ]
      }
      user_roles: {
        Row: {
          created_at: string
          created_by: string | null
          id: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Insert: {
          created_at?: string
          created_by?: string | null
          id?: string
          role: Database["public"]["Enums"]["app_role"]
          user_id: string
        }
        Update: {
          created_at?: string
          created_by?: string | null
          id?: string
          role?: Database["public"]["Enums"]["app_role"]
          user_id?: string
        }
        Relationships: []
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      can_manage_role: {
        Args: {
          _manager_id: string
          _target_role: Database["public"]["Enums"]["app_role"]
        }
        Returns: boolean
      }
      get_user_role: {
        Args: { _user_id: string }
        Returns: Database["public"]["Enums"]["app_role"]
      }
      has_role: {
        Args: {
          _role: Database["public"]["Enums"]["app_role"]
          _user_id: string
        }
        Returns: boolean
      }
      is_creator: {
        Args: { _creator_id: string; _target_user_id: string }
        Returns: boolean
      }
    }
    Enums: {
      app_role:
        | "admin"
        | "treasurer"
        | "loan_officer"
        | "credit_committee"
        | "auditor"
        | "teller"
        | "helpdesk"
      guarantor_status: "pending" | "accepted" | "rejected"
      loan_status:
        | "draft"
        | "submitted"
        | "under_review"
        | "approved"
        | "rejected"
        | "disbursed"
        | "repaying"
        | "fully_paid"
        | "defaulted"
        | "written_off"
      member_status: "active" | "dormant" | "suspended" | "exited"
      notification_channel: "sms" | "email" | "push" | "in_app"
      notification_status: "pending" | "sent" | "failed"
      transaction_type:
        | "deposit"
        | "withdrawal"
        | "loan_disbursement"
        | "loan_repayment"
        | "interest"
        | "fee"
        | "dividend"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      app_role: [
        "admin",
        "treasurer",
        "loan_officer",
        "credit_committee",
        "auditor",
        "teller",
        "helpdesk",
      ],
      guarantor_status: ["pending", "accepted", "rejected"],
      loan_status: [
        "draft",
        "submitted",
        "under_review",
        "approved",
        "rejected",
        "disbursed",
        "repaying",
        "fully_paid",
        "defaulted",
        "written_off",
      ],
      member_status: ["active", "dormant", "suspended", "exited"],
      notification_channel: ["sms", "email", "push", "in_app"],
      notification_status: ["pending", "sent", "failed"],
      transaction_type: [
        "deposit",
        "withdrawal",
        "loan_disbursement",
        "loan_repayment",
        "interest",
        "fee",
        "dividend",
      ],
    },
  },
} as const
