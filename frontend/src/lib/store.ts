import { Category, Transaction, User } from "@/lib/types";

const KEYS = { users: "spendwise_users", user: "spendwise_user", categories: "spendwise_categories", transactions: "spendwise_transactions" };
const demoUser: User = { id: "demo", name: "Maya Green", email: "hello@spendwise.demo", password: "welcome123" };
const categories: Category[] = [
  { id: "groceries", name: "Groceries", color: "#6f956f" }, { id: "home", name: "Home", color: "#bd7656" },
  { id: "wellbeing", name: "Wellbeing", color: "#b18a50" }, { id: "travel", name: "Travel", color: "#608b9a" }, { id: "work", name: "Work", color: "#776b9c" },
];
const transactions: Transaction[] = [
  { id: "t1", title: "Farmer's market", amount: 48.2, categoryId: "groceries", date: "2026-08-01", kind: "expense" },
  { id: "t2", title: "August salary", amount: 4200, categoryId: "work", date: "2026-08-01", kind: "income" },
  { id: "t3", title: "Train card", amount: 35, categoryId: "travel", date: "2026-07-30", kind: "expense" },
  { id: "t4", title: "Yoga studio", amount: 22, categoryId: "wellbeing", date: "2026-07-29", kind: "expense" },
  { id: "t5", title: "Electricity", amount: 62.5, categoryId: "home", date: "2026-07-28", kind: "expense" },
];

function read<T>(key: string, fallback: T): T { if (typeof window === "undefined") return fallback; const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) as T : fallback; }
function write<T>(key: string, value: T) { localStorage.setItem(key, JSON.stringify(value)); window.dispatchEvent(new Event("spendwise-change")); }
export function initialise() { if (typeof window === "undefined") return; if (!localStorage.getItem(KEYS.users)) write(KEYS.users, [demoUser]); if (!localStorage.getItem(KEYS.categories)) write(KEYS.categories, categories); if (!localStorage.getItem(KEYS.transactions)) write(KEYS.transactions, transactions); }
export const getUser = () => read<User | null>(KEYS.user, null);
export function signIn(email: string, password: string) { initialise(); const user = read<User[]>(KEYS.users, []).find((item) => item.email.toLowerCase() === email.toLowerCase() && item.password === password); if (!user) throw new Error("Those details do not match an account."); write(KEYS.user, user); return user; }
export function register(name: string, email: string, password: string) { initialise(); const users = read<User[]>(KEYS.users, []); if (users.some((item) => item.email.toLowerCase() === email.toLowerCase())) throw new Error("An account already uses that email."); const user = { id: crypto.randomUUID(), name, email, password }; write(KEYS.users, [...users, user]); write(KEYS.user, user); return user; }
export function signOut() { localStorage.removeItem(KEYS.user); window.dispatchEvent(new Event("spendwise-change")); }
export const getCategories = () => read<Category[]>(KEYS.categories, categories);
export const saveCategories = (items: Category[]) => write(KEYS.categories, items);
export const getTransactions = () => read<Transaction[]>(KEYS.transactions, transactions).sort((a, b) => b.date.localeCompare(a.date));
export const saveTransactions = (items: Transaction[]) => write(KEYS.transactions, items);
export function addTransaction(item: Omit<Transaction, "id">) { saveTransactions([...getTransactions(), { ...item, id: crypto.randomUUID() }]); }
export function removeTransaction(id: string) { saveTransactions(getTransactions().filter((item) => item.id !== id)); }
