import { ui } from "@arcata/envs";
import type { EmailOtpType, Session } from "@supabase/supabase-js";
import client from "./client";

/**
 * Sets the session in the Supabase client.
 * Use this when receiving a session token from another app.
 */
export async function setSession(session: Session) {
  const { data, error } = await client.auth.setSession(session);
  return { data, error };
}

export async function getCurrentUser() {
  try {
    const {
      data: { user },
      error,
    } = await client.auth.getUser();

    if (error) {
      //   console.error('Error fetching user:', error);
      return null;
    }

    return user;
  } catch (error) {
    console.error("Error:", error);
    return null;
  }
}

export async function getSession() {
  try {
    const {
      data: { session },
      error,
    } = await client.auth.getSession();

    if (error) {
      //   console.error('Error fetching user:', error);
      return null;
    }

    return session;
  } catch (error) {
    console.error("Error:", error);
    return null;
  }
}

export async function signInWithEmail(email: string | null) {
  if (!email) {
    return { data: null, error: new Error("email cannot be empty") };
  }
  const { data, error } = await client.auth.signInWithOtp({
    email,
    options: {
      emailRedirectTo: ui.get("VITE_AUTH_BASE_URL"),
      shouldCreateUser: true,
    },
  });
  return { data, error };
}

export async function verifyOTP(token_hash: string, type: EmailOtpType) {
  const { data, error } = await client.auth.verifyOtp({
    type,
    token_hash,
  });
  return { data, error };
}

export async function signOut() {
  const { error } = await client.auth.signOut();
  return { error };
}

type ProfileUpdate = {
  firstName?: string;
  lastName?: string;
  username?: string;
  timezone?: string;
  avatarUrl?: string;
};

type ProfileData = {
  id: string;
  email: string;
  first_name: string | null;
  last_name: string | null;
  full_name: string | null;
  username: string | null;
  timezone: string | null;
  avatar_url: string | null;
  website: string | null;
  created_at: string;
  updated_at: string;
};

export async function updateUserProfile(
  profile: ProfileUpdate
): Promise<{ data: ProfileData | null; error: Error | null }> {
  // Get current user
  const {
    data: { user },
  } = await client.auth.getUser();

  if (!user) {
    return { data: null, error: new Error("User not authenticated") };
  }

  // Build full_name from first and last name
  const fullName =
    profile.firstName && profile.lastName
      ? `${profile.firstName} ${profile.lastName}`
      : profile.firstName || profile.lastName || null;

  // Update the profiles table
  const { data, error } = await client
    .from("profiles")
    .update({
      first_name: profile.firstName,
      last_name: profile.lastName,
      full_name: fullName,
      username: profile.username,
      timezone: profile.timezone,
      avatar_url: profile.avatarUrl,
    })
    .eq("id", user.id)
    .select()
    .single();

  return { data, error };
}

export async function getUserProfile(): Promise<{
  data: ProfileData | null;
  error: Error | null;
}> {
  const {
    data: { user },
  } = await client.auth.getUser();

  if (!user) {
    return { data: null, error: new Error("User not authenticated") };
  }

  const { data, error } = await client
    .from("profiles")
    .select("*")
    .eq("id", user.id)
    .single();

  return { data, error };
}

/**
 * Deletes the current user's account.
 * This will:
 * 1. Delete the user's profile from the profiles table (cascade should handle related data)
 * 2. Sign out the user
 *
 * Note: Full user deletion from auth.users requires a server-side admin API call
 * or a Supabase Edge Function. This function handles the client-side cleanup.
 */
export async function deleteAccount(): Promise<{ error: Error | null }> {
  const {
    data: { user },
  } = await client.auth.getUser();

  if (!user) {
    return { error: new Error("User not authenticated") };
  }

  // Delete profile data (this will cascade to related tables via foreign keys)
  const { error: profileError } = await client
    .from("profiles")
    .delete()
    .eq("id", user.id);

  if (profileError) {
    return { error: profileError };
  }

  // Sign out the user
  const { error: signOutError } = await client.auth.signOut();

  if (signOutError) {
    return { error: signOutError };
  }

  return { error: null };
}
