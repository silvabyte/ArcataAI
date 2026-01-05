export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[];

export type Database = {
  graphql_public: {
    Tables: {
      [_ in never]: never;
    };
    Views: {
      [_ in never]: never;
    };
    Functions: {
      graphql: {
        Args: {
          extensions?: Json;
          operationName?: string;
          query?: string;
          variables?: Json;
        };
        Returns: Json;
      };
    };
    Enums: {
      [_ in never]: never;
    };
    CompositeTypes: {
      [_ in never]: never;
    };
  };
  public: {
    Tables: {
      application_answers: {
        Row: {
          answer: string;
          answer_id: number;
          application_id: number | null;
          created_at: string | null;
          job_profile_id: number | null;
          last_used_at: string | null;
          profile_id: string;
          question: string;
          question_hash: string | null;
          source: string | null;
          times_used: number | null;
          updated_at: string | null;
        };
        Insert: {
          answer: string;
          answer_id?: number;
          application_id?: number | null;
          created_at?: string | null;
          job_profile_id?: number | null;
          last_used_at?: string | null;
          profile_id: string;
          question: string;
          question_hash?: string | null;
          source?: string | null;
          times_used?: number | null;
          updated_at?: string | null;
        };
        Update: {
          answer?: string;
          answer_id?: number;
          application_id?: number | null;
          created_at?: string | null;
          job_profile_id?: number | null;
          last_used_at?: string | null;
          profile_id?: string;
          question?: string;
          question_hash?: string | null;
          source?: string | null;
          times_used?: number | null;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "application_answers_application_id_fkey";
            columns: ["application_id"];
            isOneToOne: false;
            referencedRelation: "job_applications";
            referencedColumns: ["application_id"];
          },
          {
            foreignKeyName: "application_answers_job_profile_id_fkey";
            columns: ["job_profile_id"];
            isOneToOne: false;
            referencedRelation: "job_profiles";
            referencedColumns: ["job_profile_id"];
          },
          {
            foreignKeyName: "application_answers_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      application_statuses: {
        Row: {
          color: string | null;
          column_order: number;
          created_at: string | null;
          is_default: boolean | null;
          name: string;
          profile_id: string;
          status_id: number;
          updated_at: string | null;
        };
        Insert: {
          color?: string | null;
          column_order: number;
          created_at?: string | null;
          is_default?: boolean | null;
          name: string;
          profile_id: string;
          status_id?: number;
          updated_at?: string | null;
        };
        Update: {
          color?: string | null;
          column_order?: number;
          created_at?: string | null;
          is_default?: boolean | null;
          name?: string;
          profile_id?: string;
          status_id?: number;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "application_statuses_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      companies: {
        Row: {
          cc_id: string | null;
          company_address: string | null;
          company_address_2: string | null;
          company_city: string | null;
          company_domain: string | null;
          company_id: number;
          company_jobs_source: string | null;
          company_jobs_url: string | null;
          company_linkedin_url: string | null;
          company_naics: string | null;
          company_name: string | null;
          company_phone: string | null;
          company_sic: string | null;
          company_size: string | null;
          company_state: string | null;
          company_zip: string | null;
          created_at: string | null;
          description: string | null;
          employee_count_max: number | null;
          employee_count_min: number | null;
          headquarters: string | null;
          industry: string | null;
          job_board_status: string | null;
          primary_industry: string | null;
          revenue_max: number | null;
          revenue_min: number | null;
          source_company_id: number | null;
          updated_at: string | null;
        };
        Insert: {
          cc_id?: string | null;
          company_address?: string | null;
          company_address_2?: string | null;
          company_city?: string | null;
          company_domain?: string | null;
          company_id?: number;
          company_jobs_source?: string | null;
          company_jobs_url?: string | null;
          company_linkedin_url?: string | null;
          company_naics?: string | null;
          company_name?: string | null;
          company_phone?: string | null;
          company_sic?: string | null;
          company_size?: string | null;
          company_state?: string | null;
          company_zip?: string | null;
          created_at?: string | null;
          description?: string | null;
          employee_count_max?: number | null;
          employee_count_min?: number | null;
          headquarters?: string | null;
          industry?: string | null;
          job_board_status?: string | null;
          primary_industry?: string | null;
          revenue_max?: number | null;
          revenue_min?: number | null;
          source_company_id?: number | null;
          updated_at?: string | null;
        };
        Update: {
          cc_id?: string | null;
          company_address?: string | null;
          company_address_2?: string | null;
          company_city?: string | null;
          company_domain?: string | null;
          company_id?: number;
          company_jobs_source?: string | null;
          company_jobs_url?: string | null;
          company_linkedin_url?: string | null;
          company_naics?: string | null;
          company_name?: string | null;
          company_phone?: string | null;
          company_sic?: string | null;
          company_size?: string | null;
          company_state?: string | null;
          company_zip?: string | null;
          created_at?: string | null;
          description?: string | null;
          employee_count_max?: number | null;
          employee_count_min?: number | null;
          headquarters?: string | null;
          industry?: string | null;
          job_board_status?: string | null;
          primary_industry?: string | null;
          revenue_max?: number | null;
          revenue_min?: number | null;
          source_company_id?: number | null;
          updated_at?: string | null;
        };
        Relationships: [];
      };
      conversations: {
        Row: {
          application_id: number;
          channel: string;
          channel_data: Json | null;
          conversation_id: number;
          created_at: string | null;
          occurred_at: string;
          profile_id: string;
          subject: string | null;
          summary: string | null;
          updated_at: string | null;
        };
        Insert: {
          application_id: number;
          channel: string;
          channel_data?: Json | null;
          conversation_id?: number;
          created_at?: string | null;
          occurred_at: string;
          profile_id: string;
          subject?: string | null;
          summary?: string | null;
          updated_at?: string | null;
        };
        Update: {
          application_id?: number;
          channel?: string;
          channel_data?: Json | null;
          conversation_id?: number;
          created_at?: string | null;
          occurred_at?: string;
          profile_id?: string;
          subject?: string | null;
          summary?: string | null;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "conversations_application_id_fkey";
            columns: ["application_id"];
            isOneToOne: false;
            referencedRelation: "job_applications";
            referencedColumns: ["application_id"];
          },
          {
            foreignKeyName: "conversations_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      cover_letters: {
        Row: {
          content: string;
          cover_letter_id: number;
          created_at: string | null;
          job_profile_id: number | null;
          name: string;
          profile_id: string;
          updated_at: string | null;
        };
        Insert: {
          content?: string;
          cover_letter_id?: number;
          created_at?: string | null;
          job_profile_id?: number | null;
          name: string;
          profile_id: string;
          updated_at?: string | null;
        };
        Update: {
          content?: string;
          cover_letter_id?: number;
          created_at?: string | null;
          job_profile_id?: number | null;
          name?: string;
          profile_id?: string;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "cover_letters_job_profile_id_fkey";
            columns: ["job_profile_id"];
            isOneToOne: false;
            referencedRelation: "job_profiles";
            referencedColumns: ["job_profile_id"];
          },
          {
            foreignKeyName: "cover_letters_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      job_applications: {
        Row: {
          application_date: string | null;
          application_id: number;
          created_at: string | null;
          job_id: number | null;
          job_profile_id: number | null;
          notes: string | null;
          profile_id: string;
          status_id: number | null;
          status_order: number;
          updated_at: string | null;
        };
        Insert: {
          application_date?: string | null;
          application_id?: number;
          created_at?: string | null;
          job_id?: number | null;
          job_profile_id?: number | null;
          notes?: string | null;
          profile_id: string;
          status_id?: number | null;
          status_order?: number;
          updated_at?: string | null;
        };
        Update: {
          application_date?: string | null;
          application_id?: number;
          created_at?: string | null;
          job_id?: number | null;
          job_profile_id?: number | null;
          notes?: string | null;
          profile_id?: string;
          status_id?: number | null;
          status_order?: number;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "job_applications_job_id_fkey";
            columns: ["job_id"];
            isOneToOne: false;
            referencedRelation: "jobs";
            referencedColumns: ["job_id"];
          },
          {
            foreignKeyName: "job_applications_job_profile_id_fkey";
            columns: ["job_profile_id"];
            isOneToOne: false;
            referencedRelation: "job_profiles";
            referencedColumns: ["job_profile_id"];
          },
          {
            foreignKeyName: "job_applications_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
          {
            foreignKeyName: "job_applications_status_id_fkey";
            columns: ["status_id"];
            isOneToOne: false;
            referencedRelation: "application_statuses";
            referencedColumns: ["status_id"];
          },
        ];
      };
      job_profiles: {
        Row: {
          cover_letter_data: Json | null;
          created_at: string | null;
          job_profile_id: number;
          name: string;
          profile_id: string;
          resume_data: Json | null;
          resume_file_id: string | null;
          status: Database["public"]["Enums"]["job_profile_status"];
          updated_at: string | null;
        };
        Insert: {
          cover_letter_data?: Json | null;
          created_at?: string | null;
          job_profile_id?: number;
          name: string;
          profile_id: string;
          resume_data?: Json | null;
          resume_file_id?: string | null;
          status?: Database["public"]["Enums"]["job_profile_status"];
          updated_at?: string | null;
        };
        Update: {
          cover_letter_data?: Json | null;
          created_at?: string | null;
          job_profile_id?: number;
          name?: string;
          profile_id?: string;
          resume_data?: Json | null;
          resume_file_id?: string | null;
          status?: Database["public"]["Enums"]["job_profile_status"];
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "job_profiles_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      job_stream: {
        Row: {
          application_id: number | null;
          best_match_job_profile_id: number | null;
          best_match_score: number | null;
          created_at: string | null;
          job_id: number;
          profile_id: string;
          profile_matches: Json | null;
          source: string;
          status: string | null;
          stream_id: number;
          updated_at: string | null;
        };
        Insert: {
          application_id?: number | null;
          best_match_job_profile_id?: number | null;
          best_match_score?: number | null;
          created_at?: string | null;
          job_id: number;
          profile_id: string;
          profile_matches?: Json | null;
          source: string;
          status?: string | null;
          stream_id?: number;
          updated_at?: string | null;
        };
        Update: {
          application_id?: number | null;
          best_match_job_profile_id?: number | null;
          best_match_score?: number | null;
          created_at?: string | null;
          job_id?: number;
          profile_id?: string;
          profile_matches?: Json | null;
          source?: string;
          status?: string | null;
          stream_id?: number;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "job_stream_application_id_fkey";
            columns: ["application_id"];
            isOneToOne: false;
            referencedRelation: "job_applications";
            referencedColumns: ["application_id"];
          },
          {
            foreignKeyName: "job_stream_best_match_job_profile_id_fkey";
            columns: ["best_match_job_profile_id"];
            isOneToOne: false;
            referencedRelation: "job_profiles";
            referencedColumns: ["job_profile_id"];
          },
          {
            foreignKeyName: "job_stream_job_id_fkey";
            columns: ["job_id"];
            isOneToOne: false;
            referencedRelation: "jobs";
            referencedColumns: ["job_id"];
          },
          {
            foreignKeyName: "job_stream_profile_id_fkey";
            columns: ["profile_id"];
            isOneToOne: false;
            referencedRelation: "profiles";
            referencedColumns: ["id"];
          },
        ];
      };
      jobs: {
        Row: {
          application_url: string | null;
          benefits: string[] | null;
          category: string | null;
          closed_at: string | null;
          closed_reason: string | null;
          closing_date: string | null;
          company_id: number | null;
          completion_state: string | null;
          created_at: string | null;
          description: string | null;
          education_level: string | null;
          experience_level: string | null;
          is_remote: boolean | null;
          job_id: number;
          job_type: string | null;
          last_status_check: string | null;
          location: string | null;
          posted_date: string | null;
          preferred_qualifications: string[] | null;
          qualifications: string[] | null;
          raw_attributes: Json | null;
          responsibilities: string[] | null;
          salary_currency: string | null;
          salary_max: number | null;
          salary_min: number | null;
          source_url: string | null;
          status: string | null;
          title: string;
          updated_at: string | null;
        };
        Insert: {
          application_url?: string | null;
          benefits?: string[] | null;
          category?: string | null;
          closed_at?: string | null;
          closed_reason?: string | null;
          closing_date?: string | null;
          company_id?: number | null;
          completion_state?: string | null;
          created_at?: string | null;
          description?: string | null;
          education_level?: string | null;
          experience_level?: string | null;
          is_remote?: boolean | null;
          job_id?: number;
          job_type?: string | null;
          last_status_check?: string | null;
          location?: string | null;
          posted_date?: string | null;
          preferred_qualifications?: string[] | null;
          qualifications?: string[] | null;
          raw_attributes?: Json | null;
          responsibilities?: string[] | null;
          salary_currency?: string | null;
          salary_max?: number | null;
          salary_min?: number | null;
          source_url?: string | null;
          status?: string | null;
          title: string;
          updated_at?: string | null;
        };
        Update: {
          application_url?: string | null;
          benefits?: string[] | null;
          category?: string | null;
          closed_at?: string | null;
          closed_reason?: string | null;
          closing_date?: string | null;
          company_id?: number | null;
          completion_state?: string | null;
          created_at?: string | null;
          description?: string | null;
          education_level?: string | null;
          experience_level?: string | null;
          is_remote?: boolean | null;
          job_id?: number;
          job_type?: string | null;
          last_status_check?: string | null;
          location?: string | null;
          posted_date?: string | null;
          preferred_qualifications?: string[] | null;
          qualifications?: string[] | null;
          raw_attributes?: Json | null;
          responsibilities?: string[] | null;
          salary_currency?: string | null;
          salary_max?: number | null;
          salary_min?: number | null;
          source_url?: string | null;
          status?: string | null;
          title?: string;
          updated_at?: string | null;
        };
        Relationships: [
          {
            foreignKeyName: "jobs_company_id_fkey";
            columns: ["company_id"];
            isOneToOne: false;
            referencedRelation: "companies";
            referencedColumns: ["company_id"];
          },
        ];
      };
      profiles: {
        Row: {
          avatar_url: string | null;
          created_at: string;
          email: string;
          first_name: string | null;
          full_name: string | null;
          id: string;
          last_name: string | null;
          timezone: string | null;
          updated_at: string;
          username: string | null;
          website: string | null;
        };
        Insert: {
          avatar_url?: string | null;
          created_at?: string;
          email: string;
          first_name?: string | null;
          full_name?: string | null;
          id: string;
          last_name?: string | null;
          timezone?: string | null;
          updated_at?: string;
          username?: string | null;
          website?: string | null;
        };
        Update: {
          avatar_url?: string | null;
          created_at?: string;
          email?: string;
          first_name?: string | null;
          full_name?: string | null;
          id?: string;
          last_name?: string | null;
          timezone?: string | null;
          updated_at?: string;
          username?: string | null;
          website?: string | null;
        };
        Relationships: [];
      };
    };
    Views: {
      [_ in never]: never;
    };
    Functions: {
      [_ in never]: never;
    };
    Enums: {
      job_profile_status: "draft" | "live";
    };
    CompositeTypes: {
      [_ in never]: never;
    };
  };
};

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">;

type DefaultSchema = DatabaseWithoutInternals[Extract<
  keyof Database,
  "public"
>];

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals;
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals;
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R;
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R;
      }
      ? R
      : never
    : never;

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals;
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals;
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I;
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I;
      }
      ? I
      : never
    : never;

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals;
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals;
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U;
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U;
      }
      ? U
      : never
    : never;

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals;
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals;
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never;

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals;
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals;
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never;

export const Constants = {
  graphql_public: {
    Enums: {},
  },
  public: {
    Enums: {
      job_profile_status: ["draft", "live"],
    },
  },
} as const;
