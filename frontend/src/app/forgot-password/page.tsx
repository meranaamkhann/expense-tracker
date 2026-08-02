"use client";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { ArrowRight, Leaf, Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

export default function ForgotPasswordPage() {
  const { forgotPassword } = useAuth();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const d = new FormData(e.currentTarget);
    setBusy(true);
    try {
      const msg = await forgotPassword(String(d.get("email")));
      setMessage(msg);
    } catch {
      // toast already shown
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="grid min-h-screen place-items-center bg-[#f7f3ea] p-6">
      <section className="w-full max-w-md rounded-[2rem] border border-[#e8e3d9] bg-[#fffdf8] p-8 shadow-xl shadow-[#203128]/5">
        <Link href="/" className="flex items-center gap-2 text-lg font-bold">
          <span className="grid size-9 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-4" /></span>spendwise
        </Link>
        <p className="eyebrow mt-10">Forgot your password?</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">No worries — it happens.</h1>
        <p className="mt-3 text-[#68756d]">Enter your email and we&apos;ll send you a link to reset it.</p>
        {message ? (
          <p className="mt-7 rounded-xl bg-[#dce9dd] p-4 text-sm text-[#28533a]">{message}</p>
        ) : (
          <form onSubmit={submit} className="mt-7 space-y-4">
            <label className="block text-sm font-semibold">Email
              <input name="email" type="email" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-4 py-3 outline-none focus:border-[#3e6b50]" />
            </label>
            <button disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#203128] py-3.5 font-bold text-white transition hover:bg-[#3e6b50] disabled:opacity-60">
              {busy ? <Loader2 className="size-4 animate-spin" /> : <>Send reset link <ArrowRight className="size-4" /></>}
            </button>
          </form>
        )}
        <p className="mt-6 text-center text-sm text-[#68756d]">Remembered it? <Link href="/login" className="font-bold text-[#3e6b50]">Back to login</Link></p>
      </section>
    </main>
  );
}
