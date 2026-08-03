"use client";
import { FormEvent, useEffect, useState } from "react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Budget } from "@/lib/types";
import { createBudget, deleteBudget, fetchBudgets } from "@/lib/api";
import { useWallet } from "@/features/wallet/wallet-context";

export default function BudgetsPage() {
  const { categories } = useWallet();
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setBudgets(await fetchBudgets());
    } catch {
      toast.error("Couldn't load your budgets.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
   void load();
  }, []);

  const budgetedCategoryIds = new Set(budgets.map((b) => b.categoryId));
  const availableCategories = categories.filter((c) => !budgetedCategoryIds.has(c.id));

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    setBusy(true);
    try {
      const created = await createBudget({
        categoryId: Number(d.get("categoryId")),
        monthlyLimit: Number(d.get("monthlyLimit")),
      });
      setBudgets((prev) => [...prev, created]);
      form.reset();
      toast.success("Budget set.");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Could not set that budget.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    try {
      await deleteBudget(id);
      setBudgets((prev) => prev.filter((b) => b.id !== id));
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Could not remove that budget.");
    }
  };

  return (
    <div className="mx-auto max-w-5xl">
      <p className="eyebrow">Gentle limits</p>
      <h1 className="mt-2 text-4xl font-semibold tracking-tight">Set a ceiling, not a cage.</h1>
      <p className="mt-3 max-w-xl text-[#68756d]">A monthly limit per category — just enough structure to notice before you overspend.</p>

      <div className="mt-8 grid gap-7 lg:grid-cols-[.7fr_1.3fr]">
        <form onSubmit={submit} className="card h-fit p-6">
          <h2 className="text-xl font-semibold">New budget</h2>
          {availableCategories.length === 0 ? (
            <p className="mt-4 text-sm text-[#68756d]">Every category already has a budget.</p>
          ) : (
            <>
              <label className="mt-5 block text-sm font-semibold">Category
                <select name="categoryId" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]">
                  {availableCategories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </label>
              <label className="mt-4 block text-sm font-semibold">Monthly limit (₹)
                <input name="monthlyLimit" type="number" min="1" step="1" required placeholder="e.g. 5000" className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]" />
              </label>
              <button disabled={busy} className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-[#3e6b50] py-3 font-bold text-white disabled:opacity-60">
                {busy ? <Loader2 className="size-4 animate-spin" /> : <><Plus className="size-4" /> Set budget</>}
              </button>
            </>
          )}
        </form>

        <section className="grid gap-4 sm:grid-cols-2">
          {loading && <p className="text-[#68756d]">Loading your budgets…</p>}
          {!loading && budgets.length === 0 && (
            <p className="text-[#68756d]">No budgets yet — set one to keep an eye on a category.</p>
          )}
          {budgets.map((b) => {
            const over = b.percentUsed >= 100;
            const near = b.percentUsed >= 80 && !over;
            return (
              <article key={b.id} className="card p-5">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <span className="size-3 rounded-full" style={{ background: b.categoryColor }} />
                    <h2 className="text-lg font-semibold">{b.category}</h2>
                  </div>
                  <button onClick={() => remove(b.id)} className="text-[#8a938c] hover:text-[#c96c4b]" aria-label={`Remove ${b.category} budget`}>
                    <Trash2 className="size-4" />
                  </button>
                </div>
                <div className="mt-4 flex justify-between text-sm">
                  <span className="font-semibold">₹{b.spentThisMonth.toFixed(0)} of ₹{b.monthlyLimit.toFixed(0)}</span>
                  <span className={over ? "font-bold text-[#c96c4b]" : near ? "font-bold text-[#b98a2e]" : "text-[#68756d]"}>{b.percentUsed}%</span>
                </div>
                <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-[#f2efe7]">
                  <div
                    className="h-full rounded-full"
                    style={{ width: `${Math.min(100, b.percentUsed)}%`, background: over ? "#c96c4b" : near ? "#dba43a" : b.categoryColor }}
                  />
                </div>
                {over && <p className="mt-3 text-sm font-semibold text-[#c96c4b]">Over budget this month.</p>}
                {near && <p className="mt-3 text-sm font-semibold text-[#b98a2e]">Getting close to the limit.</p>}
              </article>
            );
          })}
        </section>
      </div>
    </div>
  );
}
