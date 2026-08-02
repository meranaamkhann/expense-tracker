import { api } from "@/lib/axios";
import { Category, Transaction } from "@/lib/types";

// ---- Categories ----

export async function fetchCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>("/api/categories");
  return data;
}

export async function createCategory(input: { name: string; color: string; icon?: string }): Promise<Category> {
  const { data } = await api.post<Category>("/api/categories", input);
  return data;
}

export async function updateCategory(
  id: number,
  input: { name: string; color: string; icon?: string }
): Promise<Category> {
  const { data } = await api.put<Category>(`/api/categories/${id}`, input);
  return data;
}

export async function deleteCategory(id: number): Promise<void> {
  await api.delete(`/api/categories/${id}`);
}

// ---- Expenses ----

type ExpenseApiShape = {
  id: number;
  title: string;
  amount: number;
  currency: string;
  kind: "expense" | "income";
  notes: string | null;
  date: string;
  categoryId: number;
  category: string;
  categoryColor: string | null;
};

function toTransaction(e: ExpenseApiShape): Transaction {
  return {
    id: e.id,
    title: e.title,
    amount: e.amount,
    currency: e.currency,
    categoryId: e.categoryId,
    category: e.category,
    categoryColor: e.categoryColor ?? undefined,
    date: e.date,
    kind: e.kind,
    notes: e.notes ?? undefined,
  };
}

export async function fetchTransactions(): Promise<Transaction[]> {
  const { data } = await api.get<ExpenseApiShape[]>("/api/expenses");
  return data.map(toTransaction);
}

export async function createTransaction(input: {
  title: string;
  amount: number;
  categoryId: number;
  date: string;
  kind: "expense" | "income";
  notes?: string;
}): Promise<Transaction> {
  const { data } = await api.post<ExpenseApiShape>("/api/expenses", { ...input, currency: "INR" });
  return toTransaction(data);
}

export async function deleteTransaction(id: number): Promise<void> {
  await api.delete(`/api/expenses/${id}`);
}

// ---- Analytics ----

export type CategoryTotal = { categoryId: number; category: string; color: string | null; total: number };
export type MonthlyTotal = { month: string; income: number; expense: number };
export type AnalyticsSummary = {
  currency: string;
  totalIncome: number;
  totalExpense: number;
  balance: number;
  topCategories: CategoryTotal[];
  monthly: MonthlyTotal[];
};

export async function fetchAnalyticsSummary(months = 6): Promise<AnalyticsSummary> {
  const { data } = await api.get<AnalyticsSummary>("/api/analytics/summary", { params: { months } });
  return data;
}
