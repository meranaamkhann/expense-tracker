"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/axios";
import { toast } from "sonner";
import { User } from "@/lib/types";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  updateProfile: (name: string, email: string) => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  forgotPassword: (email: string) => Promise<string>;
  resetPassword: (token: string, newPassword: string) => Promise<string>;
  verifyEmail: (token: string) => Promise<string>;
  resendVerification: () => Promise<string>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  const fetchProfile = async () => {
    try {
      const response = await api.get<User>("/api/users/profile");
      setUser(response.data);
    } catch (error) {
      console.error("Failed to load user profile:", error);
      clearSession();
    } finally {
      setLoading(false);
    }
  };

  const clearSession = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    setUser(null);
  };

  useEffect(() => {
    const initAuth = async () => {
      if (typeof window !== "undefined") {
        const token = localStorage.getItem("accessToken");
        if (token) {
          await fetchProfile();
        } else {
          setLoading(false);
        }
      }
    };
    initAuth();

    const onSessionExpired = () => {
      clearSession();
      toast.info("Your session expired. Please log in again.");
      router.push("/login");
    };
    window.addEventListener("spendwise-session-expired", onSessionExpired);
    return () => window.removeEventListener("spendwise-session-expired", onSessionExpired);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = async (email: string, password: string) => {
    try {
      const response = await api.post("/api/auth/login", { email, password });
      const { accessToken, refreshToken, user: loggedUser } = response.data;

      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      setUser(loggedUser);
      toast.success(`Welcome back, ${loggedUser.name}!`);
      router.push("/dashboard");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Invalid credentials. Please try again.";
      toast.error(msg);
      throw error;
    }
  };

  const register = async (name: string, email: string, password: string) => {
    try {
      const response = await api.post("/api/auth/register", { name, email, password });
      const { accessToken, refreshToken, user: newUser } = response.data;

      // Registration returns a live session too, so the new user lands straight in the dashboard.
      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      setUser(newUser);
      toast.success("Account created! Welcome to SpendWise.");
      router.push("/dashboard");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Registration failed. Try again.";
      toast.error(msg);
      throw error;
    }
  };

  const logout = () => {
    api.post("/api/auth/logout").catch(() => {});
    clearSession();
    toast.info("Logged out successfully.");
    router.push("/login");
  };

  const updateProfile = async (name: string, email: string) => {
    try {
      const response = await api.put("/api/users/profile", { name, email });
      setUser(response.data);
      toast.success("Profile updated successfully!");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Profile update failed.";
      toast.error(msg);
      throw error;
    }
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    try {
      await api.put("/api/users/profile/password", { currentPassword, newPassword });
      toast.success("Password updated successfully!");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Could not update password.";
      toast.error(msg);
      throw error;
    }
  };

  const forgotPassword = async (email: string): Promise<string> => {
    try {
      const response = await api.post("/api/auth/forgot-password", { email });
      return response.data?.message || "If that email has an account, a reset link is on its way.";
    } catch (error: any) {
      const msg = error.response?.data?.message || "Could not process that request.";
      toast.error(msg);
      throw error;
    }
  };

  const resetPassword = async (token: string, newPassword: string): Promise<string> => {
    try {
      const response = await api.post("/api/auth/reset-password", { token, newPassword });
      return response.data?.message || "Password reset successfully.";
    } catch (error: any) {
      const msg = error.response?.data?.message || "That reset link is invalid or has expired.";
      toast.error(msg);
      throw error;
    }
  };

  const verifyEmail = async (token: string): Promise<string> => {
    try {
      const response = await api.post("/api/auth/verify-email", { token });
      await refreshUser();
      return response.data?.message || "Email verified.";
    } catch (error: any) {
      const msg = error.response?.data?.message || "That verification link is invalid or has expired.";
      toast.error(msg);
      throw error;
    }
  };

  const resendVerification = async (): Promise<string> => {
    try {
      const response = await api.post("/api/auth/resend-verification", { email: user?.email });
      return response.data?.message || "If that email needs verifying, a new link is on its way.";
    } catch (error: any) {
      const msg = error.response?.data?.message || "Could not resend the verification email.";
      toast.error(msg);
      throw error;
    }
  };

  const refreshUser = async () => {
    if (typeof window !== "undefined" && localStorage.getItem("accessToken")) {
      await fetchProfile();
    }
  };

  return (
    <AuthContext.Provider
      value={{ user, loading, login, register, logout, updateProfile, changePassword, forgotPassword, resetPassword, verifyEmail, resendVerification, refreshUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
