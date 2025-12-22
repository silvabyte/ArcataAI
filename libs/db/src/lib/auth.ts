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
