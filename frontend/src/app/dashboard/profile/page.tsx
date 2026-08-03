"use client";
import { FormEvent, useState } from "react";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

export default function ProfilePage() {
  const { user, updateProfile, changePassword, logout, deleteAccount } = useAuth();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [pwBusy, setPwBusy] = useState(false);
  const [pwMessage, setPwMessage] = useState("");
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  const save = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const d = new FormData(e.currentTarget);
    setBusy(true);
    setMessage("");
    try {
      await updateProfile(String(d.get("name")), String(d.get("email")));
      setMessage("Your details are saved.");
    } catch {
      // toast already shown
    } finally {
      setBusy(false);
    }
  };

  const savePassword = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = new FormData(form);
    setPwBusy(true);
    setPwMessage("");
    try {
      await changePassword(String(d.get("currentPassword")), String(d.get("newPassword")));
      setPwMessage("Password updated.");
      form.reset();
    } catch {
      // toast already shown
    } finally {
      setPwBusy(false);
    }
  };

  const submitDelete = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const d = new FormData(e.currentTarget);
    setDeleteBusy(true);
    setDeleteError("");
    try {
      await deleteAccount(String(d.get("password")));
    } catch (err: any) {
      setDeleteError(err?.response?.data?.message || "Password is incorrect.");
    } finally {
      setDeleteBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl">
      <p className="eyebrow">Your space</p>
      <h1 className="mt-2 text-4xl font-semibold tracking-tight">Keep it feeling like yours.</h1>

      <section className="card mt-8 p-6">
        <h2 className="text-xl font-semibold">Personal details</h2>
        <form onSubmit={save} className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-semibold">Name
            <input name="name" defaultValue={user?.name} required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5" />
          </label>
          <label className="text-sm font-semibold">Email
            <input name="email" type="email" defaultValue={user?.email} required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5" />
          </label>
          <button disabled={busy} className="flex items-center justify-center gap-2 rounded-xl bg-[#3e6b50] px-5 py-3 font-bold text-white sm:col-span-2 disabled:opacity-60">
            {busy ? <Loader2 className="size-4 animate-spin" /> : "Save details"}
          </button>
        </form>
        {message && <p className="mt-4 rounded-xl bg-[#dce9dd] p-3 text-sm text-[#28533a]">{message}</p>}
      </section>

      <section className="card mt-6 p-6">
        <h2 className="text-xl font-semibold">Change password</h2>
        <form onSubmit={savePassword} className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-semibold">Current password
            <input name="currentPassword" type="password" required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5" />
          </label>
          <label className="text-sm font-semibold">New password
            <input name="newPassword" type="password" minLength={8} required className="mt-2 w-full rounded-xl border border-[#d8d2c6] bg-white px-3 py-2.5" />
          </label>
          <button disabled={pwBusy} className="flex items-center justify-center gap-2 rounded-xl bg-[#203128] px-5 py-3 font-bold text-white sm:col-span-2 disabled:opacity-60">
            {pwBusy ? <Loader2 className="size-4 animate-spin" /> : "Update password"}
          </button>
        </form>
        {pwMessage && <p className="mt-4 rounded-xl bg-[#dce9dd] p-3 text-sm text-[#28533a]">{pwMessage}</p>}
      </section>

      <section className="mt-6 rounded-[22px] border border-[#f0cfc0] bg-[#fff0e9] p-6">
        <h2 className="text-xl font-semibold text-[#763e2b]">Leave this device</h2>
        <p className="mt-2 text-sm leading-6 text-[#875746]">This signs you out here. Your data stays safely stored on the server for next time.</p>
        <button onClick={logout} className="mt-5 rounded-xl border border-[#c96c4b] px-5 py-2.5 font-bold text-[#9d482f]">Log out</button>
      </section>

      <section className="mt-6 rounded-[22px] border border-[#f0cfc0] bg-[#fff0e9] p-6">
        <h2 className="text-xl font-semibold text-[#763e2b]">Delete your account</h2>
        <p className="mt-2 text-sm leading-6 text-[#875746]">This permanently removes your account, categories, and every entry You&apos;ve recorded. There&apos;s no undo.</p>
        {!confirmingDelete ? (
          <button onClick={() => setConfirmingDelete(true)} className="mt-5 rounded-xl border border-[#c96c4b] px-5 py-2.5 font-bold text-[#9d482f]">Delete my account</button>
        ) : (
          <form onSubmit={submitDelete} className="mt-5 space-y-3">
            <label className="block text-sm font-semibold text-[#763e2b]">Confirm your password to continue
              <input name="password" type="password" required className="mt-2 w-full max-w-sm rounded-xl border border-[#f0cfc0] bg-white px-3 py-2.5" />
            </label>
            {deleteError && <p className="text-sm font-medium text-[#9d482f]">{deleteError}</p>}
            <div className="flex gap-3">
              <button disabled={deleteBusy} className="flex items-center gap-2 rounded-xl bg-[#c96c4b] px-5 py-2.5 font-bold text-white disabled:opacity-60">
                {deleteBusy ? <Loader2 className="size-4 animate-spin" /> : "Yes, permanently delete"}
              </button>
              <button type="button" onClick={() => { setConfirmingDelete(false); setDeleteError(""); }} className="rounded-xl px-5 py-2.5 font-bold text-[#68756d]">Cancel</button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
