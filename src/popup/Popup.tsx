import React, { useEffect, useState } from "react";
import { FirewallVerdict, RuntimeMessage, RuntimeResponse } from "../types";
import { useDarkMode } from "../lib/theme";
import { Logo } from "../lib/Logo";

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

function HeaderIconButton({
  onClick,
  disabled,
  label,
  children,
}: {
  onClick: () => void;
  disabled?: boolean;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="relative group">
      <button
        onClick={onClick}
        disabled={disabled}
        aria-label={label}
        className="w-6 h-6 rounded flex items-center justify-center bg-white/10 hover:bg-white/20 disabled:opacity-40 text-white"
      >
        {children}
      </button>
      <span
        role="tooltip"
        className="pointer-events-none absolute top-full right-0 mt-1 z-10 whitespace-nowrap rounded bg-gray-900 text-white text-[10px] font-medium px-1.5 py-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-100 shadow-md border border-white/10"
      >
        {label}
      </span>
    </div>
  );
}

function ThemeToggle({ dark, onToggle }: { dark: boolean; onToggle: () => void }) {
  return (
    <HeaderIconButton
      onClick={onToggle}
      label={dark ? "Switch to light mode" : "Switch to dark mode"}
    >
      <span className="text-xs">{dark ? "☀" : "☾"}</span>
    </HeaderIconButton>
  );
}

function RefreshIcon({ spinning }: { spinning?: boolean }) {
  return (
    <svg
      viewBox="0 0 16 16"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={spinning ? "animate-spin" : ""}
    >
      <path d="M13.5 3.5v3h-3" />
      <path d="M2.5 12.5v-3h3" />
      <path d="M12.5 6.5A5 5 0 0 0 3.6 7.4" />
      <path d="M3.5 9.5a5 5 0 0 0 8.9-.9" />
    </svg>
  );
}

function GearIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export function Popup() {
  const [verdict, setVerdict] = useState<FirewallVerdict | null>(null);
  const [loading, setLoading] = useState(true);
  const [waiverStatus, setWaiverStatus] = useState<string>("");
  const [refreshing, setRefreshing] = useState(false);
  const { dark, toggle: toggleTheme } = useDarkMode();
  const [cvesExpanded, setCvesExpanded] = useState(false);
  const [iqServerUrl, setIqServerUrl] = useState<string>("");

  useEffect(() => {
    chrome.runtime
      .sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_SETTINGS" })
      .then((r) => {
        if (r && r.ok && "settings" in r) setIqServerUrl(r.settings.iqServerUrl || "");
      });
  }, []);

  useEffect(() => {
    chrome.runtime
      .sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_LAST_VIEWED" })
      .then((v) => {
        if (v.ok && "lastViewed" in v) setVerdict(v.lastViewed);
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
      <div className="flex flex-col w-full max-w-[420px] max-h-[600px] overflow-hidden text-sm bg-white dark:bg-gray-900 dark:text-gray-100">
        <header className="shrink-0 bg-sonatype-dark text-white px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Logo dark size={20} />
            <span className="font-semibold">HexaWatch</span>
          </div>
          <div className="flex items-center gap-2">
            <HeaderIconButton
              onClick={() => chrome.runtime.openOptionsPage()}
              label="Settings"
            >
              <GearIcon />
            </HeaderIconButton>
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

  function openFirewallReport() {
    if (!iqServerUrl || !repositoryId) return;
    const base = iqServerUrl.replace(/\/$/, "");
    const url = `${base}/assets/index.html#/firewall/repository/${encodeURIComponent(
      repositoryId,
    )}/result`;
    void chrome.tabs.create({ url });
  }

  function openWaiverPage() {
    if (!policyViolationId) return;
    const params = new URLSearchParams({
      purl: c.purl,
      policyViolationId,
      componentName: c.name,
      componentVersion: c.version,
    });
    if (repositoryId) params.set("repositoryId", repositoryId);
    const url = `${chrome.runtime.getURL("src/waiver/index.html")}?${params.toString()}`;
    void chrome.tabs.create({ url });
  }

  const CVE_LIMIT = 5;
  const shownCves = cvesExpanded ? c.cves : c.cves.slice(0, CVE_LIMIT);

  return (
    <div className="flex flex-col w-full max-w-[420px] max-h-[600px] overflow-hidden text-sm bg-white text-gray-900 dark:bg-gray-900 dark:text-gray-100">
      <header className="shrink-0 bg-sonatype-dark text-white px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="font-semibold">HexaWatch</span>
        </div>
        <div className="flex items-center gap-2">
          <HeaderIconButton
            onClick={refreshVerdict}
            disabled={refreshing}
            label={refreshing ? "Refreshing…" : "Refresh verdict"}
          >
            <RefreshIcon spinning={refreshing} />
          </HeaderIconButton>
          <HeaderIconButton
            onClick={() => chrome.runtime.openOptionsPage()}
            label="Settings"
          >
            <GearIcon />
          </HeaderIconButton>
          <ThemeToggle dark={dark} onToggle={toggleTheme} />
        </div>
      </header>

      <div className="flex-1 min-h-0 overflow-x-hidden overflow-y-auto pb-4">
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



      </div>

      <footer className="shrink-0 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900">
        {waiverStatus && (
          <div className="px-4 pt-2 text-xs text-gray-600 dark:text-gray-400">{waiverStatus}</div>
        )}
        <div className="px-4 py-3 flex items-center gap-2">
          {repositoryId && iqServerUrl && (
            <button
              onClick={openFirewallReport}
              className="text-xs px-3 py-1.5 border border-sonatype-blue text-sonatype-blue rounded hover:bg-sonatype-blue hover:text-white dark:text-blue-300 dark:border-blue-500 dark:hover:bg-blue-500 dark:hover:text-white"
            >
              View report
            </button>
          )}
          {p.waiverEligible ? (
            <button
              onClick={openWaiverPage}
              className="text-xs px-3 py-1.5 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark ml-auto"
            >
              Request waiver
            </button>
          ) : (
            <span className="text-xs text-gray-500 dark:text-gray-400 ml-auto">
              Not eligible for waiver
            </span>
          )}
        </div>
      </footer>
    </div>
  );
}
