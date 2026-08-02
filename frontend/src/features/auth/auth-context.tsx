"use client"

import React, { createContext, useContext, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/axios";
import { toast } from "sonner";

interface User {
  id: string;
  name: string;
  email: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  updateProfile: (name: string, email: string) => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  const fetchProfile = async (token: string) => {
    try {
      const response = await api.get("/api/users/profile");
      setUser(response.data);
    } catch (error: any) {
      console.error("Failed to load user profile:", error);
      logout();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const initAuth = async () => {
      if (typeof window !== "undefined") {
        const token = localStorage.getItem("accessToken");
        if (token) {
          await fetchProfile(token);
        } else {
          setLoading(false);
        }
      }
    };
    initAuth();
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
      await api.post("/api/auth/register", { name, email, password });
      toast.success("Account created successfully! You can now log in.");
      router.push("/login");
    } catch (error: any) {
      const msg = error.response?.data?.message || "Registration failed. Try again.";
      toast.error(msg);
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    setUser(null);
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

  const refreshUser = async () => {
    if (localStorage.getItem("accessToken")) {
      try {
        const response = await api.get("/api/users/profile");
        setUser(response.data);
      } catch (e) {
        console.error("Error refreshing user profile", e);
      }
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        logout,
        updateProfile,
        refreshUser,
      }}
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
