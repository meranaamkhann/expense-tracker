"use client";
import { useState, forwardRef, InputHTMLAttributes } from "react";
import { Eye, EyeOff } from "lucide-react";

// Same border/rounding/focus styles as every other input in the app — nothing new introduced.
const baseInputClasses =
  "w-full rounded-xl border border-[#d8d2c6] bg-white px-4 py-3 pr-11 outline-none focus:border-[#3e6b50]";

interface PasswordInputProps extends InputHTMLAttributes<HTMLInputElement> {
  /** Classes for the outer wrapper (e.g. "max-w-sm") — use this, not `className`, to constrain width. */
  wrapperClassName?: string;
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput({ className, wrapperClassName, ...props }, ref) {
    const [visible, setVisible] = useState(false);

    return (
      <div className={`relative ${wrapperClassName ?? ""}`}>
        <input
          ref={ref}
          type={visible ? "text" : "password"}
          className={className ?? baseInputClasses}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          tabIndex={-1}
          aria-label={visible ? "Hide password" : "Show password"}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8a938c] hover:text-[#3e6b50]"
        >
          {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>
    );
  }
);