import { ExtensionSettings, FirewallVerdict } from "../types";

function authHeader(s: ExtensionSettings): string {
  return `Basic ${btoa(`${s.userCode}:${s.passCode}`)}`;
}

export async function fetchVerdict(
  purl: string,
  settings: ExtensionSettings,
): Promise<FirewallVerdict> {
  const base = settings.iqServerUrl.replace(/\/$/, "");
  const res = await fetch(`${base}/api/v2/firewall/verdict`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authHeader(settings),
    },
    body: JSON.stringify({ purl }),
  });
  if (!res.ok) throw new Error(`Verdict failed: HTTP ${res.status}`);
  const data = (await res.json()) as FirewallVerdict;
  return { ...data, source: "mock" };
}

export async function requestWaiver(
  purl: string,
  reason: string,
  settings: ExtensionSettings,
): Promise<string> {
  const base = settings.iqServerUrl.replace(/\/$/, "");
  const res = await fetch(`${base}/api/v2/waivers`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authHeader(settings),
    },
    body: JSON.stringify({ purl, reason }),
  });
  if (!res.ok) throw new Error(`Waiver failed: HTTP ${res.status}`);
  const data = await res.json();
  return data.waiverId;
}
