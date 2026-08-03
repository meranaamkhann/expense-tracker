export type Category = {
  id: number;
  name: string;
  color: string;
  icon?: string;
  isDefault?: boolean;
};

export type Transaction = {
  id: number;
  title: string;
  amount: number;
  currency: string;
  categoryId: number;
  category: string;
  categoryColor?: string;
  date: string;
  kind: "expense" | "income";
  notes?: string;
};

export type Budget = {
  id: number;
  categoryId: number;
  category: string;
  categoryColor?: string;
  monthlyLimit: number;
  spentThisMonth: number;
  remaining: number;
  percentUsed: number;
};

export type User = {
  id: number;
  name: string;
  email: string;
  role: string;
  emailVerified: boolean;
};
