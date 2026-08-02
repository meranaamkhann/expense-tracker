export type Category = { id: string; name: string; color: string };
export type Transaction = { id: string; title: string; amount: number; categoryId: string; date: string; kind: "expense" | "income"; note?: string };
export type User = { id: string; name: string; email: string; password: string };
