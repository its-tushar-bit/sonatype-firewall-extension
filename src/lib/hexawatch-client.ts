import { ApiVrmRepo, ExtensionSettings } from "../types";

export interface HexawatchConfig {
  userCode: string;
  passCode: string;
  iqServerUrl: string;
  vrmId: string;
  vrmName: string;
  selectedRepoIds: string[];
  selectedRepos?: ApiVrmRepo[];
  updatedAt?: string;
}

function baseUrl(s: ExtensionSettings): string | null {
  const u = (s.hexawatchUrl || "").trim().replace(/\/$/, "");
  return u || null;
}

export type FetchHexawatchResult =
  | { status: "found"; config: HexawatchConfig }
  | { status: "not-found" }
  | { status: "unreachable"; error: string };

/**
 * Fetch the stored config for this userCode.
 * - "found": a row exists in hexawatch for this userCode
 * - "not-found": userCode empty, hexawatch URL not configured, no row for
 *   this userCode, or 404
 * - "unreachable": the hexawatch server itself is down / DNS/network error
 */
export async function fetchHexawatchConfig(
  settings: ExtensionSettings,
): Promise<FetchHexawatchResult> {
  if (!settings.userCode) return { status: "not-found" };
  // Hexawatch sync is an optional org-wide feature — when no URL is set we
  // treat it as "not configured" and never issue the request. Packaged /
  // distributed installs won't have a companion server to point at.
  const base = baseUrl(settings);
  if (!base) return { status: "not-found" };
  try {
    const res = await fetch(
      `${base}/extension/config?userCode=${encodeURIComponent(settings.userCode)}`,
    );
    if (res.status === 404) return { status: "not-found" };
    if (!res.ok) return { status: "unreachable", error: `HTTP ${res.status}` };
    const body = (await res.json()) as { ok: boolean; config?: HexawatchConfig };
    if (body.ok && body.config) return { status: "found", config: body.config };
    return { status: "not-found" };
  } catch (e: any) {
    return { status: "unreachable", error: e.message };
  }
}

export async function saveHexawatchConfig(
  settings: ExtensionSettings,
): Promise<{ ok: true } | { ok: false; error: string; skipped?: boolean }> {
  if (!settings.userCode) return { ok: false, error: "userCode is empty" };
  const base = baseUrl(settings);
  // No sync server configured — signal skipped so the caller can quietly
  // fall back to local persistence without surfacing a scary warning.
  if (!base) return { ok: false, error: "hexawatch URL not configured", skipped: true };
  try {
    const res = await fetch(`${base}/extension/config`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userCode: settings.userCode,
        passCode: settings.passCode,
        iqServerUrl: settings.iqServerUrl,
        vrmId: settings.vrmId,
        vrmName: settings.vrmName,
        selectedRepoIds: settings.selectedRepoIds,
        selectedRepos: settings.selectedRepos,
      }),
    });
    if (!res.ok) return { ok: false, error: `HTTP ${res.status}` };
    return { ok: true };
  } catch (e: any) {
    return { ok: false, error: e?.message || String(e) };
  }
}
