"use client";
import Link from "next/link";
import { useState } from "react";
import { ArrowRight, MailWarning, Plus, TrendingDown, TrendingUp, X } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useWallet } from "@/features/wallet/wallet-context";

const DISMISS_KEY = "spendwise-verify-banner-dismissed";

function readDismissed(): boolean {
  if (typeof window === "undefined") return false;
  return sessionStorage.getItem(DISMISS_KEY) === "true";
}

export default function DashboardPage() {
  const { user, resendVerification } = useAuth();
  const { categories, transactions, loading } = useWallet();
  const [resent, setResent] = useState(false);
  // Lazy initializer (runs once, during render) instead of an effect + setState after mount —
  // reading sessionStorage is a synchronous browser API, so there's no need to defer it to an
  // effect at all.
  const [dismissed, setDismissed] = useState(readDismissed);

  const dismissBanner = () => {
    sessionStorage.setItem(DISMISS_KEY, "true");
    setDismissed(true);
  };

  const handleResend = async () => {
    try {
      await resendVerification();
      setResent(true);
    } catch {
      // toast already shown
    }
  };

  const income = transactions.filter((t) => t.kind === "income").reduce((n, t) => n + t.amount, 0);
  const expenses = transactions.filter((t) => t.kind === "expense").reduce((n, t) => n + t.amount, 0);
  const byCat = categories
    .map((c) => ({ ...c, total: transactions.filter((t) => t.kind === "expense" && t.categoryId === c.id).reduce((n, t) => n + t.amount, 0) }))
    .filter((c) => c.total > 0)
    .sort((a, b) => b.total - a.total);

  return (
    <div className="mx-auto max-w-5xl">
      {user && !user.emailVerified && !dismissed && (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#f0cfc0] bg-[#fff0e9] px-5 py-4">
          <div className="flex items-center gap-3">
            <MailWarning className="size-5 shrink-0 text-[#9d482f]" />
            <p className="text-sm text-[#763e2b]">Please confirm your email address to secure your account.</p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={handleResend} disabled={resent} className="rounded-full border border-[#c96c4b] px-4 py-1.5 text-sm font-bold text-[#9d482f] disabled:opacity-60">
              {resent ? "Link sent — check your inbox" : "Resend verification email"}
            </button>
            <button onClick={dismissBanner} aria-label="Dismiss" className="rounded-full p-1.5 text-[#9d482f] hover:bg-[#f7d9ca]">
              <X className="size-4" />
            </button>
          </div>
        </div>
      )}
      <p className="eyebrow">A quiet check-in</p>
      <div className="mt-2 flex flex-wrap items-end justify-between gap-5">
        <div>
          <h1 className="text-4xl font-semibold tracking-tight">Hello, {user?.name?.split(" ")[0] ?? "there"}.</h1>
          <p className="mt-2 text-[#68756d]">Here&apos;s the shape of your money lately.</p>
        </div>
        <Link href="/dashboard/expenses" className="inline-flex items-center gap-2 rounded-full bg-[#c96c4b] px-5 py-3 font-bold text-white">
          <Plus className="size-4" /> Add an entry
        </Link>
      </div>

      {loading ? (
        <p className="mt-9 text-[#68756d]">Loading your numbers…</p>
      ) : (
        <>
          <section className="mt-9 grid gap-4 md:grid-cols-3">
            <article className="card p-5">
              <p className="text-sm text-[#68756d]">Money in</p>
              <p className="mt-2 text-3xl font-semibold">₹{income.toLocaleString("en-IN")}</p>
              <span className="mt-4 inline-flex items-center gap-1 text-sm font-bold text-[#3e6b50]"><TrendingUp className="size-4" /> A steady start</span>
            </article>
            <article className="card p-5">
              <p className="text-sm text-[#68756d]">Money out</p>
              <p className="mt-2 text-3xl font-semibold">₹{expenses.toLocaleString("en-IN")}</p>
              <span className="mt-4 inline-flex items-center gap-1 text-sm font-bold text-[#c96c4b]"><TrendingDown className="size-4" /> Mindful spending</span>
            </article>
            <article className="rounded-[22px] bg-[#3e6b50] p-5 text-white">
              <p className="text-sm text-[#dce9dd]">A little room left</p>
              <p className="mt-2 text-3xl font-semibold">₹{(income - expenses).toLocaleString("en-IN")}</p>
              <p className="mt-4 text-sm text-[#dce9dd]">For the things that support you.</p>
            </article>
          </section>

          <section className="mt-8 grid gap-7 lg:grid-cols-[1.25fr_.75fr]">
            <article className="card p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="eyebrow">Recent moments</p>
                  <h2 className="mt-1 text-xl font-semibold">What you&apos;ve added</h2>
                </div>
                <Link href="/dashboard/expenses" className="text-sm font-bold text-[#3e6b50]">See all</Link>
              </div>
              <div className="mt-5 divide-y divide-[#eee9df]">
                {transactions.length === 0 && <p className="py-6 text-sm text-[#68756d]">No entries yet — add your first one.</p>}
                {transactions.slice(0, 5).map((t) => {
                  const c = categories.find((x) => x.id === t.categoryId);
                  return (
                    <div key={t.id} className="flex items-center justify-between py-4">
                      <div className="flex items-center gap-3">
                        <span className="size-3 rounded-full" style={{ background: c?.color }} />
                        <div>
                          <p className="font-semibold">{t.title}</p>
                          <p className="text-sm text-[#68756d]">{c?.name} · {t.date}</p>
                        </div>
                      </div>
                      <strong className={t.kind === "income" ? "text-[#3e6b50]" : "text-[#203128]"}>
                        {t.kind === "income" ? "+" : "−"}₹{t.amount.toFixed(2)}
                      </strong>
                    </div>
                  );
                })}
              </div>
            </article>
            <article className="rounded-[22px] bg-[#e7efe6] p-6">
              <p className="eyebrow">Your attention</p>
              <h2 className="mt-2 text-xl font-semibold">Where did it go?</h2>
              <div className="mt-6 space-y-4">
                {byCat.slice(0, 4).map((c) => (
                  <div key={c.id}>
                    <div className="flex justify-between text-sm font-semibold">
                      <span>{c.name}</span><span>₹{c.total.toFixed(0)}</span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-white">
                      <div className="h-full rounded-full" style={{ width: `${Math.min(100, (c.total / Math.max(expenses, 1)) * 100)}%`, background: c.color }} />
                    </div>
                  </div>
                ))}
              </div>
              <Link href="/dashboard/analytics" className="mt-8 inline-flex items-center gap-2 text-sm font-bold text-[#3e6b50]">Reflect on patterns <ArrowRight className="size-4" /></Link>
            </article>
          </section>
        </>
      )}
    </div>
  );
}