"use client";
import { FormEvent, useState } from "react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { useWallet } from "@/features/wallet/wallet-context";

export default function ExpensesPage() {
  const { categories, transactions, addTransaction, removeTransaction, loading } = useWallet();
  const [kind, setKind] = useState<"expense" | "income">("expense");
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    setBusy(true);
    try {
      await addTransaction({
        title: String(d.get("title")),
        amount: Number(d.get("amount")),
        categoryId: Number(d.get("categoryId")),
        date: String(d.get("date")),
        kind,
        notes: String(d.get("note") || "") || undefined,
      });
      form.reset();
    } catch {
      // toast already shown by the wallet context
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-5xl">
      <p className="eyebrow">Your entries</p>
      <h1 className="mt-2 text-4xl font-semibold tracking-tight">Make room for the real numbers.</h1>
      <div className="mt-8 grid gap-7 lg:grid-cols-[.8fr_1.2fr]">
        <form onSubmit={submit} className="card h-fit p-6">
          <h2 className="text-xl font-semibold">Add a moment</h2>
          <p className="mt-1 text-sm text-[#68756d]">A small note is enough.</p>
          <div className="mt-5 flex rounded-xl bg-[#f2efe7] p-1">
            {(["expense", "income"] as const).map((item) => (
              <button type="button" onClick={() => setKind(item)} key={item} className={`flex-1 rounded-lg py-2 text-sm font-bold capitalize ${kind === item ? "bg-white text-[#3e6b50] shadow-sm" : "text-[#68756d]"}`}>{item}</button>
            ))}
          </div>
          <div className="mt-5 space-y-4">
            <label className="block text-sm font-semibold">What was it?
              <input name="title" required placeholder="A thoughtful name" className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]" />
            </label>
            <label className="block text-sm font-semibold">Amount (₹)
              <input name="amount" type="number" min="0.01" step="0.01" required placeholder="0.00" className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]" />
            </label>
            <label className="block text-sm font-semibold">Category
              <select name="categoryId" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]">
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            <label className="block text-sm font-semibold">When
              <input name="date" type="date" required defaultValue={new Date().toISOString().slice(0, 10)} className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5 outline-none focus:border-[#3e6b50]" />
            </label>
          </div>
          <button disabled={busy} className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-[#3e6b50] py-3 font-bold text-white disabled:opacity-60">
            {busy ? <Loader2 className="size-4 animate-spin" /> : <><Plus className="size-4" /> Save this entry</>}
          </button>
        </form>
        <section className="card overflow-hidden">
          <div className="border-b border-[#eee9df] p-6">
            <h2 className="text-xl font-semibold">Your timeline</h2>
            <p className="mt-1 text-sm text-[#68756d]">Every entry stays here until you remove it.</p>
          </div>
          <div className="divide-y divide-[#eee9df]">
            {!loading && transactions.length === 0 && <p className="p-6 text-sm text-[#68756d]">Nothing here yet.</p>}
            {transactions.map((t) => {
              const cat = categories.find((c) => c.id === t.categoryId);
              return (
                <article key={t.id} className="flex items-center gap-4 p-5">
                  <span className="size-3 shrink-0 rounded-full" style={{ background: cat?.color }} />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">{t.title}</p>
                    <p className="mt-1 text-sm text-[#68756d]">{cat?.name} · {t.date}</p>
                  </div>
                  <strong className={t.kind === "income" ? "text-[#3e6b50]" : ""}>{t.kind === "income" ? "+" : "−"}₹{t.amount.toFixed(2)}</strong>
                  <button onClick={() => removeTransaction(t.id)} className="rounded-lg p-2 text-[#8a938c] hover:bg-[#fae8e1] hover:text-[#c96c4b]" aria-label={`Remove ${t.title}`}>
                    <Trash2 className="size-4" />
                  </button>
                </article>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}
