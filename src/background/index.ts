import {
  RuntimeMessage,
  RuntimeResponse,
  FirewallVerdict,
  TestConnectionResult,
  ListReposResult,
  HexawatchSyncResult,
  ExtensionSettings,
  ApiVrmRepo,
} from "../types";
import { getSettings, setSettings } from "../lib/settings";
import {
  fetchVerdict,
  listReposForRepositoryManager,
  listReposForVrm,
  listRepositoryManagers,
  listVrms,
  listWaiverReasons,
  ping,
  requestWaiver,
} from "../lib/iq-client";
import { fetchHexawatchConfig, saveHexawatchConfig } from "../lib/hexawatch-client";
// Verdicts are never cached — the user needs live IQ results every time.

// Built content-script paths — see vite.config.ts rollupOptions.output.
// upstreamUrl (mapped onto remoteUrl in iq-client) points at the browsable
// UI the user configured in IQ, so we just register on that origin — no
// hardcoded format-to-UI table needed.
const CONTENT_SCRIPT_FOR_FORMAT: Record<string, string> = {
  maven: "src/content/maven.js",
  maven2: "src/content/maven.js",
  npm: "src/content/npm.js",
  pypi: "src/content/pypi.js",
};

const tabVerdicts = new Map<number, FirewallVerdict>();
let lastVerdictAny: FirewallVerdict | null = null;

chrome.runtime.onMessage.addListener(
  (msg: RuntimeMessage, sender, sendResponse: (r: RuntimeResponse) => void) => {
    console.log("[hexawatch] bg received", msg.type, "from tab", sender.tab?.id);
    handle(msg, sender.tab?.id, sender.tab?.url)
      .then((r) => {
        console.log("[hexawatch] bg responding", msg.type, r);
        sendResponse(r);
      })
      .catch((e: Error) => {
        console.error("[hexawatch] bg handler threw for", msg.type, e);
        sendResponse({ ok: false, error: e.message });
      });
    return true; // async
  },
);

chrome.tabs.onRemoved.addListener((tabId) => {
  const v = tabVerdicts.get(tabId);
  tabVerdicts.delete(tabId);
  if (v && lastVerdictAny === v && tabVerdicts.size === 0) {
    lastVerdictAny = null;
  }
});

// Re-run the scan whenever the user switches tabs. The content script owns
// purl parsing, so we just ping it and let it re-issue GET_VERDICT. Best-effort:
// tabs with no content script (chrome://, blank pages) will error silently.
chrome.tabs.onActivated.addListener(({ tabId }) => {
  chrome.tabs.sendMessage(tabId, { type: "RESCAN" } as RuntimeMessage).catch(() => {
    // No listener on this tab — expected for non-package pages.
  });
});

async function handle(
  msg: RuntimeMessage,
  tabId?: number,
  tabUrl?: string,
): Promise<RuntimeResponse> {
  switch (msg.type) {
    case "GET_VERDICT":
    case "REFRESH_VERDICT": {
      const settings = await getSettings();
      // REFRESH_VERDICT comes from the popup — the sender tab is the popup's
      // hidden tab, not the page we care about. Fall back to the active tab's
      // URL in that case so repo-picking still uses the visited page's origin.
      let pageUrl = tabUrl;
      if (msg.type === "REFRESH_VERDICT" && !isHttpUrl(pageUrl)) {
        const [active] = await chrome.tabs.query({ active: true, windowType: "normal" });
        pageUrl = active?.url;
      }
      const verdict = await fetchVerdict(msg.purl, settings, pageUrl);
      if (tabId !== undefined) {
        tabVerdicts.set(tabId, verdict);
        await updateBadge(verdict, tabId);
      }
      lastVerdictAny = verdict;
      return { ok: true, verdict };
    }
    case "GET_SETTINGS":
      return { ok: true, settings: await getSettings() };
    case "SET_SETTINGS":
      await setSettings(msg.settings);
      return { ok: true, settings: msg.settings };
    case "REQUEST_WAIVER": {
      const settings = await getSettings();
      const id = await requestWaiver(msg.purl, settings, msg.options);
      return { ok: true, waiverId: id };
    }
    case "LIST_REPO_MANAGERS": {
      const settings = await getSettings();
      try {
        const managers = await listRepositoryManagers(settings);
        return { ok: true, managersResult: { ok: true, managers } };
      } catch (e: any) {
        return { ok: true, managersResult: { ok: false, error: e.message } };
      }
    }
    case "LIST_REPOS_FOR_MANAGER": {
      const settings = await getSettings();
      try {
        const repos = await listReposForRepositoryManager(settings, msg.repositoryManagerId);
        return { ok: true, reposResult: { ok: true, repos } };
      } catch (e: any) {
        return { ok: true, reposResult: { ok: false, error: e.message } };
      }
    }
    case "LIST_WAIVER_REASONS": {
      const settings = await getSettings();
      try {
        const reasons = await listWaiverReasons(settings);
        return { ok: true, waiverReasonsResult: { ok: true, reasons } };
      } catch (e: any) {
        return { ok: true, waiverReasonsResult: { ok: false, error: e.message } };
      }
    }
    case "GET_LAST_VIEWED": {
      // Try the active normal-browser tab first; fall back to any known
      // verdict so the popup still renders when the tab lookup misses
      // (e.g. undocked DevTools stealing lastFocusedWindow).
      const tabs = await chrome.tabs.query({ active: true, windowType: "normal" });
      let lastViewed: FirewallVerdict | null = null;
      for (const t of tabs) {
        if (t.id !== undefined && tabVerdicts.has(t.id)) {
          lastViewed = tabVerdicts.get(t.id) ?? null;
          break;
        }
      }
      if (!lastViewed) lastViewed = lastVerdictAny;
      return { ok: true, lastViewed };
    }
    case "TEST_CONNECTION": {
      const settings = await getSettings();
      const testResult = await runTestConnection(settings);
      return { ok: true, testResult };
    }
    case "LIST_REPOS_FOR_VRM": {
      const settings = await getSettings();
      const reposResult = await runListRepos(settings, msg.vrmId);
      return { ok: true, reposResult };
    }
    case "PULL_HEXAWATCH_CONFIG": {
      const syncResult = await pullFromHexawatch();
      return { ok: true, syncResult };
    }
    case "PUSH_HEXAWATCH_CONFIG": {
      const settings = await getSettings();
      const r = await saveHexawatchConfig(settings);
      const syncResult: HexawatchSyncResult = r.ok
        ? { ok: true, source: "hexawatch" }
        : { ok: false, error: r.error };
      return { ok: true, syncResult };
    }
    case "SAVE_SETTINGS_SYNCED": {
      // DB is source of truth. Write it first; only mirror to chrome.storage
      // on success so a failed DB save doesn't leave the two out of sync.
      const r = await saveHexawatchConfig(msg.settings);
      if (!r.ok) {
        return { ok: true, syncResult: { ok: false, error: r.error } };
      }
      await setSettings(msg.settings);
      // Selected repos may have changed — re-register content scripts to match.
      void syncDynamicContentScripts(msg.settings);
      return { ok: true, syncResult: { ok: true, source: "hexawatch" } };
    }
    case "RESCAN":
      // Content-script → content-script only; background ignores.
      return { ok: false, error: "RESCAN not handled by background" };
  }
}

function isHttpUrl(u?: string): boolean {
  if (!u) return false;
  return u.startsWith("http://") || u.startsWith("https://");
}

// Turn a remoteUrl like "https://repo1.maven.org/maven2/" into a match pattern
// covering that origin ("https://repo1.maven.org/*"). Returns null if the URL
// can't be parsed.
function originMatchPattern(remoteUrl: string): string | null {
  try {
    const u = new URL(remoteUrl);
    if (u.protocol !== "http:" && u.protocol !== "https:") return null;
    return `${u.protocol}//${u.host}/*`;
  } catch {
    return null;
  }
}

// Serialize concurrent calls so unregister+register can't interleave and
// produce "Duplicate script ID" errors (bootstrap racing SAVE_SETTINGS_SYNCED).
let syncInFlight: Promise<void> = Promise.resolve();
function syncDynamicContentScripts(settings: ExtensionSettings): Promise<void> {
  syncInFlight = syncInFlight.then(() => syncDynamicContentScriptsInner(settings)).catch(() => {});
  return syncInFlight;
}

// Register content scripts based on the current selectedRepos. Every call
// unregisters previously-registered dynamic scripts first so removing a repo
// removes its injection too. Skips origins we don't have host permission for
// — the caller should request permission first if it matters.
async function syncDynamicContentScriptsInner(settings: ExtensionSettings): Promise<void> {
  if (!chrome.scripting) {
    console.warn("[sonatype-firewall] chrome.scripting missing — permission not granted?");
    return;
  }
  const existing = await chrome.scripting.getRegisteredContentScripts().catch(() => []);
  const existingIds = existing.map((s) => s.id).filter(Boolean);
  if (existingIds.length > 0) {
    await chrome.scripting.unregisterContentScripts({ ids: existingIds }).catch(() => {
      // Best effort — a stale registration should not block re-registration.
    });
  }

  const repos: ApiVrmRepo[] = settings.selectedRepos || [];
  // Group by format so we can register one script per format with all its origins.
  const byFormat = new Map<string, string[]>();
  for (const r of repos) {
    const script = CONTENT_SCRIPT_FOR_FORMAT[(r.format || "").toLowerCase()];
    if (!script) continue;
    if (!r.remoteUrl) continue;
    const match = originMatchPattern(r.remoteUrl);
    if (!match) continue;
    const list = byFormat.get(script) || [];
    if (!list.includes(match)) list.push(match);
    byFormat.set(script, list);
  }

  if (byFormat.size === 0) {
    console.log("[sonatype-firewall] no dynamic content scripts to register");
    return;
  }

  const perms = await chrome.permissions.getAll();
  const grantedOrigins = new Set(perms.origins || []);

  const registrations: chrome.scripting.RegisteredContentScript[] = [];
  for (const [js, matches] of byFormat.entries()) {
    // Only include origins we have permission for; log the rest so the user
    // knows to grant them via the options page.
    const allowed = matches.filter((m) =>
      [...grantedOrigins].some((g) => matchOriginCovers(g, m)),
    );
    const denied = matches.filter((m) => !allowed.includes(m));
    if (denied.length > 0) {
      console.warn(
        "[sonatype-firewall] skipping origins without host permission:",
        denied,
        "— grant them from the options page.",
      );
    }
    if (allowed.length === 0) continue;
    registrations.push({
      id: `hexawatch-${js.replace(/[^a-z0-9]/gi, "-")}`,
      js: [js],
      matches: allowed,
      runAt: "document_idle",
      persistAcrossSessions: true,
    });
  }

  if (registrations.length === 0) return;
  // Filter out anything that's already registered under the same id — the
  // unregister step above should have cleared them, but if another caller
  // registered between the two awaits we don't want to duplicate.
  const nowExisting = await chrome.scripting
    .getRegisteredContentScripts()
    .catch(() => [] as chrome.scripting.RegisteredContentScript[]);
  const existingNames = new Set(nowExisting.map((s) => s.id));
  const toRegister = registrations.filter((r) => !existingNames.has(r.id!));
  if (toRegister.length === 0) return;
  try {
    await chrome.scripting.registerContentScripts(toRegister);
    console.log(
      "[sonatype-firewall] registered content scripts:",
      toRegister.map((r) => `${r.js?.[0]} → ${r.matches?.join(", ")}`),
    );
  } catch (e) {
    console.error("[sonatype-firewall] registerContentScripts failed:", e);
  }
}

// Loose check: does a permission pattern (e.g. "https://*/*") cover a match?
function matchOriginCovers(pattern: string, match: string): boolean {
  if (pattern === match) return true;
  // https://*/* covers any https origin; https://*.example.com/* covers subdomains.
  try {
    const p = new URL(pattern.replace("*.", "wild."));
    const m = new URL(match.replace("*.", "wild."));
    if (p.protocol !== m.protocol && p.protocol !== "*:") return false;
    if (p.hostname === "*" || p.hostname === "wild.") return true;
    if (p.hostname === m.hostname) return true;
    if (p.hostname.startsWith("wild.")) {
      const suffix = p.hostname.slice("wild.".length);
      return m.hostname === suffix || m.hostname.endsWith("." + suffix);
    }
    return false;
  } catch {
    return false;
  }
}

async function pullFromHexawatch(): Promise<HexawatchSyncResult> {
  const local = await getSettings();
  const result = await fetchHexawatchConfig(local);
  if (result.status === "unreachable") {
    return { ok: false, error: `Hexawatch unreachable — ${result.error}` };
  }
  if (result.status === "not-found") return { ok: true, source: "local" };
  const remote = result.config;
  const merged: ExtensionSettings = {
    ...local,
    passCode: remote.passCode || local.passCode,
    iqServerUrl: remote.iqServerUrl || local.iqServerUrl,
    vrmId: remote.vrmId ?? local.vrmId,
    vrmName: remote.vrmName ?? local.vrmName,
    selectedRepoIds: Array.isArray(remote.selectedRepoIds)
      ? remote.selectedRepoIds
      : local.selectedRepoIds,
    selectedRepos: Array.isArray(remote.selectedRepos)
      ? remote.selectedRepos
      : local.selectedRepos,
  };
  await setSettings(merged);
  // Repos may have arrived from the DB — reconcile content-script registrations.
  void syncDynamicContentScripts(merged);
  return { ok: true, source: "hexawatch" };
}

// On service-worker startup (extension install/reload OR profile boot),
// try once to hydrate settings from hexawatch. Best-effort — silent on failure.
async function bootstrap() {
  await pullFromHexawatch().catch(() => {});
  // Even if the pull failed, register scripts from whatever we have locally.
  const s = await getSettings();
  await syncDynamicContentScripts(s);
}
chrome.runtime.onStartup?.addListener(() => {
  void bootstrap();
});
chrome.runtime.onInstalled?.addListener(() => {
  void bootstrap();
});
// Service worker warm-start: no onInstalled/onStartup fires. Kick a sync so
// registrations stay in step with settings.
void bootstrap();

async function runTestConnection(
  settings: Awaited<ReturnType<typeof getSettings>>,
): Promise<TestConnectionResult> {
  try {
    await ping(settings);
  } catch (e: any) {
    return { ok: false, error: `Cannot reach IQ Server: ${e.message}` };
  }
  try {
    const vrms = await listVrms(settings);
    return { ok: true, vrms };
  } catch (e: any) {
    return { ok: false, error: e.message };
  }
}

async function runListRepos(
  settings: Awaited<ReturnType<typeof getSettings>>,
  vrmId: string,
): Promise<ListReposResult> {
  try {
    const repos = await listReposForVrm(settings, vrmId);
    return { ok: true, repos };
  } catch (e: any) {
    return { ok: false, error: e.message };
  }
}

async function updateBadge(v: FirewallVerdict, tabId: number) {
  const v2 = v.policy.verdict;
  const text =
    v2 === "block" ? "!" : v2 === "quarantine" ? "Q" : v2 === "warn" ? "W" : "✓";
  const color =
    v2 === "block"
      ? "#D92D20"
      : v2 === "quarantine"
        ? "#F79009"
        : v2 === "warn"
          ? "#F79009"
          : "#12B76A";
  await chrome.action.setBadgeText({ text, tabId });
  await chrome.action.setBadgeBackgroundColor({ color, tabId });
}
