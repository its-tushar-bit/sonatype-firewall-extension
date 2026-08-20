import { useEffect, useState } from "react";

const THEME_KEY = "hexawatch:theme";

// Shared dark-mode hook for popup, options, and waiver pages. Reads/writes
// the preference through chrome.storage.local so all three surfaces stay in
// step, falling back to the OS prefers-color-scheme on first load.
export function useDarkMode(): {
  dark: boolean;
  toggle: () => void;
} {
  const [dark, setDark] = useState(false);

  useEffect(() => {
    chrome.storage.local.get(THEME_KEY).then((r) => {
      const t = r[THEME_KEY];
      const prefersDark =
        typeof window !== "undefined" &&
        window.matchMedia?.("(prefers-color-scheme: dark)").matches;
      setDark(t === "dark" || (t === undefined && prefersDark));
    });
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    // Paint html to match body in BOTH modes. If we leave light-mode blank,
    // Chrome fills any pixels around the 420px body with its own popup
    // background (dark on some themes), causing a strip on the right edge.
    // #111827 = Tailwind gray-900 (dark body); #ffffff = light body.
    document.documentElement.style.background = dark ? "#111827" : "#ffffff";
  }, [dark]);

  function toggle() {
    const next = !dark;
    setDark(next);
    void chrome.storage.local.set({ [THEME_KEY]: next ? "dark" : "light" });
  }

  return { dark, toggle };
}
