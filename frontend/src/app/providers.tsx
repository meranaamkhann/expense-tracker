"use client";

import { AuthProvider } from "@/features/auth/auth-context";
import { WalletProvider } from "@/features/wallet/wallet-context";
import { Toaster } from "@/components/ui/sonner";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <WalletProvider>
        {children}
        <Toaster position="top-center" richColors />
      </WalletProvider>
    </AuthProvider>
  );
}
