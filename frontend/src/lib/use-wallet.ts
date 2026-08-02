"use client";
import { useEffect, useState } from "react";
import { getCategories, getTransactions, getUser, initialise } from "@/lib/store";

export function useWallet() {
  const [snapshot, setSnapshot] = useState({ user: getUser(), categories: getCategories(), transactions: getTransactions() });
  useEffect(() => { initialise(); const refresh = () => setSnapshot({ user: getUser(), categories: getCategories(), transactions: getTransactions() }); refresh(); window.addEventListener("spendwise-change", refresh); return () => window.removeEventListener("spendwise-change", refresh); }, []);
  return snapshot;
}
