"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { toast } from "sonner";
import { Category, Transaction } from "@/lib/types";
import {
  createCategory,
  createTransaction,
  deleteCategory,
  deleteTransaction,
  fetchCategories,
  fetchTransactions,
  updateCategory,
} from "@/lib/api";
import { useAuth } from "@/features/auth/auth-context";

interface WalletContextType {
  categories: Category[];
  transactions: Transaction[];
  loading: boolean;
  refresh: () => Promise<void>;
  addTransaction: (input: {
    title: string;
    amount: number;
    categoryId: number;
    date: string;
    kind: "expense" | "income";
    notes?: string;
  }) => Promise<void>;
  removeTransaction: (id: number) => Promise<void>;
  addCategory: (input: { name: string; color: string; icon?: string }) => Promise<void>;
  editCategory: (id: number, input: { name: string; color: string; icon?: string }) => Promise<void>;
  removeCategory: (id: number) => Promise<void>;
}

const WalletContext = createContext<WalletContextType | undefined>(undefined);

export function WalletProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (!user) {
      setCategories([]);
      setTransactions([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [cats, txns] = await Promise.all([fetchCategories(), fetchTransactions()]);
      setCategories(cats);
      setTransactions(txns.sort((a, b) => b.date.localeCompare(a.date)));
    } catch (error) {
      console.error("Failed to load wallet data", error);
      toast.error("Couldn't load your data. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addTransaction: WalletContextType["addTransaction"] = async (input) => {
    try {
      const created = await createTransaction(input);
      setTransactions((prev) => [created, ...prev].sort((a, b) => b.date.localeCompare(a.date)));
      toast.success("Entry saved.");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Could not save that entry.");
      throw error;
    }
  };

  const removeTransaction = async (id: number) => {
    try {
      await deleteTransaction(id);
      setTransactions((prev) => prev.filter((t) => t.id !== id));
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Could not remove that entry.");
      throw error;
    }
  };

  const addCategory: WalletContextType["addCategory"] = async (input) => {
    try {
      const created = await createCategory(input);
      setCategories((prev) => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)));
      toast.success("Category added.");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Could not add that category.");
      throw error;
    }
  };

  const editCategory: WalletContextType["editCategory"] = async (id, input) => {
    try {
      const updated = await updateCategory(id, input);
      setCategories((prev) => prev.map((c) => (c.id === id ? updated : c)));
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Could not update that category.");
      throw error;
    }
  };

  const removeCategory = async (id: number) => {
    try {
      await deleteCategory(id);
      setCategories((prev) => prev.filter((c) => c.id !== id));
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Could not delete that category.");
      throw error;
    }
  };

  return (
    <WalletContext.Provider
      value={{ categories, transactions, loading, refresh, addTransaction, removeTransaction, addCategory, editCategory, removeCategory }}
    >
      {children}
    </WalletContext.Provider>
  );
}

export function useWallet() {
  const context = useContext(WalletContext);
  if (context === undefined) {
    throw new Error("useWallet must be used within a WalletProvider");
  }
  return context;
}
