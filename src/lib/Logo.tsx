import React from "react";
// Vite resolves these as static asset URLs. Filenames have spaces, which
// vite handles fine.
import lightLogo from "../icons/logo light mode.png";
import darkLogo from "../icons/logo dark mode.png";

export function Logo({
  dark,
  size = 24,
  className = "",
}: {
  dark: boolean;
  size?: number;
  className?: string;
}) {
  return (
    <img
      src={dark ? lightLogo : darkLogo}
      alt="HexaWatch"
      width={size}
      height={size}
      className={`inline-block rounded ${className}`}
    />
  );
}
