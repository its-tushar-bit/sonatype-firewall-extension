import React, { useEffect, useState } from "react";
import {
  ExtensionSettings,
  FirewallVerdict,
  RuntimeMessage,
  RuntimeResponse,
} from "../types";

const verdictColor: Record<string, string> = {
  block: "bg-sonatype-danger text-white",
  quarantine: "bg-sonatype-warn text-black",
  warn: "bg-sonatype-warn text-black",
  allow: "bg-gray-500 text-white",
};

const verdictLabel: Record<string, string> = {
  block: "BLOCKED",
  quarantine: "QUARANTINED",
  warn: "WARNING",
  allow: "NO VULNERABILITIES",
};

function isConfigured(s: ExtensionSettings | null): boolean {
  if (!s) return false;
  if (!s.iqServerUrl || !s.userCode || !s.passCode) return false;
  if (s.mode === "real" && (!s.vrmId || s.selectedRepoIds.length === 0)) return false;
  return true;
}

function chipTone(reason: string): string {
  const r = reason.toLowerCase();
  if (r.includes("critical"))
    return "bg-red-100 text-red-800 border-red-200 dark:bg-red-900/40 dark:text-red-200 dark:border-red-800";
  if (r.includes("high"))
    return "bg-orange-100 text-orange-800 border-orange-200 dark:bg-orange-900/40 dark:text-orange-200 dark:border-orange-800";
  if (r.includes("medium"))
    return "bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/40 dark:text-yellow-200 dark:border-yellow-800";
  if (r.includes("low"))
    return "bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900/40 dark:text-blue-200 dark:border-blue-800";
  return "bg-gray-100 text-gray-700 border-gray-200 dark:bg-gray-800 dark:text-gray-200 dark:border-gray-700";
}

function ReasonChips({ reasons }: { reasons: string[] }) {
  const [expanded, setExpanded] = useState(false);
  if (reasons.length === 0) return null;
  const LIMIT = 5;
  const shown = expanded ? reasons : reasons.slice(0, LIMIT);
  const hidden = reasons.length - shown.length;
  return (
    <div className="mt-2 flex flex-wrap gap-1.5">
      {shown.map((r, i) => (
        <span
          key={`${r}-${i}`}
          className={`text-xs px-2 py-0.5 rounded-full border ${chipTone(r)}`}
        >
          {r}
        </span>
      ))}
      {hidden > 0 && !expanded && (
        <button
          onClick={() => setExpanded(true)}
          className="text-xs px-2 py-0.5 rounded-full border bg-gray-100 text-gray-700 border-gray-200 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-200 dark:border-gray-700 dark:hover:bg-gray-700"
        >
          +{hidden} more
        </button>
      )}
      {expanded && reasons.length > LIMIT && (
        <button
          onClick={() => setExpanded(false)}
          className="text-xs px-2 py-0.5 rounded-full border bg-gray-100 text-gray-700 border-gray-200 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-200 dark:border-gray-700 dark:hover:bg-gray-700"
        >
          Show less
        </button>
      )}
    </div>
  );
}

function ModeChip({ settings }: { settings: ExtensionSettings | null }) {
  if (!isConfigured(settings)) {
    return (
      <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-gray-500 text-white tracking-wide">
        NOT CONFIGURED
      </span>
    );
  }
  if (settings!.mode === "mock") {
    return (
      <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-yellow-400 text-black tracking-wide">
        MOCK MODE
      </span>
    );
  }
  return (
    <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-sonatype-blue text-white tracking-wide">
      REAL · IQ
    </span>
  );
}

const THEME_KEY = "hexawatch:theme";

function ThemeToggle({
  dark,
  onToggle,
}: {
  dark: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      onClick={onToggle}
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      title={dark ? "Light mode" : "Dark mode"}
      className="text-xs w-6 h-6 rounded flex items-center justify-center bg-white/10 hover:bg-white/20 text-white"
    >
      {dark ? "☀" : "☾"}
    </button>
  );
}

export function Popup() {
  const [verdict, setVerdict] = useState<FirewallVerdict | null>(null);
  const [settings, setSettings] = useState<ExtensionSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [waiverStatus, setWaiverStatus] = useState<string>("");
  const [refreshing, setRefreshing] = useState(false);
  const [dark, setDark] = useState(false);
  const [cvesExpanded, setCvesExpanded] = useState(false);

  useEffect(() => {
    chrome.storage.local.get(THEME_KEY).then((r) => {
      const t = r[THEME_KEY];
      const prefersDark =
        typeof window !== "undefined" &&
        window.matchMedia?.("(prefers-color-scheme: dark)").matches;
      const isDark = t === "dark" || (t === undefined && prefersDark);
      setDark(isDark);
    });
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    document.documentElement.style.background = dark ? "#0b1220" : "";
  }, [dark]);

  function toggleTheme() {
    const next = !dark;
    setDark(next);
    void chrome.storage.local.set({ [THEME_KEY]: next ? "dark" : "light" });
  }

  useEffect(() => {
    Promise.all([
      chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_LAST_VIEWED" }),
      chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_SETTINGS" }),
    ])
      .then(([v, s]) => {
        if (v.ok && "lastViewed" in v) setVerdict(v.lastViewed);
        if (s.ok && "settings" in s) setSettings(s.settings);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="h-full p-6 text-center text-sm text-gray-500 bg-white dark:bg-gray-900 dark:text-gray-400">
        Loading…
      </div>
    );
  }
  if (!verdict) {
    return (
      <div className="flex flex-col max-h-[600px] text-sm bg-white dark:bg-gray-900 dark:text-gray-100">
        <header className="shrink-0 bg-sonatype-dark text-white px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-sonatype-blue" />
            <span className="font-semibold">HexaWatch</span>
          </div>
          <div className="flex items-center gap-2">
            <ModeChip settings={settings} />
            <ThemeToggle dark={dark} onToggle={toggleTheme} />
          </div>
        </header>
        <div className="flex-1 min-h-0 overflow-y-auto p-6">
          <div className="font-semibold mb-2">No package detected</div>
          <p className="text-gray-600 dark:text-gray-400">
            Visit a package page on{" "}
            <a
              className="text-sonatype-blue underline"
              href="https://central.sonatype.com"
              target="_blank"
            >
              Maven Central
            </a>{" "}
            to see a Sonatype Firewall verdict.
          </p>
        </div>
        <div className="shrink-0 px-4 py-3 border-t border-gray-200 bg-white dark:bg-gray-900 dark:border-gray-700 flex justify-end">
          <button
            onClick={() => chrome.runtime.openOptionsPage()}
            className="text-xs text-sonatype-blue hover:underline"
          >
            Settings
          </button>
        </div>
      </div>
    );
  }

  const c = verdict.component;
  const p = verdict.policy;
  const { policyViolationId, repositoryId } = verdict;

  async function refreshVerdict() {
    setRefreshing(true);
    setWaiverStatus("");
    const res = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "REFRESH_VERDICT",
      purl: c.purl,
    });
    if (res.ok && "verdict" in res) setVerdict(res.verdict);
    setRefreshing(false);
  }

  async function requestWaiver() {
    setWaiverStatus("Submitting…");
    const res = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "REQUEST_WAIVER",
      purl: c.purl,
      reason: `Requested via browser extension on ${new Date().toISOString()}`,
      policyViolationId,
      repositoryId,
    });
    if (res.ok && "waiverId" in res) {
      setWaiverStatus(`Waiver submitted: ${res.waiverId}`);
    } else if (!res.ok) {
      setWaiverStatus(`Failed: ${res.error}`);
    }
  }

  const CVE_LIMIT = 5;
  const shownCves = cvesExpanded ? c.cves : c.cves.slice(0, CVE_LIMIT);

  return (
    <div className="flex flex-col max-h-[600px] text-sm bg-white text-gray-900 dark:bg-gray-900 dark:text-gray-100">
      <header className="shrink-0 bg-sonatype-dark text-white px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-sonatype-blue" />
          <span className="font-semibold">HexaWatch</span>
        </div>
        <div className="flex items-center gap-2">
          <ModeChip settings={settings} />
          <ThemeToggle dark={dark} onToggle={toggleTheme} />
        </div>
      </header>

      <div className="flex-1 min-h-0 overflow-y-auto pb-4">
      <section className="px-4 pt-4">
        <div className="text-xs text-gray-500 dark:text-gray-400 uppercase">{c.ecosystem}</div>
        <div className="font-mono text-base font-semibold break-all">{c.name}</div>
        <div className="text-gray-600 dark:text-gray-400">version {c.version}</div>
      </section>

      <section className="px-4 pt-4">
        <div
          className={`inline-flex items-center px-3 py-1.5 rounded font-semibold ${
            verdictColor[p.verdict]
          }`}
        >
          {verdictLabel[p.verdict]}
        </div>
        {p.policyName && (
          <div className="mt-2 text-xs text-gray-600 dark:text-gray-400">
            Policy: <span className="font-mono">{p.policyName}</span> · stage {p.stage}
          </div>
        )}
        <ReasonChips reasons={p.reasons} />
      </section>

      <section className="px-4 pt-4">
        <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">
          Integrity Rating
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`px-2 py-0.5 rounded text-xs font-semibold ${
              c.integrityRating === "Malicious"
                ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-200"
                : c.integrityRating === "Suspicious" || c.integrityRating === "Pending"
                  ? "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-200"
                  : c.integrityRating === "Normal"
                    ? "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200"
                    : "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-200"
            }`}
          >
            {c.integrityRating}
          </span>
          {c.threatTypes.length > 0 && (
            <span className="text-xs text-red-700 dark:text-red-300">
              {c.threatTypes.join(", ")}
            </span>
          )}
        </div>
        {c.abfMatch?.matched && (
          <div className="mt-1 text-xs text-red-700 dark:text-red-300">
            ABF match → <span className="font-mono">{c.abfMatch.matchedAgainst}</span>
          </div>
        )}
      </section>

      {c.cves.length > 0 && (
        <section className="px-4 pt-4">
          <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">
            CVEs ({c.cves.length})
          </div>
          <div className="border border-gray-200 dark:border-gray-700 rounded overflow-hidden">
            <table className="w-full text-xs">
              <thead className="bg-gray-50 text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                <tr>
                  <th className="text-left px-2 py-1 font-medium w-12">CVSS</th>
                  <th className="text-left px-2 py-1 font-medium">CVE</th>
                </tr>
              </thead>
              <tbody>
                {shownCves.map((cv, i) => (
                  <tr
                    key={cv.id}
                    className={
                      i % 2 === 0
                        ? "bg-white dark:bg-gray-900"
                        : "bg-gray-50 dark:bg-gray-800/50"
                    }
                  >
                    <td className="px-2 py-1">
                      <span
                        className={`inline-block px-1.5 py-0.5 rounded font-semibold ${
                          cv.severity === "critical"
                            ? "bg-red-600 text-white"
                            : cv.severity === "high"
                              ? "bg-orange-500 text-white"
                              : cv.severity === "medium"
                                ? "bg-yellow-400 text-black"
                                : "bg-gray-300 text-gray-800"
                        }`}
                      >
                        {cv.cvss.toFixed(1)}
                      </span>
                    </td>
                    <td className="px-2 py-1 font-mono">{cv.id}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {c.cves.length > CVE_LIMIT && (
              <button
                onClick={() => setCvesExpanded((v) => !v)}
                className="w-full text-xs px-2 py-1.5 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-sonatype-blue dark:text-blue-300 hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                {cvesExpanded
                  ? "Show less"
                  : `Show ${c.cves.length - CVE_LIMIT} more`}
              </button>
            )}
          </div>
        </section>
      )}

      {c.goldenVersion && (
        <section className="px-4 pt-4">
          <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-1">
            Golden Version
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded p-2 dark:bg-blue-900/30 dark:border-blue-800">
            <div className="font-mono font-semibold text-sonatype-blue dark:text-blue-300">
              {c.goldenVersion.version}
            </div>
            <div className="text-xs text-gray-700 dark:text-gray-300">
              Fixes {c.goldenVersion.fixesCves.length} CVE(s) ·{" "}
              {c.goldenVersion.breakingChanges ? "breaking changes" : "non-breaking"}
            </div>
          </div>
        </section>
      )}

      </div>

      <footer className="shrink-0 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900">
        {waiverStatus && (
          <div className="px-4 pt-2 text-xs text-gray-600 dark:text-gray-400">{waiverStatus}</div>
        )}
        <div className="px-4 py-3 flex items-center justify-between">
          {p.waiverEligible ? (
            <button
              onClick={requestWaiver}
              className="text-xs px-3 py-1.5 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark"
            >
              Request waiver
            </button>
          ) : (
            <span className="text-xs text-gray-500 dark:text-gray-400">
              Not eligible for waiver
            </span>
          )}
          <div className="flex items-center gap-3">
            <button
              onClick={refreshVerdict}
              disabled={refreshing}
              className="text-xs text-sonatype-blue hover:underline disabled:opacity-50"
            >
              {refreshing ? "Refreshing…" : "Refresh"}
            </button>
            <button
              onClick={() => chrome.runtime.openOptionsPage()}
              className="text-xs text-sonatype-blue hover:underline"
            >
              Settings
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
}
