"use client";
import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, Leaf, Loader2, XCircle } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

function VerifyEmailInner() {
  const { verifyEmail } = useAuth();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";
  const [status, setStatus] = useState<"checking" | "success" | "error">("checking");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const verify = async () => {
      if (!token) {
        setStatus("error");
        setMessage("This link is missing its token.");
        return;
      }

      try {
        const msg = await verifyEmail(token);
        setStatus("success");
        setMessage(msg);
      } catch (err: any) {
        setStatus("error");
        setMessage(
          err?.response?.data?.message ??
            "That verification link is invalid or has expired."
        );
      }
    };

    void verify();
  }, [token, verifyEmail]);

  return (
    <section className="w-full max-w-md rounded-[2rem] border border-[#e8e3d9] bg-[#fffdf8] p-8 text-center shadow-xl shadow-[#203128]/5">
      <Link href="/" className="mx-auto flex w-fit items-center gap-2 text-lg font-bold">
        <span className="grid size-9 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-4" /></span>spendwise
      </Link>
      <div className="mt-10">
        {status === "checking" && <Loader2 className="mx-auto size-10 animate-spin text-[#3e6b50]" />}
        {status === "success" && <CheckCircle2 className="mx-auto size-10 text-[#3e6b50]" />}
        {status === "error" && <XCircle className="mx-auto size-10 text-[#c96c4b]" />}
        <p className="mt-5 text-[#68756d]">{status === "checking" ? "Confirming your email…" : message}</p>
      </div>
      <Link href={status === "success" ? "/dashboard" : "/login"} className="mt-8 inline-block rounded-xl bg-[#203128] px-6 py-3 font-bold text-white">
        {status === "success" ? "Go to dashboard" : "Back to login"}
      </Link>
    </section>
  );
}

export default function VerifyEmailPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-[#f7f3ea] p-6">
      <Suspense fallback={null}>
        <VerifyEmailInner />
      </Suspense>
    </main>
  );
}
