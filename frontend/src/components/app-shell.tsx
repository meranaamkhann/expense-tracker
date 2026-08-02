"use client";
import Link from "next/link";
import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { BarChart3, FolderHeart, Leaf, LogOut, ReceiptText, Settings2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

const links = [
  ["/dashboard", "Overview", Leaf],
  ["/dashboard/expenses", "Entries", ReceiptText],
  ["/dashboard/categories", "Categories", FolderHeart],
  ["/dashboard/analytics", "Reflections", BarChart3],
  ["/dashboard/profile", "Settings", Settings2],
] as const;

export function AppShell({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const router = useRouter();
  const { user, loading, logout } = useAuth();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return (
      <div className="grid min-h-screen place-items-center bg-[#f7f3ea]">
        <span className="grid size-10 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-5" /></span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f7f3ea] text-[#203128]">
      <header className="border-b border-[#e8e3d9] bg-[#fffdf8]/85 px-5 py-4 backdrop-blur lg:hidden">
        <Link href="/dashboard" className="flex items-center gap-2 font-bold">
          <span className="grid size-8 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-4" /></span>spendwise
        </Link>
      </header>
      <div className="mx-auto flex max-w-7xl">
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-[#e8e3d9] bg-[#fffdf8] p-6 lg:flex">
          <Link href="/dashboard" className="flex items-center gap-2 text-xl font-bold">
            <span className="grid size-10 place-items-center rounded-full bg-[#3e6b50] text-white"><Leaf className="size-5" /></span>spendwise
          </Link>
          <p className="mt-12 px-3 text-xs font-bold uppercase tracking-[.16em] text-[#8a938c]">Your money garden</p>
          <nav className="mt-3 space-y-1">
            {links.map(([href, label, Icon]) => (
              <Link key={href} href={href} className={`flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold transition ${path === href ? "bg-[#dce9dd] text-[#28533a]" : "text-[#68756d] hover:bg-[#f2efe7]"}`}>
                <Icon className="size-4" />{label}
              </Link>
            ))}
          </nav>
          <div className="mt-auto rounded-2xl bg-[#dce9dd] p-4">
            <p className="text-xs text-[#526056]">A gentle reminder</p>
            <p className="mt-1 text-sm font-semibold">Small choices grow over time.</p>
          </div>
          <div className="mt-5 flex items-center justify-between px-2">
            <span className="max-w-32 truncate text-sm font-semibold">{user?.name ?? "Guest"}</span>
            <button onClick={logout} aria-label="Log out" className="text-[#68756d] hover:text-[#c96c4b]"><LogOut className="size-4" /></button>
          </div>
        </aside>
        <main className="min-w-0 flex-1 p-5 sm:p-8 lg:p-10">{children}</main>
      </div>
    </div>
  );
}
