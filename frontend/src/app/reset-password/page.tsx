"use client";
import Link from "next/link";
import { FormEvent, Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ArrowRight, Leaf, Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { PasswordInput } from "@/components/password-input";

function ResetPasswordForm() {
  const { resetPassword } = useAuth();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");
    if (!token) {
      setError("This reset link is missing its token. Please request a new one.");
      return;
    }
    const d = new FormData(e.currentTarget);
    setBusy(true);
    try {
      const msg = await resetPassword(token, String(d.get("newPassword")));
      setMessage(msg);
    } catch (err: any) {
      setError(err?.response?.data?.message || "That reset link is invalid or has expired.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="w-full max-w-md rounded-[2rem] border border-[#e8e3d9] bg-[#fffdf8] p-8 shadow-xl shadow-[#203128]/5">
      <Link href="/" className="flex items-center gap-2 text-lg font-bold">
        <span className="grid size-9 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-4" /></span>spendwise
      </Link>
      <p className="eyebrow mt-10">Set a new password</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">Almost there.</h1>
      {message ? (
        <>
          <p className="mt-7 rounded-xl bg-[#dce9dd] p-4 text-sm text-[#28533a]">{message}</p>
          <p className="mt-6 text-center text-sm text-[#68756d]"><Link href="/login" className="font-bold text-[#3e6b50]">Go to login</Link></p>
        </>
      ) : (
        <form onSubmit={submit} className="mt-7 space-y-4">
          <label className="block text-sm font-semibold">New password
            <PasswordInput name="newPassword" minLength={8} required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-4 py-3 pr-11 outline-none focus:border-[#3e6b50]" />
          </label>
          {error && <p className="text-sm font-medium text-[#b74c3d]">{error}</p>}
          <button disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#203128] py-3.5 font-bold text-white transition hover:bg-[#3e6b50] disabled:opacity-60">
            {busy ? <Loader2 className="size-4 animate-spin" /> : <>Reset password <ArrowRight className="size-4" /></>}
          </button>
        </form>
      )}
    </section>
  );
}

export default function ResetPasswordPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-[#f7f3ea] p-6">
      <Suspense fallback={null}>
        <ResetPasswordForm />
      </Suspense>
    </main>
  );
}