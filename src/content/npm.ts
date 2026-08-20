import { getSettings, injectBadge, injectUnsupportedBadge, requestVerdict } from "./shared";
import { RuntimeMessage } from "../types";

async function runScan() {
  // URL pattern: https://www.npmjs.com/package/<name>  or  /package/<name>/v/<version>
  const m = location.pathname.match(/^\/package\/((?:@[^/]+\/)?[^/]+)(?:\/v\/([^/]+))?/);
  if (!m) return;
  const host =
    document.querySelector<HTMLElement>('[class*="package-tab"] h2')?.parentElement ||
    document.querySelector<HTMLElement>("#top main") ||
    document.querySelector<HTMLElement>("main") ||
    document.body;

  const settings = await getSettings();
  if (settings?.mode === "real") {
    injectUnsupportedBadge(host, "Maven only in this build");
    return;
  }

  const name = m[1];
  const version = m[2] || (await readVersionFromDom()) || "latest";
  const purl = `pkg:npm/${encodeURIComponent(name).replace(/%2F/g, "/")}@${version}`;

  const result = await requestVerdict(purl);
  if (result.kind === "error") {
    injectUnsupportedBadge(host, `scan failed — ${result.error}`);
    return;
  }
  injectBadge(host, result.verdict);
}

chrome.runtime.onMessage.addListener((msg: RuntimeMessage) => {
  if (msg.type === "RESCAN") void runScan();
});

void runScan();

async function readVersionFromDom(): Promise<string | null> {
  // npm renders "Version" near the install snippet; fall back to "latest"
  const el = document.querySelector('[class*="package-version"]');
  return el?.textContent?.trim() || null;
}
