"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/auth-context";
import { AdminStats, AdminUser, fetchAdminStats, fetchAdminUsers, setAdminUserEnabled } from "@/lib/api";

export default function AdminPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.replace("/dashboard");
    }
  }, [user, router]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    (async () => {
      try {
        const [statsData, usersData] = await Promise.all([fetchAdminStats(), fetchAdminUsers(0, 50)]);
        setStats(statsData);
        setUsers(usersData.content);
      } catch {
        toast.error("Couldn't load admin data.");
      } finally {
        setLoading(false);
      }
    })();
  }, [user]);

  const toggleUser = async (target: AdminUser) => {
    try {
      const updated = await setAdminUserEnabled(target.id, !target.enabled);
      setUsers((prev) => prev.map((u) => (u.id === target.id ? updated : u)));
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Could not update that user.");
    }
  };

  if (user?.role !== "ADMIN") return null;

  return (
    <div className="mx-auto max-w-5xl">
      <p className="eyebrow">Platform view</p>
      <h1 className="mt-2 flex items-center gap-3 text-4xl font-semibold tracking-tight">
        <ShieldCheck className="size-8 text-[#3e6b50]" /> Admin
      </h1>

      {loading ? (
        <p className="mt-9 text-[#68756d]">Loading…</p>
      ) : (
        <>
          <section className="mt-8 grid gap-4 md:grid-cols-3">
            <article className="card p-5">
              <p className="text-sm text-[#68756d]">Total users</p>
              <p className="mt-2 text-3xl font-semibold">{stats?.totalUsers ?? 0}</p>
            </article>
            <article className="card p-5">
              <p className="text-sm text-[#68756d]">Total entries</p>
              <p className="mt-2 text-3xl font-semibold">{stats?.totalExpenseEntries ?? 0}</p>
            </article>
            <article className="rounded-[22px] bg-[#3e6b50] p-5 text-white">
              <p className="text-sm text-[#dce9dd]">Total tracked spend</p>
              <p className="mt-2 text-3xl font-semibold">₹{(stats?.totalExpenseVolume ?? 0).toLocaleString("en-IN")}</p>
            </article>
          </section>

          <section className="card mt-7 overflow-hidden">
            <div className="border-b border-[#eee9df] p-6">
              <h2 className="text-xl font-semibold">Users</h2>
              <p className="mt-1 text-sm text-[#68756d]">Disabling a user signs them out everywhere and blocks login.</p>
            </div>
            <div className="divide-y divide-[#eee9df]">
              {users.map((u) => (
                <div key={u.id} className="flex flex-wrap items-center justify-between gap-3 p-5">
                  <div>
                    <p className="font-semibold">{u.name} <span className="ml-2 rounded-full bg-[#f2efe7] px-2 py-0.5 text-xs font-bold text-[#68756d]">{u.role}</span></p>
                    <p className="mt-1 text-sm text-[#68756d]">{u.email}{!u.emailVerified && " · unverified"}</p>
                  </div>
                  <button
                    onClick={() => toggleUser(u)}
                    disabled={u.id === user?.id}
                    className={`rounded-full border px-4 py-1.5 text-sm font-bold disabled:opacity-40 ${u.enabled ? "border-[#c96c4b] text-[#9d482f]" : "border-[#3e6b50] text-[#28533a]"}`}
                  >
                    {u.enabled ? "Disable" : "Enable"}
                  </button>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
