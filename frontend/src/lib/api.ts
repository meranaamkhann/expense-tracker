import { api } from "@/lib/axios";
import { Budget, Category, Transaction } from "@/lib/types";

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

// ---- Budgets ----

export async function fetchBudgets(): Promise<Budget[]> {
  const { data } = await api.get<Budget[]>("/api/budgets");
  return data;
}

export async function createBudget(input: { categoryId: number; monthlyLimit: number }): Promise<Budget> {
  const { data } = await api.post<Budget>("/api/budgets", input);
  return data;
}

export async function updateBudget(id: number, input: { categoryId: number; monthlyLimit: number }): Promise<Budget> {
  const { data } = await api.put<Budget>(`/api/budgets/${id}`, input);
  return data;
}

export async function deleteBudget(id: number): Promise<void> {
  await api.delete(`/api/budgets/${id}`);
}

// ---- Export ----

/** Triggers a browser download of the user's expenses as a CSV file. */
export async function downloadExpensesCsv(): Promise<void> {
  const response = await api.get("/api/expenses/export", { responseType: "blob" });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = url;
  link.download = `spendwise-export-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

// ---- Admin ----

export type AdminUser = {
  id: number;
  name: string;
  email: string;
  role: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
};

export type AdminStats = {
  totalUsers: number;
  totalExpenseEntries: number;
  totalExpenseVolume: number;
  totalIncomeVolume: number;
};

export async function fetchAdminUsers(page = 0, size = 20): Promise<{ content: AdminUser[]; totalElements: number; totalPages: number }> {
  const { data } = await api.get("/api/admin/users", { params: { page, size } });
  return data;
}

export async function setAdminUserEnabled(id: number, enabled: boolean): Promise<AdminUser> {
  const { data } = await api.put<AdminUser>(`/api/admin/users/${id}/status`, { enabled });
  return data;
}

export async function fetchAdminStats(): Promise<AdminStats> {
  const { data } = await api.get<AdminStats>("/api/admin/stats");
  return data;
}
