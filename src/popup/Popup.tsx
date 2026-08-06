import React, { useEffect, useState } from "react";
import { FirewallVerdict, RuntimeMessage, RuntimeResponse } from "../types";

const verdictColor: Record<string, string> = {
  block: "bg-sonatype-danger text-white",
  quarantine: "bg-sonatype-warn text-black",
  warn: "bg-sonatype-warn text-black",
  allow: "bg-sonatype-ok text-white",
};

const verdictLabel: Record<string, string> = {
  block: "BLOCKED",
  quarantine: "QUARANTINED",
  warn: "WARNING",
  allow: "ALLOWED",
};

export function Popup() {
  const [verdict, setVerdict] = useState<FirewallVerdict | null>(null);
  const [loading, setLoading] = useState(true);
  const [waiverStatus, setWaiverStatus] = useState<string>("");

  useEffect(() => {
    chrome.runtime
      .sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_LAST_VIEWED" })
      .then((r) => {
        if (r.ok && "lastViewed" in r) setVerdict(r.lastViewed);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="p-6 text-center text-sm text-gray-500">Loading…</div>;
  }
  if (!verdict) {
    return (
      <div className="p-6 text-sm">
        <div className="font-semibold mb-2">No package detected</div>
        <p className="text-gray-600">
          Visit a package page on{" "}
          <a className="text-sonatype-blue underline" href="https://www.npmjs.com" target="_blank">
            npm
          </a>
          ,{" "}
          <a className="text-sonatype-blue underline" href="https://pypi.org" target="_blank">
            PyPI
          </a>
          , or{" "}
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
    );
  }

  const c = verdict.component;
  const p = verdict.policy;

  async function requestWaiver() {
    setWaiverStatus("Submitting…");
    const res = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "REQUEST_WAIVER",
      purl: c.purl,
      reason: `Requested via browser extension on ${new Date().toISOString()}`,
    });
    if (res.ok && "waiverId" in res) {
      setWaiverStatus(`Waiver submitted: ${res.waiverId}`);
    } else if (!res.ok) {
      setWaiverStatus(`Failed: ${res.error}`);
    }
  }

  return (
    <div className="text-sm">
      <header className="bg-sonatype-dark text-white px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-sonatype-blue" />
          <span className="font-semibold">Sonatype Firewall</span>
        </div>
        <span className="text-xs opacity-75">
          {verdict.source === "mock" ? "MOCK MODE" : "Connected"}
        </span>
      </header>

      <section className="px-4 pt-4">
        <div className="text-xs text-gray-500 uppercase">{c.ecosystem}</div>
        <div className="font-mono text-base font-semibold break-all">{c.name}</div>
        <div className="text-gray-600">version {c.version}</div>
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
          <div className="mt-2 text-xs text-gray-600">
            Policy: <span className="font-mono">{p.policyName}</span> · stage {p.stage}
          </div>
        )}
        <ul className="mt-2 text-xs text-gray-700 list-disc list-inside">
          {p.reasons.map((r) => (
            <li key={r}>{r}</li>
          ))}
        </ul>
      </section>

      <section className="px-4 pt-4">
        <div className="text-xs uppercase tracking-wide text-gray-500 mb-1">
          Integrity Rating
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`px-2 py-0.5 rounded text-xs font-semibold ${
              c.integrityRating === "Malicious"
                ? "bg-red-100 text-red-700"
                : c.integrityRating === "Suspicious" || c.integrityRating === "Pending"
                  ? "bg-yellow-100 text-yellow-800"
                  : c.integrityRating === "Normal"
                    ? "bg-green-100 text-green-800"
                    : "bg-gray-100 text-gray-700"
            }`}
          >
            {c.integrityRating}
          </span>
          {c.threatTypes.length > 0 && (
            <span className="text-xs text-red-700">{c.threatTypes.join(", ")}</span>
          )}
        </div>
        {c.abfMatch?.matched && (
          <div className="mt-1 text-xs text-red-700">
            ABF match → <span className="font-mono">{c.abfMatch.matchedAgainst}</span>
          </div>
        )}
      </section>

      {c.cves.length > 0 && (
        <section className="px-4 pt-4">
          <div className="text-xs uppercase tracking-wide text-gray-500 mb-1">CVEs</div>
          <ul className="space-y-1">
            {c.cves.map((cv) => (
              <li key={cv.id} className="text-xs flex items-start gap-2">
                <span
                  className={`px-1.5 py-0.5 rounded font-semibold ${
                    cv.severity === "critical"
                      ? "bg-red-600 text-white"
                      : cv.severity === "high"
                        ? "bg-orange-500 text-white"
                        : "bg-gray-300"
                  }`}
                >
                  {cv.cvss.toFixed(1)}
                </span>
                <div className="flex-1">
                  <div className="font-mono">{cv.id}</div>
                  <div className="text-gray-600">{cv.title}</div>
                  {cv.reachable !== undefined && (
                    <div className={cv.reachable ? "text-red-700" : "text-green-700"}>
                      {cv.reachable ? "Reachable in scanned apps" : "Not reachable"}
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {c.goldenVersion && (
        <section className="px-4 pt-4">
          <div className="text-xs uppercase tracking-wide text-gray-500 mb-1">
            Golden Version
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded p-2">
            <div className="font-mono font-semibold text-sonatype-blue">
              {c.goldenVersion.version}
            </div>
            <div className="text-xs text-gray-700">
              Fixes {c.goldenVersion.fixesCves.length} CVE(s) ·{" "}
              {c.goldenVersion.breakingChanges ? "breaking changes" : "non-breaking"}
            </div>
          </div>
        </section>
      )}

      <section className="px-4 py-4 mt-2 border-t border-gray-200 flex items-center justify-between">
        {p.waiverEligible ? (
          <button
            onClick={requestWaiver}
            className="text-xs px-3 py-1.5 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark"
          >
            Request waiver
          </button>
        ) : (
          <span className="text-xs text-gray-500">Not eligible for waiver</span>
        )}
        <button
          onClick={() => chrome.runtime.openOptionsPage()}
          className="text-xs text-sonatype-blue hover:underline"
        >
          Settings
        </button>
      </section>
      {waiverStatus && (
        <div className="px-4 pb-3 text-xs text-gray-600">{waiverStatus}</div>
      )}
    </div>
  );
}
