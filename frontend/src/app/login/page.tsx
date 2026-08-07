"use client";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { ArrowRight, Leaf, Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { PasswordInput } from "@/components/password-input";

export default function LoginPage() {
  const { login } = useAuth();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    const data = new FormData(e.currentTarget);
    try {
      await login(String(data.get("email")), String(data.get("password")));
    } catch (err: any) {
      setError(err?.response?.data?.message || "Unable to sign in.");
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
        <p className="eyebrow mt-10">Welcome back</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Come back to your money story.</h1>
        <p className="mt-3 text-[#68756d]">Sign in with the email and password you registered with.</p>
        <form onSubmit={submit} className="mt-7 space-y-4">
          <label className="block text-sm font-semibold">Email
            <input name="email" type="email" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-4 py-3 outline-none focus:border-[#3e6b50]" />
          </label>
          <label className="block text-sm font-semibold">Password
            <PasswordInput name="password" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-4 py-3 pr-11 outline-none focus:border-[#3e6b50]" />
          </label>
          <div className="text-right">
            <Link href="/forgot-password" className="text-sm font-semibold text-[#3e6b50]">Forgot password?</Link>
          </div>
          {error && <p className="text-sm font-medium text-[#b74c3d]">{error}</p>}
          <button disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#203128] py-3.5 font-bold text-white transition hover:bg-[#3e6b50] disabled:opacity-60">
            {busy ? <Loader2 className="size-4 animate-spin" /> : <>Enter SpendWise <ArrowRight className="size-4" /></>}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-[#68756d]">New here? <Link href="/register" className="font-bold text-[#3e6b50]">Create your space</Link></p>
      </section>
    </main>
  );
}