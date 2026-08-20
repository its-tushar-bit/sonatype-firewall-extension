import React, { useEffect, useMemo, useState } from "react";
import {
  ApiVrm,
  ApiVrmRepo,
  ExtensionSettings,
  RuntimeMessage,
  RuntimeResponse,
  DEFAULT_SETTINGS,
} from "../types";
import { useDarkMode } from "../lib/theme";
import { Logo } from "../lib/Logo";

type ConnectState =
  | { status: "idle" }
  | { status: "connecting" }
  | { status: "connected"; vrms: ApiVrm[] }
  | { status: "error"; error: string };

type ReposState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "loaded"; repos: ApiVrmRepo[] }
  | { status: "error"; error: string };

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

export function Options() {
  const { dark, toggle: toggleTheme } = useDarkMode();
  const [settings, setSettings] = useState<ExtensionSettings>(DEFAULT_SETTINGS);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [connect, setConnect] = useState<ConnectState>({ status: "idle" });
  const [repos, setRepos] = useState<ReposState>({ status: "idle" });

  useEffect(() => {
    (async () => {
      // Always pull DB first if we have a userCode — it's the source of truth.
      // Then read local (now hydrated from the pull) to seed the form.
      const local = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "GET_SETTINGS",
      });
      if (local && local.ok && "settings" in local && local.settings.userCode) {
        await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
          type: "PULL_HEXAWATCH_CONFIG",
        });
      }
      const fresh = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "GET_SETTINGS",
      });
      if (fresh && fresh.ok && "settings" in fresh) setSettings(fresh.settings);
    })();
  }, []);

  const [syncStatus, setSyncStatus] = useState<string>("");

  // Auto-pull from hexawatch. Runs on: userCode blur, mount, and mode→real.
  // Silently no-op when userCode is empty.
  async function pullFromDbForUserCode(userCode: string) {
    if (!userCode.trim()) return;
    setSyncStatus("Fetching config from server…");
    // Persist the userCode locally first so PULL_HEXAWATCH_CONFIG has a key.
    await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "SET_SETTINGS",
      settings: { ...settings, userCode },
    });
    const pull = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "PULL_HEXAWATCH_CONFIG",
    });
    const fresh = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "GET_SETTINGS",
    });
    if (fresh && fresh.ok && "settings" in fresh) setSettings(fresh.settings);
    if (pull && pull.ok && "syncResult" in pull) {
      if (pull.syncResult.ok && pull.syncResult.source === "hexawatch") {
        setSyncStatus("Loaded config from server");
      } else if (pull.syncResult.ok) {
        setSyncStatus("No saved config for this user code yet");
      } else {
        setSyncStatus(`Fetch failed: ${pull.syncResult.error}`);
      }
    }
  }

  async function requestOriginsForSelectedRepos(): Promise<{ ok: boolean; error?: string }> {
    const origins = new Set<string>();
    for (const r of settings.selectedRepos) {
      if (!r.remoteUrl) continue;
      try {
        origins.add(new URL(r.remoteUrl).origin + "/*");
      } catch {
        // skip malformed
      }
    }
    if (origins.size === 0) return { ok: true };
    const originsList = [...origins];
    try {
      const has = await chrome.permissions.contains({ origins: originsList });
      if (has) return { ok: true };
      const granted = await chrome.permissions.request({ origins: originsList });
      if (!granted) {
        return {
          ok: false,
          error:
            "Host permission for one or more selected-repo origins was denied. Grant it so content scripts can inject.",
        };
      }
      return { ok: true };
    } catch (e: any) {
      return { ok: false, error: e.message };
    }
  }

  async function save() {
    setSavedAt(null);
    setSyncStatus("Saving to server…");
    // Ask for host permissions BEFORE persisting so the background can register
    // content scripts for these origins on the next sync.
    const perm = await requestOriginsForSelectedRepos();
    if (!perm.ok) {
      setSyncStatus(perm.error || "Permission denied");
      return;
    }
    const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "SAVE_SETTINGS_SYNCED",
      settings,
    });
    if (r && r.ok && "syncResult" in r && r.syncResult.ok) {
      setSavedAt(Date.now());
      if (r.syncResult.source === "hexawatch") {
        setSyncStatus("Saved to server");
      } else {
        // Hexawatch was unreachable but we persisted locally — the extension
        // is fully usable, we just skipped the org-wide sync.
        setSyncStatus(
          r.syncResult.warning
            ? `Saved locally (sync server unreachable: ${r.syncResult.warning})`
            : "Saved locally",
        );
      }
      return;
    }
    const err =
      r && r.ok && "syncResult" in r && !r.syncResult.ok
        ? r.syncResult.error
        : r && !r.ok
          ? r.error
          : "no response";
    setSyncStatus(`Save failed: ${err} — nothing was persisted`);
  }

  async function ensureOriginPermission(url: string): Promise<{ ok: boolean; error?: string }> {
    let origin: string;
    try {
      origin = new URL(url).origin + "/*";
    } catch {
      return { ok: false, error: "IQ Server URL is not a valid URL" };
    }
    try {
      const has = await chrome.permissions.contains({ origins: [origin] });
      if (has) return { ok: true };
      const granted = await chrome.permissions.request({ origins: [origin] });
      if (!granted) {
        return {
          ok: false,
          error: `Permission for ${origin} was denied. Grant it to reach this IQ Server.`,
        };
      }
      return { ok: true };
    } catch (e: any) {
      return { ok: false, error: e.message };
    }
  }

  async function testConnection() {
    // chrome.permissions.request must be the first thing inside the click handler
    // so it inherits the user gesture. Awaiting a message first drops the gesture
    // and Chrome refuses the prompt with "may only be called from a user input handler".
    const perm = await ensureOriginPermission(settings.iqServerUrl);
    if (!perm.ok) {
      setConnect({ status: "error", error: perm.error || "Permission denied" });
      return;
    }
    setConnect({ status: "connecting" });
    setRepos({ status: "idle" });
    try {
      // Persist so background reads the URL/creds we're testing.
      await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "SET_SETTINGS",
        settings,
      });
      const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
        type: "TEST_CONNECTION",
      });
      if (!r) {
        setConnect({
          status: "error",
          error: "Background did not respond — reload the extension and try again",
        });
      } else if (r.ok && "testResult" in r) {
        if (r.testResult.ok) {
          setConnect({ status: "connected", vrms: r.testResult.vrms });
          if (settings.vrmId && r.testResult.vrms.some((v) => v.id === settings.vrmId)) {
            void loadReposFor(settings.vrmId);
          }
        } else {
          setConnect({ status: "error", error: r.testResult.error });
        }
      } else if (!r.ok) {
        setConnect({ status: "error", error: r.error });
      }
    } catch (e: any) {
      setConnect({ status: "error", error: e.message });
    }
  }

  async function loadReposFor(vrmId: string) {
    setRepos({ status: "loading" });
    const r = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "LIST_REPOS_FOR_VRM",
      vrmId,
    });
    if (r && r.ok && "reposResult" in r) {
      if (r.reposResult.ok) {
        setRepos({ status: "loaded", repos: r.reposResult.repos });
        // Backfill full repo objects if we only have IDs (old settings written
        // before ExtensionSettings.selectedRepos existed). Also drops stale IDs
        // that no longer exist in the VRM.
        setSettings((prev) => {
          if (prev.selectedRepoIds.length === 0) return prev;
          const byId = new Map(r.reposResult.repos.map((x) => [x.repositoryId, x]));
          const nextRepos = prev.selectedRepoIds
            .map((id) => byId.get(id))
            .filter((x): x is ApiVrmRepo => Boolean(x));
          const nextIds = nextRepos.map((x) => x.repositoryId);
          const sameRepos =
            prev.selectedRepos.length === nextRepos.length &&
            prev.selectedRepos.every(
              (r, i) =>
                r.repositoryId === nextRepos[i].repositoryId &&
                r.remoteUrl === nextRepos[i].remoteUrl,
            );
          const sameIds =
            prev.selectedRepoIds.length === nextIds.length &&
            prev.selectedRepoIds.every((id, i) => id === nextIds[i]);
          if (sameRepos && sameIds) return prev;
          return { ...prev, selectedRepoIds: nextIds, selectedRepos: nextRepos };
        });
      } else {
        setRepos({ status: "error", error: r.reposResult.error });
      }
    } else if (r && !r.ok) {
      setRepos({ status: "error", error: r.error });
    } else {
      setRepos({ status: "error", error: "No response from background" });
    }
  }

  function onVrmChange(vrmId: string) {
    if (connect.status !== "connected") return;
    const vrm = connect.vrms.find((v) => v.id === vrmId);
    const keep = settings.vrmId === vrmId;
    setSettings({
      ...settings,
      vrmId,
      vrmName: vrm?.name || "",
      selectedRepoIds: keep ? settings.selectedRepoIds : [],
      selectedRepos: keep ? settings.selectedRepos : [],
    });
    if (vrmId) void loadReposFor(vrmId);
    else setRepos({ status: "idle" });
  }

  function addRepo(repoId: string) {
    if (!repoId || settings.selectedRepoIds.includes(repoId)) return;
    const repo = repos.status === "loaded" ? repos.repos.find((r) => r.repositoryId === repoId) : undefined;
    setSettings({
      ...settings,
      selectedRepoIds: [...settings.selectedRepoIds, repoId],
      selectedRepos: repo ? [...settings.selectedRepos, repo] : settings.selectedRepos,
    });
  }

  function removeRepo(repoId: string) {
    setSettings({
      ...settings,
      selectedRepoIds: settings.selectedRepoIds.filter((id) => id !== repoId),
      selectedRepos: settings.selectedRepos.filter((r) => r.repositoryId !== repoId),
    });
  }

  function update<K extends keyof ExtensionSettings>(k: K, v: ExtensionSettings[K]) {
    setSettings({ ...settings, [k]: v });
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-sonatype-dark dark:text-gray-100">
      <div className="max-w-2xl mx-auto p-6">
        <header className="mb-6 flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <Logo dark={dark} size={40} />
            <div>
              <h1 className="text-2xl font-semibold">HexaWatch — Settings</h1>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Configure where the extension fetches verdicts from.
              </p>
            </div>
          </div>
          <ThemeToggleButton dark={dark} onToggle={toggleTheme} />
        </header>

        <section className="bg-white dark:bg-gray-800 dark:border dark:border-gray-700 rounded shadow-sm p-5 mb-4">
          <h2 className="font-semibold mb-3">IQ Server</h2>

        <Field
          label="IQ Server URL"
          value={settings.iqServerUrl}
          onChange={(v) => update("iqServerUrl", v)}
          placeholder="http://localhost:8070"
        />
        <Field
          label="User Code"
          value={settings.userCode}
          onChange={(v) => update("userCode", v)}
          onBlur={(v) => void pullFromDbForUserCode(v)}
          placeholder="your-iq-user-code"
        />
        <Field
          label="Pass Code"
          value={settings.passCode}
          onChange={(v) => update("passCode", v)}
          type="password"
          placeholder="your-iq-pass-code"
        />

        <div className="mt-3 flex items-center gap-3">
          <button
            onClick={testConnection}
            disabled={connect.status === "connecting"}
            className="px-3 py-1.5 text-sm bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 dark:text-gray-100 rounded disabled:opacity-50"
          >
            {connect.status === "connecting" ? "Connecting…" : "Connect"}
          </button>
          <ConnectStatusView state={connect} />
        </div>

        {connect.status === "connected" && (
          <VrmPicker
            vrms={connect.vrms}
            selectedVrmId={settings.vrmId}
            onChange={onVrmChange}
          />
        )}

        {settings.vrmId && (
          <RepoPicker
            reposState={repos}
            selectedRepoIds={settings.selectedRepoIds}
            onAdd={addRepo}
            onRemove={removeRepo}
          />
        )}
      </section>

        <div className="flex items-center gap-3">
          <button
            onClick={save}
            className="px-4 py-2 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark"
          >
            Save settings
          </button>
          {savedAt && (
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Saved at {new Date(savedAt).toLocaleTimeString()}
            </span>
          )}
          {syncStatus && (
            <span className="text-sm text-gray-600 dark:text-gray-400">· {syncStatus}</span>
          )}
        </div>
      </div>
    </div>
  );
}

function ConnectStatusView({ state }: { state: ConnectState }) {
  if (state.status === "idle") return null;
  if (state.status === "connecting") return null;
  if (state.status === "error") {
    return (
      <span className="text-sm text-red-600 dark:text-red-400">Failed: {state.error}</span>
    );
  }
  return (
    <span className="text-sm text-green-700 dark:text-green-400">
      Connected · {state.vrms.length} VRM{state.vrms.length === 1 ? "" : "s"} available
    </span>
  );
}

function VrmPicker({
  vrms,
  selectedVrmId,
  onChange,
}: {
  vrms: ApiVrm[];
  selectedVrmId: string;
  onChange: (id: string) => void;
}) {
  return (
    <label className="block mt-4">
      <span className="text-sm font-medium">Virtual Repository Manager</span>
      <select
        value={selectedVrmId}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 px-3 py-1.5 text-sm"
      >
        <option value="">— select a VRM —</option>
        {vrms.map((v) => (
          <option key={v.id} value={v.id}>
            {v.name}
            {v.childRepositoryCount !== undefined ? ` (${v.childRepositoryCount})` : ""}
          </option>
        ))}
      </select>
      {vrms.length === 0 && (
        <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">
          No VRMs configured on this IQ instance.
        </span>
      )}
    </label>
  );
}

function RepoPicker({
  reposState,
  selectedRepoIds,
  onAdd,
  onRemove,
}: {
  reposState: ReposState;
  selectedRepoIds: string[];
  onAdd: (id: string) => void;
  onRemove: (id: string) => void;
}) {
  const repos = reposState.status === "loaded" ? reposState.repos : [];
  const byId = useMemo(() => new Map(repos.map((r) => [r.repositoryId, r])), [repos]);
  const available = repos.filter((r) => !selectedRepoIds.includes(r.repositoryId));

  return (
    <div className="mt-4">
      <label className="block">
        <span className="text-sm font-medium">Repositories</span>
        {reposState.status === "loading" && (
          <span className="text-xs text-gray-500 dark:text-gray-400 mt-1 block">
            Loading repositories…
          </span>
        )}
        {reposState.status === "error" && (
          <span className="text-xs text-red-600 dark:text-red-400 mt-1 block">
            Failed: {reposState.error}
          </span>
        )}
        {reposState.status === "loaded" && (
          <select
            value=""
            onChange={(e) => {
              onAdd(e.target.value);
              e.target.value = "";
            }}
            className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 px-3 py-1.5 text-sm"
          >
            <option value="">
              {available.length === 0 ? "All repositories selected" : "— add a repository —"}
            </option>
            {available.map((r) => (
              <option key={r.repositoryId} value={r.repositoryId}>
                {r.publicId} ({r.format})
              </option>
            ))}
          </select>
        )}
      </label>

      {selectedRepoIds.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {selectedRepoIds.map((id) => {
            const r = byId.get(id);
            const label = r ? `${r.publicId} · ${r.format}` : id;
            return (
              <span
                key={id}
                className="inline-flex items-center gap-1.5 pl-2 pr-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/40 border border-blue-200 dark:border-blue-800 text-xs text-blue-800 dark:text-blue-200"
              >
                {label}
                <button
                  onClick={() => onRemove(id)}
                  className="w-4 h-4 rounded-full bg-blue-200 hover:bg-blue-300 dark:bg-blue-800 dark:hover:bg-blue-700 text-blue-900 dark:text-blue-100 leading-none flex items-center justify-center"
                  aria-label={`Remove ${label}`}
                  title="Remove"
                >
                  ×
                </button>
              </span>
            );
          })}
        </div>
      )}
    </div>
  );
}

function Field(props: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  onBlur?: (v: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <label className="block mb-2">
      <span className="text-sm font-medium">{props.label}</span>
      <input
        type={props.type || "text"}
        value={props.value}
        placeholder={props.placeholder}
        onChange={(e) => props.onChange(e.target.value)}
        onBlur={(e) => props.onBlur?.(e.target.value)}
        className="mt-1 block w-full rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 dark:text-gray-100 dark:placeholder-gray-500 px-3 py-1.5 text-sm font-mono"
      />
    </label>
  );
}
