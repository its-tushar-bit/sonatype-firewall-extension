import React, { useEffect, useMemo, useState } from "react";
import {
  ApiRepositoryManager,
  ApiVrmRepo,
  ApiWaiverReason,
  RuntimeMessage,
  RuntimeResponse,
  WaiverMatcherStrategy,
  WaiverRequestOptions,
  WaiverScope,
} from "../types";
import { useDarkMode } from "../lib/theme";

// Root of the "scope" cascade in the UI. Maps to IQ OwnerType at submit time.
type ScopeRoot = "root_org" | "repository_managers";

const ROOT_ORG_ID = "ROOT_ORGANIZATION_ID";
const REPO_CONTAINER_ID = "REPOSITORY_CONTAINER_ID";

function readParams() {
  const p = new URLSearchParams(location.search);
  return {
    purl: p.get("purl") || "",
    policyViolationId: p.get("policyViolationId") || "",
    componentName: p.get("componentName") || "",
    componentVersion: p.get("componentVersion") || "",
    repositoryId: p.get("repositoryId") || "",
  };
}

function ThemeToggleButton({ dark, onToggle }: { dark: boolean; onToggle: () => void }) {
  return (
    <button
      onClick={onToggle}
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      title={dark ? "Switch to light mode" : "Switch to dark mode"}
      className="w-8 h-8 rounded flex items-center justify-center bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-sonatype-dark dark:text-gray-100"
    >
      <span className="text-sm">{dark ? "☀" : "☾"}</span>
    </button>
  );
}

export function Waiver() {
  const params = useMemo(readParams, []);
  const { dark, toggle: toggleTheme } = useDarkMode();

  const [scopeRoot, setScopeRoot] = useState<ScopeRoot>("repository_managers");
  const [allManagers, setAllManagers] = useState(false);
  const [managers, setManagers] = useState<ApiRepositoryManager[] | null>(null);
  const [managersError, setManagersError] = useState<string>("");
  const [selectedManagerId, setSelectedManagerId] = useState<string>("");

  const [allReposUnderManager, setAllReposUnderManager] = useState(false);
  const [managerRepos, setManagerRepos] = useState<ApiVrmRepo[] | null>(null);
  const [managerReposError, setManagerReposError] = useState<string>("");
  const [selectedRepoId, setSelectedRepoId] = useState<string>("");

  const [matcherStrategy, setMatcherStrategy] = useState<WaiverMatcherStrategy>("EXACT_COMPONENT");
  const [waiverReasons, setWaiverReasons] = useState<ApiWaiverReason[] | null>(null);
  const [waiverReasonsError, setWaiverReasonsError] = useState<string>("");
  const [waiverReasonId, setWaiverReasonId] = useState<string>("");
  const [comment, setComment] = useState("");
  const [noteToReviewer, setNoteToReviewer] = useState("");
  const [expiryDate, setExpiryDate] = useState<string>(""); // YYYY-MM-DD
  const [expireWhenRemediation, setExpireWhenRemediation] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState<string>("");
  const [done, setDone] = useState(false);
  const [iqServerUrl, setIqServerUrl] = useState<string>("");

  useEffect(() => {
    void (async () => {
      const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "GET_SETTINGS",
      });
      if (r && r.ok && "settings" in r) setIqServerUrl(r.settings.iqServerUrl || "");
    })();
  }, []);

  const waiversDashboardUrl = iqServerUrl
    ? `${iqServerUrl.replace(/\/$/, "")}/assets/index.html#/firewall/waivers/components/requested`
    : "";

  // Load repository managers when the user picks the "repository_managers" branch.
  useEffect(() => {
    if (scopeRoot !== "repository_managers" || managers !== null) return;
    void (async () => {
      const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "LIST_REPO_MANAGERS",
      });
      if (r && r.ok && "managersResult" in r) {
        if (r.managersResult.ok) {
          // Product decision: hide VRMs from the scope picker; the extension
          // itself is configured against a VRM, but waivers are owned by
          // concrete RMs.
          const filtered = r.managersResult.managers.filter(
            (m) => (m.managerType || "").toUpperCase() !== "VIRTUAL",
          );
          setManagers(filtered);
        } else {
          setManagersError(r.managersResult.error);
          setManagers([]);
        }
      } else if (r && !r.ok) {
        setManagersError(r.error);
        setManagers([]);
      }
    })();
  }, [scopeRoot, managers]);

  // Load preset waiver reasons once on mount.
  useEffect(() => {
    void (async () => {
      try {
        const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
          type: "LIST_WAIVER_REASONS",
        });
        console.log("[hexawatch] waiver reasons response:", r);
        if (r && r.ok && "waiverReasonsResult" in r) {
          if (r.waiverReasonsResult.ok) setWaiverReasons(r.waiverReasonsResult.reasons);
          else {
            setWaiverReasonsError(r.waiverReasonsResult.error);
            setWaiverReasons([]);
          }
        } else if (r && !r.ok) {
          setWaiverReasonsError(r.error);
          setWaiverReasons([]);
        } else {
          setWaiverReasonsError("No response from background service worker");
          setWaiverReasons([]);
        }
      } catch (e: any) {
        console.error("[hexawatch] waiver reasons failed:", e);
        setWaiverReasonsError(e?.message || String(e));
        setWaiverReasons([]);
      }
    })();
  }, []);

  // Load repos under the selected RM.
  useEffect(() => {
    if (!selectedManagerId) {
      setManagerRepos(null);
      setSelectedRepoId("");
      return;
    }
    setManagerRepos(null);
    setManagerReposError("");
    setSelectedRepoId("");
    void (async () => {
      const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "LIST_REPOS_FOR_MANAGER",
        repositoryManagerId: selectedManagerId,
      });
      if (r && r.ok && "reposResult" in r) {
        if (r.reposResult.ok) setManagerRepos(r.reposResult.repos);
        else {
          setManagerReposError(r.reposResult.error);
          setManagerRepos([]);
        }
      } else if (r && !r.ok) {
        setManagerReposError(r.error);
        setManagerRepos([]);
      }
    })();
  }, [selectedManagerId]);

  const selectedManager = managers?.find((m) => m.id === selectedManagerId);
  const selectedRepo = managerRepos?.find((r) => r.repositoryId === selectedRepoId);

  const computedScope: WaiverScope | null = (() => {
    if (scopeRoot === "root_org") {
      return { ownerType: "organization", ownerId: ROOT_ORG_ID, label: "Root Organization" };
    }
    if (allManagers) {
      return {
        ownerType: "repository_container",
        ownerId: REPO_CONTAINER_ID,
        label: "All repository managers",
      };
    }
    if (!selectedManagerId) return null;
    if (allReposUnderManager) {
      return {
        ownerType: "repository_manager",
        ownerId: selectedManagerId,
        label: `Repository manager · ${selectedManager?.name ?? selectedManagerId}`,
      };
    }
    if (!selectedRepoId) return null;
    return {
      ownerType: "repository",
      ownerId: selectedRepoId,
      label: `Repository · ${selectedRepo?.publicId ?? selectedRepoId}`,
    };
  })();

  const canSubmit = Boolean(computedScope && params.policyViolationId && !submitting);

  async function submit() {
    if (!computedScope) return;
    setSubmitting(true);
    setStatus("Submitting…");
    // IQ's ApiPolicyWaiverRequestOptionsDTO.expiryTime uses format
    // "yyyy-MM-dd'T'HH:mm:ss.SSSZZ" — an RFC 822 numeric offset like "+0000",
    // not the shorthand "Z". Rewrite the trailing "Z" so Jackson accepts it.
    const expiryTime = expiryDate
      ? new Date(expiryDate).toISOString().replace(/Z$/, "+0000")
      : undefined;
    const options: WaiverRequestOptions = {
      scope: computedScope,
      policyViolationId: params.policyViolationId,
      matcherStrategy,
      comment: comment.trim() || undefined,
      noteToReviewer: noteToReviewer.trim() || undefined,
      expiryTime,
      expireWhenRemediationAvailable: expireWhenRemediation,
      waiverReasonId: waiverReasonId || undefined,
    };
    const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "REQUEST_WAIVER",
      purl: params.purl,
      options,
    });
    setSubmitting(false);
    if (r && r.ok && "waiverId" in r) {
      setStatus(`Waiver request submitted · id ${r.waiverId}`);
      setDone(true);
    } else if (r && !r.ok) {
      setStatus(`Failed: ${r.error}`);
    } else {
      setStatus("No response from background");
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-sonatype-dark dark:text-gray-100">
      <div className="max-w-2xl mx-auto p-6">
      <header className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Request Policy Waiver</h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            For{" "}
            <span className="font-mono">
              {params.componentName}
              {params.componentVersion ? `@${params.componentVersion}` : ""}
            </span>
          </p>
        </div>
        <div className="flex items-center gap-2">
          {waiversDashboardUrl && (
            <a
              href={waiversDashboardUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-sonatype-blue dark:text-blue-300 underline hover:text-sonatype-dark dark:hover:text-blue-200 whitespace-nowrap"
            >
              View waivers dashboard →
            </a>
          )}
          <ThemeToggleButton dark={dark} onToggle={toggleTheme} />
        </div>
      </header>

      {!params.policyViolationId && (
        <div className="mb-4 p-3 rounded bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-sm text-red-800 dark:text-red-200">
          Missing policy violation id — open the popup and click Request waiver again.
        </div>
      )}

      <section className="bg-white dark:bg-gray-800 dark:border dark:border-gray-700 rounded shadow-sm p-5 mb-4">
        <h2 className="font-semibold mb-3">Scope</h2>

        <label className="block mb-3">
          <span className="text-sm font-medium">Apply this waiver to</span>
          <select
            value={scopeRoot}
            onChange={(e) => setScopeRoot(e.target.value as ScopeRoot)}
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
          >
            <option value="root_org">Root Organization</option>
            <option value="repository_managers">Repository Managers</option>
          </select>
        </label>

        {scopeRoot === "repository_managers" && (
          <>
            <label className="inline-flex items-center gap-2 mb-3 text-sm">
              <input
                type="checkbox"
                checked={allManagers}
                onChange={(e) => setAllManagers(e.target.checked)}
              />
              All repository managers
            </label>

            {!allManagers && (
              <div className="mb-3">
                <label className="block">
                  <span className="text-sm font-medium">Repository manager</span>
                  <select
                    value={selectedManagerId}
                    onChange={(e) => setSelectedManagerId(e.target.value)}
                    className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
                  >
                    <option value="">— select a repository manager —</option>
                    {(managers || []).map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.name} ({m.managerType})
                      </option>
                    ))}
                  </select>
                  {managers === null && (
                    <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">Loading managers…</span>
                  )}
                  {managersError && (
                    <span className="text-xs text-red-600 dark:text-red-400 mt-1 block">
                      Failed to load managers: {managersError}
                    </span>
                  )}
                </label>
              </div>
            )}

            {!allManagers && selectedManagerId && (
              <>
                <label className="inline-flex items-center gap-2 mb-3 text-sm">
                  <input
                    type="checkbox"
                    checked={allReposUnderManager}
                    onChange={(e) => setAllReposUnderManager(e.target.checked)}
                  />
                  All repositories under this manager
                </label>

                {!allReposUnderManager && (
                  <label className="block mb-3">
                    <span className="text-sm font-medium">Repository</span>
                    <select
                      value={selectedRepoId}
                      onChange={(e) => setSelectedRepoId(e.target.value)}
                      className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
                    >
                      <option value="">— select a repository —</option>
                      {(managerRepos || []).map((r) => (
                        <option key={r.repositoryId} value={r.repositoryId}>
                          {r.publicId} ({r.format})
                        </option>
                      ))}
                    </select>
                    {managerRepos === null && (
                      <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">Loading repositories…</span>
                    )}
                    {managerReposError && (
                      <span className="text-xs text-red-600 dark:text-red-400 mt-1 block">
                        Failed to load repositories: {managerReposError}
                      </span>
                    )}
                  </label>
                )}
              </>
            )}
          </>
        )}

        <div className="mt-2 text-xs text-gray-600 dark:text-gray-400">
          Effective scope:{" "}
          {computedScope ? (
            <span className="font-mono">{computedScope.label}</span>
          ) : (
            <span className="italic text-gray-400 dark:text-gray-500">not complete</span>
          )}
        </div>
      </section>

      <section className="bg-white dark:bg-gray-800 dark:border dark:border-gray-700 rounded shadow-sm p-5 mb-4">
        <h2 className="font-semibold mb-3">Waiver details</h2>

        <label className="block mb-3">
          <span className="text-sm font-medium">Waiver reason</span>
          <select
            value={waiverReasonId}
            onChange={(e) => setWaiverReasonId(e.target.value)}
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
            disabled={waiverReasons === null}
          >
            <option value="">— none —</option>
            {(waiverReasons || []).map((r) => (
              <option key={r.id} value={r.id}>
                {r.reasonText}
                {r.type ? ` (${r.type})` : ""}
              </option>
            ))}
          </select>
          {waiverReasons === null && (
            <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">Loading reasons…</span>
          )}
          {waiverReasonsError && (
            <span className="text-xs text-red-600 dark:text-red-400 mt-1 block">
              Failed to load reasons: {waiverReasonsError}
            </span>
          )}
          {waiverReasons && waiverReasons.length === 0 && !waiverReasonsError && (
            <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">
              No preset reasons configured on this IQ instance.
            </span>
          )}
        </label>

        <label className="block mb-3">
          <span className="text-sm font-medium">Matcher strategy</span>
          <select
            value={matcherStrategy}
            onChange={(e) => setMatcherStrategy(e.target.value as WaiverMatcherStrategy)}
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
          >
            <option value="EXACT_COMPONENT">This exact component (hash-pinned)</option>
            <option value="ALL_VERSIONS">All versions of this component</option>
            <option value="ALL_COMPONENTS">All components hit by this policy</option>
            <option value="DEFAULT">Default (matches all if no hash provided)</option>
          </select>
        </label>

        <label className="block mb-3">
          <span className="text-sm font-medium">Comment</span>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={3}
            placeholder="Why is this waiver being requested?"
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
          />
        </label>

        <label className="block mb-3">
          <span className="text-sm font-medium">Note to reviewer</span>
          <textarea
            value={noteToReviewer}
            onChange={(e) => setNoteToReviewer(e.target.value)}
            rows={2}
            placeholder="Optional — extra context for the approver"
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
          />
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="text-sm font-medium">Expiration date</span>
            <input
              type="date"
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm"
            />
            <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">Leave blank for no expiry.</span>
          </label>
          <label className="inline-flex items-start gap-2 text-sm pt-6">
            <input
              type="checkbox"
              checked={expireWhenRemediation}
              onChange={(e) => setExpireWhenRemediation(e.target.checked)}
              disabled={matcherStrategy !== "EXACT_COMPONENT"}
              className="mt-0.5"
            />
            <span>
              Expire when remediation available
              <span className="block text-xs text-gray-500 dark:text-gray-400">
                (Only valid with EXACT_COMPONENT.)
              </span>
            </span>
          </label>
        </div>
      </section>

      <div className="flex items-center gap-3">
        <button
          onClick={submit}
          disabled={!canSubmit || done}
          className="px-4 py-2 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark disabled:opacity-50"
        >
          {submitting ? "Submitting…" : "Submit waiver request"}
        </button>
        <button
          onClick={() => window.close()}
          className="px-4 py-2 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 dark:text-gray-100 rounded"
        >
          {done ? "Close" : "Cancel"}
        </button>
        {status && (
          <span className="text-sm text-gray-700 dark:text-gray-300">· {status}</span>
        )}
        {done && waiversDashboardUrl && (
          <a
            href={waiversDashboardUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-sonatype-blue dark:text-blue-300 underline hover:text-sonatype-dark dark:hover:text-blue-200"
          >
            Open dashboard →
          </a>
        )}
      </div>
      </div>
    </div>
  );
}
